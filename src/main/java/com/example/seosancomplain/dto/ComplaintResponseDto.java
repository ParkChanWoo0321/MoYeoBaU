package com.example.seosancomplain.dto;

import com.example.seosancomplain.domain.admin.comment.AdminCommentDto;
import com.example.seosancomplain.domain.complaint.Complaint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintResponseDto {
    private Long id;
    private String title;
    private String content;
    private String address;
    private String category;
    private String status;
    private List<String> imageUrls;
    private String userName;
    private String phoneNumber;
    private String createdAt;
    private String updatedAt;
    private String rejectionReason;
    private String rejectionDetail;
    private List<AdminCommentDto> comments;
    private Integer commentCount;

    public static ComplaintResponseDto from(Complaint c, List<String> imageUrls) {
        return ComplaintResponseDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .content(c.getContent())
                .address(c.getAddress())
                .category(c.getCategory().name())
                .status(c.getStatus().name())
                .imageUrls(imageUrls == null ? List.of() : imageUrls)
                .userName(c.getUserName())
                .phoneNumber(c.getPhoneNumber())
                .createdAt(c.getCreatedAt().toString())
                .updatedAt(c.getUpdatedAt() == null
                        ? c.getCreatedAt().toString()
                        : c.getUpdatedAt().toString())
                .rejectionReason(c.getRejectionReason() == null ? null : c.getRejectionReason().name())
                .rejectionDetail(c.getRejectionDetail())
                .comments(null)
                .commentCount(null)
                .build();
    }
}
