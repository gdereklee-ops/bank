/*
 * Copyright (c) 2025 JP Morgan Chase
 *
 */

package com.jpmchase.bank.service;

import com.jpmchase.bank.entity.FxRate;
import com.jpmchase.bank.repository.FxRateRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CurrencyConversionService {
    private final FxRateRepository fxRateRepository;

    @Autowired
    public CurrencyConversionService(FxRateRepository fxRateRepository) {
        this.fxRateRepository = fxRateRepository;
    }

    public Optional<BigDecimal> convert(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return Optional.of(BigDecimal.ONE);
        }
        Optional<FxRate> rate = fxRateRepository
                .findByFromCurrencyAndToCurrency(fromCurrency, toCurrency);
        if (rate.isPresent()) {
            return Optional.of(fxRateRepository
                    .findByFromCurrencyAndToCurrency(fromCurrency, toCurrency).get().getRate());
        } else {
            return Optional.empty();
        }
    }
}

