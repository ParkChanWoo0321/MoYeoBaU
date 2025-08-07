package com.example.seosancomplain.domain.attachment;

import com.example.seosancomplain.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AttachmentService {
    private final AttachmentRepository attachmentRepository;

    public Attachment save(MultipartFile file) throws IOException {
        String fileName = FileUtil.generateUniqueFileName(Objects.requireNonNull(file.getOriginalFilename()));

        String dirPath = System.getProperty("user.dir") + File.separator + "uploads";
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();

        String filePath = dirPath + File.separator + fileName;
        File dest = new File(filePath);
        file.transferTo(dest);

        String url = "/uploads/" + fileName;

        Attachment attachment = Attachment.builder()
                .fileName(fileName)
                .filePath(filePath)
                .fileSize(file.getSize())
                .url(url)
                .build();
        return attachmentRepository.save(attachment);
    }
}
