package com.example.seosancomplain.dashboard;

import lombok.*;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegionPieResponse {
    private long total;                  // 현재기간 총 건수(파이 % 계산용)
    private List<RegionPieSlice> slices; // 파이 조각들
}