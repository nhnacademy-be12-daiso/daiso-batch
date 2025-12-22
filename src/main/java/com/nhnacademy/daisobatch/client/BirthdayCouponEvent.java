package com.nhnacademy.daisobatch.client;

import java.util.List;

public record BirthdayCouponEvent(
        List<Long> userIds,
        String batchId)
{}

