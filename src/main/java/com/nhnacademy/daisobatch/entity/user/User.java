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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {     // 회원 기본 정보

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_created_id")
    private Long userCreatedId;             // 회원 고유 ID (PK, AI)

    @Column(name = "user_name", nullable = false, length = 30)
    private String userName;                // 회원 이름

    @Column(name = "phone_number", unique = true, length = 30)
    private String phoneNumber;             // 연락처 (UK)

    @Column(unique = true, length = 80)
    private String email;                    // 이메일 (UK)

    private LocalDate birth;                 // 생년월일, YYYY-MM-DD 패턴

}
