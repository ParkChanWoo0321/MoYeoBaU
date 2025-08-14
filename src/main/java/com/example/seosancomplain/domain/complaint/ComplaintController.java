package com.example.seosancomplain.domain.complaint;

import com.example.seosancomplain.dashboard.*;
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

    // 파이차트
    @GetMapping("/piechart")
    public ResponseEntity<RegionPieResponse> getRegionPie(
            @RequestParam(name = "days", required = false, defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(complaintService.computeRegionPie(days));
    }

    // Top5 리스트
    @GetMapping("/region-top5")
    public List<RegionTopDto> getRegionTop5(@RequestParam(defaultValue = "30") int days) {
        return complaintService.computeRegionTop5(days);
    }

    // 민원 내용 보기
    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponseDto> getPublicDetail(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getPublicComplaintAsResponse(id));
    }

    // 카테고리별 민원현황
    @GetMapping("/categorystat")
    public ResponseEntity<List<CategoryTrendItem>> getCategoryStat(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "true") boolean all
    ) {
        return ResponseEntity.ok(complaintService.getCategoryTrends(days, all));
    }

    // 카테고리별 글목록
    @GetMapping("/categorylist")
    public ResponseEntity<?> getComplaintsByCategory(
            @RequestParam("category") ComplaintCategory category,
            @RequestParam(name = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(name = "days", required = false, defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(complaintService.getPublicComplaintsByCategory(category, status, days));
    }

    // 민원 처리율
    @GetMapping("/resolution-rate")
    public ResponseEntity<ResolutionRateDto> getResolutionRate(
            @RequestParam(name = "days", defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(complaintService.computeResolutionRate(days));
    }

    // 평균 처리시간
    @GetMapping("/avg-handle-time")
    public ResponseEntity<AvgHandleTimeDto> getAvgHandleTime(
            @RequestParam(name = "days", defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(complaintService.computeAvgHandleTime(days));
    }
}