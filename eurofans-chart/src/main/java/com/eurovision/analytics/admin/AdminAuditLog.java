package com.eurovision.analytics.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "admin_audit_logs")
@Getter
@NoArgsConstructor
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_telegram_id", nullable = false)
    private Long adminTelegramId;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "target_type", length = 64)
    private String targetType;

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "detail", columnDefinition = "text")
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public AdminAuditLog(Long adminTelegramId, String action, String targetType, String targetId, String detail) {
        this.adminTelegramId = adminTelegramId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
        this.occurredAt = Instant.now();
    }
}
