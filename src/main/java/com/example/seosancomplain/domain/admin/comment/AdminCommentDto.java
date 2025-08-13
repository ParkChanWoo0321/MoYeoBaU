package com.example.seosancomplain.domain.admin.comment;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminCommentDto {
    private Long id;
    private String author;     // 관리자
    private String content;
    private String createdAt;
}