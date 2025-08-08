/*
 * Copyright (c) 2025 JP Morgan Chase
 *
 */

package com.jpmchase.bank.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.jpmchase.bank.entity.Account;
import com.jpmchase.bank.repository.AccountRepository;
import com.jpmchase.bank.repository.FxRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class TransferServiceTest {

    @Mock
    private AccountRepository accountRepo;

    @Mock
    private CurrencyConversionService currencyService;

    @Mock
    private FxRateRepository fxRateRepository;

    @InjectMocks
    private TransferService transferService;

    private final String USD = "USD";
    private final String AUD = "AUD";
    private final String JPN = "JPN";
    private final String CNY = "CNY";
    private Account alice;
    private Account bob;
    private Long aliceId = 1L;
    private Long bobId = 2L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        alice = new Account(aliceId, "Alice", USD, new BigDecimal("1000.00"), null);
        bob = new Account(bobId, "Bob", JPN, new BigDecimal("500.00"), null);
    }

    @Test
    void testTransfer50USDtoAliceToBob() {
        //Transfer 50 USD to Alice
        final BigDecimal transferAmount = BigDecimal.valueOf(50.00);
        alice.setBalance(alice.getBalance().add(transferAmount));
        when(accountRepo.findByIdForUpdate(aliceId)).thenReturn(Optional.of(alice));
        when(accountRepo.findByIdForUpdate(bobId)).thenReturn(Optional.of(bob));

        BigDecimal fxRate = BigDecimal.valueOf(2);

        when(currencyService.convert(USD, AUD)).thenReturn(Optional.of(fxRate));
        //When
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(aliceId, bobId, transferAmount));

        //Then
        assertEquals("Unsupported currency conversion", exception.getMessage());
    }

    @Test
    void testTransfer50USDtoAliceToBobInAUD() {
        //Given Bob account is AUD
        bob.setCurrency(AUD);

        //Transfer 50 USD to Alice
        final BigDecimal transferAmount = BigDecimal.valueOf(50.00);
        alice.setBalance(alice.getBalance().add(transferAmount));
        when(accountRepo.findByIdForUpdate(aliceId)).thenReturn(Optional.of(alice));
        when(accountRepo.findByIdForUpdate(bobId)).thenReturn(Optional.of(bob));

        BigDecimal fxRate = BigDecimal.valueOf(2);

        when(currencyService.convert(USD, AUD)).thenReturn(Optional.of(fxRate));

        //Transfer 50 USD to Bob
        transferService.transfer(aliceId, bobId, transferAmount);

        //Then
        final BigDecimal expectedAmountForAlice = BigDecimal.valueOf(999.50).setScale(2, RoundingMode.HALF_UP);
        final BigDecimal expectedAmountForBlob = BigDecimal.valueOf(600.00).setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedAmountForAlice, alice.getBalance().setScale(2, RoundingMode.HALF_UP));
        assertEquals(expectedAmountForBlob, bob.getBalance().setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void testTransfer50AUDtoBobToALiceRecurringFor20Times() {
        //Given Bob account is AUD
        bob.setCurrency(AUD);

        for(int i = 0 ;i < 20 ;i++) {
            //Transfer 50 AUD to Bob
            final BigDecimal transferAmount = BigDecimal.valueOf(50.00);
            bob.setBalance(bob.getBalance().add(transferAmount));
            when(accountRepo.findByIdForUpdate(aliceId)).thenReturn(Optional.of(alice));
            when(accountRepo.findByIdForUpdate(bobId)).thenReturn(Optional.of(bob));

            BigDecimal fxRate = BigDecimal.valueOf(0.5);
            when(currencyService.convert(AUD, USD)).thenReturn(Optional.of(fxRate));

            //Transfer 50 AUD to Alice
            transferService.transfer(bobId, aliceId, transferAmount);
        }
        //Then
        final BigDecimal expectedAmountForAlice = BigDecimal.valueOf(1500.00).setScale(2, RoundingMode.HALF_UP);
        final BigDecimal expectedAmountForBlob = BigDecimal.valueOf(490.00).setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedAmountForAlice, alice.getBalance().setScale(2, RoundingMode.HALF_UP));
        assertEquals(expectedAmountForBlob, bob.getBalance().setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void testConcurrentTransfers() throws InterruptedException {
        //Given Bob account is AUD
        bob.setCurrency(AUD);

        when(accountRepo.findByIdForUpdate(aliceId)).thenReturn(Optional.of(alice));
        when(accountRepo.findByIdForUpdate(bobId)).thenReturn(Optional.of(bob));

        final BigDecimal transferAmountAUD = BigDecimal.valueOf(20.00);
        final BigDecimal transferAmountUSD = BigDecimal.valueOf(40.00);

        BigDecimal AUDUSDFXRate = BigDecimal.valueOf(0.5);
        when(currencyService.convert(AUD, USD)).thenReturn(Optional.of(AUDUSDFXRate));

        BigDecimal USDAUDFXRate = BigDecimal.valueOf(2);
        when(currencyService.convert(USD, AUD)).thenReturn(Optional.of(USDAUDFXRate));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        Runnable task1 = () -> {
            try {
                // Transfer 20 AUD from Bob to Alice (convert to USD)
                transferService.transfer(bobId, aliceId, transferAmountAUD);
            } catch (Exception e) {
                System.out.println("Error in task1: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                // Transfer 40 USD from Alice to Bob (convert to AUD)
                transferService.transfer(aliceId, bobId, transferAmountUSD);
            } catch (Exception e) {
                System.out.println("Error in task2: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        latch.await();
        executor.shutdown();

        final BigDecimal expectedAmountForAlice = BigDecimal.valueOf(969.60).setScale(2, RoundingMode.HALF_UP);
        final BigDecimal expectedAmountForBlob = BigDecimal.valueOf(559.80).setScale(2, RoundingMode.HALF_UP);

        assertEquals(expectedAmountForAlice, alice.getBalance().setScale(2, RoundingMode.HALF_UP));
        assertEquals(expectedAmountForBlob, bob.getBalance().setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void testTransferMoneyFrom40CNYAliceToBob() {
        //Given Bob account is AUD
        bob.setCurrency(AUD);

        //Transfer 40 CNY from Alice to Bob
        final BigDecimal transferAmount = BigDecimal.valueOf(40.00);
        alice.setBalance(alice.getBalance().add(transferAmount));
        when(accountRepo.findByIdForUpdate(aliceId)).thenReturn(Optional.of(alice));
        when(accountRepo.findByIdForUpdate(bobId)).thenReturn(Optional.of(bob));
        when(currencyService.convert(CNY, AUD)).thenReturn(Optional.empty());

        //When
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(aliceId, bobId, transferAmount));

        //Then
        assertEquals("Unsupported currency conversion", exception.getMessage());
    }

    @Test
    void testSuccessfulTransferSameCurrency() {
        //Given
        bob.setCurrency(USD);
        when(accountRepo.findByIdForUpdate(aliceId)).thenReturn(Optional.of(alice));
        when(accountRepo.findByIdForUpdate(bobId)).thenReturn(Optional.of(bob));

        //When
        transferService.transfer(aliceId, bobId, new BigDecimal("100.00"));

        //Then
        assertEquals(new BigDecimal("899.00"), alice.getBalance().setScale(2, RoundingMode.HALF_UP));
        assertEquals(new BigDecimal("600.00"), bob.getBalance().setScale(2, RoundingMode.HALF_UP));

        verify(accountRepo, times(1)).save(alice);
        verify(accountRepo, times(1)).save(bob);
    }

    @Test
    void testTransferFailsIfSameAccount() {
        //When
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(aliceId, aliceId, new BigDecimal("100")));

        //Then
        assertEquals("Cannot transfer to same account", exception.getMessage());
    }

    @Test
    void testTransferFailsIfInsufficientFunds() {
        //Given
        alice.setBalance(new BigDecimal("50"));
        when(accountRepo.findByIdForUpdate(aliceId)).thenReturn(Optional.of(alice));
        when(accountRepo.findByIdForUpdate(bobId)).thenReturn(Optional.of(bob));

        //When
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(aliceId, bobId, new BigDecimal("50")));

        //Then
        assertEquals("Insufficient funds (including fee)", exception.getMessage());
    }

    @Test
    void testTransferWithCurrencyConversion() {
        //Given
        bob.setCurrency(AUD);
        when(accountRepo.findByIdForUpdate(aliceId)).thenReturn(Optional.of(alice));
        when(accountRepo.findByIdForUpdate(bobId)).thenReturn(Optional.of(bob));

        when(currencyService.convert(USD, AUD))
                .thenReturn(Optional.of(new BigDecimal("0.50")));

        //When
        transferService.transfer(aliceId, bobId, new BigDecimal("100"));

        //Then
        assertEquals(new BigDecimal("899.00"), alice.getBalance());
        assertEquals(new BigDecimal("550.00"), bob.getBalance());
    }
}