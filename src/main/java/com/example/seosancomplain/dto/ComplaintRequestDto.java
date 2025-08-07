// ComplaintRequestDto.java
package com.example.seosancomplain.dto;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ComplaintRequestDto {
    private String content;
    private String address;
    @NotNull(message = "위도는 필수입니다.")
    private Double latitude;
    @NotNull(message = "경도는 필수입니다.")
    private Double longitude;
    private ComplaintCategory category;
    private String imageUrl;
    // 본인확인
    private String userName;
    private String phoneNumber;
}
