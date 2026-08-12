package com.finova.transaction;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finova.transaction.dto.MoneyRequest;
import com.finova.transaction.dto.TransactionResponse;
import com.finova.transaction.dto.TransferRequest;

/**
 * Money-movement and statement endpoints. The acting user comes from the JWT; the body/path only
 * ever identifies accounts by their public UUID, and ownership is enforced in the service.
 */
@RestController
@RequestMapping("/api/v1")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransactionResponse> transfer(Authentication auth,
                                                        @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transfer(auth.getName(), request));
    }

    @PostMapping("/accounts/{accountId}/deposit")
    public ResponseEntity<TransactionResponse> deposit(Authentication auth,
                                                       @PathVariable UUID accountId,
                                                       @Valid @RequestBody MoneyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.deposit(auth.getName(), accountId, request));
    }

    @PostMapping("/accounts/{accountId}/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(Authentication auth,
                                                        @PathVariable UUID accountId,
                                                        @Valid @RequestBody MoneyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.withdraw(auth.getName(), accountId, request));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<Page<TransactionResponse>> history(Authentication auth,
                                                             @PathVariable UUID accountId,
                                                             @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(transactionService.history(auth.getName(), accountId, pageable));
    }
}
