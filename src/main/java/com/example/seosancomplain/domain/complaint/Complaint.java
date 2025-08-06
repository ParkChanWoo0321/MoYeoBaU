package com.example.seosancomplain.domain.complaint;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Complaint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;               // 민원 내용
    private String address;               // 동네/주소
    private Double latitude;              // 위도
    private Double longitude;             // 경도

    @Enumerated(EnumType.STRING)
    private ComplaintCategory category;   // 카테고리
    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;       // 상태

    private String imageUrl;              // 첨부사진

    // 익명 본인확인(접수,수정,삭제시)
    private String userName;              // 이름(본인확인용)
    private String phoneNumber;           // 연락처(본인확인용)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if(this.status == null) this.status = ComplaintStatus.PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
