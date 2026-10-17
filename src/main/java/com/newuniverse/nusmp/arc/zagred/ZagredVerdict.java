package com.newuniverse.nusmp.arc.zagred;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Why: the Verdict is Zagred's multi-target mechanic: he writes a different word over several players at once and each of them has a few seconds to give
 * the right answer, so the party cannot simply stack and hit one button. The rules of it are pure numbers and a pure draw, kept apart from the world so
 * they can be tested: how many players are marked (more as the fight goes on, never more than there are fighters and never everybody at once with a big
 * party), how long they have, and which words are dealt (all different, so two players are never told the same thing and the call-outs mean something).
 */
public final class ZagredVerdict {
    private ZagredVerdict() {}

    /** The words a verdict can deal, in a fixed order (the draw is a shuffle of this list). */
    public static final List<ZagredWord> POOL = List.of(ZagredWord.HALT, ZagredWord.FALL, ZagredWord.SEAL, ZagredWord.SWORDS, ZagredWord.COWER);

    /** How many players are marked: two at most in the first act that has verdicts, three after; never more than there are fighters; none for nobody. */
    public static int targets(int fighters, int stage) {
        if (fighters <= 0) return 0;
        int cap = stage <= 2 ? 2 : 3;
        return Math.max(1, Math.min(fighters, cap));
    }

    /** Ticks a marked player has to answer: 3.5 s, then 3 s, then 2.5 s at the Gates. */
    public static int windowTicks(int stage) {
        return stage <= 2 ? 70 : stage == 3 ? 60 : 50;
    }

    /** {@code count} different words from {@link #POOL}, shuffled by {@code seed} (the same seed always deals the same words). */
    public static List<ZagredWord> assign(int count, long seed) {
        List<ZagredWord> pool = new ArrayList<>(POOL);
        Collections.shuffle(pool, new Random(seed));
        int n = Math.max(0, Math.min(count, pool.size()));
        return new ArrayList<>(pool.subList(0, n));
    }
}
