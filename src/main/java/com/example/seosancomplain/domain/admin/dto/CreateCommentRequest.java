package com.example.seosancomplain.domain.admin.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class CreateCommentRequest {
    @NotBlank(message = "코멘트를 입력해 주세요.")
    private String content;
}
