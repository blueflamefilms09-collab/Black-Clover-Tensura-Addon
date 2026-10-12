package com.newuniverse.nusmp.dungeon.wild;

/**
 * The numbers and decisions of the Living Dungeon's wildlife (docs/living_dungeon.md section 1), as plain values so they are tested without
 * the game: the hunger scale, who may hunt whom, when a full apex predator sleeps and wakes, which Carcass a creature leaves, and the gates
 * that let the ecosystem bring a new creature in. The hunger scale and the birth gates are the ones the 1.18.0 creatures
 * ({@code dungeon.DungeonCreature}, {@code DungeonEcology}) used; the creatures themselves are now the real mobs of Tensura and
 * Ice and Fire. Pure.
 */
public final class WildRules {
    private WildRules() {}

    // ---------------------------------------------------------------- hunger (the scale of dungeon.DungeonCreature)
    public static final int HUNGER_MAX = 100, HUNGRY = 40, FULL = 80, START = 70;
    /** Game ticks for hunger to fall by one; a legendary creature burns slower. Starvation damage comes every STARVE_TICKS at zero. */
    public static final int DECAY_TICKS = 100, LEGEND_DECAY_TICKS = 300, STARVE_TICKS = 40;
    /** What a kill, a carcass and a veil of mycelium give back, and the magicules a meal returns to the chunk. */
    public static final int KILL_FEED = 50, CARCASS_FEED = 30, MYCELIUM_FEED = 25;
    public static final float KILL_MAGICULE = 20f, CARCASS_MAGICULE = 8f;

    // ---------------------------------------------------------------- grazing the magicules of the air (tier 1)
    public static final int GRAZE_INTERVAL = 200;
    public static final float GRAZE_MIN = 40f;

    public static int grazeGain(boolean slime) { return slime ? 18 : 10; }

    public static float grazeCost(boolean slime) { return slime ? 12f : 6f; }

    // ---------------------------------------------------------------- reach of the senses
    /** Blocks: prey search, food search (and how far above or below), the radius round its home that a creature defends, how far a player must be for it to dare to sleep. */
    public static final int PREY_RADIUS = 16, FOOD_RADIUS = 10, FOOD_HEIGHT = 3, DEN_RADIUS = 6, SLEEP_CLEAR = 10;
    /** Game ticks a woken creature stays up, and a prey that wore a hunter out is left alone. */
    public static final int WAKE_SPAN = 600, AVOID_SPAN = 600;

    public static int clampHunger(int h) { return Math.max(0, Math.min(HUNGER_MAX, h)); }

    public static boolean hungry(int h) { return h <= HUNGRY; }

    public static boolean full(int h) { return h >= FULL; }

    /** Starvation damage per interval: a flat point for small things, a fraction of the health for the big ones so they do not outlast the ecosystem. */
    public static float starveDamage(float maxHealth) { return Math.max(1f, maxHealth * 0.02f); }

    /** What a creature weighs in the chunk's biomass (0-400): a quarter of its health, between 1 and 60. */
    public static float massOf(float maxHealth) { return Math.max(1f, Math.min(60f, maxHealth / 4f)); }

    /** The weight a Carcass is left with: the dead creature's maximum health, so bigger things leave more meat. */
    public static float carcassWeight(float maxHealth) { return Math.max(0f, maxHealth); }

    /**
     * Which Carcass (CarcassBlock.KIND) an entity type leaves: 0 slime, 1 Armorsaurus (the old Living Armor), 2 Basilisk, 3 Direwolf, 4 anything else.
     * The Supermassive Slime is a slime and leaves jelly like the others.
     */
    public static int carcassKind(String id) {
        if (id == null) return 4;
        return switch (id) {
            case "tensura:slime", "tensura:metal_slime", "tensura:supermassive_slime" -> 0;
            case "tensura:armorsaurus" -> 1;
            case "tensura:basilisk" -> 2;
            case "tensura:direwolf" -> 3;
            default -> 4;
        };
    }

    // ---------------------------------------------------------------- the food web
    /**
     * May this hunter go after this prey? Tier 2 and above hunt, only something of a lower tier (a legendary hunter also takes an ordinary
     * apex predator), never a legendary creature, and never something with far more health than the hunter has.
     */
    public static boolean mayPrey(int hunterTier, boolean hunterLegend, int preyTier, boolean preyLegend, float hunterHealth, float preyHealth) {
        if (hunterTier < 2 || preyTier < 1 || preyLegend) return false;
        boolean lower = preyTier < hunterTier || (hunterLegend && preyTier <= hunterTier);
        return lower && preyHealth <= hunterHealth * 1.5f;
    }

    // ---------------------------------------------------------------- resting
    /** A full apex predator (never a legendary one) that has nothing to do, stands on the ground and has no player near, falls asleep. */
    public static boolean mayFallAsleep(boolean apex, boolean legend, int hunger, boolean hasTarget, boolean busy, boolean onGround, boolean playerNear, boolean justWoken) {
        return apex && !legend && full(hunger) && !hasTarget && !busy && onGround && !playerNear && !justWoken;
    }

    /** A sleeper wakes when it is hungry again, when it was hurt, or when somebody walks into its den. */
    public static boolean mustWake(int hunger, boolean hurt, boolean playerInDen) { return hungry(hunger) || hurt || playerInDen; }

    public static boolean hurtSince(float healthAtSleep, float healthNow) { return healthNow < healthAtSleep - 0.01f; }

    public static boolean justWoken(long now, long wokenAt) { return wokenAt != Long.MIN_VALUE && now >= wokenAt && now - wokenAt < WAKE_SPAN; }

    // ---------------------------------------------------------------- births (the gates of 1.18.0, now for the real mobs)
    public static final float LEGEND_BIOMASS = 100f, LEGEND_CHANCE = 0.004f;

    /** Slimes are born of dense magicules, as many as the air can feed. */
    public static boolean slimeBirth(float magicule, int tier1, float roll) {
        return magicule >= 150f && tier1 < Math.min(10, 2 + (int) (magicule / 100f)) && roll < 0.5f;
    }

    /** Armoured things come where slimes are plentiful and the air is fit to breathe. */
    public static boolean midBirth(int tier1, int tier2, float oxygen, float roll) {
        return tier1 >= 4 && tier2 < 3 && oxygen >= 50f && roll < 0.15f;
    }

    /** The predators come where there is something worth hunting. */
    public static boolean apexBirth(int tier2, int tier3, float oxygen, float roll) {
        return tier2 >= 2 && tier3 < 2 && oxygen >= 50f && roll < 0.08f;
    }

    /** A dragon-class creature: only where the chunk is thick with life, an apex already lives, the air is good, and very rarely. */
    public static boolean legendBirth(float biomass, int tier3, float oxygen, float roll) {
        return biomass >= LEGEND_BIOMASS && tier3 >= 1 && oxygen >= 50f && roll < LEGEND_CHANCE;
    }
}
