package com.example.seosancomplain.dto;

import com.example.seosancomplain.domain.admin.comment.AdminCommentDto;
import com.example.seosancomplain.domain.complaint.Complaint;
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
    private List<AdminCommentDto> comments;
    private Integer commentCount;
    public static ComplaintResponseDto from(Complaint c) {
        return ComplaintResponseDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .content(c.getContent())
                .address(c.getAddress())
                .status(c.getStatus())
                .category(ComplaintCategory.valueOf(c.getCategory().name()))
                .createdAt(c.getCreatedAt().toString())
                .build();
    }
}
