package com.example.seosancomplain.dashboard;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryOverviewItem {
    private final ComplaintCategory Category;
    private final long count;
    private final double percent;
    private final double deltaPercent;
    private final boolean up;
}
