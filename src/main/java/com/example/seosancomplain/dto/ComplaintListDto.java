package com.example.seosancomplain.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintListDto {
    private List<ComplaintResponseDto> complaints;
    private int totalCount;
    // 필요시 페이지 번호, 전체 페이지 등 추가
}
