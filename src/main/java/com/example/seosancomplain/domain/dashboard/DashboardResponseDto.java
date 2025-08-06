package com.example.seosancomplain.domain.dashboard;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import lombok.*;

import java.util.Map;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDto {
    private long totalCount; // 전체 민원 수
    private double completedRate; // 처리율(%)
    private Map<ComplaintCategory, Long> categoryCounts; // 카테고리별 민원 수
    private Map<ComplaintCategory, Double> categoryRates; // 카테고리별 비율(%)
}
