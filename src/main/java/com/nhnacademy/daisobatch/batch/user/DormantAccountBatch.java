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

import com.nhnacademy.daisobatch.dto.user.DormantAccountDto;
import com.nhnacademy.daisobatch.listener.CustomChunkListener;
import com.nhnacademy.daisobatch.type.user.Status;
import java.time.LocalDateTime;
import java.util.Arrays;
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
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.batch.item.support.builder.CompositeItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

@RequiredArgsConstructor
@Configuration
public class DormantAccountBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final DataSource dataSource;

    @Value("${batch.dormant.chunk-size:1000}")
    private int chunkSize;

    @Value("${batch.dormant.days:90}")
    private int days;

    @Bean
    public Job dormantAccountJob(Step dormantAccountStep) {
        return new JobBuilder("dormantAccountJob", jobRepository)
                .start(dormantAccountStep)
                .build();
    }

    @Bean
    @JobScope   // Job이 실행될 때 빈 생성, 끝나면 사라짐
    public Step dormantAccountStep(JdbcPagingItemReader<DormantAccountDto> dormantAccountReader,
                                   ItemProcessor<DormantAccountDto, DormantAccountDto> dormantAccountProcessor,
                                   CompositeItemWriter<DormantAccountDto> dormantAccountWriter) {
        return new StepBuilder("dormantAccountStep", jobRepository)
                .<DormantAccountDto, DormantAccountDto>chunk(chunkSize, platformTransactionManager)
                .reader(dormantAccountReader)       // 휴면 대상 계정 조회
                .processor(dormantAccountProcessor) // 휴면 대상 계정 상태 확인
                .writer(dormantAccountWriter)       // 상태 변경 + 이력 저장
                .faultTolerant()        // 결함 허용: 일부 데이터 오류 발생 시에도 Step 중단 방지
                .skip(Exception.class)  // 모든 예외에 대해 스킵 허용
                .skipLimit(100)         // 최대 100건까지 오류 허용
                .listener(new CustomChunkListener())    // Chunk 단위 성공/실패 로깅
                .build();
    }

    @Bean
    @StepScope  // Step이 실행될 때 빈 생성, 끝나면 사라짐
    public JdbcPagingItemReader<DormantAccountDto> dormantAccountReader(
            @Value("#{jobParameters['baseDate']}") String baseDateStr) {
        LocalDateTime baseDate = (baseDateStr != null) ? LocalDateTime.parse(baseDateStr) : LocalDateTime.now();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("lastLoginAtBefore", baseDate.minusDays(days));

        return new JdbcPagingItemReaderBuilder<DormantAccountDto>()
                .dataSource(dataSource)
                .queryProvider(dormantQueryProvider())  // 페이징 쿼리 제공
                .parameterValues(parameters)
                .pageSize(chunkSize)   // 페이지 크기 = Chunk 크기
                .name("dormantAccountReader")
                .saveState(false)   // 페이징 꼬임 방지: 재시작 시 항상 0페이지부터 읽기
                .rowMapper((rs, rowNum) -> new DormantAccountDto(   // 결과 > DTO 매핑
                        rs.getString("login_id"),
                        rs.getTimestamp("last_login_at").toLocalDateTime(),
                        rs.getLong("current_status_id")
                ))
                .build();
    }

    @Bean
    public PagingQueryProvider dormantQueryProvider() {
        SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
        factoryBean.setDataSource(dataSource);

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("login_id", Order.ASCENDING);  // 페이징 안정성을 위한 정렬 키

        factoryBean.setSelectClause("SELECT login_id, last_login_at, current_status_id");
        factoryBean.setFromClause("FROM Accounts");
        factoryBean.setWhereClause("WHERE last_login_at < :lastLoginAtBefore");
        factoryBean.setSortKeys(sortKeys);

        try {
            return factoryBean.getObject();

        } catch (Exception e) {
            throw new RuntimeException("[DormantAccountBatch] Query Provider 생성 실패", e);
        }
    }

    @Bean
    @StepScope  // Step이 실행될 때 빈 생성, 끝나면 사라짐
    public ItemProcessor<DormantAccountDto, DormantAccountDto> dormantAccountProcessor() {
        return item -> {
            // 현재 상태가 ACTIVE라면 skip
            if (item.currentStatusId().equals(Status.ACTIVE.getId())) {
                return null;
            }

            return item;
        };
    }

    @Bean
    @StepScope  // Step이 실행될 때 빈 생성, 끝나면 사라짐
    public CompositeItemWriter<DormantAccountDto> dormantAccountWriter(
            JdbcBatchItemWriter<DormantAccountDto> updateAccountStatusWriter,
            JdbcBatchItemWriter<DormantAccountDto> insertStatusHistoryWriter) {
        return new CompositeItemWriterBuilder<DormantAccountDto>()
                .delegates(Arrays.asList(
                        updateAccountStatusWriter, // Accounts 테이블 업데이트
                        insertStatusHistoryWriter  // AccountStatusHistories 테이블 인서트
                ))
                .build();
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<DormantAccountDto> updateAccountStatusWriter() {
        return new JdbcBatchItemWriterBuilder<DormantAccountDto>()
                .dataSource(dataSource)
                .sql("UPDATE Accounts SET current_status_id = (" +
                        "SELECT status_id FROM Statuses WHERE status_name = 'DORMANT'" +
                        ") WHERE login_id = :loginId AND last_login_at = :lastLoginAt")
                .beanMapped()
                .assertUpdates(false)    // 만약 Reader가 읽은 후 Writer가 실행되기 직전에 사용자가 로그인하여 상태가 변했을 때
                .build();
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<DormantAccountDto> insertStatusHistoryWriter(
            @Value("#{jobParameters['baseDate']}") String baseDateStr) {
        LocalDateTime baseDate = (baseDateStr != null) ? LocalDateTime.parse(baseDateStr) : LocalDateTime.now();

        return new JdbcBatchItemWriterBuilder<DormantAccountDto>()
                .dataSource(dataSource)
                .sql("INSERT INTO AccountStatusHistories (login_id, status_id, changed_at) " +
                        "VALUES (:loginId, (SELECT status_id FROM Statuses WHERE status_name = 'DORMANT'), :changedAt)")
                .itemSqlParameterSourceProvider(item -> {
                    Map<String, Object> params = new HashMap<>();
                    params.put("loginId", item.loginId());  // 계정 로그인 ID
                    params.put("changedAt", baseDate);      // 상태 변경 시간

                    return new MapSqlParameterSource(params);
                })
                .build();
    }

}
