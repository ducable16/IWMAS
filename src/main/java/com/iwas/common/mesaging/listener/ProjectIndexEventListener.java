package com.iwas.common.mesaging.listener;

import com.iwas.common.mesaging.event.ProjectIndexEvent;
import com.iwas.search.dto.ProjectSearchResult;
import com.iwas.search.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectIndexEventListener {

    private final ElasticsearchService engine;

    @RabbitListener(queues = "${app.rabbitmq.search-project-sync-queue}",
                    containerFactory = "rabbitListenerContainerFactory")
    public void onMessage(ProjectIndexEvent event) {
        try {
            switch (event.getOp()) {
                case UPSERT -> engine.indexProject(ProjectSearchResult.builder()
                        .id(event.getProjectId())
                        .name(event.getName())
                        .code(event.getCode())
                        .status(event.getStatus())
                        .managerId(event.getManagerId())
                        .build());
                case DELETE -> engine.deleteProject(event.getProjectId());
            }
        } catch (Exception e) {
            log.error("Failed to apply project index event op={} projectId={}: {}",
                    event.getOp(), event.getProjectId(), e.getMessage());
            throw e;
        }
    }
}
