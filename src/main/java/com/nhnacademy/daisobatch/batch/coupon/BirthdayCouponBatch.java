///*
// * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// * + Copyright 2025. NHN Academy Corp. All rights reserved.
// * + * While every precaution has been taken in the preparation of this resource,  assumes no
// * + responsibility for errors or omissions, or for damages resulting from the use of the information
// * + contained herein
// * + No part of this resource may be reproduced, stored in a retrieval system, or transmitted, in any
// * + form or by any means, electronic, mechanical, photocopying, recording, or otherwise, without the
// * + prior written permission.
// * +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// */
//
//package com.nhnacademy.daisobatch.batch.coupon;
//
//import com.nhnacademy.daisobatch.client.UserServiceClient;
//import com.nhnacademy.daisobatch.dto.BirthdayUserDto;
//import com.nhnacademy.daisobatch.entity.coupon.CouponPolicy;
//import com.nhnacademy.daisobatch.entity.coupon.UserCoupon;
//import com.nhnacademy.daisobatch.repository.coupon.CouponPolicyRepository;
//import com.nhnacademy.daisobatch.repository.coupon.UserCouponRepository;
//import com.nhnacademy.daisobatch.type.CouponStatus;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.configuration.annotation.StepScope;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.step.builder.StepBuilder;
//import org.springframework.batch.item.ItemProcessor;
//import org.springframework.batch.item.ItemReader;
//import org.springframework.batch.item.ItemWriter;
//import org.springframework.batch.item.support.ListItemReader;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.transaction.PlatformTransactionManager;
//
//@Configuration
//public class BirthdayCouponBatch {
//
//    private final UserServiceClient userServiceClient;
//    private final UserCouponRepository couponRepository;
//    private final CouponPolicyRepository couponPolicyRepository;
//    private final JobRepository jobRepository;
//    private final PlatformTransactionManager transactionManager;
//    private CouponPolicy birthdayPolicy;  // ← 캐싱
//
//
//    public BirthdayCouponBatch(UserServiceClient userServiceClient,
//                               UserCouponRepository couponRepository,
//                               CouponPolicyRepository couponPolicyRepository,
//                               JobRepository jobRepository,
//                               PlatformTransactionManager transactionManager) {
//        this.userServiceClient = userServiceClient;
//        this.couponRepository = couponRepository;
//        this.couponPolicyRepository = couponPolicyRepository;
//        this.jobRepository = jobRepository;
//        this.transactionManager = transactionManager;
//    }
//
//    // 1. Job 정의
//    @Bean
//    public Job birthdayCouponJob() {
//        return new JobBuilder("birthdayCouponJob", jobRepository)
//                .start(birthdayCouponStep())
//                .build();
//    }
//
//    // 2. Step 정의
//    // 서버가 cpu:2~4코어, 메모리: 4~8GB, DB커넥션 풀:10~20개 일떄
//    // chunk = 100일 때 10,000명당 1분 30초 걸림. 회원이 12만명일때까지는 2분내외로 처리 가능!
//    @Bean
//    public Step birthdayCouponStep() {
//        return new StepBuilder("birthdayCouponStep", jobRepository)
//                .<BirthdayUserDto, UserCoupon>chunk(100, transactionManager) // 100명씩 끊어서 처리, 1만명이명 1분 30초
//                .reader(birthdayUserReader()) // 100명 읽기
//                .processor(birthdayUserProcessor()) // 100명 변환
//                .writer(birthdayUserWriter()) // 100명 저장 + 커밋
//                .build();
//    }
//
//    // 3. Reader
//    @Bean
//    @StepScope
//    public ItemReader<BirthdayUserDto> birthdayUserReader() {
//        // 이번 달 구하기
//        int currentMonth = LocalDate.now().getMonthValue();
//        // User 서버에서 생일자 목록 조회
//        List<BirthdayUserDto> users = userServiceClient.getBirthdayUsers(currentMonth);
//        // 데이터가 없으면 빈 리스트 반환
//        if (users == null || users.isEmpty()) {
//            return new ListItemReader<>(List.of());
//        }
//        // Reader로 변환
//        return new ListItemReader<>(users);
//    }
//
//    // 4. Processor: DTO -> Entity 변환
//    @Bean
//    @StepScope
//    public ItemProcessor<BirthdayUserDto, UserCoupon> birthdayUserProcessor() {
//        if (birthdayPolicy == null) {
//            birthdayPolicy = couponPolicyRepository.findById(4L)
//                    .orElseThrow(() -> new IllegalStateException("생일 쿠폰 정책이 없습니다."));
//        }
//
//
//        return item -> {
//            // A.생일 쿠폰 정책 조회 (고정된 ID나 코드로 조회한다고 가정)
//            // 실제로는 매번 조회하면 성능 이슈가 있으므로 캐싱하거나, StepExecutionListener 등으로 미리 로딩하는 것이 좋습니다.
//
//            // 여기서는 예시로 ID가 1L인 정책을 생일 쿠폰 정책이라 가정합니다.
//
//            boolean alreadyHas = couponRepository.existsByUserIdAndCouponPolicy_CouponPolicyId(
//                    item.getUserCreatedId(), 4L);
//            if (alreadyHas) {
//                return null; // null 반환시 Writer에서 스킵.
//            }
//
//            // B. 유효기간 설정 (이번 달 1일 ~ 이번 달 마지막 날)
//            LocalDate now = LocalDate.now();
//            // 발급일: 현재 시간
//            LocalDateTime issuedAt = LocalDateTime.now();
//            // 만료일: 이번 달의 마지막 날 23시 59분 59초
//            LocalDateTime expiryAt = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59);
//
//
//            // C. 엔티티 생성 및 반환
//            return UserCoupon.builder()
//                    .couponPolicy(birthdayPolicy)
//                    .userId(item.getUserCreatedId())
//                    .status(CouponStatus.ISSUED)
//                    .issuedAt(issuedAt)
//                    .expiryAt(expiryAt)
//                    .usedAt(null)
//                    .build();
//        };
//    }
//
//    // 5. Writer: DB에 저장
//    @Bean
//    public ItemWriter<UserCoupon> birthdayUserWriter() {
//        return chunk -> {
//            couponRepository.saveAll(chunk.getItems());
//        };
//    }
//}
