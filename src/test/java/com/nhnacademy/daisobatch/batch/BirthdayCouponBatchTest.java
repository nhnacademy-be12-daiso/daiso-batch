package com.nhnacademy.daisobatch.batch;

import com.nhnacademy.daisobatch.client.UserServiceClient;
import com.nhnacademy.daisobatch.dto.BirthdayUserDto;
import com.nhnacademy.daisobatch.entity.CouponPolicy;
import com.nhnacademy.daisobatch.repository.CouponPolicyRepository;
import com.nhnacademy.daisobatch.repository.UserCouponRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest
@SpringBatchTest
public class BirthdayCouponBatchTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private CouponPolicyRepository couponPolicyRepository;

    @MockitoBean
    private UserCouponRepository userCouponRepository;

    @Test
    @DisplayName("생일 쿠폰 발급 배치가 정상 작동하는지 테스트")

    public void birthdayCouponJobTest() throws Exception{
        //given
        // 1. 생일자 유저 데이터 Mocking
        BirthdayUserDto user1 = new BirthdayUserDto();
        user1.setUserCreatedId(100L);
        user1.setUsername("testUser");
        user1.setBirth(LocalDate.now());

        when(userServiceClient.getBirthdayUsers(anyInt()))
                .thenReturn(List.of(user1));

        // 2. 쿠폰 정책 Mocking
        CouponPolicy mockPolicy = CouponPolicy.builder()
                .couponPolicyId(4L)
                .couponPolicyName("생일 쿠폰")
                .build();

        when(couponPolicyRepository.findById(4L))
                .thenReturn(Optional.of(mockPolicy));

        //when (배치 실행)
        // 잡 파라미터에 현재 시간 등을 넣어 유니크하게 만듦
        JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // Job 이름으로 실행 (jobLauncherTestUtils 알아서 찾음)
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        //then
        Assertions.assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
    }
}
