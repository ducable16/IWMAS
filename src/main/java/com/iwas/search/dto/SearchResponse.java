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
public class SearchResponse<T> {
    private List<T> items;
    private long total;
    private int page;
    private int size;
    private String source;
    private long tookMs;
}
