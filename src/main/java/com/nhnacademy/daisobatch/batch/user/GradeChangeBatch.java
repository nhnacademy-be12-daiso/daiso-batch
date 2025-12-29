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

import com.nhnacademy.daisobatch.dto.user.GradeCalculationDto;
import com.nhnacademy.daisobatch.dto.user.GradeChangeDto;
import com.nhnacademy.daisobatch.listener.user.GradeChunkListener;
import com.nhnacademy.daisobatch.listener.user.GradeSkipListener;
import com.nhnacademy.daisobatch.type.user.Grade;
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
public class GradeChangeBatch {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;
    private final DataSource dataSource;

    @Value("${batch.grade.chunk-size:1000}")
    private int chunkSize;

    @Value("${batch.grade.days:90}")
    private int days;

    @Value("${batch.grade.threshold.royal:100000}")
    private long gradeRoyalThreshold;

    @Value("${batch.grade.threshold.gold:200000}")
    private long gradeGoldThreshold;

    @Value("${batch.grade.threshold.platinum:300000}")
    private long gradePlatinumThreshold;

    @Bean
    public Job gradeChangeJob(Step gradeChangeStep) {
        return new JobBuilder("gradeChangeJob", jobRepository)
                .start(gradeChangeStep)
                .build();
    }

    @Bean
    @JobScope   // Job이 실행될 때 빈 생성, 끝나면 사라짐
    public Step gradeChangeStep(JdbcPagingItemReader<GradeCalculationDto> gradeChangeReader,
                                ItemProcessor<GradeCalculationDto, GradeChangeDto> gradeChangeProcessor,
                                CompositeItemWriter<GradeChangeDto> gradeChangeWriter) {
        return new StepBuilder("gradeChangeStep", jobRepository)
                .<GradeCalculationDto, GradeChangeDto>chunk(chunkSize, platformTransactionManager)
                .reader(gradeChangeReader)          // 등급 변경 대상 회원 조회
                .processor(gradeChangeProcessor)    // 등급 산정
                .writer(gradeChangeWriter)          // 등급 변경 + 이력 저장
                .faultTolerant()        // 결함 허용: 일부 데이터 오류 발생 시에도 Step 중단 방지
                .skip(Exception.class)  // 모든 예외에 대해 스킵 허용
                .skipLimit(100)         // 최대 100건까지 오류 허용
                .listener(new GradeChunkListener())    // Chunk 단위 성공/실패 로깅
                .listener(new GradeSkipListener())
                .build();
    }

    @Bean
    @StepScope  // Step이 실행될 때 빈 생성, 끝나면 사라짐
    public JdbcPagingItemReader<GradeCalculationDto> gradeChangeReader(
            @Value("#{jobParameters['baseDate']}") String baseDateStr) {
        LocalDateTime baseDate = (baseDateStr != null) ? LocalDateTime.parse(baseDateStr) : LocalDateTime.now();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("threeMonthsAgo", baseDate.minusDays(days));

        return new JdbcPagingItemReaderBuilder<GradeCalculationDto>()
                .dataSource(dataSource)
                .queryProvider(gradeQueryProvider())    // 페이징 쿼리 제공
                .parameterValues(parameters)
                .pageSize(chunkSize)    // 페이지 크기 = Chunk 크기
                .name("gradeChangeReader")
                .saveState(false)   // 페이징 꼬임 방지: 재시작 시 항상 0페이지부터 읽기
                .rowMapper(((rs, rowNum) -> new GradeCalculationDto(    // 결과 > DTO 매핑
                        rs.getLong("user_created_id"),
                        rs.getLong("current_grade_id"),
                        rs.getLong("net_amount")    // SQL에서 계산된 순수 금액
                )))
                .build();
    }

