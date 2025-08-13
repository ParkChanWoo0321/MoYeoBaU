package com.example.seosancomplain.domain.complaint;

import com.example.seosancomplain.domain.admin.comment.*;
import com.example.seosancomplain.domain.admin.dto.AdminReportDto;
import com.example.seosancomplain.domain.admin.dto.CategoryCountDto;
import com.example.seosancomplain.dto.ComplaintMiniDto;
import com.example.seosancomplain.domain.admin.dto.DashboardResponseDto;
import com.example.seosancomplain.domain.region.RegionStatDto;
import com.example.seosancomplain.domain.region.SeosanRegion;
import com.example.seosancomplain.dto.ComplaintDetailDto;
import com.example.seosancomplain.dto.ComplaintRequestDto;
import com.example.seosancomplain.dto.ComplaintResponseDto;
import com.example.seosancomplain.exception.CustomException;
import com.example.seosancomplain.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
    private final com.example.seosancomplain.domain.ai.SummarizerClient summarizerClient;
    private final AdminCommentRepository adminCommentRepository;
    private final com.example.seosancomplain.domain.attachment.AttachmentRepository attachmentRepository;

    public ComplaintResponseDto createComplaint(ComplaintRequestDto dto) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "제목을 입력해 주세요.");
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "민원 내용을 입력해 주세요.");
        }
        if (dto.getCategory() == null) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "카테고리를 선택해 주세요.");
        }
        if (dto.getAddress() == null || dto.getAddress().isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "주소(읍·면·동)를 입력해 주세요.");
        }
        if (!SeosanRegion.isValid(dto.getAddress())) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "지역은 서산시 읍·면·동 중에서 선택해 주세요.");
        }

        Complaint complaint = toEntity(dto);
        complaintRepository.save(complaint);
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
        if (dto.getAddress() != null && !SeosanRegion.isValid(dto.getAddress())) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "지역은 서산시 읍·면·동 중에서 선택해 주세요.");
        }
        Complaint complaint = getVerifiedComplaint(id, dto.getUserName(), dto.getPhoneNumber());
        updateEntity(complaint, dto);
        complaintRepository.save(complaint);
        return toDto(complaint);
    }

    public void deleteMyComplaint(Long id, String userName, String phoneNumber) {
        Complaint complaint = getVerifiedComplaint(id, userName, phoneNumber);
        complaintRepository.delete(complaint);
    }

    public List<ComplaintResponseDto> getAllComplaints() {
        return complaintRepository.findAll()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public ComplaintResponseDto updateComplaint(Long id, ComplaintRequestDto dto) {
        if (dto.getAddress() != null && !SeosanRegion.isValid(dto.getAddress())) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "지역은 서산시 읍·면·동 중에서 선택해 주세요.");
        }
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원 정보를 찾을 수 없습니다."));
        updateEntity(complaint, dto);
        complaintRepository.save(complaint);
        return toDto(complaint);
    }

    @Transactional
    public void deleteComplaint(Long id) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원 정보를 찾을 수 없습니다."));

        adminCommentRepository.deleteByComplaintId(id);   // 자식 1
        attachmentRepository.deleteByComplaintId(id);     // 자식 2 (있다면)
        // 다른 자식 테이블들 있으면 위에 추가

        complaintRepository.delete(c);                    // 마지막에 부모 삭제
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

    public DashboardResponseDto getDashboardStats(Integer daysOpt) {
        int days = (daysOpt == null || daysOpt <= 0) ? 30 : daysOpt;

        LocalDateTime now      = LocalDateTime.now();
        LocalDateTime curFrom  = now.minusDays(days);
        LocalDateTime prevFrom = now.minusDays(days * 2L);
        LocalDateTime prevTo   = curFrom;

        long total     = complaintRepository.count();
        long completed = complaintRepository.countByStatus(ComplaintStatus.COMPLETED);
        double completedRate = total > 0 ? round1(completed * 100.0 / total) : 0.0;

        Map<ComplaintCategory, Long> categoryCounts =
                Arrays.stream(ComplaintCategory.values())
                        .collect(Collectors.toMap(c -> c, complaintRepository::countByCategory));

        Map<ComplaintCategory, Double> categoryRates =
                categoryCounts.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> total > 0 ? round1(e.getValue() * 100.0 / total) : 0.0
                ));

        List<ComplaintMiniDto> latestFive =
                complaintRepository.findTop5ByOrderByCreatedAtDesc().stream()
                        .map(c -> ComplaintMiniDto.builder()
                                .id(c.getId())
                                .content(c.getContent())
                                .address(c.getAddress())
                                .status(c.getStatus())
                                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                                .build())
                        .collect(Collectors.toList());

        long prevTotal = complaintRepository.countByCreatedAtBetween(prevFrom, prevTo);
        long curTotal  = complaintRepository.countByCreatedAtBetween(curFrom, now);
        Double totalCountDelta = pctDelta(prevTotal, curTotal);

        double prevCompletedRate = 0.0;
        if (prevTotal > 0) {
            long prevCompleted = complaintRepository
                    .countByStatusAndCreatedAtBetween(ComplaintStatus.COMPLETED, prevFrom, prevTo);
            prevCompletedRate = round1(prevCompleted * 100.0 / prevTotal);
        }
        double curCompletedRate = 0.0;
        if (curTotal > 0) {
            long curCompleted = complaintRepository
                    .countByStatusAndCreatedAtBetween(ComplaintStatus.COMPLETED, curFrom, now);
            curCompletedRate = round1(curCompleted * 100.0 / curTotal);
        }
        Double completedRateDelta = round1(curCompletedRate - prevCompletedRate);

        double averageResolutionDays = round1(avgResolutionDays(curFrom, now));
        double averageResolutionPrev = round1(avgResolutionDays(prevFrom, prevTo));
        Double averageResolutionDelta = round1(averageResolutionDays - averageResolutionPrev);

        List<String> regions = SeosanRegion.names();
        List<Complaint> all = complaintRepository.findAll();
        List<RegionStatDto> regionStats = regions.stream()
                .map(r -> {
                    long cnt = all.stream()
                            .filter(c -> c.getAddress() != null && c.getAddress().contains(r))
                            .count();
                    return RegionStatDto.builder().region(r).count(cnt).build();
                })
                .collect(Collectors.toList());

        return DashboardResponseDto.builder()
                .totalCount(total)
                .totalCountDelta(totalCountDelta)
                .completedRate(completedRate)
                .completedRateDelta(completedRateDelta)
                .averageResolutionDays(averageResolutionDays)
                .averageResolutionDelta(averageResolutionDelta)
                .categoryCounts(categoryCounts)
                .categoryRates(categoryRates)
                .latestFive(latestFive)
                .regionStats(regionStats)
                .build();
    }

    private static Double pctDelta(long prev, long cur) {
        if (prev <= 0) return cur > 0 ? 100.0 : 0.0;
        return round1((cur - prev) * 100.0 / prev);
    }
    private static double round1(double v){ return Math.round(v * 10.0) / 10.0; }

    private double avgResolutionDays(LocalDateTime from, LocalDateTime to) {
        return complaintRepository.findAll().stream()
                .filter(c -> c.getStatus() == ComplaintStatus.COMPLETED)
                .filter(c -> c.getResolvedAt() != null && c.getCreatedAt() != null)
                .filter(c -> !c.getCreatedAt().isAfter(to) && !c.getResolvedAt().isBefore(from))
                .mapToDouble(c -> java.time.Duration.between(c.getCreatedAt(), c.getResolvedAt()).toHours() / 24.0)
                .average().orElse(0.0);
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
        return complaintRepository
                .findByCategoryAndStatus(category, ComplaintStatus.PENDING)
                .stream().map(this::toDto).toList();
    }

    private Complaint getVerifiedComplaint(Long id, String userName, String phoneNumber) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원 정보를 찾을 수 없습니다."));
        if (!complaint.getUserName().equals(userName) || !complaint.getPhoneNumber().equals(phoneNumber)) {
            throw new RuntimeException("본인만 접근할 수 있습니다.");
        }
        return complaint;
    }

    private void updateEntity(Complaint complaint, ComplaintRequestDto dto) {
        if (dto.getTitle() != null)      complaint.setTitle(dto.getTitle());
        if (dto.getContent() != null)    complaint.setContent(dto.getContent());
        if (dto.getCategory() != null)   complaint.setCategory(dto.getCategory());
        if (dto.getAddress() != null)    complaint.setAddress(dto.getAddress());
        if (dto.getImageUrl() != null)   complaint.setImageUrl(dto.getImageUrl());
        if (dto.getUserName() != null)   complaint.setUserName(dto.getUserName());
        if (dto.getPhoneNumber() != null) complaint.setPhoneNumber(dto.getPhoneNumber());
    }

    private Complaint toEntity(ComplaintRequestDto dto) {
        return Complaint.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .address(dto.getAddress())
                .category(dto.getCategory())
                .imageUrl(dto.getImageUrl())
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

        if (c.getSummary() != null && !c.getSummary().isBlank()) {
            return Mono.just(c.getSummary());
        }

        String text = c.getContent();
        if (text == null || text.isBlank()) {
            return Mono.error(new IllegalStateException("민원 내용이 비어있습니다."));
        }

        var refs = buildImageUrlsFor(c);
        var httpUrls = refs.stream().filter(u -> u.startsWith("http://") || u.startsWith("https://")).toList();
        var local    = refs.stream().filter(u -> !(u.startsWith("http://") || u.startsWith("https://"))).toList();

        if (!local.isEmpty()) {
            return Mono.fromCallable(() -> {
                        var bytes = new java.util.ArrayList<byte[]>();
                        for (var p : local) {
                            try { bytes.add(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(p))); } catch (Exception ignored) {}
                        }
                        return bytes;
                    })
                    .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                    .flatMap((java.util.List<byte[]> images) -> summarizerClient.summarizeMultipart(text, images))
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
        var c = complaintRepository.findByIdAndStatus(id, ComplaintStatus.PENDING)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "미처리 민원만 조회할 수 있습니다.")
                );

        List<String> imgs = buildImageUrlsFor(c);

        var comments = adminCommentRepository.findByComplaintIdOrderByCreatedAtAsc(c.getId())
                .stream()
                .map(cm -> AdminCommentDto.builder()
                        .id(cm.getId())
                        .author(cm.getAuthor())
                        .content(cm.getContent())
                        .createdAt(cm.getCreatedAt().toString())
                        .build())
                .toList();

        String maskedPhone = maskPhone(c.getPhoneNumber());

        return ComplaintDetailDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .content(c.getContent())
                .address(c.getAddress())
                .category(c.getCategory())
                .status(c.getStatus())
                .userName(c.getUserName())
                .phoneNumberMasked(maskedPhone)
                .imageUrls(imgs)
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                .comments(comments)
                .rejectionReason(c.getRejectionReason())
                .rejectionDetail(c.getRejectionDetail())
                .build();
    }

    public ComplaintDetailDto getPublicComplaintDetail(Long id) {
        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원을 찾을 수 없습니다."));

        List<String> imgs = buildImageUrlsFor(c);

        var comments = adminCommentRepository.findByComplaintIdOrderByCreatedAtAsc(c.getId())
                .stream()
                .map(cm -> AdminCommentDto.builder()
                        .id(cm.getId())
                        .author(cm.getAuthor())
                        .content(cm.getContent())
                        .createdAt(cm.getCreatedAt().toString())
                        .build())
                .toList();

        return ComplaintDetailDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .content(c.getContent())
                .address(c.getAddress())
                .category(c.getCategory())
                .status(c.getStatus())
                .userName(null)
                .phoneNumberMasked(null)
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
            list.add(CategoryCountDto.builder()
                    .category(c)
                    .count(cnt)
                    .build());
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
        if (content == null || content.isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "코멘트를 입력해 주세요.");
        }

        Complaint c = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("민원을 찾을 수 없습니다."));

        AdminComment saved = adminCommentRepository.save(
                AdminComment.builder()
                        .complaint(c)
                        .author("관리자")
                        .content(content)
                        .createdAt(java.time.LocalDateTime.now())
                        .build()
        );

        return AdminCommentDto.builder()
                .id(saved.getId())
                .author(saved.getAuthor())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt().toString())
                .build();
    }

    public ComplaintResponseDto rejectComplaint(Long id,
                                                RejectionReason reason,
                                                String detail) {
        if (reason == RejectionReason.OTHER &&
                (detail == null || detail.isBlank())) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "기타 사유를 입력해 주세요.");
        }

        Complaint c = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원을 찾을 수 없습니다."));

        c.setStatus(ComplaintStatus.REJECTED);
        c.setRejectionReason(reason);
        c.setRejectionDetail(reason == RejectionReason.OTHER ? detail : null);
        c.setResolvedAt(null);
        complaintRepository.save(c);

        return toDto(c);
    }
}
