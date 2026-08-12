package com.finova.fraud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finova.account.Account;
import com.finova.account.AccountStatus;
import com.finova.account.AccountType;
import com.finova.transaction.Transaction;
import com.finova.transaction.TransactionRepository;
import com.finova.transaction.TransactionType;
import com.finova.user.User;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock private FraudFlagRepository fraudFlagRepository;
    @Mock private TransactionRepository transactionRepository;

    private FraudDetectionService service;
    private Account account;

    @BeforeEach
    void setUp() {
        FraudProperties props = new FraudProperties(
                new BigDecimal("100000.00"), 60, 5, 0, 5);
        service = new FraudDetectionService(fraudFlagRepository, transactionRepository, props);

        User owner = new User();
        owner.setId(1L);
        owner.setUsername("bob");

        account = new Account();
        account.setId(10L);
        account.setPublicId(UUID.randomUUID());
        account.setOwner(owner);
        account.setAccountType(AccountType.SAVINGS);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(new BigDecimal("500000.00"));
    }

    @Test
    void largeAmountRaisesHighSeverityFlag() {
        when(transactionRepository.countBySourceAccountIdAndCreatedAtAfter(any(), any())).thenReturn(1L);

        Transaction tx = debit(new BigDecimal("150000.00"), Instant.now());
        service.evaluateDebit(account, tx);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FraudFlag>> captor = ArgumentCaptor.forClass(List.class);
        verify(fraudFlagRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .extracting(FraudFlag::getReason)
                .contains(FraudReason.LARGE_AMOUNT);
    }

    @Test
    void velocityAboveThresholdRaisesFlag() {
        when(transactionRepository.countBySourceAccountIdAndCreatedAtAfter(any(), any())).thenReturn(6L);

        Transaction tx = debit(new BigDecimal("10.00"), Instant.now());
        service.evaluateDebit(account, tx);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FraudFlag>> captor = ArgumentCaptor.forClass(List.class);
        verify(fraudFlagRepository).saveAll(captor.capture());

        assertThat(captor.getValue())
                .extracting(FraudFlag::getReason)
                .contains(FraudReason.VELOCITY);
    }

    @Test
    void quietDaytimeSmallTransferRaisesNothing() {
        // Midday UTC, small amount, low velocity.
        Instant midday = ZonedDateTime.of(2026, 8, 12, 12, 0, 0, 0, ZoneOffset.UTC).toInstant();
        when(transactionRepository.countBySourceAccountIdAndCreatedAtAfter(any(), any())).thenReturn(1L);

        service.evaluateDebit(account, debit(new BigDecimal("25.00"), midday));

        verify(fraudFlagRepository, never()).saveAll(anyList());
    }

    @Test
    void depositWithNoSourceIsIgnored() {
        service.evaluateDebit(null, debit(new BigDecimal("10.00"), Instant.now()));
        verify(fraudFlagRepository, never()).saveAll(anyList());
    }

    private Transaction debit(BigDecimal amount, Instant createdAt) {
        Transaction tx = new Transaction();
        tx.setPublicId(UUID.randomUUID());
        tx.setReference("TXN-TEST");
        tx.setType(TransactionType.TRANSFER);
        tx.setAmount(amount);
        tx.setSourceAccount(account);
        tx.setCreatedAt(createdAt);
        return tx;
    }
}
