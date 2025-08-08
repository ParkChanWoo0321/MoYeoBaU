package com.example.seosancomplain.domain.dashboard;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import com.example.seosancomplain.dto.ComplaintMiniDto;
import com.example.seosancomplain.domain.region.RegionStatDto;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponseDto {
    private long totalCount;
    private Double totalCountDelta;        // 전기간 대비 증감(%)

    private double completedRate;
    private Double completedRateDelta;     // 처리율 증감(%)

    private double averageResolutionDays;  // 평균 처리시간(일)
    private Double averageResolutionDelta; // 평균 처리시간 증감(일)

    private Map<ComplaintCategory, Long>   categoryCounts;
    private Map<ComplaintCategory, Double> categoryRates;

    private List<ComplaintMiniDto> latestFive; // 최신 5건
    private List<RegionStatDto>    regionStats; // 지역 리스트(서산시 면/동)
}
