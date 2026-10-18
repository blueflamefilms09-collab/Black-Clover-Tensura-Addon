package com.newuniverse.nusmp.relics;

/**
 * Attunement as a pure rule: a relic that needs it works only for the player it is attuned to, and a player keeps at most {@link #CAP} such
 * relics working at once (the first ones met, in the order the engine scans: hands, armour, Curios). Relics that need no attunement always work.
 */
public final class AttuneRules {
    private AttuneRules() {}

    public static final int CAP = 3;

    /** 1.84.0: the lowest grade whose relics need attunement; Normal, Special, Rare and Unique relics never do (owner request). */
    public static final Grade REQUIRED_FROM = Grade.LEGEND;

    /** Does a relic of this grade, defined with or without {@code .attune()}, need attunement? */
    public static boolean requires(boolean definedAsAttuned, Grade grade) { return definedAsAttuned && grade != null && grade.atLeast(REQUIRED_FROM); }

    /** Ticks of sneaking with the use button held that bind a relic (five seconds). */
    public static final int HOLD_TICKS = 100;

    /** Which of the carried relics work: {@code needs[i]} is whether relic i needs attunement, {@code mine[i]} whether it is attuned to the wearer. */
    public static boolean[] working(boolean[] needs, boolean[] mine, int cap) {
        boolean[] out = new boolean[needs.length];
        int used = 0;
        for (int i = 0; i < needs.length; i++) {
            if (!needs[i]) { out[i] = true; continue; }
            if (mine[i] && used < cap) { out[i] = true; used++; }
        }
        return out;
    }

    /** What using an Unbinding Seal does, from the facts of the relic in the other hand. */
    public enum Unbind { NO_RELIC, NOT_ATTUNED, RELEASED }

    public static Unbind unbind(boolean otherHandIsRelic, boolean relicIsAttuned) {
        if (!otherHandIsRelic) return Unbind.NO_RELIC;
        return relicIsAttuned ? Unbind.RELEASED : Unbind.NOT_ATTUNED;
    }

    /** May one more relic be attuned, given how many attuned relics the player carries? */
    public static boolean canAttune(int attunedCarried) { return attunedCarried < CAP; }

    /** 0 to 100, how far an attunement hold has come. */
    public static int percent(int heldTicks) { return Math.max(0, Math.min(100, heldTicks * 100 / HOLD_TICKS)); }
}
