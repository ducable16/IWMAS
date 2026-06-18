package com.iwas.common.mesaging.listener;

import com.iwas.common.mesaging.event.UserIndexEvent;
import com.iwas.search.dto.UserIndexCommand;
import com.iwas.search.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserIndexEventListener {

    private final ElasticsearchService engine;

    @RabbitListener(queues = "${app.rabbitmq.search-user-sync-queue}",
                    containerFactory = "rabbitListenerContainerFactory")
    public void onMessage(UserIndexEvent event) {
        try {
            switch (event.getOp()) {
                case UPSERT -> engine.indexUser(UserIndexCommand.builder()
                        .id(event.getUserId())
                        .email(event.getEmail())
                        .fullName(event.getFullName())
                        .position(event.getPosition())
                        .avatarId(event.getAvatarId())
                        .role(event.getRole())
                        .build());
                case DELETE -> engine.deleteUser(event.getUserId());
            }
        } catch (Exception e) {
            log.error("Failed to apply user index event op={} userId={}: {}",
                    event.getOp(), event.getUserId(), e.getMessage());
            throw e;
        }
    }
}
