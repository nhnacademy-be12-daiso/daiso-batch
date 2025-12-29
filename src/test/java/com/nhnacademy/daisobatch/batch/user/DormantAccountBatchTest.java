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
        "/sql/user/dormant-schema.sql",
        "/sql/user/dormant-data.sql"
})
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
    @DisplayName("휴면 전환 배치 실행 시, 대상자는 DORMANT로 변경되고 비대상자는 ACTIVE를 유지해야 함")
    void test1() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        String baseDateStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("baseDate", baseDateStr)
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Accounts 테이블의 현재 상태가 DORMANT(2)인지 확인
        Map<String, Object> targetAccount = jdbcTemplate.queryForMap(
                "SELECT current_status_id FROM Accounts WHERE login_id = 'targetUser'");
        assertThat(targetAccount.get("current_status_id")).isEqualTo(2L);

        // AccountStatusHistories에 이력이 추가되었는지 확인 (총 2건: 초기 ACTIVE + 신규 DORMANT)
        Integer targetHistoryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM AccountStatusHistories WHERE login_id = 'targetUser'", Integer.class);
        assertThat(targetHistoryCount).isEqualTo(2);

        // 가장 최근 이력의 상태와 변경 시간이 baseDate와 일치하는지 확인
        Map<String, Object> latestHistory = jdbcTemplate.queryForMap(
                "SELECT status_id, changed_at FROM AccountStatusHistories WHERE login_id = 'targetUser' ORDER BY changed_at DESC LIMIT 1");
        assertThat(latestHistory.get("status_id")).isEqualTo(2L);
        assertThat(latestHistory.get("changed_at").toString()).contains(now.toLocalDate().toString());

        // Accounts 테이블의 현재 상태가 ACTIVE(1)를 유지하는지 확인
        Map<String, Object> activeAccount = jdbcTemplate.queryForMap(
                "SELECT current_status_id FROM Accounts WHERE login_id = 'activeUser'");
        assertThat(activeAccount.get("current_status_id")).isEqualTo(1L);

        // 이력이 추가되지 않았는지 확인 (초기 1건 유지)
        Integer activeHistoryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM AccountStatusHistories WHERE login_id = 'activeUser'", Integer.class);
        assertThat(activeHistoryCount).isEqualTo(1);
    }

}
