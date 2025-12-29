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

package com.nhnacademy.daisobatch.listener;

import com.nhnacademy.daisobatch.dto.user.GradeCalculationDto;
import com.nhnacademy.daisobatch.dto.user.GradeChangeDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;

@Slf4j
public class CustomSkipListener implements SkipListener<GradeCalculationDto, GradeChangeDto> {

    @Override
    public void onSkipInProcess(GradeCalculationDto item, Throwable t) {
        log.error("등급 산정 중 에러 발생 - user_created_id: {}, Error: {}", item.userCreatedId(), t.getMessage());
    }

}
