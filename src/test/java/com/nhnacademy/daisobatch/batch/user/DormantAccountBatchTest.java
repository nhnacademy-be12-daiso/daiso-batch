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

import static org.assertj.core.api.Assertions.assertThat;

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
@Sql(scripts = "/sql/user/dormant-data.sql")
public class DormantAccountBatchTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("dormantAccountJob")
    private Job dormantAccountJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(dormantAccountJob);
    }

    @Test
    @DisplayName("90일 이상 미접속이면서 상태가 ACTIVE인 계정은 휴면 상태 이력이 생성되어야 함")
    void test1() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        String targetSql = "SELECT status_id FROM AccountStatusHistories " +
                "WHERE login_id = 'targetUser' " +
                "ORDER BY changed_at DESC LIMIT 1";
        Long targetStatusId = jdbcTemplate.queryForObject(targetSql, Long.class);
        assertThat(targetStatusId).isEqualTo(2L);   // DORMANT

        String countSql = "SELECT COUNT(*) FROM AccountStatusHistories WHERE login_id = 'targetUser'";
        Integer count = jdbcTemplate.queryForObject(countSql, Integer.class);
        assertThat(count).isEqualTo(2);

        String activeSql = "SELECT status_id FROM AccountStatusHistories " +
                "WHERE login_id = 'activeUser' " +
                "ORDER BY changed_at DESC LIMIT 1";
        Long activeStatusId = jdbcTemplate.queryForObject(activeSql, Long.class);
        assertThat(activeStatusId).isEqualTo(1L);   // ACTIVE
    }

}
