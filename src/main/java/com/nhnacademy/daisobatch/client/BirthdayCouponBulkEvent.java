package com.nhnacademy.daisobatch.client;

import java.util.List;

public record BirthdayCouponBulkEvent(
        List<Long> userIds,
        String batchId
) {
}
