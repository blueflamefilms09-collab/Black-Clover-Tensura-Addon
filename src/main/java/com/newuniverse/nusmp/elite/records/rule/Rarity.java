package com.newuniverse.nusmp.elite.records.rule;

import java.util.Locale;

/**
 * Why: records and titles share one six-step rarity ladder (the same six words the forge uses for quality, so a "Mythic" record and a "Mythic" item read alike). The ladder fixes three things in one
 * place: the colour a record or title is drawn in, the points an unlocked record is worth (doubling per step, used by the "points" requirement and the score on the screen) and the sort order.
 * Pure (no Minecraft class) so the rules and their tests run without the game. The enum order is the save/wire order: append only.
 */
public enum Rarity {
    COMMON(0xB8B8B8, 1), UNCOMMON(0x55E070, 2), RARE(0x58A8FF, 4), EPIC(0xB36BFF, 8), LEGENDARY(0xFFB02E, 16), MYTHIC(0xFF4D78, 32);

    /** RGB colour of the rarity (no alpha). */
    public final int color;
    /** Points one unlocked record of this rarity is worth. */
    public final int points;

    Rarity(int color, int points) {
        this.color = color;
        this.points = points;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The rarity named {@code s} (any case), or {@code fallback} for null, empty or unknown text. */
    public static Rarity parse(String s, Rarity fallback) {
        if (s == null) return fallback;
        String t = s.trim().toUpperCase(Locale.ROOT);
        for (Rarity r : values()) if (r.name().equals(t)) return r;
        return fallback;
    }

    public static Rarity byOrdinal(int i) { return values()[Math.max(0, Math.min(values().length - 1, i))]; }
}
