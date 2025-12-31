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

package com.nhnacademy.daisobatch.batch.coupon;

import com.nhnacademy.daisobatch.client.BirthdayCouponBulkEvent;
import com.nhnacademy.daisobatch.client.UserServiceClient;
import com.nhnacademy.daisobatch.dto.BirthdayUserDto;
import com.nhnacademy.daisobatch.listener.JobFailureNotificationListener;
import com.nhnacademy.daisobatch.listener.coupon.BirthdayChunkListener;
import com.nhnacademy.daisobatch.listener.coupon.BirthdaySkipListener;
import java.time.LocalDate;
import java.util.List;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BirthdayCouponBatchMSA {

    private final UserServiceClient userServiceClient;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.birthday.exchange}")
    private String birthdayExchange;

    @Value("${rabbitmq.birthday.routing-key}")
    private String birthdayRoutingKey;

    private final JobFailureNotificationListener jobFailureNotificationListener;
    private final BirthdayChunkListener birthdayChunkListener;
    private final BirthdaySkipListener birthdaySkipListener;

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
                .<BirthdayUserDto, Long>chunk(500, transactionManager)
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
                    if (lastPage) {
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

                    if (users.size() < size) {
                        lastPage = true;
                    } else {
                        page++;
                    }
                }

                return users.get(currentIndex++);
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
    @Bean(name = "birthdayUserWriterMSA")
    public ItemWriter<Long> birthdayUserWriterMSA() {
        return chunk -> {
            List<? extends Long> userIds = chunk.getItems();

            BirthdayCouponBulkEvent event =
                    new BirthdayCouponBulkEvent(List.copyOf(userIds), "birthday-" + LocalDate.now());

            rabbitTemplate.convertAndSend(birthdayExchange, birthdayRoutingKey, event);
            log.info("Bulk publish size={}, batchId={}", userIds.size(), event.batchId());
        };
    }
}
