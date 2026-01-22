package com.enone.util.sms.impl;

import com.enone.util.sms.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class CallMeBotServiceImpl implements SmsService {

    private final RestTemplate restTemplate;
    private final Map<String, String> apiKeys = new HashMap<>();
    private final String apiUrl;

    public CallMeBotServiceImpl(RestTemplate restTemplate,
                               @Value("${app.sms.callmebot.api-url:https://api.callmebot.com/whatsapp.php}") String apiUrl,
                               @Value("${app.sms.callmebot.keys:}") String rawKeys) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        
        loadApiKeys(rawKeys);
    }

    private void loadApiKeys(String rawKeys) {
        if (rawKeys == null || rawKeys.isBlank()) {
            log.warn("No se encontraron claves de CallMeBot configuradas");
            return;
        }

        for (String pair : rawKeys.split(",")) {
            String[] parts = pair.trim().split(":");
            if (parts.length == 2) {
                apiKeys.put(parts[0].trim(), parts[1].trim());
            }
        }

        log.info("CallMeBot configurado para {} números", apiKeys.size());
    }

    @Override
    public void send(String phone, String text) {
        log.info("Enviando WhatsApp a: {}", phone);
        
        try {
            String phoneClean = phone.replace("+", "").trim();
            String apiKey = apiKeys.get(phoneClean);

            if (apiKey == null) {
                throw new RuntimeException(
                    "No hay API key configurada para el teléfono " + phone + 
                    " (configura en app.sms.callmebot.keys)"
                );
            }

            String message = URLEncoder.encode(text, StandardCharsets.UTF_8);
            
            String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("phone", phoneClean)
                .queryParam("text", message)
                .queryParam("apikey", apiKey)
                .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WhatsApp enviado exitosamente a: {}", phone);
            } else {
                throw new RuntimeException("HTTP " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error enviando WhatsApp a: {}", phone, e);
            throw new RuntimeException("Error enviando SMS: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isServiceAvailable() {
        return !apiKeys.isEmpty();
    }
}