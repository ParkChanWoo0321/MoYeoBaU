package com.example.seosancomplain.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminReportDto {
    private int totalCount;        // 전체 민원 수
    private int completedCount;    // 처리 완료된 민원 수
    private int processingCount;   // 처리 중 민원 수
    private int pendingCount;      // 접수만 된 민원 수
}
