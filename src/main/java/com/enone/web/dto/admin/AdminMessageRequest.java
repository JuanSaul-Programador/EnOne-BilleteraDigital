package com.enone.web.dto.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminMessageRequest {
    private String subject;
    private String message;
}