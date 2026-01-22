package com.enone.application.service.impl;


import com.enone.application.service.AdminService;
import com.enone.domain.model.Transaction;
import com.enone.domain.model.TransactionStatus;
import com.enone.domain.model.TransactionType;
import com.enone.domain.model.User;
import com.enone.domain.repository.TransactionRepository;
import com.enone.domain.repository.UserProfileRepository;
import com.enone.domain.repository.UserRepository;
import com.enone.domain.repository.WalletRepository;
import com.enone.exception.ApiException;
import com.enone.util.email.EmailService;
import com.enone.web.dto.admin.*;
import com.enone.web.dto.wallet.TransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final EntityManager entityManager;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final UserProfileRepository userProfileRepository;
    private final EmailService emailService;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        log.info("Generando estadísticas del dashboard");
        
        try {

            List<Object[]> balanceResults = walletRepository.getSumOfBalancesByCurrency();
            Map<String, BigDecimal> totalBalances = balanceResults.stream()
                    .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (BigDecimal) row[1]
                    ));

            long totalUsers = userRepository.countAllActiveUsers();
            Instant todayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
            long newUsersToday = userRepository.countNewUsersSince(todayStart);
            
            Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
            long activeUsersLast7Days = countActiveUsersSince(sevenDaysAgo);

            Map<String, Long> userActivityStats = getUserActivityStats();

            log.debug("Obteniendo transacciones por hora...");
            Map<String, Long> hourlyTxCounts = getHourlyTransactionCounts();
            
            log.debug("Obteniendo stats de transacciones...");
            long totalTransactionsToday = countTransactionsSince(todayStart);
            BigDecimal totalVolumeToday = getTotalVolumeSince(todayStart);

            log.debug("Obteniendo transacciones recientes...");
            List<Transaction> recentTransactions = getRecentTransactionsOptimized(10);
            List<TransactionResponse> recentTxResponse = recentTransactions.stream()
                    .map(this::toTransactionResponseForAdmin)
                    .collect(Collectors.toList());

            log.debug("Obteniendo estadísticas destacadas...");
            Map<String, Object> highlightStats = getHighlightStats();

            log.debug("Obteniendo estadísticas de 2FA...");
            Map<String, Long> twoFactorStats = getTwoFactorStats();

            log.info("Dashboard stats generados exitosamente");
            return DashboardStatsDTO.builder()
                    .totalBalancePen(totalBalances.getOrDefault("PEN", BigDecimal.ZERO))
                    .totalBalanceUsd(totalBalances.getOrDefault("USD", BigDecimal.ZERO))
                    .totalUsers(totalUsers)
                    .newUsersToday(newUsersToday)
                    .activeUsersLast7Days(activeUsersLast7Days)
                    .activeUsers(userActivityStats.get("active"))
                    .inactiveUsers(userActivityStats.get("inactive"))
                    .neverUsedUsers(userActivityStats.get("neverUsed"))
                    .disabledUsers(userActivityStats.get("disabled"))
                    .hourlyTxCounts(hourlyTxCounts)
                    .totalTransactionsToday(totalTransactionsToday)
                    .totalVolumeToday(totalVolumeToday)
                    .recentTransactions(recentTxResponse)
                    .twoFactorEnabledCount(twoFactorStats.get("enabled"))
                    .twoFactorDisabledCount(twoFactorStats.get("disabled"))
                    .highlightStats(highlightStats)
                    .generatedAt(Instant.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("ERROR en getDashboardStats: {}", e.getMessage(), e);
            throw e;
        }
    }

    private List<Transaction> getRecentTransactionsOptimized(int limit) {
        String jpql = "SELECT t FROM Transaction t " +
                      "WHERE t.status = :status " +
                      "ORDER BY t.createdAt DESC";
        
        return entityManager.createQuery(jpql, Transaction.class)
                .setParameter("status", TransactionStatus.COMPLETED)
                .setMaxResults(limit)
                .getResultList();
    }

    private Map<String, Long> getHourlyTransactionCounts() {
        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
        
        String jpql = "SELECT FUNCTION('DATE_FORMAT', t.createdAt, '%Y-%m-%d %H:00') as hour_slot, " +
                      "COUNT(t) " +
                      "FROM Transaction t " +
                      "WHERE t.createdAt >= :startDate " +
                      "AND t.status = :status " +
                      "GROUP BY hour_slot " +
                      "ORDER BY hour_slot ASC";
        
        Query query = entityManager.createQuery(jpql);
        query.setParameter("startDate", twentyFourHoursAgo);
        query.setParameter("status", TransactionStatus.COMPLETED);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        return results.stream().collect(Collectors.toMap(
            row -> (String) row[0], 
            row -> (Long) row[1]    
        ));
    }

    private long countActiveUsersSince(Instant since) {
        String jpql = "SELECT COUNT(DISTINCT t.walletId) " +
                      "FROM Transaction t " +
                      "WHERE t.createdAt >= :since " +
                      "AND t.status = :status";
        
        Long count = entityManager.createQuery(jpql, Long.class)
                .setParameter("since", since)
                .setParameter("status", TransactionStatus.COMPLETED)
                .getSingleResult();
        
        return count != null ? count : 0L;
    }

    private Map<String, Long> getUserActivityStats() {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        String activeQuery = 
            "SELECT COUNT(DISTINCT w.userId) " +
            "FROM Wallet w " +
            "WHERE w.status = 'ACTIVE' " +
            "AND EXISTS (" +
            "    SELECT 1 FROM Transaction t " +
            "    WHERE t.walletId = w.id " +
            "    AND t.createdAt >= :sevenDays " +
            "    AND t.status = :txStatus" +
            ")";
        
        Long activeUsers = entityManager.createQuery(activeQuery, Long.class)
                .setParameter("sevenDays", sevenDaysAgo)
                .setParameter("txStatus", TransactionStatus.COMPLETED)
                .getSingleResult();

        String neverUsedQuery = 
            "SELECT COUNT(DISTINCT w.userId) " +
            "FROM Wallet w " +
            "WHERE w.status = 'ACTIVE' " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Transaction t " +
            "    WHERE t.walletId = w.id " +
            "    AND t.status = :txStatus" +
            ")";
        
        Long neverUsedUsers = entityManager.createQuery(neverUsedQuery, Long.class)
                .setParameter("txStatus", TransactionStatus.COMPLETED)
                .getSingleResult();

        String inactiveQuery = 
            "SELECT COUNT(DISTINCT w.userId) " +
            "FROM Wallet w " +
            "WHERE w.status = 'ACTIVE' " +
            "AND EXISTS (" +
            "    SELECT 1 FROM Transaction t " +
            "    WHERE t.walletId = w.id " +
            "    AND t.status = :txStatus" +
            ") " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Transaction t2 " +
            "    WHERE t2.walletId = w.id " +
            "    AND t2.createdAt >= :thirtyDays " +
            "    AND t2.status = :txStatus" +
            ")";
        
        Long inactiveUsers = entityManager.createQuery(inactiveQuery, Long.class)
                .setParameter("thirtyDays", thirtyDaysAgo)
                .setParameter("txStatus", TransactionStatus.COMPLETED)
                .getSingleResult();

        Long disabledUsers = userRepository.countDisabledUsers();
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("active", activeUsers != null ? activeUsers : 0L);
        stats.put("neverUsed", neverUsedUsers != null ? neverUsedUsers : 0L);
        stats.put("inactive", inactiveUsers != null ? inactiveUsers : 0L);
        stats.put("disabled", disabledUsers != null ? disabledUsers : 0L);
        
        return stats;
    }

    private long countTransactionsSince(Instant since) {
        String jpql = "SELECT COUNT(t) FROM Transaction t " +
                      "WHERE t.createdAt >= :since " +
                      "AND t.status = :status";
        
        return entityManager.createQuery(jpql, Long.class)
                .setParameter("since", since)
                .setParameter("status", TransactionStatus.COMPLETED)
                .getSingleResult();
    }

    private BigDecimal getTotalVolumeSince(Instant since) {
        String jpql = "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
                      "WHERE t.createdAt >= :since " +
                      "AND t.status = :status " +
                      "AND t.amount > 0"; 
        
        return entityManager.createQuery(jpql, BigDecimal.class)
                .setParameter("since", since)
                .setParameter("status", TransactionStatus.COMPLETED)
                .getSingleResult();
    }

