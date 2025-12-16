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

package com.nhnacademy.daisobatch.repository.user;

import com.nhnacademy.user.entity.user.User;
import com.nhnacademy.user.entity.user.UserGradeHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGradeHistoryRepository extends JpaRepository<UserGradeHistory, Long> {

    // 해당 유저의 이력 중, 변경일시를 내림차순으로 정렬하여 가장 첫 번째 데이터(최근, == 현재)를 가져옴
    @EntityGraph(attributePaths = "grade")
    Optional<UserGradeHistory> findTopByUserOrderByChangedAtDesc(User user);

}
