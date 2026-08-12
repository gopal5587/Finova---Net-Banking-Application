package com.finova.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finova.account.Account;
import com.finova.account.AccountRepository;
import com.finova.account.AccountStatus;
import com.finova.account.AccountType;
import com.finova.common.exception.BusinessRuleException;
import com.finova.transaction.dto.TransferRequest;
import com.finova.user.User;
import com.finova.user.UserRepository;

/**
 * Behavioural tests for {@link TransactionService} money movement, using mocks so the focus stays on
 * balance arithmetic and business-rule enforcement rather than persistence.
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private org.springframework.cache.CacheManager cacheManager;
    @Mock private com.finova.fraud.FraudDetectionService fraudDetectionService;

    private final TransactionReferenceGenerator referenceGenerator = new TransactionReferenceGenerator();

    private TransactionService service;

    private User owner;
    private Account from;
    private Account to;

    @BeforeEach
    void setUp() {
        service = new TransactionService(accountRepository, transactionRepository, userRepository,
                referenceGenerator, cacheManager, fraudDetectionService);

        owner = new User();
        owner.setId(1L);
        owner.setUsername("bob");

        from = account(10L, owner, new BigDecimal("100.00"));
        to = account(20L, owner, new BigDecimal("50.00"));

        lenient().when(userRepository.findByUsernameIgnoreCase("bob")).thenReturn(Optional.of(owner));
        lenient().when(cacheManager.getCache(any())).thenReturn(null);
        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void transferMovesExactAmountBetweenAccounts() {
        stubTransferLookups();

        var request = new TransferRequest(from.getPublicId(), to.getPublicId(), new BigDecimal("30.005"), "rent");
        service.transfer("bob", request);

        // 30.005 rounds HALF_EVEN to 30.00; balances must reflect exact decimal arithmetic.
        assertThat(from.getBalance()).isEqualByComparingTo("70.00");
        assertThat(to.getBalance()).isEqualByComparingTo("80.00");
    }

    @Test
    void transferRejectsInsufficientFunds() {
        stubTransferLookups();

        var request = new TransferRequest(from.getPublicId(), to.getPublicId(), new BigDecimal("1000.00"), null);

        assertThatThrownBy(() -> service.transfer("bob", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient funds");
        assertThat(from.getBalance()).isEqualByComparingTo("100.00");
        assertThat(to.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void transferToSameAccountIsRejected() {
        var request = new TransferRequest(from.getPublicId(), from.getPublicId(), new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> service.transfer("bob", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must differ");
    }

    @Test
    void transferFromFrozenAccountIsRejected() {
        from.setStatus(AccountStatus.FROZEN);
        stubTransferLookups();

        var request = new TransferRequest(from.getPublicId(), to.getPublicId(), new BigDecimal("10.00"), null);

        assertThatThrownBy(() -> service.transfer("bob", request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("frozen");
    }

    private void stubTransferLookups() {
        when(accountRepository.findIdByPublicId(from.getPublicId())).thenReturn(Optional.of(from.getId()));
        when(accountRepository.findIdByPublicId(to.getPublicId())).thenReturn(Optional.of(to.getId()));
        when(accountRepository.findByIdForUpdate(from.getId())).thenReturn(Optional.of(from));
        when(accountRepository.findByIdForUpdate(to.getId())).thenReturn(Optional.of(to));
    }

    private Account account(Long id, User owner, BigDecimal balance) {
        Account account = new Account();
        account.setId(id);
        account.setPublicId(UUID.randomUUID());
        account.setOwner(owner);
        account.setAccountType(AccountType.SAVINGS);
        account.setCurrency("INR");
        account.setBalance(balance);
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }
}
