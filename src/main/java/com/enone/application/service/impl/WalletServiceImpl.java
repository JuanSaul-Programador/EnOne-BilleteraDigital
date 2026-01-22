package com.enone.application.service.impl;

import com.enone.application.service.MockBancoService;
import com.enone.application.service.TwoFactorAuthService;
import com.enone.application.service.WalletService;
import com.enone.domain.model.*;
import com.enone.domain.repository.*;
import com.enone.exception.ApiException;

import com.enone.util.exchange.ExchangeRateService;
import com.enone.web.dto.wallet.RealizarAbonoRequest;
import com.enone.web.dto.wallet.RealizarAbonoResponse;
import com.enone.web.dto.wallet.ValidarTarjetaRequest;
import com.enone.web.dto.wallet.ValidarTarjetaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserTarjetaRepository userTarjetaRepository;
    private final ExchangeRateService exchangeRateService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final MockBancoService mockBancoService;

    private static final Random random = new Random();

    private String generateSecurityCode() {
        return String.format("%03d", random.nextInt(1000));
    }

    @Override
    public Wallet getWalletByUserId(Long userId) {
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        if (wallets.isEmpty()) {
            throw new ApiException(404, "Wallet no encontrado");
        }
        return wallets.get(0); // Retorna el primer wallet
    }

    @Override
    public List<Wallet> getAllWalletsByUserId(Long userId) {
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        if (wallets.isEmpty()) {
            throw new ApiException(404, "No se encontraron wallets para el usuario");
        }
        log.info("Se encontraron {} wallets para userId: {}", wallets.size(), userId);
        return wallets;
    }

    @Override
    public List<Transaction> getAllTransactions(Long userId, int limit) {
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        if (wallets.isEmpty()) {
            throw new ApiException(404, "No se encontraron wallets");
        }
        List<Long> walletIds = wallets.stream().map(Wallet::getId).toList();
        return transactionRepository.findLatestByWalletIds(walletIds,
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public Transaction deposit(Long userId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(400, "Monto de depósito debe ser positivo");
        }

        log.info("Iniciando depósito para usuario: {}, monto: {}", userId, amount);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"));

        if (!user.isEnabled()) {
            throw new ApiException(403,
                    "Su cuenta se encuentra bloqueada. No puede realizar depósitos. Por favor, comuníquese con soporte.");
        }

        Optional<UserTarjeta> tarjetaOpt = userTarjetaRepository.findByUserIdAndActivaTrue(userId);
        if (tarjetaOpt.isEmpty()) {
            throw new ApiException(400, "No tienes una tarjeta activa. Por favor, activa una tarjeta primero.");
        }

        UserTarjeta tarjeta = tarjetaOpt.get();
        log.info("Tarjeta encontrada: {}", tarjeta.getNumeroTarjetaEnmascarado());

        // Realizar cobro en el banco
        com.enone.web.dto.wallet.RealizarCobroRequest cobroRequest = com.enone.web.dto.wallet.RealizarCobroRequest
                .builder()
                .numeroTarjeta(tarjeta.getNumeroTarjetaCompleto())
                .monto(amount)
                .build();

        com.enone.web.dto.wallet.RealizarCobroResponse cobroResponse = mockBancoService.realizarCobro(cobroRequest);

        if (!cobroResponse.getSuccess()) {
            throw new ApiException(400, "Error al cobrar en la tarjeta: " + cobroResponse.getMensaje());
        }

        log.info("Cobro exitoso en el banco - Transaction ID: {}", cobroResponse.getTransactionId());

        // Actualizar wallet
        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, "PEN")
                .orElseThrow(() -> new ApiException(404, "Wallet PEN no encontrada"));

        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        // Crear transacción
        Transaction transaction = Transaction.builder()
                .walletId(wallet.getId())
                .amount(amount)
                .currency(wallet.getCurrency())
                .type(TransactionType.DEPOSIT)
                .description(description != null ? description
                        : "Depósito desde tarjeta " + tarjeta.getNumeroTarjetaEnmascarado())
                .status(TransactionStatus.COMPLETED)
                .balanceAfter(newBalance)
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Depósito completado - ID: {}, UID: {}, Nuevo saldo: {}",
                transaction.getId(), transaction.getTransactionUid(), newBalance);

        return transaction;
    }

    @Override
    @Transactional
    public Transaction transfer(Long fromUserId, Long toUserId, BigDecimal amount, String description, String token2fa,
            String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(400, "Monto debe ser positivo");
        }
        if (fromUserId.equals(toUserId)) {
            throw new ApiException(400, "No se puede transferir a sí mismo");
        }

        log.info("Iniciando transferencia de {} a {}, monto: {} {}", fromUserId, toUserId, amount, currency);

        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new ApiException(404, "Usuario origen no encontrado"));

        if (!fromUser.isEnabled()) {
            throw new ApiException(403,
                    "Su cuenta se encuentra bloqueada. No puede realizar transacciones. Por favor, comuníquese con soporte.");
        }

        // Verificar 2FA si está habilitado
        boolean has2FA = twoFactorAuthService.isEnabled(fromUserId);
        if (has2FA) {
            if (token2fa == null || token2fa.trim().isEmpty()) {
                throw new ApiException(400, "Código 2FA requerido");
            }
            if (!twoFactorAuthService.verifyCode(fromUserId, token2fa)) {
                throw new ApiException(400, "Código 2FA inválido");
            }
        }

        String currencyLabel = currency.toUpperCase();

        Wallet fromWallet = walletRepository.findByUserIdAndCurrency(fromUserId, currencyLabel)
                .orElseThrow(() -> new ApiException(404, "Wallet " + currencyLabel + " origen no encontrada"));
        Wallet toWallet = walletRepository.findByUserIdAndCurrency(toUserId, currencyLabel)
                .orElseThrow(() -> new ApiException(404, "Wallet " + currencyLabel + " destino no encontrada"));

        UserProfile fromProfile = userProfileRepository.findByUserId(fromUserId)
                .orElseThrow(() -> new ApiException(404, "Perfil del usuario origen no encontrado"));

        // Verificar y resetear volumen diario
        checkAndResetDailyVolume(fromProfile);

        // Verificar límites diarios
        BigDecimal userLimit = fromProfile.getDailyTransactionLimit();
        if (userLimit == null) {
            userLimit = AuthServiceImpl.DEFAULT_USER_LIMIT;
        }

        BigDecimal dailyVolumePen = fromProfile.getDailyVolumePen() != null
                ? fromProfile.getDailyVolumePen()
                : BigDecimal.ZERO;
        BigDecimal dailyVolumeUsd = fromProfile.getDailyVolumeUsd() != null
                ? fromProfile.getDailyVolumeUsd()
                : BigDecimal.ZERO;

        BigDecimal USD_TO_PEN_RATE = new BigDecimal("3.75");
        BigDecimal totalDailyVolumeInPen = dailyVolumePen.add(
                dailyVolumeUsd.multiply(USD_TO_PEN_RATE));

        BigDecimal amountInPen = "USD".equals(currencyLabel)
                ? amount.multiply(USD_TO_PEN_RATE)
                : amount;

        BigDecimal newTotalVolume = totalDailyVolumeInPen.add(amountInPen);

        if (newTotalVolume.compareTo(userLimit) > 0) {
            BigDecimal remaining = userLimit.subtract(totalDailyVolumeInPen);
            throw new ApiException(400,
                    String.format("Límite diario excedido. Límite: S/ %s, Gastado hoy: S/ %s, Disponible: S/ %s",
                            userLimit.toPlainString(),
                            totalDailyVolumeInPen.toPlainString(),
                            remaining.toPlainString()));
        }

        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new ApiException(400, "Saldo insuficiente");
        }

        // Realizar transferencia
        BigDecimal newFromBalance = fromWallet.getBalance().subtract(amount);
        BigDecimal newToBalance = toWallet.getBalance().add(amount);

        fromWallet.setBalance(newFromBalance);
        fromWallet.setUpdatedAt(Instant.now());
        toWallet.setBalance(newToBalance);
        toWallet.setUpdatedAt(Instant.now());

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        String secCode = generateSecurityCode();

        // Crear transacciones
        Transaction transactionOut = Transaction.builder()
                .walletId(fromWallet.getId())
                .amount(amount.negate())
                .currency(fromWallet.getCurrency())
                .type(TransactionType.TRANSFER_OUT)
                .description(description)
                .status(TransactionStatus.COMPLETED)
                .balanceAfter(newFromBalance)
                .relatedUserId(toUserId)
                .securityCode(secCode)
                .build();
        transactionRepository.save(transactionOut);

        Transaction transactionIn = Transaction.builder()
                .walletId(toWallet.getId())
                .amount(amount)
                .currency(toWallet.getCurrency())
                .type(TransactionType.TRANSFER_IN)
                .description(description)
                .status(TransactionStatus.COMPLETED)
                .balanceAfter(newToBalance)
                .relatedUserId(fromUserId)
                .securityCode(secCode)
                .build();
        transactionRepository.save(transactionIn);

        // Actualizar volumen diario
        if ("PEN".equals(currencyLabel)) {
            fromProfile.setDailyVolumePen(dailyVolumePen.add(amount));
        } else if ("USD".equals(currencyLabel)) {
            fromProfile.setDailyVolumeUsd(dailyVolumeUsd.add(amount));
        }
        userProfileRepository.save(fromProfile);

        log.info("Transferencia completada - ID: {}, UID: {}, Code: {}",
                transactionOut.getId(), transactionOut.getTransactionUid(), secCode);

        // TODO: Implementar notificación WebSocket
        // NotificationSocketServer.sendNotification(toUserId, notificationMessage);

        return transactionOut;
    }

    private void checkAndResetDailyVolume(UserProfile profile) {
        Instant now = Instant.now();
        Instant lastReset = profile.getLastVolumeResetAt();

        if (lastReset == null || now.isAfter(lastReset.plus(24, ChronoUnit.HOURS))) {
            profile.setDailyVolumePen(BigDecimal.ZERO);
            profile.setDailyVolumeUsd(BigDecimal.ZERO);
            profile.setLastVolumeResetAt(now);

            log.debug("Volumen diario reseteado para userId: {}", profile.getUserId());
        }
    }

    @Override
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        return exchangeRateService.getRate(fromCurrency, toCurrency);
    }

    @Override
    @Transactional
    public Wallet getOrCreateWallet(Long userId, String currency) {
        Optional<Wallet> existing = walletRepository.findByUserIdAndCurrency(userId, currency);
        if (existing.isPresent()) {
            return existing.get();
        }

        String walletNumber = generateWalletNumber();
        Wallet newWallet = Wallet.builder()
                .userId(userId)
                .walletNumber(walletNumber)
                .balance(BigDecimal.ZERO)
                .currency(currency)
                .status(WalletStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        newWallet = walletRepository.save(newWallet);
        log.info("Wallet creada para usuario {} en {}", userId, currency);
        return newWallet;
    }

    @Override
    @Transactional
    public Transaction convert(Long userId, String fromCurrency, String toCurrency, BigDecimal amount,
            String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(400, "Monto debe ser positivo");
        }
        if (fromCurrency.equals(toCurrency)) {
            throw new ApiException(400, "No se puede convertir a la misma moneda");
        }

        log.info("Iniciando conversión para usuario: {}, {} {} a {}", userId, amount, fromCurrency, toCurrency);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"));

        if (!user.isEnabled()) {
            throw new ApiException(403,
                    "Su cuenta se encuentra bloqueada. No puede realizar conversiones. Por favor, comuníquese con soporte.");
        }

        Wallet fromWallet = walletRepository.findByUserIdAndCurrency(userId, fromCurrency)
                .orElseThrow(() -> new ApiException(404, "Wallet " + fromCurrency + " no encontrada"));
        Wallet toWallet = getOrCreateWallet(userId, toCurrency);

        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new ApiException(400, "Saldo insuficiente en " + fromCurrency);
        }

        BigDecimal rate = exchangeRateService.getRate(fromCurrency, toCurrency);
        BigDecimal convertedAmount = amount.multiply(rate).setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal newFromBalance = fromWallet.getBalance().subtract(amount);
        BigDecimal newToBalance = toWallet.getBalance().add(convertedAmount);

        fromWallet.setBalance(newFromBalance);
        fromWallet.setUpdatedAt(Instant.now());
        toWallet.setBalance(newToBalance);
        toWallet.setUpdatedAt(Instant.now());

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        Transaction outTransaction = Transaction.builder()
                .walletId(fromWallet.getId())
                .amount(amount.negate())
                .currency(fromCurrency)
                .type(TransactionType.CONVERT_OUT)
                .description(description)
                .status(TransactionStatus.COMPLETED)
                .balanceAfter(newFromBalance)
                .build();
        transactionRepository.save(outTransaction);

        Transaction inTransaction = Transaction.builder()
                .walletId(toWallet.getId())
                .amount(convertedAmount)
                .currency(toCurrency)
                .type(TransactionType.CONVERT_IN)
                .description(description)
                .status(TransactionStatus.COMPLETED)
                .balanceAfter(newToBalance)
                .build();
        transactionRepository.save(inTransaction);

        log.info("Conversión completada - ID: {}, UID: {}", outTransaction.getId(), outTransaction.getTransactionUid());
        return outTransaction;
    }

    @Override
    @Transactional
    public ValidarTarjetaResponse activarTarjeta(Long userId, ValidarTarjetaRequest request) {
        log.info("Activando tarjeta para usuario: {}", userId);

        ValidarTarjetaResponse response = mockBancoService.validarTarjeta(request);
        if (!response.getSuccess()) {
            throw new ApiException(400, "Validación fallida: " + response.getMensaje());
        }

        userTarjetaRepository.desactivarTarjetasDeUsuario(userId);

        Optional<UserTarjeta> tarjetaExistenteOpt = userTarjetaRepository
                .findByNumeroTarjetaCompleto(request.getNumeroTarjeta());

        if (tarjetaExistenteOpt.isPresent()) {
            UserTarjeta tarjetaExistente = tarjetaExistenteOpt.get();
            User owner = userRepository.findById(tarjetaExistente.getUserId()).orElse(null);

            if (owner != null && owner.isEnabled() && !owner.getId().equals(userId)) {
                throw new ApiException(409, "Esta tarjeta ya está registrada y verificada por otro usuario.");
            } else {
                tarjetaExistente.setUserId(userId);
                tarjetaExistente.setActiva(true);
                tarjetaExistente.setVerificada(true);
                tarjetaExistente.setFechaVerificacion(Instant.now());
                tarjetaExistente.setHolderName(response.getHolderName());

                userTarjetaRepository.save(tarjetaExistente);

                return ValidarTarjetaResponse.builder()
                        .success(true)
                        .mensaje("Tarjeta activada exitosamente (reactivada)")
                        .numeroTarjetaEnmascarado(tarjetaExistente.getNumeroTarjetaEnmascarado())
                        .holderName(tarjetaExistente.getHolderName())
                        .build();
            }
        } else {
            // Crear nueva tarjeta
            UserTarjeta nuevaTarjeta = UserTarjeta.builder()
                    .userId(userId)
                    .numeroTarjetaCompleto(request.getNumeroTarjeta())
                    .numeroTarjetaEnmascarado(response.getNumeroTarjetaEnmascarado())
                    .holderName(response.getHolderName())
                    .verificada(true)
                    .activa(true)
                    .fechaVerificacion(Instant.now())
                    .build();

            userTarjetaRepository.save(nuevaTarjeta);

            return ValidarTarjetaResponse.builder()
                    .success(true)
                    .mensaje("Tarjeta activada exitosamente")
                    .numeroTarjetaEnmascarado(response.getNumeroTarjetaEnmascarado())
                    .holderName(response.getHolderName())
                    .build();
        }
    }

    @Override
    @Transactional
    public void desactivarTarjetaActiva(Long userId) {
        log.info("Desactivando tarjeta para userId: {}", userId);
        userTarjetaRepository.desactivarTarjetasDeUsuario(userId);
        log.info("Tarjeta desactivada para userId: {}", userId);
    }

    @Override
    @Transactional
    public Transaction withdraw(Long userId, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(400, "Monto de retiro debe ser positivo");
        }

        log.info("Iniciando retiro para usuario: {}, monto: {}", userId, amount);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "Usuario no encontrado"));

        if (!user.isEnabled()) {
            throw new ApiException(403,
                    "Su cuenta se encuentra bloqueada. No puede realizar retiros. Por favor, comuníquese con soporte.");
        }

        UserTarjeta tarjetaActiva = userTarjetaRepository.findByUserIdAndActivaTrue(userId)
                .orElseThrow(() -> new ApiException(400, "No tienes una tarjeta activa para retirar."));

        Wallet wallet = walletRepository.findByUserIdAndCurrency(userId, "PEN")
                .orElseThrow(() -> new ApiException(404, "Wallet PEN no encontrada"));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new ApiException(400, "Saldo insuficiente en tu wallet EnOne.");
        }

        BigDecimal newBalanceEnOne = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalanceEnOne);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        RealizarAbonoRequest abonoRequest = RealizarAbonoRequest.builder()
                .numeroTarjeta(tarjetaActiva.getNumeroTarjetaCompleto())
                .monto(amount)
                .build();

        RealizarAbonoResponse abonoResponse = mockBancoService.acreditarDinero(abonoRequest);

        if (!abonoResponse.getSuccess()) {
            throw new ApiException(500, "El banco destino rechazó el retiro: " + abonoResponse.getMensaje());
        }

        // Crear transacción
        Transaction transaction = Transaction.builder()
                .walletId(wallet.getId())
                .amount(amount.negate())
                .currency("PEN")
                .type(TransactionType.WITHDRAWAL)
                .description(description != null ? description
                        : "Retiro a tarjeta " + tarjetaActiva.getNumeroTarjetaEnmascarado())
                .status(TransactionStatus.COMPLETED)
                .balanceAfter(newBalanceEnOne)
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Retiro completado - ID: {}, UID: {}", transaction.getId(), transaction.getTransactionUid());
        return transaction;
    }

    @Override
    public Optional<UserTarjeta> getTarjetaActivaDeUsuario(Long userId) {
        // Best practice: Fetch the most recently updated active card to avoid
        // NonUniqueResultException
        // if multiple active cards exist due to data inconsistencies.
        return userTarjetaRepository.findTopByUserIdAndActivaTrueOrderByUpdatedAtDesc(userId);
    }

    private String generateWalletNumber() {
        return "EN" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase();
    }
}