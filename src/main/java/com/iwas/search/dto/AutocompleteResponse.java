package com.iwas.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutocompleteResponse {
    private String prefix;
    private List<SuggestionItem> suggestions;
    private String source;
    private long tookMs;
}
