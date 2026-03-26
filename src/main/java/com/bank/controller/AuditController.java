package com.bank.controller;

import com.bank.domain.AuditLog;
import com.bank.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Audit", description = "Audit log endpoints")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @GetMapping("/my-activity")
    @Operation(summary = "Get current user's audit trail")
    public ResponseEntity<Page<AuditLog>> getMyActivity(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditService.getLogsByUser(userDetails.getUsername(), pageable));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit logs for a specific entity (Admin only)")
    public ResponseEntity<Page<AuditLog>> getEntityLogs(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditService.getLogsByEntity(entityType, entityId, pageable));
    }
}
