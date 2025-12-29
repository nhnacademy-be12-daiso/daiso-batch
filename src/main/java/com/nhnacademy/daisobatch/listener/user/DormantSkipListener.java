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

import com.nhnacademy.daisobatch.dto.user.DormantAccountDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;

@Slf4j
public class DormantSkipListener implements SkipListener<DormantAccountDto, DormantAccountDto> {

    @Override
    public void onSkipInRead(Throwable t) {
        // Reader Error
        log.error("[DormantSkipListener] 조회 중 스킵 - {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(DormantAccountDto item, Throwable t) {
        // Processor Error
        log.error("[DormantSkipListener] 휴면 대상 계정 상태 확인 중 스킵 - 계정 ID: {}, 현재 상태: {}, 에러: {}",
                item.loginId(), item.currentStatusId(), t.getMessage());
    }

    @Override
    public void onSkipInWrite(DormantAccountDto item, Throwable t) {
        // Writer Error
        log.error("[DormantSkipListener] 상태 저장 중 스킵 - 계정 ID: {}, 에러: {}",
                item.loginId(), t.getMessage());
    }

}
