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

    // 기본 조회
    List<Complaint> findByUserNameAndPhoneNumber(String userName, String phoneNumber);

    // 기간/상태 집계
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByStatusAndCreatedAtBetween(ComplaintStatus status, LocalDateTime from, LocalDateTime to);

    // 대시보드
    long countByCategoryAndStatus(ComplaintCategory category, ComplaintStatus status);
    List<Complaint> findByCategoryAndStatus(ComplaintCategory category, ComplaintStatus status);
    Optional<Complaint> findByIdAndStatus(Long id, ComplaintStatus status);
    long countByAddressContainingAndCreatedAtBetween(
            String addressPart,
            java.time.LocalDateTime start,
            java.time.LocalDateTime end
    );
    long countByCategoryAndCreatedAtBetween(
            ComplaintCategory category, LocalDateTime start, LocalDateTime end);

    List<Complaint> findByCategoryAndCreatedAtBetween(
            ComplaintCategory category, LocalDateTime start, LocalDateTime end);

    List<Complaint> findByCategoryAndStatusAndCreatedAtBetween(
            ComplaintCategory category, ComplaintStatus status, LocalDateTime start, LocalDateTime end);
    List<Complaint> findByStatusAndResolvedAtBetween(
            ComplaintStatus status, LocalDateTime start, LocalDateTime end);

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);
    @Query("""
   select c.category as category, count(c) as cnt
   from Complaint c
   where c.status = :status
   group by c.category
   order by cnt desc
""")
    List<CategoryCount> findTopCategoryByStatus(@Param("status") ComplaintStatus status, Pageable pageable);

    // 해당 카테고리의 미처리 글 목록
    Page<Complaint> findByCategoryAndStatus(ComplaintCategory category, ComplaintStatus status, Pageable pageable);
}
