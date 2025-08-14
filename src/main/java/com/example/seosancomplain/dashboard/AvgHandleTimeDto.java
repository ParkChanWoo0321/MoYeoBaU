package com.example.seosancomplain.dashboard;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AvgHandleTimeDto {
    private double days;      // 금기간 평균 처리시간(일)
    private double deltaDays; // 전기간 대비 증감(일)
    private boolean up;       // 증가 여부(증감 부호 > 0)  *주의: 시간은 down이 좋은 경우가 많음(프론트에서 색상 처리)
}