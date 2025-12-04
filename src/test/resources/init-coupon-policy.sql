DELETE FROM coupon_policies;
ALTER TABLE coupon_policies ALTER COLUMN coupon_policy_id RESTART WITH 1; -- H2 사용 시 ID 컬럼의 자동 증가(Auto Increment) 값도 초기화

-- ID 1: WELCOME 쿠폰 (WELCOME)
INSERT INTO coupon_policies (
    coupon_policy_id,
    coupon_policy_name,
    coupon_type,
    discount_way,
    discount_amount,
    min_order_amount,
    max_discount_amount,
    valid_days,
    quantity,
    policy_status
) VALUES (
             1,
             '웰컴 쿠폰',
             'WELCOME',
             'FIXED',
             10000.00,
             50000,
             10000,
             30,
             999963,
             'ACTIVE'
         );

-- ID 2: 일반 쿠폰 1 (GENERAL - 선착순 쿠폰)
INSERT INTO coupon_policies (
    coupon_policy_id,
    coupon_policy_name,
    coupon_type,
    discount_way,
    discount_amount,
    min_order_amount,
    max_discount_amount,
    valid_days,
    quantity,
    policy_status
) VALUES (
             2,
             '일반 쿠폰 1',
             'GENERAL',
             'FIXED',
             5000.00,
             30000,
             5000,
             30,
             9,
             'ACTIVE'
         );

-- ID 3: 일반 쿠폰 2 (GENERAL - 5만원 이상시 주문 쿠폰)
INSERT INTO coupon_policies (
    coupon_policy_id,
    coupon_policy_name,
    coupon_type,
    discount_way,
    discount_amount,
    min_order_amount,
    max_discount_amount,
    valid_days,
    policy_status
) VALUES (
             3,
             '일반 쿠폰 2',
             'GENERAL',
             'FIXED',
             10000.00,
             50000,
             10000,
             30,
             'ACTIVE'
         );

-- ID 4: 생일 쿠폰 (BIRTHDAY) - 테스트의 핵심 정책
INSERT INTO coupon_policies (
    coupon_policy_id,
    coupon_policy_name,
    coupon_type,
    discount_way,
    discount_amount,
    min_order_amount,
    max_discount_amount,
    policy_status
) VALUES (
             4,
             '생일 쿠폰',
             'BIRTHDAY',
             'FIXED',
             3000.00,
             20000,
             3000,
             'ACTIVE'
         );