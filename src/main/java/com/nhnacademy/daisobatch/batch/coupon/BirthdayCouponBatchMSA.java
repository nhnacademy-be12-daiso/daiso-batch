package com.nhnacademy.daisobatch.batch.coupon;

import com.nhnacademy.daisobatch.client.BirthdayCouponBulkEvent;
import com.nhnacademy.daisobatch.client.UserServiceClient;
import com.nhnacademy.daisobatch.dto.BirthdayUserDto;
import com.nhnacademy.daisobatch.exception.RabbitPublishFailedException;
import com.nhnacademy.daisobatch.exception.UserServicePagingFailedException;
import com.nhnacademy.daisobatch.listener.JobFailureNotificationListener;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BirthdayCouponBatchMSA {

    private final UserServiceClient userServiceClient;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RabbitTemplate rabbitTemplate;

    private final JobFailureNotificationListener jobFailureNotificationListener;


    @Value("${rabbitmq.birthday.exchange}")
    private String birthdayExchange;

    @Value("${rabbitmq.birthday.routing-key}")
    private String birthdayRoutingKey;

    // ===== 1) Job (MSA) =====
    @Bean(name = "birthdayCouponJobMSA")
    public Job birthdayCouponJobMSA(@Qualifier("birthdayCouponStepMSA") Step step) {
        return new JobBuilder("birthdayCouponJobMSA", jobRepository)
                .start(step)
                .listener(jobFailureNotificationListener)
                .build();
    }

    // ===== 2) Step (MSA) =====
    @Bean(name = "birthdayCouponStepMSA")
    public Step birthdayCouponStepMSA(
            @Qualifier("birthdayUserReaderMSA") ItemReader<BirthdayUserDto> reader,
            @Qualifier("birthdayUserProcessorMSA") ItemProcessor<BirthdayUserDto, Long> processor,
            @Qualifier("birthdayUserWriterMSA") ItemWriter<Long> writer
    ) {
        return new StepBuilder("birthdayCouponStepMSA", jobRepository)
                .<BirthdayUserDto, Long>chunk(1000, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    // ===== 3) Reader (MSA): Feign 페이징 조회 =====
    @Bean(name = "birthdayUserReaderMSA")
    @StepScope
    public ItemReader<BirthdayUserDto> birthdayUserReaderMSA() {
        return new ItemReader<>() {
            private List<BirthdayUserDto> users = null;
            private int currentIndex = 0;

            private int page = 0;
            private final int size = 1000;

            private boolean lastPage = false;

            @Override
            public BirthdayUserDto read() {
                if (users == null || currentIndex >= users.size()) {
                    if (lastPage) return null;

                    int currentMonth = LocalDate.now().getMonthValue();
                    log.info("User 서버에서 {}월 생일자 조회 - page={}, size={}", currentMonth, page, size);

//                    users = userServiceClient.getBirthdayUsers(currentMonth, page, size);
                    users = fetchBirthdayUsersWithRetry(currentMonth, page, size);

                    currentIndex = 0;

                    if (users == null || users.isEmpty()) {
                        log.info("{}월 생일자 더 없음(빈 페이지) - 종료", currentMonth);
                        lastPage = true;
                        return null;
                    }

                    log.info("{}월 생일자 page={} 조회 {}명", currentMonth, page, users.size());

                    if (users.size() < size) {
                        lastPage = true;
                    } else {
                        page++;
                    }
                }

                return users.get(currentIndex++);
            }

            private List<BirthdayUserDto> fetchBirthdayUsersWithRetry(int month, int page, int size) {
                for (int attempt = 1; attempt <= 3; attempt++) {
                    try {
                        return userServiceClient.getBirthdayUsers(month, page, size);
                    } catch (RetryableException e) {
                        // 네트워크/타임아웃/UnknownHost 등 "연결계열"
                        if (attempt == 3) {
                            throw new UserServicePagingFailedException(
                                    "User 서버 생일자 페이징 조회 실패 (Retryable) " +
                                            "(month=" + month + ", page=" + page + ")", e
                            );
                        }
                        sleep();

                    } catch (FeignException e) {
                        if (e.status() >= 400 && e.status() < 500) {
                            throw e; // 4xx 즉시 실패
                        }
                        if (attempt == 3) throw e;
                        sleep();
                    }
                }
                throw new UserServicePagingFailedException(
                        "User 서버 생일자 페이징 조회 실패 " +
                                "(month=" + month + ", page=" + page + ")"
                );
            }

            private void sleep() {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {}
            }

        };
    }

    // ===== 4) Processor (MSA): DTO -> userId =====
    @Bean(name = "birthdayUserProcessorMSA")
    @StepScope
    public ItemProcessor<BirthdayUserDto, Long> birthdayUserProcessorMSA() {
        return item -> item.getUserCreatedId();
    }

    // ===== 5) Writer (MSA): RabbitMQ publish =====
//    @Bean(name = "birthdayUserWriterMSA")
//    public ItemWriter<Long> birthdayUserWriterMSA() {
//        return chunk -> {
//            List<? extends Long> userIds = chunk.getItems();
//
//            BirthdayCouponBulkEvent event =
//                    new BirthdayCouponBulkEvent(List.copyOf(userIds), "birthday-" + LocalDate.now());
//
//            rabbitTemplate.convertAndSend(birthdayExchange, birthdayRoutingKey, event);
//            log.info("Bulk publish size={}, batchId={}", userIds.size(), event.batchId());
//        };
//    }
    @Bean(name = "birthdayUserWriterMSA")
    public ItemWriter<Long> birthdayUserWriterMSA() {
        return chunk -> {
            List<? extends Long> userIds = chunk.getItems();

            BirthdayCouponBulkEvent event =
                    new BirthdayCouponBulkEvent(List.copyOf(userIds), "birthday-" + LocalDate.now());

            CorrelationData cd = new CorrelationData(event.batchId());

            rabbitTemplate.convertAndSend(birthdayExchange, birthdayRoutingKey, event, message -> {
                message.getMessageProperties().setCorrelationId(event.batchId());
                return message;
            }, cd);

            // broker confirm 기다림
            CorrelationData.Confirm confirm = cd.getFuture().get(5, java.util.concurrent.TimeUnit.SECONDS);

            if (!confirm.isAck()) {
                throw new RabbitPublishFailedException(
                        "Rabbit publish NACK.(메시지 큐 접근 실패) batchId=" + event.batchId() + ", cause=" + confirm.getReason()
                );
            }
            // (선택) returns는 아래 “ReturnsTracker” 방식으로 잡는 게 깔끔
            // log.info("Bulk publish confirmed size={}, batchId={}", userIds.size(), event.batchId());
        };
    }




}
