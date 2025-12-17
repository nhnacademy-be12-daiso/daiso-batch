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

package com.nhnacademy.daisobatch.repository.order;

import com.nhnacademy.daisobatch.entity.order.Order;
import com.nhnacademy.daisobatch.type.OrderStatus;
import java.time.ZonedDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 특정 회원의 지정된 날짜 이후 순수 주문 금액 합계 조회
    // 1. 해당 회원의 주문이 맞는지
    // 2. 기간 내 주문 했는지
    // 3. 구매 확정된 주문인지
    @Query("SELECT COALESCE(SUM(o.totalPrice), 0) " +
            "FROM Order o " +
            "WHERE o.userCreatedId = :userCreatedId " +
            "AND o.orderDate >= :startDate " +
            "AND o.orderStatus = :orderStatus")
    long calculateTotalAmount(Long userCreatedId, ZonedDateTime startDate, OrderStatus orderStatus);

}
