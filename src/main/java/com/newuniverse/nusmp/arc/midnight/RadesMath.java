package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;

/**
 * Why: Rades's fight rests on a few numbers that must stay in balance with each other: how many risen dead he may keep (his army), how much of every blow the
 * army soaks (the ward), how strong each corpse is, and how hard he must be pressed before he runs. If the ward were stronger than the party's reach the fight
 * would be a wall; if the corpses were stronger than the party the army would be the whole fight. They live in this pure class so a test can hold them to the
 * rules of the brief: the ward has a ceiling, anti-magic (the counter the story shows) takes it away, the corpses are weak enough to cut down and strong
 * enough to matter, and the retreat needs a real burst, not the first scratch.
 */
public final class RadesMath {
    private RadesMath() {}

    private static int byStage(int power, int... values) { return values[Math.max(0, Math.min(values.length - 1, PowerCurve.stage(power)))]; }

    // ---------------------------------------------------------------- the army
    /** The most corpses he keeps alive at his power, never more than the server's limit. */
    public static int thrallCap(int power, int configLimit) { return Math.max(0, Math.min(byStage(power, 2, 3, 4, 5, 6), configLimit)); }

    /** Corpses one Raise the Dead calls. */
    public static int graveCount(int power) { return byStage(power, 2, 2, 3, 3, 4); }

    public static double thrallHealth(int power) { return 20.0 + 8.0 * PowerCurve.stage(power); }

    public static double thrallAttack(int power) { return 4.0 + PowerCurve.stage(power); }

    public static double thrallSpeed(int power) { return 0.23 + 0.01 * PowerCurve.stage(power); }

    /** A corpse crumbles after this long even if nobody kills it (a minute): an army that cannot grow stale. */
    public static final int THRALL_LIFE = 1200;

    // ---------------------------------------------------------------- the ward
    public static final double WARD_PER_THRALL = 0.07, WARD_MAX = 0.42;
    /** Ticks the ward stays cut after a blow of anti-magic (eight seconds). */
    public static final int SEVER_TICKS = 160;

    /** The share of a blow that gets through the ward: each standing corpse soaks 7%, all of them together at most 42%, and a severed thread soaks nothing. */
    public static double wardFactor(int thralls, boolean severed) {
        if (severed || thralls <= 0) return 1.0;
        return 1.0 - Math.min(WARD_MAX, thralls * WARD_PER_THRALL);
    }

    // ---------------------------------------------------------------- the shots
    public static int bulletCount(int power) { return byStage(power, 4, 5, 6, 7, 8); }

    public static final double BULLET_SPEED = 1.8, BULLET_RADIUS = 0.45, BULLET_SPREAD = 0.5;
    public static final int BULLET_LIFE = 26, BULLET_HITS_PER_FOE = 3;

    public static int thunderCount(int power) { return byStage(power, 3, 3, 4, 4, 5); }

    public static final double THUNDER_RADIUS = 2.4;
    public static final int THUNDER_WARN = 22;

    /** The blast of a burst corpse. */
    public static final double BURST_RADIUS = 3.0;

    // ---------------------------------------------------------------- thread, hands, wail
    public static final double THREAD_REACH = 22.0, THREAD_STOP = 3.0, HANDS_RADIUS = 2.8, WAIL_RADIUS = 10.0, WAIL_BAND = 1.4, WAIL_SPEED = 1.0, WAIL_JUMP = 0.75;
    /** The thread follows its target until this many ticks are left, then freezes. */
    public static final int THREAD_LOCK = 8;

    /** Ticks a wail of radius {@code reach} takes to run out. */
    public static int wailTicks(double reach) { return (int) Math.ceil(reach / WAIL_SPEED) + 2; }

    // ---------------------------------------------------------------- the Soul Abductor and the retreat
    /** Corpses that fell within this many ticks can be called back (fifteen seconds). */
    public static final int ABDUCT_WINDOW = 300;
    /** Health lost within {@link #RETREAT_WINDOW} ticks as a share of his maximum that makes him leave, the cooldown after a retreat, and how far the gate takes him. */
    public static final double RETREAT_FRACTION = 0.12, RETREAT_DISTANCE = 14.0;
    public static final int RETREAT_WINDOW = 60, RETREAT_COOLDOWN = 400;

    /** Is he pressed hard enough to run? {@code lost} is the health he has actually lost within the window; the gate must also be ready. */
    public static boolean shouldRetreat(float lost, float maxHealth, long now, long readyAt) {
        return now >= readyAt && maxHealth > 0f && lost >= maxHealth * RETREAT_FRACTION;
    }

    /** How long the corpses run wild after the Legion or the Wrath (speed and strength). */
    public static final int FRENZY_TICKS = 160;
}
