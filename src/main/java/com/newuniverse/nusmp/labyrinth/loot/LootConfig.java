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
    /** 1.107.0: Luck-scaled loot and the backpacks (section {@code [luck]} and {@code [backpack]}). */
    public static final ModConfigSpec.BooleanValue LUCK_COUNTS, LUCK_COINS, LUCK_ARMOUR, BACKPACK_DROPS, BACKPACK_HAND, BACKPACK_PICKUP;
    public static final ModConfigSpec.DoubleValue LUCK_GAIN, LUCK_DEPTH_GAIN, LUCK_MAX_FACTOR, LUCK_COIN_GAIN, LUCK_COIN_MAX, ARMOUR_RATE, BACKPACK_RATE;
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

        b.push("luck");
        LUCK_COUNTS = b.comment("The number of items in a dungeon kill or chest follows a bell curve whose middle rises with the killer's or opener's Luck attribute and with the floor depth. It only ever ADDS to what the table gave. false = counts are exactly the table's.")
                .define("scaleCounts", true);
        LUCK_GAIN = b.comment("The middle's rise per point of Luck, as a fraction of the base count (0.06 = +6% per point).")
                .defineInRange("gainPerLuck", 0.06, 0.0, 1.0);
        LUCK_DEPTH_GAIN = b.comment("The middle's rise from the deepest floors, as a fraction of the base count (it saturates: floor 200 gives half of it).")
                .defineInRange("depthGain", 0.5, 0.0, 5.0);
        LUCK_MAX_FACTOR = b.comment("The most a count may be multiplied by, whatever the Luck. 1 = the curve does nothing.")
                .defineInRange("maxFactor", 3.0, 1.0, 10.0);
        LUCK_COINS = b.comment("Tensura coins also scale with Luck (a better amount and a better chance that a mob pays), not only with Looting.")
                .define("scaleCoins", true);
        LUCK_COIN_GAIN = b.comment("The coin amount's rise per point of Luck, as a fraction (0.05 = +5% per point).")
                .defineInRange("coinGainPerLuck", 0.05, 0.0, 1.0);
        LUCK_COIN_MAX = b.comment("The most a coin payout may be multiplied by because of Luck.")
                .defineInRange("coinMaxFactor", 2.5, 1.0, 10.0);
        LUCK_ARMOUR = b.comment("A kill or a chest of the dungeon may also leave one piece of dungeon armour with randomised stat lines; the chance and the tier rise with Luck and depth.")
                .define("armourRolls", true);
        ARMOUR_RATE = b.comment("Multiplies the chance of the extra armour roll.")
                .defineInRange("armourRate", 1.0, 0.0, 20.0);
        b.pop();

        b.push("backpack");
        BACKPACK_DROPS = b.comment("Backpacks and backpack upgrades drop from the dungeon's loot, more often with Luck and depth.")
                .define("drops", true);
        BACKPACK_RATE = b.comment("Multiplies the chance of a backpack or an upgrade dropping.")
                .defineInRange("dropRate", 1.0, 0.0, 20.0);
        BACKPACK_HAND = b.comment("Using a backpack in the air opens its inventory without placing it.")
                .define("openInHand", true);
        BACKPACK_PICKUP = b.comment("Sneaking and using a placed backpack with an empty hand picks it up with its contents (only a backpack a player placed).")
                .define("sneakPickup", true);
        b.pop();
        SPEC = b.build();
    }

    private LootConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** The item-count curve from the config. */
    public static LuckGauss.Params luckItems() { return new LuckGauss.Params(get(LUCK_GAIN), get(LUCK_DEPTH_GAIN), LuckGauss.ITEMS.sigmaRatio(), get(LUCK_MAX_FACTOR)); }

    /** The coin curve from the config. */
    public static LuckGauss.Params luckCoins() { return new LuckGauss.Params(get(LUCK_COIN_GAIN), 0.0, LuckGauss.COINS.sigmaRatio(), get(LUCK_COIN_MAX)); }

    /** The rates as the pure drop decision takes them. */
    public static KillLoot.Rates rates() {
        return new KillLoot.Rates(get(DROP_RATE), get(GEAR_RATE), get(FOOD_RATE), get(MAX_PER_KILL), get(BOSS_BONUS));
    }
}
