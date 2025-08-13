package com.example.seosancomplain.domain.complaint;

import com.example.seosancomplain.dto.ComplaintDetailDto;
import com.example.seosancomplain.domain.admin.dto.DashboardResponseDto;
import com.example.seosancomplain.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    // 민원 등록
    @PostMapping("")
    public ResponseEntity<ComplaintResponseDto> create(@RequestBody @Valid ComplaintRequestDto dto) {
        return ResponseEntity.ok(complaintService.createComplaint(dto));
    }

    // 내 민원 목록 조회 (본인확인)
    @GetMapping("/my")
    public ResponseEntity<ComplaintListDto> getMyComplaints(
            @RequestParam String userName,
            @RequestParam String phoneNumber
    ) {
        List<ComplaintResponseDto> list = complaintService.getMyComplaints(userName, phoneNumber);
        ComplaintListDto dto = ComplaintListDto.builder()
                .complaints(list)
                .totalCount(list.size())
                .build();
        return ResponseEntity.ok(dto);
    }

    // 내 민원 단건 조회 (본인확인)
    @GetMapping("/my/{id}")
    public ResponseEntity<ComplaintResponseDto> getMyComplaint(
            @PathVariable Long id,
            @RequestParam String userName,
            @RequestParam String phoneNumber
    ) {
        return ResponseEntity.ok(complaintService.getMyComplaint(id, userName, phoneNumber));
    }

    // 내 민원 수정 (본인확인)
    @PatchMapping("/my/{id}")
    public ResponseEntity<ComplaintResponseDto> updateMyComplaint(
            @PathVariable Long id,
            @RequestBody ComplaintRequestDto dto
    ) {
        return ResponseEntity.ok(complaintService.updateMyComplaint(id, dto));
    }

    // 내 민원 삭제 (본인확인)
    @DeleteMapping("/my/{id}")
    public ResponseEntity<Map<String, Object>> deleteMyComplaint(
            @PathVariable Long id,
            @RequestParam String userName,
            @RequestParam String phoneNumber
    ) {
        complaintService.deleteMyComplaint(id, userName, phoneNumber);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "민원이 정상적으로 삭제되었습니다.");

        return ResponseEntity.ok(body);
    }

    // 민원 목록 (공용)
    @GetMapping("")
    public ResponseEntity<List<ComplaintResponseDto>> getAll() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    // 대시보드 (전체 요약)
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponseDto> getDashboard(
            @RequestParam(value = "days", required = false) Integer days
    ) {
        return ResponseEntity.ok(complaintService.getDashboardStats(days));
    }

    // 민원 내용 보기
    @GetMapping("/{id}")
    public ResponseEntity<ComplaintDetailDto> getPublicDetail(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getPublicComplaintDetail(id));
    }
}
