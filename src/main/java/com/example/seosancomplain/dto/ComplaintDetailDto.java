package com.example.seosancomplain.dto;

import com.example.seosancomplain.domain.admin.comment.AdminCommentDto;
import com.example.seosancomplain.domain.complaint.*;
import lombok.*;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintDetailDto {
    private Long id;

    // 상단 카드
    private String title;
    private String content;
    private ComplaintCategory category;
    private ComplaintStatus status;

    // 작성자 영역
    private String userName;
    private String phoneNumber;

    // 이미지/주소
    private String address;
    private List<String> imageUrls;

    private String createdAt;

    // 댓글
    private List<AdminCommentDto> comments;

    // 반려내용
    private RejectionReason rejectionReason;
    private String rejectionDetail; // 기타 내용 (기타가 아닐시 null)
}