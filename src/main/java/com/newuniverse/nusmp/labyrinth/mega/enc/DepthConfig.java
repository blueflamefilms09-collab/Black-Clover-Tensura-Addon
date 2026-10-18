package com.newuniverse.nusmp.labyrinth.mega.enc;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the depth rule, {@code nusmp-labyrinth-mega-depth-server.toml}, section {@code [depth]}. Its own file so the rule can be retuned or switched off without touching the
 * Dungeon Master's switches: a server that finds the deep floors too hard turns {@code strengthScale} down, one that wants the old dumb mobs turns {@code smarts} off, and neither has to
 * touch the encounter tables. Plain {@code define} / {@code defineInRange} only (a list-validated value throws while the server is loading), read through {@link #get}.
 */
public final class DepthConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, STRENGTH, SMARTS, DROPS;
    public static final ModConfigSpec.DoubleValue STRENGTH_SCALE, SHARE_ALREADY_SCALED, DROP_ROLLS_MAX, DROP_COUNT_MAX, DROP_LUCK_MAX, XP_MAX, DROP_SCALE;
    public static final ModConfigSpec.ConfigValue<String> MIND_FLOORS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("depth");
        ENABLED = b.comment("The depth rule: every hostile mob born in the Mega-Dungeon gets stronger and smarter the further its floor is from the Hub (up or down, equally). false = mobs are as they were before 1.89.0.")
                .define("enabled", true);
        STRENGTH = b.comment("Stronger: health, damage, speed, armour, knockback resistance and follow range grow with the depth (saturating curves, see docs/labyrinth/depth.md).")
                .define("strength", true);
        SMARTS = b.comment("Smarter: the mob gets a mind level from the depth (0 to 6) and one more behaviour for each level: memory, alarm, flanking, regrouping, closing down kiters, hunting the weakest.")
                .define("smarts", true);
        STRENGTH_SCALE = b.comment("Multiplies every strength bonus. 1.0 = as designed, 0.5 = half as strong, 2.0 = twice.")
                .defineInRange("strengthScale", 1.0, 0.0, 5.0);
        SHARE_ALREADY_SCALED = b.comment("Mobs of the Dungeon Master and the Mob Foundry already scale with the floor by their own rules. This is the share of the depth bonus they get on top (0 = none, 1 = all).")
                .defineInRange("shareForScaledMobs", 0.5, 0.0, 1.0);
        MIND_FLOORS = b.comment("The six depths at which the mind level goes up by one, comma separated and strictly rising. A bad list falls back to 6,16,40,100,250,500.")
                .define("mindFloors", DepthRules.DEFAULT_MIND_FLOORS);
        DROPS = b.comment("Better drops: the deeper the floor, the more extra rolls of the mob's own loot table, the bigger every stack, the luckier those rolls and the more experience a kill gives (coins and equipment already scale with depth by their own rules and are not touched). Needs a player kill.")
                .define("drops", true);
        DROP_ROLLS_MAX = b.comment("The most extra rolls of the mob's loot table at the limit of depth (reached slowly: about half of it at floor 200).")
                .defineInRange("dropRollsMax", DepthDropRules.DEFAULTS.rollsMax(), 0.0, 20.0);
        DROP_COUNT_MAX = b.comment("The most that is added to every dropped stack at the limit of depth (1.0 = twice as many).")
                .defineInRange("dropCountMax", DepthDropRules.DEFAULTS.countMax(), 0.0, 10.0);
        DROP_LUCK_MAX = b.comment("The most Luck added to the extra rolls at the limit of depth.")
                .defineInRange("dropLuckMax", DepthDropRules.DEFAULTS.luckMax(), 0.0, 20.0);
        XP_MAX = b.comment("The most that is added to the experience of a kill at the limit of depth (3.0 = four times as much).")
                .defineInRange("xpMax", DepthDropRules.DEFAULTS.xpMax(), 0.0, 20.0);
        DROP_SCALE = b.comment("Multiplies all four of the drop ceilings above. 0 = the deep floors drop what the shallow ones do.")
                .defineInRange("dropScale", DepthDropRules.DEFAULTS.scale(), 0.0, 10.0);
        b.pop();
        SPEC = b.build();
    }

    private DepthConfig() {}

    /** The drop ceilings as the rules take them. */
    public static DepthDropRules.Params dropParams() {
        return new DepthDropRules.Params(get(DROP_ROLLS_MAX), get(DROP_COUNT_MAX), get(DROP_LUCK_MAX), get(XP_MAX), get(DROP_SCALE));
    }

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
