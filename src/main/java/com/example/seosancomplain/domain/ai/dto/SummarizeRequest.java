// SummarizeRequest.java  (추출 요청)
package com.example.seosancomplain.domain.ai.dto;
import lombok.*;
import java.util.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SummarizeRequest {
    private String complaint_text;
    private List<ImageInput> images = new ArrayList<>();
    private Map<String,Object> meta = new HashMap<>();
}
