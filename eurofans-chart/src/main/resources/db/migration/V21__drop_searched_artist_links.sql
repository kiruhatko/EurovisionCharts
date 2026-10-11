-- Spotify and Last.fm links created by the old name searches (Spotify) and artist.getinfo lookups
-- (Last.fm) are replaced by the direct links in eurovision-artists.json: run /reloadartists after
-- this migration to recreate them from that file.
DELETE FROM eurovision_artist_external_ids WHERE provider IN ('SPOTIFY', 'LASTFM');
