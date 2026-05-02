package com.iwas.common.mesaging.publisher;

import com.iwas.common.mesaging.event.UserIndexEvent;
import com.iwas.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserIndexEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.search-sync-exchange}")
    private String exchange;

    public void publish(UserIndexEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, RabbitMQConfig.SEARCH_ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("Failed to publish user index event for userId={}, op={}: {}",
                    event.getUserId(), event.getOp(), e.getMessage());
        }
    }
}
