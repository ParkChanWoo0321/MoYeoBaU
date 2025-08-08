package com.example.seosancomplain.domain.region;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegionStatDto {
    private String region; // 해미면, 고북면 등
    private long count;
    private double rate;   // 전체 대비 %
}
