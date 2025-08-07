package com.example.seosancomplain.dto;

import com.example.seosancomplain.domain.complaint.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegionReportDto {
    private String address;
    private ComplaintStatus status;
    private Long count;
}

