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

import com.nhnacademy.daisobatch.dto.user.GradeChangeDto;
import com.nhnacademy.daisobatch.entity.user.User;
import com.nhnacademy.daisobatch.entity.user.UserGradeHistory;
import com.nhnacademy.daisobatch.repository.order.OrderRepository;
import com.nhnacademy.daisobatch.repository.user.UserGradeHistoryRepository;
import com.nhnacademy.daisobatch.type.OrderStatus;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
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
public class GradeChangeBatch {

    private final JobRepository jobRepository;

    private final PlatformTransactionManager platformTransactionManager;

    private final EntityManagerFactory entityManagerFactory;    // JPA Reader

    private final DataSource dataSource;                        // JDBC Writer

    private final OrderRepository orderRepository;
    private final UserGradeHistoryRepository userGradeHistoryRepository;

    private static final int CHUNK_SIZE = 1000;

    private static final long GRADE_GENERAL_THRESHOLD = 100_000L;
    private static final long GRADE_ROYAL_THRESHOLD = 200_000L;
    private static final long GRADE_GOLD_THRESHOLD = 300_000L;

    @Bean
    public Job gradeChangeJob() {
        return new JobBuilder("gradeChangeJob", jobRepository)
                .start(gradeChangeStep())
                .build();
    }

    @Bean
    public Step gradeChangeStep() {
        return new StepBuilder("gradeChangeStep", jobRepository)
                .<User, GradeChangeDto>chunk(CHUNK_SIZE, platformTransactionManager)
                .reader(gradeChangeReader())
                .processor(gradeChangeProcessor())
                .writer(gradeChangeWriter())
                .build();
    }

    @Bean
    @StepScope  // Step이 실행될 때 빈 생성, 끝나면 사라짐
    public JpaPagingItemReader<User> gradeChangeReader() {
        return new JpaPagingItemReaderBuilder<User>()
                // 1. 각 계정의 가장 '최신' 상태 변경 이력의 ID를 찾습니다.
                // 2. 위에서 찾은 ID로 실제 상태 정보를 조인합니다.
                // 3. 조건 필터링: 현재 상태가 ACTIVE인 계정
                .queryString("SELECT u FROM User u " +
                        "JOIN u.account a " +
                        "JOIN AccountStatusHistory ash ON ash.account = a " +
                        "WHERE ash.changedAt = (SELECT MAX(h.changedAt) FROM AccountStatusHistory h WHERE h.account = a) " +
                        "AND ash.status.statusName = 'ACTIVE' " +
                        "ORDER BY u.userCreatedId ASC") // 페이징 시 동일한 정렬이 보장되어야 페이지 경계에서 누락이나 중복이 발생하지 않음
                .pageSize(CHUNK_SIZE)
                .entityManagerFactory(entityManagerFactory)
                .name("gradeChangeReader")
                .build();
    }

    @Bean
    @StepScope  // Step이 실행될 때 빈 생성, 끝나면 사라짐
    public ItemProcessor<User, GradeChangeDto> gradeChangeProcessor() {
        return user -> {
            // 기준 기간 (최근 3개월)
            ZonedDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3).atZone(ZoneId.of("Asia/Seoul"));

            // 순수 주문 금액 계산 (DB 조회)
            long amount = calculateAmount(user.getUserCreatedId(), threeMonthsAgo, OrderStatus.COMPLETED);

            // 목표 등급 ID 계산
            Long newGradeId = newGradeId(amount);

            // 현재 등급 조회
            Long currentGradeId = getCurrentGradeId(user.getUserCreatedId());

            // 등급이 동일 하면 패스 (Writer로 넘기지 않음)
            if (newGradeId.equals(currentGradeId)) {
                return null;
            }

            // 등급 변동 사유
            String reason = String.format("최근 3개월 구매 실적 반영 (%d원)", amount);

            // 변경 대상이면 DTO 생성
            return new GradeChangeDto(user.getUserCreatedId(), newGradeId, reason);
        };
    }

    @Bean
    @StepScope  // Step이 실행될 때 빈 생성, 끝나면 사라짐
    public JdbcBatchItemWriter<GradeChangeDto> gradeChangeWriter() {
        return new JdbcBatchItemWriterBuilder<GradeChangeDto>()
                .dataSource(dataSource)
                .sql("INSERT INTO UserGradeHistories (user_created_id, grade_id, reason, changed_at)" +
                        "VALUES (:userCreatedId, :newGradeId, :reason, :changedAt)")
                .itemSqlParameterSourceProvider(gradeChangeDto -> {
                    // 파라미터 매핑 (Entity -> SQL 파라미터)
                    Map<String, Object> params = new HashMap<>();

                    params.put("userCreatedId", gradeChangeDto.userCreatedId()); // Users의 PK
                    params.put("newGradeId", gradeChangeDto.gradeId());          // 변경될 Grades의 PK
                    params.put("reason", gradeChangeDto.reason());               // 변경 사유
                    params.put("changedAt", LocalDateTime.now());                // 변경 일시

                    return new MapSqlParameterSource(params);
                })
                .build();
    }

    // 순수 주문 금액 계산
    private long calculateAmount(Long userCreatedId, ZonedDateTime since, OrderStatus orderStatus) {
        return orderRepository.calculateTotalAmount(userCreatedId, since, orderStatus);
    }

    // 금액별 등급 ID 매핑
    private Long newGradeId(long amount) {
        if (amount >= GRADE_GOLD_THRESHOLD) {
            return 4L;  // PLATINUM
        }
        if (amount >= GRADE_ROYAL_THRESHOLD) {
            return 3L;  // GOLD
        }
        if (amount >= GRADE_GENERAL_THRESHOLD) {
            return 2L;  // ROYAL
        }

        return 1L;  // GENERAL
    }

    // 회원의 현재 등급 ID 조회
    private Long getCurrentGradeId(Long userCreatedId) {
        // 이력 조회
        UserGradeHistory history =
                userGradeHistoryRepository.findTopByUser_UserCreatedIdOrderByChangedAtDesc(userCreatedId);

        if (history == null) {
            return 1L;
        }

        return history.getGrade().getGradeId();
    }

}
