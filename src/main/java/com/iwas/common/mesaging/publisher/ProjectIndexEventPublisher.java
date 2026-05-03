package com.iwas.common.mesaging.publisher;

import com.iwas.common.mesaging.event.ProjectIndexEvent;
import com.iwas.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectIndexEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.search-project-sync-exchange}")
    private String exchange;

    public void publish(ProjectIndexEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange, RabbitMQConfig.SEARCH_PROJECT_ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("Failed to publish project index event for projectId={}, op={}: {}",
                    event.getProjectId(), event.getOp(), e.getMessage());
        }
    }
}
