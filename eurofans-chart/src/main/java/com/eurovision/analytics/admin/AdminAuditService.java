package com.eurovision.analytics.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {

    private final AdminAuditLogRepository repository;

    public AdminAuditService(AdminAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void log(long adminTelegramId, String action, String targetType, String targetId, String detail) {
        repository.save(new AdminAuditLog(adminTelegramId, action, targetType, targetId, detail));
    }
}
