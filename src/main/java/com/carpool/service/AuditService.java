package com.carpool.service;

import com.carpool.entity.AuditLog;
import com.carpool.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String action, String actorId, String targetId, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setActorId(actorId);
        log.setTargetId(targetId);
        log.setDetails(details == null ? "{}" : details);
        auditLogRepository.save(log);
    }
}
