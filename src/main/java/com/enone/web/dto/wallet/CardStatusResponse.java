package com.enone.web.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardStatusResponse {
    private boolean hasActiveCard;
    private String maskedCardNumber;
    private String holderName;
    private Instant activatedAt;
    private boolean verified;
}