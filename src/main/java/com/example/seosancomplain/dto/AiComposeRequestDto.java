package com.example.seosancomplain.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
public class AiComposeRequestDto {
    private String content;                 // 자유 텍스트 (빈 문자열 가능)
    private List<String> imageUrls;         // 이미지 URL들 (없으면 빈 리스트)
    private Map<String, Object> meta;       // 옵션. 없으면 서버에서 서산 기본 메타 주입
}