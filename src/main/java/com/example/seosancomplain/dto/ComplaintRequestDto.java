package com.example.seosancomplain.dto;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintRequestDto {
    private String title;
    private String content;
    private String address;
    private String docHtml;                // AI/사용자 최종본(HTML) (선택)
    private String docMarkdown;
    private List<ComplaintCategory> categories;
    private List<String> imageUrls;
    // 본인확인
    private String userName;
    private String phoneNumber;

    @Builder.Default
    private Boolean autoCompose = Boolean.TRUE;
}