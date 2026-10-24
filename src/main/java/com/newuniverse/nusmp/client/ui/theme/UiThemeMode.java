package com.newuniverse.nusmp.client.ui.theme;

/**
 * The looks of the mod's menus: the translucent anime look (default), the classic book look kept from before, and (appended, 2.x) the Great
 * Spirit look: the anime look's layout with the Parchment-and-Ink / Slime-Crest skins, filigree corners, clover frames, the compile animation,
 * the slime ripple and the alert crest. {@link UiTheme#anime()} stays true for GREAT_SPIRIT, so every anime branch still runs and the Great Spirit
 * parts are layered on top where {@link UiTheme#greatSpirit()} asks.
 */
public enum UiThemeMode {
    ANIME, CLASSIC, GREAT_SPIRIT;

    /** Reads the config text ("anime" / "classic" / "great_spirit", any case); anything else is the default, ANIME. */
    public static UiThemeMode parse(String s) {
        if (s != null) {
            String t = s.trim();
            if (t.equalsIgnoreCase("classic")) return CLASSIC;
            if (t.equalsIgnoreCase("great_spirit")) return GREAT_SPIRIT;
        }
        return ANIME;
    }
}
