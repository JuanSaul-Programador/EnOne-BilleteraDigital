package com.enone.application.mapper;


import com.enone.web.dto.common.ApiResponse;

public interface CommonMapper {
    
    <T> ApiResponse<T> toSuccessResponse(T data);
    
    <T> ApiResponse<T> toSuccessResponse(String message, T data);
    
    <T> ApiResponse<T> toErrorResponse(String error);
    
    <T> ApiResponse<T> toErrorResponse(String error, Integer status);
}