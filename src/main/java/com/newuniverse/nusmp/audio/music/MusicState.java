package com.newuniverse.nusmp.audio.music;

/**
 * Why: what the server tells one client about the music, as plain values: the layer, the id of the theme of the floor (the client maps it to a family itself, so a changed table
 * or a resource-pack mapping needs no server change) and the sign of the road (the boss loop). A value type so the packet, the client and the tests share it.
 *
 * @param layer    the layer, never null
 * @param themeId  the Region Theme id of the floor, "" when the player is in no sector
 * @param polarity positive for the Holy road, negative for the Abyssal one
 */
public record MusicState(MusicLayer layer, String themeId, int polarity) {
    /** Longest theme id the wire carries. */
    public static final int MAX_THEME = 64;

    public static final MusicState OFF = new MusicState(MusicLayer.OFF, "", 1);

    public MusicState {
        layer = layer == null ? MusicLayer.OFF : layer;
        themeId = themeId == null ? "" : (themeId.length() > MAX_THEME ? themeId.substring(0, MAX_THEME) : themeId);
        polarity = polarity < 0 ? -1 : 1;
    }

    /** The track this state asks for, or null when it asks for nothing. */
    public String track() { return MusicCatalog.trackFor(layer, themeId, polarity); }
}
