package com.newuniverse.nusmp.arc.danmachi;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the switches of the DanMachi monsters, {@code nusmp-danmachi-server.toml}, section {@code [danmachi]}. A server owner must be able to turn the
 * Dungeon Level (the party-strength Level-Up at the Gates) off or retune it, change how loud the Howl is and how many monsters a roar may call, and switch
 * off the friends' hunt requests, without editing data files. It is its own file (not more keys in the arc config, which belongs to the arc systems) so the
 * DanMachi layer ships and is documented on its own. Every value is a plain {@code define} / {@code defineInRange} (never {@code defineInList}: a
 * list-validated value throws while the server is still loading; the party mode is a string that {@link DungeonLevel.Mode#parse} reads, with a safe
 * fallback) and every reader goes through {@link #get}, which answers the default while the spec is not loaded yet.
 */
public final class DanMachiConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue LEVEL_UP, HOWL_FEAR, QUESTS;
    public static final ModConfigSpec.DoubleValue LEVEL_BASE_EP, LEVEL_DECADES, LEVEL_DAMAGE, LEVEL_SPEED;
    public static final ModConfigSpec.ConfigValue<String> PARTY_MODE;
    public static final ModConfigSpec.IntValue LEVEL_MARGIN, LEVEL_MAX_STEPS, MAX_ADDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("danmachi");
        LEVEL_UP = b.comment("The Dungeon Level: when the Gates of a DanMachi monster open (a quarter of its health left) and the party is strong enough, the monster Levels up "
                        + "(a second form, more damage, and its leveled move). False: the Gates only enrage it, whatever the party.")
                .define("levelUp", true);
        LEVEL_BASE_EP = b.comment("Existence points (EP) at which a fighter counts as Level 1. A Level lasts levelDecades powers of ten above it: with 1000 and 0.5, Level 2 starts at about 3,162 EP, "
                        + "Level 3 at 10,000, Level 5 at 100,000, Level 7 at 1,000,000 and Level 9 at 10,000,000.")
                .defineInRange("levelBaseEp", DungeonLevel.DEFAULT_BASE_EP, 1.0, 1.0E12);
        LEVEL_DECADES = b.comment("How many powers of ten of EP make one Level (0.05 to 3).")
                .defineInRange("levelDecades", DungeonLevel.DEFAULT_DECADES, 0.05, 3.0);
        PARTY_MODE = b.comment("How the Levels of the fighters near the boss become the party's Level: mean (default), median, strongest or weakest. Anything else means mean.")
                .define("partyMode", "mean");
        LEVEL_MARGIN = b.comment("The party's Level must reach the monster's Level plus this for the monster to Level up (negative: weaker parties trigger it too).")
                .defineInRange("levelUpMargin", DungeonLevel.DEFAULT_MARGIN, -4, 9);
        LEVEL_MAX_STEPS = b.comment("Most Level-Up steps a monster can take (1 to 3 are sensible; 0 switches the Level-Up off). A step is a damage and speed bonus; the first step also gives the leveled form.")
                .defineInRange("maxLevelSteps", DungeonLevel.DEFAULT_MAX_STEPS, 0, 5);
        LEVEL_DAMAGE = b.comment("Damage bonus of a leveled monster per step (0.10 = 10%).")
                .defineInRange("damagePerStep", DungeonLevel.DEFAULT_DAMAGE_PER_STEP, 0.0, 1.0);
        LEVEL_SPEED = b.comment("Movement speed bonus of a leveled monster per step (0.03 = 3%).")
                .defineInRange("speedPerStep", DungeonLevel.DEFAULT_SPEED_PER_STEP, 0.0, 0.5);
        HOWL_FEAR = b.comment("The Howl of the Minotaur and the Black Goliath frightens (slows and weakens) fighters whose Level is below the monster's. False: the Howl only shouts.")
                .define("howlFear", true);
        MAX_ADDS = b.comment("Most monsters one boss may have called at a time (the Spartoi, the Water City, the Black Goliath's roar). 0 switches the calls off.")
                .defineInRange("maxAdds", 6, 0, AddPlan.HARD_CAP);
        QUESTS = b.comment("The friends' hunt requests (Eina, Welf, Lili, Ais) are finished by the fall of their boss. False: the requests are still offered but never finish by themselves.")
                .define("quests", true);
        b.pop();
        SPEC = b.build();
    }

    private DanMachiConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean levelUp() { return get(LEVEL_UP); }

    public static double levelBaseEp() { return get(LEVEL_BASE_EP); }

    public static double levelDecades() { return get(LEVEL_DECADES); }

    public static DungeonLevel.Mode partyMode() { return DungeonLevel.Mode.parse(get(PARTY_MODE)); }

    public static int levelMargin() { return get(LEVEL_MARGIN); }

    /** The step cap, 0 when the Level-Up is switched off. */
    public static int maxSteps() { return levelUp() ? get(LEVEL_MAX_STEPS) : 0; }

    public static double damagePerStep() { return get(LEVEL_DAMAGE); }

    public static double speedPerStep() { return get(LEVEL_SPEED); }

    public static boolean howlFear() { return get(HOWL_FEAR); }

    public static int maxAdds() { return get(MAX_ADDS); }

    public static boolean quests() { return get(QUESTS); }
}
