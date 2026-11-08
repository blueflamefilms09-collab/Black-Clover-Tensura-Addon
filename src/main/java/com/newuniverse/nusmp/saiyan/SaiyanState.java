package com.newuniverse.nusmp.saiyan;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: the per-player progress of the Saiyan system (3.63.0, Stage 1) as plain data with the rules that change it, so Battle Power, the daily budget, the repeat-kill memory and the cooldowns can be
 * tested without a player. Ki is NOT stored here: for a Saiyan Ki is the player's Tensura magicule pool ({@link SaiyanKi}), so Tensura saves and regenerates it. Saved as an attachment
 * ({@link SaiyanStore}, copied on death); times are absolute game ticks. A player who is not a Saiyan has nothing saved.
 */
public final class SaiyanState {
    public static final int MAX_RECENT = 24;
    public static final int MAX_COOLDOWNS = 16;

    public boolean saiyan, awakened;
    /** Stamina; negative = never set (full on the first look). */
    public double stamina = -1.0;
    public double battlePower = SaiyanRules.START_BP;
    /** The game day the counter below belongs to, and the Battle Power gained on it. */
    public long bpDay = -1;
    public double bpToday;
    /** Kills of one creature type in the current window: type -> {count, window start tick}. */
    public final Map<String, long[]> recent = new LinkedHashMap<>();
    /** When each Ki attack is ready again (game tick). */
    public final Map<String, Long> readyAt = new HashMap<>();

    public SaiyanRules.Rank rank() { return SaiyanRules.rankOf(battlePower); }

    /** What is left to gain today (rolls the day over first). */
    public double roomToday(long now, double fraction, double floor) {
        rollDay(now);
        return SaiyanRules.dailyRoom(battlePower, fraction, floor, bpToday);
    }

    private void rollDay(long now) {
        long d = SaiyanRules.dayOf(now);
        if (d != bpDay) {
            bpDay = d;
            bpToday = 0.0;
        }
    }

    /** Adds Battle Power up to the day's room and the hard cap; returns what was really added. */
    public double addBattlePower(double gain, long now, double fraction, double floor) {
        double give = Math.min(Math.max(0.0, gain), roomToday(now, fraction, floor));
        give = Math.min(give, SaiyanRules.BP_HARD_CAP - battlePower);
        if (give <= 0.0) return 0.0;
        battlePower += give;
        bpToday += give;
        return give;
    }

    /** Sets Battle Power directly (a command): clamped, and the day's counter is left alone. */
    public void setBattlePower(double bp) { battlePower = SaiyanRules.clampBp(bp); }

    // ------------------------------------------------------------------ the repeat-kill memory
    /** How many kills of this type fall inside the window that started at the first of them (a stale window counts as none). */
    public int recentKills(String type, long now, long windowTicks) {
        long[] r = recent.get(type);
        return r == null || now - r[1] > windowTicks || now < r[1] ? 0 : (int) r[0];
    }

    /** Remembers one more kill of the type; the memory is bounded and the oldest entry goes first. */
    public void noteKill(String type, long now, long windowTicks) {
        long[] r = recent.get(type);
        if (r == null || now - r[1] > windowTicks || now < r[1]) r = new long[] {0, now};
        r[0]++;
        recent.remove(type);
        recent.put(type, r);
        while (recent.size() > MAX_RECENT) recent.remove(recent.keySet().iterator().next());
    }

    // ------------------------------------------------------------------ cooldowns
    public boolean ready(String kind, long now) {
        Long t = readyAt.get(kind);
        return t == null || now >= t;
    }

    public long left(String kind, long now) {
        Long t = readyAt.get(kind);
        return t == null ? 0 : Math.max(0, t - now);
    }

    public void startCooldown(String kind, long now, int ticks) {
        if (readyAt.size() >= MAX_COOLDOWNS && !readyAt.containsKey(kind)) {
            readyAt.values().removeIf(t -> t <= now);
            while (readyAt.size() >= MAX_COOLDOWNS) {                                  // still full: the one that ends soonest goes
                String soonest = null;
                long least = Long.MAX_VALUE;
                for (Map.Entry<String, Long> e : readyAt.entrySet()) if (e.getValue() < least) { least = e.getValue(); soonest = e.getKey(); }
                readyAt.remove(soonest);
            }
        }
        readyAt.put(kind, now + Math.max(0, ticks));
    }

    /** A game clock that was reset (a restored world) must not leave a cooldown of years. */
    public void clampClock(long now, int maxTicks) { readyAt.replaceAll((k, v) -> Math.min(v, now + maxTicks)); }

    /** True when nothing is worth saving (not a Saiyan and no progress): the attachment is then not written. */
    public boolean isEmpty() { return !saiyan && !awakened && battlePower <= SaiyanRules.START_BP + 1.0e-9; }
}
