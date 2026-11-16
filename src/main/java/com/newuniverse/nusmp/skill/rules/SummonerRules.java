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

    // ======================================================================== canon (Hell Mode wiki: Summoner, Summons, named summon pages)
    // The canon pass of docs/summoner_rework.md: the wiki's Summoning level decides what opens, so a mode opens at the share of the mastery that equals the Summoning
    // level it belongs to. Modes 9.. are appended (never reordered). "design" in a comment = the mod's own scale or choice, NOT from the wiki.

    /** Appended modes: the summon types that are not cards of their own (Insect, Fish, Spirit, Dragon, Angel) and the class skills Awakening, Deputize and Storage. */
    public static final int INSECT = 9, FISH = 10, SPIRIT_CAST = 11, DRAGON = 12, AWAKENING = 13, DEPUTIZE = 14, STORAGE = 15, ANGEL = 16, MODES_CANON = 17;

    /** Summoning level 1..7 (the wiki's Summoning skill; Allen has 7 from his level 55). The mastery runs from level 1 at 0 to level 7 at 6/7 (design). */
    public static final int MAX_LEVEL = 7;
    public static int summoningLevel(double masteryFrac) { return Math.max(1, Math.min(MAX_LEVEL, 1 + (int) Math.floor(masteryFrac * MAX_LEVEL + 1.0e-9))); }

    /** Ranks of the wiki, weakest first: index 0 = H. */
    public static final String RANKS = "HGFEDCBAS";
    public static char rankLetter(int index) { return RANKS.charAt(Math.max(0, Math.min(RANKS.length() - 1, index))); }

    /**
     * The nine summon types of the wiki's Summons page: the Summoning level at which the type opens (Allen's status snapshots: Insect/Beast H at 1, Bird G at 2, Grass F
     * at 3, Stone E at 4, Fish D at 5, Spirit C at 6, Dragon B at 7), its creation mana (the wiki table; ratios only), its recipe and its role. The first rank of a type is
     * always the level minus one steps from H. Angel (Merus, rank A) is unique and its recipe is not stated.
     */
    public enum Family {
        INSECT("Insect", 1, 1, "", "debuffs on enemies"), BEAST("Beast", 1, 1, "", "attack"),
        BIRD("Bird", 2, 3, "Insect + Beast", "intelligence and information gathering"), GRASS("Grass", 3, 5, "Insect + Bird", "recovery"),
        STONE("Stone", 4, 9, "Bird + Grass", "defence and tanking"), FISH("Fish", 5, 15, "Grass + Stone", "buffs"),
        SPIRIT("Spirit", 6, 19, "Bird + Fish", "long-range attack"), DRAGON("Dragon", 7, 70, "Spirit + Stone", "area attack"),
        ANGEL("Angel", 8, 0, "(not stated)", "unique: Merus");

        public final String label, recipe, role;
        public final int level, mp;

        Family(String label, int level, int mp, String recipe, String role) { this.label = label; this.level = level; this.mp = mp; this.recipe = recipe; this.role = role; }

        /** Rank index (0 = H) of the first rank this type can be made at. */
        public int firstRank() { return level - 1; }
    }

    /** The canon name of Allen's summon of this type and rank index (the wiki's named summon pages), or null if the wiki names none. */
    public static String canonName(Family f, int rank) {
        return switch (f) {
            case INSECT -> rank == 0 ? "Denka" : null;
            case BEAST -> rank == 0 ? "Mousey" : rank == 1 ? "Moley" : rank == 7 ? "Hayate" : null;
            case BIRD -> rank == 1 ? "Chappy" : rank == 3 ? "Hawk" : rank == 7 ? "Tsubame" : null;
            case GRASS -> rank == 2 ? "Appo" : null;
            case STONE -> rank == 3 ? "Wally" : rank == 5 ? "Steely" : rank == 6 ? "Mira" : null;
            case SPIRIT -> rank == 5 ? "Maria" : rank == 6 ? "Ellie" : rank == 7 ? "Okiyosan" : null;
            case DRAGON -> rank == 6 ? "Dradra" : null;
            case ANGEL -> rank == 7 ? "Merus" : null;
        };
    }

    /**
     * What a card is called: Allen's own convention is type plus rank letter ("Beast H", "Stone E"); a stone summon takes the wiki's name (Wally, Steely, Mira) because a
     * golem fits the stone type, while a beast summon here is a wolf and keeps the letter (Mousey and Moley are a mouse and a mole). {@code rank} is the Synthesis rank (0 = plain).
     */
    public static String cardLabel(Family f, int rank, boolean useCanonNames) {
        int idx = f.firstRank() + Math.max(0, rank);
        String named = useCanonNames ? canonName(f, idx) : null;
        return named != null ? named : f.label + " " + rankLetter(idx);
    }

    /** The Summoning level a mode needs under the canon order (1 = open from the start). */
    public static int canonLevel(int mode) {
        return switch (mode) {
            case SYNTHESIS, BIRD -> Family.BIRD.level;          // Synthesis opens at Summoning level 2 (wiki); the Bird type (Hawk Eye, Hawk's own ability) opens there too
            case STRENGTHEN, SPIRIT -> Family.GRASS.level;      // Strengthening at level 3 (wiki); mode 6 (the healing pulse) is the Grass type's role, recovery
            case STONE, STORAGE -> Family.STONE.level;          // Stone type and Storage at level 4 (wiki)
            case FISH -> Family.FISH.level;                     // Fish type at level 5 (Sharing opens there too, see the doc)
            case AWAKENING, SPIRIT_CAST -> Family.SPIRIT.level; // Awakening at level 6 (wiki); the Spirit type (rank C) at 6
            case DRAGON, DEPUTIZE -> Family.DRAGON.level;       // Dragon type (rank B) at 7; Deputize after Allen's level-65 seal (his Summoning is 7 then)
            default -> 1;                                       // Beast, Insect, Recall (Deletion), and the Ultimate-only cards
        };
    }

    /** Whether the mode exists for this form (the base Summoner never lists the Ultimate-only cards: Legion, Wraith, Angel). */
    public static boolean listedCanon(int mode, boolean ultimate) {
        return mode >= 0 && mode < MODES_CANON && (ultimate || (mode != LEGION && mode != WRAITH && mode != ANGEL));
    }

    /** Share of the mastery at which a mode opens under the canon order. */
    public static double canonUnlockAt(int mode) { return (canonLevel(mode) - 1) / (double) MAX_LEVEL; }

    public static boolean unlockedCanon(int mode, double masteryFrac, boolean ultimate, boolean forced) {
        return ultimate || forced || masteryFrac >= canonUnlockAt(mode) - 1.0e-9;
    }

    public static int canonUnlockPercent(int mode) { return (int) Math.ceil(canonUnlockAt(mode) * 100.0 - 1.0e-9); }

    // ---- Strengthening, canon: stays with the summon (a card is not kept here), two stats, six levels with the wiki's steps +10 +20 +50 +100 +200 +500 ----
    public static final int[] STRENGTH_STEPS = {10, 20, 50, 100, 200, 500};
    /** Strengthening level 1..6: Summoning level minus two (it opens at 3 as level 1); the Ultimate has all six. */
    public static int strengthenLevel(double masteryFrac, boolean ultimate) { return ultimate ? STRENGTH_STEPS.length : Math.max(1, Math.min(STRENGTH_STEPS.length, summoningLevel(masteryFrac) - 2)); }
    /** Share of the summon's health and damage added (design: each wiki point is 0.2 %, so level 6 doubles the two stats). */
    public static double strengthenBonus(int level) { return STRENGTH_STEPS[Math.max(1, Math.min(STRENGTH_STEPS.length, level)) - 1] * 0.002; }

    // ---- Deputize and the command tiers (wiki: Soldier +2,500 / General +5,000 / King +10,000 = ratio 1 : 2 : 4) ----
    public static final int CMD_NONE = 0, CMD_SOLDIER = 1, CMD_GENERAL = 2, CMD_KING = 3;
    public static final int MAX_GENERALS = 3, MAX_KINGS = 1;
    /** Share of health and damage a command tier adds (design scale: 25 % for a Soldier; the 1 : 2 : 4 ratio is the wiki's). */
    public static double commandBonus(int cmd) { return cmd == CMD_SOLDIER ? 0.25 : cmd == CMD_GENERAL ? 0.5 : cmd == CMD_KING ? 1.0 : 0.0; }
    /**
     * What a press of Deputize does to the summon you look at: none/soldier -> General (three per kind, wiki); General -> King when Kingnification is open (the Ultimate;
     * one per kind, wiki), else back to none; King -> none. -1 = refused (the kind already has its three generals).
     */
    public static int nextCommand(int cmd, int generalsOfKind, int kingsOfKind, boolean kingnification) {
        if (cmd == CMD_GENERAL) return kingnification && kingsOfKind < MAX_KINGS ? CMD_KING : CMD_NONE;
        if (cmd == CMD_KING) return CMD_NONE;
        return generalsOfKind < MAX_GENERALS ? CMD_GENERAL : -1;
    }

    // ---- Awakening (wiki: 100 MP, a summon's awakened skill, one a day; with a Soldier / General / King the wait is 6 h / 3 h / 1 h) ----
    public static final int DAY_TICKS = 24000;
    public static int awakeningCooldown(int topCommand) {
        return switch (topCommand) { case CMD_SOLDIER -> DAY_TICKS / 4; case CMD_GENERAL -> DAY_TICKS / 8; case CMD_KING -> DAY_TICKS / 24; default -> DAY_TICKS; };
    }

    // ---- prices and cooldowns, in multiples of the card price (wiki MP ratios: Synthesis 5, Strengthening 10, Awakening 100) ----
    public static double canonStrengthenCost(double cardCost) { return cardCost; }
    public static double awakeningCost(double cardCost) { return cardCost * 10.0; }
    /** Creation mana of a type, softened (design: a fifth of the wiki ratio, at least one card price): Insect 1, Fish 3, Spirit 3.8, Dragon 14. */
    public static double typeCost(double cardCost, Family f) { return cardCost * Math.max(1.0, f.mp * 0.2); }
    public static int castCooldown(int configCooldown, int mode) {
        int f = switch (mode) { case INSECT -> 3; case FISH -> 6; case SPIRIT_CAST -> 4; case DRAGON -> 8; case ANGEL -> 20; default -> 1; };
        return configCooldown * f;
    }

    // ---- numbers of the type casts (all design; every one bounded by SkillTargets.MAX_FOES = 24, the Angel by 8) ----
    public static final int ANGEL_TARGETS = 8;
    public static double insectRange(Tier t) { return t.pick(10.0, 14.0, 20.0); }
    public static int insectSeconds(Tier t) { return t.pick(8, 12, 20); }
    public static double fishRange() { return 16.0; }
    public static int fishSeconds(Tier t) { return t.pick(30, 60, 120); }
    public static double spiritRange() { return 24.0; }
    public static double spiritDamage(Tier t) { return t.pick(8.0, 14.0, 24.0); }
    public static double dragonRange(Tier t) { return t.pick(8.0, 12.0, 16.0); }
    public static double dragonDamage(Tier t) { return t.pick(6.0, 10.0, 18.0); }
    public static double angelDamage(Tier t) { return t.pick(14.0, 24.0, 40.0); }
    public static int awakenSeconds(Tier t) { return t.pick(30, 45, 90); }
}
