package com.newuniverse.nusmp.arc.midnight;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Rhya's magic is to copy, and the fight must show it: the last spell that hit him is the one he answers with, in its colours and with its rider, and he
 * resists it for a while because he knows it. That needs a memory with three rules that are easy to get wrong and easy to test: the newest magic wins, a
 * magic he met again moves to the front instead of filling a second slot, and what he learned fades after twenty-four seconds. At the Gates (the Third Eye
 * open) he holds two at once. The keys are plain strings (the magic's id), so this class has no game types.
 */
public final class ImitationMemory {
    /** Ticks a copied magic is remembered. */
    public static final int WINDOW = 480;
    /** What a magic he knows costs the party: a blow of it deals 70%. */
    public static final double RESONANCE = 0.70;

    private final List<String> keys = new ArrayList<>();
    private final List<Long> ticks = new ArrayList<>();
    private int capacity;

    public ImitationMemory(int capacity) { this.capacity = Math.max(1, capacity); }

    /** Changes how many magics he can hold (the Gates raise it to two); extra memories beyond the new size are forgotten, oldest first. */
    public void setCapacity(int capacity) {
        this.capacity = Math.max(1, capacity);
        while (keys.size() > this.capacity) {
            keys.remove(0);
            ticks.remove(0);
        }
    }

    public int capacity() { return capacity; }

    /** He learns {@code key} at {@code tick}: it becomes the newest, whether or not he knew it. */
    public void learn(String key, long tick) {
        if (key == null || key.isEmpty()) return;
        int at = keys.indexOf(key);
        if (at >= 0) {
            keys.remove(at);
            ticks.remove(at);
        }
        keys.add(key);
        ticks.add(tick);
        while (keys.size() > capacity) {
            keys.remove(0);
            ticks.remove(0);
        }
    }

    /** The magics he still knows at {@code now}, the newest first. */
    public List<String> known(long now) {
        List<String> out = new ArrayList<>();
        for (int i = keys.size() - 1; i >= 0; i--) if (now - ticks.get(i) < WINDOW) out.add(keys.get(i));
        return out;
    }

    /** The newest magic he still knows, or null. */
    public String current(long now) {
        List<String> k = known(now);
        return k.isEmpty() ? null : k.get(0);
    }

    public boolean knows(String key, long now) { return key != null && known(now).contains(key); }

    /** The share of a blow of {@code attackerMagic} that gets through: 70% if he knows it, the whole blow otherwise (a weapon blow has no magic: null). */
    public double factorAgainst(String attackerMagic, long now) { return knows(attackerMagic, now) ? RESONANCE : 1.0; }

    public void clear() {
        keys.clear();
        ticks.clear();
    }
}
