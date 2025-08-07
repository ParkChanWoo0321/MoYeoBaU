package com.example.seosancomplain.domain.complaint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByUserNameAndPhoneNumber(String userName, String phoneNumber);
    List<Complaint> findByCategory(ComplaintCategory category);
    long countByCategory(ComplaintCategory category);
    long countByAddressAndCreatedAtBetween(String address, java.time.LocalDateTime from, java.time.LocalDateTime to);
    List<Complaint> findByStatus(ComplaintStatus status);
    long countByStatus(ComplaintStatus status);
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    @Query("SELECT c.address, COUNT(c) FROM Complaint c GROUP BY c.address")
    List<Object[]> countByAddressGroup();
    long countByStatusAndCreatedAtBetween(ComplaintStatus status, LocalDateTime from, LocalDateTime to);
    @Query("SELECT c.address, c.status, COUNT(c) " +
            "FROM Complaint c " +
            "WHERE c.createdAt BETWEEN :start AND :end " +
            "GROUP BY c.address, c.status")
    List<Object[]> countByAddressAndStatusBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
    @Query("SELECT c.address, COUNT(c) as cnt " +
            "FROM Complaint c " +
            "WHERE c.status = :status " +
            "GROUP BY c.address " +
            "ORDER BY cnt DESC")
    List<Object[]> topAddressByStatus(@Param("status") ComplaintStatus status);
}
