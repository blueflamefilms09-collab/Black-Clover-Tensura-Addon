package com.newuniverse.nusmp.labyrinth.mega.enc;

/**
 * Why: a goblin from sector twelve should not be the goblin from sector one, and the way to say so without a second set of monsters is a
 * multiplier on the body: a few percent more health and damage for every threat level above the first. The numbers are applied as attribute
 * modifiers (multiplicative on the final value, so they compose with the Labyrinth's health buffer instead of fighting it), and this class holds
 * only the arithmetic: how much per level, capped so that a very deep sector scales the monsters into a hard wall rather than into the
 * stratosphere (+300 percent at most). Pure.
 */
public final class ThreatScale {
    private ThreatScale() {}

    /** The most any scaling adds: +300 percent (four times the base). */
    public static final double CAP = 3.0;

    /** {@code perLevel} for every threat level above one, as the amount of a multiplicative modifier (0.06 per level is +6 percent). */
    public static double amount(int threat, double perLevel) {
        if (!(perLevel > 0.0)) return 0.0;
        return Math.min(CAP, perLevel * Math.max(0, threat - 1));
    }
}
