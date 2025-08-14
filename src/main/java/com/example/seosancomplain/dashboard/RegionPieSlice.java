package com.example.seosancomplain.dashboard;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegionPieSlice {
    private String name;     // 라벨(지역명)
    private long value;      // 건수
    private double percent;  // 현재기간 비중(%)
}