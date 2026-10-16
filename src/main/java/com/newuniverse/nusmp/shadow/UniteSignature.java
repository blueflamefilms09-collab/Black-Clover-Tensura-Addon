package com.newuniverse.nusmp.shadow;

import java.util.Map;

/**
 * Why: the three quick Unite pages (Canis, Gallus, Canis x Felis) are existing spells that only hand out potion effects, so they leave no
 * state of their own to hook an armour overlay on. This class recognises them by the exact set of effects they just applied (amplifiers and a
 * fresh 30 second duration), without touching the spells. The match is pure so the tests can prove each signature picks exactly one mode and
 * that ordinary potions do not match.
 *
 * <p>Input: effect id ({@code speed}, {@code strength}, {@code jump}, {@code slow_falling}, {@code resistance}) to {amplifier, ticks left}.
 * Signatures (from {@code WikiSpells.uniteCanis / uniteGallus / uniteCanisFelis}): Canis = Speed III + Strength I + Jump II;
 * Gallus = Slow Falling I + Jump V + Speed II; Canis x Felis = Speed III + Strength II + Resistance I + Jump II.
 */
public final class UniteSignature {
    private UniteSignature() {}

    /** Which old page the effects came from. */
    public enum Source { NONE, CANIS, GALLUS, CANIS_FELIS }

    /** A freshly applied effect still has nearly its whole 600 ticks; anything lower is an older effect that just looks similar. */
    public static final int FRESH_TICKS = 592;

    public static Source detect(Map<String, int[]> effects) {
        if (has(effects, "speed", 2) && has(effects, "strength", 1) && has(effects, "resistance", 0) && has(effects, "jump", 1)) return Source.CANIS_FELIS;
        if (has(effects, "slow_falling", 0) && has(effects, "jump", 4) && has(effects, "speed", 1)) return Source.GALLUS;
        if (has(effects, "speed", 2) && has(effects, "strength", 0) && has(effects, "jump", 1) && !effects.containsKey("resistance")) return Source.CANIS;
        return Source.NONE;
    }

    private static boolean has(Map<String, int[]> e, String id, int amplifier) {
        int[] v = e.get(id);
        return v != null && v[0] == amplifier && v[1] >= FRESH_TICKS;
    }

    /** The mode a source starts (Canis x Felis shows the Canis armour: two devils, one body). */
    public static UniteKind kind(Source s) {
        return switch (s) {
            case CANIS, CANIS_FELIS -> UniteKind.CANIS;
            case GALLUS -> UniteKind.GALLUS;
            default -> null;
        };
    }
}
