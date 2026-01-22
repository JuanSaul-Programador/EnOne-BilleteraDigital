package com.enone.application.mapper;


import com.enone.domain.model.User;
import com.enone.domain.model.UserProfile;
import com.enone.web.dto.user.UserResponse;

public interface UserMapper {
    
    UserResponse toResponse(User user, UserProfile profile);
    
    UserResponse toResponse(User user);
}