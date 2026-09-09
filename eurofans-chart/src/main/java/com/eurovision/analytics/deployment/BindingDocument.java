package com.eurovision.analytics.deployment;

import java.nio.charset.StandardCharsets;

/**
 * The signed deployment-binding artifact produced offline by
 * {@code scripts/generate-binding.sh} and shipped alongside (never inside) the
 * application image. Every field except {@code signature} is part of the
 * canonical, Ed25519-signed payload; {@code signature} is the base64-encoded
 * raw Ed25519 signature over {@link #canonicalPayload()}.
 *
 * <p>{@code createdAt} is kept as the raw ISO-8601 string that was signed
 * (rather than parsed to {@code Instant}) so the canonicalization is a pure
 * byte-for-byte match of whatever the offline signing script hashed, with no
 * risk of a round-trip formatting mismatch.
 */
public record BindingDocument(
        String deploymentId,
        Long authorizedChatId,
        String environment,
        String createdAt,
        String version,
        String applicationIdentifier,
        String nonce,
        String signature
) {

    /**
     * Deterministic, order-fixed representation of every signed field.
     * Must byte-for-byte match what the offline signing script hashed.
     */
    public byte[] canonicalPayload() {
        String canonical = String.join("\n",
                nullToEmpty(deploymentId),
                authorizedChatId == null ? "" : authorizedChatId.toString(),
                nullToEmpty(environment),
                nullToEmpty(createdAt),
                nullToEmpty(version),
                nullToEmpty(applicationIdentifier),
                nullToEmpty(nonce)
        );
        return canonical.getBytes(StandardCharsets.UTF_8);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
