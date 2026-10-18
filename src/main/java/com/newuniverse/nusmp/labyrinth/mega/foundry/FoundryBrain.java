package com.newuniverse.nusmp.labyrinth.mega.foundry;

import java.util.List;
import java.util.Random;

/**
 * Why: WHICH ability a mob uses next is the part of its behaviour a player feels most, and it must be a decision that is the same in a test and in the game, so it is a small
 * pure object: it remembers when each ability of the mob may be used again, and given a {@link FoundryAbility.Situation} it picks the first ability (starting from a random one in
 * the list, so a mob with two abilities does not always open with the same) that is off cooldown and fits the situation. It rolls at most once per {@code every} ticks (the
 * archetype's rhythm), and never while a cast is in progress. The game side ({@code FoundryAbilityGoal}) asks it each tick, then performs the cast and the effect.
 * No Minecraft type, no clock of its own: the caller passes the tick.
 */
public final class FoundryBrain {
    private final List<FoundryAbility> abilities;
    private final long[] readyAt;
    private final int every;
    private long nextRoll;

    public FoundryBrain(List<FoundryAbility> abilities, int every) {
        this.abilities = List.copyOf(abilities);
        this.readyAt = new long[abilities.size()];
        this.every = Math.max(1, every);
    }

    public List<FoundryAbility> abilities() { return abilities; }

    /** Is a roll due? (The AI checks this first so it builds the situation, which asks the world questions, only when the answer can matter.) */
    public boolean due(long now) { return now >= nextRoll; }

    /** Is the ability off cooldown at this tick? */
    public boolean ready(int index, long now) { return index >= 0 && index < readyAt.length && now >= readyAt[index]; }

    /** Ticks left of an ability's cooldown (0 when ready). */
    public long cooldownLeft(int index, long now) { return index < 0 || index >= readyAt.length ? 0 : Math.max(0L, readyAt[index] - now); }

    /** Starts the cooldown of an ability used at {@code now}. */
    public void used(int index, long now) {
        if (index >= 0 && index < readyAt.length) readyAt[index] = now + abilities.get(index).cd();
    }

    /**
     * The index of the ability to cast now, or -1. At most one roll per {@code every} ticks; {@code casting} (a cast is under way) always answers -1. The passive abilities
     * (death burst) are never chosen. The search starts at a random ability so a mob does not repeat its opening move.
     */
    public int choose(long now, boolean casting, FoundryAbility.Situation s, Random rnd) {
        if (casting || abilities.isEmpty() || now < nextRoll) return -1;
        nextRoll = now + every;
        int n = abilities.size();
        int start = n == 1 ? 0 : rnd.nextInt(n);
        for (int k = 0; k < n; k++) {
            int i = (start + k) % n;
            FoundryAbility a = abilities.get(i);
            if (a.passive() || !ready(i, now)) continue;
            if (a.fits(s)) return i;
        }
        return -1;
    }

    /** The first passive ability (the death burst), or -1. */
    public int passive() {
        for (int i = 0; i < abilities.size(); i++) if (abilities.get(i).passive()) return i;
        return -1;
    }

    /** Forgets every cooldown (a respawn, a new identity). */
    public void reset(long now) {
        java.util.Arrays.fill(readyAt, 0L);
        nextRoll = now;
    }
}
