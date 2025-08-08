/*
 * Copyright (c) 2025 JP Morgan Chase
 *
 */

package com.jpmchase.bank.service;

import com.jpmchase.bank.entity.Account;
import com.jpmchase.bank.repository.AccountRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransferService {
    private final AccountRepository accountRepo;
    private final CurrencyConversionService currencyService;
    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("0.01");

    @Autowired
    public TransferService(AccountRepository accountRepo,
                           CurrencyConversionService currencyService) {
        this.accountRepo = accountRepo;
        this.currencyService = currencyService;
    }

    @Transactional
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        if (fromId.equals(toId)) throw new IllegalArgumentException("Cannot transfer to same account");

        Account from = accountRepo.findByIdForUpdate(fromId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        Account to = accountRepo.findByIdForUpdate(toId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        BigDecimal fee = amount.multiply(FEE_PERCENTAGE);
        BigDecimal totalDebit = amount.add(fee);

        if (from.getBalance().compareTo(totalDebit) < 0) {
            throw new IllegalArgumentException("Insufficient funds (including fee)");
        }

        from.setBalance(from.getBalance().subtract(totalDebit));

        BigDecimal convertedAmount;
        if (!from.getCurrency().equals(to.getCurrency())) {
            BigDecimal rate = currencyService.convert(from.getCurrency(), to.getCurrency())
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported currency conversion"));
            convertedAmount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        } else {
            convertedAmount = amount;
        }
        to.setBalance(to.getBalance().add(convertedAmount));

        accountRepo.save(from);
        accountRepo.save(to);
    }
}
