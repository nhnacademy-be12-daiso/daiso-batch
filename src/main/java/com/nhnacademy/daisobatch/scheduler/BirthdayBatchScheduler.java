package com.nhnacademy.daisobatch.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BirthdayBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job birthdayCouponJob;

    public BirthdayBatchScheduler(JobLauncher jobLauncher, Job birthdayCouponJob) {
        this.jobLauncher = jobLauncher;
        this.birthdayCouponJob = birthdayCouponJob;
    }

    // 매월 1월 0시 0분 0초에 실행
    @Scheduled(cron = "0 0 0 1 * *")
    public void runBirthdayJob(){
        try {
            // 같은 Job을 여러 번 실행하려면 파라미터(시간 등)가 달라야 함
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(birthdayCouponJob, jobParameters);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
