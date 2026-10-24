package com.newuniverse.nusmp.client.companion.gs;

/**
 * Which talk screen a player gets: the Great Spirit one (portrait, typed dialogue, rapport leaves, gift slots, ...) or the CLASSIC plain one that shipped first. The client option
 * {@code companions.screenStyle} is AUTO, GREAT_SPIRIT or CLASSIC: AUTO follows the UI theme (great_spirit gives the Great Spirit screen, anything else the classic one), the other two force
 * a screen. Pure (no Minecraft types), unit tested.
 */
public enum CompanionStyle {
    GREAT_SPIRIT, CLASSIC;

    /** The config value: AUTO follows the theme. */
    public enum Choice {
        AUTO, GREAT_SPIRIT, CLASSIC;

        /** Reads the config text (any case, '-' or ' ' for '_'); anything unknown is AUTO. */
        public static Choice parse(String s) {
            if (s != null) {
                String t = s.trim().replace('-', '_').replace(' ', '_');
                for (Choice c : values()) if (c.name().equalsIgnoreCase(t)) return c;
            }
            return AUTO;
        }
    }

    /** The screen to open: a forced choice wins; AUTO is the Great Spirit screen only while the great_spirit theme is on. */
    public static CompanionStyle choose(Choice choice, boolean greatSpiritTheme) {
        if (choice == Choice.GREAT_SPIRIT) return GREAT_SPIRIT;
        if (choice == Choice.CLASSIC) return CLASSIC;
        return greatSpiritTheme ? GREAT_SPIRIT : CLASSIC;
    }
}
