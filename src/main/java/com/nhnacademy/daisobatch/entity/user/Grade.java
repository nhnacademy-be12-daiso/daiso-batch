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

package com.nhnacademy.daisobatch.entity.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Grades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Grade {    // 회원 등급 및 혜택 정보

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Long gradeId;           // 등급 고유 ID (PK, AI)

    @Column(name = "grade_name", nullable = false, length = 10)
    private String gradeName;       // GENERAL, ROYAL, GOLD, PLATINUM

    // BigDecimal 사용법 참고: https://dev.gmarket.com/75
    @Column(name = "point_rate", nullable = false, precision = 4, scale = 2)    // 99.99% 까지 가능
    private BigDecimal pointRate;   // 등급별 적립률

}
