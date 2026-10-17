package com.newuniverse.nusmp.labyrinth.mega.safe;

import java.util.Locale;

/**
 * Why: Dungeons and Dragons has exactly two rests and the whole safe-zone design of the Mega-Dungeon hangs on keeping them apart: a SHORT
 * rest (a seat or a hearth, ten seconds, a share of health and mana, three of them per long rest) is a breather you take between fights, a
 * LONG rest (a bed, thirty seconds, everything back, a real pause with a cool-down) is the reset that makes the party commit to defending a
 * room. One tiny pure enum is shared by the rules ({@link RestRules}), the saved state ({@link RestState}), the events and the admin command
 * so none of them ever compares strings or ordinals; {@link #id()} is the lower-case word used in lang keys and as the quest-hook argument.
 */
public enum RestKind {
    /** A seat or a hearth: quick, partial, limited by {@link RestRules#DEFAULT_SHORT_USES} uses until the next long rest. */
    SHORT,
    /** A bed: full recovery, refills the short rests, subject to a cool-down so it cannot be used to skip every fight. */
    LONG;

    /** {@code short} / {@code long}. */
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The kind with this id (case-insensitive), or null. */
    public static RestKind byId(String id) {
        if (id == null) return null;
        for (RestKind k : values()) if (k.id().equalsIgnoreCase(id.trim())) return k;
        return null;
    }
}