    @Bean
    public PagingQueryProvider gradeQueryProvider() {
        SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean();
        factoryBean.setDataSource(dataSource);

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("user_created_id", Order.ASCENDING);   // 페이징 안정성을 위한 정렬 키

        // 순수금액 = 주문금액 - (쿠폰 + 배송비 + 취소금액 + 포장비)
        // OrderDetails 테이블의 price 사용
        factoryBean.setSelectClause("SELECT u.user_created_id, u.current_grade_id, " +
                "COALESCE(SUM(od.price), 0) as net_amount");
        factoryBean.setFromClause("FROM Users u " +
                "JOIN Accounts a ON u.user_created_id = a.user_created_id " +
                "LEFT JOIN Orders o ON u.user_created_id = o.user_created_id AND o.order_date >= :threeMonthsAgo " +
                "LEFT JOIN OrderDetails od ON o.order_id = od.order_id AND od.order_detail_status = 'COMPLETED'");
        factoryBean.setWhereClause("WHERE a.current_status_id = (" +
                "SELECT status_id FROM Statuses WHERE status_name = 'ACTIVE')");
        factoryBean.setGroupClause("GROUP BY u.user_created_id, u.current_grade_id");
        factoryBean.setSortKeys(sortKeys);

        try {
            return factoryBean.getObject();

        } catch (Exception e) {
            throw new RuntimeException("[GradeChangeBatch] Query Provider 생성 실패", e);
        }
    }

    @Bean
    @StepScope  // Step이 실행될 때 빈 생성, 끝나면 사라짐
    public ItemProcessor<GradeCalculationDto, GradeChangeDto> gradeChangeProcessor() {
        return item -> {
            Long newGradeId = calculateNewGradeId(item.netAmount());

            // 등급 변화가 없으면 skip
            if (newGradeId.equals(item.currentGradeId())) {
                return null;
            }

            String reason = String.format("최근 3개월 순수 구매액 %d원 기준 등급 조정", item.netAmount());

            return new GradeChangeDto(item.userCreatedId(), newGradeId, reason);
        };
    }

    @Bean
    @StepScope  // Step이 실행될 때 빈 생성, 끝나면 사라짐
    public CompositeItemWriter<GradeChangeDto> gradeChangeWriter(
            JdbcBatchItemWriter<GradeChangeDto> updateUserGradeWriter,
            JdbcBatchItemWriter<GradeChangeDto> insertGradeHistoryWriter) {
        return new CompositeItemWriterBuilder<GradeChangeDto>()
                .delegates(Arrays.asList(
                        updateUserGradeWriter,      // Users 테이블 업데이트
                        insertGradeHistoryWriter    // UserGradeHistories 테이블 인서트
                ))
                .build();
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<GradeChangeDto> updateUserGradeWriter() {
        return new JdbcBatchItemWriterBuilder<GradeChangeDto>()
                .dataSource(dataSource)
                .sql("UPDATE Users SET current_grade_id = :gradeId " +
                        "WHERE user_created_id = :userCreatedId AND current_grade_id = :currentGradeId")
                .beanMapped()
                .build();
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<GradeChangeDto> insertGradeHistoryWriter(
            @Value("#{jobParameters['baseDate']}") String baseDateStr) {
        LocalDateTime baseDate = (baseDateStr != null) ? LocalDateTime.parse(baseDateStr) : LocalDateTime.now();

        return new JdbcBatchItemWriterBuilder<GradeChangeDto>()
                .dataSource(dataSource)
                .sql("INSERT INTO UserGradeHistories (user_created_id, grade_id, reason, changed_at) " +
                        "VALUES (:userCreatedId, :gradeId, :reason, :changedAt)")
                .itemSqlParameterSourceProvider(item -> {
                    Map<String, Object> params = new HashMap<>();
                    params.put("userCreatedId", item.userCreatedId());  // 회원 ID
                    params.put("gradeId", item.gradeId());              // 등급 ID
                    params.put("reason", item.reason());                // 등급 변경 사유
                    params.put("changedAt", baseDate);                  // 등급 변경 시간

                    return new MapSqlParameterSource(params);
                })
                .build();
    }

    // 금액별 등급 ID 매핑
    private Long calculateNewGradeId(long amount) {
        if (amount >= gradePlatinumThreshold) {
            return Grade.PLATINUM.getId();
        }
        if (amount >= gradeGoldThreshold) {
            return Grade.GOLD.getId();
        }
        if (amount >= gradeRoyalThreshold) {
            return Grade.ROYAL.getId();
        }

        return Grade.GENERAL.getId();
    }

}
