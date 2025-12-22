package com.nhnacademy.daisobatch.dto;

import java.util.List;

public record BirthdayCouponBulkEvent(
        List<Long> userIds,
        String batchId   // 추적용(옵션)
) {

}
