package com.enone.web.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    
    private Long id;
    private String transactionUid;
    private String securityCode;
    private String type;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String status;
    private BigDecimal balanceAfter;
    private Instant createdAt;
    
    // For transfers
    private String fromUser;
    private String toUser;
    private Long relatedUserId;
}