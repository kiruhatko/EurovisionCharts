package com.eurovision.analytics.chartstudio;

import com.eurovision.analytics.eurovision.EurovisionCountry;
import org.springframework.stereotype.Component;

/**
 * Maps a {@link EurovisionCountry#getIsoCode()} to the matching heart-flag
 * asset under {@code src/main/resources/static/flags/}. Country identity
 * always comes from the artist's own record, never from whether the track
 * itself is a Eurovision entry (spec: chart-studio must show a country's
 * flag next to any artist assigned that country).
 */
@Component
public class FlagCatalog {

    public static final String DEFAULT_FLAG_URL = "/flags/_default.png";

    public String urlFor(EurovisionCountry country) {
        if (country == null || country.getIsoCode() == null || country.getIsoCode().isBlank()) {
            return DEFAULT_FLAG_URL;
        }
        return "/flags/" + country.getIsoCode().toUpperCase() + ".png";
    }
}