private  TransactionResponse toTransactionResponseForAdmin(Transaction tx) {
        if (tx == null) {
            return null;
        }

        String fromUser = null;
        String toUser = null;
        
        if (tx.getType() == TransactionType.TRANSFER_IN) {
            fromUser = tx.getRelatedUserId() != null 
                ? "Usuario-" + maskUserId(tx.getRelatedUserId())
                : null;
        } else if (tx.getType() == TransactionType.TRANSFER_OUT) {
            toUser = tx.getRelatedUserId() != null 
                ? "Usuario-" + maskUserId(tx.getRelatedUserId())
                : null;
        }

        Instant createdAtLocal = tx.getCreatedAt()
                .atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ZoneOffset.ofHours(-5))
                .toInstant();

        return TransactionResponse.builder()
                .id(tx.getId())
                .transactionUid(tx.getTransactionUid())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .type(tx.getType().toString())
                .description(sanitizeDescription(tx.getDescription()))
                .status(tx.getStatus().toString())
                .balanceAfter(null)        
                .createdAt(createdAtLocal) 
                .securityCode(null)        
                .fromUser(fromUser)      
                .toUser(toUser)           
                .build();
    }
    
    private String maskUserId(Long userId) {
        if (userId == null) {
            return "N/A";
        }
        
        String userIdStr = String.valueOf(userId);
        if (userIdStr.length() <= 3) {
            return "***" + userIdStr;
        }
        return "***" + userIdStr.substring(userIdStr.length() - 3);
    }

    private String sanitizeDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "Sin descripción";
        }

        String sanitized = description.replaceAll(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", 
            "[email]"
        );

        sanitized = sanitized.replaceAll("\\b\\d{9,}\\b", "[teléfono]");
        sanitized = sanitized.replaceAll("\\b\\d{8}\\b", "[documento]");

        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 97) + "...";
        }
        
        return sanitized;
    }

    @Override
    @Transactional
    public void enableUser(Long userId) {
        log.info("Intentando activar usuario ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado ID: " + userId));

        if (user.getDeletedAt() != null) {
            throw new ApiException(400, "No se puede reactivar una cuenta eliminada permanentemente.");
        }

        if (user.isEnabled()) {
            log.info("Usuario ID {} ya estaba activo.", userId);
            return; 
        }

        user.setEnabled(true);
        userRepository.save(user);
        
        log.info("Usuario ID {} activado exitosamente", userId);
    }

    @Override
    @Transactional
    public void sendMessageToUser(Long userId, AdminMessageRequest request) {
        log.info("Enviando mensaje a usuario ID: {}", userId);
        
        try {
            if (request == null || request.getSubject() == null || request.getSubject().isBlank() ||
                request.getMessage() == null || request.getMessage().isBlank()) {
                throw new ApiException(400, "Asunto y mensaje requeridos.");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(404, "Usuario no encontrado ID: " + userId));

            if (user.getDeletedAt() != null) {
                throw new ApiException(400, "No se puede enviar mensaje a una cuenta eliminada.");
            }

            String userEmail = (user.getProfile() != null) ? user.getProfile().getEmail() : null;
            if (userEmail == null || userEmail.isBlank()) {
                throw new ApiException(404, "Usuario sin email registrado.");
            }

            String userName = (user.getProfile() != null && user.getProfile().getFirstName() != null)
                             ? user.getProfile().getFirstName() : "Usuario";

            String finalMessageBody = "Hola " + userName + ",\n\n" +
                                      "Mensaje del equipo de EnOne:\n\n" +
                                      request.getMessage() + "\n\n" +
                                      "Saludos,\nEl equipo de EnOne";

            emailService.send(userEmail, request.getSubject(), finalMessageBody);
            
            log.info("Mensaje enviado exitosamente a usuario ID: {}", userId);


        } catch (Exception e) { 
            log.error("Error al enviar mensaje a ID {}: {}", userId, e.getMessage());
            if (e instanceof ApiException) throw e;
            throw new ApiException(500, "Error interno al enviar mensaje.");
        }
    }
    
    @Override
    public PagedResponse<UserAdminResponse> listUsers(int page, int size, String search) {
        log.debug("Listando usuarios - página: {}, tamaño: {}, búsqueda: {}", page, size, search);
        
        String searchTerm = (search != null && !search.isBlank()) ? "%" + search.trim().toLowerCase() + "%" : null;

        StringBuilder jpqlData = new StringBuilder("SELECT u FROM User u LEFT JOIN u.profile p WHERE u.deletedAt IS NULL ");

        if (searchTerm != null) {
            jpqlData.append("AND (LOWER(u.username) LIKE :search ") 
                  .append("OR LOWER(p.email) LIKE :search ")     
                  .append("OR LOWER(p.firstName) LIKE :search ") 
                  .append("OR LOWER(p.lastName) LIKE :search ")  
                  .append("OR p.documentNumber LIKE :search) "); 
        }
        jpqlData.append("ORDER BY u.id ASC"); 

        TypedQuery<User> query = entityManager.createQuery(jpqlData.toString(), User.class);
        if (searchTerm != null) {
            query.setParameter("search", searchTerm);
        }
        query.setFirstResult(page * size); 
        query.setMaxResults(size);         

        List<User> users = query.getResultList();

        StringBuilder jpqlCount = new StringBuilder("SELECT COUNT(u.id) FROM User u LEFT JOIN u.profile p WHERE u.deletedAt IS NULL ");
        if (searchTerm != null) {
             jpqlCount.append("AND (LOWER(u.username) LIKE :search OR LOWER(p.email) LIKE :search OR LOWER(p.firstName) LIKE :search OR LOWER(p.lastName) LIKE :search OR p.documentNumber LIKE :search) ");
        }

        TypedQuery<Long> countQuery = entityManager.createQuery(jpqlCount.toString(), Long.class);
        if (searchTerm != null) {
            countQuery.setParameter("search", searchTerm);
        }
        long totalElements = countQuery.getSingleResult();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<UserAdminResponse> userResponses = users.stream()
                .map(user -> UserAdminResponse.builder()
                        .id(user.getId())
                        .firstName(user.getProfile() != null ? user.getProfile().getFirstName() : null)
                        .lastName(user.getProfile() != null ? user.getProfile().getLastName() : null)
                        .email(user.getUsername()) 
                        .enabled(user.isEnabled())
                        .deletedAt(user.getDeletedAt())
                        .build())
                .collect(Collectors.toList());

        return PagedResponse.<UserAdminResponse>builder()
                .content(userResponses)
                .number(page) 
                .size(size)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .build();
    }
    
    @Override
    @Transactional
    public void blockUser(Long userId, BlockUserRequest request) {
        log.info("Intentando bloquear usuario ID: {}", userId);
        
        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new ApiException(400, "Se requiere una razón para bloquear al usuario.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado ID: " + userId));

        if (user.getDeletedAt() != null) {
            throw new ApiException(400, "No se puede bloquear una cuenta eliminada.");
        }
        
        if (!user.isEnabled()) {
            log.info("Usuario ID {} ya estaba bloqueado", userId);
            return; 
        }

        user.setEnabled(false); 
        userRepository.save(user);

        String userEmail = (user.getProfile() != null) ? user.getProfile().getEmail() : null;
        if (userEmail == null || userEmail.isBlank()) {
            log.warn("Usuario bloqueado, pero no se pudo notificar: sin email.");
            return;
        }

        String userName = (user.getProfile() != null && user.getProfile().getFirstName() != null)
                         ? user.getProfile().getFirstName() : "Usuario";

        String subject = "Notificación Importante: Suspensión Temporal de Cuenta EnOne";
        String finalMessageBody = "Hola " + userName + ",\n\n" +
                                  "Te informamos que tu cuenta EnOne ha sido bloqueada temporalmente por motivos de seguridad.\n\n" +
                                  "Razón del bloqueo: " + request.getReason() + "\n\n" +
                                  "Tus fondos están seguros, pero no podrás realizar depósitos, retiros o transferencias hasta que tu cuenta sea reactivada.\n\n" +
                                  "Por favor, comunícate con soporte para más información.\n\n" +
                                  "Saludos,\nEl equipo de EnOne";

        try {
            emailService.send(userEmail, subject, finalMessageBody);
            log.info("Notificación de bloqueo enviada a usuario ID: {}", userId);
        } catch (Exception e) {
            log.error("Error enviando notificación de bloqueo a usuario ID {}: {}", userId, e.getMessage());
        }
        log.info("Usuario ID {} bloqueado exitosamente", userId);
    }  
  @Override
    public Map<String, Long> getUserGrowthStats(int days) {
        log.info("Generando stats de crecimiento de usuarios ({} días)...", days);
        
        try {
            Map<String, Long> dailyCounts = new LinkedHashMap<>();
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                dailyCounts.put(date.toString(), 0L); 
            }

            LocalDateTime startDateTime = today.minusDays(days - 1).atStartOfDay();

            String sql = "SELECT DATE(p.created_at) as signup_date, COUNT(p.user_id) as user_count " +
                        "FROM user_profile p " + 
                        "WHERE p.created_at >= ? " +
                        "GROUP BY DATE(p.created_at) " +
                        "ORDER BY signup_date ASC";

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, startDateTime);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            for (Object[] row : results) {
                String dateFromDB = row[0].toString(); 
                Long countFromDB = (row[1] != null) ? ((Number) row[1]).longValue() : 0L; 
                
                if (dailyCounts.containsKey(dateFromDB)) {
                    dailyCounts.put(dateFromDB, countFromDB);
                }
            }

            log.info("Stats de crecimiento generados: {} días", dailyCounts.size());
            return dailyCounts;
            
        } catch (Exception e) {
            log.error("Error generando stats de crecimiento: {}", e.getMessage(), e);

            Map<String, Long> emptyStats = new LinkedHashMap<>();
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            for (int i = days - 1; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                emptyStats.put(date.toString(), 0L); 
            }
            return emptyStats;
        }
    }

    private Map<String, Long> getTwoFactorStats() {
        try {
            String jpqlEnabled = "SELECT COUNT(p.userId) FROM UserProfile p " +
                               "JOIN p.user u " +
                               "WHERE p.twoFactorEnabled = true " +
                               "AND u.enabled = true " +
                               "AND u.deletedAt IS NULL";
            Long enabledCount = entityManager.createQuery(jpqlEnabled, Long.class).getSingleResult();

            String jpqlDisabled = "SELECT COUNT(p.userId) FROM UserProfile p " +
                                "JOIN p.user u " +
                                "WHERE (p.twoFactorEnabled = false OR p.twoFactorEnabled IS NULL) " +
                                "AND u.enabled = true " +
                                "AND u.deletedAt IS NULL";
            Long disabledCount = entityManager.createQuery(jpqlDisabled, Long.class).getSingleResult();

            Map<String, Long> stats = new HashMap<>();
            stats.put("enabled", enabledCount != null ? enabledCount : 0L);
            stats.put("disabled", disabledCount != null ? disabledCount : 0L);

            log.debug("Stats 2FA - Habilitado: {}, Deshabilitado: {}", enabledCount, disabledCount);
            return stats;

        } catch (Exception e) {
            log.error("Error obteniendo stats de 2FA: {}", e.getMessage(), e);
            Map<String, Long> defaultStats = new HashMap<>();
            defaultStats.put("enabled", 0L);
            defaultStats.put("disabled", 0L);
            return defaultStats;
        }
    }

    @Override
    public Map<String, Object> getRealtimeTransactionStats() {
        log.info("Generando estadísticas de transacciones en tiempo real...");
        
        try {
            Map<String, Object> stats = new HashMap<>();

            String jpqlRecent = "SELECT t FROM Transaction t " +
                              "WHERE t.status = :status " +
                              "ORDER BY t.createdAt DESC";
            
            List<Transaction> recentTx = entityManager.createQuery(jpqlRecent, Transaction.class)
                    .setParameter("status", TransactionStatus.COMPLETED)
                    .setMaxResults(10)
                    .getResultList();
            
            List<Map<String, Object>> recentTxData = recentTx.stream()
                    .map(this::toAnonymizedTransaction)
                    .collect(Collectors.toList());

            Instant last24Hours = Instant.now().minus(24, ChronoUnit.HOURS);
            String jpqlTodayVolume = "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
                                   "WHERE t.createdAt >= :last24Hours " +
                                   "AND t.status = :status";
            
            BigDecimal todayVolume = entityManager.createQuery(jpqlTodayVolume, BigDecimal.class)
                    .setParameter("last24Hours", last24Hours)
                    .setParameter("status", TransactionStatus.COMPLETED)
                    .getSingleResult();

            Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
            String jpqlLastHour = "SELECT COUNT(t) FROM Transaction t " +
                                "WHERE t.createdAt >= :oneHourAgo " +
                                "AND t.status = :status";
            
            Long txLastHour = entityManager.createQuery(jpqlLastHour, Long.class)
                    .setParameter("oneHourAgo", oneHourAgo)
                    .setParameter("status", TransactionStatus.COMPLETED)
                    .getSingleResult();
            
            double txPerMinute = txLastHour != null ? txLastHour / 60.0 : 0.0;

            String jpqlByType = "SELECT t.type, COUNT(t) FROM Transaction t " +
                              "WHERE t.createdAt >= :last24Hours " +
                              "AND t.status = :status " +
                              "GROUP BY t.type";

            List<Object[]> typeResults = entityManager.createQuery(jpqlByType)
                    .setParameter("last24Hours", last24Hours)
                    .setParameter("status", TransactionStatus.COMPLETED)
                    .getResultList();
            
            Map<String, Long> txByType = new HashMap<>();
            for (Object[] row : typeResults) {
                txByType.put(row[0].toString(), ((Number) row[1]).longValue());
            }

            String systemStatus = "OPERATIONAL"; 
            
            stats.put("recentTransactions", recentTxData);
            stats.put("todayVolume", todayVolume);
            stats.put("transactionsPerMinute", Math.round(txPerMinute * 100.0) / 100.0);
            stats.put("transactionsByType", txByType);
            stats.put("systemStatus", systemStatus);
            stats.put("lastUpdated", Instant.now());
            
            log.info("Stats en tiempo real generadas - TX recientes: {}, Volumen: {}, TX/min: {}", 
                    recentTxData.size(), todayVolume, txPerMinute);
            return stats;
            
        } catch (Exception e) {
            log.error("Error generando stats en tiempo real: {}", e.getMessage(), e);

            Map<String, Object> emptyStats = new HashMap<>();
            emptyStats.put("recentTransactions", List.of());
            emptyStats.put("todayVolume", BigDecimal.ZERO);
            emptyStats.put("transactionsPerMinute", 0.0);
            emptyStats.put("transactionsByType", Map.of());
            emptyStats.put("systemStatus", "ERROR");
            emptyStats.put("lastUpdated", Instant.now());
            return emptyStats;
        }
    }

    @Override
    public Map<String, Object> getActivityHeatmap(int days) {
        log.info("Generando mapa de calor de actividad ({} días)...", days);
        
        try {
            Instant startDate = Instant.now().minus(days, ChronoUnit.DAYS);
            
            String jpql = "SELECT t FROM Transaction t WHERE t.status = :status AND t.createdAt >= :startDate ORDER BY t.createdAt DESC";
            List<Transaction> allTransactions = entityManager.createQuery(jpql, Transaction.class)
                    .setParameter("status", TransactionStatus.COMPLETED)
                    .setParameter("startDate", startDate)
                    .setMaxResults(10000) 
                    .getResultList();
            
            log.info("Total transacciones encontradas (últimos {} días): {}", days, allTransactions.size());
            
            if (allTransactions.isEmpty()) {
                log.info("No hay transacciones completadas en los últimos {} días", days);
                return createEmptyHeatmap(days);
            }
            
            return processTransactionsForHeatmap(allTransactions, days);
        } catch (Exception e) {
            log.error("Error generando mapa de calor: {}", e.getMessage(), e);
            return createEmptyHeatmap(days);
        }
    }

    private Map<String, Object> processTransactionsForHeatmap(List<Transaction> transactions, int days) {
        Map<String, Object> heatmap = new HashMap<>();
        
        Long[] hourlyActivity = new Long[24];
        Long[] weeklyActivity = new Long[7];
        for (int i = 0; i < 24; i++) hourlyActivity[i] = 0L;
        for (int i = 0; i < 7; i++) weeklyActivity[i] = 0L;
        
        for (Transaction tx : transactions) {   
            LocalDateTime localDateTime = tx.getCreatedAt()
                    .atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(ZoneOffset.ofHours(-5))
                    .toLocalDateTime();
            
            int hour = localDateTime.getHour();
            int dayOfWeek = localDateTime.getDayOfWeek().getValue() % 7; 
            
            if (hour >= 0 && hour < 24) {
                hourlyActivity[hour]++;
            }
            if (dayOfWeek >= 0 && dayOfWeek < 7) {
                weeklyActivity[dayOfWeek]++;
            }
        }
        
        Long maxActivity = 0L;
        Long totalActivity = 0L;
        int peakHour = 0;
        
        for (int i = 0; i < 24; i++) {
            totalActivity += hourlyActivity[i];
            if (hourlyActivity[i] > maxActivity) {
                maxActivity = hourlyActivity[i];
                peakHour = i;
            }
        }
        
        heatmap.put("hourlyActivity", hourlyActivity);
        heatmap.put("weeklyActivity", weeklyActivity);
        heatmap.put("peakHour", peakHour);
        heatmap.put("maxActivity", maxActivity);
        heatmap.put("totalActivity", totalActivity);
        heatmap.put("averagePerHour", totalActivity > 0 ? totalActivity / 24.0 : 0.0);
        heatmap.put("daysAnalyzed", days);
        heatmap.put("generatedAt", Instant.now().toEpochMilli()); 
        
        log.info("Mapa de calor generado - TX procesadas: {}, Pico: {}:00 con {} transacciones", 
                transactions.size(), peakHour, maxActivity);
        
        return heatmap;
    }

    private Map<String, Object> createEmptyHeatmap(int days) {
        Map<String, Object> emptyHeatmap = new HashMap<>();
        
        Long[] hourlyActivity = new Long[24];
        Long[] weeklyActivity = new Long[7];
        for (int i = 0; i < 24; i++) hourlyActivity[i] = 0L;
        for (int i = 0; i < 7; i++) weeklyActivity[i] = 0L;
        
        emptyHeatmap.put("hourlyActivity", hourlyActivity);
        emptyHeatmap.put("weeklyActivity", weeklyActivity);
        emptyHeatmap.put("peakHour", 0);
        emptyHeatmap.put("maxActivity", 0L);
        emptyHeatmap.put("totalActivity", 0L);
        emptyHeatmap.put("averagePerHour", 0.0);
        emptyHeatmap.put("daysAnalyzed", days);
        emptyHeatmap.put("generatedAt", Instant.now().toEpochMilli()); 
        
        return emptyHeatmap;
    }

    private Map<String, Object> toAnonymizedTransaction(Transaction tx) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", "TX-***" + String.valueOf(tx.getId()).substring(Math.max(0, String.valueOf(tx.getId()).length() - 3)));
        data.put("type", tx.getType().toString());
        data.put("amount", tx.getAmount());
        data.put("currency", tx.getCurrency());
        data.put("status", tx.getStatus().toString());
        data.put("createdAt", tx.getCreatedAt());
        data.put("description", sanitizeDescription(tx.getDescription()));
        return data;
    }

    private Map<String, Object> getHighlightStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
            Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
            
            String countRecentQuery = "SELECT COUNT(t) FROM Transaction t " +
                                     "WHERE t.createdAt >= :sevenDaysAgo " +
                                     "AND t.status = :status";
            Long recentTxCount = entityManager.createQuery(countRecentQuery, Long.class)
                    .setParameter("sevenDaysAgo", sevenDaysAgo)
                    .setParameter("status", TransactionStatus.COMPLETED)
                    .getSingleResult();
            
            Instant timeRange = (recentTxCount > 0) ? sevenDaysAgo : thirtyDaysAgo;
            String timeRangeLabel = (recentTxCount > 0) ? "7 días" : "30 días";
            
            log.debug("Usando rango de tiempo: últimos {} (transacciones encontradas: {})", 
                     timeRangeLabel, recentTxCount);

            String jpqlMostActive = "SELECT COUNT(t), t.walletId FROM Transaction t " +
                                  "WHERE t.createdAt >= :timeRange " +
                                  "AND t.status = :status " +
                                  "GROUP BY t.walletId " +
                                  "ORDER BY COUNT(t) DESC";
            
            Query mostActiveQuery = entityManager.createQuery(jpqlMostActive);
            mostActiveQuery.setParameter("timeRange", timeRange);
            mostActiveQuery.setParameter("status", TransactionStatus.COMPLETED);
            mostActiveQuery.setMaxResults(1);

            List<Object[]> mostActiveResult = mostActiveQuery.getResultList();
            
            Long mostActiveCount = 0L;
            if (!mostActiveResult.isEmpty()) {
                mostActiveCount = ((Number) mostActiveResult.get(0)[0]).longValue();
            }

            String jpqlAllTransactions = "SELECT t.createdAt FROM Transaction t " +
                                        "WHERE t.createdAt >= :timeRange " +
                                        "AND t.status = :status";

            List<Instant> allTxTimes = entityManager.createQuery(jpqlAllTransactions, Instant.class)
                    .setParameter("timeRange", timeRange)
                    .setParameter("status", TransactionStatus.COMPLETED)
                    .getResultList();
            
            Map<Integer, Long> hourCounts = new HashMap<>();
            for (int i = 0; i < 24; i++) {
                hourCounts.put(i, 0L);
            }
            
            for (Instant txTime : allTxTimes) {
                LocalDateTime peruTime = txTime
                        .atZone(ZoneOffset.UTC)
                        .withZoneSameInstant(ZoneOffset.ofHours(-5))
                        .toLocalDateTime();
                int hour = peruTime.getHour();
                hourCounts.put(hour, hourCounts.get(hour) + 1);
            }
            
            int peakHourInt = 0;
            Long maxCount = 0L;
            for (Map.Entry<Integer, Long> entry : hourCounts.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    peakHourInt = entry.getKey();
                }
            }
            
            String peakHour = "N/A";
            if (maxCount > 0) {
                int nextHour = (peakHourInt + 1) % 24;
                peakHour = String.format("%02d:00 - %02d:00", peakHourInt, nextHour);
            }

            String jpqlCurrencyPreference = "SELECT t.currency, COUNT(t) FROM Transaction t " +
                                          "WHERE t.createdAt >= :timeRange " +
                                          "AND t.status = :status " +
                                          "GROUP BY t.currency " +
                                          "ORDER BY COUNT(t) DESC";
            
            Query currencyQuery = entityManager.createQuery(jpqlCurrencyPreference);
            currencyQuery.setParameter("timeRange", timeRange);
            currencyQuery.setParameter("status", TransactionStatus.COMPLETED);
            
            @SuppressWarnings("unchecked")
            List<Object[]> currencyResults = currencyQuery.getResultList();
            
            String preferredCurrency = "N/A";
            if (!currencyResults.isEmpty()) {
                String topCurrency = (String) currencyResults.get(0)[0];
                Long topCount = ((Number) currencyResults.get(0)[1]).longValue();
                
                Long totalTx = currencyResults.stream()
                    .mapToLong(row -> ((Number) row[1]).longValue())
                    .sum();
                
                if (totalTx > 0) {
                    double currencyPercentage = (topCount * 100.0) / totalTx;
                    preferredCurrency = String.format("%s (%.0f%%)", topCurrency, currencyPercentage);
                }
            }

            String jpqlMostUsedMethod = "SELECT t.type, COUNT(t) FROM Transaction t " +
                                      "WHERE t.createdAt >= :timeRange " +
                                      "AND t.status = :status " +
                                      "GROUP BY t.type " +
                                      "ORDER BY COUNT(t) DESC";
            
            Query methodQuery = entityManager.createQuery(jpqlMostUsedMethod);
            methodQuery.setParameter("timeRange", timeRange);
            methodQuery.setParameter("status", TransactionStatus.COMPLETED);
            methodQuery.setMaxResults(1);

            List<Object[]> methodResults = methodQuery.getResultList();
            
            String mostUsedMethod = "N/A";
            if (!methodResults.isEmpty()) {
                Object typeObj = methodResults.get(0)[0];
                String methodType = typeObj.toString();
                
                if (methodType.contains("DEPOSIT")) {
                    mostUsedMethod = "Depósitos";
                } else if (methodType.contains("TRANSFER")) {
                    mostUsedMethod = "Transferencias";
                } else if (methodType.contains("CONVERT")) {
                    mostUsedMethod = "Conversiones";
                } else if (methodType.contains("WITHDRAWAL") || methodType.contains("WITHDRAW")) {
                    mostUsedMethod = "Retiros";
                } else {
                    mostUsedMethod = methodType;
                }
            }
            
            stats.put("mostActiveUserTransactions", mostActiveCount);
            stats.put("peakHour", peakHour);
            stats.put("preferredCurrency", preferredCurrency);
            stats.put("mostUsedMethod", mostUsedMethod);
            stats.put("timeRangeUsed", timeRangeLabel);
            
            log.debug("Estadísticas destacadas (últimos {}): Usuario más activo: {} tx, Hora pico: {}, Moneda: {}, Método: {}", 
                     timeRangeLabel, mostActiveCount, peakHour, preferredCurrency, mostUsedMethod);
            
            return stats;
            
        } catch (Exception e) {
            log.error("ERROR generando estadísticas destacadas: {}", e.getMessage(), e);
            
            Map<String, Object> defaultStats = new HashMap<>();
            defaultStats.put("mostActiveUserTransactions", 0L);
            defaultStats.put("peakHour", "N/A");
            defaultStats.put("preferredCurrency", "N/A");
            defaultStats.put("mostUsedMethod", "N/A");
            defaultStats.put("timeRangeUsed", "N/A");
            return defaultStats;
        }
    }
}