package com.newuniverse.nusmp.avatar.dnd;

/**
 * Why: the creation roll uses the six Dungeons and Dragons ability scores and the mod had no shared enum for them (its D&D pieces roll d20s only), so the
 * Avatar system names them once, here. Pure (no Minecraft), constants only ever appended because the order is the order of the rolled array.
 */
public enum Stat {
    STR("Strength"), DEX("Dexterity"), CON("Constitution"), INT("Intelligence"), WIS("Wisdom"), CHA("Charisma");

    public final String displayName;

    Stat(String displayName) { this.displayName = displayName; }

    /** The D&D modifier of a score: floor((score - 10) / 2). */
    public static int modifier(int score) { return Math.floorDiv(score - 10, 2); }

    /** Case-insensitive lookup by short name; null when unknown. */
    public static Stat byName(String s) {
        if (s == null) return null;
        for (Stat t : values()) if (t.name().equalsIgnoreCase(s.trim())) return t;
        return null;
    }
}
