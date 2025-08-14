package com.example.seosancomplain.dashboard;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryTrendItem {
    private ComplaintCategory category;
    private double valuePercent;        // %
    private boolean up;                 // 화살표
}