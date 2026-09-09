package com.eurovision.analytics.oauth.soundcloud;

import com.eurovision.analytics.config.ProviderProperties;
import com.eurovision.analytics.connectedaccount.ConnectedAccount;
import com.eurovision.analytics.connectedaccount.Provider;
import com.eurovision.analytics.connectedaccount.TokenRevocationHandler;
import com.eurovision.analytics.security.TokenEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SoundCloudTokenRevocationHandler implements TokenRevocationHandler {

    private static final Logger log = LoggerFactory.getLogger(SoundCloudTokenRevocationHandler.class);

    private final WebClient soundcloudAuthWebClient;
    private final ProviderProperties.SoundCloud config;
    private final TokenEncryptionService tokenEncryptionService;

    public SoundCloudTokenRevocationHandler(WebClient soundcloudAuthWebClient,
                                             ProviderProperties properties,
                                             TokenEncryptionService tokenEncryptionService) {
        this.soundcloudAuthWebClient = soundcloudAuthWebClient;
        this.config = properties.soundcloud();
        this.tokenEncryptionService = tokenEncryptionService;
    }

    @Override
    public Provider provider() {
        return Provider.SOUNDCLOUD;
    }

    @Override
    public void revoke(ConnectedAccount account) {
        String accessToken = tokenEncryptionService.decrypt(account.getAccessTokenEncrypted());
        if (accessToken == null) {
            return;
        }
        try {
            soundcloudAuthWebClient.post()
                    .uri("/sign-out")
                    .body(BodyInserters.fromFormData("client_id", config.clientId())
                            .with("client_secret", config.clientSecret())
                            .with("access_token", accessToken))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("SoundCloud token revocation call failed for connectedAccountId={}: {}",
                    account.getId(), e.getMessage());
        }
    }
}
