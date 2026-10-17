package com.newuniverse.nusmp.arc.danmachi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Minotaur of the anime does not fight a party, it hunts one boy: it fixes on the weakest adventurer and runs them down. That is the Minotaur's
 * own mechanic, "the Hunt", and it needs a clear counter or it would just be a curse on the weakest player: the others can draw its horn by being louder.
 * This is the whole rule as a small pure state machine (who is the prey, when the mark is re-chosen, when another fighter steals it), so it can be tested
 * without a game and the boss class only feeds it the foes and the hits.
 *
 * <p>The rules: the prey is the foe with the lowest Dungeon Level (then the nearest, then the lowest id); the mark is kept for {@link #REPICK_TICKS} and then
 * chosen again. Any other fighter steals it for {@link #STEAL_TICKS} by landing {@link #TAUNT_HITS} hits within {@link #TAUNT_WINDOW} ticks, or one blow of
 * at least {@link #TAUNT_BIG} of the boss's health ("the louder prey"). The prey takes {@link #PREY_BONUS} times the Minotaur's damage: the weak are in danger.
 */
public final class PreyBook {
    public static final int TAUNT_HITS = 3, TAUNT_WINDOW = 60, STEAL_TICKS = 160, REPICK_TICKS = 200;
    public static final double TAUNT_BIG = 0.05;
    public static final float PREY_BONUS = 1.15f;

    /** A foe the boss could hunt: its entity id, Dungeon Level and distance from the boss. */
    public record Candidate(int id, int level, double distance) {}

    private int prey = -1;
    private long markedAt, stolenUntil;
    private final Map<Integer, long[]> hits = new HashMap<>();

    /** The weakest of the foes: lowest Level, then nearest, then lowest id; -1 when there are none. */
    public static int choose(List<Candidate> foes) {
        Candidate best = null;
        if (foes != null) {
            for (Candidate c : foes) {
                if (c == null) continue;
                if (best == null || c.level() < best.level()
                        || c.level() == best.level() && (c.distance() < best.distance() || c.distance() == best.distance() && c.id() < best.id())) best = c;
            }
        }
        return best == null ? -1 : best.id();
    }

    /** The id of the prey, or -1. */
    public int prey() { return prey; }

    public boolean hasPrey() { return prey >= 0; }

    /** The factor on the Minotaur's damage to this victim. */
    public float bonusFor(int victimId) { return victimId == prey && prey >= 0 ? PREY_BONUS : 1.0f; }

    /** The mark is stolen and still holds at {@code now}. */
    public boolean stolen(long now) { return prey >= 0 && now < stolenUntil; }

    /**
     * Once a second: keeps the prey while it is among the foes and the mark is fresh (or stolen), otherwise chooses again. Returns the prey id or -1 when
     * there is nobody to hunt (the mark is dropped).
     */
    public int update(long now, List<Candidate> foes) {
        if (foes == null || foes.isEmpty()) {
            prey = -1;
            return -1;
        }
        boolean present = false;
        for (Candidate c : foes) if (c != null && c.id() == prey) present = true;
        if (present && (now < stolenUntil || now - markedAt < REPICK_TICKS)) return prey;
        int next = choose(foes);
        if (next != prey) hits.clear();
        prey = next;
        markedAt = now;
        stolenUntil = 0;
        return prey;
    }

    /**
     * A fighter hit the boss for {@code amount} (the boss has {@code maxHealth}). Returns true when this hit stole the mark: the fighter is now the prey
     * for {@link #STEAL_TICKS}. The prey's own hits never steal anything.
     */
    public boolean onHit(long now, int attacker, float amount, float maxHealth) {
        if (attacker < 0 || attacker == prey || amount <= 0f) return false;
        long[] recent = hits.computeIfAbsent(attacker, k -> new long[TAUNT_HITS]);
        for (int i = 0; i < recent.length - 1; i++) recent[i] = recent[i + 1];
        recent[recent.length - 1] = now + 1;                       // +1 so that tick 0 is not mistaken for "no hit yet"
        boolean quick = recent[0] > 0 && now + 1 - recent[0] <= TAUNT_WINDOW;
        boolean big = maxHealth > 0f && amount >= TAUNT_BIG * maxHealth;
        if (!quick && !big) return false;
        prey = attacker;
        markedAt = now;
        stolenUntil = now + STEAL_TICKS;
        hits.clear();
        return true;
    }

    /** Forgets the hunt (a new fight). */
    public void clear() {
        prey = -1;
        markedAt = 0;
        stolenUntil = 0;
        hits.clear();
    }
}
