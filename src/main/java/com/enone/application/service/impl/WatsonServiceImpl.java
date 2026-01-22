package com.enone.application.service.impl;

import com.enone.application.service.WatsonService;
import com.enone.domain.model.Transaction;
import com.enone.domain.model.Wallet;
import com.enone.domain.repository.TransactionRepository;
import com.enone.domain.repository.WalletRepository;
import com.enone.web.dto.chat.ChatResponse;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.assistant.v2.Assistant;
import com.ibm.watson.assistant.v2.model.StatelessMessageInput;
import com.ibm.watson.assistant.v2.model.StatelessMessageResponse;
import com.ibm.watson.assistant.v2.model.MessageStatelessOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class WatsonServiceImpl implements WatsonService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    private Assistant assistant;

    @Value("${ibm.watson.apikey:}")
    private String apiKey;

    @Value("${ibm.watson.url:}")
    private String serviceUrl;

    @Value("${ibm.watson.assistant.id:}")
    private String assistantId;

    @Value("${ibm.watson.environment.id:draft}")
    private String environmentId;

    public WatsonServiceImpl(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    private Assistant getAssistant() {
        if (assistant == null && apiKey != null && !apiKey.isEmpty()) {
            try {
                IamAuthenticator authenticator = new IamAuthenticator.Builder()
                        .apikey(apiKey)
                        .build();
                assistant = new Assistant("2021-06-14", authenticator);
                assistant.setServiceUrl(serviceUrl);
            } catch (Exception e) {

            }
        }
        return assistant;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && assistantId != null && !assistantId.isEmpty();
    }

    @Override
    public ChatResponse sendMessage(Long userId, String messageText, String sessionId) {
        if (!isConfigured()) {
            return ChatResponse.builder()
                    .message("El asistente virtual no está configurado (Falta API Key).")
                    .intentDetected("ERROR_CONFIG")
                    .build();
        }

        try {
            Assistant service = getAssistant();
            if (service == null) {
                throw new RuntimeException("No se pudo conectar con Watson.");
            }

            StatelessMessageInput input = new StatelessMessageInput.Builder()
                    .messageType("text")
                    .text(messageText)
                    .build();

            MessageStatelessOptions.Builder optionsBuilder = new MessageStatelessOptions.Builder()
                    .assistantId(assistantId)
                    .input(input)
                    .userId(userId.toString());

            if (environmentId != null && !environmentId.isEmpty()) {
                optionsBuilder.environmentId(environmentId);
            }

            StatelessMessageResponse response = service.messageStateless(optionsBuilder.build()).execute().getResult();

            String botReply = "No entendí eso.";
            if (response.getOutput() != null && response.getOutput().getGeneric() != null
                    && !response.getOutput().getGeneric().isEmpty()) {
                botReply = response.getOutput().getGeneric().get(0).text();
            }

            String intent = "";
            if (response.getOutput() != null && response.getOutput().getIntents() != null
                    && !response.getOutput().getIntents().isEmpty()) {
                intent = response.getOutput().getIntents().get(0).intent();
            }

            log.info("Watson Intent detectado: {}", intent);

            if ("consultar_saldo".equals(intent) || messageText.toLowerCase().contains("saldo")
                    || messageText.toLowerCase().contains("tengo")) {
                botReply = getSecureBalance(userId);
            } else if ("consultar_movimientos".equals(intent) || messageText.toLowerCase().contains("movimiento")
                    || messageText.toLowerCase().contains("transacción")) {
                botReply = getLastTransaction(userId);
            }

            return ChatResponse.builder()
                    .message(botReply)
                    .sessionId(sessionId)
                    .intentDetected(intent)
                    .build();

        } catch (Exception e) {
            log.error("Error en Watson Service", e);

            if (messageText.toLowerCase().contains("saldo") || messageText.toLowerCase().contains("tengo")) {
                return ChatResponse.builder()
                        .message(getSecureBalance(userId))
                        .intentDetected("consultar_saldo_local")
                        .build();
            } else if (messageText.toLowerCase().contains("movimiento")
                    || messageText.toLowerCase().contains("transacción")) {
                return ChatResponse.builder()
                        .message(getLastTransaction(userId))
                        .intentDetected("consultar_movimientos_local")
                        .build();
            }

            return ChatResponse.builder()
                    .message("Lo siento, hubo un error conectando con el asistente. Intenta más tarde.")
                    .intentDetected("ERROR")
                    .build();
        }
    }

    private String getSecureBalance(Long userId) {
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        if (wallets.isEmpty()) {
            return "No tienes billeteras activas.";
        }

        StringBuilder sb = new StringBuilder("**Saldo Disponible**\n");
        for (Wallet w : wallets) {
            String symbol = "USD".equals(w.getCurrency()) ? "$" : "S/";
            sb.append(w.getCurrency()).append(": ").append(symbol).append(" ")
                    .append(w.getBalance()).append("\n");
        }
        return sb.toString();
    }

    private String getLastTransaction(Long userId) {
        List<Transaction> txs = transactionRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, 1));

        if (txs.isEmpty()) {
            return "No tienes movimientos recientes.";
        }

        Transaction last = txs.get(0);

        String typeFormatted = last.getType().toString();
        switch (last.getType().toString()) {
            case "TRANSFER_IN":
                typeFormatted = "Transferencia Recibida";
                break;
            case "TRANSFER_OUT":
                typeFormatted = "Transferencia Enviada";
                break;
            case "DEPOSIT":
                typeFormatted = "Depósito";
                break;
            case "WITHDRAWAL":
                typeFormatted = "Retiro";
                break;
        }

        String statusFormatted = last.getStatus().toString();
        switch (last.getStatus().toString()) {
            case "COMPLETED":
                statusFormatted = "Completado";
                break;
            case "PENDING":
                statusFormatted = "Pendiente";
                break;
            case "FAILED":
                statusFormatted = "Fallido";
                break;
        }

        String symbol = "USD".equals(last.getCurrency()) ? "$" : "S/";

        return String.format("**Último Movimiento**\n%s\nMonto: %s %s\nEstado: %s",
                typeFormatted, symbol, last.getAmount(), statusFormatted);
    }
}
