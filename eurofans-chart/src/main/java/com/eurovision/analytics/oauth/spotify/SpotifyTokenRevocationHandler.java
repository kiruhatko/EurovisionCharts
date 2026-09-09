package com.eurovision.analytics.oauth.spotify;

import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.connectedaccount.TokenRevocationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Spotify's Web API / Accounts service does not publish a token-revocation
 * endpoint for third-party applications (unlike SoundCloud's RFC 7009-style
 * revoke). Disconnecting here can only ever delete the locally stored,
 * encrypted tokens; the user must revoke third-party access from their own
 * Spotify account settings if they want it invalidated server-side too. This
 * limitation is documented rather than hidden, per spec rule #9.
 */
@Component
public class SpotifyTokenRevocationHandler implements TokenRevocationHandler {

    private static final Logger log = LoggerFactory.getLogger(SpotifyTokenRevocationHandler.class);

    @Override
    public Provider provider() {
        return Provider.SPOTIFY;
    }

    @Override
    public void revoke(ConnectedAccount account) {
        log.info("Spotify has no public token-revocation endpoint; clearing local tokens only for connectedAccountId={}",
                account.getId());
    }
}
