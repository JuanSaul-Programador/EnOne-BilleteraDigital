package com.enone.util.exchange;

import java.math.BigDecimal;

public interface ExchangeRateService {
    

    BigDecimal getRate(String fromCurrency, String toCurrency);
    

    BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency);

    boolean isServiceAvailable();
}