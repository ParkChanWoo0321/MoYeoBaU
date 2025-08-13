package com.example.seosancomplain.domain.admin.dto;

import com.example.seosancomplain.domain.complaint.RejectionReason;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class RejectRequestDto {
    @NotNull(message = "반려 사유를 선택해 주세요.")
    private RejectionReason reason;
    // 기타일 때만 값 사용
    private String detail;
}