package com.newuniverse.nusmp;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/** Server config: world/serverconfig/nusmp-server.toml */
public final class NUConfig {
    public static final ModConfigSpec SPEC;

    // Summoner
    public static final ModConfigSpec.IntValue SUMMON_MAX;
    public static final ModConfigSpec.IntValue SUMMON_MAX_MASTERED;
    public static final ModConfigSpec.DoubleValue SUMMON_MAGICULE_COST;
    public static final ModConfigSpec.IntValue SUMMON_COOLDOWN;

    // Grimoire look (0.20)
    public static final ModConfigSpec.BooleanValue GRIMOIRE_UNIQUE_LOOKS;

    // Faith
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BESTOWABLE_SKILLS;
    public static final ModConfigSpec.IntValue BESTOW_FAITH_COST;
    public static final ModConfigSpec.IntValue BESTOW_COOLDOWN;
    public static final ModConfigSpec.DoubleValue PRAY_MAGICULE_COST;
    public static final ModConfigSpec.IntValue PRAY_COOLDOWN;

    // DanMachi
    public static final ModConfigSpec.DoubleValue FIREBOLT_DAMAGE;
    public static final ModConfigSpec.DoubleValue FIREBOLT_MAGICULE_COST;
    public static final ModConfigSpec.IntValue FIREBOLT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue ARGONAUT_DAMAGE_PER_CHARGE;
    public static final ModConfigSpec.DoubleValue ARGONAUT_MAGICULE_PER_CHARGE;
    public static final ModConfigSpec.IntValue ARGONAUT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue LIARIS_CHANCE;
    public static final ModConfigSpec.IntValue LIARIS_MASTERY_PER_KILL;
    public static final ModConfigSpec.DoubleValue AIRIEL_MAGICULE_COST;
    public static final ModConfigSpec.IntValue AIRIEL_COOLDOWN;

    // DanMachi gods & magics
    public static final ModConfigSpec.DoubleValue DM_DAMAGE_MULT;
    public static final ModConfigSpec.DoubleValue DM_COST_MULT;
    public static final ModConfigSpec.DoubleValue DM_COOLDOWN_MULT;

    // Ultimate evolution
    public static final ModConfigSpec.BooleanValue EVOLUTION_ENABLED;
    public static final ModConfigSpec.DoubleValue EVOLUTION_MIN_EP;
    public static final ModConfigSpec.BooleanValue EVOLUTION_REPLACES_BASE;

    // God-tier
    public static final ModConfigSpec.BooleanValue GOD_TIER_AFFECTS_PLAYERS;
    public static final ModConfigSpec.DoubleValue BOSS_HEALTH_THRESHOLD;
    public static final ModConfigSpec.DoubleValue INSTANT_DEATH_BOSS_SHARE;
    public static final ModConfigSpec.DoubleValue ALMIGHTY_DODGE_CHANCE;

    // Black Clover grimoires
    public static final ModConfigSpec.BooleanValue GRIMOIRE_ENABLED;
    public static final ModConfigSpec.DoubleValue THREE_LEAF_CHANCE;
    public static final ModConfigSpec.DoubleValue FOUR_LEAF_CHANCE;
    public static final ModConfigSpec.DoubleValue FIVE_LEAF_CHANCE;
    public static final ModConfigSpec.DoubleValue DESPAIR_CHANCE;
    public static final ModConfigSpec.IntValue DEVIL_UNION_SECONDS;
    public static final ModConfigSpec.BooleanValue ANNOUNCE_THREE_LEAF;
    public static final ModConfigSpec.IntValue WEIGHT_CLOVER, WEIGHT_SPADE, WEIGHT_HEART, WEIGHT_DIAMOND;
    public static final ModConfigSpec.BooleanValue ALLOW_FORBIDDEN_START;
    public static final ModConfigSpec.DoubleValue BAL_MOB_DAMAGE_MULT;
    public static final ModConfigSpec.BooleanValue GRIEF;
    public static final ModConfigSpec.BooleanValue WORLD_TREE_PERMANENT;   // 0.45
    public static final ModConfigSpec.BooleanValue ZAGRED_BOSS_ENABLED, KOTODAMA_BOSS_REWARD;   // 0.47
    public static final ModConfigSpec.BooleanValue ANTI_MAGIC_EVENT_ONLY;                       // 0.48
    public static final ModConfigSpec.IntValue PVP_HIT_CAP_PERCENT, SPELL_DAMAGE_PERCENT, SPELL_COOLDOWN_PERCENT;   // 0.48
    // Magic gear
    public static final ModConfigSpec.DoubleValue GEAR_REGEN_JUNIOR, GEAR_REGEN_SENIOR, GEAR_SPADE_REGEN_PENALTY, GEAR_CONDUCTION_DAMAGE;
    public static final ModConfigSpec.IntValue GEAR_SENIOR_DAMAGE, GEAR_ELEMENT_BONUS, GEAR_RECOIL_REDUCTION, GEAR_EAGLE_REDUCTION, GEAR_HEART_COST,
            GEAR_DIAMOND_COOLDOWN, GEAR_SPADE_DAMAGE, GEAR_DEVIL_DAMAGE, GEAR_DEVIL_GRACE, GEAR_FORTUNE_SUCCESS, GEAR_FORTUNE_CRACK;
    // Forbidden magic
    public static final ModConfigSpec.DoubleValue FORBIDDEN_HP_PRICE, FORBIDDEN_MAXHP_PRICE, FORBIDDEN_MANA_TAX;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("summoner");
        SUMMON_MAX = b.comment("Max summons alive at once.").defineInRange("maxSummons", 3, 1, 50);
        SUMMON_MAX_MASTERED = b.comment("Max summons alive once Summoner is mastered.").defineInRange("maxSummonsMastered", 6, 1, 50);
        SUMMON_MAGICULE_COST = b.comment("Magicules spent per summon.").defineInRange("magiculeCost", 500.0, 0.0, 1.0E9);
        SUMMON_COOLDOWN = b.comment("Cooldown after summoning (same unit Tensura uses for skill cooldowns, believed to be ticks: 20 = 1 second).").defineInRange("cooldown", 100, 0, 1_000_000);
        b.pop();

