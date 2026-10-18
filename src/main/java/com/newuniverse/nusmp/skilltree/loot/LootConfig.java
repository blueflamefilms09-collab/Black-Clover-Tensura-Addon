package com.newuniverse.nusmp.skilltree.loot;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the drop rates of the Skill Books are the knob a server owner will want to turn first ("too stingy", "too generous", "no books on my server at all"), so they live in
 * their own server config file, {@code nusmp-skilltree-loot-server.toml}, section {@code [skill_books]}, registered by {@code ItemsSystem.init} like every other system's
 * file in this mod, and read through one {@link #get} helper that answers the default while the spec is not loaded yet (a loot roll or a self-test can run before the
 * config does, and a plain read of an unloaded value throws). Every value is a plain {@code define} / {@code defineInRange}, never a list-validated one: a list-validated
 * value throws while the server is still loading. The ranges are the sane ones (a chance is 0 to 1, a Guardian adds at most ten SP Books), and {@link LootRules.Chances}
 * clamps again, so a hand-edited file cannot break the rule. The names are the ones the design uses, so a note in the docs and a line in the file mean the same thing.
 */
public final class LootConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, HOLY_BOSS_DROPS;
    public static final ModConfigSpec.DoubleValue ABYSSAL_BOOK_CHANCE, ABYSSAL_SP_CHANCE, BOSS_BOOK_CHANCE;
    public static final ModConfigSpec.IntValue BOSS_SP_MIN, BOSS_SP_MAX;
    /** 1.82.0 (unique skill lines): the rift chests' books, the pity rule, and how many redraws the pity rule makes before it scans. */
    public static final ModConfigSpec.BooleanValue RIFT_BOOKS, PITY;
    public static final ModConfigSpec.DoubleValue RIFT_BOOK_SCALE;
    public static final ModConfigSpec.IntValue PITY_REROLLS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("skill_books");
        ENABLED = b.comment("Skill Books and SP Books drop from the Mega-Dungeon's Abyssal monsters and decennial Guardians. Off: nothing drops (shop and commands still work).")
                .define("enabled", true);
        ABYSSAL_BOOK_CHANCE = b.comment("Chance per kill, as a fraction (0.004 = 0.4%), that an ordinary monster of an ABYSSAL floor drops a route Skill Book.")
                .defineInRange("abyssalBookChance", 0.004, 0.0, 1.0);
        ABYSSAL_SP_CHANCE = b.comment("Chance per kill, as a fraction (0.04 = 4%), that an ordinary monster of an ABYSSAL floor drops one SP Book.")
                .defineInRange("abyssalSpChance", 0.04, 0.0, 1.0);
        BOSS_BOOK_CHANCE = b.comment("Chance, as a fraction (0.30 = 30%), that the Guardian of a decennial boss floor (floor 10, 20, 30 ...) of the Abyssal road drops a route Skill Book.")
                .defineInRange("bossBookChance", 0.30, 0.0, 1.0);
        BOSS_SP_MIN = b.comment("The fewest SP Books a decennial Guardian always drops.")
                .defineInRange("bossSpMin", 1, 0, LootRules.MAX_BOSS_SP);
        BOSS_SP_MAX = b.comment("The most SP Books a decennial Guardian always drops (at least bossSpMin).")
                .defineInRange("bossSpMax", 3, 0, LootRules.MAX_BOSS_SP);
        HOLY_BOSS_DROPS = b.comment("Also let the Guardians of the HOLY road's decennial floors drop books. The design says no: Holy floors have their own loot.")
                .define("holyBossDrops", false);
        b.pop();
        b.push("lines");
        RIFT_BOOKS = b.comment("Skill Books of any line may also appear in the chests of the dimensional rifts' pocket realms (data/nusmp/loot_modifiers/rift_books.json lists the tables and their chances).")
                .define("riftBooks", true);
        RIFT_BOOK_SCALE = b.comment("Multiplies every chance of rift_books.json (1.0 = as shipped, 0 = no rift books, 2.0 = twice as likely; a chance never goes above 1).")
                .defineInRange("riftBookScale", 1.0, 0.0, 10.0);
        PITY = b.comment("After a route book of a line the player already owns, the player's NEXT route book is always of a line they do not own (unless they own every line).")
                .define("pity", true);
        PITY_REROLLS = b.comment("How many times the pity rule redraws by weight before it scans the lines for one the player does not own.")
                .defineInRange("pityRerolls", 12, 1, 200);
        b.pop();
        SPEC = b.build();
    }

    private LootConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** The odds as the pure rule wants them. */
    public static LootRules.Chances chances() {
        return new LootRules.Chances(get(ABYSSAL_BOOK_CHANCE), get(ABYSSAL_SP_CHANCE), get(BOSS_BOOK_CHANCE), get(BOSS_SP_MIN), get(BOSS_SP_MAX), get(HOLY_BOSS_DROPS));
    }
}
