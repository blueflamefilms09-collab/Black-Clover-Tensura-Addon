package com.newuniverse.nusmp.ultimate;

/**
 * Why: Ultimate Magic (docs/magic/ultimate_magic.md) is the most dangerous thing a grimoire can do on a public server, so every number that bounds it lives HERE, in one pure class (no Minecraft types), and is unit
 * tested: what a cast costs, who may start one, when a cast is cancelled, how much damage one pulse may do, how long the cooldown is, how many may run at once on the whole server. The game classes
 * ({@link UltimateManager}) only read these functions; a datapack or a config value cannot go past a bound here (every config input is clamped by {@link #clampInt} / {@link #clampDouble}).
 *
 * <p>The price (decision, documented): NOTHING is paid when the wind-up starts. The caster must be at MAX magicule to start (99 percent counts, regeneration and rounding), must keep at least
 * {@link #HOLD_FRACTION} of it during the wind-up (a spell cast meanwhile that drains the reserve cancels the Ultimate), and at the moment of release 100 percent of the CURRENT magicule is taken in one go.
 * A cancelled cast therefore spent nothing and needs no refund (it also survives a crash: the magicule only changes at release). A cancelled cast only costs a short {@link #FAIL_LOCKOUT_TICKS} lockout.
 */
public final class UltimateRules {
    private UltimateRules() {}

    // ---------------------------------------------------------------- bounds (nothing can be configured past these)
    public static final double FULL_FRACTION = 0.99, HOLD_FRACTION = 0.90;
    public static final int MIN_WINDUP_TICKS = 100, MAX_WINDUP_TICKS = 400;
    public static final double MIN_RADIUS = 6.0, MAX_RADIUS = 24.0;
    public static final float MIN_RAW = 10f, MAX_RAW = 80f;
    /** Targets hit by ONE pulse; a cast has {@link #PULSES} pulses, so at most 72 hits in all. */
    public static final int MAX_TARGETS = 24, PULSES = 3, PULSE_GAP_TICKS = 10;
    /** Entities the area query may even look at per pulse (a mob farm in range cannot make a pulse expensive); the nearest {@link #MAX_TARGETS} of them are hit. */
    public static final int MAX_CANDIDATES = 96;
    public static final double[] PULSE_SHARE = {0.60, 0.25, 0.15};
    public static final int MAX_DEBUFFS = 4, MAX_DEBUFF_TICKS = 600, MAX_DEBUFF_AMPLIFIER = 3, MAX_IGNITE_SECONDS = 15, MAX_FREEZE_TICKS = 300;
    public static final double MAX_PUSH = 2.5, MAX_LIFT = 1.0;
    public static final int MIN_COOLDOWN_TICKS = 6000, DEFAULT_COOLDOWN_TICKS = 18000, MAX_COOLDOWN_TICKS = 144000;
    public static final int DEFAULT_SIMULTANEOUS = 2, MAX_SIMULTANEOUS = 8;
    public static final int FAIL_LOCKOUT_TICKS = 200, MIN_AGE_FOR_SNEAK_CANCEL = 20, SNEAK_CANCEL_TICKS = 15;
    public static final double DEFAULT_INTERRUPT = 0.35, MIN_INTERRUPT = 0.10, MAX_INTERRUPT = 1.0;
    public static final double MIN_SCALE = 0.25, MAX_SCALE = 2.0;
    /** At most this many nearby players get the "begins an Ultimate" warning line. */
    public static final int MAX_WARNED_PLAYERS = 32;
    public static final double WARN_EXTRA_RANGE = 48.0;
    /** The most effect packets one cast may send (wind-up re-sends included); every one still goes through the per-tick budget. */
    public static final int MAX_EFFECT_PACKETS = 16;
    public static final int CHARGE_FX_PERIOD = 40, FX_EMIT_PERIOD = 10, CHARGE_PARTICLES = 14;

    // ---------------------------------------------------------------- clamps
    public static int clampInt(int v, int lo, int hi) { return v < lo ? lo : Math.min(v, hi); }

