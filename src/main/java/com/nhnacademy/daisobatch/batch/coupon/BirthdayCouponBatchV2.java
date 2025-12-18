package com.nhnacademy.daisobatch.batch.coupon;

import com.nhnacademy.daisobatch.client.BirthdayCouponEvent;
import com.nhnacademy.daisobatch.client.UserServiceClient;
import com.nhnacademy.daisobatch.dto.BirthdayUserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BirthdayCouponBatchV2 {

    private final UserServiceClient userServiceClient;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.birthday.exchange}")
    private String birthdayExchange;

    @Value("${rabbitmq.birthday.routing-key}")
    private String birthdayRoutingKey;

    // ===== 1. Job 정의 =====
    @Bean
    public Job birthdayCouponJob() {
        return new JobBuilder("birthdayCouponJob", jobRepository)
                .start(birthdayCouponStep())
                .build();
    }

    // ===== 2. Step 정의 =====
    @Bean
    public Step birthdayCouponStep() {
        return new StepBuilder("birthdayCouponStep", jobRepository)
                .<BirthdayUserDto, BirthdayCouponEvent>chunk(100, transactionManager)
                .reader(birthdayUserReader())
                .processor(birthdayUserProcessor())
                .writer(birthdayUserWriter())
                .build();
    }
    // ===== 3. Reader: User 서버에서 조회 =====
    @Bean
    @StepScope
    public ItemReader<BirthdayUserDto> birthdayUserReader() {
        return new ItemReader<BirthdayUserDto>() {
            private List<BirthdayUserDto> users;
            private int currentIndex = 0;

            @Override
            public BirthdayUserDto read() {
                // 첫 호출 시 User 서버에서 데이터 로드
                if (users == null) {
                    int currentMonth = LocalDate.now().getMonthValue();
                    log.info("User 서버에서 {}월 생일자 조회", currentMonth);

                    users = userServiceClient.getBirthdayUsers(currentMonth);

                    if (users == null || users.isEmpty()) {
                        log.info("{}월 생일자 없음", currentMonth);
                        return null;
                    }

                    log.info("{}월 생일자 {}명 조회 완료", currentMonth, users.size());
                }

                // 하나씩 반환
                if (currentIndex < users.size()) {
                    return users.get(currentIndex++);
                }

                return null; // 끝
            }
        };
    }
    // ===== 4. Processor: DTO -> Event 변환 =====
    @Bean
    @StepScope
    public ItemProcessor<BirthdayUserDto, BirthdayCouponEvent> birthdayUserProcessor() {
        return item -> {
            Long userId = item.getUserCreatedId();
            log.debug("Processing userId={}", userId);

            // 이벤트 생성만!
            return new BirthdayCouponEvent(userId);
        };
    }

    // ===== 5. Writer: RabbitMQ로 이벤트 발행 =====
    @Bean
    public ItemWriter<BirthdayCouponEvent> birthdayUserWriter() {
        return chunk -> {
            List<? extends BirthdayCouponEvent> events = chunk.getItems();

            log.info("RabbitMQ 이벤트 발행 시작: {}건", events.size());

            for (BirthdayCouponEvent event : events) {
                rabbitTemplate.convertAndSend(
                        birthdayExchange,
                        birthdayRoutingKey,
                        event
                );
            }

            log.info("RabbitMQ 이벤트 발행 완료: {}건", events.size());
        };
    }
}
