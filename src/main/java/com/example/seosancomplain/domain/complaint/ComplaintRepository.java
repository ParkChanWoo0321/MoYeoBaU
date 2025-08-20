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

    // ===== 다중 카테고리: "하나라도 포함" 기준 =====

    // (집계) 특정 카테고리를 하나라도 포함하고, 상태가 일치하는 글 수
    @Query("""
        SELECT COUNT(DISTINCT c)
        FROM Complaint c
        JOIN c.categories cat
        WHERE cat = :category AND c.status = :status
    """)
    long countByCategoryAndStatus(@Param("category") ComplaintCategory category,
                                  @Param("status") ComplaintStatus status);

    // (목록) 특정 카테고리를 하나라도 포함하고, 상태가 일치하는 글들
    @Query("""
        SELECT DISTINCT c
        FROM Complaint c
        JOIN c.categories cat
        WHERE cat = :category AND c.status = :status
        ORDER BY c.createdAt DESC
    """)
    List<Complaint> findByCategoryAndStatus(@Param("category") ComplaintCategory category,
                                            @Param("status") ComplaintStatus status);

    Optional<Complaint> findByIdAndStatus(Long id, ComplaintStatus status);

    // 주소 키워드 + 기간 (단일 필드 기반이라 그대로 사용)
    long countByAddressContainingAndCreatedAtBetween(String addressPart,
                                                     LocalDateTime start,
                                                     LocalDateTime end);

    // (집계) 특정 카테고리를 하나라도 포함하고, 기간 내 글 수
    @Query("""
        SELECT COUNT(DISTINCT c)
        FROM Complaint c
        JOIN c.categories cat
        WHERE cat = :category
          AND c.createdAt BETWEEN :start AND :end
    """)
    long countByCategoryAndCreatedAtBetween(@Param("category") ComplaintCategory category,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    // (목록) 특정 카테고리를 하나라도 포함하고, 기간 내 글들
    @Query("""
        SELECT DISTINCT c
        FROM Complaint c
        JOIN c.categories cat
        WHERE cat = :category
          AND c.createdAt BETWEEN :start AND :end
        ORDER BY c.createdAt DESC
    """)
    List<Complaint> findByCategoryAndCreatedAtBetween(@Param("category") ComplaintCategory category,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    // (목록) 특정 카테고리를 하나라도 포함 + 상태 + 기간
    @Query("""
        SELECT DISTINCT c
        FROM Complaint c
        JOIN c.categories cat
        WHERE cat = :category
          AND c.status = :status
          AND c.createdAt BETWEEN :start AND :end
        ORDER BY c.createdAt DESC
    """)
    List<Complaint> findByCategoryAndStatusAndCreatedAtBetween(@Param("category") ComplaintCategory category,
                                                               @Param("status") ComplaintStatus status,
                                                               @Param("start") LocalDateTime start,
                                                               @Param("end") LocalDateTime end);

    // 상태 + 처리 완료일 범위 (그대로)
    List<Complaint> findByStatusAndResolvedAtBetween(ComplaintStatus status,
                                                     LocalDateTime start,
                                                     LocalDateTime end);

    // 상태별 페이징 (그대로)
    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    // 대시보드: 상태별 상위 카테고리 집계 (민원 1건이 여러 카테고리에 중복 집계됨)
    @Query("""
        SELECT cat AS category, COUNT(DISTINCT c) AS cnt
        FROM Complaint c
        JOIN c.categories cat
        WHERE c.status = :status
        GROUP BY cat
        ORDER BY cnt DESC
    """)
    List<CategoryCount> findTopCategoryByStatus(@Param("status") ComplaintStatus status, Pageable pageable);

    // 해당 카테고리의 미처리 글 목록 (페이징) - "하나라도 포함"
    @Query(value = """
            SELECT DISTINCT c
            FROM Complaint c
            JOIN c.categories cat
            WHERE cat = :category AND c.status = :status
            ORDER BY c.createdAt DESC
        """,
            countQuery = """
            SELECT COUNT(DISTINCT c)
            FROM Complaint c
            JOIN c.categories cat
            WHERE cat = :category AND c.status = :status
        """)
    Page<Complaint> findByCategoryAndStatus(@Param("category") ComplaintCategory category,
                                            @Param("status") ComplaintStatus status,
                                            Pageable pageable);
}