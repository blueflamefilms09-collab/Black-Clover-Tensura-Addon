package com.newuniverse.nusmp.labyrinth.dnd.vital;

/**
 * Why: the numbers of the Vital Strike (threshold, meter, execution, cooldown) are pure arithmetic, so they live here with no Minecraft class
 * in the signatures. That makes them unit-testable by plain JUnit in CI (the tests cannot start a server), and it keeps the handler
 * ({@link VitalStrike}) down to "read the event, ask this class, write the event". Everything is a function of its arguments: the meter is not
 * ticked every game tick, it is stored as (value, tick it was written) and the decay is applied lazily when it is read ({@link #meterAt}),
 * so a player who never fights costs nothing and a mob-heavy dungeon does no per-player loop. Every input that can be hostile to float
 * arithmetic (NaN or infinite damage from a modded source, zero or negative max health) is answered with "no Vital Strike" instead of an
 * overflow or a division by zero, because a damage event is a place where another mod's numbers arrive unchecked.
 * <p>
 * The rule the owner gave (brief item 7): a hit at a critical threshold, or a hit by a high-tier Relic once the meter is full, bypasses the
 * buffer with a hyper-lethal execution. The buffer itself (see {@link HpBuffer}) is never removed: a Vital Strike is a hit that is turned into
 * a bigger hit, so every other rule that sits after the event (armour, resistance, absorption, totems, the target's own damage reduction)
 * still applies to it.
 */
public final class VitalMath {
    private VitalMath() {}

    /** The meter gained by one hit with a relic of the required grade. */
    public static final double METER_PER_HIT = 0.25;
    /** The meter lost per second (5 %), so 20 ticks. */
    public static final double METER_DECAY_PER_SECOND = 0.05;
    /** Decay per game tick. */
    public static final double METER_DECAY_PER_TICK = METER_DECAY_PER_SECOND / 20.0;
    /** A meter at or above this is full (guards the float error of four additions of 0.25 with a little decay). */
    public static final double FULL = 1.0 - 1e-9;

    /** What a hit became: whether it is a Vital Strike, the damage to hand back to the event, and the player's meter after it. */
    public record Outcome(boolean vital, float damage, double meter, boolean heavy, boolean relicCharged) {}

    /**
     * Can this target be struck at all? Only a buffered, non-boss mob. A boss has its own balance (the Floor Warden's 200 HP, the Zogratis
     * budget in {@code BudgetedBoss}); a mob that was never buffered (it spawned outside the Labyrinth, or the buffer is off) has no buffer to
     * bypass, so the rule does not apply to it.
     */
    public static boolean eligible(boolean boss, boolean buffered) { return !boss && buffered; }

    /** Is {@code amount} a heavy blow on a target with this maximum health? Never true for non-finite or non-positive numbers. */
    public static boolean heavyHit(float amount, float maxHealth, double threshold) {
        if (!(amount > 0f) || Float.isInfinite(amount)) return false;
        if (!(maxHealth > 0f) || Float.isInfinite(maxHealth)) return false;
        if (!(threshold > 0.0)) return false;
        return (double) amount >= threshold * (double) maxHealth;
    }

    /** The damage of an execution: at least {@code share} of the target's maximum health, never less than the hit that caused it, never NaN. */
    public static float executeDamage(float amount, float maxHealth, double share) {
        float base = (amount > 0f && !Float.isNaN(amount)) ? amount : 0f;
        if (!(maxHealth > 0f) || Float.isInfinite(maxHealth)) return base;
        double s = Math.max(0.0, Math.min(1.0, share));
        double exec = Math.min((double) maxHealth * s, (double) Float.MAX_VALUE);
        return (float) Math.max((double) Math.min(base, Float.MAX_VALUE), exec);
    }

    /** The meter as it stands at {@code now}: the stored value minus the decay since {@code writtenAt}. A clock that went backwards loses nothing. */
    public static double meterAt(double stored, long writtenAt, long now) {
        double v = clamp01(stored);
        long dt = now - writtenAt;
        if (dt <= 0) return v;
        return clamp01(v - METER_DECAY_PER_TICK * (double) dt);
    }

    /** The meter after one more relic hit. */
    public static double gain(double meter) { return clamp01(clamp01(meter) + METER_PER_HIT); }

    public static boolean full(double meter) { return meter >= FULL; }

    /** Ticks left of the cooldown (0 = ready). A clock that went backwards (a reloaded world) counts as ready, never as an endless wait. */
    public static long cooldownLeft(long lastVital, long now, int cooldownTicks) {
        if (cooldownTicks <= 0 || lastVital == Long.MIN_VALUE) return 0L;
        long elapsed = now - lastVital;
        if (elapsed < 0) return 0L;
        return Math.max(0L, (long) cooldownTicks - elapsed);
    }

    /**
     * The whole decision for one hit on an eligible target.
     * <ul>
     * <li>Not eligible (boss or no buffer): nothing changes, the damage stays.</li>
     * <li>Heavy hit and off cooldown: Vital Strike; the meter is untouched (a heavy hit is its own trigger).</li>
     * <li>Relic hit and off cooldown: the meter gains; when it reaches full the hit is a Vital Strike and the meter resets to 0.</li>
     * <li>On cooldown: neither path fires and the meter does not charge, so a player cannot bank a strike during the cooldown.</li>
     * </ul>
     *
     * @param meterNow the meter after the lazy decay ({@link #meterAt})
     */
    public static Outcome resolve(float amount, float maxHealth, boolean eligible, boolean relicHit, double meterNow,
                                  boolean onCooldown, double threshold, double executeShare) {
        double meter = clamp01(meterNow);
        if (!eligible || onCooldown) return new Outcome(false, amount, meter, false, false);
        boolean heavy = heavyHit(amount, maxHealth, threshold);
        boolean charged = false;
        if (relicHit) {
            meter = gain(meter);
            charged = full(meter);
        }
        if (heavy || charged) {
            if (charged) meter = 0.0;
            return new Outcome(true, executeDamage(amount, maxHealth, executeShare), meter, heavy, charged);
        }
        return new Outcome(false, amount, meter, false, false);
    }

    /** The grade gate: is a relic of grade ordinal {@code grade} at or above the configured minimum? */
    public static boolean gradeQualifies(int grade, int minGrade) { return grade >= minGrade; }

    private static double clamp01(double v) { return Double.isNaN(v) ? 0.0 : Math.max(0.0, Math.min(1.0, v)); }
}
