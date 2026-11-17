package com.newuniverse.nusmp.relics.djinn;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Belial's numbers (pure, so BelialSeverRulesTest can check every bound): the SEVER hit of the canon Belial (owner notes: his scythe ignores physical
 * durability by severing the target's spirit) and the EXILE (the spirit is cut loose and sent "to another dimension", played as a short, safe state in
 * place, never a real teleport between dimensions).
 *
 * <p><b>Sever.</b> A hit is {@code power + fraction x max health}, with the fraction smaller against players and much smaller against bosses, then
 * limited per hit (a share of max health plus a small floor) and per second (a rolling window per target, {@link Window}). It ignores armour, the
 * hit cooldown and every immunity the mod knows (see {@code BelialSever}) but never the caps here: it is "broken" against defences, not against the
 * balance of the game.
 *
 * <p><b>Exile.</b> 3 to 6 seconds (a player at most 4), at most {@link #MAX_VICTIMS} victims per cast, {@link #MAX_PER_CASTER} per caster at once,
 * {@link #MAX_GLOBAL} on the whole server, a victim is immune to a new exile for {@link #IMMUNE_TICKS} after it returns, and bosses, creative and
 * spectator players and anything a guard vetoes are never exiled (they take the sever hit instead).
 */
public final class BelialSeverRules {
    private BelialSeverRules() {}

    /** Who is hit: a plain mob, a player (the balance law's PvP cap applies on top), a boss (very little). */
    public enum Kind { MOB, PLAYER, BOSS }

    // ---------------------------------------------------------------- sever
    /** Share of the target's max health a spirit_sever adds to the act's power. */
    public static final double ACT_FRAC = 0.08;
    /** The same for the Extreme Magic, per strike. */
    public static final double EXTREME_FRAC = 0.12;
    /** The fraction is scaled by this much against a player / a boss. */
    public static final double PLAYER_FRAC_MULT = 0.5, BOSS_FRAC_MULT = 0.15;
    /** The power is scaled by this much against a boss. */
    public static final double BOSS_POWER_MULT = 0.5;
    /** One hit never takes more than this share of max health (plus {@link #HIT_FLOOR}) from a mob / a player / a boss. */
    public static final double MOB_HIT_SHARE = 0.30, PLAYER_HIT_SHARE = 0.25, BOSS_HIT_SHARE = 0.03;
    /** Added to the per-hit cap so a weak mob is not capped below the act's own power. */
    public static final double HIT_FLOOR = 8.0;
    /** One hit never takes more than this, whatever the target. */
    public static final double HARD_HIT_CAP = 400.0;
    /** In any one second (20 ticks) a target takes at most this share of max health from sever hits (a mob / a player / a boss). */
    public static final double MOB_SECOND_SHARE = 0.60, PLAYER_SECOND_SHARE = 0.40, BOSS_SECOND_SHARE = 0.06;
    /** The window of the per-second cap, in ticks. */
    public static final int WINDOW_TICKS = 20;
    /** A hit smaller than this is dropped (nothing worth a packet). */
    public static final double MIN_HIT = 0.5;

    private static double fracMult(Kind k) { return k == Kind.BOSS ? BOSS_FRAC_MULT : k == Kind.PLAYER ? PLAYER_FRAC_MULT : 1.0; }

    /** The hit before the caps: {@code power + fraction x maxHealth}, softer against players and bosses. */
    public static double raw(Kind k, double power, double maxHealth, double frac) {
        double p = k == Kind.BOSS ? power * BOSS_POWER_MULT : power;
        return Math.max(0.0, p) + Math.max(0.0, frac) * fracMult(k) * Math.max(0.0, maxHealth);
    }

    /** The most one hit may take from this target. */
    public static double hitCap(Kind k, double maxHealth) {
        double share = k == Kind.BOSS ? BOSS_HIT_SHARE : k == Kind.PLAYER ? PLAYER_HIT_SHARE : MOB_HIT_SHARE;
        double floor = k == Kind.BOSS ? 0.0 : HIT_FLOOR;
        return Math.min(HARD_HIT_CAP, share * Math.max(0.0, maxHealth) + floor);
    }

    /** The most a target takes from sever hits in one window. */
    public static double secondCap(Kind k, double maxHealth) {
        double share = k == Kind.BOSS ? BOSS_SECOND_SHARE : k == Kind.PLAYER ? PLAYER_SECOND_SHARE : MOB_SECOND_SHARE;
        return Math.max(0.0, share * Math.max(0.0, maxHealth)) + (k == Kind.BOSS ? 0.0 : HIT_FLOOR);
    }

    /** The hit after the per-hit cap (the per-second cap is {@link Window#grant}). */
    public static double hit(Kind k, double power, double maxHealth, double frac) {
        return Math.min(raw(k, power, maxHealth, frac), hitCap(k, maxHealth));
    }

    /** A rolling one-second budget for one target: {@link #grant} gives what is left of the cap, never more than wanted. */
    public static final class Window {
        private long start = Long.MIN_VALUE;
        private double taken;

        public double grant(long now, double want, double cap) {
            if (start == Long.MIN_VALUE || now - start >= WINDOW_TICKS || now < start) {
                start = now;
                taken = 0.0;
            }
            double g = Math.max(0.0, Math.min(want, cap - taken));
            taken += g;
            return g;
        }

        public long start() { return start; }

        public double taken() { return taken; }
    }

    /** Per-target windows, bounded: stale ones are dropped by {@link #sweep}, and the table never holds more than {@link #MAX_WINDOWS}. */
    public static final int MAX_WINDOWS = 512;

    /** Drops the windows whose second is over; if the table is still over {@link #MAX_WINDOWS} it is cleared (a target never takes more than its cap anyway: the next hit opens a fresh window). */
    public static void sweep(Map<UUID, Window> windows, long now) {
        for (Iterator<Window> it = windows.values().iterator(); it.hasNext(); ) {
            Window w = it.next();
            if (now - w.start() >= WINDOW_TICKS || now < w.start()) it.remove();
        }
        if (windows.size() > MAX_WINDOWS) windows.clear();
    }

    // ---------------------------------------------------------------- exile
    public static final int EXILE_MIN = 60, EXILE_MAX = 120, EXILE_PLAYER_MAX = 80;
    public static final int MAX_VICTIMS = 3, MAX_PER_CASTER = 4, MAX_GLOBAL = 24;
    /** After it returns, a victim cannot be exiled again for this long. */
    public static final int IMMUNE_TICKS = 100;
    /** The immune table is bounded the same way as the windows. */
    public static final int MAX_IMMUNE = 256;
    /** An exiled target is held in place this often (ticks). */
    public static final int HOLD_EVERY = 4;
    /** A returning victim is put back at its spot only if it was moved further than this (blocks). */
    public static final double RETURN_SLACK = 0.75;

    /** How long a victim stays in the other dimension: 3 s, +1 s in a Full Equip, +0.5 s for a dark Djinn; 3 to 6 s for a mob, at most 4 s for a player. */
    public static int exileTicks(boolean player, boolean full, boolean dark) {
        int t = EXILE_MIN + (full ? 20 : 0) + (dark ? 10 : 0);
        return Math.max(EXILE_MIN, Math.min(player ? EXILE_PLAYER_MAX : EXILE_MAX, t));
    }

    /** Why a target is not exiled ({@link #OK}: it is). */
    public enum Refusal { OK, DISABLED, BOSS, CREATIVE, VETO, IMMUNE, ALREADY, CASTER_CAP, GLOBAL_CAP }

    /** The one rule of who can be exiled. A refused target takes the sever hit instead. */
    public static Refusal canExile(boolean enabled, Kind k, boolean creativeOrSpectator, boolean vetoed, boolean immune, boolean already, int casterCount, int globalCount) {
        if (!enabled) return Refusal.DISABLED;
        if (k == Kind.BOSS) return Refusal.BOSS;
        if (creativeOrSpectator) return Refusal.CREATIVE;
        if (vetoed) return Refusal.VETO;
        if (already) return Refusal.ALREADY;
        if (immune) return Refusal.IMMUNE;
        if (casterCount >= MAX_PER_CASTER) return Refusal.CASTER_CAP;
        if (globalCount >= MAX_GLOBAL) return Refusal.GLOBAL_CAP;
        return Refusal.OK;
    }

    /** Drops the expired immunities (game time) and clears the table if it is still too large. */
    public static void sweepImmune(Map<UUID, Long> immune, long now) {
        immune.values().removeIf(until -> until <= now);
        if (immune.size() > MAX_IMMUNE) immune.clear();
    }

    /** A fresh window table (kept here so a test can build one without the glue). */
    public static Map<UUID, Window> newWindows() { return new HashMap<>(); }
}
