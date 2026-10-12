package com.newuniverse.nusmp.entity.zogratis;

/**
 * Dante's Body Magic regeneration, as a pure state machine (no Minecraft types, unit tested). The boss feeds it every hit he is about
 * to take and it answers with what the flesh does about it:
 * <ul>
 *   <li><b>The pulse</b>: a hit that would leave him at or under {@link #MORTAL} of his max health is a mortal wound. If the pulse is
 *       ready it closes at once and restores a share of his max health ({@link #pulseFraction}, doubled once the Gates are open),
 *       then it needs {@link #cooldownTicks} to knit again. Each pulse is a little weaker than the last ({@link #STRAIN}).</li>
 *   <li><b>Counter-play</b>, all of it fair and visible: an anti-magic hit cuts the Body Magic for {@link #SUPPRESS_TICKS} (no pulse, no
 *       trickle, and the cooldown restarts); a burst of damage in a couple of seconds halves the pulse and a bigger one denies it
 *       ({@link #OVERLOAD_HALF}, {@link #OVERLOAD_DENY}); a blow that no pulse could cover is simply lethal; and while he is exposed
 *       or staggered after an ultimate the pulse and the trickle are halved ({@link #STAGGER}).</li>
 *   <li><b>The trickle</b>: a small share of max health each second on top of the framework's regeneration, also doubled at the Gates.</li>
 * </ul>
 * Time is in ticks and supplied by the caller; the machine keeps no clock of its own.
 */
public final class BodyRegen {
    /** What the flesh did about a hit. */
    public enum Result { IDLE, FIRED, COOLDOWN, SUPPRESSED, OVERLOADED, OVERWHELMED }

    /** The answer for one hit: what happened, and how much health to restore before the hit lands. */
    public record Decision(Result result, float heal) {}

    /** A hit that leaves him at or under this share of max health is a mortal wound. */
    public static final double MORTAL = 0.30;
    /** Anti-magic cuts the Body Magic for this long (8 s). */
    public static final int SUPPRESS_TICKS = 160;
    /** Pulse and trickle factor while he is exposed or staggered. */
    public static final double STAGGER = 0.5;
    /** Each pulse used makes the next one this much weaker. */
    public static final double STRAIN = 0.75;
    /** Share of max health dealt within the burst window that halves the pulse, and that denies it. */
    public static final double OVERLOAD_HALF = 0.10, OVERLOAD_DENY = 0.20;
    /** The burst counter fades by this factor every tick (half-life about a second and a half). */
    public static final double BURST_DECAY = 0.975;
    /** The burst counter never holds more than this (an absurd blow cannot overflow it). */
    private static final double BURST_CAP = 1.0e6;

    private static final double[] PULSE = {0.08, 0.10, 0.12, 0.14, 0.26};
    private static final int[] COOLDOWN = {800, 760, 700, 640, 520};
    private static final double[] TRICKLE = {0.0004, 0.0005, 0.0006, 0.00075, 0.0015};

    private long readyAt, suppressedUntil, lastTick;
    private double burst;
    private int used;

    /** Share of max health one pulse restores at this power (0.08 at the opening, 0.14 at 80%, 0.26 once the Gates are open). */
    public static double pulseFraction(int power) { return PULSE[PowerCurve.stage(power)]; }

    /** Ticks a pulse needs before it can close another wound (40 s at the opening, 26 s once the Gates are open). */
    public static int cooldownTicks(int power) { return COOLDOWN[PowerCurve.stage(power)]; }

    /** Share of max health restored each second by the trickle. */
    public static double trickleFraction(int power) { return TRICKLE[PowerCurve.stage(power)]; }

    /** How much weaker the next pulse is after 'used' pulses: 1, 0.75, 0.56 ... */
    public static double strain(int used) { return Math.pow(STRAIN, Math.max(0, used)); }

    /**
     * A hit of 'incoming' (the damage he is about to take, with every bonus) at tick 'now' while he has 'health' of 'maxHealth'.
     * The caller restores {@link Decision#heal()} first and then lets the hit land.
     */
    public Decision onHit(long now, float health, float maxHealth, float incoming, int power, boolean staggered, boolean antiMagic) {
        decay(now);
        if (!(incoming > 0f)) incoming = 0f;                         // a negative or NaN amount counts as nothing, and never poisons the counter
        burst = Math.min(BURST_CAP, burst + incoming);
        if (antiMagic) {
            suppressedUntil = Math.max(suppressedUntil, now + SUPPRESS_TICKS);
            readyAt = Math.max(readyAt, now + SUPPRESS_TICKS);
            return new Decision(Result.SUPPRESSED, 0f);
        }
        if (health - incoming > maxHealth * MORTAL) return new Decision(Result.IDLE, 0f);
        if (now < suppressedUntil) return new Decision(Result.SUPPRESSED, 0f);
        if (now < readyAt) return new Decision(Result.COOLDOWN, 0f);
        double pulse = maxHealth * pulseFraction(power) * strain(used) * (staggered ? STAGGER : 1.0);
        double load = maxHealth <= 0 ? 0 : burst / maxHealth;
        if (load >= OVERLOAD_DENY) return new Decision(Result.OVERLOADED, 0f);
        boolean half = load >= OVERLOAD_HALF;
        if (half) pulse *= 0.5;
        if (burst >= health + pulse) return new Decision(Result.OVERWHELMED, 0f);
        float heal = (float) Math.max(0, Math.min(pulse, maxHealth - health));
        readyAt = now + cooldownTicks(power);
        used++;
        return new Decision(Result.FIRED, heal);
    }

    /** Health to restore this second from the trickle (0 while the Body Magic is cut). */
    public float trickle(long now, float maxHealth, int power, boolean staggered) {
        if (now < suppressedUntil) return 0f;
        return (float) (maxHealth * trickleFraction(power) * (staggered ? STAGGER : 1.0));
    }

    private void decay(long now) {
        long dt = Math.max(0, now - lastTick);
        lastTick = now;
        if (dt > 0) burst *= Math.pow(BURST_DECAY, Math.min(dt, 600));
    }

    /** Damage dealt in the recent past, faded (what the overload rules read). */
    public double burst(long now) {
        decay(now);
        return burst;
    }

    public boolean ready(long now) { return now >= readyAt && now >= suppressedUntil; }
    public boolean suppressed(long now) { return now < suppressedUntil; }
    public int cooldownLeft(long now) { return (int) Math.max(0, readyAt - now); }
    public int suppressedLeft(long now) { return (int) Math.max(0, suppressedUntil - now); }
    public int used() { return used; }

    /** A new fight: the flesh is whole again. */
    public void reset() {
        readyAt = 0;
        suppressedUntil = 0;
        burst = 0;
        used = 0;
        lastTick = 0;
    }

    /** Puts back a saved state ('cooldownLeft' and 'suppressedLeft' in ticks from 'now'). */
    public void restore(long now, int cooldownLeft, int suppressedLeft, int used) {
        readyAt = now + Math.max(0, cooldownLeft);
        suppressedUntil = now + Math.max(0, suppressedLeft);
        this.used = Math.max(0, used);
        burst = 0;
        lastTick = now;
    }
}
