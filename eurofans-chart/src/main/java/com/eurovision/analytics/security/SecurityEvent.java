package com.eurovision.analytics.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "security_events")
@Getter
@NoArgsConstructor
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private SecurityEventType eventType;

    @Column(name = "telegram_user_id")
    private Long telegramUserId;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "detail", columnDefinition = "text")
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public SecurityEvent(SecurityEventType eventType, Long telegramUserId, Long chatId, String detail) {
        this.eventType = eventType;
        this.telegramUserId = telegramUserId;
        this.chatId = chatId;
        this.detail = detail;
        this.occurredAt = Instant.now();
    }
}
