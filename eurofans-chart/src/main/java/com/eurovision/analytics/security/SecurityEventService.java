package com.eurovision.analytics.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityEventService {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventService.class);

    private final SecurityEventRepository repository;

    public SecurityEventService(SecurityEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Persisted in its own transaction so a security event survives even when
     * the operation that triggered it is about to be rolled back or has
     * already thrown.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(SecurityEventType type, Long telegramUserId, Long chatId, String detail) {
        log.warn("SecurityEvent[{}] user={} chat={} detail={}", type, telegramUserId, chatId, detail);
        repository.save(new SecurityEvent(type, telegramUserId, chatId, detail));
    }
}
