package com.newuniverse.nusmp.skill.rules;

/**
 * Why: the five DanMachi spells (Wynn Fimbulvetr, Rea Laevateinn, Arcs Ray, Futsunomitama, Uchide no Kozuchi) are CHANTED: you hold the
 * key, one line of the chant finishes every few ticks, and releasing before the last line breaks the chant at no cost. How many lines each
 * spell has, how quickly mastery speeds the chant up, and every spell's numbers are pure data here, so the chant timing and the balance of
 * all five are unit-tested together.
 */
public final class ChantRules {
    private ChantRules() {}

    /** Lines, base magicule cost and cooldown (ticks) of each spell, before the DanMachi multipliers. */
    public enum Spell {
        WYNN_FIMBULVETR("wynn_fimbulvetr", 3, 3000, 600),
        REA_LAEVATEINN("rea_laevateinn", 3, 3500, 700),
        ARCS_RAY("arcs_ray", 1, 800, 100),
        FUTSUNOMITAMA("futsunomitama", 2, 2000, 500),
        UCHIDE_NO_KOZUCHI("uchide_no_kozuchi", 3, 4000, 6000);

        public final String id;
        public final int lines;
        public final double cost;
        public final int cooldown;

        Spell(String id, int lines, double cost, int cooldown) {
            this.id = id;
            this.lines = lines;
            this.cost = cost;
            this.cooldown = cooldown;
        }
    }

    /** Ticks one line of the chant takes: mastery makes the chant three times faster. */
    public static int ticksPerLine(Tier tier) { return tier == Tier.BASE ? 14 : 6; }

    public static int totalTicks(Spell spell, Tier tier) { return spell.lines * ticksPerLine(tier); }

    /** Lines finished after {@code heldTicks} of holding (never more than the spell has). */
    public static int linesDone(int heldTicks, Spell spell, Tier tier) {
        if (heldTicks <= 0) return 0;
        return Math.min(spell.lines, heldTicks / ticksPerLine(tier));
    }

    /** True once the whole chant is finished, i.e. releasing now casts. */
    public static boolean complete(int heldTicks, Spell spell, Tier tier) { return linesDone(heldTicks, spell, tier) >= spell.lines; }

    /** Longest hold: the chant plus a few seconds the finished chant can be held ready. */
    public static int maxHeldTicks(Spell spell, Tier tier) { return totalTicks(spell, tier) + 100; }

    // ---- Wynn Fimbulvetr: a blizzard at the target ----
    public static double blizzardRadius(Tier t) { return t == Tier.BASE ? 7 : 10; }
    /** Damage of the first strike; the lingering blizzard adds {@link #blizzardTickDamage} per pulse. */
    public static double blizzardDamage(Tier t) { return t == Tier.BASE ? 20 : 30; }
    public static double blizzardTickDamage(Tier t) { return t == Tier.BASE ? 3 : 5; }
    public static int blizzardTicks(Tier t) { return t == Tier.BASE ? 60 : 100; }
    public static int blizzardInterval() { return 20; }
    public static int freezeTicks(Tier t) { return t == Tier.BASE ? 200 : 260; }
    public static int slowTicks(Tier t) { return t == Tier.BASE ? 140 : 200; }

    // ---- Rea Laevateinn: pillars of flame around the caster ----
    public static double pillarRadius(Tier t) { return t == Tier.BASE ? 10 : 14; }
    public static int pillarWaves(Tier t) { return t == Tier.BASE ? 3 : 5; }
    public static int pillarsPerWave(Tier t) { return t == Tier.BASE ? 6 : 9; }
    public static int pillarInterval() { return 12; }
    public static double pillarDamage(Tier t) { return t == Tier.BASE ? 8 : 12; }
    public static double pillarHitRadius() { return 2.2; }
    public static int pillarBurnSeconds(Tier t) { return t == Tier.BASE ? 6 : 9; }
    /** Pillars never erupt closer than this to the caster (the caster is safe either way). */
    public static double pillarMinRadius() { return 3.0; }

    // ---- Arcs Ray: a piercing beam of light ----
    public static double rayRange(Tier t) { return t == Tier.BASE ? 48 : 64; }
    public static double rayDamage(Tier t) { return t == Tier.BASE ? 14 : 22; }
    public static double rayWidth() { return 1.2; }
    /** Light burns the undead: damage multiplier against them. */
    public static double rayUndeadFactor() { return 1.5; }

    // ---- Futsunomitama: a gravity field ----
    public static double gravityRadius(Tier t) { return t == Tier.BASE ? 6 : 8; }
    public static double gravityDamage(Tier t) { return t == Tier.BASE ? 10 : 16; }
    public static double gravityTickDamage(Tier t) { return t == Tier.BASE ? 2 : 3; }
    public static int gravityTicks(Tier t) { return t == Tier.BASE ? 60 : 120; }
    public static int gravityInterval() { return 10; }
    public static int pinTicks(Tier t) { return t == Tier.BASE ? 100 : 160; }
    public static double pullStrength(Tier t) { return t == Tier.BASE ? 0.25 : 0.4; }

    // ---- Uchide no Kozuchi: Level Boost on an ally ----
    public static int boostTicks(Tier t) { return t == Tier.BASE ? 1200 : 2400; }
    public static int boostAmp() { return 1; }
    public static int boostAbsorbAmp(Tier t) { return t == Tier.BASE ? 2 : 4; }
    public static double boostHealShare(Tier t) { return t == Tier.BASE ? 0.25 : 0.5; }
    public static double boostRange() { return 12.0; }

    /** Number of pulses a lingering zone of {@code duration} ticks makes at {@code interval}. */
    public static int pulses(int duration, int interval) { return interval <= 0 ? 0 : Math.max(0, duration / interval); }
}
