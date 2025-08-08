package com.example.seosancomplain.domain.ai;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiResponseDto {
    private ComplaintCategory category; // AI가 분류한 카테고리
    private String summary;             // 민원 요약
    private String clusterId;           // 유사 민원 클러스터 ID (옵션)
}
