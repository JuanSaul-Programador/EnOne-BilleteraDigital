package com.enone.web.dto.auth;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class LoginResponse {
    
    private Boolean success;
    private String message;
    private String token;
    private String tokenType;
    private Long expiresAt;
    private String username;
    private List<String> roles;
}