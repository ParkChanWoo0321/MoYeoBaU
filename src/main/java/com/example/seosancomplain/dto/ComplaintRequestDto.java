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
    @NotBlank(message="제목을 입력해 주세요.")
    private String title;
    @NotBlank(message="내용을 입력해 주세요.")
    private String content;
    private String address;
    private List<ComplaintCategory> categories;
    private List<String> imageUrls;
    // 본인확인
    private String userName;
    private String phoneNumber;
}