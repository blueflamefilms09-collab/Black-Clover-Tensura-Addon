package com.newuniverse.nusmp.client.ui.theme;

/** The two looks of the mod's menus: the translucent anime look (default) and the classic book look kept from before. */
public enum UiThemeMode {
    ANIME, CLASSIC;

    /** Reads the config text ("anime" / "classic", any case); anything else is the default, ANIME. */
    public static UiThemeMode parse(String s) {
        if (s != null && s.trim().equalsIgnoreCase("classic")) return CLASSIC;
        return ANIME;
    }
}
