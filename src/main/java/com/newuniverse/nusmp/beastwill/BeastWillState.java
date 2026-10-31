package com.newuniverse.nusmp.beastwill;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the per-player progress of the Beast Will system (3.45.0), as plain data with the rules that change it, so the progress, the strain and the daily budgets can be tested without a player. One
 * player has ONE active will and may have kept the progress of a few others ({@link #MAX_WILLS}); the STRAIN belongs to the body, not to the will, and the cooldowns are kept per will, so swapping wills
 * can neither shed strain nor start a cooldown over. Saved as an attachment ({@link BeastWillStore}, copied on death); times are absolute game ticks. The running form is saved as {@link #phase}
 * but is always cut back to off at login, because the attribute modifiers it applied are not saved.
 */
public final class BeastWillState {
    public static final int MAX_WILLS = 8;

    public String active = "";
    public int phase;
    public float strain;
    public long lockUntil, swapLockUntil;
    public final Map<String, Progress> wills = new LinkedHashMap<>();
    /** Game day of the last core this player got of each will (the farming gate). */
    public final Map<String, Long> coreDay = new HashMap<>();

    /** The progress of one will. */
    public static final class Progress {
        public float assimilation, mastery;
        /** The game day the two counters below belong to. */
        public long day = -1;
        public float dayAssimilation, dayMastery;
        /** When each ability KIND is ready again (game tick). */
        public final Map<String, Long> readyAt = new HashMap<>();

        void rollDay(long today) {
            if (day != today) {
                day = today;
                dayAssimilation = 0;
                dayMastery = 0;
            }
        }
    }

    public Progress progress(String id) { return wills.get(id); }

    public Progress activeProgress() { return active.isEmpty() ? null : wills.get(active); }

    public boolean has(String id) { return wills.containsKey(id); }

    /** Binds a will (keeps its progress when it was bound before). False when the player already keeps {@link #MAX_WILLS} others. */
    public boolean bind(String id) {
        if (!wills.containsKey(id)) {
            if (wills.size() >= MAX_WILLS) return false;
            wills.put(id, new Progress());
        }
        return true;
    }

    /** Makes a bound will the active one. The form is ended; strain and every cooldown stay exactly as they were. */
    public void activate(String id, long swapLockUntil) {
        if (!id.equals(active)) this.swapLockUntil = swapLockUntil;
        active = id;
        phase = BeastWillRules.OFF;
    }

    // ------------------------------------------------------------------ progress, with the day's budget
    /** Adds assimilation (a share of the whole) up to the day's cap; returns what was really added. */
    public double gainAssimilation(Progress p, double fraction, double dayCap, long now) {
        p.rollDay(BeastWillRules.dayOf(now));
        double room = Math.max(0, 1.0 - p.assimilation);
        double give = Math.min(room, BeastWillRules.budgeted(fraction, p.dayAssimilation, dayCap));
        p.assimilation += (float) give;
        p.dayAssimilation += (float) give;
        return give;
    }

    public double gainMastery(Progress p, double fraction, double dayCap, long now) {
        p.rollDay(BeastWillRules.dayOf(now));
        double room = Math.max(0, 1.0 - p.mastery);
        double give = Math.min(room, BeastWillRules.budgeted(fraction, p.dayMastery, dayCap));
        p.mastery += (float) give;
        p.dayMastery += (float) give;
        return give;
    }

    // ------------------------------------------------------------------ strain
    public void addStrain(double delta) { strain = BeastWillRules.clampStrain(strain + delta); }

    public boolean locked(long now) { return now < lockUntil; }

    public boolean swapLocked(long now) { return now < swapLockUntil; }

    /**
     * The will is rejected at full strain: the form ends, the will goes quiet for {@code lockTicks}, the assimilation drops by {@code assimLoss} (never below zero, never the whole of it) and the strain falls
     * to {@code strainAfter} so the player is not locked out forever. Nothing else is touched: no skill, no level, no item is lost.
     */
    public void reject(long now, int lockTicks, double assimLoss, double strainAfter) {
        Progress p = activeProgress();
        if (p != null) p.assimilation = (float) Math.max(0, p.assimilation - Math.max(0, Math.min(0.5, assimLoss)));
        lockUntil = now + Math.max(0, lockTicks);
        phase = BeastWillRules.OFF;
        strain = BeastWillRules.clampStrain(Math.min(strain, strainAfter));
    }

    // ------------------------------------------------------------------ cooldowns (per will and kind, absolute)
    public boolean ready(Progress p, String kind, long now) {
        Long t = p.readyAt.get(kind);
        return t == null || now >= t;
    }

    public long left(Progress p, String kind, long now) {
        Long t = p.readyAt.get(kind);
        return t == null ? 0 : Math.max(0, t - now);
    }

    public void startCooldown(Progress p, String kind, long now, int ticks) { p.readyAt.put(kind, now + Math.max(0, ticks)); }

    /** A game clock that was reset (a restored world) must not leave a cooldown of years: every entry is cut to {@code maxTicks} from now. */
    public void clampClock(long now, int maxTicks) {
        for (Progress p : wills.values()) p.readyAt.replaceAll((k, v) -> Math.min(v, now + maxTicks));
        lockUntil = Math.min(lockUntil, now + 10 * maxTicks);
        swapLockUntil = Math.min(swapLockUntil, now + 10 * maxTicks);
    }

    /** True when nothing is worth saving (no will, no strain): the attachment is then not written. */
    public boolean isEmpty() { return wills.isEmpty() && strain <= 0 && lockUntil == 0 && coreDay.isEmpty(); }
}
