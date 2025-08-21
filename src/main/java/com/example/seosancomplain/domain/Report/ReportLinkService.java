package com.example.seosancomplain.domain.Report;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class ReportLinkService {
    @Value("${report.base-url}") private String baseUrl;
    @Value("${report.daily-pattern}") private String dailyPattern;
    @Value("${report.monthly-pattern}") private String monthlyPattern;

    public String buildDailyUrl(LocalDate date) {
        return baseUrl + dailyPattern
                .replace("{yyyy}", String.format("%04d", date.getYear()))
                .replace("{MM}", String.format("%02d", date.getMonthValue()))
                .replace("{dd}", String.format("%02d", date.getDayOfMonth()))
                .replace("{yyyyMMdd}", String.format("%04d%02d%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth()));
    }

    public String buildMonthlyUrl(YearMonth ym) {
        return baseUrl + monthlyPattern
                .replace("{yyyy}", String.format("%04d", ym.getYear()))
                .replace("{MM}", String.format("%02d", ym.getMonthValue()))
                .replace("{yyyyMM}", String.format("%04d%02d", ym.getYear(), ym.getMonthValue()));
    }
}
