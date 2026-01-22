package com.enone.web.dto.admin;

import lombok.Builder; 
import lombok.Data;
import java.time.Instant;

@Data 
@Builder 
public class UserAdminResponse {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private boolean enabled;
    private java.util.List<String> roles;
    private Instant createdAt;
    private Instant deletedAt;
    private boolean twoFactorEnabled;
}