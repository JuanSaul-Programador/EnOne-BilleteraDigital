package com.enone.application.mapper.impl;


import com.enone.application.mapper.CommonMapper;
import com.enone.web.dto.common.ApiResponse;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CommonMapperImpl implements CommonMapper {

    @Override
    public <T> ApiResponse<T> toSuccessResponse(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public <T> ApiResponse<T> toSuccessResponse(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public <T> ApiResponse<T> toErrorResponse(String error) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .timestamp(Instant.now())
                .build();
    }

    @Override
    public <T> ApiResponse<T> toErrorResponse(String error, Integer status) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(error)
                .status(status)
                .timestamp(Instant.now())
                .build();
    }
}