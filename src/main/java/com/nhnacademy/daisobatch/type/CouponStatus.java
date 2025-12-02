package com.nhnacademy.daisobatch.type;


public enum CouponStatus {
    /** 사용자에게 발급되어 사용 가능한 상태 */
    ISSUED,

    /** 사용자가 주문에 성공적으로 적용하여 사용이 완료된 상태 */
    USED,

    /** 사용 기간이 만료되어 사용할 수 없는 상태 */
    EXPIRED,

    /** 주문 취소/환불로 인해 사용 상태에서 복구된 상태 (재사용 가능) */
    CANCELED
}