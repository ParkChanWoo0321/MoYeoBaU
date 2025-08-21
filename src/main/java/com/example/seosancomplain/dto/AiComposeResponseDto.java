package com.example.seosancomplain.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.util.List;

@Data
public class AiComposeResponseDto {
    private String title;
    private String docHtml;                 // 있을 수도/없을 수도
    private String docMarkdown;             // md 우선 사용 시
    private JsonNode fields;                // AI가 돌려준 필드 원본
    private List<String> categorySuggestions;
    private String addressCandidate;
}