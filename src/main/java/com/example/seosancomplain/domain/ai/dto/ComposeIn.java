// ComposeIn.java (문서화 입력)
package com.example.seosancomplain.domain.ai.dto;
import lombok.*;
import java.util.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ComposeIn {
    private String complaint_text;              // 없으면 null
    private List<ImageInput> images;            // 없으면 null
    private ComplaintFields fields;             // 이미 추출값이 있으면 바로 사용
    private Map<String,Object> meta;            // org, title_prefix 등
}
