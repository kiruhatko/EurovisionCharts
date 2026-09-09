package com.eurovision.analytics.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "telegram_id", nullable = false, unique = true)
    private Long telegramId;

    @Column(name = "telegram_username")
    private String telegramUsername;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "tracking_enabled", nullable = false)
    private boolean trackingEnabled = true;

    @Column(name = "chart_participation_enabled", nullable = false)
    private boolean chartParticipationEnabled = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User(Long telegramId, String telegramUsername, String firstName) {
        this.telegramId = telegramId;
        this.telegramUsername = telegramUsername;
        this.firstName = firstName;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
