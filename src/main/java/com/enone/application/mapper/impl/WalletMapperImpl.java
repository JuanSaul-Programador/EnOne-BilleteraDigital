package com.enone.application.mapper.impl;


import com.enone.application.mapper.WalletMapper;
import com.enone.domain.model.Transaction;
import com.enone.domain.model.UserTarjeta;
import com.enone.domain.model.Wallet;
import com.enone.web.dto.wallet.BalanceResponse;
import com.enone.web.dto.wallet.TransactionResponse;
import com.enone.web.dto.wallet.ValidarTarjetaResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class WalletMapperImpl implements WalletMapper {

    @Override
    public BalanceResponse toBalanceResponse(Wallet wallet) {
        return BalanceResponse.builder()
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .walletNumber(wallet.getWalletNumber())
                .build();
    }

    @Override
    public List<BalanceResponse> toBalanceResponseList(List<Wallet> wallets) {
        return wallets.stream()
                .map(this::toBalanceResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TransactionResponse toTransactionResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionUid(transaction.getTransactionUid())
                .securityCode(transaction.getSecurityCode())
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .status(transaction.getStatus().name())
                .balanceAfter(transaction.getBalanceAfter())
                .createdAt(transaction.getCreatedAt())
                .relatedUserId(transaction.getRelatedUserId())
                .build();
    }

    @Override
    public TransactionResponse toTransactionResponse(Transaction transaction, String fromUser, String toUser) {
        TransactionResponse response = toTransactionResponse(transaction);
        response.setFromUser(fromUser);
        response.setToUser(toUser);
        return response;
    }

    @Override
    public List<TransactionResponse> toTransactionResponseList(List<Transaction> transactions) {
        return transactions.stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ValidarTarjetaResponse toValidarTarjetaResponse(UserTarjeta tarjeta, boolean success, String mensaje) {
        return ValidarTarjetaResponse.builder()
                .success(success)
                .mensaje(mensaje)
                .numeroTarjetaEnmascarado(tarjeta != null ? tarjeta.getNumeroTarjetaEnmascarado() : null)
                .holderName(tarjeta != null ? tarjeta.getHolderName() : null)
                .build();
    }

    @Override
    public com.enone.web.dto.wallet.CardStatusResponse toCardStatusResponse(java.util.Optional<UserTarjeta> tarjetaOpt) {
        if (tarjetaOpt.isPresent()) {
            UserTarjeta tarjeta = tarjetaOpt.get();
            return com.enone.web.dto.wallet.CardStatusResponse.builder()
                    .hasActiveCard(true)
                    .maskedCardNumber(tarjeta.getNumeroTarjetaEnmascarado())
                    .holderName(tarjeta.getHolderName())
                    .activatedAt(tarjeta.getFechaVerificacion())
                    .verified(tarjeta.getVerificada())
                    .build();
        } else {
            return com.enone.web.dto.wallet.CardStatusResponse.builder()
                    .hasActiveCard(false)
                    .maskedCardNumber(null)
                    .holderName(null)
                    .activatedAt(null)
                    .verified(false)
                    .build();
        }
    }
}