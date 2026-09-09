package com.eurovision.analytics.connectedaccount;

import com.eurovision.analytics.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "connected_accounts")
@Getter
@Setter
@NoArgsConstructor
public class ConnectedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private Provider provider;

    @Column(name = "provider_account_id", nullable = false)
    private String providerAccountId;

    @Column(name = "provider_display_name")
    private String providerDisplayName;

    @Column(name = "access_token_encrypted")
    private byte[] accessTokenEncrypted;

    @Column(name = "refresh_token_encrypted")
    private byte[] refreshTokenEncrypted;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "scopes")
    private String scopes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ConnectedAccountStatus status = ConnectedAccountStatus.ACTIVE;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;

    @Column(name = "last_token_refresh_at")
    private Instant lastTokenRefreshAt;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "last_sync_status")
    private String lastSyncStatus;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures = 0;

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    public ConnectedAccount(User user, Provider provider, String providerAccountId, String providerDisplayName) {
        this.user = user;
        this.provider = provider;
        this.providerAccountId = providerAccountId;
        this.providerDisplayName = providerDisplayName;
        this.connectedAt = Instant.now();
        this.status = ConnectedAccountStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == ConnectedAccountStatus.ACTIVE;
    }
}