    public static double clampDouble(double v, double lo, double hi) { return Double.isNaN(v) ? lo : v < lo ? lo : Math.min(v, hi); }

    public static int windupTicks(int base, double scale) { return clampInt((int) Math.round(base * clampDouble(scale, MIN_SCALE, MAX_SCALE)), MIN_WINDUP_TICKS, MAX_WINDUP_TICKS); }

    public static double radius(double base, double scale) { return clampDouble(base * clampDouble(scale, MIN_SCALE, MAX_SCALE), MIN_RADIUS, MAX_RADIUS); }

    public static int cooldownTicks(int configured) { return clampInt(configured, MIN_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS); }

    public static int simultaneousCap(int configured) { return clampInt(configured, 1, MAX_SIMULTANEOUS); }

    public static double interruptFraction(double configured) { return clampDouble(configured, MIN_INTERRUPT, MAX_INTERRUPT); }

    // ---------------------------------------------------------------- the price
    /** Is the caster at (practically) max magicule? A zero maximum never counts. */
    public static boolean atMax(double current, double max) { return max > 0 && current >= max * FULL_FRACTION; }

    /** Is the reserve still held in the middle of the wind-up? */
    public static boolean reserveHeld(double current, double max) { return max > 0 && current >= max * HOLD_FRACTION; }

    /** What the release takes: ALL of the current magicule (never more than what is there, never negative). */
    public static double release(double current) { return current <= 0 || Double.isNaN(current) ? 0.0 : current; }

    // ---------------------------------------------------------------- starting
    public enum Start { OK, DISABLED, GATE, COOLDOWN, LOCKOUT, BUSY, GLOBAL_CAP, NOT_FULL }

    /**
     * May this player begin an Ultimate now? The first failing reason, in a fixed order, so the message is stable. {@code creative} skips only the magicule requirement (an admin test); every other rule still holds.
     */
    public static Start start(boolean enabled, boolean gateOpen, long now, long cooldownUntil, long lockUntil, boolean busy, int active, int cap, double current, double max, boolean creative) {
        if (!enabled) return Start.DISABLED;
        if (!gateOpen) return Start.GATE;
        if (busy) return Start.BUSY;
        if (now < cooldownUntil) return Start.COOLDOWN;
        if (now < lockUntil) return Start.LOCKOUT;
        if (active >= simultaneousCap(cap)) return Start.GLOBAL_CAP;
        if (!creative && !atMax(current, max)) return Start.NOT_FULL;
        return Start.OK;
    }

    // ---------------------------------------------------------------- cancelling
    /** Why a cast ended before its release. All of them leave the magicule untouched (it is only taken at release). */
    public enum Cancel { DEATH, LOGOUT, DIMENSION, DAMAGE, SEALED, SNEAK, LOW_MANA, DISABLED, SHUTDOWN, ADMIN }

    /** Does the player's own cancel (sneaking) count yet? Not in the first second (the key that started the cast may still be down), and only after {@link #SNEAK_CANCEL_TICKS} ticks of it. */
    public static boolean sneakCancels(boolean enabled, int age, int sneakTicks) { return enabled && age >= MIN_AGE_FOR_SNEAK_CANCEL && sneakTicks >= SNEAK_CANCEL_TICKS; }

    /** Damage taken during the wind-up interrupts it at this fraction of max health (default 35 percent in total). */
    public static boolean interrupts(double damageTaken, double maxHealth, double fraction) {
        return maxHealth > 0 && damageTaken >= maxHealth * interruptFraction(fraction);
    }

    /** What a cancelled cast costs: nothing in magicule, and a lockout before the next try (a cast that was refused at the start has none). */
    public static long lockoutUntil(long now, Cancel why) { return why == Cancel.SHUTDOWN || why == Cancel.ADMIN ? 0L : now + FAIL_LOCKOUT_TICKS; }

