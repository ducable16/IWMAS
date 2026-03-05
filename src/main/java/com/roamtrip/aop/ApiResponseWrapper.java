package com.roamtrip.aop;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roamtrip.dto.response.ApiResponse;
import com.roamtrip.entity.enums.ErrorCode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.io.File;

@RestControllerAdvice(basePackages = "com.bookverse.controller")
@AllArgsConstructor
@Slf4j
public class ApiResponseWrapper implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> type = returnType.getParameterType();

        return !ApiResponse.class.isAssignableFrom(type)
                && !ResponseEntity.class.isAssignableFrom(type)
                && !Resource.class.isAssignableFrom(type)
                && !File.class.isAssignableFrom(type);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  @Nonnull MethodParameter returnType,
                                  @Nonnull MediaType selectedContentType,
                                  @Nonnull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @Nonnull ServerHttpRequest request,
                                  @Nonnull ServerHttpResponse response) {
        if (body == null) {
            return ApiResponse.success();
        }
        else if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON); // Set header về JSON
            try {
                return objectMapper.writeValueAsString(ApiResponse.success(body));
            } catch (JsonProcessingException e) {
                log.error("Lỗi khi convert String sang JSON: {}", e.getMessage());
                return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể convert sang JSON");
            }
        }
        return ApiResponse.success(body);
    }
}