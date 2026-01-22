package com.enone.application.mapper;


import com.enone.domain.model.User;
import com.enone.web.dto.admin.UserAdminResponse;
import com.enone.web.dto.admin.PagedResponse;
import com.enone.web.dto.admin.DashboardStatsDTO;
import com.enone.web.dto.wallet.TransactionResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AdminMapper {
    
    UserAdminResponse toUserAdminResponse(User user);
    
    <T> PagedResponse<T> toPagedResponse(Page<T> page);
    
    DashboardStatsDTO toDashboardStatsResponse(
        Map<String, BigDecimal> balances,
        Map<String, Long> userStats,
        Map<String, Long> transactionStats,
        List<TransactionResponse> recentTransactions
    );
}