package com.nhnacademy.daisobatch.repository.coupon;

import com.nhnacademy.daisobatch.entity.coupon.CouponPolicy;
import com.nhnacademy.daisobatch.type.CouponPolicyStatus;
import com.nhnacademy.daisobatch.type.CouponType;
import com.nhnacademy.daisobatch.type.DiscountWay;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponPolicyJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public CouponPolicy findById(Long policyId) {
        String sql = """
            SELECT
                coupon_policy_id,
                coupon_policy_name,
                coupon_type,
                discount_way,
                discount_amount,
                min_order_amount,
                max_discount_amount,
                valid_days,
                valid_start_date,
                valid_end_date,
                quantity,
                policy_status
            FROM coupon_policies
            WHERE coupon_policy_id = ?
        """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                        CouponPolicy.builder()
                                .couponPolicyId(rs.getLong("coupon_policy_id"))
                                .couponPolicyName(rs.getString("coupon_policy_name"))
                                .couponType(CouponType.valueOf(rs.getString("coupon_type")))
                                .discountWay(DiscountWay.valueOf(rs.getString("discount_way")))
                                .discountAmount(rs.getBigDecimal("discount_amount"))
                                .minOrderAmount(rs.getObject("min_order_amount", Long.class))
                                .maxDiscountAmount(rs.getObject("max_discount_amount", Long.class))
                                .validDays(rs.getObject("valid_days", Integer.class))
                                .validStartDate(rs.getTimestamp("valid_start_date") != null
                                        ? rs.getTimestamp("valid_start_date").toLocalDateTime()
                                        : null)
                                .validEndDate(rs.getTimestamp("valid_end_date") != null
                                        ? rs.getTimestamp("valid_end_date").toLocalDateTime()
                                        : null)
                                .quantity(rs.getObject("quantity", Integer.class))
                                .couponPolicyStatus(
                                        CouponPolicyStatus.valueOf(rs.getString("policy_status")))
                                .build(),
                policyId
        );
    }
}
