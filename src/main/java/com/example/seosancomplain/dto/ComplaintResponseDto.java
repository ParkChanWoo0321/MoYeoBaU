package com.example.seosancomplain.dto;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import com.example.seosancomplain.domain.complaint.ComplaintStatus;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ComplaintResponseDto {
    private Long id;
    private String content;
    private String address;
    private Double latitude;
    private Double longitude;
    private ComplaintCategory category;
    private ComplaintStatus status;
    private String imageUrl;
    private String userName;
    private String phoneNumber;
    private String createdAt;
    private String updatedAt;
}
