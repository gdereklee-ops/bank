/*
 * Copyright (c) 2025 JP Morgan Chase
 *
 */

package com.jpmchase.bank;

import com.jpmchase.bank.entity.Account;
import com.jpmchase.bank.entity.FxRate;
import com.jpmchase.bank.repository.AccountRepository;
import com.jpmchase.bank.repository.FxRateRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BankApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankApplication.class, args);
    }

    @Bean
    CommandLineRunner init(AccountRepository acctRepo, FxRateRepository fxRateRepo) {
        return args -> {
            acctRepo.save(new Account(null, "Alice", "USD", new BigDecimal("1000"), null));
            acctRepo.save(new Account(null, "Bob", "JPN", new BigDecimal("500"), null));

            fxRateRepo.saveAll(List.of(
                    FxRate.builder().fromCurrency("USD").toCurrency("AUD").rate(new BigDecimal("2")).build(),
                    FxRate.builder().fromCurrency("AUD").toCurrency("USD").rate(new BigDecimal("0.5")).build()
            ));
        };
    }
}
