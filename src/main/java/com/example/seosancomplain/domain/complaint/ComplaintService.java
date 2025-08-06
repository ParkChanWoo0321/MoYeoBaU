package com.example.seosancomplain.domain.complaint;

import com.example.seosancomplain.domain.dashboard.DashboardResponseDto;
import com.example.seosancomplain.dto.AdminReportDto;
import com.example.seosancomplain.dto.ComplaintRequestDto;
import com.example.seosancomplain.dto.ComplaintResponseDto;
import com.example.seosancomplain.dto.MapPointDto;
import com.example.seosancomplain.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.example.seosancomplain.exception.ErrorCode;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;

    // 1. (익명) 민원 등록
    public ComplaintResponseDto createComplaint(ComplaintRequestDto dto) {
        if (dto.getLatitude() == null || dto.getLongitude() == null) {
            throw new CustomException(ErrorCode.VALIDATION_FAIL, "위치 정보(위도/경도)는 필수입니다.");
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

    // 7. (관리자) 상태별 민원 목록
    public List<ComplaintResponseDto> getByStatus(ComplaintStatus status) {
        return complaintRepository.findByStatus(status)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // 8. (관리자) 민원 수정(본인확인 없이)
    public ComplaintResponseDto updateComplaint(Long id, ComplaintRequestDto dto) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원 정보를 찾을 수 없습니다."));
        updateEntity(complaint, dto);
        complaintRepository.save(complaint);
        return toDto(complaint);
    }

    // 9. (관리자) 민원 삭제(본인확인 없이)
    public void deleteComplaint(Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원 정보를 찾을 수 없습니다."));
        complaintRepository.delete(complaint);
    }

    // 10. (관리자) 민원 상태변경
    public ComplaintResponseDto updateStatus(Long id, ComplaintStatus status) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("민원 정보를 찾을 수 없습니다."));
        complaint.setStatus(status);
        complaintRepository.save(complaint);
        return toDto(complaint);
    }

    // 11. (대시보드) 전체/카테고리별 통계
    public DashboardResponseDto getDashboardStats() {
        long total = complaintRepository.count();
        long completed = complaintRepository.countByStatus(ComplaintStatus.COMPLETED);

        Map<ComplaintCategory, Long> categoryCounts = Arrays.stream(ComplaintCategory.values())
                .collect(Collectors.toMap(
                        c -> c,
                        complaintRepository::countByCategory
                ));

        Map<ComplaintCategory, Double> categoryRates = categoryCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> total > 0 ? (e.getValue() * 100.0 / total) : 0.0
                ));

        double completedRate = total > 0 ? (completed * 100.0 / total) : 0.0;

        return DashboardResponseDto.builder()
                .totalCount(total)
                .completedRate(completedRate)
                .categoryCounts(categoryCounts)
                .categoryRates(categoryRates)
                .build();
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

    public List<MapPointDto> getComplaintMapPoints() {
        List<Complaint> complaints = complaintRepository.findAll();
        // 꼭 필요한 정보만 MapPointDto로 가공해서 반환
        return complaints.stream()
                .map(c -> new MapPointDto(c.getLatitude(), c.getLongitude(), c.getCategory(), c.getStatus()))
                .collect(Collectors.toList());
    }

    // ====== 내부 유틸 ======

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
        complaint.setContent(dto.getContent());
        complaint.setCategory(dto.getCategory());
        complaint.setAddress(dto.getAddress());
        complaint.setLatitude(dto.getLatitude());
        complaint.setLongitude(dto.getLongitude());
        complaint.setImageUrl(dto.getImageUrl());
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
}
