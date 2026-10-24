package com.newuniverse.nusmp.mana.method;

/**
 * Hold-to-charge of the Mana Method (pure, tested, no game classes). In the anime the size of a rune array is the size of the spell: a mage who writes only the inner ring fires a
 * small spell, one who writes every ring fires a massive one. Here the player HOLDS the grimoire's cast key (the grimoire's own hold, no new key); the server measures the ticks held
 * and this class turns them into a {@link Charge}:
 * <ul>
 *   <li><b>BASIC</b> (held less than {@code minHold}, a tap or an early release): ONE ring tier (the hand ring), no second rune even when sneaking, power x{@value #BASIC_POWER},
 *       a smaller bolt that Grows less;</li>
 *   <li><b>CHARGED</b> (from {@code minHold} up to {@code maxHold}): two ring tiers, the power rises smoothly from x{@value #CHARGED_POWER_MIN} to x{@value #CHARGED_POWER_MAX} with the ticks held;</li>
 *   <li><b>FULL</b> (held {@code maxHold} ticks or more): all three ring tiers and the loose glyphs, both runes when sneaking, power x{@value #FULL_POWER}, the biggest bolt and the
 *       strongest Growth.</li>
 * </ul>
 * So the power is continuous inside CHARGED and steps at the two edges: three readable stages for the eye (what the rings show), one smooth number for the damage. The 3.4.0 spell
 * (a fixed 18 tick charge, everything drawn, power x1) is {@link Charge#LEGACY}: it is what a cast without a hold gets, and what every cast gets with the setting off.
 */
public final class RuneCharge {
    private RuneCharge() {}

    public enum Stage { BASIC, CHARGED, FULL }

    // ---------------------------------------------------------------- the numbers
    public static final double BASIC_POWER = 0.5, CHARGED_POWER_MIN = 0.75, CHARGED_POWER_MAX = 1.6, FULL_POWER = 1.6;
    /** The size of the bolt (hit volume and splash) per stage: basic, the charged range and full. */
    public static final double BASIC_SIZE = 0.75, CHARGED_SIZE_MIN = 0.9, CHARGED_SIZE_MAX = 1.4, FULL_SIZE = 1.5;
    /** How strongly the Grow command scales the bolt as it flies: basic, the charged range and full (x1 = the 3.4.0 numbers). */
    public static final double BASIC_GROWTH = 0.6, CHARGED_GROWTH_MIN = 0.9, CHARGED_GROWTH_MAX = 1.4, FULL_GROWTH = 1.5;
    /** Ticks the rune takes to finish after the release (the rings still unwritten of the reached tier are completed), per stage; the bolt leaves after them. */
    public static final int FINISH_BASIC = 6, FINISH_CHARGED = 8, FINISH_FULL = 10;

    // ---------------------------------------------------------------- the settings' ranges
    public static final int DEFAULT_MIN_HOLD = 8, DEFAULT_MAX_HOLD = 40;
    public static final int MIN_HOLD_LOW = 2, MIN_HOLD_HIGH = 30, MAX_HOLD_LOW = 12, MAX_HOLD_HIGH = 100, MIN_SPAN = 8;
    /** The hold is never counted past this (the grimoire's own limit is 200 ticks). */
    public static final int HOLD_CAP = 200;

    /** The minimum hold from the config, kept in range. */
    public static int minHold(int configured) { return Math.max(MIN_HOLD_LOW, Math.min(MIN_HOLD_HIGH, configured)); }

    /** The full hold from the config, kept in range and at least {@link #MIN_SPAN} ticks above the minimum hold. */
    public static int maxHold(int configuredMin, int configuredMax) {
        int min = minHold(configuredMin);
        return Math.max(min + MIN_SPAN, Math.min(MAX_HOLD_HIGH, Math.max(MAX_HOLD_LOW, configuredMax)));
    }

    // ---------------------------------------------------------------- the curve
    public static Stage stage(int held, int min, int max) {
        if (held < min) return Stage.BASIC;
        return held >= max ? Stage.FULL : Stage.CHARGED;
    }

    /** How far through the charged range a hold is, 0..1: 0 at the minimum hold, 1 at the full hold. */
    public static double fraction(int held, int min, int max) {
        if (max <= min) return held >= max ? 1.0 : 0.0;
        return Math.max(0.0, Math.min(1.0, (held - min) / (double) (max - min)));
    }

    public static double power(Stage s, double f) {
        return switch (s) {
            case BASIC -> BASIC_POWER;
            case CHARGED -> CHARGED_POWER_MIN + (CHARGED_POWER_MAX - CHARGED_POWER_MIN) * f;
            case FULL -> FULL_POWER;
        };
    }

    public static double size(Stage s, double f) {
        return switch (s) {
            case BASIC -> BASIC_SIZE;
            case CHARGED -> CHARGED_SIZE_MIN + (CHARGED_SIZE_MAX - CHARGED_SIZE_MIN) * f;
            case FULL -> FULL_SIZE;
        };
    }

    public static double growth(Stage s, double f) {
        return switch (s) {
            case BASIC -> BASIC_GROWTH;
            case CHARGED -> CHARGED_GROWTH_MIN + (CHARGED_GROWTH_MAX - CHARGED_GROWTH_MIN) * f;
            case FULL -> FULL_GROWTH;
        };
    }

    /** The ring tier a stage draws: 1 = the hand ring, 2 = plus the middle ring, 3 = plus the outer ring, the loose glyphs and the lightning. */
    public static int tier(Stage s) {
        return switch (s) {
            case BASIC -> 1;
            case CHARGED -> 2;
            case FULL -> 3;
        };
    }

    public static int finishTicks(Stage s) {
        return switch (s) {
            case BASIC -> FINISH_BASIC;
            case CHARGED -> FINISH_CHARGED;
            case FULL -> FINISH_FULL;
        };
    }

    /** What a hold came to. {@code legacy} = the 3.4.0 spell (no hold was measured). */
    public record Charge(Stage stage, int held, double fraction, double power, double size, double growth, boolean legacy) {
        /** The fixed 18 tick spell of 3.4.0: full picture, power x1, size x1, Growth x1, both runes when sneaking. */
        public static final Charge LEGACY = new Charge(Stage.FULL, 0, 1.0, 1.0, 1.0, 1.0, true);

        public int tier() { return legacy ? 3 : RuneCharge.tier(stage); }

        /** A basic spell never writes the second rune, whatever the caster does with the sneak key. */
        public boolean allowsSecondRune() { return legacy || stage != Stage.BASIC; }

        /** The ticks of the cast's own charge before the bolt leaves: the finish of the rune after a release, or the old fixed charge. */
        public int castTicks(int legacyTicks) { return legacy ? legacyTicks : finishTicks(stage); }
    }

    /** The charge of a hold of {@code held} ticks under the settings {@code min} / {@code max}. */
    public static Charge of(int held, int min, int max) {
        int h = Math.max(0, Math.min(HOLD_CAP, held));
        Stage s = stage(h, min, max);
        double f = fraction(h, min, max);
        return new Charge(s, h, f, power(s, f), size(s, f), growth(s, f), false);
    }

    /** The modifiers a charge lets a cast carry: the page's own, or both runes when the caster sneaks and the stage allows it. */
    public static int modsFor(Charge c, RuneElement e, boolean sneaking) {
        return sneaking && c.allowsSecondRune() ? RuneMods.ALL : e.defaultMods();
    }
}
