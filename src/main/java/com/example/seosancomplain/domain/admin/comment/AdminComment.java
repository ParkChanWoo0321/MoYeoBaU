package com.example.seosancomplain.domain.admin.comment;

import com.example.seosancomplain.domain.complaint.Complaint;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="complaint_id")
    private Complaint complaint;

    @Column(nullable=false, length=1000)
    private String content;

    @Column(nullable=false)
    private String author;

    @Column(nullable=false)
    private LocalDateTime createdAt;
}