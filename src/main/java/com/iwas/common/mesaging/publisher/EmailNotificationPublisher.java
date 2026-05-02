package com.iwas.common.mesaging.publisher;

import com.iwas.auth.entity.EmailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailNotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.email-queue}")
    private String queueName;

    public void publish(EmailMessage message) {
        rabbitTemplate.convertAndSend(queueName, message);
    }
}
