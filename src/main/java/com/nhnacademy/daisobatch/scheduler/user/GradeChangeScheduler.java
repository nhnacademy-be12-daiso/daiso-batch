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

package com.nhnacademy.daisobatch.scheduler.user;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class GradeChangeScheduler { // 등급 변경 자동 전환 스케줄러

    private final JobLauncher jobLauncher;
    private final Job gradeChangeJob;

    @Scheduled(cron = "0 0 1 * * *")    // 매일 새벽 1시에 등급 변경 배치 실행
    @SchedulerLock(name = "gradeChangeJob", lockAtLeastFor = "30s", lockAtMostFor = "30m")
    public void runGradeChangeJob() {
        try {
            log.info(">>>>> 회원 등급 변경 배치 시작 [{}]", LocalDateTime.now());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("baseDate", LocalDateTime.now().toString())
                    .toJobParameters();

            jobLauncher.run(gradeChangeJob, jobParameters);

            log.info("<<<<< 회원 등급 변경 배치 완료");

        } catch (Exception e) {
            log.error("등급 변경 배치 실패", e);
        }
    }

}
