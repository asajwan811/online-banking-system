package com.bank.service;

import com.bank.domain.AuditLog;
import com.bank.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, Long entityId, String performedBy, String details) {
        AuditLog log = new AuditLog(action, entityType, entityId, performedBy, details);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByUser(String username, Pageable pageable) {
        return auditLogRepository.findByPerformedByOrderByCreatedAtDesc(username, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByEntity(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable);
    }
}
