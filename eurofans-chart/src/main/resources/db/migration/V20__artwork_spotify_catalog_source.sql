ALTER TABLE artwork_cache DROP CONSTRAINT chk_artwork_cache_source;
ALTER TABLE artwork_cache ADD CONSTRAINT chk_artwork_cache_source
    CHECK (source IN ('COVER_ART_ARCHIVE', 'PROVIDER_NATIVE', 'SPOTIFY_CATALOG'));

-- Everything cached so far was picked before the sharper sources worked (mostly Last.fm's 300px
-- images or its grey placeholder). It is only a cache, so start over.
DELETE FROM artwork_cache;
