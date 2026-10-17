package com.newuniverse.nusmp.labyrinth.mega.flow;

import java.util.List;

/**
 * Why: "every tenth-floor Holy boss drops a guaranteed Metal Vessel" is the owner's single exception to "Unique-or-higher relics come only from
 * Crucibles", so the question of WHICH relics can be handed out that way, and how one is chosen among those still free, has to be exact and
 * testable. This is the pure half of the answer. {@link #eligible} is the definition of a candidate in four facts the game side reads off a relic: it
 * is one of a kind (a consumable is not, and the Order's ledger could not tell whether it was alive), the catalog flagged it as a vessel, its
 * grade is high enough to carry a Djinn, and it is not one half of a pair (a pair is two stacks issued together by its own path, and no vessel is one).
 * {@link #pick} chooses one candidate by weight: the weights are the mod's own loot weights of the grades (Legend 4, God 1.5, Genesis 0.5), so
 * the reward is exactly as rare as the rest of the mod says its grades are, and the roll comes from the caller so a test can walk every
 * boundary. No Minecraft import.
 */
public final class VesselRules {
    private VesselRules() {}

    /** Why: a relic id and its chance weight together, the only two things a draw needs to know about a relic. */
    public record Candidate(String id, double weight) {}

    /** Can this relic be handed out as the decennial Metal Vessel (before asking the ledger whether it is still free)? */
    public static boolean eligible(boolean oneOfAKind, boolean flaggedVessel, boolean djinnCapableGrade, boolean pairHalf) {
        return oneOfAKind && flaggedVessel && djinnCapableGrade && !pairHalf;
    }

    /**
     * The index of the candidate a roll in [0, 1) lands on, walking the candidates in list order with their weights as widths; -1 when the pool is
     * empty or no candidate has a positive, finite weight. A candidate with a zero, negative or NaN weight is never chosen. A roll outside [0, 1)
     * is clamped, so 1.0 picks the last candidate that can be picked instead of walking off the end.
     */
    public static int pick(List<Candidate> pool, double roll) {
        if (pool == null || pool.isEmpty()) return -1;
        double total = 0.0;
        int last = -1;
        for (int i = 0; i < pool.size(); i++) {
            double w = pool.get(i).weight();
            if (w > 0.0 && !Double.isInfinite(w)) {
                total += w;
                last = i;
            }
        }
        if (last < 0) return -1;
        double r = (Double.isNaN(roll) ? 0.0 : Math.max(0.0, Math.min(1.0, roll))) * total;
        double acc = 0.0;
        for (int i = 0; i < pool.size(); i++) {
            double w = pool.get(i).weight();
            if (!(w > 0.0) || Double.isInfinite(w)) continue;
            acc += w;
            if (r < acc) return i;
        }
        return last;
    }
}
