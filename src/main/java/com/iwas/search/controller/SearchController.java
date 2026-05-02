package com.iwas.search.controller;

import com.iwas.search.dto.AutocompleteResponse;
import com.iwas.search.dto.SearchRequest;
import com.iwas.search.dto.SearchResponse;
import com.iwas.search.dto.UserSearchResult;
import com.iwas.search.service.SearchService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/autocomplete")
    public AutocompleteResponse autocomplete(@RequestParam("q") @NotBlank String q) {
        return searchService.autocomplete(q);
    }

    @GetMapping("/search")
    public SearchResponse<UserSearchResult> search(
            @RequestParam("q") @NotBlank String q,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) int size,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {
        SearchRequest req = SearchRequest.builder()
                .query(q).page(page).size(size).sortBy(sortBy).sortDir(sortDir).build();
        return searchService.search(req);
    }
}
