package com.newuniverse.nusmp.labyrinth.mega.flow;

import java.util.Locale;

/**
 * Why: the Rukh flow of the Holy floors (1.55.0) compares TWO things, the "flow" of a place (what the air is thick with) and the alignment of
 * a cast (what kind of magic the player just used), and every part of it (the table that classifies a cast, the rules that decide a boost or a
 * rejection, the memory of the last cast, the commands) must speak about them with the same three words. This is that vocabulary as a pure
 * enum with no Minecraft import, so the rules and their JUnit tests never need the game: LIGHT (holy, Light Rukh), DARK (demonic, Dark Rukh)
 * and NEUTRAL (everything else: the great majority of the mod's magic, which the flow neither helps nor hurts). It is deliberately a separate
 * type from {@code RukhFx.Alignment} (which only has the two colours a particle can have and lives in the game-side particle package):
 * this one also has "neither", and it is what the data file and the config speak. Saved by {@link #id()}, never by ordinal.
 */
public enum Alignment {
    /** Holy magic: Light Magic and everything whose name says light, holy, sacred or solar. Boosted by a Light flow, rejected by a Dark one. */
    LIGHT,
    /** Demonic magic: Dark, Shadow, Curse, the Demon families and everything whose name says dark, devil, hell, blood or death. The mirror of LIGHT. */
    DARK,
    /** Neither: the flow does nothing to it (no boost, no rejection). The answer for every cast no rule names, and for a place with no flow. */
    NEUTRAL;

    /** The lower-case id used in the data file, the config, the commands and the lang keys ({@code light}, {@code dark}, {@code neutral}). */
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** LIGHT for DARK and DARK for LIGHT; NEUTRAL has no opposite and answers itself. */
    public Alignment opposite() { return this == LIGHT ? DARK : this == DARK ? LIGHT : NEUTRAL; }

    /**
     * The alignment with this id, case-insensitive, ignoring spaces around it: {@code light}, {@code dark}, {@code neutral}, and {@code none} (which reads as
     * NEUTRAL, because the config says "no flow" that way). Null for null or any other text, so the caller chooses the fallback.
     */
    public static Alignment fromId(String id) {
        if (id == null) return null;
        String s = id.trim().toLowerCase(Locale.ROOT);
        if (s.equals("none")) return NEUTRAL;
        for (Alignment a : values()) if (a.id().equals(s)) return a;
        return null;
    }
}
