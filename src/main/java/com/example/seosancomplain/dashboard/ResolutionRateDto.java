package com.example.seosancomplain.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResolutionRateDto {
    private double ratePercent;   // 금기간 처리율(%)
    private double deltaPercent;  // 전기간 대비 증감(%p)
    private boolean up;           // 증가 여부(증감 부호 > 0)
}