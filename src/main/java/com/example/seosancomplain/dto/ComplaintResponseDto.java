package com.example.seosancomplain.dto;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import com.example.seosancomplain.domain.complaint.ComplaintStatus;
import com.example.seosancomplain.domain.complaint.RejectionReason;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintResponseDto {
    private Long id;
    private String title;
    private String content;
    private String address;
    private ComplaintCategory category;
    private ComplaintStatus status;
    private List<String> imageUrls;
    private String userName;
    private String phoneNumber;
    private String createdAt;
    private String updatedAt;
    private RejectionReason rejectionReason;
    private String rejectionDetail;
}
