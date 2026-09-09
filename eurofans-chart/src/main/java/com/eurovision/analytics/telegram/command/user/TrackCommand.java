package com.eurovision.analytics.telegram.command.user;

import com.eurovision.analytics.artwork.ArtworkCache;
import com.eurovision.analytics.artwork.ArtworkResolutionService;
import com.eurovision.analytics.nowplaying.NowPlayingResolver;
import com.eurovision.analytics.nowplaying.NowPlayingResult;
import com.eurovision.analytics.telegram.Command;
import com.eurovision.analytics.telegram.CommandContext;
import com.eurovision.analytics.telegram.MessageSender;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Renamed 1:1 from the legacy {@code /now}; identical behavior, per spec section 5.2/8. */
@Component
public class TrackCommand implements Command {

    private static final String NOTHING_EUROVISION = "Наразі не грає нічого з Eurovision каталогу.";
    private static final String NOTHING_PLAYING = "Наразі нічого не грає.";

    private final NowPlayingResolver nowPlayingResolver;
    private final ArtworkResolutionService artworkResolutionService;
    private final MessageSender messageSender;

    public TrackCommand(NowPlayingResolver nowPlayingResolver, ArtworkResolutionService artworkResolutionService,
                         MessageSender messageSender) {
        this.nowPlayingResolver = nowPlayingResolver;
        this.artworkResolutionService = artworkResolutionService;
        this.messageSender = messageSender;
    }

    @Override
    public String name() {
        return "track";
    }

    @Override
    public void handle(CommandContext ctx) {
        NowPlayingResult result = nowPlayingResolver.resolveNowPlaying(ctx.user().getId());

        switch (result) {
            case NowPlayingResult.NothingPlaying ignored -> messageSender.send(ctx.chatId(), NOTHING_PLAYING);
            case NowPlayingResult.NonEurovision ignored -> messageSender.send(ctx.chatId(), NOTHING_EUROVISION);
            case NowPlayingResult.EurovisionTrack track -> {
                String caption = "🎧 <b>ЗАРАЗ ГРАЄ</b> (джерело: " + displayName(track.provider()) + ")\n"
                        + escape(track.signal().rawArtistName()) + " / " + escape(track.signal().rawTrackName())
                        + (track.signal().rawAlbumName() != null ? " / " + escape(track.signal().rawAlbumName()) : "")
                        + "\n🇪🇺 " + escape(track.artist().getCanonicalName())
                        + "\n#Eurovision #" + track.artist().getCanonicalName().replaceAll("\\s+", "");

                Optional<ArtworkCache> artwork = artworkResolutionService.resolve(
                        track.provider(), track.signal().rawArtistName(), track.signal().rawTrackName(),
                        track.signal().nativeArtworkUrl());

                if (artwork.isPresent()) {
                    messageSender.sendPhoto(ctx.chatId(), artwork.get().getOriginalUrl(), caption);
                } else {
                    messageSender.send(ctx.chatId(), caption);
                }
            }
        }
    }

    private String displayName(com.eurovision.analytics.connectedaccount.Provider provider) {
        return switch (provider) {
            case SPOTIFY -> "Spotify";
            case APPLE_MUSIC -> "Apple Music";
            case SOUNDCLOUD -> "SoundCloud";
            case LASTFM -> "Last.fm";
        };
    }

    private String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
