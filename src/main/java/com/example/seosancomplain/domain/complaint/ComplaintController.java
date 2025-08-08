package com.example.seosancomplain.domain.complaint;

import com.example.seosancomplain.domain.dashboard.DashboardResponseDto;
import com.example.seosancomplain.dto.ComplaintListDto;
import com.example.seosancomplain.dto.ComplaintRequestDto;
import com.example.seosancomplain.dto.ComplaintResponseDto;
import com.example.seosancomplain.dto.MapPointDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<Void> deleteMyComplaint(
            @PathVariable Long id,
            @RequestParam String userName,
            @RequestParam String phoneNumber
    ) {
        complaintService.deleteMyComplaint(id, userName, phoneNumber);
        return ResponseEntity.noContent().build();
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

    // 지도 (위치 포인트)
    @GetMapping("/map")
    public ResponseEntity<List<MapPointDto>> getComplaintMapPoints() {
        return ResponseEntity.ok(complaintService.getComplaintMapPoints());
    }
}
