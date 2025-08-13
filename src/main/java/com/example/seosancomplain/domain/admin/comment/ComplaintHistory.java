package com.example.seosancomplain.domain.admin.comment;

import com.example.seosancomplain.domain.complaint.Complaint;
import com.example.seosancomplain.domain.complaint.ComplaintStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="complaint_id")
    private Complaint complaint;

    @Enumerated(EnumType.STRING)
    private HistoryType type;

    @Enumerated(EnumType.STRING)
    private ComplaintStatus statusSnapshot;  // 상태 변경 시 스냅샷

    @Column(length=500)
    private String memo;

    private LocalDateTime createdAt;
}