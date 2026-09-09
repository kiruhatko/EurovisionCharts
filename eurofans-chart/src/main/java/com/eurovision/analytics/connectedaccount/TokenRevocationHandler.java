package com.eurovision.analytics.connectedaccount;

/**
 * Implemented once per provider that supports server-side token revocation
 * (Spotify, SoundCloud). Apple Music has no revocation endpoint for MusicKit
 * user tokens and Last.fm never issues a token, so neither implements this.
 */
public interface TokenRevocationHandler {

    Provider provider();

    /** Best-effort: a failed revoke call must never block local disconnect. */
    void revoke(ConnectedAccount account);
}
