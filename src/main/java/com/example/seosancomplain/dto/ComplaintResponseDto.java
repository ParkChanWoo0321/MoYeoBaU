package com.example.seosancomplain.dto;

import com.example.seosancomplain.domain.admin.comment.AdminCommentDto;
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
}