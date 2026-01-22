package com.enone.web.controller;

import com.enone.application.service.WatsonService;
import com.enone.web.dto.chat.ChatRequest;
import com.enone.web.dto.chat.ChatResponse;
import com.enone.web.dto.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatbotController {

    private final WatsonService watsonService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@Valid @RequestBody ChatRequest request, Authentication auth) {
        try {
            Long userId = Long.parseLong(auth.getName());

            ChatResponse response = watsonService.sendMessage(
                    userId,
                    request.getMessage(),
                    request.getSessionId());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()));
        }
    }
}
