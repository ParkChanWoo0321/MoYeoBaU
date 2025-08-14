package com.example.seosancomplain.domain.complaint;

import com.example.seosancomplain.dashboard.*;
import com.example.seosancomplain.domain.admin.CategoryCount;
import com.example.seosancomplain.domain.admin.comment.AdminComment;
import com.example.seosancomplain.domain.admin.comment.AdminCommentDto;
import com.example.seosancomplain.domain.admin.comment.AdminCommentRepository;
import com.example.seosancomplain.domain.admin.dto.AdminReportDto;
import com.example.seosancomplain.domain.admin.dto.CategoryCountDto;
import com.example.seosancomplain.domain.ai.SummarizerClient;
import com.example.seosancomplain.domain.attachment.Attachment;
import com.example.seosancomplain.domain.attachment.AttachmentRepository;
import com.example.seosancomplain.domain.region.SeosanRegion;
import com.example.seosancomplain.dto.ComplaintDetailDto;
import com.example.seosancomplain.dto.ComplaintRequestDto;
import com.example.seosancomplain.dto.ComplaintResponseDto;
import com.example.seosancomplain.exception.CustomException;
import com.example.seosancomplain.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final SummarizerClient summarizerClient;
    private final AdminCommentRepository adminCommentRepository;
    private final AttachmentRepository attachmentRepository;

    public ComplaintResponseDto createComplaint(ComplaintRequestDto dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank())
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "제목을 입력해 주세요.");
        if (dto.getContent() == null || dto.getContent().isBlank())
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "민원 내용을 입력해 주세요.");
        if (dto.getCategory() == null)
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "카테고리를 선택해 주세요.");
        if (dto.getAddress() == null || dto.getAddress().isBlank())
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "주소(읍·면·동)를 입력해 주세요.");
        if (!SeosanRegion.isValid(dto.getAddress()))
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "지역은 서산시 읍·면·동 중에서 선택해 주세요.");

        Complaint complaint = toEntity(dto);
        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty())
            complaint.setImageUrl(dto.getImageUrls().getFirst());
        complaintRepository.save(complaint);

        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            List<Attachment> atts = attachmentRepository.findByUrlIn(dto.getImageUrls());
            for (Attachment a : atts) a.setComplaint(complaint);
            attachmentRepository.saveAll(atts);
        }
        return toDto(complaint);
    }

    public List<ComplaintResponseDto> getMyComplaints(String userName, String phoneNumber) {
        return complaintRepository.findByUserNameAndPhoneNumber(userName, phoneNumber)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public ComplaintResponseDto getMyComplaint(Long id, String userName, String phoneNumber) {
        return toDto(getVerifiedComplaint(id, userName, phoneNumber));
    }

    public ComplaintResponseDto updateMyComplaint(Long id, ComplaintRequestDto dto) {
        if (dto.getAddress() != null && !SeosanRegion.isValid(dto.getAddress()))
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "지역은 서산시 읍·면·동 중에서 선택해 주세요.");
        Complaint complaint = getVerifiedComplaint(id, dto.getUserName(), dto.getPhoneNumber());
        updateEntity(complaint, dto);
        complaintRepository.save(complaint);

        if (dto.getImageUrls() != null) {
            if (!dto.getImageUrls().isEmpty()) complaint.setImageUrl(dto.getImageUrls().getFirst());
            else complaint.setImageUrl(null);
            List<Attachment> atts = attachmentRepository.findByUrlIn(dto.getImageUrls());
            for (Attachment a : atts) a.setComplaint(complaint);
            attachmentRepository.saveAll(atts);
            complaintRepository.save(complaint);
        }
        return toDto(complaint);
    }

    public void deleteMyComplaint(Long id, String userName, String phoneNumber) {
        Complaint complaint = getVerifiedComplaint(id, userName, phoneNumber);
        complaintRepository.delete(complaint);
    }

    public List<ComplaintResponseDto> getAllComplaints() {
        return complaintRepository.findAll().stream().map(this::toDto).toList();
    }

    public ComplaintResponseDto updateComplaint(Long id, ComplaintRequestDto dto) {
        if (dto.getAddress() != null && !SeosanRegion.isValid(dto.getAddress()))
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "지역은 서산시 읍·면·동 중에서 선택해 주세요.");
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원 정보를 찾을 수 없습니다."));
        updateEntity(complaint, dto);

        if (dto.getImageUrls() != null) {
            if (!dto.getImageUrls().isEmpty()) complaint.setImageUrl(dto.getImageUrls().getFirst());
            else complaint.setImageUrl(null);
            List<Attachment> atts = attachmentRepository.findByUrlIn(dto.getImageUrls());
            for (Attachment a : atts) a.setComplaint(complaint);
            attachmentRepository.saveAll(atts);
        }
        complaintRepository.save(complaint);
        return toDto(complaint);
    }

    @Transactional
    public void deleteComplaint(Long id) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원 정보를 찾을 수 없습니다."));
        adminCommentRepository.deleteByComplaintId(id);
        attachmentRepository.deleteByComplaintId(id);
        complaintRepository.delete(c);
    }

    public ComplaintResponseDto updateStatus(Long id, ComplaintStatus status) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원 정보를 찾을 수 없습니다."));
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
        if (dto.getCategory() != null) complaint.setCategory(dto.getCategory());
        if (dto.getAddress() != null) complaint.setAddress(dto.getAddress());
        if (dto.getUserName() != null) complaint.setUserName(dto.getUserName());
        if (dto.getPhoneNumber() != null) complaint.setPhoneNumber(dto.getPhoneNumber());
    }

    private Complaint toEntity(ComplaintRequestDto dto) {
        return Complaint.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .address(dto.getAddress())
                .category(dto.getCategory())
                .imageUrl((dto.getImageUrls() != null && !dto.getImageUrls().isEmpty())
                        ? dto.getImageUrls().getFirst() : null)
                .userName(dto.getUserName())
                .phoneNumber(dto.getPhoneNumber())
                .status(ComplaintStatus.PENDING)
                .build();
    }

    private ComplaintResponseDto toDto(Complaint c) {
        return ComplaintResponseDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .content(c.getContent())
                .address(c.getAddress())
                .category(c.getCategory())
                .status(c.getStatus())
                .userName(c.getUserName())
                .phoneNumber(c.getPhoneNumber())
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                .updatedAt(c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null)
                .imageUrls(buildImageUrlsFor(c))
                .rejectionReason(c.getRejectionReason())
                .rejectionDetail(c.getRejectionDetail())
                .build();
    }

    @Transactional
    public Mono<String> summarizeAndSave(Long complaintId) {
        Complaint c = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found: " + complaintId));

        if (c.getSummary() != null && !c.getSummary().isBlank())
            return Mono.just(c.getSummary());

        String text = c.getContent();
        if (text == null || text.isBlank())
            return Mono.error(new IllegalStateException("민원 내용이 비어있습니다."));

        List<String> refs = buildImageUrlsFor(c);
        List<String> httpUrls = refs.stream().filter(u -> u.startsWith("http://") || u.startsWith("https://")).toList();
        List<String> local = refs.stream().filter(u -> !(u.startsWith("http://") || u.startsWith("https://"))).toList();

        if (!local.isEmpty()) {
            return Mono.fromCallable(() -> {
                        List<byte[]> bytes = new ArrayList<>();
                        for (String p : local) {
                            try {
                                bytes.add(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(p)));
                            } catch (Exception ignored) {
                            }
                        }
                        return bytes;
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(images -> summarizerClient.summarizeMultipart(text, images))
                    .map(res -> {
                        c.setSummary(res.getSummary());
                        complaintRepository.save(c);
                        return res.getSummary();
                    });
        }

        return summarizerClient.summarizeJson(text, httpUrls)
                .publishOn(Schedulers.boundedElastic())
                .map(res -> {
                    c.setSummary(res.getSummary());
                    complaintRepository.save(c);
                    return res.getSummary();
                });
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
        String maskedPhone = maskPhone(c.getPhoneNumber());
        return ComplaintDetailDto.builder()
                .id(c.getId()).title(c.getTitle()).content(c.getContent())
                .address(c.getAddress()).category(c.getCategory()).status(c.getStatus())
                .userName(c.getUserName()).phoneNumberMasked(maskedPhone)
                .imageUrls(imgs).createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
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

    private List<String> buildImageUrlsFor(Complaint c) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        var atts = attachmentRepository.findByComplaintIdOrderByUploadedAtAsc(c.getId());
        for (var att : atts) {
            String u = (att.getUrl() != null && !att.getUrl().isBlank()) ? att.getUrl() : att.getFilePath();
            if (u != null && !u.isBlank()) set.add(u);
        }
        if (c.getImageUrl() != null && !c.getImageUrl().isBlank()) set.add(c.getImageUrl());
        return new ArrayList<>(set);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String digits = phone.replaceAll("\\D", "");
        int n = digits.length();
        if (n >= 7) {
            String head = digits.substring(0, 3);
            String tail = digits.substring(n - 4);
            return head + "-****-" + tail;
        }
        return "***";
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

// --------------------- 여기부터 변경 ---------------------

    /**
     * 파이차트용 지역 비율
     * - 기간 내 총합(total) 기준 퍼센트 계산
     * - 글이 없는 지역은 제외 (0건 필터링)
     * - 제한 없이 "존재하는 지역 모두" 반환
     */
    public RegionPieResponse computeRegionPie(int daysOpt) {
        int days = (daysOpt <= 0) ? 30 : daysOpt;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime curFrom = now.minusDays(days);

        long curTotal = complaintRepository.countByCreatedAtBetween(curFrom, now);

        // 전체가 0이면 빈 배열 반환
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
                // ✅ 0건 지역 제외
                .filter(s -> s.getValue() > 0)
                // 보기 좋게 개수 내림차순 정렬 (동률이면 이름 오름차순)
                .sorted((a, b) -> {
                    int byCount = Long.compare(b.getValue(), a.getValue());
                    return (byCount != 0) ? byCount : a.getName().compareTo(b.getName());
                })
                // ✅ 제한 없음: 존재하는 지역 모두 반환
                .toList();

        return RegionPieResponse.builder()
                .total(curTotal)
                .slices(slices)
                .build();
    }

    /**
     * 지역 Top5 (증감률 포함)
     * - 현재 기간 내 0건 지역은 제외
     * - 상위 5개만 반환
     */
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
                    boolean up = (deltaPct > 0);
                    return RegionTopDto.builder()
                            .region(r).count(curCnt).percent(percent).deltaPercent(deltaPct).up(up).build();
                })
                // ✅ 현재 기간에 0건인 지역 제외
                .filter(dto -> dto.getCount() > 0)
                // 개수 내림차순 → 이름 오름차순
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

