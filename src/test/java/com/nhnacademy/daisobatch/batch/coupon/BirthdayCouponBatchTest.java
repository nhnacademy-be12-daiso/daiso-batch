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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.nhnacademy.daisobatch.client.UserServiceClient;
import com.nhnacademy.daisobatch.dto.BirthdayUserDto;
import com.nhnacademy.daisobatch.entity.coupon.CouponPolicy;
import com.nhnacademy.daisobatch.entity.coupon.UserCoupon;
import com.nhnacademy.daisobatch.repository.coupon.CouponPolicyRepository;
import com.nhnacademy.daisobatch.repository.coupon.UserCouponRepository;
import com.nhnacademy.daisobatch.type.CouponStatus;
import com.nhnacademy.daisobatch.type.CouponType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@Sql(scripts = "/sql/coupon/init-coupon-policy.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class BirthdayCouponBatchTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("birthdayCouponJob")
    private Job birthdayCouponJob;

    @MockBean
    private UserServiceClient userServiceClient;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(birthdayCouponJob);
    }

    @AfterEach
    void tearDown() {
        userCouponRepository.deleteAll();
    }

    @Test
    @DisplayName("생일 쿠폰 발급 배치가 정상 작동하는지 테스트")
    public void birthdayCouponJobTest() throws Exception {
        // given
        BirthdayUserDto user1 = new BirthdayUserDto();
        user1.setUserCreatedId(100L);
        user1.setUsername("testUser");
        user1.setBirth(LocalDate.now());

        when(userServiceClient.getBirthdayUsers(anyInt()))
                .thenReturn(List.of(user1));

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // 쿠폰이 생성되었는지 확인
        long couponCount = userCouponRepository.count();
        System.out.println("✅ 생성된 쿠폰 개수: " + couponCount);
        assertTrue(couponCount > 0);
    }

    @Test
    @DisplayName("여러 명의 생일자에게 쿠폰이 발급되는지 테스트")
    public void multipleBirthdayUsersTest() throws Exception {
        // given
        BirthdayUserDto user1 = new BirthdayUserDto();
        user1.setUserCreatedId(100L);
        user1.setUsername("user1");
        user1.setBirth(LocalDate.now());

        BirthdayUserDto user2 = new BirthdayUserDto();
        user2.setUserCreatedId(200L);
        user2.setUsername("user2");
        user2.setBirth(LocalDate.now());

        BirthdayUserDto user3 = new BirthdayUserDto();
        user3.setUserCreatedId(300L);
        user3.setUsername("user3");
        user3.setBirth(LocalDate.now());

        when(userServiceClient.getBirthdayUsers(anyInt()))
                .thenReturn(Arrays.asList(user1, user2, user3));

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // then
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 3명에게 쿠폰이 발급되었는지 확인
        long couponCount = userCouponRepository.count();
        assertThat(couponCount).isEqualTo(3);

        List<UserCoupon> user1Coupons = userCouponRepository.findByUserId(100L);
        assertThat(user1Coupons).hasSize(1);

        UserCoupon userCoupon = user1Coupons.get(0);
        String couponPolicyName = userCoupon.getCouponPolicy().getCouponPolicyName();
        assertThat(couponPolicyName).isEqualTo("생일 쿠폰"); // qkf

        List<UserCoupon> user2Coupons = userCouponRepository.findByUserId(200L);
        assertThat(user2Coupons).hasSize(1);

        List<UserCoupon> user3Coupons = userCouponRepository.findByUserId(300L);
        assertThat(user3Coupons).hasSize(1);

    }

    @Test
    @DisplayName("중복 발급 방지 - 이미 생일 쿠폰을 받은 사용자는 재발급 안됨")
    public void preventDuplicateCouponTest() throws Exception {
        // given
        BirthdayUserDto user1 = new BirthdayUserDto();
        user1.setUserCreatedId(100L);
        user1.setUsername("testUser");
        user1.setBirth(LocalDate.now());

        // 이미 생일 쿠폰을 받은 상태로 설정
        CouponPolicy birthdayPolicy = couponPolicyRepository.findById(4L).orElseThrow();
        UserCoupon existingCoupon = UserCoupon.builder()
                .couponPolicy(birthdayPolicy)
                .userId(100L)
                .status(CouponStatus.ISSUED)
                .issuedAt(LocalDateTime.now())
                .expiryAt(LocalDateTime.now().plusDays(30))
                .build();
        userCouponRepository.save(existingCoupon); // 이미 발급 받아서 저장 안됨. 즉 주석처리해도 쿠폰 잘 돌아감!

        when(userServiceClient.getBirthdayUsers(anyInt()))
                .thenReturn(List.of(user1));

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 여전히 쿠폰이 1개만 있어야 함 (중복 발급 안됨)
        long couponCount = userCouponRepository.count();
        assertThat(couponCount).isEqualTo(1);
    }

    @Test
    @DisplayName("생일자가 없을 때 배치가 정상 종료되는지 테스트")
    public void noBirthdayUsersTest() throws Exception {
        // given
        when(userServiceClient.getBirthdayUsers(anyInt()))
                .thenReturn(Collections.emptyList());

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 발급된 쿠폰이 없어야 함
        long couponCount = userCouponRepository.count();
        assertThat(couponCount).isEqualTo(0);
    }

    @Test
    @DisplayName("발급된 쿠폰의 상태가 ISSUED인지 확인")
    public void couponStatusIsIssuedTest() throws Exception {
        // given
        BirthdayUserDto user1 = new BirthdayUserDto();
        user1.setUserCreatedId(100L);
        user1.setUsername("testUser");
        user1.setBirth(LocalDate.now());

        when(userServiceClient.getBirthdayUsers(anyInt()))
                .thenReturn(List.of(user1));

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncherTestUtils.launchJob(jobParameters);

        // then
        List<UserCoupon> coupons = userCouponRepository.findByUserId(100L);
        assertThat(coupons).hasSize(1);

        UserCoupon coupon = coupons.get(0);
        assertThat(coupon.getStatus()).isEqualTo(CouponStatus.ISSUED);
        assertThat(coupon.getIssuedAt()).isNotNull();
        assertThat(coupon.getExpiryAt()).isNotNull();
        assertThat(coupon.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("발급된 쿠폰의 만료일이 이번 달 마지막 날인지 확인")
    public void couponExpiryDateTest() throws Exception {
        // given
        BirthdayUserDto user1 = new BirthdayUserDto();
        user1.setUserCreatedId(100L);
        user1.setUsername("testUser");
        user1.setBirth(LocalDate.now());

        when(userServiceClient.getBirthdayUsers(anyInt()))
                .thenReturn(List.of(user1));

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncherTestUtils.launchJob(jobParameters);

        // then
        List<UserCoupon> coupons = userCouponRepository.findByUserId(100L);
        UserCoupon coupon = coupons.get(0);

        LocalDate now = LocalDate.now();
        LocalDate expectedExpiryDate = now.withDayOfMonth(now.lengthOfMonth());

        assertThat(coupon.getExpiryAt().toLocalDate()).isEqualTo(expectedExpiryDate);
    }

    @Test
    @DisplayName("발급된 쿠폰이 올바른 정책과 연결되어 있는지 확인")
    public void couponPolicyLinkTest() throws Exception {
        // given
        BirthdayUserDto user1 = new BirthdayUserDto();
        user1.setUserCreatedId(100L);
        user1.setUsername("testUser");
        user1.setBirth(LocalDate.now());

        when(userServiceClient.getBirthdayUsers(anyInt()))
                .thenReturn(List.of(user1));

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncherTestUtils.launchJob(jobParameters);

        // then
        List<UserCoupon> coupons = userCouponRepository.findByUserId(100L);
        UserCoupon coupon = coupons.get(0);

        CouponPolicy policy = coupon.getCouponPolicy();
        assertThat(policy).isNotNull();
        assertThat(policy.getCouponType()).isEqualTo(CouponType.BIRTHDAY);
        assertThat(policy.getCouponPolicyId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("100명의 대량 데이터 처리 테스트 (청크 크기 검증)")
    public void bulkDataProcessingTest() throws Exception {
        // given
        List<BirthdayUserDto> users = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            BirthdayUserDto user = new BirthdayUserDto();
            user.setUserCreatedId((long) i);
            user.setUsername("user" + i);
            user.setBirth(LocalDate.now());
            users.add(user);
        }

        when(userServiceClient.getBirthdayUsers(anyInt()))
                .thenReturn(users);

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 100명 모두 쿠폰을 받았는지 확인
        long couponCount = userCouponRepository.count();
        assertThat(couponCount).isEqualTo(100);
    }

//    @Test
//    @DisplayName("10,000명의 대량 데이터 처리 테스트")
//    public void bulkDataTest() throws Exception{
//        //given
//        ArrayList<BirthdayUserDto> users = new ArrayList<>();
//        int totalUsers = 10000;
//        for(int i = 1; i <= totalUsers; i++){
//            BirthdayUserDto user = new BirthdayUserDto();
//            user.setUserCreatedId((long) i);
//            user.setUsername("user" + i);
//            user.setBirth(LocalDate.now());
//            users.add(user);
//        }
//        when(userServiceClient.getBirthdayUsers(anyInt()))
//                .thenReturn(users); // Mocking은 Job의 Reader 구현에 따라 다르게 설정해야 합니다.
//
//        JobParameters jobParameters = new JobParametersBuilder()
//                .addLong("time", System.currentTimeMillis())
//                .toJobParameters();
//
//        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
//
//        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
//
//        long couponCount = userCouponRepository.count();
//        assertThat(couponCount).isEqualTo(totalUsers); // 10000명
//
//        //when
//
//        //then
//
//    }

    @Test
    @DisplayName("배치 실행 메타데이터 검증")
    public void jobMetadataTest() throws Exception {
        // given
        BirthdayUserDto user1 = new BirthdayUserDto();
        user1.setUserCreatedId(100L);
        user1.setUsername("testUser");
        user1.setBirth(LocalDate.now());

        when(userServiceClient.getBirthdayUsers(anyInt()))
                .thenReturn(List.of(user1));

        // when
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then
        assertThat(jobExecution.getJobInstance().getJobName()).isEqualTo("birthdayCouponJob");
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        // Step 실행 정보 확인
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStepName()).isEqualTo("birthdayCouponStep");
        assertThat(stepExecution.getReadCount()).isEqualTo(1); // 1명 읽음
        assertThat(stepExecution.getWriteCount()).isEqualTo(1); // 1명 쓰기
        assertThat(stepExecution.getCommitCount()).isGreaterThan(0); // 커밋 발생
    }


}