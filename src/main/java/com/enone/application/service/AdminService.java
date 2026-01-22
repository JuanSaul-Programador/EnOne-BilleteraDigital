package com.enone.application.service;



import com.enone.web.dto.admin.*;

import java.util.Map;

public interface AdminService {
    
    DashboardStatsDTO getDashboardStats();
    void enableUser(Long userId);
    void sendMessageToUser(Long userId, AdminMessageRequest request);
    PagedResponse<UserAdminResponse> listUsers(int page, int size, String search);
    void blockUser(Long userId, BlockUserRequest request);
    Map<String, Long> getUserGrowthStats(int days);
    Map<String, Object> getRealtimeTransactionStats();
    Map<String, Object> getActivityHeatmap(int days);
}