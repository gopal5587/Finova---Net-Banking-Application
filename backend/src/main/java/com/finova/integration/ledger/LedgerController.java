package com.finova.integration.ledger;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final BlockchainLedgerService blockchainLedgerService;

    public LedgerController(BlockchainLedgerService blockchainLedgerService) {
        this.blockchainLedgerService = blockchainLedgerService;
    }

    @GetMapping("/chain")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LedgerBlock>> chain() {
        return ResponseEntity.ok(blockchainLedgerService.snapshot());
    }

    @GetMapping("/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> verify() {
        return ResponseEntity.ok(Map.of(
                "valid", blockchainLedgerService.isChainValid(),
                "blocks", blockchainLedgerService.snapshot().size()
        ));
    }
}
