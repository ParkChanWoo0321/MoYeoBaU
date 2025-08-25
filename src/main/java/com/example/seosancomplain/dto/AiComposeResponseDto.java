package com.example.seosancomplain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
public class AiComposeResponseDto {
    private String title;
    @JsonAlias({"complaint_text", "complaintText"})
    private String content;

    @JsonAlias({"images", "imageUrls", "image_urls"})
    private List<String> imageUrls;
    private String addressCandidate;
    private Map<String, Object> meta;
    private List<String> categorySuggestions;
    private JsonNode fields;
    private String docHtml;
    private String docMarkdown;
}