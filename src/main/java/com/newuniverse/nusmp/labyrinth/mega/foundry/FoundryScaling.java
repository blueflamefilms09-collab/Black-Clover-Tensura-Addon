package com.newuniverse.nusmp.labyrinth.mega.foundry;

/**
 * Why: every number that makes a tier-5 foundry mob a different fight from a tier-1 one lives here as plain arithmetic with no Minecraft type (the pattern of
 * {@code GuardianScaling}), so a JUnit test can pin the rule the owner cares about: DEEPER IS HARDER, in every stat, without exception. A mob's strength is its tier (five
 * tiers of 200 floors, {@link com.newuniverse.nusmp.labyrinth.mega.guard.DndRules#tierOf}), a small growth inside the tier by how far down its 200 floors the mob stands, and
 * its archetype's multipliers. The base table grows by about two thirds per tier and the growth inside a tier is capped at 40 percent, which is less than the step to the next
 * tier, so the last floor of tier t is still weaker than the first floor of tier t+1 (the test proves this for every archetype). The Dungeon Master's own threat scaling
 * (up to +300 percent, applied by the encounter spawner to every monster it makes) comes ON TOP of these numbers, so they are deliberately modest. Health is clamped under the
 * vanilla attribute ceiling (1024); the damage and armour caps keep a deep mob a hard wall and not a one-hit kill (armour stays under the attribute's own 30).
 */
public final class FoundryScaling {
    private FoundryScaling() {}

    public static final int TIERS = 5, FLOORS_PER_TIER = 200;
    /** Base values of tiers 1 to 5: health, melee damage (hearts x 2), armour points, movement speed (blocks per tick, vanilla scale). */
    public static final double[] HEALTH = {24.0, 40.0, 66.0, 110.0, 180.0};
    public static final double[] DAMAGE = {3.0, 5.0, 8.0, 12.0, 18.0};
    public static final double[] ARMOR = {0.0, 2.0, 5.0, 9.0, 14.0};
    public static final double[] SPEED = {0.24, 0.25, 0.26, 0.27, 0.28};
    /** The most a mob grows inside its tier (health, damage) and the caps that keep the result sane. */
    public static final double HEALTH_GROWTH = 0.40, DAMAGE_GROWTH = 0.30;
    public static final double MAX_HEALTH = 1000.0, MAX_DAMAGE = 120.0, MAX_ARMOR = 28.0, MAX_SPEED = 0.42, MIN_SPEED = 0.12;
    /** The hitbox the entity type is registered with; the SCALE attribute turns it into the mob's own size. */
    public static final double TYPE_HEIGHT = 1.95;

    public static int clampTier(int tier) { return Math.max(1, Math.min(TIERS, tier)); }

    /** The tier of a signed floor number: 1..200 is tier 1 ... 801 and deeper is tier 5 (the same rule as the Guardian profiles). */
    public static int tierOf(int floor) { return Math.min(TIERS, (Math.max(1, Math.abs(floor)) - 1) / FLOORS_PER_TIER + 1); }

    /** 0 on the first floor of a tier, 1 on the last (a floor past 1000 counts as the last). */
    public static double progress(int floor) {
        int f = Math.max(1, Math.abs(floor));
        if (f > TIERS * FLOORS_PER_TIER) return 1.0;
        return ((f - 1) % FLOORS_PER_TIER) / (double) (FLOORS_PER_TIER - 1);
    }

    private static double at(double[] table, int tier) { return table[clampTier(tier) - 1]; }

    /** Maximum health: the tier's base, grown by the progress inside the tier, times the archetype's multiplier, never above {@link #MAX_HEALTH}. */
    public static double health(FoundryArchetype a, int tier, int floor) {
        return Math.min(MAX_HEALTH, at(HEALTH, tier) * (1.0 + HEALTH_GROWTH * progress(floor)) * a.health());
    }

    public static double damage(FoundryArchetype a, int tier, int floor) {
        return Math.min(MAX_DAMAGE, at(DAMAGE, tier) * (1.0 + DAMAGE_GROWTH * progress(floor)) * a.damage());
    }

    public static double armor(FoundryArchetype a, int tier) { return Math.min(MAX_ARMOR, at(ARMOR, tier) * a.armor()); }

    public static double speed(FoundryArchetype a, int tier) { return Math.max(MIN_SPEED, Math.min(MAX_SPEED, at(SPEED, tier) * a.speed())); }

    /** The SCALE attribute that makes the registered hitbox (1.95 high) the height of the model times the mob's size. */
    public static double scaleAttribute(double modelHeightBlocks, double size) {
        double h = modelHeightBlocks > 0.0 ? modelHeightBlocks : TYPE_HEIGHT;
        double s = h * (size > 0.0 ? size : 1.0) / TYPE_HEIGHT;
        return Math.max(0.4, Math.min(3.0, s));
    }

    /** How hard a mob is for the encounter budget: its health, which the budget turns into weak-monster equivalents (20 health = 1). */
    public static double toughness(FoundryArchetype a, int tier, int floor) { return health(a, tier, floor); }
}
