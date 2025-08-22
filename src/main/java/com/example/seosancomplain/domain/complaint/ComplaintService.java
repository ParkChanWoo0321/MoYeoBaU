package com.example.seosancomplain.domain.complaint;

import com.example.seosancomplain.dashboard.*;
import com.example.seosancomplain.domain.admin.CategoryCount;
import com.example.seosancomplain.domain.admin.comment.AdminComment;
import com.example.seosancomplain.domain.admin.comment.AdminCommentDto;
import com.example.seosancomplain.domain.admin.comment.AdminCommentRepository;
import com.example.seosancomplain.domain.admin.dto.AdminReportDto;
import com.example.seosancomplain.domain.admin.dto.CategoryCountDto;
import com.example.seosancomplain.domain.ai.AiMinwonClient;
import com.example.seosancomplain.domain.ai.SummarizerClient;
import com.example.seosancomplain.domain.attachment.Attachment;
import com.example.seosancomplain.domain.attachment.AttachmentRepository;
import com.example.seosancomplain.domain.region.SeosanRegion;
import com.example.seosancomplain.dto.*;
import com.example.seosancomplain.exception.CustomException;
import com.example.seosancomplain.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final AdminCommentRepository adminCommentRepository;
    private final AttachmentRepository attachmentRepository;
    private final ObjectMapper objectMapper;
    private final SummarizerClient summarizerClient;
    private final AiMinwonClient aiMinwonClient;

    @Value("${ai.timeout.response-ms:300000}")
    private long aiTimeoutMs;

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    @Transactional(readOnly = true)
    public AiComposeResponseDto composeOnly(AiComposeRequestDto dto) {
        var images = (dto.getImageUrls() == null ? List.<AiMinwonClient.ImageInput>of()
                : dto.getImageUrls().stream().map(AiMinwonClient.ImageInput::fromUrl).toList());

        var in = AiMinwonClient.AiComposeIn.builder()
                .complaintText(dto.getContent() == null ? "" : dto.getContent())
                .images(images)
                .meta(defaultSeosanMeta(dto.getMeta()))
                .build();

        var out = aiMinwonClient.compose(in);

        AiComposeResponseDto res = new AiComposeResponseDto();
        res.setTitle(out.getTitle());
        res.setDocHtml(firstNotBlank(out.getDocHtml(), null));
        res.setDocMarkdown(firstNotBlank(out.getDocMarkdown(), out.getBody()));
        res.setFields(out.getFields());
        res.setCategorySuggestions(extractCategories(out.getFields()));
        res.setAddressCandidate(textOrNull(out.getFields(), "address"));
        return res;
    }

    @Transactional
    public ComplaintResponseDto createComplaint(ComplaintRequestDto dto) {
        // 0) 최소 요건: 주소 유효 + (내용 or 이미지 중 하나는 존재)
        if (isBlank(dto.getAddress()) || !SeosanRegion.isValid(dto.getAddress())) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "지역은 서산시 읍·면·동 중에서 선택해 주세요.");
        }
        List<String> imageUrls = (dto.getImageUrls() == null) ? List.of() : dto.getImageUrls();
        boolean hasImages = !imageUrls.isEmpty();

        String docHtml = dto.getDocHtml();
        String docMd   = dto.getDocMarkdown();
        String content = dto.getContent();

        if (isBlank(content) && isBlank(docHtml) && isBlank(docMd)) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL,
                    "민원서 내용이 비어 있습니다. AI 작성 결과를 반영해 제목/본문을 채워 주세요.");
        }
        // 카테고리
        if (dto.getCategories() == null || dto.getCategories().isEmpty()) {
            try {
                var in = AiMinwonClient.AiComposeIn.builder()
                        .complaintText(firstNotBlank(
                                content,
                                docMd,
                                asPlainText(docHtml)  // HTML 본문이 왔으면 텍스트로 정리
                        ))
                        .images(imageUrls.stream().map(AiMinwonClient.ImageInput::fromUrl).toList())
                        .meta(defaultSeosanMeta(null))
                        .build();

                var out = aiMinwonClient.compose(in);

                // 제목/본문이 비어 있으면 AI 결과로 채워주기
                if (isBlank(dto.getTitle())) {
                    dto.setTitle(firstNotBlank(out.getTitle(), "민원서"));
                }
                if (isBlank(docHtml) && isBlank(docMd)) {
                    dto.setDocHtml(firstNotBlank(out.getDocHtml(), null));
                    dto.setDocMarkdown(firstNotBlank(out.getDocMarkdown(), out.getBody()));
                    docHtml  = dto.getDocHtml();
                    docMd    = dto.getDocMarkdown();
                    content  = firstNotBlank(content, out.getBody());
                }

                // 카테고리 후보 → enum 매핑
                var candidates = extractCategories(out.getFields()); // List<String>
                var enums = mapCategoryNamesToEnum(candidates);      // List<ComplaintCategory>

                // 후보가 비면 텍스트 기반 간이 추정
                if (enums.isEmpty()) {
                    var guess = toCategoryEnum(firstNotBlank(
                            content, out.getBody(), out.getTitle()
                    ));
                    if (guess != null) {
                        enums = List.of(guess);
                    }
                }

                // 마지막 보루(정말 못 찾은 경우): 기타행정으로 기본값
                if (enums.isEmpty()) {
                    enums = List.of(ComplaintCategory.OTHERS_ADMIN);
                }
                dto.setCategories(enums);

            } catch (Exception e) {
                // AI 호출 실패 시에도 서비스가 막히지 않도록 최소한의 동작
                var fallback = toCategoryEnum(firstNotBlank(content, docMd, asPlainText(docHtml)));
                dto.setCategories(fallback != null
                        ? List.of(fallback)
                        : List.of(ComplaintCategory.OTHERS_ADMIN));
                if (isBlank(dto.getTitle())) dto.setTitle("민원서");
            }
        }

        if (isBlank(dto.getTitle())) {
            dto.setTitle("민원서");
        }
        if (dto.getCategories() == null || dto.getCategories().isEmpty()) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "카테고리를 선택해 주세요.");
        }

        // 3) 저장
        Complaint entity = toEntity(dto);        // 이 메서드는 dto의 title/address/content/categories 매핑 포함
        if (hasImages) entity.setImageUrl(imageUrls.get(0));

        if (notBlank(docHtml)) {
            entity.setDocHtml(docHtml);
            entity.setComposeStatus(ComposeStatus.COMPOSED); // 또는 EDITED (아래 enum 확장 참고)
        } else if (notBlank(docMd)) {
            entity.setDocMarkdown(docMd);
            entity.setComposeStatus(ComposeStatus.COMPOSED);
        } else {
            // content 텍스트로만 등록하는 케이스
            entity.setComposeStatus(ComposeStatus.NONE);
        }

        complaintRepository.save(entity);
        if (hasImages) attachImages(entity, imageUrls);

        return toDto(entity);
    }

    // 라벨 리스트 -> enum 리스트
    private List<ComplaintCategory> mapCategoryNamesToEnum(List<String> names) {
        if (names == null || names.isEmpty()) return List.of();
        return names.stream()
                .map(this::toCategoryEnum)   // 한 개 라벨을 enum으로
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String asPlainText(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // 라벨 하나 -> enum 하나 (네 enum 값에 맞춘 매핑)
    private ComplaintCategory toCategoryEnum(String name) {
        String k = normalizeCat(name);

        // 1) enum 명칭/직역에 가까운 표현
        if (k.equals("environmentcleaning") || k.equals("환경청소")) return ComplaintCategory.ENVIRONMENT_CLEANING;
        if (k.equals("facilitydamage")      || k.equals("시설물파손") || k.equals("시설물관리")) return ComplaintCategory.FACILITY_DAMAGE;
        if (k.equals("trafficparking")      || k.equals("교통주정차")) return ComplaintCategory.TRAFFIC_PARKING;
        if (k.equals("safetyrisk")          || k.equals("안전위험"))   return ComplaintCategory.SAFETY_RISK;
        if (k.equals("livinginconvenience") || k.equals("생활불편"))   return ComplaintCategory.LIVING_INCONVENIENCE;
        if (k.equals("othersadmin")         || k.equals("기타행정")   || k.equals("행정"))     return ComplaintCategory.OTHERS_ADMIN;

        // 2) 일반적인 키워드 매핑(한국어/영어 혼합, 공백/슬래시 제거 기준)
        if (k.contains("주차") || k.contains("교통") || k.contains("parking") || k.contains("traffic") || k.contains("이중주차") || k.contains("불법주정차"))
            return ComplaintCategory.TRAFFIC_PARKING;

        if (k.contains("쓰레기") || k.contains("무단투기") || k.contains("환경") || k.contains("청소")
                || k.contains("분리수거") || k.contains("낙엽") || k.contains("폐기물")
                || k.contains("garbage") || k.contains("waste") || k.contains("litter"))
            return ComplaintCategory.ENVIRONMENT_CLEANING;

        if (k.contains("파손") || k.contains("고장") || k.contains("보수") || k.contains("보도블럭")
                || k.contains("도로파손") || k.contains("가로등") || k.contains("신호등")
                || k.contains("표지판") || k.contains("난간") || k.contains("펜스")
                || k.contains("damage") || k.contains("broken") || k.contains("repair"))
            return ComplaintCategory.FACILITY_DAMAGE;

        if (k.contains("안전") || k.contains("위험") || k.contains("침수") || k.contains("낙석")
                || k.contains("전선") || k.contains("전도") || k.contains("hazard")
                || k.contains("safety") || k.contains("risk") || k.contains("danger"))
            return ComplaintCategory.SAFETY_RISK;

        if (k.contains("생활") || k.contains("불편") || k.contains("소음")
                || k.contains("complaint") || k.contains("noise") || k.contains("odor") || k.contains("악취"))
            return ComplaintCategory.LIVING_INCONVENIENCE;

        if (k.contains("행정") || k.contains("기타") || k.contains("other") || k.contains("admin"))
            return ComplaintCategory.OTHERS_ADMIN;

        return null; // 매핑 실패 시 null (상위에서 filter)
    }

    // 라벨 정규화(공백/슬래시/하이픈 제거 + 영어 소문자화)
    private String normalizeCat(String s) {
        if (s == null) return "";
        String t = s.replaceAll("\\s+", "")  // 공백 제거
                .replace("/", "")
                .replace("-", "");
        return t.toLowerCase(java.util.Locale.ROOT);
    }


    private JsonNode toJsonNode(Object v) {
        if (v == null) return NullNode.getInstance();
        if (v instanceof JsonNode jn) return jn;
        if (v instanceof String s) {
            try { return objectMapper.readTree(s); }
            catch (Exception e) { return NullNode.getInstance(); }
        }
        return objectMapper.valueToTree(v);  // Map/POJO -> JsonNode
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private String firstNotBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) if (notBlank(v)) return v;
        return null;
    }

    // 서산 기본 메타 주입
    private Map<String, Object> defaultSeosanMeta(Map<String, Object> in) {
        Map<String, Object> meta = new HashMap<>();
        if (in != null) meta.putAll(in);
        meta.putIfAbsent("org", "서산시청");
        meta.putIfAbsent("receiver", "서산시청장 귀하");
        meta.putIfAbsent("title_prefix", "[서산시]");
        return meta;
    }

    // AI fields에서 카테고리/주소 후보 추출
    private List<String> extractCategories(JsonNode fields) {
        if (fields == null) return List.of();

        var node = fields.path("categories");
        if (node.isArray() && node.size() > 0) {
            List<String> list = new ArrayList<>();
            node.forEach(n -> { if (n.isTextual()) list.add(n.asText()); });
            if (!list.isEmpty()) return list;
        }
        node = fields.path("category_candidates");
        if (node.isArray() && node.size() > 0) {
            List<String> list = new ArrayList<>();
            node.forEach(n -> { if (n.isTextual()) list.add(n.asText()); });
            if (!list.isEmpty()) return list;
        }
        var cat = textOrNull(fields, "category");
        if (notBlank(cat)) return List.of(cat);
        cat = textOrNull(fields, "category_primary");
        if (notBlank(cat)) return List.of(cat);

        return List.of();
    }

    private String textOrNull(JsonNode node, String key) {
        if (node == null) return null;
        var v = node.path(key);
        return v.isMissingNode() || v.isNull() ? null : v.asText(null);
    }


    public List<ComplaintResponseDto> getMyComplaints(String userName, String phoneNumber) {
        return complaintRepository.findByUserNameAndPhoneNumber(userName, phoneNumber)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public ComplaintResponseDto getMyComplaint(Long id, String userName, String phoneNumber) {
        return toDto(getVerifiedComplaint(id, userName, phoneNumber));
    }

    @Transactional
    public ComplaintResponseDto updateMyComplaint(Long id, ComplaintRequestDto dto) {
        if (dto.getAddress() != null && !SeosanRegion.isValid(dto.getAddress()))
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "지역은 서산시 읍·면·동 중에서 선택해 주세요.");
        Complaint complaint = getVerifiedComplaint(id, dto.getUserName(), dto.getPhoneNumber());
        updateEntity(complaint, dto);

        if (dto.getImageUrls() != null) {
            if (!dto.getImageUrls().isEmpty()) complaint.setImageUrl(dto.getImageUrls().getFirst());
            else complaint.setImageUrl(null);
            attachImages(complaint, dto.getImageUrls());
        }
        complaintRepository.save(complaint);
        return toDto(complaint);
    }

    @Transactional
    public void deleteMyComplaint(Long id, String userName, String phoneNumber) {
        Complaint complaint = getVerifiedComplaint(id, userName, phoneNumber);
        adminCommentRepository.deleteByComplaintId(id);
        attachmentRepository.deleteByComplaintId(id);
        complaintRepository.delete(complaint);
    }

    public List<ComplaintResponseDto> getAllComplaints() {
        return complaintRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public ComplaintResponseDto updateComplaint(Long id, ComplaintRequestDto dto) {
        if (dto.getAddress() != null && !SeosanRegion.isValid(dto.getAddress()))
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "지역은 서산시 읍·면·동 중에서 선택해 주세요.");
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원 정보를 찾을 수 없습니다."));
        updateEntity(complaint, dto);

        if (dto.getImageUrls() != null) {
            if (!dto.getImageUrls().isEmpty()) complaint.setImageUrl(dto.getImageUrls().getFirst());
            else complaint.setImageUrl(null);
            attachImages(complaint, dto.getImageUrls());
        }
        complaintRepository.save(complaint);
        return toDto(complaint);
    }

    @Transactional
    public void deleteComplaint(Long id) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원을 찾을 수 없습니다."));
        adminCommentRepository.deleteByComplaintId(id);
        attachmentRepository.deleteByComplaintId(id);
        complaintRepository.delete(c);
    }

    public ComplaintResponseDto updateStatus(Long id, ComplaintStatus status) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원을 찾을 수 없습니다."));
        c.setStatus(status);
        if (status == ComplaintStatus.COMPLETED) {
            if (c.getResolvedAt() == null) c.setResolvedAt(LocalDateTime.now());
        } else {
            c.setResolvedAt(null);
        }
        complaintRepository.save(c);
        return toDto(c);
    }

    public AdminReportDto getAdminReport(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        int totalCount = (int) complaintRepository.countByCreatedAtBetween(start, end);
        int completedCount = (int) complaintRepository.countByStatusAndCreatedAtBetween(ComplaintStatus.COMPLETED, start, end);
        int processingCount = (int) complaintRepository.countByStatusAndCreatedAtBetween(ComplaintStatus.IN_PROGRESS, start, end);
        int pendingCount = (int) complaintRepository.countByStatusAndCreatedAtBetween(ComplaintStatus.PENDING, start, end);
        return AdminReportDto.builder()
                .totalCount(totalCount)
                .completedCount(completedCount)
                .processingCount(processingCount)
                .pendingCount(pendingCount)
                .build();
    }

    public List<ComplaintResponseDto> getByCategory(ComplaintCategory category) {
        return complaintRepository.findByCategoryAndStatus(category, ComplaintStatus.PENDING)
                .stream().map(this::toDto).toList();
    }

    private Complaint getVerifiedComplaint(Long id, String userName, String phoneNumber) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원 정보를 찾을 수 없습니다."));
        if (!Objects.equals(complaint.getUserName(), userName)
                || !Objects.equals(complaint.getPhoneNumber(), phoneNumber)) {
            throw new RuntimeException("본인만 접근할 수 있습니다.");
        }
        return complaint;
    }

    private void updateEntity(Complaint complaint, ComplaintRequestDto dto) {
        if (dto.getTitle() != null) complaint.setTitle(dto.getTitle());
        if (dto.getContent() != null) complaint.setContent(dto.getContent());
        if (dto.getAddress() != null) complaint.setAddress(dto.getAddress());
        if (dto.getUserName() != null) complaint.setUserName(dto.getUserName());
        if (dto.getPhoneNumber() != null) complaint.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getCategories() != null) {
            LinkedHashSet<ComplaintCategory> cats = new LinkedHashSet<>(dto.getCategories());
            if (cats.isEmpty()) throw new CustomException(ErrorCode.VALIDATION_FAIL, "카테고리는 최소 1개 이상이어야 합니다.");
            complaint.getCategories().clear();
            complaint.getCategories().addAll(cats);
        }
    }

    private Complaint toEntity(ComplaintRequestDto dto) {
        LinkedHashSet<ComplaintCategory> cats = new LinkedHashSet<>(dto.getCategories() == null ? List.of() : dto.getCategories());
        if (cats.isEmpty()) throw new CustomException(ErrorCode.VALIDATION_FAIL, "카테고리는 최소 1개 이상이어야 합니다.");
        return Complaint.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .address(dto.getAddress())
                .categories(cats)
                .imageUrl((dto.getImageUrls() != null && !dto.getImageUrls().isEmpty())
                        ? dto.getImageUrls().getFirst() : null)
                .userName(dto.getUserName())
                .phoneNumber(dto.getPhoneNumber())
                .status(ComplaintStatus.PENDING)
                .build();
    }

    private ComplaintResponseDto toDto(Complaint c) {
        Map<String, String> fields = parseSummary(c.getSummary());
        return ComplaintResponseDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .content(c.getContent())
                .address(c.getAddress())
                .categories(new ArrayList<>(c.getCategories()))
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .imageUrls(buildImageUrlsFor(c))
                .userName(c.getUserName())
                .phoneNumber(formatPhone(c.getPhoneNumber()))
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                .updatedAt(c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null)
                .rejectionReason(c.getRejectionReason() != null ? c.getRejectionReason().name() : null)
                .rejectionDetail(c.getRejectionDetail())
                .summaryLocation(fields.getOrDefault("location", ""))
                .summaryPhenomenon(fields.getOrDefault("phenomenon", ""))
                .summaryProblem(fields.getOrDefault("problem", ""))
                .summaryRisk(fields.getOrDefault("risk", ""))
                .summaryRequest(fields.getOrDefault("request", ""))
                .docHtml(c.getDocHtml())
                .docMarkdown(c.getDocMarkdown())
                .composeStatus(c.getComposeStatus() != null ? c.getComposeStatus().name() : "NONE")
                .composeError(c.getComposeError())
                .build();
    }

    public ComplaintDetailDto getComplaintDetail(Long id) {
        Complaint c = complaintRepository.findByIdAndStatus(id, ComplaintStatus.PENDING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "미처리 민원만 조회할 수 있습니다."));
        List<String> imgs = buildImageUrlsFor(c);
        List<AdminCommentDto> comments = adminCommentRepository.findByComplaintIdOrderByCreatedAtAsc(c.getId())
                .stream()
                .map(cm -> AdminCommentDto.builder()
                        .id(cm.getId()).author(cm.getAuthor()).content(cm.getContent())
                        .createdAt(cm.getCreatedAt().toString()).build())
                .toList();

        return ComplaintDetailDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .content(c.getContent())
                .address(c.getAddress())
                .categories(new ArrayList<>(c.getCategories()))
                .status(c.getStatus())
                .userName(c.getUserName())
                .phoneNumber(formatPhone(c.getPhoneNumber()))
                .imageUrls(imgs)
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                .comments(comments)
                .rejectionReason(c.getRejectionReason())
                .rejectionDetail(c.getRejectionDetail())
                .build();
    }

    public List<CategoryCountDto> getPendingCategoryCounts() {
        List<CategoryCountDto> list = new ArrayList<>();
        for (ComplaintCategory c : ComplaintCategory.values()) {
            long cnt = complaintRepository.countByCategoryAndStatus(c, ComplaintStatus.PENDING);
            list.add(CategoryCountDto.builder().category(c).count(cnt).build());
        }
        return list;
    }

    private String formatPhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 11) return digits.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
        if (digits.length() == 10) return digits.replaceFirst("(\\d{3})(\\d{3})(\\d{4})", "$1-$2-$3");
        return phone;
    }

    public AdminCommentDto addAdminComment(Long complaintId, String content) {
        if (content == null || content.isBlank())
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "코멘트를 입력해 주세요.");
        Complaint c = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("민원을 찾을 수 없습니다."));
        AdminComment saved = adminCommentRepository.save(
                AdminComment.builder()
                        .complaint(c).author("관리자").content(content)
                        .createdAt(LocalDateTime.now()).build()
        );
        return AdminCommentDto.builder()
                .id(saved.getId()).author(saved.getAuthor()).content(saved.getContent())
                .createdAt(saved.getCreatedAt().toString()).build();
    }

    public RegionPieResponse computeRegionPie(int daysOpt) {
        int days = (daysOpt <= 0) ? 30 : daysOpt;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime curFrom = now.minusDays(days);

        long curTotal = complaintRepository.countByCreatedAtBetween(curFrom, now);

        if (curTotal == 0) {
            return RegionPieResponse.builder()
                    .total(0L)
                    .slices(Collections.emptyList())
                    .build();
        }

        List<String> regions = SeosanRegion.names();

        List<RegionPieSlice> slices = regions.stream()
                .map(r -> {
                    long cnt = complaintRepository.countByAddressContainingAndCreatedAtBetween(r, curFrom, now);
                    return RegionPieSlice.builder()
                            .name(r)
                            .value(cnt)
                            .percent((cnt == 0) ? 0.0 : round1(cnt * 100.0 / curTotal))
                            .build();
                })
                .filter(s -> s.getValue() > 0)
                .sorted((a, b) -> {
                    int byCount = Long.compare(b.getValue(), a.getValue());
                    return (byCount != 0) ? byCount : a.getName().compareTo(b.getName());
                })
                .toList();

        return RegionPieResponse.builder()
                .total(curTotal)
                .slices(slices)
                .build();
    }

    public List<RegionTopDto> computeRegionTop5(int daysOpt) {
        int days = (daysOpt <= 0) ? 30 : daysOpt;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime curFrom = now.minusDays(days);
        LocalDateTime prevFrom = curFrom.minusDays(days);

        long curTotal = complaintRepository.countByCreatedAtBetween(curFrom, now);
        List<String> regions = SeosanRegion.names();

        return regions.stream()
                .map(r -> {
                    long curCnt = complaintRepository.countByAddressContainingAndCreatedAtBetween(r, curFrom, now);
                    long prevCnt = complaintRepository.countByAddressContainingAndCreatedAtBetween(r, prevFrom, curFrom);
                    double percent = (curTotal == 0) ? 0.0 : round1(curCnt * 100.0 / curTotal);
                    double deltaPct = (prevCnt == 0 && curCnt == 0) ? 0.0
                            : round1((curCnt - prevCnt) * 100.0 / Math.max(prevCnt, 1));
                    boolean up = (curCnt > prevCnt);
                    return RegionTopDto.builder()
                            .region(r).count(curCnt).percent(percent).deltaPercent(deltaPct).up(up).build();
                })
                .sorted((a, b) -> {
                    int byCount = Long.compare(b.getCount(), a.getCount());
                    return (byCount != 0) ? byCount : a.getRegion().compareTo(b.getRegion());
                })
                .limit(5)
                .toList();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    @Transactional
    public ComplaintResponseDto rejectComplaint(Long id, RejectionReason reason, String detail) {
        if (reason == RejectionReason.OTHER && (detail == null || detail.isBlank()))
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "기타 사유를 입력해 주세요.");
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원을 찾을 수 없습니다."));
        c.setStatus(ComplaintStatus.REJECTED);
        c.setRejectionReason(reason);
        c.setRejectionDetail(reason == RejectionReason.OTHER ? detail : null);
        c.setResolvedAt(null);
        complaintRepository.save(c);
        return toDto(c);
    }

    public ComplaintResponseDto getPublicComplaintAsResponse(Long id) {
        var c = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원을 찾을 수 없습니다."));
        return toDto(c);
    }

    public List<CategoryOverviewItem> computeCategoryOverview(int daysOpt, boolean includeZero) {
        int days = (daysOpt <= 0) ? 30 : daysOpt;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime curFrom = now.minusDays(days);
        LocalDateTime prevFrom = curFrom.minusDays(days);

        long total = complaintRepository.countByCreatedAtBetween(curFrom, now);
        if (total == 0) return List.of();

        List<CategoryOverviewItem> items = new ArrayList<>();
        for (ComplaintCategory cat : ComplaintCategory.values()) {
            long cur = complaintRepository.countByCategoryAndCreatedAtBetween(cat, curFrom, now);
            if (!includeZero && cur == 0) continue;

            long prev = complaintRepository.countByCategoryAndCreatedAtBetween(cat, prevFrom, curFrom);
            double percent = round1(cur * 100.0 / total);
            double deltaPct = (prev == 0 && cur == 0) ? 0.0
                    : round1((cur - prev) * 100.0 / Math.max(prev, 1));
            boolean up = deltaPct > 0;

            items.add(CategoryOverviewItem.builder()
                    .Category(cat)
                    .count(cur)
                    .percent(percent)
                    .deltaPercent(deltaPct)
                    .up(up)
                    .build());
        }

        items.sort((a, b) -> {
            int byCnt = Long.compare(b.getCount(), a.getCount());
            return (byCnt != 0) ? byCnt : a.getCategory().name().compareTo(b.getCategory().name());
        });
        return items;
    }

    public List<ComplaintResponseDto> getPublicComplaintsByCategory(ComplaintCategory category, String statusOpt, int daysOpt) {
        int days = (daysOpt <= 0) ? 30 : daysOpt;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(days);

        List<Complaint> list;
        if (statusOpt == null || statusOpt.equalsIgnoreCase("ALL")) {
            list = complaintRepository.findByCategoryAndCreatedAtBetween(category, from, now);
        } else {
            ComplaintStatus st = ComplaintStatus.valueOf(statusOpt.toUpperCase());
            list = complaintRepository.findByCategoryAndStatusAndCreatedAtBetween(category, st, from, now);
        }

        list.sort(Comparator.comparing(Complaint::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return list.stream().map(this::toDto).toList();
    }

    public List<CategoryTrendItem> getCategoryTrends(int daysOpt, boolean includeZero) {
        return computeCategoryOverview(daysOpt, includeZero).stream()
                .map(o -> CategoryTrendItem.builder()
                        .category(o.getCategory())
                        .valuePercent(o.getDeltaPercent())
                        .up(o.isUp())
                        .build())
                .toList();
    }

    public ResolutionRateDto computeResolutionRate(int daysOpt) {
        int days = (daysOpt <= 0) ? 30 : daysOpt;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime curFrom = now.minusDays(days);
        LocalDateTime prevFrom = curFrom.minusDays(days);

        long curTotal = complaintRepository.countByCreatedAtBetween(curFrom, now);
        long curDone = complaintRepository.countByStatusAndCreatedAtBetween(ComplaintStatus.COMPLETED, curFrom, now);

        long prevTotal = complaintRepository.countByCreatedAtBetween(prevFrom, curFrom);
        long prevDone = complaintRepository.countByStatusAndCreatedAtBetween(ComplaintStatus.COMPLETED, prevFrom, curFrom);

        double curRate = round1(curTotal == 0 ? 0.0 : (curDone * 100.0 / curTotal));
        double prevRate = round1(prevTotal == 0 ? 0.0 : (prevDone * 100.0 / prevTotal));
        double delta = round1(curRate - prevRate);
        boolean up = delta > 0;

        return ResolutionRateDto.builder()
                .ratePercent(curRate)
                .deltaPercent(delta)
                .up(up)
                .build();
    }

    public AvgHandleTimeDto computeAvgHandleTime(int daysOpt) {
        int days = (daysOpt <= 0) ? 30 : daysOpt;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime curFrom = now.minusDays(days);
        LocalDateTime prevFrom = curFrom.minusDays(days);

        List<Complaint> curResolved = complaintRepository.findByStatusAndResolvedAtBetween(
                ComplaintStatus.COMPLETED, curFrom, now);

        double curDays = averageDays(curResolved);

        List<Complaint> prevResolved = complaintRepository.findByStatusAndResolvedAtBetween(
                ComplaintStatus.COMPLETED, prevFrom, curFrom);

        double prevDays = averageDays(prevResolved);

        double delta = round1(curDays - prevDays);
        boolean up = delta > 0;

        return AvgHandleTimeDto.builder()
                .days(curDays)
                .deltaDays(delta)
                .up(up)
                .build();
    }

    private static double averageDays(List<Complaint> list) {
        if (list == null || list.isEmpty()) return 0.0;
        double sum = 0.0;
        int n = 0;
        for (Complaint c : list) {
            if (c.getCreatedAt() == null || c.getResolvedAt() == null) continue;
            long seconds = java.time.Duration.between(c.getCreatedAt(), c.getResolvedAt()).getSeconds();
            double days = seconds / 86400.0;
            sum += days;
            n++;
        }
        if (n == 0) return 0.0;
        return round1(sum / n);
    }

    public Page<Complaint> getTopCategoryPendingComplaints(Pageable pageable) {
        List<CategoryCount> top = complaintRepository.findTopCategoryByStatus(
                ComplaintStatus.PENDING,
                PageRequest.of(0, 1)
        );
        if (top.isEmpty()) {
            return Page.empty(pageable);
        }
        var topCategory = top.getFirst().getCategory();
        return complaintRepository.findByCategoryAndStatus(topCategory, ComplaintStatus.PENDING, pageable);
    }

    public Page<ComplaintResponseDto> getPendingListAsUnified(Pageable pageable) {
        Page<Complaint> page = complaintRepository.findByStatus(ComplaintStatus.PENDING, pageable);
        return page.map(this::toDto);
    }

    public Page<ComplaintResponseDto> getTopCategoryPendingAsUnified(Pageable pageable) {
        return getTopCategoryPendingComplaints(pageable).map(this::toDto);
    }

    private Map<String, String> parseSummary(String summaryJson) {
        if (summaryJson == null || summaryJson.isBlank()) return emptyFields();
        try {
            Map<String, String> m = objectMapper.readValue(summaryJson, new TypeReference<>() {});
            emptyFields().forEach(m::putIfAbsent);
            return m;
        } catch (Exception e) {
            return emptyFields();
        }
    }

    private Map<String, String> emptyFields() {
        Map<String, String> m = new HashMap<>();
        m.put("location", "");
        m.put("phenomenon", "");
        m.put("problem", "");
        m.put("risk", "");
        m.put("request", "");
        return m;
    }

    @Transactional
    public Map<String, String> ensureSummaryFields(Long complaintId) {
        Complaint c = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found: " + complaintId));

        String existing = c.getSummary();
        if (existing != null && !existing.isBlank()) {
            Map<String, String> parsed = parseSummary(existing);
            emptyFields().forEach(parsed::putIfAbsent);
            return parsed;
        }

        String text = c.getContent() == null ? "" : c.getContent();
        List<byte[]> imageBytes = fetchImageBytesFromFilePath(c.getId());
        List<String> imageUrls = imageBytes.isEmpty() ? buildImageUrlsFor(c) : List.of();

        SummarizerClient.FieldsResponse resp;
        try {
            if (!imageBytes.isEmpty()) {
                resp = summarizerClient
                        .summarizeFieldsMultipart(text, imageBytes)
                        .timeout(Duration.ofMillis(aiTimeoutMs))
                        .block();
            } else {
                resp = summarizerClient
                        .summarizeFieldsJson(text, imageUrls)
                        .timeout(Duration.ofMillis(aiTimeoutMs))
                        .block();
            }
        } catch (Exception e) {
            throw new CustomException(ErrorCode.AI_TIMEOUT_OR_ERROR, "AI 요청 실패: " + e.getMessage());
        }

        Map<String, String> normalized = emptyFields();
        if (resp != null) {
            normalized.put("location", tidyKo(Optional.ofNullable(resp.getLocation()).orElse("")));
            normalized.put("phenomenon", tidyKo(Optional.ofNullable(resp.getPhenomenon()).orElse("")));
            normalized.put("problem", tidyKo(Optional.ofNullable(resp.getProblem()).orElse("")));
            normalized.put("risk", tidyKo(Optional.ofNullable(resp.getRisk()).orElse("")));
            normalized.put("request", tidyRequest(Optional.ofNullable(resp.getRequest()).orElse("")));
        }

        try {
            String json = objectMapper.writeValueAsString(normalized);
            c.setSummary(json);
            complaintRepository.save(c);
        } catch (Exception e) {
            throw new RuntimeException("요약 저장 실패", e);
        }

        return normalized;
    }

    private List<byte[]> fetchImageBytesFromFilePath(Long complaintId) {
        List<byte[]> out = new ArrayList<>();
        try {
            var atts = attachmentRepository.findByComplaintIdOrderByUploadedAtAsc(complaintId);
            for (Attachment a : atts) {
                String path = a.getFilePath();
                if (path == null || path.isBlank()) continue;
                Path p = Paths.get(path);
                if (Files.exists(p) && Files.isRegularFile(p)) {
                    out.add(Files.readAllBytes(p));
                    if (out.size() >= 5) break;
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    private String toPublicUrl(String u) {
        if (u == null || u.isBlank()) return null;
        if (u.startsWith("http://") || u.startsWith("https://")) return u;
        if (u.startsWith("/")) return publicBaseUrl + u;
        return publicBaseUrl + "/" + u;
    }

    private List<String> buildImageUrlsFor(Complaint c) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        var atts = attachmentRepository.findByComplaintIdOrderByUploadedAtAsc(c.getId());
        for (var att : atts) {
            String raw = (att.getUrl() != null && !att.getUrl().isBlank()) ? att.getUrl() : att.getFilePath();
            String pub = toPublicUrl(raw);
            if (pub != null) set.add(pub);
        }
        String cover = toPublicUrl(c.getImageUrl());
        if (cover != null) set.add(cover);
        return new ArrayList<>(set);
    }

    private void attachImages(Complaint complaint, List<String> urls) {
        if (urls == null || urls.isEmpty()) return;
        List<Attachment> existing = attachmentRepository.findByUrlIn(urls);
        Set<String> existingUrls = existing.stream()
                .map(Attachment::getUrl)
                .filter(u -> u != null && !u.isBlank())
                .collect(Collectors.toSet());

        List<Attachment> toUpdateOwner = new ArrayList<>();
        for (Attachment a : existing) {
            if (a.getComplaint() == null || !Objects.equals(a.getComplaint().getId(), complaint.getId())) {
                a.setComplaint(complaint);
                toUpdateOwner.add(a);
            }
        }
        if (!toUpdateOwner.isEmpty()) attachmentRepository.saveAll(toUpdateOwner);

        List<Attachment> toCreate = new ArrayList<>();
        for (String u : urls) {
            if (u == null || u.isBlank()) continue;
            if (!existingUrls.contains(u)) {
                toCreate.add(Attachment.builder()
                        .url(u)
                        .complaint(complaint)
                        .build());
            }
        }
        if (!toCreate.isEmpty()) attachmentRepository.saveAll(toCreate);
    }

    private static String tidyKo(String s) {
        if (s == null) return "";
        String out = s.trim();
        out = out.replaceAll("[~˜]+", "");
        out = out.replaceAll("\\s+", " ");
        out = out.replaceAll("\\s*,\\s*", ", ");
        out = out.replaceAll("\\s*\\.", ".");
        out = out.replaceAll("\\s+,", ",");
        out = out.replaceAll(",\\s*,", ", ");
        out = out.replaceAll("문제이 발생했습니다", "문제가 발생했습니다");
        out = out.replaceAll("발생이 발생했습니다", "발생했습니다");
        out = out.replaceAll("파여 있음입니다", "파여 있습니다");
        out = out.replaceAll("들려 있고", "들떠 있고");
        out = out.replaceAll("있어요이", "있어요");
        out = out.replaceAll("있어요\\s*발생했습니다", "있습니다.");
        out = out.replaceAll("있어요(?![가-힣])", "있습니다");
        out = out.replaceAll("있음입니다", "있습니다");
        out = out.replaceAll("있음\\.", "있습니다.");
        out = out.replaceAll("\\b(발생했습니다)(\\s*발생했습니다)+", "$1");
        out = out.replaceAll("\\b(위험이 있습니다)(\\s*위험이 있습니다)+", "$1");
        out = out.replaceAll("(입니다|습니다|해요|에요)\\s*(입니다|습니다)", "$1");
        out = out.replaceAll("(수 있습니다)\\s*위험이 있습니다\\.?$", "$1.");
        out = out.replaceAll("(수도 있습니다)\\s*위험이 있습니다\\.?$", "$1.");
        out = out.replaceAll("(위험합니다)\\s*위험이 있습니다\\.?$", "$1.");
        if (!out.isEmpty() && !out.matches(".*[.?!]$")) out = out + ".";
        out = out.replaceAll("\\.(\\s*\\.)+$", ".");
        return out.trim();
    }

    private static String tidyRequest(String s) {
        String out = tidyKo(s);
        out = out.replaceAll("부탁드립니다이", "부탁드립니다");
        out = out.replaceAll("요청이 필요합니다\\.?$", "요청합니다.");
        out = out.replaceAll("\\b부탁드립니다\\s*요청합니다\\.?$", "부탁드립니다.");
        out = out.replaceAll("\\b요청합니다\\s*부탁드립니다\\.?$", "요청합니다.");
        out = out.replaceAll("\\b(부탁드립니다|요청합니다)\\b(\\s*\\1\\b)+\\.?$", "$1.");
        if (out.matches(".*필요합니다\\.?$")) {
            out = out.replaceAll("필요합니다\\.?$", "요청합니다.");
        }
        if (!out.endsWith("요청합니다.") && !out.endsWith("부탁드립니다.")) {
            out = out.replaceAll("\\.*$", "") + " 요청합니다.";
        }
        return out;
    }
}