package com.example.seosancomplain.dashboard;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionTopDto {
    private String region;        // 지역이름
    private long count;           // 민원 수
    private double percent;       // %
    private double deltaPercent;  // 1.5  (전기간 대비 증감률)
    private boolean up;           // 화살표
}