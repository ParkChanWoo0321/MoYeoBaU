package com.example.seosancomplain.domain.attachment;

import com.example.seosancomplain.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AttachmentService {
    private final AttachmentRepository attachmentRepository;

    @Value("${file.upload-dir:${user.dir}/uploads}")
    private String uploadDir;

    public Attachment save(MultipartFile file) throws IOException {
        String fileName = FileUtil.generateUniqueFileName(Objects.requireNonNull(file.getOriginalFilename()));

        Path base = Paths.get(uploadDir).toAbsolutePath().normalize();
        File dir = base.toFile();
        if (!dir.exists()) dir.mkdirs();

        String filePath = base.resolve(fileName).toString();
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

    public List<Attachment> saveAll(List<MultipartFile> files) throws IOException {
        List<Attachment> results = new ArrayList<>();
        if (files == null || files.isEmpty()) return results;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            Attachment att = save(file);
            results.add(att);
        }
        return results;
    }

    public List<String> saveAllAndReturnUrls(List<MultipartFile> files) throws IOException {
        List<Attachment> atts = saveAll(files);
        List<String> urls = new ArrayList<>(atts.size());
        for (Attachment a : atts) {
            urls.add(a.getUrl());
        }
        return urls;
    }
}
