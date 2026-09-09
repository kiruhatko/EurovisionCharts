package com.eurovision.analytics.deployment;

import com.eurovision.analytics.config.DeploymentBindingProperties;
import com.eurovision.analytics.security.SecurityEventService;
import com.eurovision.analytics.security.SecurityEventType;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Fail-closed verification of the Ed25519-signed deployment binding.
 * Runs once at boot, before Telegram long-polling is ever registered
 * (see {@code TelegramBotLifecycle}, which depends on this bean).
 *
 * <p>No command in this application can alter, bypass, or re-run this check
 * with different inputs at runtime. The only escape hatch is the explicit,
 * loudly-logged {@code deployment.binding-enabled=false} "casual mode" for
 * single-user personal deployments, which is itself set only via startup
 * configuration.
 */
@Component
public class DeploymentBindingVerifier {

    private static final Logger log = LoggerFactory.getLogger(DeploymentBindingVerifier.class);

    private final DeploymentBindingProperties properties;
    private final ResourceLoader resourceLoader;
    private final SecurityEventService securityEventService;
    private final DeploymentBindingMetadataRepository metadataRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private VerifiedDeploymentBinding verified;

    public DeploymentBindingVerifier(DeploymentBindingProperties properties,
                                      ResourceLoader resourceLoader,
                                      SecurityEventService securityEventService,
                                      DeploymentBindingMetadataRepository metadataRepository) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.securityEventService = securityEventService;
        this.metadataRepository = metadataRepository;
    }

    @PostConstruct
    public void verify() {
        if (!properties.bindingEnabled()) {
            log.warn("############################################################");
            log.warn("# DEPLOYMENT BINDING IS DISABLED (casual mode).             #");
            log.warn("# This bot will operate in ANY chat it is added to, with   #");
            log.warn("# no group-lock enforcement. Do not use this mode for a    #");
            log.warn("# shared/production deployment.                            #");
            log.warn("############################################################");
            this.verified = new VerifiedDeploymentBinding(true, null, null, properties.environment());
            persistAuditRecord(this.verified, "casual");
            return;
        }

        BindingDocument document = loadBindingDocument();
        PublicKey publicKey = loadSigningPublicKey();
        verifySignature(document, publicKey);
        verifyApplicationIdentifier(document);
        verifyAuthorizedChatIdConsistency(document);

        this.verified = new VerifiedDeploymentBinding(
                false,
                document.authorizedChatId(),
                document.deploymentId(),
                document.environment()
        );

        log.info("Deployment binding verified: deploymentId={}, environment={}, authorizedChatId={}",
                document.deploymentId(), document.environment(), document.authorizedChatId());
        persistAuditRecord(this.verified, document.version());
    }

    private void persistAuditRecord(VerifiedDeploymentBinding binding, String version) {
        try {
            metadataRepository.save(new DeploymentBindingMetadata(
                    binding.deploymentId(),
                    binding.authorizedChatId(),
                    binding.environment(),
                    properties.applicationIdentifier(),
                    version,
                    binding.casualMode()));
        } catch (Exception e) {
            log.warn("Could not persist deployment binding audit record: {}", e.getMessage());
        }
    }

    public VerifiedDeploymentBinding requireVerified() {
        if (verified == null) {
            throw new IllegalStateException("Deployment binding has not been verified yet");
        }
        return verified;
    }

    private BindingDocument loadBindingDocument() {
        String path = properties.bindingFilePath();
        if (path == null || path.isBlank()) {
            fail(SecurityEventType.INVALID_BINDING, "No deployment binding file path configured");
        }
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            fail(SecurityEventType.INVALID_BINDING, "Deployment binding file not found: " + path);
        }
        try (InputStream in = resource.getInputStream()) {
            return jsonMapper.readValue(in, BindingDocument.class);
        } catch (IOException e) {
            fail(SecurityEventType.INVALID_BINDING, "Deployment binding file could not be parsed: " + e.getMessage());
            throw new IllegalStateException("unreachable");
        }
    }

    private PublicKey loadSigningPublicKey() {
        String base64Key = properties.signingPublicKey();
        if (base64Key == null || base64Key.isBlank()) {
            fail(SecurityEventType.INVALID_BINDING, "No deployment binding signing public key configured");
        }
        try {
            byte[] derBytes = Base64.getDecoder().decode(base64Key.trim());
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
            return keyFactory.generatePublic(new X509EncodedKeySpec(derBytes));
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            fail(SecurityEventType.INVALID_BINDING, "Deployment binding signing public key is malformed: " + e.getMessage());
            throw new IllegalStateException("unreachable");
        }
    }

    private void verifySignature(BindingDocument document, PublicKey publicKey) {
        if (document.signature() == null || document.signature().isBlank()) {
            fail(SecurityEventType.SIGNATURE_FAILURE, "Deployment binding document has no signature");
        }
        boolean valid;
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update(document.canonicalPayload());
            valid = signature.verify(Base64.getDecoder().decode(document.signature()));
        } catch (Exception e) {
            fail(SecurityEventType.SIGNATURE_FAILURE, "Deployment binding signature verification error: " + e.getMessage());
            throw new IllegalStateException("unreachable");
        }
        if (!valid) {
            fail(SecurityEventType.SIGNATURE_FAILURE, "Deployment binding signature is invalid or the document was tampered with");
        }
    }

    private void verifyApplicationIdentifier(BindingDocument document) {
        String expected = properties.applicationIdentifier();
        if (expected == null || !expected.equals(document.applicationIdentifier())) {
            fail(SecurityEventType.INVALID_BINDING,
                    "Deployment binding applicationIdentifier mismatch (expected=" + expected
                            + ", actual=" + document.applicationIdentifier() + ")");
        }
    }

    private void verifyAuthorizedChatIdConsistency(BindingDocument document) {
        Long configured = properties.authorizedGroupChatIdAsLong();
        if (configured == null) {
            fail(SecurityEventType.INVALID_BINDING, "AUTHORIZED_GROUP_CHAT_ID is not configured");
        }
        if (document.authorizedChatId() == null || !configured.equals(document.authorizedChatId())) {
            fail(SecurityEventType.INVALID_BINDING,
                    "AUTHORIZED_GROUP_CHAT_ID does not match the chat id embedded in the signed binding document");
        }
    }

    private void fail(SecurityEventType type, String detail) {
        log.error("DEPLOYMENT BINDING VERIFICATION FAILED: {}", detail);
        try {
            securityEventService.record(type, null, null, detail);
        } catch (Exception ignored) {
            // The security_events table may not be reachable yet; the startup
            // failure below is the authoritative fail-closed signal regardless.
        }
        throw new IllegalStateException("Deployment binding verification failed: " + detail);
    }
}
