package com.newuniverse.nusmp.dungeon.nexus.relic;

/**
 * The numbers of the Devil-Bound Weapon Sheath (docs/living_dungeon.md section 7): charge from parried and blocked magic (0 to 100), the
 * Anti-Magic slash it releases once full, and the Mana Siphon curse that discharges it into the wearer when it sits full for more than a
 * minute. Pure.
 */
public final class SheathMath {
    private SheathMath() {}

    public static final int MAX = 100;
    /** Ticks fully charged (and worn) before it discharges, the two warnings before that, and what a second of full charge counts for. */
    public static final int FULL_LIMIT = 1200, WARN_AT = 800, CRACKLE_AT = 1050;
    /** The slash: length and half-width in blocks, how many it can hit, and its raw damage before the balance law scales it. */
    public static final double SLASH_RANGE = 28.0, SLASH_WIDTH = 1.9;
    public static final int SLASH_TARGETS = 12;
    public static final float SLASH_RAW = 70f;
    /** The discharge hurts at most this share of the wearer's maximum health, and never takes the last point. */
    public static final float DISCHARGE_SHARE = 0.30f;
    /** How many magicules (a share of the maximum pool) one point of charge feeds back to the wearer. */
    public static final double MAGICULE_PER_POINT = 0.0005;

    /** Charge points a blocked magical hit of this size adds: 2 per point of damage, between 5 and 40, and half again for a true parry. */
    public static int gain(float blocked, boolean parried) {
        if (!(blocked > 0f)) return 0;
        double g = Math.max(5.0, Math.min(40.0, blocked * 2.0));
        return (int) Math.round(parried ? g * 1.5 : g);
    }

    public static int add(int charge, int gain) { return Math.max(0, Math.min(MAX, charge + Math.max(0, gain))); }

    public static boolean full(int charge) { return charge >= MAX; }

    /** The ticks the sheath has sat full while worn, after {@code elapsed} more of them: back to zero as soon as it is not full. */
    public static int fullTicks(int fullTicks, int charge, int elapsed) {
        if (!full(charge)) return 0;
        return Math.min(FULL_LIMIT + 200, Math.max(0, fullTicks) + Math.max(0, elapsed));
    }

    public enum Siphon { QUIET, WARN, CRACKLE, DISCHARGE }

    public static Siphon siphon(int fullTicks) {
        if (fullTicks > FULL_LIMIT) return Siphon.DISCHARGE;
        if (fullTicks >= CRACKLE_AT) return Siphon.CRACKLE;
        if (fullTicks >= WARN_AT) return Siphon.WARN;
        return Siphon.QUIET;
    }

    /** Damage of the discharge: a share of the maximum health, but always leaving at least one point. */
    public static float dischargeDamage(float health, float maxHealth) {
        return Math.max(0f, Math.min(maxHealth * DISCHARGE_SHARE, health - 1f));
    }

    /** The share of the maximum magicule pool a gain of this many points feeds back. */
    public static double magiculeShare(int gainedPoints) { return Math.max(0, gainedPoints) * MAGICULE_PER_POINT; }
}
