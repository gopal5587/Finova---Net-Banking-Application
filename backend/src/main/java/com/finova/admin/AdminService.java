package com.finova.admin;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finova.account.Account;
import com.finova.account.AccountRepository;
import com.finova.account.AccountStatus;
import com.finova.admin.dto.AdminAccountView;
import com.finova.admin.dto.AuditLogView;
import com.finova.admin.dto.FraudFlagView;
import com.finova.audit.AuditLogRepository;
import com.finova.audit.Auditable;
import com.finova.common.exception.BusinessRuleException;
import com.finova.common.exception.ResourceNotFoundException;
import com.finova.fraud.FraudFlag;
import com.finova.fraud.FraudFlagRepository;

/**
 * Administrative oversight operations. All reads are paginated to stay safe on large datasets, and
 * state-changing actions (freeze/unfreeze) are audited via {@link Auditable}.
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final AccountRepository accountRepository;
    private final AuditLogRepository auditLogRepository;
    private final FraudFlagRepository fraudFlagRepository;

    public AdminService(AccountRepository accountRepository,
                        AuditLogRepository auditLogRepository,
                        FraudFlagRepository fraudFlagRepository) {
        this.accountRepository = accountRepository;
        this.auditLogRepository = auditLogRepository;
        this.fraudFlagRepository = fraudFlagRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminAccountView> listAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable).map(AdminAccountView::from);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogView> listAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByEventTimeDesc(pageable).map(AuditLogView::from);
    }

    @Transactional(readOnly = true)
    public Page<FraudFlagView> listFraudFlags(Boolean resolved, Pageable pageable) {
        Page<FraudFlag> flags = (resolved == null)
                ? fraudFlagRepository.findAllByOrderByCreatedAtDesc(pageable)
                : fraudFlagRepository.findByResolvedOrderByCreatedAtDesc(resolved, pageable);
        return flags.map(FraudFlagView::from);
    }

    @Auditable(action = "ADMIN_RESOLVE_FRAUD_FLAG", targetType = "FraudFlag")
    @Transactional
    public FraudFlagView resolveFraudFlag(UUID flagId) {
        FraudFlag flag = fraudFlagRepository.findByPublicId(flagId)
                .orElseThrow(() -> new ResourceNotFoundException("Fraud flag not found"));
        flag.setResolved(true);
        log.info("Admin resolved fraud flag {}", flagId);
        return FraudFlagView.from(flag);
    }

    @Auditable(action = "ADMIN_FREEZE_ACCOUNT", targetType = "Account")
    @Transactional
    public AdminAccountView freeze(UUID accountId) {
        Account account = requireAccount(accountId);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BusinessRuleException("Closed accounts cannot be frozen");
        }
        account.setStatus(AccountStatus.FROZEN);
        log.warn("Admin froze account {}", accountId);
        return AdminAccountView.from(account);
    }

    @Auditable(action = "ADMIN_UNFREEZE_ACCOUNT", targetType = "Account")
    @Transactional
    public AdminAccountView unfreeze(UUID accountId) {
        Account account = requireAccount(accountId);
        if (account.getStatus() != AccountStatus.FROZEN) {
            throw new BusinessRuleException("Only frozen accounts can be unfrozen");
        }
        account.setStatus(AccountStatus.ACTIVE);
        log.warn("Admin unfroze account {}", accountId);
        return AdminAccountView.from(account);
    }

    private Account requireAccount(UUID accountId) {
        return accountRepository.findByPublicId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }
}
