package com.example.seosancomplain.domain.user;

import com.example.seosancomplain.domain.complaint.ComplaintService;
import com.example.seosancomplain.domain.complaint.ComplaintStatus;
import com.example.seosancomplain.dto.AdminReportDto;
import com.example.seosancomplain.dto.ComplaintRequestDto;
import com.example.seosancomplain.dto.ComplaintResponseDto;
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
    public ResponseEntity<List<ComplaintResponseDto>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    // 특정 상태별 민원 목록 (예: 처리중)
    @GetMapping("/complaints/status")
    public ResponseEntity<List<ComplaintResponseDto>> getByStatus(@RequestParam ComplaintStatus status) {
        return ResponseEntity.ok(complaintService.getByStatus(status));
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

    @GetMapping("/report")
    public ResponseEntity<AdminReportDto> getReport(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AdminReportDto report = complaintService.getAdminReport(from, to);
        return ResponseEntity.ok(report);
    }
}
