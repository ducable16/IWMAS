package com.iwas.search.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwas.search.config.SearchProperties;
import com.iwas.search.dto.SuggestionItem;
import com.iwas.search.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisAdapter implements RedisService {

    private static final String KEY_FORMAT = "autocomplete:%s:%s";
    private static final TypeReference<List<SuggestionItem>> LIST_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SearchProperties properties;

    @Override
    public List<SuggestionItem> getSuggestions(String entity, String prefix) {
        String key = key(entity, prefix);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, LIST_TYPE);
        } catch (Exception e) {
            log.warn("Redis autocomplete read failed for key={}: {}", key, e.getMessage());
            return List.of();
        }
    }

    @Override
    public void putSuggestions(String entity, String prefix, List<SuggestionItem> items) {
        if (items == null || items.isEmpty()) return;
        String key = key(entity, prefix);
        try {
            String json = objectMapper.writeValueAsString(items);
            redisTemplate.opsForValue().set(key, json,
                    Duration.ofSeconds(properties.getAutocomplete().getCacheTtlSeconds()));
        } catch (Exception e) {
            log.warn("Redis autocomplete write failed for key={}: {}", key, e.getMessage());
        }
    }

    @Override
    public void invalidate(String entity, String prefix) {
        try {
            redisTemplate.delete(key(entity, prefix));
        } catch (Exception e) {
            log.warn("Redis invalidate failed: {}", e.getMessage());
        }
    }

    private String key(String entity, String prefix) {
        return String.format(KEY_FORMAT, entity, prefix.toLowerCase());
    }
}
