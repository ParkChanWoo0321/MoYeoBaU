// ComposeOut.java (문서화 결과)
package com.example.seosancomplain.domain.ai.dto;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ComposeOut {
    private String title;
    private String body;
    private String html;
    private ComplaintFields fields;
}
