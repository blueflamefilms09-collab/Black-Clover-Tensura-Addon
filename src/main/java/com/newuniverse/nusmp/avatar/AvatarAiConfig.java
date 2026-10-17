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

    // ---- the Avatar rebuild: inventory, weapon AI, looting, loadout, evolution, party, delving (documented in docs/avatar/av4_ai.md)
    public static final ModConfigSpec.BooleanValue DROP_GEAR, AUTO_EQUIP, CONSUME_AMMO, TENSURA_BOND, WEAPON_AI, SHIELD_AI, LOOT, LOOT_SHARE, LOADOUT, EVOLUTION,
            AUTO_NAMING, DELVE;
    public static final ModConfigSpec.IntValue RANGED_MIN, RANGED_MAX, LOOT_RADIUS, LOOT_SCAN_TICKS, MERCHANT_LOOT_RADIUS, LOOT_GRACE_TICKS, EVOLUTION_CHECK_SECONDS,
            EVOLUTION_COOLDOWN_SECONDS, FESTIVAL_SECONDS, PARTY_MAX, AUTO_NAMING_COOLDOWN_SECONDS, AUTO_NAMING_RADIUS, DELVE_MINUTES, DELVE_REST_MINUTES, DELVE_MAX_CHAIN;
    public static final ModConfigSpec.DoubleValue LOADOUT_SKILL_COST, EVOLUTION_EP_FACTOR, KILL_EP_SHARE, AUTO_NAMING_MIN_MAGICULE, AUTO_NAMING_COST, PARTY_TAX_PERCENT,
            DELVE_RETURN_HEALTH, DELVE_MIN_HEALTH;
    public static final ModConfigSpec.ConfigValue<String> DELVE_ROAD;

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
        FRENZY_BURN = b.comment("MAXIMUM health the Avatar loses per frenzy cast (a temporary modifier the end of the frenzy removes). It can never kill itself: the maximum stops at 1.").defineInRange("frenzyHealthBurnPerCast", 2.0, 0.0, 100.0);
        FRENZY_SPEED = b.comment("Movement speed bonus during a frenzy (0.3 = +30%).").defineInRange("frenzySpeedBonus", 0.3, 0.0, 2.0);

        DROP_GEAR = b.comment("true = a dying Avatar drops everything it carries (hands, armour, trinkets, pockets). false = the gear goes to its creator instead.").define("dropGearOnDeath", true);
        AUTO_EQUIP = b.comment("An Avatar that picks up better armour or a better weapon of the kind it holds puts it on (the old piece goes into its pockets).").define("autoEquip", true);
        CONSUME_AMMO = b.comment("true = a bow or crossbow Avatar uses up arrows from its pockets. false = its arrows are free (it can shoot with an empty quiver).").define("consumeAmmo", false);
        TENSURA_BOND = b.comment("Keep Tensura's own owner record of the Avatar in step with its loyalty (set while it serves, cut by betrayal and awakening) so Tensura treats it as the creator's subordinate.").define("tensuraBond", true);
        WEAPON_AI = b.comment("false = every Avatar fights in melee whatever it holds (the 1.51.0 behaviour).").define("weaponAi", true);
        RANGED_MIN = b.comment("A bow Avatar backs away when its target is closer than this many blocks.").defineInRange("rangedMinRange", 6, 2, 20);
        RANGED_MAX = b.comment("A bow Avatar stops and shoots from this many blocks.").defineInRange("rangedMaxRange", 18, 6, 28);
        SHIELD_AI = b.comment("An Avatar with a shield raises it against enemies that are close.").define("shieldAi", true);

        LOOT = b.comment("false = Avatars never pick up items from the ground.").define("loot", true);
        LOOT_RADIUS = b.comment("Blocks around an Avatar in which it looks for dropped items.").defineInRange("lootRadius", 8, 2, 24);
        LOOT_SCAN_TICKS = b.comment("Ticks between two looks for dropped items (the scan is cached in between).").defineInRange("lootScanTicks", 20, 5, 200);
        MERCHANT_LOOT_RADIUS = b.comment("A Merchant Avatar goes for RARE drops (rarity RARE or better, relics, magic stones, stacks of valuables) from this far away.").defineInRange("merchantLootRadius", 24, 8, 48);
        LOOT_GRACE_TICKS = b.comment("Ticks after a player dropped an item during which no Avatar will take it (60 = 3 seconds).").defineInRange("lootPlayerGraceTicks", 60, 0, 600);
        LOOT_SHARE = b.comment("An Avatar hands its valuables to its creator when asked or when its pockets are nearly full.").define("lootShareWithCreator", true);

        LOADOUT = b.comment("false = Avatars cast only what the old grimoire / Tensura-magic code chose (no Soul Forge skills, no aptitude books).").define("loadout", true);
        LOADOUT_SKILL_COST = b.comment("Percent of its magicule pool a pressed Tensura skill costs the Avatar on top of what the skill takes itself (free in a Yandere frenzy).").defineInRange("loadoutSkillCostPercent", 8.0, 0.0, 100.0);

        EVOLUTION = b.comment("Avatars evolve into their race's next form when their EP is high enough (and by a Tensura Harvest Festival).").define("evolution", true);
        EVOLUTION_EP_FACTOR = b.comment("Share of the next race's base EP an Avatar must have before it evolves into it.").defineInRange("evolutionEpFactor", 0.6, 0.05, 5.0);
        EVOLUTION_CHECK_SECONDS = b.comment("Seconds between two looks at whether an Avatar may evolve.").defineInRange("evolutionCheckSeconds", 10, 1, 600);
        EVOLUTION_COOLDOWN_SECONDS = b.comment("Seconds an Avatar rests after an evolution before it may evolve again.").defineInRange("evolutionCooldownSeconds", 300, 0, 86400);
        FESTIVAL_SECONDS = b.comment("Seconds the Avatar's own Harvest Festival lasts before its body changes (used when Tensura's festival is not available for a mob).").defineInRange("festivalSeconds", 20, 1, 600);
        KILL_EP_SHARE = b.comment("Share of a slain monster's EP an Avatar gains for its kill (0 = Avatars only grow by naming and festivals). One kill never adds more than 2% of the Avatar's EP.").defineInRange("killEpShare", 0.04, 0.0, 1.0);

        AUTO_NAMING = b.comment("Avatars with spare magicules may name wild Tensura monsters and keep them as an NPC party.").define("autoNaming", true);
        PARTY_MAX = b.comment("How many named monsters one Avatar keeps at most.").defineInRange("partyMax", 3, 0, 8);
        AUTO_NAMING_COOLDOWN_SECONDS = b.comment("Seconds an Avatar waits between two namings.").defineInRange("autoNamingCooldownSeconds", 300, 10, 86400);
        AUTO_NAMING_MIN_MAGICULE = b.comment("The share of its maximum magicule an Avatar must hold before it may name a monster.").defineInRange("autoNamingMinMagicule", 0.8, 0.0, 1.0);
        AUTO_NAMING_COST = b.comment("The share of its maximum magicule a naming costs the Avatar.").defineInRange("autoNamingCostShare", 0.25, 0.0, 1.0);
        PARTY_TAX_PERCENT = b.comment("Percent of its maximum magicule each living party member takes from the Avatar, for as long as the member lives (at most half in all).").defineInRange("partyTaxPercent", 5.0, 0.0, 50.0);
        AUTO_NAMING_RADIUS = b.comment("Blocks around an Avatar in which it looks for a monster to name.").defineInRange("autoNamingRadius", 12, 4, 32);

        DELVE = b.comment("An Avatar whose stance is Delve may enter the Mega-Dungeon on its own for a while and come back with loot.").define("delve", true);
        DELVE_MINUTES = b.comment("The longest an Avatar stays in the dungeon on one trip.").defineInRange("delveMinutes", 10, 1, 120);
        DELVE_RETURN_HEALTH = b.comment("An Avatar turns back when its health falls below this share.").defineInRange("delveReturnHealth", 0.35, 0.05, 0.95);
        DELVE_MIN_HEALTH = b.comment("An Avatar sets out only when its health is at least this share.").defineInRange("delveMinHealth", 0.7, 0.1, 1.0);
        DELVE_MAX_CHAIN = b.comment("The deepest chain index (0 = the first Landing) an Avatar will go to, however far its creator has been.").defineInRange("delveMaxChain", 4, 0, 50);
        DELVE_REST_MINUTES = b.comment("Minutes an Avatar rests at its creator's side after a trip before it may set out again.").defineInRange("delveRestMinutes", 3, 0, 120);
        DELVE_ROAD = b.comment("The road an Avatar delves on: holy or abyssal.").define("delveRoad", "holy");
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

    public static boolean dropGearOnDeath() { return get(DROP_GEAR); }
    public static boolean autoEquip() { return get(AUTO_EQUIP); }
    public static boolean consumeAmmo() { return get(CONSUME_AMMO); }
    public static boolean tensuraBond() { return get(TENSURA_BOND); }
    public static boolean weaponAi() { return get(WEAPON_AI); }
    public static int rangedMin() { return get(RANGED_MIN); }
    public static int rangedMax() { return get(RANGED_MAX); }
    public static boolean shieldAi() { return get(SHIELD_AI); }
    public static boolean lootEnabled() { return get(LOOT); }
    public static int lootRadius() { return get(LOOT_RADIUS); }
    public static int lootScanTicks() { return get(LOOT_SCAN_TICKS); }
    public static int merchantLootRadius() { return get(MERCHANT_LOOT_RADIUS); }
    public static int lootGraceTicks() { return get(LOOT_GRACE_TICKS); }
    public static boolean lootShareWithCreator() { return get(LOOT_SHARE); }
    public static boolean loadoutEnabled() { return get(LOADOUT); }
    public static double loadoutSkillCostPercent() { return get(LOADOUT_SKILL_COST); }
    public static boolean evolutionEnabled() { return get(EVOLUTION); }
    public static double evolutionEpFactor() { return get(EVOLUTION_EP_FACTOR); }
    public static int evolutionCheckTicks() { return get(EVOLUTION_CHECK_SECONDS) * 20; }
    public static int evolutionCooldownTicks() { return get(EVOLUTION_COOLDOWN_SECONDS) * 20; }
    public static int festivalTicks() { return get(FESTIVAL_SECONDS) * 20; }
    public static double killEpShare() { return get(KILL_EP_SHARE); }
    public static boolean autoNaming() { return get(AUTO_NAMING); }
    public static int partyMax() { return get(PARTY_MAX); }
    public static int autoNamingCooldownTicks() { return get(AUTO_NAMING_COOLDOWN_SECONDS) * 20; }
    public static double autoNamingMinMagicule() { return get(AUTO_NAMING_MIN_MAGICULE); }
    public static double autoNamingCostShare() { return get(AUTO_NAMING_COST); }
    public static double partyTaxPercent() { return get(PARTY_TAX_PERCENT); }
    public static int autoNamingRadius() { return get(AUTO_NAMING_RADIUS); }
    public static boolean delveEnabled() { return get(DELVE); }
    public static int delveMinutes() { return get(DELVE_MINUTES); }
    public static double delveReturnHealth() { return get(DELVE_RETURN_HEALTH); }
    public static double delveMinHealth() { return get(DELVE_MIN_HEALTH); }
    public static int delveMaxChain() { return get(DELVE_MAX_CHAIN); }
    public static long delveRestTicks() { return get(DELVE_REST_MINUTES) * 1200L; }
    /** True for the Abyssal road; anything else (including a typo) is the Holy road. */
    public static boolean delveAbyssal() { return "abyssal".equalsIgnoreCase(get(DELVE_ROAD)); }

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
