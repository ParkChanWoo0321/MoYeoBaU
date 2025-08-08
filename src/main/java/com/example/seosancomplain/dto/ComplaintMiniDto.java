package com.example.seosancomplain.dto;

import com.example.seosancomplain.domain.complaint.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintMiniDto {
    private Long id;
    private String content;
    private String address;
    private ComplaintStatus status;
    private String createdAt;
}
