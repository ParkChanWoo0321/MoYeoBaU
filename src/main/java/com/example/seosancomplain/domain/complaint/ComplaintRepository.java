package com.example.seosancomplain.domain.complaint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUserNameAndPhoneNumber(String userName, String phoneNumber);
    List<Complaint> findByCategory(ComplaintCategory category);
    long countByCategory(ComplaintCategory category);
    // 통계, 리포트용 예시 추가
    long countByAddressAndCreatedAtBetween(String address, java.time.LocalDateTime from, java.time.LocalDateTime to);
    List<Complaint> findByStatus(ComplaintStatus status);
    long countByStatus(ComplaintStatus status);
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    @Query("SELECT c.address, COUNT(c) FROM Complaint c GROUP BY c.address")
    List<Object[]> countByAddressGroup();
    long countByStatusAndCreatedAtBetween(ComplaintStatus status, LocalDateTime from, LocalDateTime to);
}
