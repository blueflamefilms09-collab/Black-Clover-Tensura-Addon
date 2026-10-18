package com.newuniverse.nusmp.skilltree;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: a server owner must be able to retune the Skill Tree without a rebuild: switch it off, decide whether only the active class may cast, how long a class switch takes, whether players may
 * respec themselves, what a spare copy of a book is worth, how many Skill Points a player can hold and how fast purchases may come in. They live in their own file,
 * {@code nusmp-skilltree-server.toml}, section {@code [skilltree]} (the loot and the shop have theirs), so the three builders of this system never edited one spec and a server that wants only
 * the loot changes can leave this file alone. It is a SERVER config on purpose: the values decide server-side rules, and NeoForge copies a server config to a connecting client, so the
 * screen can read the same numbers it will be held to. Every value is a plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the
 * server is still loading, which is why the choice "SP or refuse" for a duplicate book is a boolean), and every reader goes through {@link #get}, which answers the default while the spec is not
 * loaded yet (a client in the main menu, a unit test, an early event).
 */
public final class TreeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, REQUIRE_ACTIVE_CLASS, RESPEC, DUPLICATE_BOOK_GIVES_SP;
    public static final ModConfigSpec.IntValue CLASS_SWITCH_SECONDS, MAX_SP, PURCHASE_RATE_LIMIT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("skilltree");
        ENABLED = b.comment("Master switch of the Skill Tree: when false nothing can be unlocked, bought or cast, and Skill Point gains are ignored. Saved trees are kept.")
                .define("enabled", true);
        REQUIRE_ACTIVE_CLASS = b.comment("Only the techniques of the ACTIVE class (the route chosen in the Skill Tree screen) can be cast. When false, every unlocked route's bought techniques can be cast.")
                .define("requireActiveClass", true);
        CLASS_SWITCH_SECONDS = b.comment("Seconds a player must wait between two class changes (the first class is free; creative players and commands ignore it).")
                .defineInRange("classSwitchSeconds", 30, 0, 3600);
        RESPEC = b.comment("Players may reset their own nodes with /multiverse tree respec [route] and get the Skill Points back. Admins can always use /multiverse tree reset.")
                .define("respec", false);
        DUPLICATE_BOOK_GIVES_SP = b.comment("A Skill Book of a route the player already has: true = it is used up and gives the route's duplicate_sp Skill Points; false = it is refused and kept.")
                .define("duplicateBookGivesSp", true);
        MAX_SP = b.comment("The most Skill Points a player can hold from gains (a total already above a lowered cap is not reduced).")
                .defineInRange("maxSp", 9999, 1, 1_000_000);
        PURCHASE_RATE_LIMIT = b.comment("The most purchase and class-change packets the server accepts from one player per second; the rest are dropped.")
                .defineInRange("purchaseRateLimit", 10, 1, 100);
        b.pop();
        SPEC = b.build();
    }

    private TreeConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try {
            return v.get();
        } catch (IllegalStateException notLoaded) {
            return v.getDefault();
        }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static boolean requireActiveClass() { return get(REQUIRE_ACTIVE_CLASS); }

    public static int classSwitchSeconds() { return get(CLASS_SWITCH_SECONDS); }

    public static boolean respec() { return get(RESPEC); }

    public static boolean duplicateBookGivesSp() { return get(DUPLICATE_BOOK_GIVES_SP); }

    public static int maxSp() { return get(MAX_SP); }

    public static int purchaseRateLimit() { return get(PURCHASE_RATE_LIMIT); }
}
