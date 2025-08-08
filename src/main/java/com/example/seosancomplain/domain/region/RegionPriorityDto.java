package com.example.seosancomplain.domain.region;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegionPriorityDto {
    private String address;
    private Long pendingCount;
}
