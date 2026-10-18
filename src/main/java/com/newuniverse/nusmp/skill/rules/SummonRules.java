package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Summoner (Allen, Hell Mode) and its Ultimate Lord of Summons decide how many summons may stand, how strong each card is, how long it
 * lives and what a cast costs. Those are pure numbers; keeping them out of the entity code means the cap, the Ultimate being stronger than
 * the base, and the lifetime that cleans summons up are all unit-tested. Cards are append-only so a stored mode index never changes meaning.
 */
public final class SummonRules {
    private SummonRules() {}

    /** Append-only: BEAST and STONE are the Unique's cards; LEGION and WRAITH exist only in Lord of Summons. */
    public enum Card {
        BEAST, STONE, LEGION, WRAITH;
        public boolean ultimateOnly() { return this == LEGION || this == WRAITH; }
    }

    /** Hard ceiling on one player's summons, whatever the config says (a server cannot be flooded by a config typo). */
    public static final int HARD_CAP = 64;
    /** A summon farther than this from its owner is called back; farther than {@link #TELEPORT_DISTANCE} it is teleported. */
    public static final double LEASH_DISTANCE = 12.0, TELEPORT_DISTANCE = 28.0, ENGAGE_RANGE = 16.0;

    /** Summons the player may have alive at once. The Ultimate doubles the mastered limit. */
    public static int maxSummons(int configBase, int configMastered, Tier tier) {
        int v = switch (tier) {
            case BASE -> configBase;
            case MASTERED -> Math.max(configBase, configMastered);
            case ULTIMATE -> Math.max(configBase, configMastered) * 2;
        };
        return SkillMath.clamp(v, 1, HARD_CAP);
    }

    /** How many creatures one cast of the card calls. */
    public static int mobCount(Card card, Tier tier) {
        return switch (card) {
            case BEAST -> tier == Tier.ULTIMATE ? 3 : 1;
            case STONE -> 1;
            case LEGION -> 5;
            case WRAITH -> 4;
        };
    }

    /** Whether the tier may use the card at all. */
    public static boolean available(Card card, Tier tier) { return !card.ultimateOnly() || tier == Tier.ULTIMATE; }

    public static double health(Card card, Tier tier) {
        return switch (card) {
            case BEAST -> tier.pick(30, 60, 120);
            case STONE -> tier.pick(100, 200, 400);
            case LEGION -> 70;
            case WRAITH -> 24;
        };
    }

    public static double damage(Card card, Tier tier) {
        return switch (card) {
            case BEAST -> tier.pick(5, 10, 20);
            case STONE -> tier.pick(15, 20, 40);
            case LEGION -> 12;
            case WRAITH -> 8;
        };
    }

    /** Body scale (1.0 = normal): the Ultimate's Stone Card is a colossus. */
    public static double scale(Card card, Tier tier) {
        return card == Card.STONE && tier == Tier.ULTIMATE ? 1.35 : 1.0;
    }

    /** How long a summon stays before it fades (ticks). Nothing summoned lives forever, so none is left behind. */
    public static int lifetimeTicks(Card card, Tier tier) {
        int base = tier.pick(12000, 24000, 48000);
        return card == Card.WRAITH ? Math.min(base, 2400) : base;
    }

    /** Slots a cast of the card takes from the cap (one per creature). */
    public static int slots(Card card, Tier tier) { return mobCount(card, tier); }

    /** How many of {@code want} creatures fit under the cap right now. */
    public static int allowed(int alive, int max, int want) {
        return SkillMath.clamp(max - Math.max(0, alive), 0, Math.max(0, want));
    }

    /** Magicule cost of a cast: the configured per-summon cost, discounted for the multi-creature cards. */
    public static double cost(Card card, double configCost) {
        return switch (card) {
            case BEAST, STONE -> configCost;
            case LEGION -> configCost * 3.0;
            case WRAITH -> configCost * 2.0;
        };
    }

    /** Cooldown of a cast in ticks. */
    public static int cooldown(Card card, int configCooldown) {
        return switch (card) {
            case BEAST, STONE -> configCooldown;
            case LEGION -> configCooldown * 4;
            case WRAITH -> configCooldown * 3;
        };
    }

    /** Mastery points a cast earns (more for bigger casts). */
    public static int masteryGain(Card card) { return card == Card.LEGION ? 3 : 1; }

    /** True when the summon must go: its time is up, or its owner is gone. */
    public static boolean shouldVanish(long now, long until, boolean ownerOnline, boolean ownerAlive) {
        return !ownerOnline || !ownerAlive || (until > 0 && now >= until);
    }

    /** How many of the owner's summons are over the cap and must be removed (oldest first). */
    public static int excess(int alive, int max) { return Math.max(0, alive - Math.max(1, max)); }

    /** Where a summon stands relative to its owner: 0 = fine, 1 = walk back, 2 = teleport. */
    public static int leashAction(double distance) {
        if (distance > TELEPORT_DISTANCE) return 2;
        return distance > LEASH_DISTANCE ? 1 : 0;
    }
}
