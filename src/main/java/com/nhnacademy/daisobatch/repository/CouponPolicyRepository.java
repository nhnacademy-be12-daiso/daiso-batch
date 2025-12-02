package com.nhnacademy.daisobatch.repository;

import com.nhnacademy.daisobatch.entity.CouponPolicy;
import com.nhnacademy.daisobatch.type.CouponPolicyStatus;
import com.nhnacademy.daisobatch.type.CouponType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, Long> {
    // CouponType으로 조회 (WELCOME, BIRTHDAY 등)
    List<CouponPolicy> findByCouponType(CouponType couponType);

    // ACTIVE 상태만 조회
    List<CouponPolicy> findByCouponPolicyStatus(CouponPolicyStatus status);
}