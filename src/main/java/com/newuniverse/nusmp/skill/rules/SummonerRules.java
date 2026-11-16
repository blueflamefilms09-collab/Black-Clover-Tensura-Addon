package com.newuniverse.nusmp.skill.rules;

/**
 * Why: the Summoner rework (docs/summoner_rework.md) folds every skill of Allen (Hell Mode) into the one Summoner: his cards, Deletion (Recall),
 * Strengthening, Synthesis, the Bird Card's sight (the old Hawk Eye), the Spirit Card's healing (the old Spirit Card) and the stat reflection passive.
 * Which of them a player may use at which mastery, what Synthesis may make and what the passive gives are pure numbers; keeping them here, out of the
 * entity code, means the unlock order, the rank ceiling and every bound are unit-tested. Modes are append-only: the numbers never change meaning.
 */
public final class SummonerRules {
    private SummonerRules() {}

    /** Mode indexes of the Summoner. 0..4 are the old ones (Legion and Wraith belong to the Summoner of Beginnings); 5..8 are the folded-in skills. */
    public static final int BEAST = 0, STONE = 1, RECALL = 2, LEGION = 3, WRAITH = 4, BIRD = 5, SPIRIT = 6, STRENGTHEN = 7, SYNTHESIS = 8, MODES = 9;

    /** Whether a mode exists for this form at all (the base Summoner never lists the two Ultimate-only cards). */
    public static boolean listed(int mode, boolean ultimate) {
        return mode >= 0 && mode < MODES && (ultimate || (mode != LEGION && mode != WRAITH));
    }

    /**
     * Share of the mastery (0..1) at which a mode opens for a Summoner. Beast Card, Stone Card and Recall (Deletion) are open from the start, like the
     * first cards Allen can create; the rest open in the order the anime lets him grow: the Bird Card's sight, Strengthening, Synthesis, the Spirit Card.
     */
    public static double unlockAt(int mode) {
        return switch (mode) {
            case BIRD -> 0.15;
            case STRENGTHEN -> 0.30;
            case SYNTHESIS -> 0.45;
            case SPIRIT -> 0.60;
            default -> 0.0;
        };
    }

    /** Mastery share at which the stat reflection passive starts (an Ultimate has it from the start). */
    public static final double REFLECT_AT = 0.75;

    /** A mode is usable by the Ultimate always, by a Unique once its mastery share reaches {@link #unlockAt}, and by a holder carried over from the old skill (forced). */
    public static boolean unlocked(int mode, double masteryFrac, boolean ultimate, boolean forced) {
        return ultimate || forced || masteryFrac >= unlockAt(mode) - 1.0e-9;
    }

    /** Whole percent of mastery a mode needs (for the message). */
    public static int unlockPercent(int mode) { return (int) Math.round(unlockAt(mode) * 100.0); }

    public static boolean reflects(double masteryFrac, boolean ultimate) { return ultimate || masteryFrac >= REFLECT_AT - 1.0e-9; }

    // ---- Synthesis (Allen combines two cards of the same rank into one of a higher rank) ----
    /** The highest rank a synthesised summon reaches: rank 0 is a plain card. */
    public static int maxRank(Tier tier) { return tier.ultimate() ? 3 : tier == Tier.MASTERED ? 2 : 1; }

    /** Whether two summons of this rank may be combined (a rank above the ceiling never forms). */
    public static boolean canSynthesize(int rank, Tier tier) { return rank >= 0 && rank < maxRank(tier); }

    /** Health and damage factor of a rank: each rank is half as strong again, so two cards become one stronger than either but weaker than both. */
    public static double rankFactor(int rank) { return Math.pow(1.5, Math.max(0, Math.min(rank, 3))); }

    /** Magicules for one Synthesis (the anime asks a small fixed price; here a fraction of a card's). */
    public static double synthesisCost(double cardCost) { return cardCost * 0.5; }

    // ---- Strengthening (a stat boost on all of your summons) ----
    public static int strengthenSeconds(Tier tier) { return tier.pick(30, 60, 120); }
    public static int strengthenAmplifier(Tier tier) { return tier.pick(0, 0, 1); }
    public static int strengthenCooldown() { return 400; }
    public static int synthesisCooldown() { return 200; }
    public static double strengthenCost(double cardCost) { return cardCost * 0.4; }
    /** How far the boost reaches around the caster (a summon farther away is not touched, so there is no world scan). */
    public static final double STRENGTHEN_RANGE = 48.0;

    // ---- Bird Card (the old Hawk Eye) and Spirit Card, as modes ----
    /** Ticks between two pulses of the sight / of the healing (the old toggles' own intervals). */
    public static int sightInterval() { return 20; }
    public static int spiritInterval() { return AuraRules.spiritInterval(); }

    // ---- stat reflection (S2 of the anime: Allen gains part of the attributes of the summons he keeps) ----
    /** Effect levels the owner takes from the summons they keep: -1 = none. Bounded: at most one short effect per kind of card, once a second. */
    public static int reflectLevel(int alive, boolean isStrongKind) { return alive <= 0 ? -1 : (alive >= 4 && isStrongKind) ? 1 : 0; }
    public static final int REFLECT_TICKS = 60;

    // ---- mastery ----
    /**
     * Mastery points a cast earns. The old Summoner earned 1 point of the (ten thousand) it needs, so no mode could ever open; with the rework on a cast
     * earns {@code weight} times {@code percent} percent of the maximum, rounded up to at least one point (at 0.5 % a Summoner masters after about 200 casts).
     */
    public static double castGain(double maxMastery, int weight, double percent, boolean rework) {
        if (!rework) return Math.max(0, weight);
        return Math.max(1.0, maxMastery * Math.max(0.0, Math.min(percent, 5.0)) / 100.0 * Math.max(0, weight));
    }

    /** Mastery points a ten-second stretch of the Bird or Spirit Card earns (a fifth of a cast), at least one. */
    public static double pulseGain(double maxMastery, double percent, boolean rework) {
        return rework ? Math.max(1.0, maxMastery * Math.max(0.0, Math.min(percent, 5.0)) / 100.0 * 0.2) : 1.0;
    }

    // ---- the old skills folded in ----
    /** Bit flags kept in the Summoner's own tag for a holder carried over from the old Hawk Eye / Spirit Card: the mode opens at once. */
    public static final int FORCE_BIRD = 1, FORCE_SPIRIT = 2;

    public static int forceFlag(int mode) { return mode == BIRD ? FORCE_BIRD : mode == SPIRIT ? FORCE_SPIRIT : 0; }

    public static boolean forced(int flags, int mode) { int f = forceFlag(mode); return f != 0 && (flags & f) != 0; }
}
