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
    private String address;               // 동네/주소(자유 입력 또는 행정구역 문자열)
    private Double latitude;              // 위도
    private Double longitude;             // 경도

    @Enumerated(EnumType.STRING)
    private ComplaintCategory category;   // 카테고리
    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;       // 상태

    private String imageUrl;              // 첨부사진 URL

    // 익명 본인확인(접수, 수정, 삭제 시 사용)
    private String userName;              // 이름(본인확인용)
    private String phoneNumber;           // 연락처(본인확인용)

    @Column(updatable = false)
    private LocalDateTime createdAt; // 처음 올린 시간
    private LocalDateTime updatedAt; // 마지막 수정 시간
    private LocalDateTime resolvedAt; // 처리완료로 전환된 시간

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) this.status = ComplaintStatus.PENDING;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
