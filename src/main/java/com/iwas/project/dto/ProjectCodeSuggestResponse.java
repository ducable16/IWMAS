package com.iwas.project.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectCodeSuggestResponse {
    private String code;
}
