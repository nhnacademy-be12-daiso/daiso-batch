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

package com.nhnacademy.daisobatch.listener.coupon;

import com.nhnacademy.daisobatch.dto.BirthdayUserDto;
import com.nhnacademy.daisobatch.entity.coupon.UserCoupon;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BirthdaySkipListener implements SkipListener<BirthdayUserDto, UserCoupon> {

    @Override
    public void onSkipInRead(Throwable t) {
        log.error("[BirthdaySkipListener] 조회 중 스킵 - {}", msg(t));
    }

    @Override
    public void onSkipInProcess(BirthdayUserDto item, Throwable t) {
        // processor 단계에서는 입력이 BirthdayUserDto니까 userCreatedId만 찍으면 충분
        log.error("[BirthdaySkipListener] 가공 중 스킵 - userCreatedId: {}, 에러: {}",
                item != null ? item.getUserCreatedId() : null,
                msg(t));
    }

    @Override
    public void onSkipInWrite(UserCoupon item, Throwable t) {
        // writer 단계에서는 userId / couponPolicyId만 있으면 추적 가능
        Long userId = (item != null) ? item.getUserId() : null;
        Long policyId = (item != null && item.getCouponPolicy() != null)
                ? item.getCouponPolicy().getCouponPolicyId()
                : null;

        log.error("[BirthdaySkipListener] 저장 중 스킵 - userId: {}, policyId: {}, 에러: {}",
                userId, policyId, msg(t));
    }

    private String msg(Throwable t) {
        return (t == null) ? "null" : String.valueOf(t.getMessage());
    }
}
