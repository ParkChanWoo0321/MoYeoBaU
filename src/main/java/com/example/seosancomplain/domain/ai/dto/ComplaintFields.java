// ComplaintFields.java  (추출 결과; 한국어 키를 그대로 받으려면 @JsonProperty)
package com.example.seosancomplain.domain.ai.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ComplaintFields {
    @JsonProperty("위치")     private String 위치;
    @JsonProperty("현상")     private String 현상;
    @JsonProperty("문제점")   private String 문제점;
    @JsonProperty("위험성")   private String 위험성;
    @JsonProperty("요청사항")  private String 요청사항;
}
