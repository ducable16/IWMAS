package com.iwas.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                       ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keys = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer values = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(keys);
        template.setHashKeySerializer(keys);
        template.setValueSerializer(values);
        template.setHashValueSerializer(values);
        template.afterPropertiesSet();
        return template;
    }
}