// --------------------- 여기까지 변경 ---------------------

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
        return toDto(c); // 기존 목록/내 글 상세와 동일 구조
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
            if (!includeZero && cur == 0) continue; // includeZero가 false면 0건 카테고리 제외

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


    /**
     * 일반 사용자용: 카테고리별 글 목록 (status: ALL/PENDING/IN_PROGRESS/COMPLETED, 기본 30일)
     */
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

        // 최신순
        list.sort(Comparator.comparing(Complaint::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return list.stream().map(this::toDto).toList();
    }

    public List<CategoryTrendItem> getCategoryTrends(int daysOpt, boolean includeZero) {
        return computeCategoryOverview(daysOpt, includeZero).stream()
                .map(o -> CategoryTrendItem.builder()
                        .category(o.getCategory())
                        .valuePercent(o.getDeltaPercent()) // 디자인은 증감률만 표시
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

        double curRate = (curTotal == 0) ? 0.0 : round1(curDone * 100.0 / curTotal);
        double prevRate = (prevTotal == 0) ? 0.0 : round1(prevDone * 100.0 / prevTotal);

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

        // 금기간: '해결된 시점'이 금기간인 완료 건들만 대상으로 평균 처리시간 계산
        List<Complaint> curResolved = complaintRepository.findByStatusAndResolvedAtBetween(
                ComplaintStatus.COMPLETED, curFrom, now);

        double curDays = averageDays(curResolved);

        // 전기간
        List<Complaint> prevResolved = complaintRepository.findByStatusAndResolvedAtBetween(
                ComplaintStatus.COMPLETED, prevFrom, curFrom);

        double prevDays = averageDays(prevResolved);

        double delta = round1(curDays - prevDays); // 감소(음수)면 성능 개선
        boolean up = delta > 0;

        return AvgHandleTimeDto.builder()
                .days(curDays)
                .deltaDays(delta)
                .up(up)
                .build();
    }

    // 유틸
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

    public Page<Complaint> getPendingAll(Pageable pageable) {
        return complaintRepository.findByStatus(ComplaintStatus.PENDING, pageable);
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
}
