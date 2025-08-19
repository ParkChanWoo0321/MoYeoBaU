package com.example.seosancomplain.domain.admin;

import com.example.seosancomplain.domain.admin.comment.AdminCommentDto;
import com.example.seosancomplain.dto.ComplaintDetailDto;
import com.example.seosancomplain.domain.admin.dto.ComplaintStatusUpdateDto;
import com.example.seosancomplain.domain.admin.dto.CreateCommentRequest;
import com.example.seosancomplain.domain.admin.dto.RejectRequestDto;
import com.example.seosancomplain.domain.admin.dto.AdminReportDto;
import com.example.seosancomplain.domain.admin.dto.CategoryCountDto;
import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import com.example.seosancomplain.domain.complaint.ComplaintService;
import com.example.seosancomplain.dto.ComplaintRequestDto;
import com.example.seosancomplain.dto.ComplaintResponseDto;
import com.example.seosancomplain.dto.*;
import jakarta.validation.Valid;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ComplaintService complaintService;

    @Value("${admin.secret}")
    private String adminSecret;

    // 모든 관리자 API에 공통 적용
    @ModelAttribute
    public void verifyAdmin(
            @RequestHeader(value = "PASSWORD", required = false) String headerSecret,
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

    // 카테고리별 미처리 건수
    @GetMapping("/complaints/categories")
    public ResponseEntity<List<CategoryCountDto>> getCategoryCardsAdmin(
            @RequestParam(required = false) Boolean ignoredPendingOnly
    ) {
        return ResponseEntity.ok(complaintService.getPendingCategoryCounts());
    }

    // 민원 수정
    @PatchMapping("/complaints/{id}")
    public ResponseEntity<ComplaintResponseDto> updateComplaint(@PathVariable Long id,
                                                                @RequestBody ComplaintRequestDto dto) {
        return ResponseEntity.ok(complaintService.updateComplaint(id, dto));
    }

    // 민원 삭제
    @DeleteMapping("/complaints/{id}")
    public ResponseEntity<Map<String, Object>> deleteComplaintByAdmin(@PathVariable Long id) {
        complaintService.deleteComplaint(id);
        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "민원이 정상적으로 삭제되었습니다.");
        return ResponseEntity.ok(body);
    }

    // 관리자 대시보드
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

    // 일간 리포트
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

    // AI 요약
    @PostMapping("/complaints/{id}/ai-summary")
    public SummaryDto upsertAndGetSummary(@PathVariable Long id) {
        Map<String,String> f = complaintService.ensureSummaryFields(id);
        return new SummaryDto(
                f.getOrDefault("location",""),
                f.getOrDefault("phenomenon",""),
                f.getOrDefault("problem",""),
                f.getOrDefault("risk",""),
                f.getOrDefault("request","")
        );
    }

    public record SummaryDto(String location, String phenomenon, String problem, String risk, String request) {}

    // 상세(내용/이미지/댓글)
    @GetMapping("/complaints/{id}")
    public ResponseEntity<ComplaintDetailDto> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(complaintService.getComplaintDetail(id));
    }

    // 코멘트 작성
    @PostMapping("/complaints/{id}/comments")
    public ResponseEntity<AdminCommentDto> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest req) {
        return ResponseEntity.ok(complaintService.addAdminComment(id, req.getContent()));
    }

    // 반려
    @PostMapping("/complaints/{id}/reject")
    public ResponseEntity<ComplaintResponseDto> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequestDto dto
    ) {
        return ResponseEntity.ok(complaintService.rejectComplaint(id, dto.getReason(), dto.getDetail()));
    }

    // 접수된 미처리 민원
    @GetMapping("/complaints/pending")
    public ResponseEntity<ComplaintListDto> getAllPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String[] parts = sort.split(",");
        String sortField = parts[0];
        Sort.Direction direction = (parts.length > 1 && parts[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<ComplaintResponseDto> result = complaintService.getPendingListAsUnified(pageable);

        ComplaintListDto dto = ComplaintListDto.builder()
                .complaints(result.getContent())
                .totalCount((int) result.getTotalElements())
                .build();

        return ResponseEntity.ok(dto);
    }


    // 긴급민원/다발민원
    @GetMapping("/complaints/emergency")
    public ResponseEntity<ComplaintListDto> getTopCategoryPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String[] parts = sort.split(",");
        String sortField = parts[0];
        Sort.Direction direction = (parts.length > 1 && parts[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<ComplaintResponseDto> result = complaintService.getTopCategoryPendingAsUnified(pageable);

        ComplaintListDto dto = ComplaintListDto.builder()
                .complaints(result.getContent())
                .totalCount((int) result.getTotalElements())
                .build();

        return ResponseEntity.ok(dto);
    }
}