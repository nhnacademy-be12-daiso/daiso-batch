package com.nhnacademy.daisobatch.repository.coupon;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class IssuedCouponJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public Set<Long> findIssuedUserIdsByPolicyId(Long policyId) {
        String sql = """
            SELECT user_created_id
            FROM user_coupons
            WHERE coupon_policy_id = ?
        """;

        return new HashSet<>(
                jdbcTemplate.queryForList(sql, Long.class, policyId)
        );
    }


}
