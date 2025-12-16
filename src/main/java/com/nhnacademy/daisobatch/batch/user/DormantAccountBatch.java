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

import com.nhnacademy.daisobatch.entity.user.Account;
import com.nhnacademy.daisobatch.entity.user.Status;
import com.nhnacademy.daisobatch.repository.user.StatusRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

@RequiredArgsConstructor
@Configuration
public class DormantAccountBatch {

    private final JobRepository jobRepository;

    private final PlatformTransactionManager platformTransactionManager;

    private final EntityManagerFactory entityManagerFactory;    // JPA Reader

    private final DataSource dataSource;                        // JDBC Writer

    private final StatusRepository statusRepository;

    private static final int CHUNK_SIZE = 1000;

    @Bean
    public Job dormantAccountJob() {
        return new JobBuilder("dormantAccountJob", jobRepository)
                .start(dormantAccountStep())
                .build();
    }

    @Bean
    @JobScope   // Job이 실행될 때 빈 생성, 끝나면 사라짐
    public Step dormantAccountStep() {
        return new StepBuilder("dormantAccountStep", jobRepository)
                .<Account, Account>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(dormantAccountReader())
                .processor(dormantAccountProcessor())
                .writer(dormantAccountWriter())
                .build();
    }

    @Bean
    @StepScope  // Step이 실행될 때 빈 생성, 끝나면 사라짐
    public JpaPagingItemReader<Account> dormantAccountReader() {
        // 파라미터 설정 (지금으로부터 90일 전)
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("lastLoginAtBefore", LocalDateTime.now().minusDays(90));

        return new JpaPagingItemReaderBuilder<Account>()
                // 1. 각 유저별로 가장 '최신' 상태 변경 이력의 ID를 찾습니다.
                // 2. 위에서 찾은 ID로 실제 상태 정보를 조인합니다.
                // 3. 조건 필터링: 로그인 날짜 기준 + 현재 상태가 ACTIVE인 사람

                // 로그인 날짜로 검색할 때, 유저별 상태 이력 조인할 때 인덱스 필수!!
                .queryString("SELECT a FROM Accounts a " +
                        "JOIN AccountStatusHistories ash ON ash.account = a " +
                        "WHERE a.lastLoginAt < :lastLoginAtBefore " +
                        "AND ash.changedAt = (SELECT MAX(h.changedAt) FROM AccountStatusHistories h WHERE h.account = a) " +
                        "AND ash.status.statusName = 'ACTIVE'" +
                        "ORDER BY a.loginId ASC")   // 페이징 시 동일한 정렬이 보장되어야 페이지 경계에서 누락이나 중복이 발생하지 않음
                .parameterValues(parameters)
                .pageSize(CHUNK_SIZE)
                .entityManagerFactory(entityManagerFactory)
                .name("dormantAccountReader")
                .build();
    }

    @Bean
    @StepScope  // Step이 실행될 때 빈 생성, 끝나면 사라짐
    public ItemProcessor<Account, Account> dormantAccountProcessor() {
        // Reader에서 읽은 데이터의 타입을 변환하거나 필터링이 필요할 때
        return account -> account;

    }

    @Bean
    @StepScope  // Step이 실행될 때 빈 생성, 끝나면 사라짐
    public JdbcBatchItemWriter<Account> dormantAccountWriter() {
        Status dormantStatus = statusRepository.findByStatusName("DORMANT")
                .orElseThrow(() -> new RuntimeException("존재하지 않는 상태"));

        long dormantStatusId = dormantStatus.getStatusId();

        return new JdbcBatchItemWriterBuilder<Account>()
                .dataSource(dataSource)
                .sql("INSERT INTO AccountStatusHistories (login_id, status_id, changed_at) VALUES (:loginId, :statusId, :changedAt)")
                .itemSqlParameterSourceProvider(account -> {
                    // 파라미터 매핑 (Entity -> SQL 파라미터)
                    Map<String, Object> params = new HashMap<>();

                    params.put("loginId", account.getLoginId()); // Account의 PK
                    params.put("statusId", dormantStatusId);       // 위에서 조회한 휴면 상태 PK
                    params.put("changedAt", LocalDateTime.now());  // 변경 일시

                    return new MapSqlParameterSource(params);
                })
                .build();
    }

}
