package com.example.seosancomplain.domain.attachment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/upload")
    public ResponseEntity<Attachment> upload(@RequestParam("file") MultipartFile file) {
        try {
            Attachment attachment = attachmentService.save(file);
            return ResponseEntity.ok(attachment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
