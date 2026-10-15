package com.newuniverse.nusmp.boss.arc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: in the last act Lucius shows 3 to 5 identical copies and each one casts Time, Gravity or Spatial magic by itself. Left to
 * their own timers they would sometimes cast together and the players would face a wall of identical telegraphs they cannot read.
 * This pure scheduler (no Minecraft types, unit tested) fixes the count (3 at 30% health, one more for every 5% lost, at most 5),
 * gives every clone one spell in turn, staggers the casts so that at most one clone begins a cast on any tick and two clones never
 * begin the <i>same</i> spell within {@link #SPELL_GAP} ticks of each other, and holds the {@link PressureBudget} that all the
 * Lucius side shares, so the copies and the turned Paladins together cannot out-damage the cap on one player.
 */
public final class CloneScheduler {
    public enum Spell { TIME, GRAVITY, SPATIAL }

    public static final int MIN_CLONES = 3, MAX_CLONES = 5;
    /** Ticks between two casts of one clone, and the least gap between two clones starting the same spell. */
    public static final int CAST_INTERVAL = 160, SPELL_GAP = 40, FIRST_CAST_DELAY = 60;

    /** How many clones the health fraction asks for: 0 above the line, 3 at 30%, +1 per 5% lost, 5 at the most. */
    public static int cloneCount(float healthFraction) {
        if (Float.isNaN(healthFraction) || healthFraction >= LuciusArc.P3_BELOW) return 0;
        int extra = (int) Math.floor((LuciusArc.P3_BELOW - Math.max(0f, healthFraction) + 1e-6f) / 0.05f);
        return Math.max(MIN_CLONES, Math.min(MAX_CLONES, MIN_CLONES + extra));
    }

    /** The spell a clone in this slot casts: Time, Gravity, Spatial, then round again. */
    public static Spell spellOf(int slot) { return Spell.values()[Math.floorMod(slot, Spell.values().length)]; }

    /** A cast that starts now. */
    public record Cast(int slot, Spell spell) {}

    private final List<Long> nextAt = new ArrayList<>();
    private final Map<Spell, Long> lastStart = new HashMap<>();

    public int slots() { return nextAt.size(); }

    /**
     * Sets the number of slots. New slots get their first cast {@link #FIRST_CAST_DELAY} ticks later plus a stagger of
     * slot * (interval / slots), so the copies never begin in step.
     */
    public void resize(int count, long now) {
        int n = Math.max(0, Math.min(MAX_CLONES, count));
        while (nextAt.size() > n) nextAt.remove(nextAt.size() - 1);
        while (nextAt.size() < n) {
            int slot = nextAt.size();
            nextAt.add(now + FIRST_CAST_DELAY + (long) slot * (CAST_INTERVAL / Math.max(1, n)));
        }
    }

    public void clear() {
        nextAt.clear();
        lastStart.clear();
    }

    /**
     * One tick. 'alive' says which slots have a living clone that may cast (a slot without one only waits). Returns the casts that
     * begin now: at most one, and never a spell that another clone started less than {@link #SPELL_GAP} ticks ago.
     */
    public List<Cast> tick(long now, boolean[] alive) {
        for (int i = 0; i < nextAt.size(); i++) {
            if (nextAt.get(i) > now) continue;
            boolean can = alive != null && i < alive.length && alive[i];
            if (!can) {
                nextAt.set(i, now + 10);
                continue;
            }
            Spell sp = spellOf(i);
            long last = lastStart.getOrDefault(sp, Long.MIN_VALUE / 2);
            if (now - last < SPELL_GAP) {
                nextAt.set(i, last + SPELL_GAP);
                continue;
            }
            lastStart.put(sp, now);
            nextAt.set(i, now + CAST_INTERVAL);
            return List.of(new Cast(i, sp));
        }
        return List.of();
    }

    /**
     * The damage the whole Lucius side (clones, turned Paladins) may do to one victim per second: a share of the victim's maximum
     * health. Every hit asks {@link #allow} for its amount and gets back what fits; the window is 20 ticks, per victim.
     */
    public static final class PressureBudget {
        public static final double SHARE_PER_SECOND = 0.30;
        public static final int WINDOW = 20;

        private final Map<Integer, long[]> windows = new HashMap<>();   // victim -> {window start tick, spent as float bits}

        /** Returns the part of 'amount' that fits in this victim's second (0 when it is spent). */
        public float allow(int victim, long now, float amount, float victimMaxHealth) {
            if (!(amount > 0f) || !(victimMaxHealth > 0f)) return 0f;
            float cap = (float) (victimMaxHealth * SHARE_PER_SECOND);
            long[] w = windows.computeIfAbsent(victim, k -> new long[]{now, Float.floatToIntBits(0f)});
            if (now - w[0] >= WINDOW) {
                w[0] = now;
                w[1] = Float.floatToIntBits(0f);
            }
            float spent = Float.intBitsToFloat((int) w[1]);
            float give = Math.max(0f, Math.min(amount, cap - spent));
            w[1] = Float.floatToIntBits(spent + give);
            if (windows.size() > 64) windows.entrySet().removeIf(e -> now - e.getValue()[0] > WINDOW * 4);
            return give;
        }

        public void clear() { windows.clear(); }
    }
}
