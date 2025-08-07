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

    // 1. 민원 등록 (익명)
    @PostMapping("")
    public ResponseEntity<ComplaintResponseDto> create(@RequestBody @Valid ComplaintRequestDto dto) {
        return ResponseEntity.ok(complaintService.createComplaint(dto));
    }

    // 2. 내 민원 목록 조회 (본인확인) → ComplaintListDto로 변경
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

    // 3. 내 민원 단건 조회 (본인확인)
    @GetMapping("/my/{id}")
    public ResponseEntity<ComplaintResponseDto> getMyComplaint(
            @PathVariable Long id,
            @RequestParam String userName,
            @RequestParam String phoneNumber
    ) {
        return ResponseEntity.ok(complaintService.getMyComplaint(id, userName, phoneNumber));
    }

    // 4. 내 민원 수정 (본인확인)
    @PatchMapping("/my/{id}")
    public ResponseEntity<ComplaintResponseDto> updateMyComplaint(
            @PathVariable Long id,
            @RequestBody ComplaintRequestDto dto
    ) {
        return ResponseEntity.ok(complaintService.updateMyComplaint(id, dto));
    }

    // 5. 내 민원 삭제 (본인확인)
    @DeleteMapping("/my/{id}")
    public ResponseEntity<Void> deleteMyComplaint(
            @PathVariable Long id,
            @RequestParam String userName,
            @RequestParam String phoneNumber
    ) {
        complaintService.deleteMyComplaint(id, userName, phoneNumber);
        return ResponseEntity.noContent().build();
    }

    // 6. 전체 민원 목록 (관리자/공용) - 그대로 List로 반환
    @GetMapping("")
    public ResponseEntity<List<ComplaintResponseDto>> getAll() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    // 7. 대시보드 통계 전체
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponseDto> getDashboard() {
        return ResponseEntity.ok(complaintService.getDashboardStats());
    }

    // 상태별 민원 목록 조회
    @GetMapping("/status")
    public ResponseEntity<List<ComplaintResponseDto>> getByStatus(@RequestParam ComplaintStatus status) {
        return ResponseEntity.ok(complaintService.getByStatus(status));
    }

    @GetMapping("/map")
    public ResponseEntity<List<MapPointDto>> getComplaintMapPoints() {
        return ResponseEntity.ok(complaintService.getComplaintMapPoints());
    }

    // 확장: AI 요약, 위치별 히트맵 등 추가 엔드포인트 필요시 위처럼 추가
}
