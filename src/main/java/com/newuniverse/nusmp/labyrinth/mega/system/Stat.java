package com.newuniverse.nusmp.labyrinth.mega.system;

import java.util.Locale;

/**
 * Why: the System of the Holy road (the owner's "Solo Leveling" brief) rewards a cleared floor with a permanent stat point, and the five stats have to be the
 * same thing in the quest data (which stat a quest pays), the saved player data (how many points of each), the attribute table (what a point does) and the
 * overlay (the {@code STR 3  AGI 1 ...} row). One tiny pure enum is that shared word. The ORDER is part of the saved format (the points are stored as a list in
 * this order), so entries are only ever appended at the end, never reordered. Pure: no Minecraft import, so JUnit pins every rule.
 */
public enum Stat {
    /** Strength: hits harder. Paid by combat quests. */
    STR,
    /** Agility: moves faster. Paid by combat and speed quests. */
    AGI,
    /** Vitality: more health. Paid by the quest that asks you not to be hurt. */
    VIT,
    /** Intelligence: a deeper pool of magicule. Paid by the quest that clears rooms. */
    INT,
    /** Sense: luck, and a longer reach of the presence sense. Paid by the ambush quest. */
    SENSE;

    /** How many stats there are (the length of the saved list). */
    public static final int COUNT = values().length;

    /** The lower-case id used in the fixed attribute-modifier ids ({@code nusmp:system/str}), in commands and in JSON. */
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The stat with this id or name (case-insensitive, surrounding spaces ignored), or null for null or an unknown word (an old save or a typo in a datapack). */
    public static Stat fromId(String s) {
        if (s == null) return null;
        String t = s.trim();
        for (Stat st : values()) if (st.name().equalsIgnoreCase(t)) return st;
        return null;
    }
}
