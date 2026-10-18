package com.newuniverse.nusmp.heroweapon.weapon.kit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the budget of a technique is stated per foe ("one foe takes at most N applications") and per cast ("at most M foes"), and a technique that sends several
 * blades, legs or pulses has to keep to both whatever the geometry does: a foe standing where two legs of a path meet must not be hit three times, and a crowd
 * must not be hit by more foes than the table says. One {@code Tally} per cast counts the applications on each foe and says whether one more is allowed. It is
 * plain Java so a test can pin the two limits.
 */
public final class Tally {
    private final int maxDistinct, maxPerTarget;
    private final Map<UUID, Integer> counts = new HashMap<>();

    /** A tally that allows {@code maxDistinct} different foes, each taking at most {@code maxPerTarget} applications. */
    public Tally(int maxDistinct, int maxPerTarget) {
        this.maxDistinct = Math.max(0, maxDistinct);
        this.maxPerTarget = Math.max(0, maxPerTarget);
    }

    /** True when one more application on {@code id} stays inside both limits. */
    public boolean allows(UUID id) {
        Integer n = counts.get(id);
        return n == null ? counts.size() < maxDistinct && maxPerTarget > 0 : n < maxPerTarget;
    }

    /** True when {@code id} has already taken an application in this cast (a repeat hit must reset the foe's hurt time to land). */
    public boolean seen(UUID id) { return counts.containsKey(id); }

    /** Records one application on {@code id}. */
    public void add(UUID id) { counts.merge(id, 1, Integer::sum); }

    public int count(UUID id) { return counts.getOrDefault(id, 0); }

    /** How many different foes have been touched. */
    public int distinct() { return counts.size(); }
}
