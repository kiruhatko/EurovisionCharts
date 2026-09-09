package com.eurovision.analytics.deployment;

import com.eurovision.analytics.config.DeploymentBindingProperties;
import com.eurovision.analytics.security.SecurityEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeploymentBindingVerifierTest {

    private static final String APPLICATION_IDENTIFIER = "eurovision-analytics-platform";

    @TempDir
    Path tempDir;

    private KeyPair keyPair;
    private String publicKeyBase64;
    private SecurityEventService securityEventService;
    private DeploymentBindingMetadataRepository metadataRepository;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        keyPair = generator.generateKeyPair();
        publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        securityEventService = Mockito.mock(SecurityEventService.class);
        metadataRepository = Mockito.mock(DeploymentBindingMetadataRepository.class);
    }

    private DeploymentBindingVerifier verifier(DeploymentBindingProperties properties, Path bindingFile) {
        ResourceLoader resourceLoader = new ResourceLoader() {
            @Override
            public Resource getResource(String location) {
                if (location.equals(properties.bindingFilePath())) {
                    return new FileSystemResource(bindingFile);
                }
                return new FileSystemResource(Path.of("/nonexistent"));
            }

            @Override
            public ClassLoader getClassLoader() {
                return getClass().getClassLoader();
            }
        };
        return new DeploymentBindingVerifier(properties, resourceLoader, securityEventService, metadataRepository);
    }

    private String signedDocumentJson(long authorizedChatId, String applicationIdentifier, PrivateKey signingKey) throws Exception {
        String deploymentId = "test-deployment";
        String environment = "test";
        String createdAt = Instant.now().toString();
        String version = "1";
        String nonce = "test-nonce";

        String canonical = String.join("\n", deploymentId, Long.toString(authorizedChatId), environment,
                createdAt, version, applicationIdentifier, nonce);

        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(signingKey);
        signature.update(canonical.getBytes(StandardCharsets.UTF_8));
        String signatureBase64 = Base64.getEncoder().encodeToString(signature.sign());

        return """
                {
                  "deploymentId": "%s",
                  "authorizedChatId": %d,
                  "environment": "%s",
                  "createdAt": "%s",
                  "version": "%s",
                  "applicationIdentifier": "%s",
                  "nonce": "%s",
                  "signature": "%s"
                }
                """.formatted(deploymentId, authorizedChatId, environment, createdAt, version,
                applicationIdentifier, nonce, signatureBase64);
    }

    private DeploymentBindingProperties propertiesFor(long authorizedChatId, Path bindingFile) {
        return new DeploymentBindingProperties(true, Long.toString(authorizedChatId), bindingFile.toString(),
                publicKeyBase64, APPLICATION_IDENTIFIER, "test");
    }

    @Test
    void validSignatureVerifiesSuccessfully() throws Exception {
        long chatId = -1001234567890L;
        Path bindingFile = tempDir.resolve("binding.json");
        Files.writeString(bindingFile, signedDocumentJson(chatId, APPLICATION_IDENTIFIER, keyPair.getPrivate()));

        DeploymentBindingVerifier verifier = verifier(propertiesFor(chatId, bindingFile), bindingFile);
        verifier.verify();

        VerifiedDeploymentBinding result = verifier.requireVerified();
        assertThat(result.casualMode()).isFalse();
        assertThat(result.authorizedChatId()).isEqualTo(chatId);
        assertThat(result.isChatAuthorized(chatId)).isTrue();
        assertThat(result.isChatAuthorized(chatId + 1)).isFalse();
    }

    @Test
    void tamperedPayloadFailsVerification() throws Exception {
        long chatId = -100999L;
        Path bindingFile = tempDir.resolve("binding.json");
        String json = signedDocumentJson(chatId, APPLICATION_IDENTIFIER, keyPair.getPrivate());
        // Tamper with the authorizedChatId after signing, without re-signing.
        String tampered = json.replace(Long.toString(chatId), Long.toString(chatId - 1));
        Files.writeString(bindingFile, tampered);

        DeploymentBindingVerifier verifier = verifier(propertiesFor(chatId - 1, bindingFile), bindingFile);

        assertThatThrownBy(verifier::verify).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void signatureFromWrongKeyFailsVerification() throws Exception {
        long chatId = -55555L;
        KeyPair otherKeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Path bindingFile = tempDir.resolve("binding.json");
        Files.writeString(bindingFile, signedDocumentJson(chatId, APPLICATION_IDENTIFIER, otherKeyPair.getPrivate()));

        DeploymentBindingVerifier verifier = verifier(propertiesFor(chatId, bindingFile), bindingFile);

        assertThatThrownBy(verifier::verify).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mismatchedAuthorizedChatIdFailsEvenWithValidSignature() throws Exception {
        long signedChatId = -111L;
        long configuredChatId = -222L;
        Path bindingFile = tempDir.resolve("binding.json");
        Files.writeString(bindingFile, signedDocumentJson(signedChatId, APPLICATION_IDENTIFIER, keyPair.getPrivate()));

        DeploymentBindingVerifier verifier = verifier(propertiesFor(configuredChatId, bindingFile), bindingFile);

        assertThatThrownBy(verifier::verify).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingBindingFileFailsClosed() {
        Path missing = tempDir.resolve("does-not-exist.json");
        DeploymentBindingVerifier verifier = verifier(propertiesFor(1L, missing), missing);

        assertThatThrownBy(verifier::verify).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void casualModeSkipsVerificationAndAuthorizesAnyChat() {
        DeploymentBindingProperties properties = new DeploymentBindingProperties(
                false, null, null, null, APPLICATION_IDENTIFIER, "dev");
        DeploymentBindingVerifier verifier = verifier(properties, tempDir.resolve("unused.json"));

        verifier.verify();

        VerifiedDeploymentBinding result = verifier.requireVerified();
        assertThat(result.casualMode()).isTrue();
        assertThat(result.isChatAuthorized(123L)).isTrue();
        assertThat(result.isChatAuthorized(-999L)).isTrue();
    }

    @Test
    void requireVerifiedBeforeVerifyThrows() {
        DeploymentBindingVerifier verifier = verifier(propertiesFor(1L, tempDir.resolve("x.json")), tempDir.resolve("x.json"));
        assertThatThrownBy(verifier::requireVerified).isInstanceOf(IllegalStateException.class);
    }
}
