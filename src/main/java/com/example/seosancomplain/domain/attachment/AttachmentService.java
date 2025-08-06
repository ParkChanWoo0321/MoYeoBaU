package com.example.seosancomplain.domain.attachment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class AttachmentService {
    private final AttachmentRepository attachmentRepository;

    public Attachment save(MultipartFile file) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String filePath = "uploads/" + fileName; // 실제 환경에 맞게 경로 지정
        File dest = new File(filePath);
        file.transferTo(dest);

        Attachment attachment = Attachment.builder()
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(file.getSize())
                .build();

        return attachmentRepository.save(attachment);
    }
}
