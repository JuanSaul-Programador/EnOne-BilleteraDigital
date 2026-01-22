package com.enone.application.mapper.impl;


import com.enone.application.mapper.AdminMapper;
import com.enone.domain.model.Role;
import com.enone.domain.model.User;
import com.enone.web.dto.admin.UserAdminResponse;
import com.enone.web.dto.admin.PagedResponse;
import com.enone.web.dto.admin.DashboardStatsDTO;
import com.enone.web.dto.wallet.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AdminMapperImpl implements AdminMapper {

    @Override
    public UserAdminResponse toUserAdminResponse(User user) {
        return UserAdminResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getProfile() != null ? user.getProfile().getFirstName() : null)
                .lastName(user.getProfile() != null ? user.getProfile().getLastName() : null)
                .email(user.getProfile() != null ? user.getProfile().getEmail() : null)
                .phone(user.getProfile() != null ? user.getProfile().getPhone() : null)
                .enabled(user.getEnabled())
                .roles(user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .deletedAt(user.getDeletedAt())
                .twoFactorEnabled(user.getProfile() != null ? user.getProfile().getTwoFactorEnabled() : false)
                .build();
    }

    @Override
    public <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .number(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    @Override
    public DashboardStatsDTO toDashboardStatsResponse(
            Map<String, BigDecimal> balances,
            Map<String, Long> userStats,
            Map<String, Long> transactionStats,
            List<TransactionResponse> recentTransactions) {
        
        return DashboardStatsDTO.builder()
                .totalBalancePen(balances.getOrDefault("PEN", BigDecimal.ZERO))
                .totalBalanceUsd(balances.getOrDefault("USD", BigDecimal.ZERO))
                .totalBalanceEur(balances.getOrDefault("EUR", BigDecimal.ZERO))
                .totalUsers(userStats.getOrDefault("totalUsers", 0L))
                .newUsersToday(userStats.getOrDefault("newUsersToday", 0L))
                .activeUsersLast7Days(userStats.getOrDefault("activeUsersLast7Days", 0L))
                .activeUsers(userStats.getOrDefault("activeUsers", 0L))
                .inactiveUsers(userStats.getOrDefault("inactiveUsers", 0L))
                .neverUsedUsers(userStats.getOrDefault("neverUsedUsers", 0L))
                .disabledUsers(userStats.getOrDefault("disabledUsers", 0L))
                .twoFactorEnabledCount(userStats.getOrDefault("twoFactorEnabledCount", 0L))
                .twoFactorDisabledCount(userStats.getOrDefault("twoFactorDisabledCount", 0L))
                .totalTransactionsToday(transactionStats.getOrDefault("totalTransactionsToday", 0L))
                .totalVolumeToday(BigDecimal.valueOf(transactionStats.getOrDefault("totalVolumeToday", 0L)))
                .transactionsByType(createTransactionsByTypeMap(transactionStats))
                .recentTransactions(recentTransactions)
                .generatedAt(Instant.now())
                .build();
    }

    private Map<String, Long> createTransactionsByTypeMap(Map<String, Long> transactionStats) {
        Map<String, Long> typeMap = new HashMap<>();
        typeMap.put("DEPOSIT", transactionStats.getOrDefault("depositCount", 0L));
        typeMap.put("WITHDRAWAL", transactionStats.getOrDefault("withdrawalCount", 0L));
        typeMap.put("TRANSFER", transactionStats.getOrDefault("transferCount", 0L));
        typeMap.put("CONVERT", transactionStats.getOrDefault("convertCount", 0L));
        return typeMap;
    }
}