package com.newuniverse.nusmp.avatar;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: every number the Avatar's personality, chatter, casting and Yandere code reads must be tunable (and switchable off) without a rebuild, but
 * AvatarConfig belongs to the creation system, so these knobs live in their own spec file. The lead registers it with
 * {@code modContainer.registerConfig(ModConfig.Type.SERVER, AvatarAiConfig.SPEC, "nusmp-avatar-ai-server.toml")}. Plain define/defineInRange only;
 * every read falls back to the default before the config has loaded (early ticks, unit tests).
 */
public final class AvatarAiConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CHATTER_ENABLED, YANDERE_LETHAL, CAST_ENABLED, YANDERE_JEALOUSY;
    public static final ModConfigSpec.IntValue CHATTER_RADIUS, CD_COMBAT_START, CD_LOW_HP, CD_SKILL_CAST, CD_IDLE, CD_FRENZY, CHATTER_BUDGET,
            FRENZY_SECONDS, EXHAUST_SECONDS, JEALOUS_RADIUS, CAST_GAP_TICKS;
    public static final ModConfigSpec.DoubleValue FRENZY_BURN, INTERCEPT_SHARE, FRENZY_SPEED;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("avatar");
        CHATTER_ENABLED = b.comment("false = Avatars never speak in chat.").define("chatterEnabled", true);
        CHATTER_RADIUS = b.comment("Blocks around an Avatar in which players hear it.").defineInRange("chatterRadius", 30, 4, 128);
        CD_COMBAT_START = b.comment("Seconds between 'combat start' lines of one Avatar.").defineInRange("chatterCooldownCombatStart", 20, 0, 600);
        CD_LOW_HP = b.comment("Seconds between 'creator is hurt' lines of one Avatar.").defineInRange("chatterCooldownCreatorLowHp", 15, 0, 600);
        CD_SKILL_CAST = b.comment("Seconds between 'skill cast' shouts of one Avatar.").defineInRange("chatterCooldownSkillCast", 4, 0, 600);
        CD_IDLE = b.comment("Seconds between idle remarks of one Avatar.").defineInRange("chatterCooldownIdle", 90, 0, 3600);
        CD_FRENZY = b.comment("Seconds between frenzy barks of one Avatar.").defineInRange("chatterCooldownFrenzy", 6, 0, 600);
        CHATTER_BUDGET = b.comment("At most this many Avatar lines are sent per server tick, so a swarm of Avatars cannot flood the chat.").defineInRange("chatterBudgetPerTick", 3, 1, 64);
        CAST_ENABLED = b.comment("false = Avatars never cast grimoire pages (they still fight with their weapon).").define("castEnabled", true);
        CAST_GAP_TICKS = b.comment("Least ticks between two casts of one Avatar (before intelligence / wisdom and frenzy scaling).").defineInRange("castGapTicks", 60, 5, 1200);
        YANDERE_JEALOUSY = b.comment("false = Yandere Avatars do not warn or body-block people who come close to their creator.").define("yandereJealousy", true);
        YANDERE_LETHAL = b.comment("false = a Yandere Avatar can never take a player below 1 health. true = its hits can kill players.").define("yandereLethal", false);
        JEALOUS_RADIUS = b.comment("Blocks around the creator that make a Yandere Avatar jealous.").defineInRange("yandereJealousRadius", 5, 2, 16);
        INTERCEPT_SHARE = b.comment("Share of a hit on the creator that a Yandere Avatar takes on itself (0 = never intercepts).").defineInRange("yandereInterceptShare", 0.7, 0.0, 1.0);
        FRENZY_SECONDS = b.comment("How long a Yandere frenzy lasts after the creator was hurt (refreshes while the creator keeps being hurt).").defineInRange("yandereFrenzySeconds", 15, 1, 120);
        EXHAUST_SECONDS = b.comment("Slowness after the frenzy ends, so it is never permanent.").defineInRange("yandereExhaustionSeconds", 6, 0, 60);
        FRENZY_BURN = b.comment("Health the Avatar burns per frenzy cast. It can never kill itself: it stops at 1 health.").defineInRange("frenzyHealthBurnPerCast", 2.0, 0.0, 100.0);
        FRENZY_SPEED = b.comment("Movement speed bonus during a frenzy (0.3 = +30%).").defineInRange("frenzySpeedBonus", 0.3, 0.0, 2.0);
        b.pop();
        SPEC = b.build();
    }

    private AvatarAiConfig() {}

    public static boolean chatterEnabled() { return get(CHATTER_ENABLED); }
    public static int chatterRadius() { return get(CHATTER_RADIUS); }
    public static int chatterBudgetPerTick() { return get(CHATTER_BUDGET); }
    public static boolean castEnabled() { return get(CAST_ENABLED); }
    public static int castGapTicks() { return get(CAST_GAP_TICKS); }
    public static boolean yandereJealousy() { return get(YANDERE_JEALOUSY); }
    public static boolean yandereLethal() { return get(YANDERE_LETHAL); }
    public static int jealousRadius() { return get(JEALOUS_RADIUS); }
    public static double interceptShare() { return get(INTERCEPT_SHARE); }
    public static int frenzyTicks() { return get(FRENZY_SECONDS) * 20; }
    public static int exhaustionTicks() { return get(EXHAUST_SECONDS) * 20; }
    public static float frenzyBurn() { return get(FRENZY_BURN).floatValue(); }
    public static double frenzySpeedBonus() { return get(FRENZY_SPEED); }

    /** Chatter cooldown of one event in ticks (events without a knob never wait). */
    public static int chatterCooldownTicks(ChatEvent e) {
        return switch (e) {
            case COMBAT_START -> get(CD_COMBAT_START) * 20;
            case CREATOR_LOW_HP -> get(CD_LOW_HP) * 20;
            case SKILL_CAST -> get(CD_SKILL_CAST) * 20;
            case IDLE -> get(CD_IDLE) * 20;
            case FRENZY -> get(CD_FRENZY) * 20;
            default -> 0;
        };
    }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
