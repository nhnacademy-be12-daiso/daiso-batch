package com.nhnacademy.daisobatch.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitPublishConfirmConfig {

    private final RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void initCallbacks() {
        // 1) broker에 실제로 들어갔는지(ACK/NACK)
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 여기서 바로 실패로 처리할 수 있게 로그/표식
                // (Step에서 기다렸다가 확인하는 구조가 더 깔끔해서 아래 3번에서 처리)
                // 일단 원인 로그는 남김
                log.error("[Rabbit Confirm] NACK correlationId={}, cause={}",
                        correlationData != null ? correlationData.getId() : "null",
                        cause);
            }
        });

        // 2) 라우팅 실패(큐 없음, 바인딩 없음 등)
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("[Rabbit Return] replyCode={}, replyText={}, exchange={}, routingKey={}",
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    returned.getExchange(),
                    returned.getRoutingKey());
        });
    }
}

