package com.newuniverse.nusmp;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * 0.48: the server config switches that make sense per world are gamerules too, so they can be flipped in game with /gamerule.
 * <ul>
 *   <li><b>Off-by-default features</b> (the boss, its reward, grief, World Tree trees staying, ...): on if the config <i>or</i>
 *       the gamerule is on.</li>
 *   <li><b>On-by-default rules</b> (grimoire rolls, skill evolution, Anti-Magic event-only): on only while both are on, so either
 *       one can switch it off.</li>
 *   <li><b>Numbers</b> (PvP hit cap, spell damage, cooldowns): the gamerule wins once it is changed from its default; until then
 *       the config value is used.</li>
 * </ul>
 * Registered in common setup; before that (and on a client level) the config alone decides.
 */
public final class NUGameRules {
    private NUGameRules() {}

    public static GameRules.Key<GameRules.BooleanValue> ZAGRED_BOSS, KOTODAMA_BOSS_REWARD, GRIEF_BLOCKS, WORLD_TREE_TREES_STAY, GOD_TIER_AFFECTS_PLAYERS,
            FORBIDDEN_START, ANNOUNCE_THREE_LEAF, GRIMOIRE_ROLLS, SKILL_EVOLUTION, ANTI_MAGIC_EVENT_ONLY;
    public static GameRules.Key<GameRules.IntegerValue> PVP_HIT_CAP_PERCENT, SPELL_DAMAGE_PERCENT, SPELL_COOLDOWN_PERCENT;
    static final int PVP_CAP_DEFAULT = 40, DAMAGE_DEFAULT = 100, COOLDOWN_DEFAULT = 60;

    public static void init() {
        if (ZAGRED_BOSS != null) return;
        GameRules.Category c = GameRules.Category.MISC;
        ZAGRED_BOSS = GameRules.register("nusmpZagredBoss", c, GameRules.BooleanValue.create(false));
        KOTODAMA_BOSS_REWARD = GameRules.register("nusmpKotodamaBossReward", c, GameRules.BooleanValue.create(false));
        GRIEF_BLOCKS = GameRules.register("nusmpGriefBlocks", c, GameRules.BooleanValue.create(false));
        WORLD_TREE_TREES_STAY = GameRules.register("nusmpWorldTreeTreesStay", c, GameRules.BooleanValue.create(false));
        GOD_TIER_AFFECTS_PLAYERS = GameRules.register("nusmpGodTierAffectsPlayers", c, GameRules.BooleanValue.create(false));
        FORBIDDEN_START = GameRules.register("nusmpForbiddenStart", c, GameRules.BooleanValue.create(false));
        ANNOUNCE_THREE_LEAF = GameRules.register("nusmpAnnounceThreeLeaf", c, GameRules.BooleanValue.create(false));
        GRIMOIRE_ROLLS = GameRules.register("nusmpGrimoireRolls", c, GameRules.BooleanValue.create(true));
        SKILL_EVOLUTION = GameRules.register("nusmpSkillEvolution", c, GameRules.BooleanValue.create(true));
        ANTI_MAGIC_EVENT_ONLY = GameRules.register("nusmpAntiMagicEventOnly", c, GameRules.BooleanValue.create(true));
        PVP_HIT_CAP_PERCENT = GameRules.register("nusmpPvpHitCapPercent", c, GameRules.IntegerValue.create(PVP_CAP_DEFAULT));
        SPELL_DAMAGE_PERCENT = GameRules.register("nusmpSpellDamagePercent", c, GameRules.IntegerValue.create(DAMAGE_DEFAULT));
        SPELL_COOLDOWN_PERCENT = GameRules.register("nusmpSpellCooldownPercent", c, GameRules.IntegerValue.create(COOLDOWN_DEFAULT));
    }

    private static boolean rule(Level level, GameRules.Key<GameRules.BooleanValue> key, boolean fallback) {
        if (key == null || level == null || level.isClientSide) return fallback;
        return level.getGameRules().getBoolean(key);
    }

    private static int number(Level level, GameRules.Key<GameRules.IntegerValue> key, int def, int config) {
        if (key == null || level == null || level.isClientSide) return config;
        int g = level.getGameRules().getInt(key);
        return g != def ? g : config;
    }

    // ---------------------------------------------------------------- off by default: config OR gamerule
    public static boolean zagredBoss(Level l) { return NUConfig.ZAGRED_BOSS_ENABLED.get() || rule(l, ZAGRED_BOSS, false); }
    public static boolean kotodamaBossReward(Level l) { return NUConfig.KOTODAMA_BOSS_REWARD.get() || rule(l, KOTODAMA_BOSS_REWARD, false); }
    public static boolean griefBlocks(Level l) { return NUConfig.GRIEF.get() || rule(l, GRIEF_BLOCKS, false); }
    public static boolean worldTreeTreesStay(Level l) { return NUConfig.WORLD_TREE_PERMANENT.get() || rule(l, WORLD_TREE_TREES_STAY, false); }
    public static boolean godTierAffectsPlayers(Level l) { return NUConfig.GOD_TIER_AFFECTS_PLAYERS.get() || rule(l, GOD_TIER_AFFECTS_PLAYERS, false); }
    public static boolean forbiddenStart(Level l) { return NUConfig.ALLOW_FORBIDDEN_START.get() || rule(l, FORBIDDEN_START, false); }
    public static boolean announceThreeLeaf(Level l) { return NUConfig.ANNOUNCE_THREE_LEAF.get() || rule(l, ANNOUNCE_THREE_LEAF, false); }

    // ---------------------------------------------------------------- on by default: config AND gamerule
    public static boolean grimoireRolls(Level l) { return NUConfig.GRIMOIRE_ENABLED.get() && rule(l, GRIMOIRE_ROLLS, true); }
    public static boolean skillEvolution(Level l) { return NUConfig.EVOLUTION_ENABLED.get() && rule(l, SKILL_EVOLUTION, true); }
    public static boolean antiMagicEventOnly(Level l) { return NUConfig.ANTI_MAGIC_EVENT_ONLY.get() && rule(l, ANTI_MAGIC_EVENT_ONLY, true); }

    // ---------------------------------------------------------------- numbers: the gamerule once changed, else the config
    /** The most one addon spell hit can take from a player, as a share of their max health (0..1). */
    public static double pvpHitCap(Level l) { return number(l, PVP_HIT_CAP_PERCENT, PVP_CAP_DEFAULT, NUConfig.PVP_HIT_CAP_PERCENT.get()) / 100.0; }
    /** Global addon spell damage factor. */
    public static double spellDamage(Level l) { return number(l, SPELL_DAMAGE_PERCENT, DAMAGE_DEFAULT, NUConfig.SPELL_DAMAGE_PERCENT.get()) / 100.0; }
    /** The cooldown factor for tooltips (client side, maybe before any server config is loaded). */
    public static double cooldownShown() {
        try { return NUConfig.SPELL_COOLDOWN_PERCENT.get() / 100.0; } catch (RuntimeException notLoaded) { return COOLDOWN_DEFAULT / 100.0; }
    }

    /** Global cooldown factor for grimoire pages, magic weapons and Kotodama words (0.6 = 40% shorter). */
    public static double spellCooldown(Level l) { return number(l, SPELL_COOLDOWN_PERCENT, COOLDOWN_DEFAULT, NUConfig.SPELL_COOLDOWN_PERCENT.get()) / 100.0; }
}
