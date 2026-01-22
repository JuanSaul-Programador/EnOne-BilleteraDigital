package com.enone.domain.repository;

import com.enone.domain.model.Wallet;
import com.enone.domain.model.WalletStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    List<Wallet> findByUserId(Long userId);

    Optional<Wallet> findByUserIdAndCurrency(Long userId, String currency);

    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId AND w.currency = :currency AND w.status = :status")
    Optional<Wallet> findByUserIdAndCurrencyAndStatus(
            @Param("userId") Long userId,
            @Param("currency") String currency,
            @Param("status") WalletStatus status
    );

    List<Wallet> findByUserIdAndStatus(Long userId, WalletStatus status);

    Optional<Wallet> findByWalletNumber(String walletNumber);

    boolean existsByWalletNumber(String walletNumber);

    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId ORDER BY w.createdAt ASC")
    List<Wallet> findByUserIdOrderByCreatedAt(@Param("userId") Long userId);

    @Query("SELECT w.currency as currency, SUM(w.balance) as totalBalance FROM Wallet w " +
            "WHERE w.status = 'ACTIVE' GROUP BY w.currency")
    List<Object[]> getSumOfBalancesByCurrency();

    @Query("SELECT w FROM Wallet w WHERE w.userId IN :userIds")
    List<Wallet> findByUserIdIn(@Param("userIds") List<Long> userIds);

    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.status = :status")
    long countByStatus(@Param("status") WalletStatus status);

    @Query("SELECT w.currency, COUNT(w) FROM Wallet w WHERE w.status = 'ACTIVE' GROUP BY w.currency")
    List<Object[]> countWalletsByCurrency();
}