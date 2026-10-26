package com.newuniverse.nusmp.entity.veldanava.skills;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config of the Veldanava ultimate skills: world/serverconfig/nusmp-veldanava-skills-server.toml. Everything here changes gameplay, so it is a SERVER setting; how the card screen
 * looks is not configurable. Each call reads the default when the config is not loaded yet (a unit test, the first tick of a server).
 */
public final class JudgmentConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, REWARD_SCREEN, EXECUTE_PLAYERS, EXECUTE_BOSSES;
    public static final ModConfigSpec.DoubleValue COST_SCALE, DEBT_DAMAGE, FINAL_CAP, ACTIVE_DAMAGE_CAP;
    public static final ModConfigSpec.IntValue DEBT_MAX, FIELDS_PER_PLAYER, FIELDS_TOTAL;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("veldanava_skills");
        ENABLED = b.comment("false = the Veldanava ultimate skills (Sandalphon, ...) do nothing when used, and the Judgment effects are harmless. The skill itself stays learned.")
                .define("enabled", true);
        REWARD_SCREEN = b.comment("true = the Will of the Creator offers the five ultimate skills on a card screen (one skill, once in a player's life). false = it offers the three attribute boons of 3.11 in chat, as before.")
                .define("rewardScreen", true);
        COST_SCALE = b.comment("Multiplier of every Magicule cost of the Veldanava skills (1 = the cost in the skill description, 0 = free).")
                .defineInRange("costScale", 1.0, 0.0, 10.0);
        DEBT_DAMAGE = b.comment("Damage (HP) one stack of Temporal Debt is worth when it is detonated, before the multiplier of the skill that detonates it.")
                .defineInRange("debtDamage", 20.0, 0.0, 10000.0);
        FINAL_CAP = b.comment("The most damage one Final Judgment can deal to a mob (a player is limited by the Balance Law's per-hit cap on top).")
                .defineInRange("finalJudgmentCap", 50000.0, 1.0, 1.0E7);
        ACTIVE_DAMAGE_CAP = b.comment("The most damage one press of a Sandalphon active (Bullet, Cleave, a field pulse) can deal to a mob before the Balance Law.")
                .defineInRange("activeDamageCap", 4000.0, 1.0, 1.0E6);
        EXECUTE_PLAYERS = b.comment("Condemnation executes a PLAYER who falls below 20% health. false = a player is dealt the damage of a hit instead (the Balance Law's no-one-shot rule).")
                .define("executePlayers", false);
        EXECUTE_BOSSES = b.comment("Condemnation executes a boss (the c:bosses tag or a very large health pool) who falls below 20% health. false = a boss takes a hit of 10% of its maximum health instead.")
                .define("executeBosses", false);
        DEBT_MAX = b.comment("The most Temporal Debt stacks one creature can carry.")
                .defineInRange("debtMax", 200, 1, 1000);
        FIELDS_PER_PLAYER = b.comment("The most damage fields (Verdict of Ruin) one player can keep up at once; a new one replaces the oldest.")
                .defineInRange("fieldsPerPlayer", 2, 1, 8);
        FIELDS_TOTAL = b.comment("The most damage fields of all players together on the server.")
                .defineInRange("fieldsTotal", 24, 1, 256);
        b.pop();
        SPEC = b.build();
    }

    private JudgmentConfig() {}

    public static boolean enabled() { return get(ENABLED); }

    public static boolean rewardScreen() { return get(REWARD_SCREEN); }

    public static double costScale() { return get(COST_SCALE); }

    public static double debtDamage() { return get(DEBT_DAMAGE); }

    public static double finalCap() { return get(FINAL_CAP); }

    public static double activeCap() { return get(ACTIVE_DAMAGE_CAP); }

    public static boolean executePlayers() { return get(EXECUTE_PLAYERS); }

    public static boolean executeBosses() { return get(EXECUTE_BOSSES); }

    public static int debtMax() { return get(DEBT_MAX); }

    public static int fieldsPerPlayer() { return get(FIELDS_PER_PLAYER); }

    public static int fieldsTotal() { return get(FIELDS_TOTAL); }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
