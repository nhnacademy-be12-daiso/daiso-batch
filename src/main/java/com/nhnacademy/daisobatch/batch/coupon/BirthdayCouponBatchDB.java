package com.nhnacademy.daisobatch.batch.coupon;

import com.nhnacademy.daisobatch.dto.BirthdayUserDto;
import com.nhnacademy.daisobatch.entity.coupon.CouponPolicy;
import com.nhnacademy.daisobatch.entity.coupon.UserCoupon;
import com.nhnacademy.daisobatch.repository.coupon.CouponPolicyRepository;
import com.nhnacademy.daisobatch.repository.coupon.UserCouponRepository;
import com.nhnacademy.daisobatch.type.CouponStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class BirthdayCouponBatchDB {

    private static final long BIRTHDAY_POLICY_ID = 4L;

    private final UserCouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    // Processor에서 재사용할 쿠폰 쟁책 캐시
    private CouponPolicy birthdayPolicy;

    // 1. Job 정의
    @Bean(name = "birthdayCouponJobDB")
    public Job birthdayCouponJobDB(@Qualifier("birthdayCouponStepDB") Step step) {
        return new JobBuilder("birthdayCouponJobDB", jobRepository)
                .start(step)
                .build();
    }

    // 2. Step 정의
    @Bean(name = "birthdayCouponStepDB")
    public Step birthdayCouponStepDB(
            @Qualifier("birthdayUserReaderDB") JdbcPagingItemReader<BirthdayUserDto> reader,
            @Qualifier("birthdayUserProcessorDB") ItemProcessor<BirthdayUserDto, UserCoupon> processor,
            @Qualifier("birthdayUserJdbcWriterDB") ItemWriter<UserCoupon> writer
    ) {
        return new StepBuilder("birthdayCouponStepDB", jobRepository)
                .<BirthdayUserDto, UserCoupon>chunk(500, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    // 3. Reader (DB 직접 조회 + 페이징)
    @Bean(name = "birthdayUserReaderDB")
    @StepScope
    public JdbcPagingItemReader<BirthdayUserDto> birthdayUserReaderDB(
            @Value("#{jobParameters['currentMonth']}") Integer currentMonth
    ) {
        // month는 JobParameter 우선 (재실행/재현성 보장)
        int month = (currentMonth != null) ? currentMonth : LocalDate.now().getMonthValue();

        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT u.user_created_id AS user_created_id");
        queryProvider.setFromClause(
        """
            FROM Users u
            JOIN Accounts a ON a.user_created_id = u.user_created_id
        """);
        queryProvider.setWhereClause(
        """
            WHERE u.birth IS NOT NULL
            AND MONTH(u.birth) = :month
            AND a.current_status_id = :statusId
        """);

        Map<String, Order> sortKeys = new LinkedHashMap<>();
        sortKeys.put("u.user_created_id", Order.ASCENDING); // alias 포함(모호성 제거)
        queryProvider.setSortKeys(sortKeys);

        Map<String, Object> params = new HashMap<>();
        params.put("month", month);
        params.put("statusId", 1); // ACTIVE = 1 하드코딩

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



    // 4. Processor: DTO -> Entity 변환 (중복 발급 장지 + 정책 캐싱)
    @Bean(name = "birthdayUserProcessorDB")
    @StepScope
    public ItemProcessor<BirthdayUserDto, UserCoupon> birthdayUserProcessorDB() {
        if (birthdayPolicy == null) {
            birthdayPolicy = couponPolicyRepository.findById(BIRTHDAY_POLICY_ID)
                    .orElseThrow(() -> new IllegalStateException("생일 쿠폰 정책이 없습니다. policyId=" + BIRTHDAY_POLICY_ID));
        }

        final Set<Long> issuedUserIds =
                new HashSet<>(couponRepository.findIssuedUserIdsByPolicyId(BIRTHDAY_POLICY_ID));

        return item -> {
            Long userId = item.getUserCreatedId();
            if (issuedUserIds.contains(userId)) return null;
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
    @Bean(name = "birthdayUserJdbcWriterDB")
    public JdbcBatchItemWriter<UserCoupon> birthdayUserJdbcWriterDB() {
        return new JdbcBatchItemWriterBuilder<UserCoupon>()
                .dataSource(dataSource)
                .sql("""
                    INSERT INTO user_coupons
                      (user_created_id, coupon_policy_id, status, issue_at, expiry_at, used_at)
                    VALUES
                      (:userId, :couponPolicyId, :status, :issuedAt, :expiryAt, :usedAt)
                """)
                .itemSqlParameterSourceProvider(item -> {
                    MapSqlParameterSource ps = new MapSqlParameterSource();
                    ps.addValue("userId", item.getUserId());
                    ps.addValue("couponPolicyId", item.getCouponPolicy().getCouponPolicyId());
                    ps.addValue("status", item.getStatus().name());
                    ps.addValue("issuedAt", item.getIssuedAt());
                    ps.addValue("expiryAt", item.getExpiryAt());
                    ps.addValue("usedAt", item.getUsedAt());
                    return ps;
                })
                .build();
    }
}
