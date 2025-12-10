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

package com.nhnacademy.daisobatch.repository.coupon;

import com.nhnacademy.daisobatch.entity.coupon.UserCoupon;
import com.nhnacademy.daisobatch.type.CouponStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

//    // 특정 사용자의 모든 쿠폰 조회
//    List<UserCoupon> findByUserId(Long userId);

    @Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.couponPolicy WHERE uc.userId = :userId")
    List<UserCoupon> findByUserId(@Param("userId") Long userId);

    // 특정 사용자의 특정 상태 쿠폰 조회
    @Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.couponPolicy WHERE uc.userId = :userId AND uc.status = :status")
    List<UserCoupon> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") CouponStatus status);

    // 특정 사용자의 사용 가능한 쿠폰 조회 (ISSUED 또는 CANCELED)
    List<UserCoupon> findByUserIdAndStatusIn(Long userId, List<CouponStatus> statuses);

    boolean existsByUserIdAndCouponPolicy_CouponPolicyId(Long userId, Long couponPolicyId);

    List<UserCoupon> findAllByStatusAndExpiryAtBefore(CouponStatus status, LocalDateTime expiryAtBefore);

    long countByCouponPolicyCouponPolicyId(Long couponPolicyId); // <-- 이 줄로 변경

    // UserCouponRepository.java에 추가
    @Modifying
    @Query("UPDATE UserCoupon uc SET uc.status = 'EXPIRED' WHERE uc.status = 'ISSUED' AND uc.expiryAt < :now")
    int bulkExpireCoupons(@Param("now") LocalDateTime now);
}