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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class BirthdayBatchSchedulerV2 {

    private final JobLauncher jobLauncher;
    private final Job birthdayCouponJob;

    // 매월 1일 0시 0분 0초에 실행
    @Scheduled(cron = "0 0 0 1 * *")
    @SchedulerLock(
            name = "birthdayCouponJob", // 이 이름으로 DB에서 구분 (유니크해야 함)
            lockAtLeastFor = "30s",  // 작업이 0.1초 만에 끝나도 최소 30초는 락을 유지 (다른 서버가 혹시라도 실행하는 것 방지)
            lockAtMostFor = "10m"    // 10분이 지나도 안 끝나면 락 강제 해제 (데드락 방지)
    )
    public void runBirthdayCouponJob() {
        log.info("=== 생일 쿠폰 배치 시작 ===");
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLocalDateTime("runDate", LocalDateTime.now())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(birthdayCouponJob, params);

            if(execution.getStatus() == BatchStatus.COMPLETED){
                log.info("=== 생일 쿠폰 배치 성공 ===");
            } else {
                log.error("=== 생일 쿠폰 배치 실패 ===");
            }

        } catch (Exception e) {
            log.error("배치 실행 중 오류", e);
        }
    }
}
