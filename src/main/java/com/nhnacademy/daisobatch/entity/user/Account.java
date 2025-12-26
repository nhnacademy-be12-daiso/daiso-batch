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
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {      // 회원 인증 (로그인) 정보

    @Id
    @Column(name = "login_id", length = 16)
    private String loginId;         // 로그인 아이디: 사용자 입력 (PK)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_created_id", nullable = false, unique = true)
    private User user;              // Users 테이블 외래키 (FK), 일대일 관계

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_status_id", nullable = false)
    private Status status;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;      // 최근 로그인 일시

}
