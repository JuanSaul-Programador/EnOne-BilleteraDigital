package com.enone.util.exchange.impl;

import com.enone.exception.ApiException;
import com.enone.util.exchange.ExchangeRateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class ExchangeRateServiceImpl implements ExchangeRateService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String primaryApiUrl;
    private final String fallbackApiUrl;
    
    public ExchangeRateServiceImpl(RestTemplate restTemplate,
                                  ObjectMapper objectMapper,
                                  @Value("${app.exchange.primary-api:https://api.exchangerate-api.com/v4/latest/}") String primaryApiUrl,
                                  @Value("${app.exchange.fallback-api:https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/}") String fallbackApiUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.primaryApiUrl = primaryApiUrl;
        this.fallbackApiUrl = fallbackApiUrl;
    }
    
    @Override
    @Cacheable(value = "exchangeRates", key = "#fromCurrency + '_' + #toCurrency")
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        log.info("Obteniendo tipo de cambio: {} -> {}", fromCurrency, toCurrency);
        
        if (fromCurrency.equals(toCurrency)) {
            return BigDecimal.ONE;
        }
        
        try {
            return getRateFromPrimaryAPI(fromCurrency, toCurrency);
        } catch (Exception e) {
            log.warn("API primaria falló, intentando fallback: {}", e.getMessage());
            try {
                return getRateFromFallbackAPI(fromCurrency, toCurrency);
            } catch (Exception e2) {
                log.warn("API fallback falló, usando tasas estáticas: {}", e2.getMessage());
                return getStaticFallbackRate(fromCurrency, toCurrency);
            }
        }
    }
    
    @Override
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        BigDecimal rate = getRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public boolean isServiceAvailable() {
        try {
            getRate("USD", "PEN");
            return true;
        } catch (Exception e) {
            log.warn("Servicio de tipos de cambio no disponible: {}", e.getMessage());
            return false;
        }
    }
    
    private BigDecimal getRateFromPrimaryAPI(String from, String to) {
        try {
            String url = primaryApiUrl + from;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ApiException(502, "Error al obtener tipo de cambio desde API primaria");
            }
            
            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode rates = json.get("rates");
            
            if (!rates.has(to)) {
                throw new ApiException(400, "Moneda no soportada: " + to);
            }
            
            BigDecimal rate = rates.get(to).decimalValue();
            log.info("Tipo de cambio obtenido desde API primaria: 1 {} = {} {}", from, rate, to);
            
            return rate.setScale(4, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            throw new RuntimeException("Error en API primaria: " + e.getMessage(), e);
        }
    }
    
    private BigDecimal getRateFromFallbackAPI(String from, String to) {
        try {
            String url = fallbackApiUrl + from.toLowerCase() + ".json";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ApiException(502, "Fallback API falló");
            }
            
            JsonNode json = objectMapper.readTree(response.getBody());
            JsonNode rates = json.get(from.toLowerCase());
            
            if (!rates.has(to.toLowerCase())) {
                throw new ApiException(400, "Moneda no soportada: " + to);
            }
            
            BigDecimal rate = rates.get(to.toLowerCase()).decimalValue();
            log.info("Tipo de cambio desde fallback API: 1 {} = {} {}", from, rate, to);
            
            return rate.setScale(4, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            throw new RuntimeException("Error en fallback API: " + e.getMessage(), e);
        }
    }
    
    private BigDecimal getStaticFallbackRate(String from, String to) {
        log.info("Usando tasas estáticas para: {} -> {}", from, to);
        

        if ("PEN".equals(from) && "USD".equals(to)) {
            return new BigDecimal("0.2700");
        }
        if ("USD".equals(from) && "PEN".equals(to)) {
            return new BigDecimal("3.7000");
        }
        if ("EUR".equals(from) && "USD".equals(to)) {
            return new BigDecimal("1.0800");
        }
        if ("USD".equals(from) && "EUR".equals(to)) {
            return new BigDecimal("0.9260");
        }
        if ("EUR".equals(from) && "PEN".equals(to)) {
            return new BigDecimal("4.0000");
        }
        if ("PEN".equals(from) && "EUR".equals(to)) {
            return new BigDecimal("0.2500");
        }
        
        throw new ApiException(400, "Conversión no soportada: " + from + " -> " + to);
    }
}