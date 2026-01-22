package com.enone.web.controller;

import com.enone.web.dto.admin.DashboardStatsDTO;
import com.enone.web.dto.common.ApiResponse;
import com.enone.application.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats(Authentication authentication) {
        try {
            log.info("Usuario '{}' accediendo al dashboard", authentication.getName());
            DashboardStatsDTO stats = adminService.getDashboardStats();
            return ResponseEntity.ok(ApiResponse.success(stats));

        } catch (Exception e) {
            log.error("Error obteniendo stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error al obtener estadísticas del dashboard"));
        }
    }

    @GetMapping("/stats/user-growth")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUserGrowthStats(
            @RequestParam(defaultValue = "30") int days) {
        try {
            if (days < 1 || days > 90) {
                days = 30;
            }
            Map<String, Long> stats = adminService.getUserGrowthStats(days);
            return ResponseEntity.ok(ApiResponse.success(stats));
            
        } catch (Exception e) {
            log.error("Error obteniendo stats de crecimiento: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error al obtener estadísticas de crecimiento"));
        }
    }

    @GetMapping("/stats/realtime-transactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRealtimeTransactionStats() {
        try {
            Map<String, Object> stats = adminService.getRealtimeTransactionStats();
            return ResponseEntity.ok(ApiResponse.success(stats));
            
        } catch (Exception e) {
            log.error("Error obteniendo stats en tiempo real: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error al obtener estadísticas en tiempo real"));
        }
    }

    @GetMapping("/stats/activity-heatmap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActivityHeatmap(
            @RequestParam(defaultValue = "7") int days) {
        try {
            if (days < 1 || days > 30) {
                days = 7;
            }
            Map<String, Object> heatmap = adminService.getActivityHeatmap(days);
            return ResponseEntity.ok(ApiResponse.success(heatmap));
            
        } catch (Exception e) {
            log.error("Error obteniendo mapa de calor: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error al obtener mapa de calor de actividad"));
        }
    }
}