package com.nhnacademy.daisobatch.batch;

import com.nhnacademy.daisobatch.client.UserServiceClient;
import com.nhnacademy.daisobatch.dto.BirthdayUserDto;
import com.nhnacademy.daisobatch.entity.CouponPolicy;
import com.nhnacademy.daisobatch.entity.UserCoupon;
import com.nhnacademy.daisobatch.repository.CouponPolicyRepository;
import com.nhnacademy.daisobatch.repository.UserCouponRepository;
import com.nhnacademy.daisobatch.type.CouponStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class BirthdayCouponBatch {

    private final UserServiceClient userServiceClient;
    private final UserCouponRepository couponRepository;
    private final CouponPolicyRepository couponPolicyRepository;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    public BirthdayCouponBatch(UserServiceClient userServiceClient,
                               UserCouponRepository couponRepository,
                               CouponPolicyRepository couponPolicyRepository,
                               JobRepository jobRepository,
                               PlatformTransactionManager transactionManager) {
        this.userServiceClient = userServiceClient;
        this.couponRepository = couponRepository;
        this.couponPolicyRepository = couponPolicyRepository;
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }
    // 1. Job 정의
    @Bean
    public Job birthdayCouponJob(){
        return new JobBuilder("birthdayCouponJob", jobRepository)
                .start(birthdayCouponStep())
                .build();
    }
    // 2. Step 정의
    @Bean
    public Step birthdayCouponStep(){
        return new StepBuilder("birthdayCouponStep",jobRepository)
                .<BirthdayUserDto, UserCoupon>chunk(10, transactionManager) // 10명씩 끊어서 처리
                .reader(birthdayUserReader())
                .processor(birthdayUserProcessor())
                .writer(birthdayUserWriter())
                .build();
    }
    // 3. Reader
    @Bean
    public ItemReader<BirthdayUserDto> birthdayUserReader(){
        // 이번 달 구하기
        int currentMonth = LocalDate.now().getMonthValue();

        // User 서버에서 생일자 목록 조회
        List<BirthdayUserDto> users = userServiceClient.getBirthdayUsers(currentMonth);
        // 데이터가 없으면 빈 리스트 반환
        if (users == null || users.isEmpty()) {
            return new ListItemReader<>(List.of());
        }
        // Reader로 변환
        return new ListItemReader<>(users);
    }
    // 4. Processor: DTO -> Entity 변환
    @Bean
    public ItemProcessor<BirthdayUserDto, UserCoupon> birthdayUserProcessor(){
        return item -> {
            // A.생일 쿠폰 정책 조회 (고정된 ID나 코드로 조회한다고 가정)
            // 실제로는 매번 조회하면 성능 이슈가 있으므로 캐싱하거나, StepExecutionListener 등으로 미리 로딩하는 것이 좋습니다.
            // 여기서는 예시로 ID가 1L인 정책을 생일 쿠폰 정채깅라 가정합니다.
            CouponPolicy policy = couponPolicyRepository.findById(4L)
                    .orElseThrow(() -> new IllegalStateException("생일 쿠폰 정책(ID:4)이 존재하지 않습니다."));

            // B. 유효기간 설정 (이번 달 1일 ~ 이번 달 마지막 날)
            LocalDate now = LocalDate.now();
            // 발급일: 현재 시간
            LocalDateTime issuedAt = LocalDateTime.now();
            // 만료일: 이번 달의 마지막 날 23시 59분 59초
            LocalDateTime expiryAt = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59);


            // C. 엔티티 생성 및 반환
            return UserCoupon.builder()
                    .couponPolicy(policy)
                    .userId(item.getUserCreatedId())
                    .status(CouponStatus.ISSUED)
                    .issuedAt(issuedAt)
                    .expiryAt(expiryAt)
                    .usedAt(null)
                    .build();
        };
    }
    // 5. Writer: DB에 저장
    @Bean
    public ItemWriter<UserCoupon> birthdayUserWriter() {
        return chunk -> {
            couponRepository.saveAll(chunk.getItems());
        };
    }
}
