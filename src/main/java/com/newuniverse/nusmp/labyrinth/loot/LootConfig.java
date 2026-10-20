package com.newuniverse.nusmp.labyrinth.loot;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the functional anime loot, {@code nusmp-labyrinth-loot-server.toml}, section {@code [loot]}. The drop rates are the numbers an owner retunes first
 * (a server of one friend wants more, a server of forty wants less), so there is one multiplier for everything, one each for gear and food, and the cap per kill; the
 * sale of loot to a Hub Merchant, the critical-chance lines and the drops of the dungeon's own monsters outside the dungeon each have a switch. Every value is a plain
 * {@code define} / {@code defineInRange} and every reader goes through {@link #get}, which answers the default while the spec is not loaded yet.
 */
public final class LootConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, OURS_OUTSIDE, SELL, CRITS, TRINKETS;
    public static final ModConfigSpec.DoubleValue DROP_RATE, GEAR_RATE, FOOD_RATE, BOSS_BONUS, SELL_FACTOR;
    public static final ModConfigSpec.IntValue MAX_PER_KILL;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("loot");
        ENABLED = b.comment("The global loot modifier of the dungeon: monsters leave their parts, meals, elixirs and pre-rolled gear. false = none of it drops (the items still exist and can be crafted).")
                .define("enabled", true);
        DROP_RATE = b.comment("Multiplies the chance of every drop of the modifier.")
                .defineInRange("dropRate", 1.0, 0.0, 20.0);
        GEAR_RATE = b.comment("Multiplies the chance of gear drops on top of dropRate.")
                .defineInRange("gearRate", 1.0, 0.0, 20.0);
        FOOD_RATE = b.comment("Multiplies the chance of food and elixir drops on top of dropRate.")
                .defineInRange("foodRate", 1.0, 0.0, 20.0);
        MAX_PER_KILL = b.comment("The most items one kill leaves (a boss may leave three more).")
                .defineInRange("maxDropsPerKill", 3, 0, 8);
        BOSS_BONUS = b.comment("How many times more often a boss's ordinary drop rows hit (its guaranteed rare parts, gear and tonic come in addition).")
                .defineInRange("bossBonus", 3.0, 1.0, 20.0);
        OURS_OUTSIDE = b.comment("The mod's own monsters (the Dungeon Master's, the Mob Foundry's, the bosses) also drop outside the dungeon dimensions. false = the modifier does nothing outside them.")
                .define("oursOutsideDungeon", true);
        SELL = b.comment("Sneak and use on a Hub Merchant sells the item in hand (a stack of parts, a meal, a piece of gear) for coins.")
                .define("sellToMerchant", true);
        SELL_FACTOR = b.comment("Multiplies what the Merchant pays for loot.")
                .defineInRange("sellFactor", 1.0, 0.0, 10.0);
        CRITS = b.comment("The critical chance lines of loot gear turn some blows into critical hits.")
                .define("criticalChanceLines", true);
        TRINKETS = b.comment("Rings, amulets, charms, belts and cloaks of the loot apply their stat lines while worn in a Curios slot (or held in the off hand without Curios).")
                .define("trinketsApply", true);
        b.pop();
        SPEC = b.build();
    }

    private LootConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** The rates as the pure drop decision takes them. */
    public static KillLoot.Rates rates() {
        return new KillLoot.Rates(get(DROP_RATE), get(GEAR_RATE), get(FOOD_RATE), get(MAX_PER_KILL), get(BOSS_BONUS));
    }
}
