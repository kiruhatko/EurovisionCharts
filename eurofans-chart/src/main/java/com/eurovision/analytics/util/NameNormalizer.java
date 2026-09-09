package com.eurovision.analytics.util;

import java.text.Normalizer;

/**
 * Deterministic name normalization used ONLY as a fallback-alias lookup key.
 * Per the spec's core principle, a normalized-name match is never sufficient
 * on its own to establish artist identity — it only narrows candidates for
 * {@code EurovisionIdentityResolver}, which still requires an unambiguous
 * single match before trusting it.
 */
public final class NameNormalizer {

    private NameNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(raw, Normalizer.Form.NFKD);
        StringBuilder sb = new StringBuilder(decomposed.length());
        for (char c : decomposed.toCharArray()) {
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                continue;
            }
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            } else if (Character.isWhitespace(c) && sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }
}
