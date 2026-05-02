package com.iwas.search.service;

import com.iwas.search.dto.SuggestionItem;

import java.util.List;

public interface RedisService {

    List<SuggestionItem> getSuggestions(String entity, String prefix);

    void putSuggestions(String entity, String prefix, List<SuggestionItem> items);

    void invalidate(String entity, String prefix);
}
