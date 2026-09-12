package com.eurovision.analytics.util;

import com.eurovision.analytics.eurovision.EurovisionCountry;

/**
 * ISO 3166-1 alpha-2 -> Unicode regional-indicator flag emoji (e.g. "UA" -> 🇺🇦),
 * for Telegram messages that name one specific artist's country. Falls back to
 * the generic Eurovision flag when the artist has no country assigned, or the
 * code isn't exactly two Latin letters (e.g. informal codes like "XK").
 */
public final class FlagEmoji {

    private static final String FALLBACK = "🇪🇺"; // 🇪🇺

    private FlagEmoji() {
    }

    public static String forCountry(EurovisionCountry country) {
        if (country == null || country.getIsoCode() == null) {
            return FALLBACK;
        }
        return forIsoCode(country.getIsoCode());
    }

    public static String forIsoCode(String isoCode) {
        if (isoCode == null || isoCode.length() != 2) {
            return FALLBACK;
        }
        String upper = isoCode.toUpperCase();
        char c1 = upper.charAt(0);
        char c2 = upper.charAt(1);
        if (c1 < 'A' || c1 > 'Z' || c2 < 'A' || c2 > 'Z') {
            return FALLBACK;
        }
        int base = 0x1F1E6;
        return new String(Character.toChars(base + (c1 - 'A')))
                + new String(Character.toChars(base + (c2 - 'A')));
    }
}
