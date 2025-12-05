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

package com.nhnacademy.daisobatch.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class JobController {

    private final JobLauncher jobLauncher;
    private final Job birthdayCouponJob;
    private final Job dormantAccountJob;

    @GetMapping("/batch/birthday")
    public String runBirthdayJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis()) // 중복 실행 가능하도록 시간 파라미터 추가
                    .toJobParameters();
            jobLauncher.run(birthdayCouponJob, jobParameters);
            return "생일 쿠폰 배치 실행 완료!";
        } catch (Exception e) {
            e.printStackTrace();
            return "배치 실행 실패: " + e.getMessage();
        }
    }

    @GetMapping("/batch/dormant")
    public String runDormantJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(dormantAccountJob, jobParameters);

            return "휴면 계정 전환 배치 작업 완료";

        } catch (Exception e) {
            return "배치 실행 실패: " + e.getMessage();
        }
    }

}
