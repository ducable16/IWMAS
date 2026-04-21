package com.iwas.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailNotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.email-queue:email.verification}")
    private String queueName;

    public void publish(EmailMessage message) {
        rabbitTemplate.convertAndSend(queueName, message);
    }
}
