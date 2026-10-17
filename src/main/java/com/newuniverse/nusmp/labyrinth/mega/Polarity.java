package com.newuniverse.nusmp.labyrinth.mega;

import java.util.Locale;

/**
 * Why: the Mega-Dungeon of 1.55.0 is BIDIRECTIONAL. From Floor 0 (the Sanctuary Hub) one road climbs through the Holy floors (+1, +2, ...) and one
 * descends through the Abyssal floors (-1, -2, ...); a player commits to one of them, so the code that has to ask "which way is this place / this
 * player going" needs a tiny type that is the same everywhere. It is a pure enum (no Minecraft import) so the floor arithmetic, the alignment
 * lock and the pool/theme biases can be unit tested, saved by {@link #id()} (the ordinal is never saved), and read by builders that must not
 * import each other. The sign is the floor number's sign: Holy floors are positive, Abyssal floors negative, Floor 0 belongs to neither.
 */
public enum Polarity {
    /** Ascension: the Holy floors, +1 upward. Inspired by Solo Leveling, Magi and Slay the Spire (the owner's brief). */
    HOLY(1),
    /** Descent: the Abyssal floors, -1 downward. Inspired by DanMachi and The Exiled Heavy Knight (the owner's brief). */
    ABYSSAL(-1);

    private final int sign;

    Polarity(int sign) { this.sign = sign; }

    /** +1 for HOLY, -1 for ABYSSAL: multiply a floor depth (1, 2, 3 ...) by it to get the signed floor number. */
    public int sign() { return sign; }

    /** The lower-case id used in saved data, JSON, commands and lang keys ({@code holy}, {@code abyssal}). */
    public String id() { return name().toLowerCase(Locale.ROOT); }

    public Polarity opposite() { return this == HOLY ? ABYSSAL : HOLY; }

    /** The polarity with this id (case-insensitive), or null for null or an unknown id (callers decide the fallback; an old world has none saved). */
    public static Polarity fromId(String id) {
        if (id == null) return null;
        for (Polarity p : values()) if (p.id().equalsIgnoreCase(id)) return p;
        return null;
    }

    /** The polarity of a signed floor number: HOLY above 0, ABYSSAL below 0, null for Floor 0 (the Hub belongs to neither road). */
    public static Polarity ofFloor(int floor) { return floor > 0 ? HOLY : floor < 0 ? ABYSSAL : null; }
}
