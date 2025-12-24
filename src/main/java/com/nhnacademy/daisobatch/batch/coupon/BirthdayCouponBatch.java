package com.nhnacademy.daisobatch.batch.coupon;

import com.nhnacademy.daisobatch.dto.BirthdayUserDto;
import com.nhnacademy.daisobatch.entity.coupon.CouponPolicy;
import com.nhnacademy.daisobatch.entity.coupon.UserCoupon;
import com.nhnacademy.daisobatch.repository.coupon.CouponPolicyRepository;
import com.nhnacademy.daisobatch.repository.coupon.UserCouponRepository;
import com.nhnacademy.daisobatch.type.CouponStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class BirthdayCouponBatch {

    private static final long BIRTHDAY_POLICY_ID = 4L;

    private final UserCouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    // Processor에서 재사용할 쿠폰 쟁책 캐시
    private CouponPolicy birthdayPolicy;

    // 1. Job 정의
    @Bean
    public Job birthdayCouponJob() {
        return new JobBuilder("birthdayCouponJob", jobRepository)
                .start(birthdayCouponStep())
                .build();
    }

    // 2. Step 정의
    @Bean
    public Step birthdayCouponStep() {
        return new StepBuilder("birthdayCouponStep", jobRepository)
                .<BirthdayUserDto, UserCoupon>chunk(500, transactionManager) // 100명씩 끊어서 처리, 1만명이명 1분 30초
                .reader(birthdayUserReader()) // 100명 읽기
                .processor(birthdayUserProcessor()) // 100명 변환
                .writer(birthdayUserWriter()) // 100명 저장 + 커밋
                .build();
    }

    // 3. Reader (DB 직접 조회 + 페이징)
    @Bean
    @StepScope
    public JdbcPagingItemReader<BirthdayUserDto> birthdayUserReader() {
        int currentMonth = LocalDate.now().getMonthValue();
        // MySQL용 페이징 SQL 생성기 -> Spring Batch가 MySQL에 맞는 LIMIT/OFFSET 기반 페이징 쿼리를 자동으로 만들어주는 도구
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT user_created_id");
        queryProvider.setFromClause("FROM Users");
        queryProvider.setWhereClause("WHERE birth IS NOT NULL AND MONTH(birth) = :month");

        // 페이징 안정성을 위한 정렬 키(유니크/증가 PK)
        Map<String, Order> sortKeys = new LinkedHashMap<>();
        sortKeys.put("user_created_id", Order.ASCENDING);
        queryProvider.setSortKeys(sortKeys);

        Map<String, Object> params = new HashMap<>();
        params.put("month", currentMonth);

        return new JdbcPagingItemReaderBuilder<BirthdayUserDto>()
                .name("birthdayUserReader")
                .dataSource(dataSource)
                .queryProvider(queryProvider)
                .parameterValues(params)
                .pageSize(1000)   // DB에서 1000개씩 가져옴 (chunk=100과 별개)
                .fetchSize(1000)
                .rowMapper((rs, rowNum) -> new BirthdayUserDto(rs.getLong("user_created_id")))
                .build();
    }

    // 4. Processor: DTO -> Entity 변환 (중복 발급 장지 + 정책 캐싱)
    @Bean
    @StepScope
    public ItemProcessor<BirthdayUserDto, UserCoupon> birthdayUserProcessor() {
        // 1) 정책 1번만 로딩(캐시)
        if (birthdayPolicy == null) {
            birthdayPolicy = couponPolicyRepository.findById(BIRTHDAY_POLICY_ID)
                    .orElseThrow(() -> new IllegalStateException("생일 쿠폰 정책이 없습니다. policyId=" + BIRTHDAY_POLICY_ID));
        }

        // 2) 이미 발급된 유저들 한 번에 조회해서 Set 캐시 (핵심!)
        //    Set은 contains가 O(1)이라 per-user DB 조회가 사라짐
        final java.util.Set<Long> issuedUserIds =
                new java.util.HashSet<>(couponRepository.findIssuedUserIdsByPolicyId(BIRTHDAY_POLICY_ID));

        return item -> {
            Long userId = item.getUserCreatedId();

            // 3) 중복이면 스킵
            if (issuedUserIds.contains(userId)) {
                return null;
            }

            // 4) 발급 대상이면 Set에도 추가(같은 배치 실행 중 중복 방지)
            issuedUserIds.add(userId);

            LocalDate now = LocalDate.now();
            LocalDateTime issuedAt = LocalDateTime.now();
            LocalDateTime expiryAt = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59);

            return UserCoupon.builder()
                    .couponPolicy(birthdayPolicy)
                    .userId(userId)
                    .status(CouponStatus.ISSUED)
                    .issuedAt(issuedAt)
                    .expiryAt(expiryAt)
                    .usedAt(null)
                    .build();
        };
    }


    // 5. Writer: DB에 저장 (null 제거 후 저장)
    @Bean
    public ItemWriter<UserCoupon> birthdayUserWriter() {
        return chunk -> {
            var items = chunk.getItems().stream()
                    .filter(java.util.Objects::nonNull) // processor에서 null 스킵 대비
                    .collect(Collectors.toList());

            if (!items.isEmpty()) {
                couponRepository.saveAll(items);
            }
        };
    }
}
