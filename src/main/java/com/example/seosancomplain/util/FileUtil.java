package com.example.seosancomplain.util;

import java.util.UUID;

public class FileUtil {
    public static String generateUniqueFileName(String originalName) {
        String ext = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx > 0) ext = originalName.substring(dotIdx);
        return UUID.randomUUID() + ext;
    }
}
