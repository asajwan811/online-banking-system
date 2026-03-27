package com.bank.service;

import com.bank.domain.AuditLog;
import com.bank.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void log_SavesAuditLogWithCorrectFields() {
        AuditLog saved = new AuditLog("TRANSFER", "TRANSACTION", 1L, "testuser", "Transfer details");
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(saved);

        auditService.log("TRANSFER", "TRANSACTION", 1L, "testuser", "Transfer details");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog captured = captor.getValue();
        assertEquals("TRANSFER", captured.getAction());
        assertEquals("TRANSACTION", captured.getEntityType());
        assertEquals(1L, captured.getEntityId());
        assertEquals("testuser", captured.getPerformedBy());
    }

    @Test
    void getLogsByUser_ReturnsPaginatedResults() {
        AuditLog log = new AuditLog("LOGIN", "USER", 1L, "testuser", "Login event");
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findByPerformedByOrderByCreatedAtDesc(eq("testuser"), any()))
                .thenReturn(page);

        Page<AuditLog> result = auditService.getLogsByUser("testuser", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("LOGIN", result.getContent().get(0).getAction());
    }

    @Test
    void getLogsByEntity_ReturnsCorrectLogs() {
        AuditLog log = new AuditLog("TRANSFER", "TRANSACTION", 5L, "user1", "details");
        Page<AuditLog> page = new PageImpl<>(List.of(log));
        when(auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(eq("TRANSACTION"), eq(5L), any()))
                .thenReturn(page);

        Page<AuditLog> result = auditService.getLogsByEntity("TRANSACTION", 5L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(5L, result.getContent().get(0).getEntityId());
    }
}