    // ---------------------------------------------------------------- damage
    /** 1 at the centre, {@code 0.55} at the edge. */
    public static double falloff(double distance, double radius) {
        if (radius <= 0) return 1.0;
        return 1.0 - 0.45 * clampDouble(distance / radius, 0.0, 1.0);
    }

    /** The raw damage handed to the grimoire's balance law for one target in one pulse: base x scale x pulse share x falloff, clamped to {@link #MAX_RAW}. */
    public static float pulseRaw(float baseRaw, double damageScale, int pulse, double distance, double radius) {
        if (pulse < 0 || pulse >= PULSES) return 0f;
        double raw = clampDouble(baseRaw, MIN_RAW, MAX_RAW) * clampDouble(damageScale, 0.0, MAX_SCALE) * PULSE_SHARE[pulse] * falloff(distance, radius);
        return (float) clampDouble(raw, 0.0, MAX_RAW);
    }

    /** Debuffs last half as long on a boss. */
    public static int debuffTicks(int ticks, boolean boss) { return clampInt(boss ? ticks / 2 : ticks, 0, MAX_DEBUFF_TICKS); }

    public static int debuffAmplifier(int amp) { return clampInt(amp, 0, MAX_DEBUFF_AMPLIFIER); }

    // ---------------------------------------------------------------- pulses and the effect budget
    /** The tick (counted from the release) of pulse {@code i}. */
    public static int pulseTick(int i) { return clampInt(i, 0, PULSES - 1) * PULSE_GAP_TICKS; }

    /** The tick of the wind-up (age) at which the n-th charge picture is (re)sent: 0, 40, 80, ... but never more than 6 in all. */
    public static boolean chargeFxDue(int age, int windup) { return age % CHARGE_FX_PERIOD == 0 && age < windup && age / CHARGE_FX_PERIOD < 6; }

    /** The worst case of effect packets for one cast of this wind-up length (start circle + charge re-sends + 4 release + 2 pulse echoes). */
    public static int packetsFor(int windup) {
        int charge = 0;
        for (int a = 0; a < windup; a++) if (chargeFxDue(a, windup)) charge++;
        return 1 + charge + 4 + (PULSES - 1);
    }

    /** Particle packets in the wind-up: two per emission, one emission every {@link #FX_EMIT_PERIOD} ticks. */
    public static int particlePacketsFor(int windup) { return 2 * (windup / FX_EMIT_PERIOD + 1); }

    // ---------------------------------------------------------------- text
    public static String reason(Start s) {
        return switch (s) {
            case OK -> "";
            case DISABLED -> "Ultimate Magic is switched off on this server.";
            case GATE -> "Ultimate Magic is beyond you. It still needs: %s.";
            case COOLDOWN -> "The mana of the world has not settled yet (%s).";
            case LOCKOUT -> "Your mana is still unsettled from the broken cast (%s).";
            case BUSY -> "You are already gathering an Ultimate.";
            case GLOBAL_CAP -> "Too much magic is already being gathered in this world. Try again in a moment.";
            case NOT_FULL -> "Ultimate Magic needs ALL of your mana. Fill your magicule to the brim first.";
        };
    }

    public static String reason(Cancel c) {
        return switch (c) {
            case DEATH -> "The Ultimate dies with its caster.";
            case LOGOUT -> "The caster left; the Ultimate fades.";
            case DIMENSION -> "The caster crossed into another world; the Ultimate unravels.";
            case DAMAGE -> "The blows broke your concentration. The Ultimate unravels (nothing was spent).";
            case SEALED -> "Your magic was sealed. The Ultimate unravels (nothing was spent).";
            case SNEAK -> "You let the Ultimate go (nothing was spent).";
            case LOW_MANA -> "Your reserve of mana broke. The Ultimate unravels (nothing was spent).";
            case DISABLED -> "Ultimate Magic was switched off.";
            case SHUTDOWN -> "The world is closing.";
            case ADMIN -> "The Ultimate was ended.";
        };
    }
}
