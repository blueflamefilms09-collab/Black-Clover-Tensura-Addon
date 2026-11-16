package com.newuniverse.nusmp.beastwill;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Why (docs/beastwill_canon_rework.md): the pure rules of the canon rework of the 27 Beast Wills, kept in one class so the unit tests, the CI self-test and the game read the same numbers. It decides which data
 * folder the game reads (the canon rework is a third folder next to the remade wills and the classic ones, so the setting {@code beastWills.canonRework} can restore the previous wills exactly), when a
 * pulse ignites its targets, how much a devouring bite heals, what the mastery milestones are and what they give, how far each rank pushes the unlocks back, and the check that no two wills share a
 * signature. Pure: no Minecraft types, nothing here touches a player or a world.
 */
public final class BeastWillCanon {
    private BeastWillCanon() {}

    /** The data folders: the remade wills (the previous behaviour), the classic wills (before the remake) and the canon rework (the new default). */
    public static final String BASE_ROOT = "beast_will", CLASSIC_ROOT = "beast_will_classic", CANON_ROOT = "beast_will_canon";

    /** The folder the game reads: the classic wills win when asked for (they restore the oldest behaviour), then the canon rework, otherwise the remade wills. */
    public static String rootFor(boolean classic, boolean canon) { return classic ? CLASSIC_ROOT : canon ? CANON_ROOT : BASE_ROOT; }

    // ------------------------------------------------------------------ mastery milestones
    /** The mastery shares (of the whole) at which a milestone is reached: a quarter, a half, three quarters, all. */
    public static final double[] MILESTONES = {0.25, 0.5, 0.75, 1.0};

    /** How many milestones the mastery has reached, 0 to 4. */
    public static int milestone(double mastery) {
        int n = 0;
        for (double m : MILESTONES) if (mastery >= m - 1.0e-6) n++;
        return n;
    }

    /** The share of every ability cooldown that remains after the milestones (1.0 with the rework off): nothing at a quarter, then 5 %, 8 % and 10 % shorter; never below 0.9. */
    public static double cooldownFactor(boolean canon, double mastery) {
        if (!canon) return 1.0;
        return switch (milestone(mastery)) {
            case 0, 1 -> 1.0;
            case 2 -> 0.95;
            case 3 -> 0.92;
            default -> 0.90;
        };
    }

    // ------------------------------------------------------------------ the abilities
    /** A devouring bite heals the caster this share of what it dealt, at most {@link #DEVOUR_MAX_HEAL} health (three hearts). */
    public static final double DEVOUR_SHARE = 0.4;
    public static final float DEVOUR_MAX_HEAL = 6.0f;

    public static float devourHeal(double dealt) { return (float) Math.min(DEVOUR_MAX_HEAL, Math.max(0.0, dealt) * DEVOUR_SHARE); }

    /** Each hop of a thunder chain keeps this share of the hit before it. */
    public static final double CHAIN_FALLOFF = 0.8;

    public static double chainDamage(double raw, int hop) { return Math.max(0, raw) * Math.pow(CHAIN_FALLOFF, Math.max(0, hop)); }

    private static final String[] FIERY = {"fire", "flame", "heat", "magma", "ember", "inferno", "blaze"};

    /** Is this affinity a fiery one? (A pulse of an earth, wind or stone beast must not set its target alight.) */
    public static boolean fiery(String affinity) {
        if (affinity == null) return false;
        String s = affinity.toLowerCase(Locale.ROOT);
        for (String f : FIERY) if (s.contains(f)) return true;
        return false;
    }

    /** Does a draconic pulse of this power set its target alight? With the rework off the old rule holds (power 3 and more always ignites); with it on only a fiery beast ignites. */
    public static boolean ignites(double power, String affinity, boolean canon) { return power >= 3 && (!canon || fiery(affinity)); }

    // ------------------------------------------------------------------ progression by rank
    /** The share of assimilation at which the first form unlocks, by rank: the higher the beast, the later (A 10 %, S 15 %, SS 20 %). */
    public static double acquireUnlock(BeastRank rank) {
        if (rank == null) return 0.10;
        return switch (rank) {
            case SS -> 0.20;
            case S -> 0.15;
            default -> 0.10;
        };
    }

    /** The mastery at which the second form unlocks, by rank (A 50 %, S 60 %, SS 70 %). */
    public static double integrateMastery(BeastRank rank) {
        if (rank == null) return 0.50;
        return switch (rank) {
            case SS -> 0.70;
            case S -> 0.60;
            default -> 0.50;
        };
    }

    /** The first problem with the progression of one will under the canon rework, or null. */
    public static String progressionProblem(BeastWillDef d) {
        if (Math.abs(d.acquire().unlockAssimilation() - acquireUnlock(d.minRank())) > 1.0e-9) return d.id() + ": the first form must unlock at " + acquireUnlock(d.minRank()) + " for rank " + d.minRank();
        if (d.integrate().unlockMastery() + 1.0e-9 < integrateMastery(d.minRank())) return d.id() + ": the second form must ask at least " + integrateMastery(d.minRank()) + " mastery for rank " + d.minRank();
        return null;
    }

    // ------------------------------------------------------------------ distinct identities
    /** The two kinds of a phase as one sorted key, e.g. {@code "draconic_pulse+world_howl"}. */
    static String pair(BeastWillDef.Phase ph) {
        TreeSet<String> k = new TreeSet<>();
        for (BeastWillDef.Ability a : ph.abilities()) k.add(a.kind());
        return String.join("+", k);
    }

    /**
     * Every way a set of wills fails the rule "no two wills with the same signature": two wills with the same pair of ability kinds in the same phase, a will whose two abilities of one phase are of one kind
     * (they would share a cooldown), and an ability id used twice. An empty list means every will is its own.
     */
    public static List<String> signatureProblems(Collection<BeastWillDef> wills) {
        List<String> out = new ArrayList<>();
        Map<String, String> acquire = new HashMap<>(), integrate = new HashMap<>(), ids = new HashMap<>();
        for (BeastWillDef d : wills) {
            for (int phase = 1; phase <= 2; phase++) {
                BeastWillDef.Phase ph = d.phase(phase);
                Set<String> kinds = new HashSet<>();
                for (BeastWillDef.Ability a : ph.abilities()) {
                    if (!kinds.add(a.kind())) out.add(d.id() + ": both abilities of phase " + phase + " are of kind " + a.kind());
                    String owner = ids.put(a.id(), d.id());
                    if (owner != null) out.add(a.id() + ": the ability id is used by " + owner + " and " + d.id());
                }
                String owner = (phase == 1 ? acquire : integrate).put(pair(ph), d.id());
                if (owner != null) out.add(d.id() + " and " + owner + " have the same " + (phase == 1 ? "Acquire" : "Integrate") + " signature " + pair(ph));
            }
        }
        return out;
    }
}
