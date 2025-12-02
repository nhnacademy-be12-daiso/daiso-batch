package com.nhnacademy.daisobatch.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobController {

    private final JobLauncher jobLauncher;
    private final Job birthdayCouponJob;

    public JobController(JobLauncher jobLauncher, Job birthdayCouponJob) {
        this.jobLauncher = jobLauncher;
        this.birthdayCouponJob = birthdayCouponJob;
    }

    @GetMapping("/batch/birthday")
    public String runBirthdayJob(){
        try{
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis()) // 중복 실행 가능하도록 시간 파라미터 추가
                    .toJobParameters();
            jobLauncher.run(birthdayCouponJob, jobParameters);
            return "생일 쿠폰 배치 실행 완료!";
        } catch (Exception e){
            e.printStackTrace();
            return "배치 실행 실패: " + e.getMessage();
        }
    }
}
