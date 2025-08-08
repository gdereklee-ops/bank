/*
 * Copyright (c) 2025 JP Morgan Chase
 *
 */

package com.jpmchase.bank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jpmchase.bank.entity.FxRate;
import com.jpmchase.bank.repository.FxRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CurrencyConversionServiceTest {

    private FxRateRepository fxRateRepository;
    private CurrencyConversionService service;

    @BeforeEach
    void setUp() {
        fxRateRepository = mock(FxRateRepository.class);
        service = new CurrencyConversionService(fxRateRepository);
    }

    @Test
    void testUSDToAUD() {
        //Given
        final FxRate fxRate = new FxRate(1L, "USD", "AUD", BigDecimal.valueOf(2.00));
        when(fxRateRepository.findByFromCurrencyAndToCurrency("USD", "AUD")).thenReturn(Optional.of(fxRate));

        BigDecimal amount = new BigDecimal("50");
        //When
        Optional<BigDecimal> result = service.convert("USD", "AUD");

        //Then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("100.00"), amount.multiply(result.get())
                .setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void testAUDToUSD() {
        //Given
        final FxRate fxRate = new FxRate(1L, "AUD", "USD", BigDecimal.valueOf(0.5));
        when(fxRateRepository.findByFromCurrencyAndToCurrency("AUD", "USD")).thenReturn(Optional.of(fxRate));
        BigDecimal amount = new BigDecimal("100");

        //When
        Optional<BigDecimal> result = service.convert("AUD", "USD");

        //Then
        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("50.00"), amount.multiply(result.get())
                .setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void testUnsupportedConversion() {
        //Given
        final FxRate fxRate = new FxRate(1L, "AUD", "USD", BigDecimal.valueOf(0.5));
        when(fxRateRepository.findByFromCurrencyAndToCurrency("AUD", "USD")).thenReturn(Optional.of(fxRate));

        //When
        Optional<BigDecimal> result = service.convert("GBP", "CNY");

        //Then
        assertFalse(result.isPresent());
    }
}