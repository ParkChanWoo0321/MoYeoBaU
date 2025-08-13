package com.example.seosancomplain.domain.complaint;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // 기본 조회
    List<Complaint> findByUserNameAndPhoneNumber(String userName, String phoneNumber);
    long countByCategory(ComplaintCategory category);
    long countByStatus(ComplaintStatus status);

    // 기간/상태 집계
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByStatusAndCreatedAtBetween(ComplaintStatus status, LocalDateTime from, LocalDateTime to);

    // 대시보드
    List<Complaint> findTop5ByOrderByCreatedAtDesc();

    long countByCategoryAndStatus(ComplaintCategory category, ComplaintStatus status);
    List<Complaint> findByCategoryAndStatus(ComplaintCategory category, ComplaintStatus status);
    Optional<Complaint> findByIdAndStatus(Long id, ComplaintStatus status);
}
