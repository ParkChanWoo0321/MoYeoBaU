package com.example.seosancomplain.domain.attachment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<String>> uploadMulti(@RequestPart("files") List<MultipartFile> files) {
        try {
            if (files == null || files.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            List<String> urls = attachmentService.saveAllAndReturnUrls(files);
            return ResponseEntity.ok(urls);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}