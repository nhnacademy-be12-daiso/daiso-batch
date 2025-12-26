package com.nhnacademy.daisobatch.scheduler.coupon;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class BirthdayBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job birthdayCouponJob;   // ✅ 여기엔 “선택된 Job”만 들어옴

    // ✅ 생성자 파라미터에 Qualifier를 붙여야 100% 해결됨
    public BirthdayBatchScheduler(
            JobLauncher jobLauncher,
            @Qualifier("birthdayCouponJobMSA") Job birthdayCouponJob // 또는 DB면 birthdayCouponJobDB
    ) {
        this.jobLauncher = jobLauncher;
        this.birthdayCouponJob = birthdayCouponJob;
    }

    @Scheduled(cron = "0 0 0 1 * *")
    @SchedulerLock(
            name = "birthdayCouponJobMSA", // ✅ Job과 맞추는 게 좋음
            lockAtLeastFor = "30s",
            lockAtMostFor = "30m"
    )
    public void runBirthdayCouponJob() {
        log.info("=== 생일 쿠폰 배치 시작 ===");
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(birthdayCouponJob, params);

            log.info("=== 생일 쿠폰 배치 종료: status={} ===", execution.getStatus());
        } catch (Exception e) {
            log.error("배치 실행 중 오류", e);
        }
    }
}
