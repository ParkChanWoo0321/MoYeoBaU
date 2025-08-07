package com.example.seosancomplain.domain.user;

import com.example.seosancomplain.domain.complaint.ComplaintService;
import com.example.seosancomplain.domain.complaint.ComplaintStatus;
import com.example.seosancomplain.dto.AdminReportDto;
import com.example.seosancomplain.dto.ComplaintListDto;
import com.example.seosancomplain.dto.ComplaintRequestDto;
import com.example.seosancomplain.dto.ComplaintResponseDto;
import com.example.seosancomplain.dto.ComplaintStatusUpdateDto;
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

    // 전체 민원 목록 (ComplaintListDto로 리턴)
    @GetMapping("/complaints")
    public ResponseEntity<ComplaintListDto> getAllComplaints() {
        List<ComplaintResponseDto> list = complaintService.getAllComplaints();
        ComplaintListDto dto = ComplaintListDto.builder()
                .complaints(list)
                .totalCount(list.size())
                .build();
        return ResponseEntity.ok(dto);
    }

    // 특정 상태별 민원 목록 (ComplaintListDto로 리턴)
    @GetMapping("/complaints/status")
    public ResponseEntity<ComplaintListDto> getByStatus(@RequestParam ComplaintStatus status) {
        List<ComplaintResponseDto> list = complaintService.getByStatus(status);
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

    // 관리자 대시보드/통계
    @GetMapping("/report")
    public ResponseEntity<AdminReportDto> getReport(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        AdminReportDto report = complaintService.getAdminReport(from, to);
        return ResponseEntity.ok(report);
    }

    // 민원 상태변경 (플로우차트에 명시된 관리자 기능)
    @PatchMapping("/complaints/{id}/status")
    public ResponseEntity<ComplaintResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestBody ComplaintStatusUpdateDto statusUpdateDto
    ) {
        return ResponseEntity.ok(
                complaintService.updateStatus(id, statusUpdateDto.getStatus())
        );
    }
}
