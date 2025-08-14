package com.example.seosancomplain.domain.attachment;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    @Modifying
    @Transactional
    void deleteByComplaintId(Long complaintId);
    List<Attachment> findByComplaintIdOrderByUploadedAtAsc(Long complaintId);
    List<Attachment> findByUrlIn(List<String> urls);
}
