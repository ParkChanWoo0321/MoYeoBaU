package com.example.seosancomplain.domain.admin;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import com.example.seosancomplain.domain.complaint.ComplaintService;
import com.example.seosancomplain.domain.complaint.ComplaintStatus;
import com.example.seosancomplain.domain.region.RegionPriorityDto;
import com.example.seosancomplain.domain.region.RegionReportDto;
import com.example.seosancomplain.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ComplaintService complaintService;

    @Value("${admin.secret}")
    private String adminSecret;

    @ModelAttribute
    public void verifyAdmin(
            @RequestHeader(value = "X-ADMIN-SECRET", required = false) String headerSecret,
            @RequestParam(value = "adminSecret", required = false) String paramSecret
    ) {
        String provided = (headerSecret != null && !headerSecret.isBlank()) ? headerSecret : paramSecret;
        if (provided == null || !provided.equals(adminSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 비밀번호가 올바르지 않습니다.");
        }
    }

    // 카테고리별 민원 상세보기
    @GetMapping("/complaints/category")
    public ResponseEntity<ComplaintListDto> getByCategory(@RequestParam ComplaintCategory category) {
        List<ComplaintResponseDto> list = complaintService.getByCategory(category);
        ComplaintListDto dto = ComplaintListDto.builder()
                .complaints(list)
                .totalCount(list.size())
                .build();
        return ResponseEntity.ok(dto);
    }

    // 민원 수정
    @PatchMapping("/complaints/{id}")
    public ResponseEntity<ComplaintResponseDto> updateComplaint(@PathVariable Long id,
                                                                @RequestBody ComplaintRequestDto dto) {
        return ResponseEntity.ok(complaintService.updateComplaint(id, dto));
    }

    // 민원 삭제
    @DeleteMapping("/complaints/{id}")
    public ResponseEntity<Void> deleteComplaint(@PathVariable Long id) {
        complaintService.deleteComplaint(id);
        return ResponseEntity.noContent().build();
    }

    // 관리자 대시보드 (전체요약)
    @GetMapping("/report")
    public ResponseEntity<AdminReportDto> getReport(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(complaintService.getAdminReport(from, to));
    }

    // 민원 상태변경
    @PatchMapping("/complaints/{id}/status")
    public ResponseEntity<ComplaintResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestBody ComplaintStatusUpdateDto statusUpdateDto
    ) {
        return ResponseEntity.ok(complaintService.updateStatus(id, statusUpdateDto.getStatus()));
    }

    // 지역별 월간 / 일간 리포트 발행
    @GetMapping("/report/region")
    public ResponseEntity<List<RegionReportDto>> getRegionReport(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(complaintService.getRegionReport(from, to));
    }

    // 우선순위별 지역 목록화
    @GetMapping("/priority")
    public ResponseEntity<List<RegionPriorityDto>> getPriorityRegions() {
        return ResponseEntity.ok(complaintService.getPriorityRegions());
    }

    // 오늘 리포트
    @GetMapping("/report/daily")
    public ResponseEntity<AdminReportDto> reportDaily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        LocalDate d = (day == null) ? LocalDate.now() : day;
        return ResponseEntity.ok(complaintService.getAdminReport(d, d));
    }

    // 월간 리포트
    @GetMapping("/report/monthly")
    public ResponseEntity<AdminReportDto> reportMonthly(@RequestParam String yearMonth) {
        java.time.YearMonth ym = java.time.YearMonth.parse(yearMonth);
        LocalDate from = ym.atDay(1);
        LocalDate to   = ym.atEndOfMonth();
        return ResponseEntity.ok(complaintService.getAdminReport(from, to));
    }

    // 카테고리별 상태 카운트 (기본: PENDING)
    @GetMapping("/stats/category")
    public ResponseEntity<List<CategoryStatDto>> categoryStats(
            @RequestParam(defaultValue = "PENDING") ComplaintStatus status) {
        return ResponseEntity.ok(complaintService.getCategoryStatsByStatus(status));
    }

    // 우선순위 민원 목록 (기본: PENDING, 5건)
    @GetMapping("/priority/complaints")
    public ResponseEntity<List<ComplaintMiniDto>> priorityComplaints(
            @RequestParam(defaultValue = "PENDING") ComplaintStatus status,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(complaintService.getPriorityComplaints(status, limit));
    }

    @GetMapping("/complaints/page")
    public ResponseEntity<Page<ComplaintResponseDto>> getAllPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt,DESC") String sort
    ) {
        String[] parts = sort.split(",");
        Sort s = Sort.by(Sort.Direction.fromString(parts.length > 1 ? parts[1] : "DESC"), parts[0]);
        return ResponseEntity.ok(complaintService.getAllPaged(PageRequest.of(page, size, s)));
    }
}
