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

package com.nhnacademy.daisobatch.entity.coupon;

import com.nhnacademy.daisobatch.dto.coupon.CouponPolicyUpdateRequest;
import com.nhnacademy.daisobatch.type.CouponPolicyStatus;
import com.nhnacademy.daisobatch.type.CouponType;
import com.nhnacademy.daisobatch.type.DiscountWay;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coupon_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CouponPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_policy_id")
    private Long couponPolicyId;

    @Column(name = "coupon_policy_name") // 쿠폰 정책 이름
    private String couponPolicyName;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", nullable = false) // 쿠폰 정책 종류
    private CouponType couponType;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_way", nullable = false) // 할인 타입
    private DiscountWay discountWay;

    @Column(name = "discount_amount") // 할인 금액
    private BigDecimal discountAmount;

    @Column(name = "min_order_amount") // 최소 주문 금액
    private Long minOrderAmount;

    @Column(name = "max_discount_amount") // 최대 할인 금액
    private Long maxDiscountAmount;

    @Column(name = "valid_days") // 쿠폰 상대 유효 일수
    private Integer validDays;

    @Column(name = "valid_start_date") // 쿠폰 고정 유효기간 시작일
    private LocalDateTime validStartDate;

    @Column(name = "valid_end_date") // 쿠폰 고정 유효기간 끝나는일
    private LocalDateTime validEndDate;

    @Column(name = "quantity")
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_status")
    private CouponPolicyStatus couponPolicyStatus;

    // 수량 감소
    public void decreaseQuantity() {
        if (this.quantity == null) {
            return; // 무제한이면 차감 안 함
        }
        if (this.quantity <= 0) {
            throw new IllegalStateException("발급 가능한 쿠폰이 없습니다.");
        }
        this.quantity--;
    }

    public void increaseQuantity() {
        if (this.quantity != null) {
            this.quantity++;
        }
    }

    // 전체 수정 (발급 전)
    public void update(CouponPolicyUpdateRequest request) {
        this.couponPolicyName = request.getCouponPolicyName();
        this.couponType = request.getCouponType();
        this.discountWay = request.getDiscountWay();
        this.discountAmount = request.getDiscountAmount();
        this.minOrderAmount = request.getMinOrderAmount();
        this.maxDiscountAmount = request.getMaxDiscountAmount();
        this.validDays = request.getValidDays();
        this.validStartDate = request.getValidStartDate();
        this.validEndDate = request.getValidEndDate();
        this.quantity = request.getQuantity();
        this.couponPolicyStatus = request.getPolicyStatus();  // 상태 포함
    }

    // 상태만 수정 (발급 후)
    public void updateStatus(CouponPolicyStatus status) {
        this.couponPolicyStatus = status;
    }


}