        b.push("faith");
        BESTOWABLE_SKILLS = b.comment("Skills that Bestow can grant (one random skill the target doesn't have).",
                        "Find IDs in game: type '/tensura edit <you> ability grant ' and press Tab.")
                .defineListAllowEmpty("bestowableSkills", List.of("tensura:magic_sense", "tensura:sage"), () -> "tensura:", o -> o instanceof String);
        BESTOW_FAITH_COST = b.comment("Faith points spent per bestowal.").defineInRange("bestowFaithCost", 10, 0, 1_000_000);
        BESTOW_COOLDOWN = b.comment("Cooldown after bestowing (ticks: 12000 = 10 minutes).").defineInRange("bestowCooldown", 12000, 0, 10_000_000);
        PRAY_MAGICULE_COST = b.comment("Magicules spent to gain 1 Faith with Pray.").defineInRange("prayMagiculeCost", 1000.0, 0.0, 1.0E9);
        PRAY_COOLDOWN = b.comment("Cooldown after praying (ticks).").defineInRange("prayCooldown", 200, 0, 1_000_000);
        b.pop();

        b.push("danmachi");
        FIREBOLT_DAMAGE = b.comment("Firebolt damage (doubled when mastered).").defineInRange("fireboltDamage", 8.0, 0.0, 10000.0);
        FIREBOLT_MAGICULE_COST = b.defineInRange("fireboltMagiculeCost", 100.0, 0.0, 1.0E9);
        FIREBOLT_COOLDOWN = b.comment("Ticks (20 = 1 second).").defineInRange("fireboltCooldown", 20, 0, 1_000_000);
        ARGONAUT_DAMAGE_PER_CHARGE = b.comment("Damage per charge level (max 5 charges, x1.5 when mastered).").defineInRange("argonautDamagePerCharge", 8.0, 0.0, 10000.0);
        ARGONAUT_MAGICULE_PER_CHARGE = b.defineInRange("argonautMagiculePerCharge", 200.0, 0.0, 1.0E9);
        ARGONAUT_COOLDOWN = b.comment("Cooldown after release (ticks).").defineInRange("argonautCooldown", 200, 0, 1_000_000);
        LIARIS_CHANCE = b.comment("Chance per kill that Liaris Freese grows your skills (0.0 - 1.0).").defineInRange("liarisChance", 0.5, 0.0, 1.0);
        LIARIS_MASTERY_PER_KILL = b.comment("Mastery points added to each of your skills when it triggers.").defineInRange("liarisMasteryPerKill", 1, 0, 1000);
        AIRIEL_MAGICULE_COST = b.defineInRange("airielMagiculeCost", 300.0, 0.0, 1.0E9);
        AIRIEL_COOLDOWN = b.comment("Ticks.").defineInRange("airielCooldown", 100, 0, 1_000_000);
        b.pop();

