package com.example.seosancomplain.domain.complaint;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // --- 기본 조회 ---
    List<Complaint> findByUserNameAndPhoneNumber(String userName, String phoneNumber);
    List<Complaint> findByCategory(ComplaintCategory category);
    long countByCategory(ComplaintCategory category);
    long countByStatus(ComplaintStatus status);

    // --- 기간/상태 집계 ---
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByStatusAndCreatedAtBetween(ComplaintStatus status, LocalDateTime from, LocalDateTime to);

    // --- 대시보드: 최신 5건 ---
    List<Complaint> findTop5ByOrderByCreatedAtDesc();

    @Query("""
           SELECT c.address, c.status, COUNT(c)
           FROM Complaint c
           WHERE c.createdAt BETWEEN :start AND :end
           GROUP BY c.address, c.status
           """)
    List<Object[]> countByAddressAndStatusBetween(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    @Query("""
           SELECT c.address, COUNT(c) as cnt
           FROM Complaint c
           WHERE c.status = :status
           GROUP BY c.address
           ORDER BY cnt DESC
           """)
    List<Object[]> topAddressByStatus(@Param("status") ComplaintStatus status);
    @Query("SELECT c.category, COUNT(c) " +
            "FROM Complaint c " +
            "WHERE c.status = :status " +
            "GROUP BY c.category")
    List<Object[]> countByCategoryAndStatus(@Param("status") ComplaintStatus status);
    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);
}
