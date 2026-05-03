package com.iwas.common.mesaging.init;

import com.iwas.project.entity.Project;
import com.iwas.project.repository.ProjectRepository;
import com.iwas.search.entity.ProjectSearchDocument;
import com.iwas.search.repository.ElasticsearchProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.search", name = "bootstrap-on-startup", havingValue = "true", matchIfMissing = false)
public class ProjectIndexBootstrap implements ApplicationRunner {

    private final ProjectRepository projectRepository;
    private final ElasticsearchProjectRepository projectSearchRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<Project> projects = projectRepository.findAllActive();
            List<ProjectSearchDocument> docs = projects.stream().map(this::toDoc).toList();
            projectSearchRepository.saveAll(docs);
            log.info("Search bootstrap: indexed {} projects into Elasticsearch", docs.size());
        } catch (Exception e) {
            log.error("Project search bootstrap failed: {}", e.getMessage(), e);
        }
    }

    private ProjectSearchDocument toDoc(Project p) {
        return ProjectSearchDocument.builder()
                .id(p.getId())
                .name(p.getName())
                .code(p.getCode())
                .status(p.getStatus() == null ? null : p.getStatus().name())
                .managerId(p.getManagerId())
                .isDeleted(Boolean.TRUE.equals(p.getIsDeleted()))
                .build();
    }
}
