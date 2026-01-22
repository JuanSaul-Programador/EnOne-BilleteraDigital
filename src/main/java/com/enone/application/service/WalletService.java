package com.enone.application.service;



import com.enone.domain.model.Transaction;
import com.enone.domain.model.UserTarjeta;
import com.enone.domain.model.Wallet;
import com.enone.web.dto.wallet.ValidarTarjetaRequest;
import com.enone.web.dto.wallet.ValidarTarjetaResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WalletService {
    
    Wallet getWalletByUserId(Long userId);
    List<Wallet> getAllWalletsByUserId(Long userId);
    List<Transaction> getAllTransactions(Long userId, int limit);
    Optional<UserTarjeta> getTarjetaActivaDeUsuario(Long userId);
    Transaction deposit(Long userId, BigDecimal amount, String description);
    ValidarTarjetaResponse activarTarjeta(Long userId, ValidarTarjetaRequest request);
    Transaction transfer(Long fromUserId, Long toUserId, BigDecimal amount, String description, String token2fa, String currency);
    Transaction convert(Long userId, String fromCurrency, String toCurrency, BigDecimal amount, String description);
    BigDecimal getExchangeRate(String fromCurrency, String toCurrency);
    Wallet getOrCreateWallet(Long userId, String currency);
    void desactivarTarjetaActiva(Long userId);
    Transaction withdraw(Long userId, BigDecimal amount, String description);
}