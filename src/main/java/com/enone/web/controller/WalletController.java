package com.enone.web.controller;

import com.enone.web.dto.common.ApiResponse;
import com.enone.web.dto.wallet.*;
import com.enone.domain.model.Transaction;
import com.enone.domain.model.User;
import com.enone.domain.model.UserTarjeta;
import com.enone.domain.model.Wallet;
import com.enone.exception.ApiException;
import com.enone.application.mapper.WalletMapper;
import com.enone.domain.repository.UserRepository;
import com.enone.application.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;
    private final WalletMapper walletMapper;

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(Authentication authentication) {
        log.info("🏦 GET /balance - Authentication: {}", authentication);
        log.info("🏦 GET /balance - Principal: {}", authentication != null ? authentication.getName() : "NULL");
        log.info("🏦 GET /balance - Authorities: {}",
                authentication != null ? authentication.getAuthorities() : "NULL");

        try {
            Long userId = Long.parseLong(authentication.getName());
            log.info("🏦 GET /balance - UserId extraído: {}", userId);

            Wallet wallet = walletService.getWalletByUserId(userId);
            log.info("🏦 GET /balance - Wallet encontrado: {}", wallet != null ? wallet.getCurrency() : "NULL");

            BalanceResponse response = walletMapper.toBalanceResponse(wallet);
            log.info("✅ GET /balance - Respuesta exitosa para userId: {}", userId);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("💥 ERROR en GET /balance: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionResponse>> deposit(
            Authentication authentication,
            @RequestBody @Valid DepositRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        Transaction tx = walletService.deposit(userId, request.getAmount(), request.getDescription());
        TransactionResponse response = walletMapper.toTransactionResponse(tx);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            Authentication authentication,
            @RequestBody @Valid TransferRequest request) {
        Long userId = Long.parseLong(authentication.getName());

        // Buscar usuario por email O teléfono
        User toUser = userRepository.findByEmailOrPhone(request.getToUsername())
                .orElseThrow(() -> new ApiException(404,
                        "Usuario destino no encontrado. Verifica el email o teléfono: " + request.getToUsername()));

        // Verificar que no se transfiera a sí mismo
        if (toUser.getId().equals(userId)) {
            throw new ApiException(400, "No puedes transferir dinero a ti mismo.");
        }

        // Verificar que el usuario destino esté activo
        if (!toUser.isEnabled()) {
            throw new ApiException(400, "El usuario destino está deshabilitado.");
        }

        String currency = request.getCurrency();
        if (currency == null || (!currency.equals("PEN") && !currency.equals("USD"))) {
            throw new ApiException(400, "Moneda (currency) no válida. Debe ser PEN o USD.");
        }

        Transaction tx = walletService.transfer(
                userId, toUser.getId(), request.getAmount(),
                request.getDescription(), request.getToken2fa(), currency);

        // Obtener nombres para la respuesta
        String toName = toUser.getUsername();
        if (toUser.getProfile() != null) {
            String first = toUser.getProfile().getFirstName();
            String last = toUser.getProfile().getLastName();
            if (first != null || last != null) {
                toName = (first != null ? first : "") + " " + (last != null ? last : "");
                toName = toName.trim();
            }
        }

        // Para transfer, fromUser es el usuario actual (opcional devolverlo) y toUser
        // es el destino
        TransactionResponse response = walletMapper.toTransactionResponse(tx, null, toName);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = Long.parseLong(authentication.getName());
        List<Transaction> transactions = walletService.getAllTransactions(userId, limit);

        // Obtener todos los IDs de usuarios relacionados
        java.util.Set<Long> relatedUserIds = transactions.stream()
                .filter(tx -> tx.getRelatedUserId() != null)
                .map(Transaction::getRelatedUserId)
                .collect(java.util.stream.Collectors.toSet());

        // Mapa para buscar nombres rápidamente
        Map<Long, String> userNames = new java.util.HashMap<>();
        if (!relatedUserIds.isEmpty()) {
            List<User> users = userRepository.findAllById(relatedUserIds);
            for (User u : users) {
                String fullName = u.getUsername(); // Default
                if (u.getProfile() != null) {
                    try {
                        String first = u.getProfile().getFirstName();
                        String last = u.getProfile().getLastName();
                        if (first != null || last != null) {
                            fullName = (first != null ? first : "") + " " + (last != null ? last : "");
                            fullName = fullName.trim();
                        }
                    } catch (Exception e) {
                        // Ignorar lazy loading errors si ocurren, aunque con findAllById debería
                        // traerlo si está configurado
                    }
                }
                userNames.put(u.getId(), fullName);
            }
        }

        List<TransactionResponse> responses = transactions.stream()
                .map(tx -> {
                    String relatedName = null;
                    if (tx.getRelatedUserId() != null) {
                        relatedName = userNames.getOrDefault(tx.getRelatedUserId(), "Usuario");
                    }

                    String fromUser = null;
                    String toUser = null;

                    if ("TRANSFER_IN".equals(tx.getType().name())) {
                        fromUser = relatedName;
                    } else if ("TRANSFER_OUT".equals(tx.getType().name())) {
                        toUser = relatedName;
                    }

                    return walletMapper.toTransactionResponse(tx, fromUser, toUser);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/convert")
    public ResponseEntity<ApiResponse<TransactionResponse>> convert(
            Authentication authentication,
            @RequestBody @Valid ConvertRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        Transaction tx = walletService.convert(
                userId, request.getFromCurrency(), request.getToCurrency(),
                request.getAmount(), request.getDescription());
        TransactionResponse response = walletMapper.toTransactionResponse(tx);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/exchange-rate")
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> getExchangeRate(
            @RequestParam String from,
            @RequestParam String to) {
        if (from == null || to == null || from.isBlank() || to.isBlank()) {
            throw new ApiException(400, "Parámetros 'from' y 'to' son requeridos");
        }
        BigDecimal rate = walletService.getExchangeRate(from, to);
        ExchangeRateResponse response = ExchangeRateResponse.builder()
                .fromCurrency(from)
                .toCurrency(to)
                .rate(rate)
                .timestamp(java.time.Instant.now())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<BalanceResponse>>> getAllWallets(Authentication authentication) {
        log.info("🏦 GET /all - Authentication: {}", authentication);
        log.info("🏦 GET /all - Principal: {}", authentication != null ? authentication.getName() : "NULL");

        try {
            Long userId = Long.parseLong(authentication.getName());
            log.info("🏦 GET /all - UserId extraído: {}", userId);

            List<Wallet> wallets = walletService.getAllWalletsByUserId(userId);
            log.info("🏦 GET /all - Wallets encontrados: {} wallets", wallets != null ? wallets.size() : 0);

            if (wallets != null) {
                wallets.forEach(w -> log.info("🏦 Wallet: {} - Balance: {} {}", w.getWalletNumber(), w.getBalance(),
                        w.getCurrency()));
            }

            List<BalanceResponse> responses = wallets.stream()
                    .map(walletMapper::toBalanceResponse)
                    .collect(Collectors.toList());

            log.info("✅ GET /all - Respuesta exitosa para userId: {} con {} wallets", userId, responses.size());
            return ResponseEntity.ok(ApiResponse.success(responses));
        } catch (Exception e) {
            log.error("💥 ERROR en GET /all: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/activar-tarjeta/status")
    public ResponseEntity<ApiResponse<CardStatusResponse>> getCardStatus(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        try {
            log.info("Verificando estado de tarjeta para userId: {}", userId);
            Optional<UserTarjeta> tarjetaOpt = walletService.getTarjetaActivaDeUsuario(userId);
            CardStatusResponse response = walletMapper.toCardStatusResponse(tarjetaOpt);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error al verificar estado de tarjeta: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error al verificar tarjeta: " + e.getMessage()));
        }
    }

    @PostMapping("/activar-tarjeta")
    public ResponseEntity<ApiResponse<ValidarTarjetaResponse>> activarTarjeta(
            Authentication authentication,
            @RequestBody @Valid ValidarTarjetaRequest request) {
        Long userId = Long.parseLong(authentication.getName());

        log.info("Solicitud de activación de tarjeta para userId: {}", userId);

        try {
            ValidarTarjetaResponse response = walletService.activarTarjeta(userId, request);

            if (response.getSuccess()) {
                log.info("Tarjeta activada exitosamente para userId: {}", userId);
                return ResponseEntity.ok(ApiResponse.success(response));
            } else {
                log.error("Error al activar tarjeta: {}", response.getMensaje());
                return ResponseEntity.badRequest().body(ApiResponse.error(response.getMensaje()));
            }

        } catch (ApiException e) {
            log.error("Excepción API al activar tarjeta: {}", e.getMessage());
            return ResponseEntity.status(e.getStatus()).body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Excepción GENERAL al activar tarjeta: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error interno al activar tarjeta: " + e.getMessage()));
        }
    }

    @PostMapping("/desactivar-tarjeta")
    public ResponseEntity<ApiResponse<String>> desactivarTarjeta(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        log.info("Solicitud para desactivar tarjeta de userId: {}", userId);

        try {
            walletService.desactivarTarjetaActiva(userId);
            return ResponseEntity.ok(ApiResponse.success("Tarjeta desactivada"));
        } catch (Exception e) {
            log.error("Error al desactivar tarjeta: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionResponse>> withdraw(
            Authentication authentication,
            @RequestBody @Valid WithdrawRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        Transaction tx = walletService.withdraw(userId, request.getAmount(), request.getDescription());
        TransactionResponse response = walletMapper.toTransactionResponse(tx);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/validate-recipient")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateRecipient(@RequestParam String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new ApiException(400, "El identificador es requerido");
        }

        User user = userRepository.findByEmailOrPhone(id.trim())
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"));

        // Verificar que el usuario esté activo
        if (!user.isEnabled()) {
            throw new ApiException(400, "El usuario está deshabilitado");
        }

        // Verificar que tenga perfil
        if (user.getProfile() == null) {
            throw new ApiException(400, "Usuario sin perfil completo");
        }

        Map<String, Object> response = Map.of(
                "id", user.getId(),
                "firstName", user.getProfile().getFirstName() != null ? user.getProfile().getFirstName() : "",
                "lastName", user.getProfile().getLastName() != null ? user.getProfile().getLastName() : "",
                "email", user.getProfile().getEmail() != null ? user.getProfile().getEmail() : "");

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}