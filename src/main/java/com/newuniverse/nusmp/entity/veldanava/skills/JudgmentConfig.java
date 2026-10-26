package com.newuniverse.nusmp.entity.veldanava.skills;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config of the Veldanava ultimate skills: world/serverconfig/nusmp-veldanava-skills-server.toml. Everything here changes gameplay, so it is a SERVER setting; how the card screen
 * looks is not configurable. Each call reads the default when the config is not loaded yet (a unit test, the first tick of a server).
 */
public final class JudgmentConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, REWARD_SCREEN, EXECUTE_PLAYERS, EXECUTE_BOSSES, POCKET_DIMENSION, STRICT_GOVERNANCE, WINGS_FLIGHT, SOLOMON_TERRAIN, SOLOMON_REPLAY, SOLOMON_BYPASS;
    public static final ModConfigSpec.DoubleValue COST_SCALE, COOLDOWN_SCALE, DEBT_DAMAGE, FINAL_CAP, ACTIVE_DAMAGE_CAP;
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
        COOLDOWN_SCALE = b.comment("Multiplier of every cooldown of the Veldanava skills (1 = the cooldown in the skill description: the normal one, or the shorter one once the skill is mastered; 0 = no cooldowns).")
                .defineInRange("cooldownScale", 1.0, 0.0, 10.0);
        DEBT_DAMAGE = b.comment("Damage (HP) one stack of Temporal Debt is worth when it is detonated, before the multiplier of the skill that detonates it.")
                .defineInRange("debtDamage", 20.0, 0.0, 10000.0);
        FINAL_CAP = b.comment("The most damage one Final Judgment can deal to a mob (a player is limited by the Balance Law's per-hit cap on top).")
                .defineInRange("finalJudgmentCap", 50000.0, 1.0, 1.0E7);
        ACTIVE_DAMAGE_CAP = b.comment("The most damage one press of a Sandalphon active (Bullet, Cleave, a field pulse) can deal to a mob before the Balance Law.")
                .defineInRange("activeDamageCap", 4000.0, 1.0, 1.0E6);
        EXECUTE_PLAYERS = b.comment("Condemnation executes a PLAYER who falls below 20% health (default since 3.14.0). false = a player is dealt the damage of a hit instead (the Balance Law's no-one-shot rule).")
                .define("executePlayers", true);
        EXECUTE_BOSSES = b.comment("Condemnation executes a boss (the c:bosses tag or a very large health pool) who falls below 20% health. false = a boss takes a hit of 10% of its maximum health instead.")
                .define("executeBosses", false);
        POCKET_DIMENSION = b.comment("true = Dimensional Labyrinth (Israfil) pulls its target into a pocket dimension, an ever-shifting maze (default since 3.14.0). false = the old cage of walls round the target, as in the first 3.14.0 builds. A boss is always caged, never pulled in.")
                .define("pocketDimension", true);
        STRICT_GOVERNANCE = b.comment("true = Jibril's Strict Governance doubles the cooldowns of the skills and spells of enemy players within 30 blocks (default since 3.15.0). false = the toggle does nothing, so no player's cooldowns are ever touched by another player's skill.")
                .define("strictGovernance", true);
        WINGS_FLIGHT = b.comment("true = Jibril's Wings of Revelation give creative-style flight and a faster flight speed (default since 3.15.0). false = the wings only dodge blows in the air and are drawn, they give no flight.")
                .define("wingsFlight", true);
        DEBT_MAX = b.comment("The most Temporal Debt stacks one creature can carry.")
                .defineInRange("debtMax", 200, 1, 1000);
        FIELDS_PER_PLAYER = b.comment("The most damage fields (Verdict of Ruin) one player can keep up at once; a new one replaces the oldest.")
                .defineInRange("fieldsPerPlayer", 2, 1, 8);
        FIELDS_TOTAL = b.comment("The most damage fields of all players together on the server.")
                .defineInRange("fieldsTotal", 24, 1, 256);
        SOLOMON_TERRAIN = b.comment("true = Solomon's The 72 Pillars dig craters in the ground (a few blocks a pillar, at most 600 a cast, never in the Dungeon, never with the mobGriefing rule off; default since 3.18.0). false = the pillars only hurt, the ground is left alone.")
                .define("solomonTerrainDamage", true);
        SOLOMON_REPLAY = b.comment("true = Solomon's Grand Spell Copy and Spell Chain replay grimoire spells for free (default since 3.18.0). false = both are refused, so no spell is ever cast by anyone but its caster.")
                .define("solomonSpellReplay", true);
        SOLOMON_BYPASS = b.comment("true = Solomon's Omnipotent Magic makes the owner's magic blows ignore armour and resistances (limited by the Balance Law; default since 3.18.0). false = the toggle only gives the foresight and the presence sense.")
                .define("solomonMagicBypass", true);
        b.pop();
        SPEC = b.build();
    }

    private JudgmentConfig() {}

    public static boolean enabled() { return get(ENABLED); }

    public static boolean rewardScreen() { return get(REWARD_SCREEN); }

    public static double costScale() { return get(COST_SCALE); }

    public static double cooldownScale() { return get(COOLDOWN_SCALE); }

    public static double debtDamage() { return get(DEBT_DAMAGE); }

    public static double finalCap() { return get(FINAL_CAP); }

    public static double activeCap() { return get(ACTIVE_DAMAGE_CAP); }

    public static boolean executePlayers() { return get(EXECUTE_PLAYERS); }

    public static boolean executeBosses() { return get(EXECUTE_BOSSES); }

    public static boolean pocketDimension() { return get(POCKET_DIMENSION); }

    public static boolean strictGovernance() { return get(STRICT_GOVERNANCE); }

    public static boolean wingsFlight() { return get(WINGS_FLIGHT); }

    public static boolean solomonTerrain() { return get(SOLOMON_TERRAIN); }

    public static boolean solomonReplay() { return get(SOLOMON_REPLAY); }

    public static boolean solomonBypass() { return get(SOLOMON_BYPASS); }

    public static int debtMax() { return get(DEBT_MAX); }

    public static int fieldsPerPlayer() { return get(FIELDS_PER_PLAYER); }

    public static int fieldsTotal() { return get(FIELDS_TOTAL); }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
