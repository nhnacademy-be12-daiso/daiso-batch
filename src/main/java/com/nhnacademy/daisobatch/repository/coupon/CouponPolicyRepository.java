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

import com.nhnacademy.daisobatch.entity.coupon.CouponPolicy;
import com.nhnacademy.daisobatch.type.CouponPolicyStatus;
import com.nhnacademy.daisobatch.type.CouponType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, Long> {
    // CouponType으로 조회 (WELCOME, BIRTHDAY 등)
    List<CouponPolicy> findByCouponType(CouponType couponType);

    // ACTIVE 상태만 조회
    List<CouponPolicy> findByCouponPolicyStatus(CouponPolicyStatus status);
}