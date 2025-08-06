package com.example.seosancomplain.dto;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import com.example.seosancomplain.domain.complaint.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MapPointDto {
    private double latitude;
    private double longitude;
    private ComplaintCategory category;
    private ComplaintStatus status;
}
