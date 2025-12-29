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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class JobFailureNotificationListener implements JobExecutionListener {

    @Value("${dooray.hook.url}")
    private String doorayHookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("[JobFailureNotificationListener] 배치 실패: 두레이 알림 전송 시도");
            sendDoorayNotification(jobExecution);
        } else {
            log.debug("[JobFailureNotificationListener] 배치 성공: {}",
                    jobExecution.getJobInstance().getJobName());
        }
    }

    private void sendDoorayNotification(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        String exitDescription = jobExecution.getExitStatus().getExitDescription();
        String description = (exitDescription != null && exitDescription.length() > 1000)
                ? String.format("%s ... (생략)", exitDescription.substring(0, 1000)) : exitDescription;

        Map<String, Object> doorayBody = new HashMap<>();
        doorayBody.put("botName", "Dasio Batch Bot");
        doorayBody.put("text", "**배치 작업 실패 알림**");

        Map<String, String> attachment = new HashMap<>();
        attachment.put("title", String.format("Job Name: %s", jobName));
        attachment.put("text", String.format("Error Log:\n```\n%s\n```", description));
        attachment.put("color", "#FF0000");

        doorayBody.put("attachments", Collections.singletonList(attachment));

        try {
            restTemplate.postForEntity(doorayHookUrl, doorayBody, String.class);
            log.debug("[JobFailureNotificationListener] 두레이 알림 전송 완료");

        } catch (Exception e) {
            log.error("[JobFailureNotificationListener] 두레이 알림 전송 실패", e);
        }
    }

}
