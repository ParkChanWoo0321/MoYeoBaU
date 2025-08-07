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
}
