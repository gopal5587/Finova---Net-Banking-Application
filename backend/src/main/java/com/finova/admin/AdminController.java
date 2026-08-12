package com.finova.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finova.admin.dto.AdminAccountView;
import com.finova.admin.dto.AuditLogView;

/**
 * Admin oversight API. Access is enforced both by the URL rule in SecurityConfig and by
 * {@link PreAuthorize} here (defence in depth), so a non-admin can never reach these methods.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/accounts")
    public ResponseEntity<Page<AdminAccountView>> accounts(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listAccounts(pageable));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogView>> auditLogs(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listAuditLogs(pageable));
    }

    @PostMapping("/accounts/{accountId}/freeze")
    public ResponseEntity<AdminAccountView> freeze(@PathVariable UUID accountId) {
        return ResponseEntity.ok(adminService.freeze(accountId));
    }

    @PostMapping("/accounts/{accountId}/unfreeze")
    public ResponseEntity<AdminAccountView> unfreeze(@PathVariable UUID accountId) {
        return ResponseEntity.ok(adminService.unfreeze(accountId));
    }
}