        b.push("danmachi_gods_and_magic");
        DM_DAMAGE_MULT = b.comment("Multiplies damage of all god and magic skills.").defineInRange("damageMultiplier", 1.0, 0.0, 100.0);
        DM_COST_MULT = b.comment("Multiplies magicule cost of all god and magic skills.").defineInRange("costMultiplier", 1.0, 0.0, 100.0);
        DM_COOLDOWN_MULT = b.comment("Multiplies cooldowns of all god and magic skills.").defineInRange("cooldownMultiplier", 1.0, 0.0, 100.0);
        b.pop();

        b.push("ultimate_evolution");
        EVOLUTION_ENABLED = b.comment("Mastered Unique skills evolve into their Ultimate automatically.").define("enabled", true);
        EVOLUTION_MIN_EP = b.comment("Minimum EP needed to evolve (0 = mastery is enough).").defineInRange("minimumEP", 0.0, 0.0, 1.0E12);
        EVOLUTION_REPLACES_BASE = b.comment("Remove the Unique skill when it evolves (like Great Sage -> Raphael).").define("replacesBase", true);
        b.pop();

        b.push("god_tier");
        GOD_TIER_AFFECTS_PLAYERS = b.comment("Can Instant Death / All Fiction / Hollow Purple instantly kill or erase PLAYERS? If false, players take a share of max health instead.").define("affectsPlayers", false);
        BOSS_HEALTH_THRESHOLD = b.comment("Anything with at least this much max health (or tagged c:bosses) counts as a boss and can't be instantly killed.").defineInRange("bossHealthThreshold", 300.0, 1.0, 1.0E9);
        INSTANT_DEATH_BOSS_SHARE = b.comment("Share of max health Instant Death removes from bosses (0.3 = 30%).").defineInRange("instantDeathBossShare", 0.3, 0.0, 1.0);
        ALMIGHTY_DODGE_CHANCE = b.comment("Chance The Almighty makes an attack never happen (x1.5 when mastered).").defineInRange("almightyDodgeChance", 0.2, 0.0, 1.0);
        b.pop();

        b.push("black_clover");
        GRIMOIRE_ENABLED = b.comment("Players roll for a grimoire once TR Nightmare gives them a soul type.").define("enabled", true);
        THREE_LEAF_CHANCE = b.comment("Percent chance of a three-leaf grimoire.").defineInRange("threeLeafChance", 10.0, 0.0, 100.0);
        FOUR_LEAF_CHANCE = b.comment("Percent chance of a four-leaf grimoire.").defineInRange("fourLeafChance", 2.0, 0.0, 100.0);
        FIVE_LEAF_CHANCE = b.comment("Percent chance of a five-leaf grimoire (with a devil).").defineInRange("fiveLeafChance", 1.0, 0.0, 100.0);
        DESPAIR_CHANCE = b.comment("Chance (0-1) a four-leaf owner's grimoire darkens into a five-leaf when they die.").defineInRange("despairChance", 0.05, 0.0, 1.0);
        DEVIL_UNION_SECONDS = b.defineInRange("devilUnionSeconds", 30, 1, 600);
        ANNOUNCE_THREE_LEAF = b.comment("Announce three-leaf grimoires to the whole server (four/five-leaf always are).").define("announceThreeLeaf", false);
        WEIGHT_CLOVER = b.comment("Kingdom weights at Grimoire Acceptance.").defineInRange("weightClover", 40, 0, 1000);
        WEIGHT_SPADE = b.defineInRange("weightSpade", 20, 0, 1000);
        WEIGHT_HEART = b.defineInRange("weightHeart", 20, 0, 1000);
        WEIGHT_DIAMOND = b.defineInRange("weightDiamond", 20, 0, 1000);
        ALLOW_FORBIDDEN_START = b.comment("Balance law: five-leaf / triple spade can't be a starting roll unless this is true.").define("allowForbiddenStart", false);
        GRIEF = b.comment("Grief flag: can grimoire explosions break blocks? (default off)").define("griefBlocks", false);
        WORLD_TREE_PERMANENT = b.comment("World Tree Magic grows real trees. false (default): each tree is taken back when its spell ends; true: the trees stay.")
                .define("worldTreeTreesStay", false);
        ZAGRED_BOSS_ENABLED = b.comment("The Zagred boss fight (4 phases). Off by default: when on, ops can start it with /multiverse boss zagred.")
                .define("zagredBossEnabled", false);
        KOTODAMA_BOSS_REWARD = b.comment("Beating the Zagred boss grants Kotodama Magic to everyone who fought it. Off by default: Kotodama stays creative only.")
                .define("kotodamaBossReward", false);
        ANTI_MAGIC_EVENT_ONLY = b.comment("Anti-Magic is event-only: it is never rolled; admins grant it with /multiverse event antimagic <player>.")
                .define("antiMagicEventOnly", true);
        PVP_HIT_CAP_PERCENT = b.comment("0.48 balance: the most one addon spell hit can take from a player, as % of their max health.")
                .defineInRange("pvpHitCapPercent", 40, 1, 100);
        SPELL_DAMAGE_PERCENT = b.comment("0.48 balance: addon spell damage, % (applied on top of the EP scaling).").defineInRange("spellDamagePercent", 100, 0, 10000);
        SPELL_COOLDOWN_PERCENT = b.comment("0.48 pacing: grimoire page and magic weapon cooldowns, % of their listed length (60 = 40% shorter).")
                .defineInRange("spellCooldownPercent", 60, 1, 1000);
        BAL_MOB_DAMAGE_MULT = b.comment("Balance law: multiplier on addon spell damage vs mobs (players: see pvpHitCapPercent).").defineInRange("mobDamageMultiplier", 1.0, 0.0, 10.0);
        GRIMOIRE_UNIQUE_LOOKS = b.comment("Each owner's grimoire gets its own cosmetic variation (cover, trim metal, thickness, clasp, colour drift). Off = every grimoire of a type looks identical.").define("uniqueLooks", true);
        b.pop();

