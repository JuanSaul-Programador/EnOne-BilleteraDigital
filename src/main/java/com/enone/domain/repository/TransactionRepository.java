package com.enone.domain.repository;

import com.enone.domain.model.Transaction;
import com.enone.domain.model.TransactionStatus;
import com.enone.domain.model.TransactionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.walletId IN " +
            "(SELECT w.id FROM Wallet w WHERE w.userId = :userId) " +
            "ORDER BY t.createdAt DESC")
    List<Transaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.walletId IN " +
            "(SELECT w.id FROM Wallet w WHERE w.userId = :userId) " +
            "AND t.type = :type ORDER BY t.createdAt DESC")
    List<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            Pageable pageable
    );

    @Query("SELECT t FROM Transaction t WHERE t.walletId = :walletId ORDER BY t.createdAt DESC")
    List<Transaction> findLatestByWalletId(@Param("walletId") Long walletId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.walletId IN :walletIds ORDER BY t.createdAt DESC")
    List<Transaction> findLatestByWalletIds(@Param("walletIds") List<Long> walletIds, Pageable pageable);

    @Query("SELECT t FROM Transaction t ORDER BY t.createdAt DESC")
    List<Transaction> findLatestTransactions(Pageable pageable);

    Optional<Transaction> findByTransactionUid(String transactionUid);

    List<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    @Query("SELECT SUM(ABS(t.amount)) FROM Transaction t WHERE t.walletId IN " +
            "(SELECT w.id FROM Wallet w WHERE w.userId = :userId AND w.currency = :currency) " +
            "AND t.createdAt >= :startDate AND t.createdAt <= :endDate " +
            "AND t.status = :status")
    BigDecimal sumDailyVolumeByUserAndCurrency(
            @Param("userId") Long userId,
            @Param("currency") String currency,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("status") TransactionStatus status
    );

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.walletId IN " +
            "(SELECT w.id FROM Wallet w WHERE w.userId = :userId) " +
            "AND t.createdAt >= :startDate")
    Long countTransactionsByUserSince(@Param("userId") Long userId, @Param("startDate") Instant startDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);

    @Query("SELECT t.type, COUNT(t) FROM Transaction t WHERE t.status = 'COMPLETED' GROUP BY t.type")
    List<Object[]> countTransactionsByType();

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.type = :type AND t.status = 'COMPLETED' AND t.currency = :currency")
    BigDecimal sumAmountByTypeAndCurrency(@Param("type") TransactionType type, @Param("currency") String currency);

    @Query("SELECT t FROM Transaction t WHERE t.relatedUserId = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findByRelatedUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
