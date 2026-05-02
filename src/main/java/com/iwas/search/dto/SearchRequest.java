package com.iwas.search.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    private String query;

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
