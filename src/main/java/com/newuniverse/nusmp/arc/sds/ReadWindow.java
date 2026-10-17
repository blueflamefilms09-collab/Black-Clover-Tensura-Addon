package com.newuniverse.nusmp.arc.sds;

import com.newuniverse.nusmp.arc.sds.kit.DamageKind;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;

/**
 * Why: Light of Prying Eyes is Gowther's answer to a party that wins by doing one thing as hard as it can (the owner's "transcend standard stat checks"): he reads
 * the thoughts of whoever fights him, which in the game means the KIND of blow they keep dealing, and for a few seconds he turns that kind aside. The rule is made so a
 * player can see it and beat it: it reads only what was actually dealt in the last eight seconds, needs a minimum of it (a party that has hardly hit him is not read),
 * cuts only the ONE kind it read (every other kind lands in full), lasts a fixed time and cannot be started again for the move's own long cool-down. The counter is the
 * obvious one, and it is the point: switch from blades to spells, or from spells to arrows, while the reading lasts. This pure class keeps the tally of what was dealt and
 * the reading; the glue says what kind each blow was ({@link DamageKind}).
 */
public final class ReadWindow {
    private record Hit(long tick, DamageKind kind, float amount) {}

    private final ArrayDeque<Hit> hits = new ArrayDeque<>();
    private final int span;
    private DamageKind reading;
    private long readUntil = Long.MIN_VALUE;

    /** A window that looks back {@code spanTicks} ticks (at least 1). */
    public ReadWindow(int spanTicks) { this.span = Math.max(1, spanTicks); }

    private void prune(long now) {
        while (!hits.isEmpty() && now - hits.peekFirst().tick() >= span) hits.pollFirst();
    }

    /** Notes a blow he took (a non-positive or non-finite amount counts for nothing). */
    public void record(long now, DamageKind kind, float amount) {
        prune(now);
        if (kind != null && Float.isFinite(amount) && amount > 0f) hits.addLast(new Hit(now, kind, amount));
    }

    /** The damage of each kind in the window. */
    public Map<DamageKind, Float> tally(long now) {
        prune(now);
        Map<DamageKind, Float> m = new EnumMap<>(DamageKind.class);
        for (Hit h : hits) m.merge(h.kind(), h.amount(), Float::sum);
        return m;
    }

    /** The kind he would read: the one dealt the most, when the window holds at least {@code minTotal} in all; null otherwise. Ties go to the earlier kind of the enum. */
    public DamageKind dominant(long now, float minTotal) {
        Map<DamageKind, Float> m = tally(now);
        float total = 0f;
        for (float v : m.values()) total += v;
        if (total < minTotal) return null;
        DamageKind best = null;
        float bestAmount = 0f;
        for (DamageKind k : DamageKind.values()) {
            float v = m.getOrDefault(k, 0f);
            if (v > bestAmount) {
                best = k;
                bestAmount = v;
            }
        }
        return best;
    }

    /** Starts a reading of {@code kind} for {@code ticks}. */
    public void begin(long now, DamageKind kind, int ticks) {
        reading = kind;
        readUntil = now + Math.max(1, ticks);
    }

    public boolean active(long now) { return reading != null && now < readUntil; }

    /** The kind being read, or null when no reading is on. */
    public DamageKind reading(long now) { return active(now) ? reading : null; }

    public int ticksLeft(long now) { return active(now) ? (int) (readUntil - now) : 0; }

    /** What a blow of {@code kind} is multiplied by now: {@code 1 - cut} while that kind is being read, else 1. */
    public float factor(long now, DamageKind kind, float cut) {
        if (!active(now) || kind != reading) return 1f;
        return 1f - Math.max(0f, Math.min(0.9f, cut));
    }

    /** Ends the reading now (anti-magic, a stagger). */
    public void end() {
        reading = null;
        readUntil = Long.MIN_VALUE;
    }

    /** A fresh fight. */
    public void reset() {
        hits.clear();
        end();
    }
}
