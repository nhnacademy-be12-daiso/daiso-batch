/*
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 * + Copyright 2025. NHN Academy Corp. All rights reserved.
 * + * While every precaution has been taken in the preparation of this resource,  assumes no
 * + responsibility for errors or omissions, or for damages resulting from the use of the information
 * + contained herein
 * + No part of this resource may be reproduced, stored in a retrieval system, or transmitted, in any
 * + form or by any means, electronic, mechanical, photocopying, recording, or otherwise, without the
 * + prior written permission.
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

package com.nhnacademy.daisobatch.scheduler.coupon;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
    @SchedulerLock(
            name = "birthdayJob", // 이 이름으로 DB에서 구분 (유니크해야 함)
            lockAtLeastFor = "30s",  // 작업이 0.1초 만에 끝나도 최소 30초는 락을 유지 (다른 서버가 혹시라도 실행하는 것 방지)
            lockAtMostFor = "10m"    // 10분이 지나도 안 끝나면 락 강제 해제 (데드락 방지)
    )
    public void runBirthdayJob() {
        try {
            // 같은 Job을 여러 번 실행하려면 파라미터(시간 등)가 달라야 함
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(birthdayCouponJob, jobParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
