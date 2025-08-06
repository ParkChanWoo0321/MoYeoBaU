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
    // 필요하다면 카테고리별 통계, 월간 변화량, 지역별 현황 등 필드 추가 가능
}
