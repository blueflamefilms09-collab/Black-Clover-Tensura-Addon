package com.newuniverse.nusmp.corpse;

/**
 * Why (owner report 3.7.0, docs/magic/rades_cast_fix.md): a numbered undead whose geo model or texture failed to load was drawn as NOTHING (the geo drawer returns at once for an empty model), so the
 * owner saw only the name tag and the magic circle. This pure class (no game types) is the one rule of what a renderer draws: the new geo look when its model and texture are there, else the zombie
 * stand-in (tinted, with the name) so the body is never invisible, or nothing when the client switch {@code corpse.fallbackLook} is off (the old behaviour). It also holds which renderer registration
 * wins, so the order of the two registrations (the dungeon kit and the new looks) is one tested rule and not a comment.
 */
public final class CorpseLookRules {
    private CorpseLookRules() {}

    public enum Draw { GEO, FALLBACK, NOTHING }

    /** What to draw: the geo model when it has parts and its texture exists; else the stand-in when allowed; else nothing. */
    public static Draw draw(boolean modelHasParts, boolean textureExists, boolean fallbackEnabled) {
        if (modelHasParts && textureExists) return Draw.GEO;
        return fallbackEnabled ? Draw.FALLBACK : Draw.NOTHING;
    }

    /** The word the client log gives for a draw decision. */
    public static String describe(Draw d) {
        return switch (d) {
            case GEO -> "the new geo look";
            case FALLBACK -> "the zombie stand-in (the geo model or its texture did not load)";
            case NOTHING -> "nothing (the geo model or its texture did not load and corpse.fallbackLook is off)";
        };
    }

    /**
     * Which of two registrations of the same entity type stays: the NeoForge bus runs listeners of one priority in the order they were added and the renderer registered LAST stays. 'orderA' and
     * 'orderB' are the positions of the two registrations (the one with the larger position runs later and wins; equal positions: the second argument).
     */
    public static boolean laterWins(int orderA, int orderB) { return orderA > orderB; }
}
