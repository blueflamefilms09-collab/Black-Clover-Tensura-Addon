package com.newuniverse.nusmp.arc.early;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: Lotus Whomalt does not fight, he waits. In the anime he had been running a weakening spell on Luck since the moment Luck arrived; it works slowly,
 * and once enough smoke has been breathed the target loses their balance and falls. A fight cannot be "wait and see", but it can be that slow-burn made
 * visible and made learnable: every foe carries a dose of smoke from 0 to {@value #MAX}, it climbs while they stand in his clouds (and, slowly and only to {@value #SIGHT_CAP}, in his sight) and falls
 * when they keep clear, and what it does to them is a fixed ladder (weakness from 3, slowness from 6, a stumble at {@value #MAX}). The party can always read
 * the number (the action bar shows it), always lower it (leave the cloud, break his line of sight, close in and finish him), and a party that ignores it is
 * weakened without ever being hit. This pure class is the ledger and the ladder; the game glue ({@code LotusWhomaltBoss}) feeds it positions and applies
 * the potion effects.
 */
public final class DoseLedger {
    public static final int MAX = 10;
    /** The dose a foe is left with after the stumble (they are not put back to nothing: the smoke is still in them). */
    public static final int AFTER_FALL = 5;

    private final Map<UUID, Integer> dose = new HashMap<>();

    /** Adds {@code amount} (may be negative) to a foe's dose and returns the new dose, kept between 0 and {@value #MAX}. */
    public int add(UUID id, int amount) {
        int next = Math.max(0, Math.min(MAX, dose(id) + amount));
        if (next == 0) dose.remove(id);
        else dose.put(id, next);
        return next;
    }

    public int dose(UUID id) { return dose.getOrDefault(id, 0); }

    /** Sets a foe's dose outright (the stumble leaves {@value #AFTER_FALL}). */
    public int set(UUID id, int value) { return add(id, value - dose(id)); }

    public void forget(UUID id) { dose.remove(id); }

    public List<UUID> ids() { return new ArrayList<>(dose.keySet()); }

    public void clear() { dose.clear(); }

    // ---------------------------------------------------------------- the ladder
    /** Amplifier of the Weakness effect a dose gives: -1 for none (under 3), 0 from 3, 1 from 6. */
    public static int weakness(int dose) { return dose >= 6 ? 1 : dose >= 3 ? 0 : -1; }

    /** Amplifier of the Slowness effect: -1 for none (under 6), 0 from 6, 1 from 8. */
    public static int slowness(int dose) { return dose >= 8 ? 1 : dose >= 6 ? 0 : -1; }

    /** Does this dose make the foe lose their balance? */
    public static boolean falls(int dose) { return dose >= MAX; }

    /** The damage of the Bloom that spends the doses: {@code perPoint} for each point of dose, none for a dose under 3. */
    public static float bloom(int dose, float perPoint) { return dose < 3 ? 0f : dose * perPoint; }

    /** The highest dose his gaze alone can give: a foe he can only see is weakened, never slowed, and the cloud does the rest. */
    public static final int SIGHT_CAP = 5;

    /**
     * The dose one second later. In a cloud it climbs by the cloud's gain. In his sight, with nothing round them, it climbs by one on every beat of his aura but
     * never above {@value #SIGHT_CAP} (and sinks back to it on the decay beat if it was higher: a foe who has stepped out of the smoke but not out of his eye
     * is not freed at once). Out of his sight and out of the smoke it falls by one on every decay beat. Always between 0 and {@value #MAX}.
     */
    public static int step(int dose, int cloudGain, boolean inSight, boolean auraBeat, boolean decayBeat) {
        int d = Math.max(0, Math.min(MAX, dose));
        if (cloudGain > 0) return Math.min(MAX, d + cloudGain);
        if (inSight) {
            if (auraBeat && d < SIGHT_CAP) return d + 1;
            if (decayBeat && d > SIGHT_CAP) return d - 1;
            return d;
        }
        return decayBeat ? Math.max(0, d - 1) : d;
    }
}