        b.push("magic_gear");
        GEAR_REGEN_JUNIOR = b.comment("Junior robe: % of max magicule regenerated per second.").defineInRange("juniorRegen", 0.5, 0.0, 100.0);
        GEAR_REGEN_SENIOR = b.comment("Senior robe: % of max magicule regenerated per second.").defineInRange("seniorRegen", 1.0, 0.0, 100.0);
        GEAR_SENIOR_DAMAGE = b.comment("Senior robe: % grimoire damage bonus.").defineInRange("seniorDamage", 5, 0, 1000);
        GEAR_ELEMENT_BONUS = b.comment("Squad robes: % damage bonus for their element.").defineInRange("squadElementBonus", 15, 0, 1000);
        GEAR_RECOIL_REDUCTION = b.comment("Black Bull robe: % less self-damage from pages.").defineInRange("blackBullRecoilReduction", 50, 0, 100);
        GEAR_EAGLE_REDUCTION = b.comment("Silver Eagle cloak: % less damage taken.").defineInRange("silverEagleReduction", 10, 0, 100);
        GEAR_HEART_COST = b.comment("Heart uniform: % less grimoire magicule cost.").defineInRange("heartCostReduction", 10, 0, 100);
        GEAR_DIAMOND_COOLDOWN = b.comment("Diamond coat: % shorter grimoire cooldowns.").defineInRange("diamondCooldownReduction", 15, 0, 90);
        GEAR_SPADE_DAMAGE = b.comment("Spade war coat: % grimoire damage bonus.").defineInRange("spadeDamage", 15, 0, 1000);
        GEAR_SPADE_REGEN_PENALTY = b.comment("Spade war coat: % of max magicule lost per second.").defineInRange("spadeRegenPenalty", 0.3, 0.0, 100.0);
        GEAR_DEVIL_DAMAGE = b.comment("Devil-bound coat: % grimoire damage bonus.").defineInRange("devilDamage", 20, 0, 1000);
        GEAR_DEVIL_GRACE = b.comment("Devil-bound coat: seconds after a devil price before the coat withers you.").defineInRange("devilGraceSeconds", 600, 10, 100000);
        GEAR_CONDUCTION_DAMAGE = b.comment("Magic tools: attribute damage added per hit.").defineInRange("conductionDamage", 3.0, 0.0, 100.0);
        GEAR_FORTUNE_SUCCESS = b.comment("Fortune Die: % chance of the five-sided evolution.").defineInRange("fortuneSuccess", 30, 0, 100);
        GEAR_FORTUNE_CRACK = b.comment("Fortune Die: % chance it cracks the cover instead.").defineInRange("fortuneCrack", 20, 0, 100);
        b.pop();

        b.push("forbidden_magic");
        FORBIDDEN_HP_PRICE = b.comment("HP paid per forbidden cast (stacks +25% per recent cast, decays every 5 min).").defineInRange("hpPrice", 3.0, 0.0, 100.0);
        FORBIDDEN_MAXHP_PRICE = b.comment("Devil contract 'life' price: max HP removed.").defineInRange("maxHpPrice", 4.0, 0.0, 100.0);
        FORBIDDEN_MANA_TAX = b.comment("Devil contract 'mana' price: permanent extra grimoire cost (0.15 = +15%).").defineInRange("manaTax", 0.15, 0.0, 10.0);
        b.pop();

        SPEC = b.build();
    }

    private NUConfig() {}
}
