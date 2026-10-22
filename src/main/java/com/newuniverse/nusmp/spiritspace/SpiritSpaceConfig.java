package com.newuniverse.nusmp.spiritspace;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the server owner decides whether the Spirit of Space exists ({@code magic.spiritSpace}, default true), how far its portals reach, and how many of its things may exist across the whole server at
 * once. These change gameplay, so they are SERVER settings (file nusmp-spiritspace-server.toml, registered by {@link SpiritSpaceBoot}). Every number is clamped again by
 * {@link SpiritSpaceRules} to its hard ceiling when it is read, so a hand edited file cannot lift a cap past what the doc promises.
 *
 * <p>There is deliberately NO setting for server-wide or cross-dimension travel: that kind of portal can drop a player into unloaded chunks of anywhere, so it is not built (docs/magic/spirit_space.md,
 * section 2.1).</p>
 */
public final class SpiritSpaceConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SPIRIT_SPACE, REQUIRE_SPATIAL_BOOK;
    public static final ModConfigSpec.IntValue MAX_SPIRITS, MAX_WISPS, MAX_PAIRS, WAYPOINTS, PORTAL_COOLDOWN, GRAVITY_TARGETS, CUT_PROJECTILES;
    public static final ModConfigSpec.DoubleValue PORTAL_RANGE, GRAVITY_RADIUS, CUT_RADIUS, CUT_DAMAGE, MANA_COST_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("magic");
        SPIRIT_SPACE = b.comment("The Spirit of Space: a rare spatial spirit (a tear in reality) that a Spatial Magic caster links with its own key. It marks waypoints and opens short portals, lifts nearby enemies and cuts projectiles out of the air.",
                "false = it does not exist (the key does nothing, the abilities refuse); nothing else in the mod changes.")
                .define("spiritSpace", true);
        REQUIRE_SPATIAL_BOOK = b.comment("true = only a player who owns the Spatial Magic grimoire can link the spirit (a spatial mage is a rare thing in Black Clover). false = anyone can.")
                .define("requireSpatialBook", true);
        MANA_COST_SCALE = b.comment("Multiplies the mana the three abilities cost (0 = free).").defineInRange("manaCostScale", 1.0, 0.0, 5.0);
        b.pop();
        b.push("travel");
        PORTAL_RANGE = b.comment("How far a portal may reach, in blocks (3D distance), inside the SAME dimension only. Server-wide and cross-dimension portals do not exist.")
                .defineInRange("portalRange", SpiritSpaceRules.RANGE_DEFAULT, SpiritSpaceRules.RANGE_MIN, SpiritSpaceRules.RANGE_HARD);
        WAYPOINTS = b.comment("Waypoints a player may keep.").defineInRange("waypoints", SpiritSpaceRules.WAYPOINTS_DEFAULT, 1, SpiritSpaceRules.WAYPOINTS_HARD);
        PORTAL_COOLDOWN = b.comment("Ticks before a player can open another portal (500 = 25 seconds).")
                .defineInRange("portalCooldown", SpiritSpaceRules.PORTAL_COOLDOWN_DEFAULT, SpiritSpaceRules.PORTAL_COOLDOWN_MIN, 6000);
        b.pop();
        b.push("caps");
        MAX_SPIRITS = b.comment("Most spirits linked across the whole server (one per player).").defineInRange("maxSpirits", SpiritSpaceRules.SPIRITS_DEFAULT, 1, SpiritSpaceRules.SPIRITS_HARD);
        MAX_WISPS = b.comment("Most powered wisp forms out across the whole server (one per player, a few seconds each).").defineInRange("maxWisps", SpiritSpaceRules.WISPS_DEFAULT, 1, SpiritSpaceRules.WISPS_HARD);
        MAX_PAIRS = b.comment("Most portal ring pairs open across the whole server (one pair per player, a few seconds each).").defineInRange("maxPortalPairs", SpiritSpaceRules.PAIRS_DEFAULT, 1, SpiritSpaceRules.PAIRS_HARD);
        GRAVITY_RADIUS = b.comment("Radius of the gravity pulse, in blocks (one scan per cast).").defineInRange("gravityRadius", SpiritSpaceRules.GRAVITY_RADIUS_DEFAULT, 2.0, SpiritSpaceRules.GRAVITY_RADIUS_HARD);
        GRAVITY_TARGETS = b.comment("Most enemies one gravity pulse lifts.").defineInRange("gravityTargets", SpiritSpaceRules.GRAVITY_TARGETS_DEFAULT, 1, SpiritSpaceRules.GRAVITY_TARGETS_HARD);
        CUT_RADIUS = b.comment("Radius round the owner in which the spatial cut deletes projectiles, in blocks (one scan per cast).").defineInRange("cutRadius", SpiritSpaceRules.CUT_RADIUS_DEFAULT, 2.0, SpiritSpaceRules.CUT_RADIUS_HARD);
        CUT_PROJECTILES = b.comment("Most projectiles (arrows, fireballs, spell bolts) one cut deletes.").defineInRange("cutProjectiles", SpiritSpaceRules.CUT_PROJECTILES_DEFAULT, 1, SpiritSpaceRules.CUT_PROJECTILES_HARD);
        CUT_DAMAGE = b.comment("Raw damage of the cut on its one target, before the balance law (the player hit cap still applies). The cut never removes blocks.")
                .defineInRange("cutDamage", SpiritSpaceRules.CUT_DAMAGE_DEFAULT, 0.0, SpiritSpaceRules.CUT_DAMAGE_HARD);
        b.pop();
        SPEC = b.build();
    }

    private SpiritSpaceConfig() {}

    /** Reads a value; before the config is loaded (tests, early startup) the default stands in. */
    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(SPIRIT_SPACE); }
    public static boolean requireSpatialBook() { return get(REQUIRE_SPATIAL_BOOK); }
    public static double manaCostScale() { return SpiritSpaceRules.clamp(get(MANA_COST_SCALE), 0.0, 5.0); }
    public static double portalRange() { return SpiritSpaceRules.clamp(get(PORTAL_RANGE), SpiritSpaceRules.RANGE_MIN, SpiritSpaceRules.RANGE_HARD); }
    public static int waypoints() { return SpiritSpaceRules.clamp(get(WAYPOINTS), 1, SpiritSpaceRules.WAYPOINTS_HARD); }
    public static int portalCooldown() { return SpiritSpaceRules.clamp(get(PORTAL_COOLDOWN), SpiritSpaceRules.PORTAL_COOLDOWN_MIN, 6000); }
    public static int maxSpirits() { return SpiritSpaceRules.clamp(get(MAX_SPIRITS), 1, SpiritSpaceRules.SPIRITS_HARD); }
    public static int maxWisps() { return SpiritSpaceRules.clamp(get(MAX_WISPS), 1, SpiritSpaceRules.WISPS_HARD); }
    public static int maxPairs() { return SpiritSpaceRules.clamp(get(MAX_PAIRS), 1, SpiritSpaceRules.PAIRS_HARD); }
    public static double gravityRadius() { return SpiritSpaceRules.clamp(get(GRAVITY_RADIUS), 2.0, SpiritSpaceRules.GRAVITY_RADIUS_HARD); }
    public static int gravityTargets() { return SpiritSpaceRules.clamp(get(GRAVITY_TARGETS), 1, SpiritSpaceRules.GRAVITY_TARGETS_HARD); }
    public static double cutRadius() { return SpiritSpaceRules.clamp(get(CUT_RADIUS), 2.0, SpiritSpaceRules.CUT_RADIUS_HARD); }
    public static int cutProjectiles() { return SpiritSpaceRules.clamp(get(CUT_PROJECTILES), 1, SpiritSpaceRules.CUT_PROJECTILES_HARD); }
    public static double cutDamage() { return SpiritSpaceRules.clamp(get(CUT_DAMAGE), 0.0, SpiritSpaceRules.CUT_DAMAGE_HARD); }
}
