package com.example.seosancomplain.domain.complaint;

import com.example.seosancomplain.domain.admin.CategoryCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByUserNameAndPhoneNumber(String userName, String phoneNumber);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByStatusAndCreatedAtBetween(ComplaintStatus status, LocalDateTime from, LocalDateTime to);

    @Query("""
        SELECT COUNT(c)
        FROM Complaint c
        WHERE c.category = :category AND c.status = :status
    """)
    long countByCategoryAndStatus(@Param("category") ComplaintCategory category,
                                  @Param("status") ComplaintStatus status);

    @Query("""
        SELECT c
        FROM Complaint c
        WHERE c.category = :category AND c.status = :status
        ORDER BY c.createdAt DESC
    """)
    List<Complaint> findByCategoryAndStatus(@Param("category") ComplaintCategory category,
                                            @Param("status") ComplaintStatus status);

    Optional<Complaint> findByIdAndStatus(Long id, ComplaintStatus status);

    long countByAddressContainingAndCreatedAtBetween(String addressPart,
                                                     LocalDateTime start,
                                                     LocalDateTime end);

    @Query("""
        SELECT COUNT(c)
        FROM Complaint c
        WHERE c.category = :category
          AND c.createdAt BETWEEN :start AND :end
    """)
    long countByCategoryAndCreatedAtBetween(@Param("category") ComplaintCategory category,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    @Query("""
        SELECT c
        FROM Complaint c
        WHERE c.category = :category
          AND c.createdAt BETWEEN :start AND :end
        ORDER BY c.createdAt DESC
    """)
    List<Complaint> findByCategoryAndCreatedAtBetween(@Param("category") ComplaintCategory category,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    @Query("""
        SELECT c
        FROM Complaint c
        WHERE c.category = :category
          AND c.status = :status
          AND c.createdAt BETWEEN :start AND :end
        ORDER BY c.createdAt DESC
    """)
    List<Complaint> findByCategoryAndStatusAndCreatedAtBetween(@Param("category") ComplaintCategory category,
                                                               @Param("status") ComplaintStatus status,
                                                               @Param("start") LocalDateTime start,
                                                               @Param("end") LocalDateTime end);

    List<Complaint> findByStatusAndResolvedAtBetween(ComplaintStatus status,
                                                     LocalDateTime start,
                                                     LocalDateTime end);

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    @Query("""
        SELECT c.category AS category, COUNT(c) AS cnt
        FROM Complaint c
        WHERE c.status = :status
        GROUP BY c.category
        ORDER BY cnt DESC
    """)
    List<CategoryCount> findTopCategoryByStatus(@Param("status") ComplaintStatus status, Pageable pageable);

    Page<Complaint> findByCategoryAndStatus(ComplaintCategory category,
                                            ComplaintStatus status,
                                            Pageable pageable);
}