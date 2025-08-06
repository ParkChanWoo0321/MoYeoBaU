package com.example.seosancomplain.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportScheduler {

    // 매달 1일 0시에 실행 (cron 표현식)
    @Scheduled(cron = "0 0 0 1 * ?")
    public void generateMonthlyReport() {
        log.info("월간 민원 리포트 생성 작업 실행!");
        // 실제 통계/리포트 생성 로직 추가
    }
}
