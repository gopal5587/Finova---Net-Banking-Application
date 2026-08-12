package com.finova.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finova.account.Account;
import com.finova.account.AccountRepository;
import com.finova.account.AccountStatus;
import com.finova.account.AccountType;
import com.finova.transaction.Transaction;
import com.finova.transaction.TransactionReferenceGenerator;
import com.finova.transaction.TransactionRepository;
import com.finova.transaction.TransactionType;
import com.finova.user.User;

@ExtendWith(MockitoExtension.class)
class InterestAccrualServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private org.springframework.cache.CacheManager cacheManager;

    private final TransactionReferenceGenerator referenceGenerator = new TransactionReferenceGenerator();
    private InterestAccrualService service;
    private Account savings;

    @BeforeEach
    void setUp() {
        service = new InterestAccrualService(
                accountRepository, transactionRepository, referenceGenerator, cacheManager,
                new BigDecimal("0.04"));

        User owner = new User();
        owner.setId(1L);
        owner.setUsername("bob");

        savings = new Account();
        savings.setId(10L);
        savings.setPublicId(UUID.randomUUID());
        savings.setOwner(owner);
        savings.setAccountType(AccountType.SAVINGS);
        savings.setStatus(AccountStatus.ACTIVE);
        savings.setCurrency("INR");
        savings.setBalance(new BigDecimal("120000.00"));

        lenient().when(cacheManager.getCache(any())).thenReturn(null);
        lenient().when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void creditsMonthlyInterestUsingHalfEvenRounding() {
        when(accountRepository.findByAccountTypeAndStatus(AccountType.SAVINGS, AccountStatus.ACTIVE))
                .thenReturn(List.of(savings));
        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(savings));

        int credited = service.accrueMonthlyInterest();

        // 120000 * 0.04 / 12 = 400.00
        assertThat(credited).isEqualTo(1);
        assertThat(savings.getBalance()).isEqualByComparingTo("120400.00");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(TransactionType.INTEREST);
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("400.00");
    }

    @Test
    void skipsZeroBalanceAccounts() {
        savings.setBalance(BigDecimal.ZERO);
        when(accountRepository.findByAccountTypeAndStatus(AccountType.SAVINGS, AccountStatus.ACTIVE))
                .thenReturn(List.of(savings));
        when(accountRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(savings));

        assertThat(service.accrueMonthlyInterest()).isZero();
    }
}
