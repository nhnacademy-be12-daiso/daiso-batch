package com.nhnacademy.daisobatch.batch.coupon;

import com.nhnacademy.daisobatch.client.BirthdayCouponBulkEvent;
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
import org.springframework.context.annotation.Profile;
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
    public Job birthdayCouponJob(Step birthdayCouponStep) {
        return new JobBuilder("birthdayCouponJob", jobRepository)
                .start(birthdayCouponStep)
                .build();
    }

    // ===== 2. Step 정의 =====
    @Bean
    public Step birthdayCouponStep(ItemReader<BirthdayUserDto> birthdayUserReader) {
        return new StepBuilder("birthdayCouponStep", jobRepository)
                .<BirthdayUserDto, Long>chunk(100, transactionManager) // <reader가 읽는 타입, writer가 받는 타입>
                // read + process를 100번해서 writer 1번 호출한다.
                .reader(birthdayUserReader)
                .processor(item -> item.getUserCreatedId())
                .writer(birthdayUserWriter())
                .build();
    }
    // ===== 3. Reader: User 서버에서 조회 =====
    @Bean
    @StepScope
    public ItemReader<BirthdayUserDto> birthdayUserReader() {
        return new ItemReader<>() {
            private List<BirthdayUserDto> users = null;
            private int currentIndex = 0;

            private int page = 0;
            private final int size = 1000; // api page size

            private boolean lastPage = false; // 종료 플래그

            @Override
            public BirthdayUserDto read() {
                // 현재 페이지가 비었거나 다 소진했으면 다음 페이지 로드
                if (users == null || currentIndex >= users.size()) {
                    if(lastPage){
                        return null;
                    }
                    int currentMonth = LocalDate.now().getMonthValue();
                    log.info("User 서버에서 {}월 생일자 조회 - page={}, size={}", currentMonth, page, size);


                    users = userServiceClient.getBirthdayUsers(currentMonth, page, size);
                    currentIndex = 0;

                    if (users == null || users.isEmpty()) {
                        log.info("{}월 생일자 더 없음(빈 페이지) - 종료", currentMonth);
                        lastPage = true;
                        return null;
                    }

                    log.info("{}월 생일자 page={} 조회 {}명", currentMonth, page, users.size());

                    // List 기반 마지막 페이지 판정: size보다 작으면 마지막
                    if(users.size() < size){
                        lastPage = true;
                    } else{
                        page++; // 다음 페이지로
                    }

                }
                return users.get(currentIndex++);
            }
        };
    }

//    @Bean
//    @StepScope
//    @Profile("dev")
//    public ItemReader<BirthdayUserDto> perfBirthdayUserReader(
//            @Value("#{jobParameters['total']}") Long totalParam,
//            @Value("#{jobParameters['month']}") Long monthParam
//    ) {
////        final long total = (totalParam == null) ? 100_000L : totalParam;
//        final long total = 100L;
//        final int month = (monthParam == null) ? 10 : monthParam.intValue();
//
//        return new ItemReader<>() {
//            private long idx = 0;
//
//            @Override
//            public BirthdayUserDto read() {
//                if (idx >= total) return null;
//
//                BirthdayUserDto dto = new BirthdayUserDto();
//                dto.setUserCreatedId(1_000_000L + idx);
//                dto.setUsername("dev-" + idx);
//                dto.setBirth(LocalDate.of(1990, month, (int)((idx % 28) + 1)));
//
//                idx++;
//                return dto;
//            }
//        };
//    }


    // ===== 4. Processor: DTO -> Long 변환 =====
    @Bean
    @StepScope
    public ItemProcessor<BirthdayUserDto, Long> birthdayUserProcessor() {
        return item -> {
            Long userId = item.getUserCreatedId();
            log.debug("Processing userId={}", userId);
            return userId;
        };
    }

    // ===== 5. Writer: RabbitMQ로 이벤트 발행 =====
    @Bean
    public ItemWriter<Long> birthdayUserWriter() {
        return chunk -> {
            List<? extends Long> userIds = chunk.getItems(); // 최대 100개

            BirthdayCouponBulkEvent event =
                    new BirthdayCouponBulkEvent(List.copyOf(userIds), "birthday-" + LocalDate.now());

            rabbitTemplate.convertAndSend(birthdayExchange, birthdayRoutingKey, event);
            log.info("Bulk publish size={}, batchId={}", userIds.size(), event.batchId());

        };
    }
}
