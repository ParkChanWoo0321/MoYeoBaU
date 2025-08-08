package com.example.seosancomplain.domain.complaint;

import com.example.seosancomplain.domain.admin.AdminReportDto;
import com.example.seosancomplain.domain.dashboard.DashboardResponseDto;
import com.example.seosancomplain.domain.region.RegionPriorityDto;
import com.example.seosancomplain.domain.region.RegionReportDto;
import com.example.seosancomplain.domain.region.RegionStatDto;
import com.example.seosancomplain.domain.region.SeosanRegion;
import com.example.seosancomplain.dto.*;
import com.example.seosancomplain.exception.CustomException;
import com.example.seosancomplain.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;

    // 1. (익명) 민원 등록
    public ComplaintResponseDto createComplaint(ComplaintRequestDto dto) {
        if (dto.getLatitude() == null || dto.getLongitude() == null) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "위치 정보(위도/경도)는 필수입니다.");
        }
        if (!SeosanRegion.isValid(dto.getAddress())) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "지역은 서산시 읍·면·동 중에서 선택해 주세요.");
        }
        Complaint complaint = toEntity(dto);
        complaintRepository.save(complaint);
        return toDto(complaint);
    }

    // 2. 내 민원 전체 조회 (본인확인)
    public List<ComplaintResponseDto> getMyComplaints(String userName, String phoneNumber) {
        return complaintRepository.findByUserNameAndPhoneNumber(userName, phoneNumber)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // 3. 내 민원 상세 조회 (본인확인)
    public ComplaintResponseDto getMyComplaint(Long id, String userName, String phoneNumber) {
        return toDto(getVerifiedComplaint(id, userName, phoneNumber));
    }

    // 4. 내 민원 수정 (본인확인)
    public ComplaintResponseDto updateMyComplaint(Long id, ComplaintRequestDto dto) {
        // 주소를 변경하려는 경우에만 검증
        if (dto.getAddress() != null && !SeosanRegion.isValid(dto.getAddress())) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "지역은 서산시 읍·면·동 중에서 선택해 주세요.");
        }
        Complaint complaint = getVerifiedComplaint(id, dto.getUserName(), dto.getPhoneNumber());
        updateEntity(complaint, dto);
        complaintRepository.save(complaint);
        return toDto(complaint);
    }

    // 5. 내 민원 삭제 (본인확인)
    public void deleteMyComplaint(Long id, String userName, String phoneNumber) {
        Complaint complaint = getVerifiedComplaint(id, userName, phoneNumber);
        complaintRepository.delete(complaint);
    }

    // 6. (관리자/공용) 전체 민원 목록
    public List<ComplaintResponseDto> getAllComplaints() {
        return complaintRepository.findAll()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // 7. (관리자) 민원 수정(본인확인 없이)
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

    // 8. (관리자) 민원 삭제(본인확인 없이)
    public void deleteComplaint(Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원 정보를 찾을 수 없습니다."));
        complaintRepository.delete(complaint);
    }

    // 9. (관리자) 민원 상태변경
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

    // 10. 대시보드 (디자인 스펙 반영)
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

        // ✅ 지역 통계: Enum 기반으로 안전하게 집계
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

    // ---- 헬퍼 ----
    private static Double pctDelta(long prev, long cur) {
        if (prev <= 0) return cur > 0 ? 100.0 : 0.0;
        return round1((cur - prev) * 100.0 / prev);
    }
    private static double round1(double v){ return Math.round(v * 10.0) / 10.0; }

    /** 기간 내 완료 건들의 평균 처리시간(일) */
    private double avgResolutionDays(LocalDateTime from, LocalDateTime to) {
        return complaintRepository.findAll().stream()
                .filter(c -> c.getStatus() == ComplaintStatus.COMPLETED)
                .filter(c -> c.getResolvedAt() != null && c.getCreatedAt() != null)
                .filter(c -> !c.getCreatedAt().isAfter(to) && !c.getResolvedAt().isBefore(from))
                .mapToDouble(c -> java.time.Duration.between(c.getCreatedAt(), c.getResolvedAt()).toHours() / 24.0)
                .average().orElse(0.0);
    }

    // --- 리포트/지도/카테고리 등 기존 메서드 ---
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

    public List<MapPointDto> getComplaintMapPoints() {
        List<Complaint> complaints = complaintRepository.findAll();
        return complaints.stream()
                .map(c -> new MapPointDto(c.getLatitude(), c.getLongitude(), c.getCategory(), c.getStatus()))
                .collect(Collectors.toList());
    }

    public List<RegionReportDto> getRegionReport(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        List<Object[]> results = complaintRepository.countByAddressAndStatusBetween(start, end);
        List<RegionReportDto> list = new ArrayList<>();
        for (Object[] row : results) {
            String address = (String) row[0];
            ComplaintStatus status = (ComplaintStatus) row[1];
            Long count = (Long) row[2];
            list.add(new RegionReportDto(address, status, count));
        }
        return list;
    }

    public List<RegionPriorityDto> getPriorityRegions() {
        List<Object[]> results = complaintRepository.topAddressByStatus(ComplaintStatus.PENDING);
        List<RegionPriorityDto> list = new ArrayList<>();
        for (Object[] row : results) {
            String address = (String) row[0];
            Long count = (Long) row[1];
            list.add(new RegionPriorityDto(address, count));
        }
        return list;
    }

    public List<ComplaintResponseDto> getByCategory(ComplaintCategory category) {
        return complaintRepository.findByCategory(category)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // 본인확인(중복 로직 통합)
    private Complaint getVerifiedComplaint(Long id, String userName, String phoneNumber) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원 정보를 찾을 수 없습니다."));
        if (!complaint.getUserName().equals(userName) || !complaint.getPhoneNumber().equals(phoneNumber)) {
            throw new RuntimeException("본인만 접근할 수 있습니다.");
        }
        return complaint;
    }

    // Entity 업데이트 (수정시)
    private void updateEntity(Complaint complaint, ComplaintRequestDto dto) {
        if (dto.getContent() != null)    complaint.setContent(dto.getContent());
        if (dto.getCategory() != null)   complaint.setCategory(dto.getCategory());
        if (dto.getAddress() != null)    complaint.setAddress(dto.getAddress());
        if (dto.getLatitude() != null)   complaint.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null)  complaint.setLongitude(dto.getLongitude());
        if (dto.getImageUrl() != null)   complaint.setImageUrl(dto.getImageUrl());
    }

    // DTO → Entity 변환 (등록시)
    private Complaint toEntity(ComplaintRequestDto dto) {
        return Complaint.builder()
                .content(dto.getContent())
                .address(dto.getAddress())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .category(dto.getCategory())
                .imageUrl(dto.getImageUrl())
                .userName(dto.getUserName())
                .phoneNumber(dto.getPhoneNumber())
                .status(ComplaintStatus.PENDING)
                .build();
    }

    // Entity → DTO 변환 (응답)
    private ComplaintResponseDto toDto(Complaint c) {
        return ComplaintResponseDto.builder()
                .id(c.getId())
                .content(c.getContent())
                .address(c.getAddress())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .category(c.getCategory())
                .status(c.getStatus())
                .imageUrl(c.getImageUrl())
                .userName(c.getUserName())
                .phoneNumber(c.getPhoneNumber())
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                .updatedAt(c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null)
                .build();
    }

    public List<CategoryStatDto> getCategoryStatsByStatus(ComplaintStatus status) {
        List<Object[]> rows = complaintRepository.countByCategoryAndStatus(status);
        List<CategoryStatDto> list = new ArrayList<>();
        for (Object[] r : rows) {
            list.add(CategoryStatDto.builder()
                    .category((ComplaintCategory) r[0])
                    .count((Long) r[1])
                    .build());
        }
        return list;
    }

    public List<ComplaintMiniDto> getPriorityComplaints(ComplaintStatus status, int limit) {
        var page = complaintRepository.findByStatus(
                status,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return page.stream()
                .map(c -> ComplaintMiniDto.builder()
                        .id(c.getId())
                        .content(c.getContent())
                        .address(c.getAddress())
                        .status(c.getStatus())
                        .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                        .build())
                .toList();
    }

    public Page<ComplaintResponseDto> getAllPaged(PageRequest pageable) {
        return complaintRepository.findAll(pageable).map(this::toDto);
    }
}
