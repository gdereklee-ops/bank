/*
 * Copyright (c) 2025 JP Morgan Chase
 *
 */

package com.jpmchase.bank.repository;

import com.jpmchase.bank.entity.FxRate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FxRateRepository extends JpaRepository<FxRate, Long> {
    Optional<FxRate> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
}

