package com.enone.application.mapper;



import com.enone.domain.model.Transaction;
import com.enone.domain.model.UserTarjeta;
import com.enone.domain.model.Wallet;
import com.enone.web.dto.wallet.BalanceResponse;
import com.enone.web.dto.wallet.CardStatusResponse;
import com.enone.web.dto.wallet.TransactionResponse;
import com.enone.web.dto.wallet.ValidarTarjetaResponse;

import java.util.List;
import java.util.Optional;

public interface WalletMapper {
    
    BalanceResponse toBalanceResponse(Wallet wallet);
    
    List<BalanceResponse> toBalanceResponseList(List<Wallet> wallets);
    
    TransactionResponse toTransactionResponse(Transaction transaction);
    
    TransactionResponse toTransactionResponse(Transaction transaction, String fromUser, String toUser);
    
    List<TransactionResponse> toTransactionResponseList(List<Transaction> transactions);
    
    ValidarTarjetaResponse toValidarTarjetaResponse(UserTarjeta tarjeta, boolean success, String mensaje);
    
    CardStatusResponse toCardStatusResponse(Optional<UserTarjeta> tarjeta);
}