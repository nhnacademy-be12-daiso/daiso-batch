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
public class DormantAccountScheduler {    // 휴면 계정 자동 전환 스케줄러

    private final JobLauncher jobLauncher;
    private final Job dormantAccountJob;

    //  Cron 표현식 설명 (cron = "초 분 시 일 월 요일 년")
    // ───────────────────────────────────────────────────
    // * : 모든 값
    // ? : 특정 값 없음
    // - : 범위 (예: MON-WED)
    // , : 여러 값 지정 (예: MON,WED,FRI)
    // / : 주기 설정 (예: 0/5 → 0분부터 5분 간격)
    // L : 마지막 (예: 월의 마지막 날, 요일의 마지막 요일)
    // W : 가장 가까운 평일 (예: 15W → 15일 기준 가장 가까운 평일)
    // # : 몇 번째 주의 요일 (예: 3#2 → 둘째 주 수요일)
    // ───────────────────────────────────────────────────
    @Scheduled(cron = "0 0 4 * * *")    // 매일 새벽 4시에 휴면 계정 전환 배치 실행
    @SchedulerLock(name = "dormantAccountJob", lockAtLeastFor = "30s", lockAtMostFor = "30m")
    public void runDormantAccountJob() {
        try {
            log.info(">>>>> 휴면 계정 전환 배치 시작 [{}]", LocalDateTime.now());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("baseDate", LocalDateTime.now().toString())
                    .toJobParameters();

            jobLauncher.run(dormantAccountJob, jobParameters);

            log.info("<<<<< 휴면 계정 전환 배치 완료");

        } catch (Exception e) {
            log.error("휴면 계정 전환 배치 실패", e);
        }
    }

}
