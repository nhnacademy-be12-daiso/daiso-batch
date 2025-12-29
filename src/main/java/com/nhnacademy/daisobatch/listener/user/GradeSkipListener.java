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

package com.nhnacademy.daisobatch.listener.user;

import com.nhnacademy.daisobatch.dto.user.GradeCalculationDto;
import com.nhnacademy.daisobatch.dto.user.GradeChangeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;

@Slf4j
public class GradeSkipListener implements SkipListener<GradeCalculationDto, GradeChangeDto> {

    @Override
    public void onSkipInRead(Throwable t) {
        // Reader Error
        log.error("[GradeSkipListener] 조회 중 스킵 - {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(GradeCalculationDto item, Throwable t) {
        // Processor Error
        log.error("[GradeSkipListener] 등급 산정 중 스킵 - 회원 ID: {}, 구매한 금액: {}, 에러: {}",
                item.userCreatedId(), item.netAmount(), t.getMessage());
    }

    @Override
    public void onSkipInWrite(GradeChangeDto item, Throwable t) {
        // Writer Error
        log.error("[GradeSkipListener] 등급 저장 중 스킵 - 회원 ID: {}, 변경하려던 등급: {}, 에러: {}",
                item.userCreatedId(), item.gradeId(), t.getMessage());
    }

}
