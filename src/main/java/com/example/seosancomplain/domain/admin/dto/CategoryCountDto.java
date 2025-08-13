package com.example.seosancomplain.domain.admin.dto;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryCountDto {
    private ComplaintCategory category;
    private long count; // 미처리 건수
}