package com.newuniverse.nusmp.unique;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the server owner decides whether the Unique Guardian runs, how long an item must be unaccounted for before it counts as destroyed, and how long a holder may stay away before their 1-of-1 returns to the pool. These
 * change gameplay, so they are SERVER settings: {@code nusmp-unique-server.toml} (registered by {@link UniqueBoot}). With {@code enabled = false} every 1-of-1 behaves exactly as before the guardian existed.
 */
public final class UniqueConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, FLOOR_ONLY;
    /** Self-test only: forces {@link #floorOnly()} for one test run (null = the setting). */
    static volatile Boolean testFloorOnly;
    public static final ModConfigSpec.IntValue GRACE_SECONDS, ABSENCE_DAYS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("unique");
        ENABLED = b.comment("The Unique Guardian: when the only copy of a 1-of-1 (Black Sword Greed, Shishou, a Canon Grimoire, a Dragon Ball, a unique relic such as an Infinity Stone) is destroyed or deleted, it goes back into the loot pool. false = nothing is tracked or returned (the old behaviour).")
                .define("enabled", true);
        FLOOR_ONLY = b.comment("4.103.2: a 1-of-1 goes back into the pool ONLY when its dropped item is destroyed on the floor (removed from the world, lost in the void). A copy that is merely not seen in the holder's inventory, ender chest, Curios or open menu is never counted as lost, so it can sit in a Sophisticated Backpack, a storage mod, a drawer or a vault, and a holder who stays away never frees it. false = the 4.23.0 behaviour (grace clock for unaccounted copies, holder absence, destroyed containers).")
                .define("floorOnly", true);
        GRACE_SECONDS = b.comment("How long a 1-of-1 may be unaccounted for (gone from its holder's inventory, ender chest, Curios and open container, with no item entity and no other holder) before it counts as destroyed. A destroyed item entity is confirmed after five seconds regardless.")
                .defineInRange("graceSeconds", 300, 30, 86_400);
        ABSENCE_DAYS = b.comment("A holder who has not been online for this many days is gone for good: the 1-of-1 they carry returns to the pool and their copy dissolves. 0 = never.")
                .defineInRange("absenceDays", 21, 0, 3650);
        b.pop();
        SPEC = b.build();
    }

    private UniqueConfig() {}

    public static boolean enabled() {
        try { return ENABLED.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    public static boolean floorOnly() {
        Boolean t = testFloorOnly;
        if (t != null) return t;
        try { return FLOOR_ONLY.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    public static int graceSeconds() {
        try { return GRACE_SECONDS.get(); } catch (IllegalStateException notLoaded) { return 300; }
    }

    public static int absenceDays() {
        try { return ABSENCE_DAYS.get(); } catch (IllegalStateException notLoaded) { return 21; }
    }
}
