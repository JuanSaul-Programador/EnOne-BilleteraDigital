package com.enone.application.mapper;


import com.enone.domain.model.User;
import com.enone.domain.model.UserProfile;
import com.enone.web.dto.auth.LoginResponse;
import com.enone.web.dto.auth.RegisterCompleteRequest;

public interface AuthMapper {
    
    LoginResponse toLoginResponse(User user, String token, long expiresAt);
    
    UserProfile toUserProfile(RegisterCompleteRequest request, Long userId);
}