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
import com.nhnacademy.daisobatch.repository.user.GradeRepository;
import com.nhnacademy.daisobatch.repository.user.UserGradeHistoryRepository;
import jakarta.persistence.EntityManagerFactory;
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
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@RequiredArgsConstructor
@Configuration
public class GradeChangeBatch {

    private final JobRepository jobRepository;

    private final PlatformTransactionManager platformTransactionManager;

    private final EntityManagerFactory entityManagerFactory;    // JPA Reader

    private final DataSource dataSource;                        // JDBC Writer

    // private final OrderRepository orderRepository;
    private final GradeRepository gradeRepository;
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
    public JpaPagingItemReader<User> gradeChangeReader() {
        // 현재 상태가 ACTIVE인 계정 조회
        return new JpaPagingItemReaderBuilder<User>()
                .queryString("SELECT u FROM Users u " +
                        "JOIN u.account a " +
                        "JOIN AccountStatusHistories ash ON ash.account = a " +
                        "WHERE ash.changedAt = (SELECT MAX(h.changedAt) FROM AccountStatusHistories h WHERE h.account = a) " +
                        "AND ash.status.statusName = 'ACTIVE' " +
                        "ORDER BY u.userCreatedId ASC")
                .pageSize(CHUNK_SIZE)
                .entityManagerFactory(entityManagerFactory)
                .name("gradeChangeReader")
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<User, GradeChangeDto> gradeChangeProcessor() {
        return null;
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<GradeChangeDto> gradeChangeWriter() {
        return null;
    }

}
