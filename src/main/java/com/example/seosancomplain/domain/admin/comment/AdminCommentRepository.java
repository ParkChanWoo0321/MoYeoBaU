package com.example.seosancomplain.domain.admin.comment;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface AdminCommentRepository extends JpaRepository<AdminComment, Long> {
    @Modifying
    @Transactional
    void deleteByComplaintId(Long complaintId);
    List<AdminComment> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
}