package com.example.seosancomplain.domain.user;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import com.example.seosancomplain.domain.complaint.ComplaintService;
import com.example.seosancomplain.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ComplaintService complaintService;

    // 전체 민원 목록
    @GetMapping("/complaints")
    public ResponseEntity<ComplaintListDto> getAllComplaints() {
        List<ComplaintResponseDto> list = complaintService.getAllComplaints();
        ComplaintListDto dto = ComplaintListDto.builder()
                .complaints(list)
                .totalCount(list.size())
                .build();
        return ResponseEntity.ok(dto);
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
        AdminReportDto report = complaintService.getAdminReport(from, to);
        return ResponseEntity.ok(report);
    }

    // 민원 상태변경
    @PatchMapping("/complaints/{id}/status")
    public ResponseEntity<ComplaintResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestBody ComplaintStatusUpdateDto statusUpdateDto
    ) {
        return ResponseEntity.ok(
                complaintService.updateStatus(id, statusUpdateDto.getStatus())
        );
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
}
