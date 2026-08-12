package com.finova.account;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finova.account.dto.AccountResponse;
import com.finova.account.dto.BalanceResponse;
import com.finova.account.dto.CreateAccountRequest;

/**
 * Account management endpoints, all scoped to the authenticated user (username taken from the JWT,
 * never from the request body, so a client cannot act on someone else's account).
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(Authentication auth,
                                                  @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse created = accountService.createAccount(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> list(Authentication auth) {
        return ResponseEntity.ok(accountService.listAccounts(auth.getName()));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> get(Authentication auth, @PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.getAccount(auth.getName(), accountId));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> balance(Authentication auth, @PathVariable UUID accountId) {
        return ResponseEntity.ok(accountService.getBalance(auth.getName(), accountId));
    }
}
