package com.iwas.search.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;
import com.iwas.skill.dto.RequiredSkill;
import com.iwas.user.enums.UserRole;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    @Size(max = 200)
    private String query;

    @Default
    private List<RequiredSkill> requiredSkills = List.of();

    private UserRole role;

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
