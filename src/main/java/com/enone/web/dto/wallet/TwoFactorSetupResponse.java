package com.enone.web.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorSetupResponse {
    
    private Boolean success;
    private String message;
    private String qrCodeUrl;
    private String qrCode;
    private String secret;
    private String manualEntry;
    private String[] backupCodes;
}