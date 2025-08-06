package com.example.seosancomplain.dto;

import com.example.seosancomplain.domain.complaint.ComplaintStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintStatusUpdateDto {
    private ComplaintStatus status;
}
