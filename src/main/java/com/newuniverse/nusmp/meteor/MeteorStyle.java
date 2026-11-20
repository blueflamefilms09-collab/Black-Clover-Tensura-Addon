package com.newuniverse.nusmp.meteor;

import java.util.Locale;

/**
 * The look of a meteor (docs/meteor_fall.md). Pure (no game classes). Append new styles at the END only: the ordinal is what the entity syncs to clients.
 * {@link #key} names the texture pair {@code textures/entity/meteor_rock_<key>.png} / {@code meteor_tail_<key>.png}; {@link #core} is the glow tint (ARGB) the renderer
 * and the trail particles use.
 */
public enum MeteorStyle {
    FIRE("fire", 0xFFFF7A1A), ICE("ice", 0xFF9AE6FF), DARK("dark", 0xFF9A3CFF), KI("ki", 0xFFFFE066), HOLY("holy", 0xFFFFF4C2), STAR("star", 0xFFB8C8FF);

    public final String key;
    public final int core;

    MeteorStyle(String key, int core) {
        this.key = key;
        this.core = core;
    }

    /** The style for a synced ordinal; an unknown number reads as FIRE. */
    public static MeteorStyle of(int ordinal) {
        MeteorStyle[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : FIRE;
    }

    /** The style for a name ({@code "ice"}), or {@code fallback}. */
    public static MeteorStyle byName(String name, MeteorStyle fallback) {
        if (name == null) return fallback;
        String n = name.toLowerCase(Locale.ROOT);
        for (MeteorStyle s : values()) if (s.key.equals(n)) return s;
        return fallback;
    }
}
