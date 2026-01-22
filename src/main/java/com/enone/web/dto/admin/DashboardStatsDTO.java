package com.enone.web.dto.admin;


import com.enone.web.dto.wallet.TransactionResponse;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DashboardStatsDTO {
    // Balances totales
    private BigDecimal totalBalancePen;
    private BigDecimal totalBalanceUsd;
    private BigDecimal totalBalanceEur;
    
    // Estadísticas de usuarios
    private Long totalUsers;
    private Long newUsersToday;
    private Long activeUsersLast7Days;
    
    // Estadísticas de transacciones
    private Map<String, Long> hourlyTxCounts;
    private Long totalTransactionsToday;
    private BigDecimal totalVolumeToday;
    private Map<String, Long> transactionsByType;
    
    // Transacciones recientes
    private List<TransactionResponse> recentTransactions;
    
    // Metadata
    private Instant generatedAt;
    
    private Long activeUsers;        // enabled=true + tx últimos 7 días
    private Long inactiveUsers;      // enabled=true + sin tx 30+ días
    private Long neverUsedUsers;     // enabled=true + nunca transaccionaron
    private Long disabledUsers;
    
    // Estadísticas de 2FA
    private Long twoFactorEnabledCount;
    private Long twoFactorDisabledCount;
    
    // Estadísticas destacadas
    private Map<String, Object> highlightStats;   
}