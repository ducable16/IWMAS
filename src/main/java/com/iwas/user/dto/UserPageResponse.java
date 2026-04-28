package com.iwas.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserPageResponse {
    private List<?> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}