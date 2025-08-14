package com.example.seosancomplain.dashboard;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryTrendItem {
    private ComplaintCategory category; // 라벨은 프론트에서 매핑
    private double valuePercent;        // 화면에 찍을 % (여기선 deltaPercent)
    private boolean up;                 // true: ▲, false: ▼
}