package com.newuniverse.nusmp.labyrinth.mega.gear;

import java.util.Locale;

/**
 * Why: the owner's brief names exactly three grades of equipment that monsters may drop without limit, taken from Tensura's grading scale: Normal
 * (everyday items without magic), Special (refined craftsmanship) and Rare (high-grade items that turn magic aside). The four higher grades of that
 * scale (Unique, Legend, God, Genesis) are RELICS and are handed out only by the Crucible, so this enum deliberately stops at three: a drop table,
 * a roll or a saved component that names a fourth grade would be a way to leak a relic-tier item into the infinite stream. It is a pure enum (no
 * Minecraft import) so the roller, the tables and the resistance rules are unit tested, and it is saved by {@link #id()} (never by ordinal). Only
 * the game part maps a grade to the relic {@code Grade} (same three names) to borrow its colour and label, so the tiers look the same as in the
 * rest of the mod. Entries are only ever appended.
 */
public enum GearGrade {
    NORMAL("normal"),
    SPECIAL("special"),
    RARE("rare");

    private final String id;

    GearGrade(String id) { this.id = id; }

    /** The lower-case id used in saved data, JSON, commands, quests and lang keys ({@code normal}, {@code special}, {@code rare}). */
    public String id() { return id; }

    /** The grade with this id (case-insensitive, trimmed), or null for null or an unknown id (callers decide the fallback; a saved stack must never fail to load). */
    public static GearGrade fromId(String id) {
        if (id == null) return null;
        String s = id.trim().toLowerCase(Locale.ROOT);
        for (GearGrade g : values()) if (g.id.equals(s)) return g;
        return null;
    }

    public boolean atLeast(GearGrade other) { return ordinal() >= other.ordinal(); }
}
