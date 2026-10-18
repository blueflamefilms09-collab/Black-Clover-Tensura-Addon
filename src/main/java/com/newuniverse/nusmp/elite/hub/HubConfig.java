package com.newuniverse.nusmp.elite.hub;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Hub economy (1.83.0, docs/elite/hub.md), {@code serverconfig/nusmp-elite-hub-server.toml}. Its own file, like every other system of the mod, so a server owner can
 * switch the whole Hub economy off ({@code hub.enabled}) or change only what the dungeon pays and what the shops charge without touching another file. {@code economy.currency} is the
 * switch the owner asked for: {@code tensura_coins} (default) or {@code magic_stones} (the old behaviour; the stone items, ids and saves are untouched either way). Every value is a plain
 * {@code define} / {@code defineInRange} (never {@code defineInList}, which throws while the server is still loading) and every reader goes through {@link #get}, which answers the default while the
 * spec is not loaded yet (a loot roll or a spawn can be asked before the server config is read).
 */
public final class HubConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, KEEP_STONE_DROPS, NPC_SPAWN, QUESTS_ENABLED, MAP_ENABLED, SHOP_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> CURRENCY, MAP_MODE;
    public static final ModConfigSpec.IntValue STONE_VALUE, MAX_ACTIVE_QUESTS, DAILY_COUNT, LOOT_MOB_BASE, LOOT_CHEST_BASE, LOOT_BOSS_BASE, MAP_MAX_ROOMS;
    public static final ModConfigSpec.DoubleValue LOOT_SCALE, LOOT_MOB_CHANCE, LOOT_RIFT_MULT, LOOT_TIER_GROWTH, LOOT_FLOOR_GROWTH, LOOT_LOOTING, PRICE_SCALE, REWARD_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("hub");
        ENABLED = b.comment("Master switch of the Hub economy: the Tensura-coin wallet, the coin drops, the Hub NPCs, the shop, the quests and the Dungeon Map. false = none of it does anything (the old Magic Stone drops stay as they were).")
                .define("enabled", true);
        b.pop();

        b.push("economy");
        CURRENCY = b.comment("What the Mega-Dungeon pays and what the Hub's shops and quests charge and pay: 'tensura_coins' (default; Tensura Reincarnated's bronze/silver/gold/stellar gold coins) or 'magic_stones' (the old behaviour: Magic Stones and crystals drop, trades cost Magic Stones). The Magic Stone items, ids and saves are kept either way.")
                .define("currency", "tensura_coins");
        KEEP_STONE_DROPS = b.comment("With currency 'tensura_coins': true = Magic Stones and crystals ALSO keep dropping from the dungeon's mobs (coins are added next to them). false (default) = the stone drops are replaced by coins.")
                .define("keepStoneDrops", false);
        STONE_VALUE = b.comment("With currency 'magic_stones': how many bronze coins one Magic Stone is worth, so a price written in coins can be charged in stones (default 100 = one silver coin per stone).")
                .defineInRange("stoneValue", 100, 1, 1_000_000);
        b.pop();

        b.push("coins");
        LOOT_SCALE = b.comment("Multiplier on every coin drop (mobs, chests, rift chests, bosses). 0 = no coins drop at all.").defineInRange("scale", 1.0, 0.0, 100.0);
        LOOT_MOB_CHANCE = b.comment("Chance that a dungeon mob (Holy, Abyssal or foundry) leaves coins; +5 points per Looting level.").defineInRange("mobChance", 0.55, 0.0, 1.0);
        LOOT_MOB_BASE = b.comment("Bronze coins a first-floor mob leaves (the payout grows with the floor).").defineInRange("mobBase", 6, 0, 1_000_000);
        LOOT_CHEST_BASE = b.comment("Bronze coins a first-floor dungeon chest adds.").defineInRange("chestBase", 80, 0, 10_000_000);
        LOOT_BOSS_BASE = b.comment("Bronze coins a first-tier decennial Guardian leaves (default 1500 = 15 silver).").defineInRange("bossBase", 1500, 0, 100_000_000);
        LOOT_RIFT_MULT = b.comment("A chest inside a Secret Rift pocket pays this many times a plain chest.").defineInRange("riftMultiplier", 3.0, 1.0, 100.0);
        LOOT_TIER_GROWTH = b.comment("The payout is multiplied by this per tier (a tier is 200 floors, five tiers).").defineInRange("tierGrowth", 3.0, 1.0, 10.0);
        LOOT_FLOOR_GROWTH = b.comment("Extra payout over a tier, as a fraction: the last floor of a tier pays (1 + this) times its first (always kept below the step to the next tier).").defineInRange("floorGrowth", 0.5, 0.0, 1.0);
        LOOT_LOOTING = b.comment("Extra payout per Looting level on mobs and chests, as a fraction.").defineInRange("lootingBonus", 0.10, 0.0, 1.0);
        b.pop();

        b.push("npcs");
        NPC_SPAWN = b.comment("The Quest Giver and the Merchant are placed on the plaza of Floor 0 (and put back if they are gone). false = no automatic placement; spawn them with their eggs or /summon.")
                .define("spawn", true);
        b.pop();

        b.push("shop");
        SHOP_ENABLED = b.comment("The Merchant sells (data/nusmp/elite/hub/shop.json). Every price is in coins.").define("enabled", true);
        PRICE_SCALE = b.comment("Multiplier on every shop price.").defineInRange("priceScale", 1.0, 0.0, 100.0);
        b.pop();

        b.push("quests");
        QUESTS_ENABLED = b.comment("The Quest Giver offers quests (data/nusmp/elite/hub/quests/*.json) and kills, floors and deliveries are counted.").define("enabled", true);
        MAX_ACTIVE_QUESTS = b.comment("How many quests a player can have accepted at once.").defineInRange("maxActive", 5, 1, 20);
        DAILY_COUNT = b.comment("How many daily quests the board shows each day.").defineInRange("dailyCount", 4, 0, 20);
        REWARD_SCALE = b.comment("Multiplier on every quest reward.").defineInRange("rewardScale", 1.0, 0.0, 100.0);
        b.pop();

        b.push("map");
        MAP_ENABLED = b.comment("The Dungeon Map item works (and is sold).").define("enabled", true);
        MAP_MODE = b.comment("'durable' (default): the map is never used up. 'per_floor': using the map on a floor you have not mapped yet uses one map up; opening it again on a floor you already mapped is free (so you pay once per floor). 'consumable': every use uses one map up.")
                .define("mode", "durable");
        MAP_MAX_ROOMS = b.comment("The most rooms one map shows (a safety cap for the packet).").defineInRange("maxRooms", 400, 20, 2000);
        b.pop();
        SPEC = b.build();
    }

    private HubConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** Is the Hub economy on at all? */
    public static boolean enabled() { return get(ENABLED); }

    /** The coin loot numbers from the config. */
    public static CoinLoot.Params lootParams() {
        return new CoinLoot.Params(get(LOOT_MOB_CHANCE), get(LOOT_MOB_BASE), get(LOOT_CHEST_BASE), get(LOOT_BOSS_BASE), get(LOOT_RIFT_MULT),
                get(LOOT_TIER_GROWTH), get(LOOT_FLOOR_GROWTH), get(LOOT_LOOTING), get(LOOT_SCALE));
    }
}
