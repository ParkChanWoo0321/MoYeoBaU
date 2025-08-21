package com.example.seosancomplain.domain.Report;

import lombok.*;

@Getter @Builder @AllArgsConstructor @NoArgsConstructor
public class ReportDownloadLink {
    private String url; // PDF 다운로드 링크 (원본 사이트)
}