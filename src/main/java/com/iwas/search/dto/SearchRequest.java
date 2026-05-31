package com.iwas.search.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    /**
     * Free-text query. Optional when {@link #requiredSkills} is present — a skill-only
     * search matches every active user owning the required skills.
     */
    @Size(max = 200)
    private String query;

    /** Skill constraints; the user must satisfy ALL of them (AND). */
    @Default
    private List<RequiredSkill> requiredSkills = List.of();

    @Default
    @Min(0)
    private int page = 0;

    @Default
    @Min(1)
    private int size = 20;

    private String sortBy;

    @Default
    private String sortDir = "desc";
}
