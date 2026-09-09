package com.eurovision.analytics.deployment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Audit trail of successful boot-time binding verifications. Purely
 * observational: nothing in the application reads this table back to decide
 * authorization, so it can never become a bypass path for the binding check.
 */
@Entity
@Table(name = "deployment_binding_metadata")
@Getter
@NoArgsConstructor
public class DeploymentBindingMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deployment_id", nullable = false)
    private String deploymentId;

    @Column(name = "authorized_chat_id")
    private Long authorizedChatId;

    @Column(name = "environment", nullable = false, length = 64)
    private String environment;

    @Column(name = "application_identifier", nullable = false)
    private String applicationIdentifier;

    @Column(name = "binding_version", nullable = false, length = 32)
    private String bindingVersion;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    @Column(name = "casual_mode", nullable = false)
    private boolean casualMode;

    public DeploymentBindingMetadata(String deploymentId, Long authorizedChatId, String environment,
                                      String applicationIdentifier, String bindingVersion, boolean casualMode) {
        this.deploymentId = deploymentId;
        this.authorizedChatId = authorizedChatId;
        this.environment = environment;
        this.applicationIdentifier = applicationIdentifier;
        this.bindingVersion = bindingVersion;
        this.casualMode = casualMode;
        this.verifiedAt = Instant.now();
    }
}
