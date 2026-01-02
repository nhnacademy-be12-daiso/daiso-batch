/*
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 * + Copyright 2026. NHN Academy Corp. All rights reserved.
 * + * While every precaution has been taken in the preparation of this resource,  assumes no
 * + responsibility for errors or omissions, or for damages resulting from the use of the information
 * + contained herein
 * + No part of this resource may be reproduced, stored in a retrieval system, or transmitted, in any
 * + form or by any means, electronic, mechanical, photocopying, recording, or otherwise, without the
 * + prior written permission.
 * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */

package com.nhnacademy.daisobatch.batch.coupon;

import com.nhnacademy.daisobatch.dto.BirthdayUserDto;
import com.nhnacademy.daisobatch.dto.coupon.BirthdayCouponIssueDto;
import com.nhnacademy.daisobatch.dto.coupon.BirthdayPolicyDto;
import com.nhnacademy.daisobatch.listener.JobFailureNotificationListener;
import com.nhnacademy.daisobatch.listener.coupon.BirthdayChunkListener;
import com.nhnacademy.daisobatch.listener.coupon.BirthdaySkipListener;
import com.nhnacademy.daisobatch.type.CouponPolicyStatus;
import com.nhnacademy.daisobatch.type.CouponStatus;
import com.nhnacademy.daisobatch.type.CouponType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BirthdayCouponBatchDB {

    private final JdbcTemplate jdbcTemplate;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    private final JobFailureNotificationListener jobFailureNotificationListener;
    private final BirthdayChunkListener birthdayChunkListener;
    private final BirthdaySkipListener birthdaySkipListener;

    // 1. Job 정의
    @Bean(name = "birthdayCouponJobDB")
    public Job birthdayCouponJobDB(@Qualifier("birthdayCouponStepDB") Step step) {
        return new JobBuilder("birthdayCouponJobDB", jobRepository)
                .start(step)
                .listener(jobFailureNotificationListener)
                .build();
    }

    // 2. Step 정의
    @Bean(name = "birthdayCouponStepDB")
    public Step birthdayCouponStepDB(
            @Qualifier("birthdayUserReaderDB") JdbcPagingItemReader<BirthdayUserDto> reader,
            @Qualifier("birthdayUserProcessorDB") ItemProcessor<BirthdayUserDto, BirthdayCouponIssueDto> processor,
            @Qualifier("birthdayUserJdbcWriterDB") JdbcBatchItemWriter<BirthdayCouponIssueDto> writer
    ) {
        return new StepBuilder("birthdayCouponStepDB", jobRepository)
                .<BirthdayUserDto, BirthdayCouponIssueDto>chunk(1000, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()

                // Skip 전략: 중복 데이터나 알 수 없는 데이터 오류는 건너뜀
                .skip(DuplicateKeyException.class) // 이미 쿠폰이 있어 PK 충돌이 나면 Skip
                .skip(IllegalArgumentException.class) // 로직상 데이터 문제 시 Skip
                .skip(DataIntegrityViolationException.class)
                .skipLimit(100)

                // Retry 전략: DB 연결 문제나 데드락은 재시도
                .retry(TransientDataAccessException.class) // 일시적 DB 오류
                .retryLimit(3)

                .listener(birthdayChunkListener) // 생일 쿠폰 배치에서 chunk 단위로 성공/실패/진행 상황을 알려주는 로그 감시자
                .listener(birthdaySkipListener)
                .build();
    }

    // 3. Reader (DB 직접 조회 + 페이징)
    @Bean(name = "birthdayUserReaderDB")
    @StepScope
    public JdbcPagingItemReader<BirthdayUserDto> birthdayUserReaderDB(
            @Value("#{jobParameters['currentMonth']}") Integer currentMonth) {
        int month = (currentMonth != null) ? currentMonth : LocalDate.now().getMonthValue();

        Long policyId = getBirthdayPolicyIdJdbc();

        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT u.user_created_id AS user_created_id ");
        queryProvider.setFromClause("FROM Users u " +
                "JOIN Accounts a ON a.user_created_id = u.user_created_id ");
        queryProvider.setWhereClause("WHERE u.birth IS NOT NULL " +
                "AND MONTH(u.birth) = :month " +
                "AND a.current_status_id = :statusId " +
                "AND NOT EXISTS (" +
                "SELECT 1 FROM user_coupons uc " +
                "WHERE uc.user_created_id = u.user_created_id " +
                "AND uc.coupon_policy_id = :policyId)");

        Map<String, Order> sortKeys = new LinkedHashMap<>();
        sortKeys.put("u.user_created_id", Order.ASCENDING);
        queryProvider.setSortKeys(sortKeys);

        Map<String, Object> params = new HashMap<>();
        params.put("month", month);
        params.put("statusId", 1);
        params.put("policyId", policyId);

        return new JdbcPagingItemReaderBuilder<BirthdayUserDto>()
                .name("birthdayUserReaderDB")
                .dataSource(dataSource)
                .queryProvider(queryProvider)
                .parameterValues(params)
                .pageSize(1000)
                .fetchSize(1000)
                .rowMapper((rs, rowNum) -> new BirthdayUserDto(rs.getLong("user_created_id")))
                .build();
    }


    // 4. Processor: 중복 발급 장지 + 정책 캐싱
    @Bean(name = "birthdayUserProcessorDB")
    @StepScope
    public ItemProcessor<BirthdayUserDto, BirthdayCouponIssueDto> birthdayUserProcessorDB() {

        final BirthdayPolicyDto policy = getBirthdayPolicyDtoJdbc();

        return item -> {
            LocalDateTime now = LocalDateTime.now();

            // 만료일: 이번 달 마지막 날 23:59:59
            LocalDateTime expiryAt;

            if (policy.validDays() != null && policy.validDays() > 0) {
                expiryAt = now.plusDays(policy.validDays())
                        .withHour(23).withMinute(59).withSecond(59);

            } else if (policy.validEndDate() != null) {
                expiryAt = policy.validEndDate();

            } else {
                expiryAt = now.withDayOfMonth(now.toLocalDate().lengthOfMonth())
                        .withHour(23).withMinute(59).withSecond(59);
            }

            return new BirthdayCouponIssueDto(
                    item.getUserCreatedId(),
                    policy.couponPolicyId(),
                    CouponStatus.ISSUED.name(),
                    now,
                    expiryAt);
        };
    }


    // 5. Writer: DB에 저장 (null 제거 후 저장)
    @Bean(name = "birthdayUserJdbcWriterDB")
    public JdbcBatchItemWriter<BirthdayCouponIssueDto> birthdayUserJdbcWriterDB() {
        return new JdbcBatchItemWriterBuilder<BirthdayCouponIssueDto>()
                .dataSource(dataSource)
                .sql("INSERT INTO user_coupons (user_created_id, coupon_policy_id, status, issue_at, expiry_at, used_at) " +
                        "VALUES (:userId, :couponPolicyId, :status, :issuedAt, :expiryAt, NULL)")
                .itemSqlParameterSourceProvider(item -> {
                    MapSqlParameterSource ps = new MapSqlParameterSource();
                    ps.addValue("userId", item.userCreatedId());
                    ps.addValue("couponPolicyId", item.couponPolicyId());
                    ps.addValue("status", item.status());
                    ps.addValue("issuedAt", item.issuedAt());
                    ps.addValue("expiryAt", item.expiryAt());
                    return ps;
                })
                .build();
    }

    private Long getBirthdayPolicyIdJdbc() {
        String sql = "SELECT coupon_policy_id FROM coupon_policies WHERE coupon_type = ? AND policy_status = ?";

        try {
            return jdbcTemplate.queryForObject(sql, Long.class,
                    CouponType.BIRTHDAY.name(), CouponPolicyStatus.ACTIVE.name());

        } catch (Exception e) {
            throw new IllegalStateException("활성화된 생일 쿠폰 정책(ID)을 찾을 수 없습니다.");
        }
    }

    private BirthdayPolicyDto getBirthdayPolicyDtoJdbc() {
        String sql = "SELECT coupon_policy_id, valid_days, valid_end_date " +
                "FROM coupon_policies WHERE coupon_type = ? AND policy_status = ? LIMIT 1";

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new BirthdayPolicyDto(
                            rs.getLong("coupon_policy_id"),
                            rs.getObject("valid_days", Integer.class),
                            rs.getObject("valid_end_date", LocalDateTime.class)),
                    CouponType.BIRTHDAY.name(),
                    CouponPolicyStatus.ACTIVE.name());

        } catch (Exception e) {
            throw new IllegalStateException("활성화된 생일 쿠폰 정책을 찾을 수 없습니다.");
        }
    }

}
