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

package com.nhnacademy.daisobatch.batch.user;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = {
        "/sql/user/grade-schema.sql",
        "/sql/user/grade-data.sql"
})
public class GradeChangeBatchTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("gradeChangeJob")
    private Job gradeChangeJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(gradeChangeJob);
    }

    @Test
    @DisplayName("등급 산정 배치 실행 시, 구매 금액에 따라 등급이 변경되고 이력이 저장되어야 함")
    void test1() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String baseDateStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", baseDateStr)
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 15만원 구매자가 ROYAL(2)인지 확인
        Long royalUserGrade = jdbcTemplate.queryForObject(
                "SELECT current_grade_id FROM Users WHERE user_created_id = 10", Long.class);
        assertThat(royalUserGrade).isEqualTo(2L);

        // 35만원 구매자가 PLATINUM(4)인지 확인
        Long platinumUserGrade = jdbcTemplate.queryForObject(
                "SELECT current_grade_id FROM Users WHERE user_created_id = 20", Long.class);
        assertThat(platinumUserGrade).isEqualTo(4L);

        // PENDING 상태 주문은 GENERAL(1)을 유지하는지 확인
        Long pendingUserGrade = jdbcTemplate.queryForObject(
                "SELECT current_grade_id FROM Users WHERE user_created_id = 30", Long.class);
        assertThat(pendingUserGrade).isEqualTo(1L);

        // 이력 저장 확인
        Map<String, Object> history = jdbcTemplate.queryForMap(
                "SELECT grade_id, reason FROM UserGradeHistories WHERE user_created_id = 10");
        assertThat(history.get("grade_id")).isEqualTo(2L);
        assertThat(history.get("reason").toString()).contains("150000원");
    }

}
