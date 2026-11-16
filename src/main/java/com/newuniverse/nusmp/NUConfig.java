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

    // DanMachi
    public static final ModConfigSpec.DoubleValue FIREBOLT_DAMAGE;
    public static final ModConfigSpec.DoubleValue FIREBOLT_MAGICULE_COST;
    public static final ModConfigSpec.IntValue FIREBOLT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue ARGONAUT_DAMAGE_PER_CHARGE;
    public static final ModConfigSpec.DoubleValue ARGONAUT_MAGICULE_PER_CHARGE;
    public static final ModConfigSpec.IntValue ARGONAUT_COOLDOWN;
    public static final ModConfigSpec.BooleanValue ARGONAUT_FIREBOLT_MERGE;
    public static final ModConfigSpec.BooleanValue ARGONAUT_WIKI_CANON;
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
    public static final ModConfigSpec.BooleanValue EP_NEEDS_MASTERY;                     // the EP part of addon damage grows in with the skill's mastery (balance.EpMastery); false = the old damage
    public static final ModConfigSpec.BooleanValue LATE_GAME_SCALING;                    // 3.90: mod damage grows with the caster's max magicule (balance.LateGameScaling); false = the old damage
    public static final ModConfigSpec.DoubleValue LATE_GAME_BASELINE, LATE_GAME_CAP, GRIMOIRE_DAMAGE_MULT;
    public static final ModConfigSpec.BooleanValue GRIEF;
    public static final ModConfigSpec.BooleanValue WORLD_TREE_PERMANENT;   // 0.45
    public static final ModConfigSpec.BooleanValue WEAPON_DRAINS_TARGET;     // 1.113.0: Elsdocia and the Demon Dweller take magicules from whoever they hit (the old behaviour); default false = they only charge
    public static final ModConfigSpec.BooleanValue LIGHT_BONE_JA;           // 1.99.0: Light and Bone Magic after the Black Clover wiki (book.LightBoneJa); false = the old pages
    public static final ModConfigSpec.BooleanValue BOSS_GLOBAL_BUFFS;                    // 1.25.0: Mod Boss Global Buff Engine
    public static final ModConfigSpec.DoubleValue BOSS_BUFF_MULTIPLIER;
    // 1.99.0: the armour policy (book.ArmourPolicy): how strong the worn armours and forms are and what they cost
    public static final ModConfigSpec.DoubleValue ARMOUR_UPKEEP_SCALE, ARMOUR_BONUS_SCALE, DIVE_ABSORB_SCALE;
    public static final ModConfigSpec.IntValue DEVIL_FORM_LIMIT_SECONDS, DEVIL_FORM_AFTERMATH_SECONDS;
    public static final ModConfigSpec.BooleanValue ZAGRED_BOSS_ENABLED, KOTODAMA_BOSS_REWARD;   // 0.47
    public static final ModConfigSpec.BooleanValue ANTI_MAGIC_EVENT_ONLY;                       // 0.48
    public static final ModConfigSpec.BooleanValue NEN_ENABLED, ALL_FICTION_REBUILT, NEN_VFX;          // 3.46.0
    public static final ModConfigSpec.BooleanValue NEN_VFX_V2;          // 3.57.0
    public static final ModConfigSpec.BooleanValue GATE_ENABLED, KI_ENABLED, DB_ENABLED;                  // 3.47.0, 3.56.0
    public static final ModConfigSpec.BooleanValue BEAST_SKILLS_ENABLED, BEAST_SKILL_GRANT_ON_MASTERY;   // 3.59.0
    public static final ModConfigSpec.BooleanValue DB_LEGACY_SKILLS;                                     // Dragon Ball rework phase 1
    public static final ModConfigSpec.BooleanValue SHINRA_CANON;                                         // Shinra canon pass (docs/shinra_rework.md, Fire Force wiki)
    public static final ModConfigSpec.BooleanValue SHINRA_REWORK;                                        // Shinra rework (docs/shinra_rework.md)
    public static final ModConfigSpec.BooleanValue TRANSFORM_RESET_ON_DEATH;                                                // transformations.resetOnDeath (core.xform.TransformGuard, docs/transformation_fixes.md)
    public static final ModConfigSpec.BooleanValue ALL_FICTION_GOD, ALL_FICTION_CANON;                                      // All Fiction god-class rework (docs/all_fiction_rework.md)
    public static final ModConfigSpec.IntValue PVP_HIT_CAP_PERCENT, SPELL_DAMAGE_PERCENT, SPELL_COOLDOWN_PERCENT;   // 0.48
    // Magic gear
    public static final ModConfigSpec.DoubleValue GEAR_REGEN_JUNIOR, GEAR_REGEN_SENIOR, GEAR_SPADE_REGEN_PENALTY, GEAR_CONDUCTION_DAMAGE;
    public static final ModConfigSpec.IntValue GEAR_SENIOR_DAMAGE, GEAR_ELEMENT_BONUS, GEAR_RECOIL_REDUCTION, GEAR_EAGLE_REDUCTION, GEAR_HEART_COST,
            GEAR_DIAMOND_COOLDOWN, GEAR_SPADE_DAMAGE, GEAR_DEVIL_DAMAGE, GEAR_DEVIL_GRACE, GEAR_FORTUNE_SUCCESS, GEAR_FORTUNE_CRACK;
    // Forbidden magic
    public static final ModConfigSpec.DoubleValue FORBIDDEN_HP_PRICE, FORBIDDEN_MAXHP_PRICE, FORBIDDEN_MANA_TAX;
    public static final ModConfigSpec.BooleanValue TENSURA_SKILL_DAMAGE_SOURCE;   // guide P1 #2 (skill.SkillDamage)
    public static final ModConfigSpec.BooleanValue GF_CAST_EVENT, GF_DIRTY_ON_CHANGE;   // guide P2 #6 (book.GrimoireCastEvent), P3 #14 (GrimoireBook.onHeld, AntiMagic.setAmp)
    // The Ultimate classes (skill.rules.UltimateClass, docs/ultimate_classes.md): one value per class and column, plus the switches that restore the old behaviour
    private static final int UC_N = com.newuniverse.nusmp.skill.rules.UltimateClass.values().length;
    public static final ModConfigSpec.DoubleValue[] UC_EP_CAP = new ModConfigSpec.DoubleValue[UC_N], UC_COST_PCT = new ModConfigSpec.DoubleValue[UC_N],
            UC_COST_REF = new ModConfigSpec.DoubleValue[UC_N], UC_KILL_BELOW = new ModConfigSpec.DoubleValue[UC_N], UC_SHRED_BELOW = new ModConfigSpec.DoubleValue[UC_N];
    public static final ModConfigSpec.IntValue[] UC_MAX_MASTERY = new ModConfigSpec.IntValue[UC_N];
    public static final ModConfigSpec.BooleanValue UC_FLAT_COSTS, UC_CLASS_MASTERY, UC_MASTERED_TIER, UC_MILESTONES, UC_CLASS_DAMAGE;
    public static final ModConfigSpec.DoubleValue UC_LATE_SHARE, UC_PLAIN_COST_PCT, UC_PLAIN_COST_REF;
    // Summoner rework (docs/summoner_rework.md): Allen's Hawk Eye, Spirit Card, Strengthening and Synthesis folded into the Summoner
    public static final ModConfigSpec.BooleanValue SUMMONER_REWORK;
    public static final ModConfigSpec.DoubleValue SUMMONER_MASTERY_PERCENT;
    public static final ModConfigSpec.BooleanValue SUMMONER_CANON;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("summoner");
        SUMMON_MAX = b.comment("Max summons alive at once.").defineInRange("maxSummons", 3, 1, 50);
        SUMMON_MAX_MASTERED = b.comment("Max summons alive once Summoner is mastered.").defineInRange("maxSummonsMastered", 6, 1, 50);
        SUMMON_MAGICULE_COST = b.comment("Magicules spent per summon.").defineInRange("magiculeCost", 500.0, 0.0, 1.0E9);
        SUMMON_COOLDOWN = b.comment("Cooldown after summoning (same unit Tensura uses for skill cooldowns, believed to be ticks: 20 = 1 second).").defineInRange("cooldown", 100, 0, 1_000_000);
        b.pop();

        b.push("danmachi");
        FIREBOLT_DAMAGE = b.comment("Firebolt damage (doubled when mastered).").defineInRange("fireboltDamage", 8.0, 0.0, 10000.0);
        FIREBOLT_MAGICULE_COST = b.defineInRange("fireboltMagiculeCost", 100.0, 0.0, 1.0E9);
        FIREBOLT_COOLDOWN = b.comment("Ticks (20 = 1 second).").defineInRange("fireboltCooldown", 20, 0, 1_000_000);
        ARGONAUT_DAMAGE_PER_CHARGE = b.comment("Damage per charge level (max 5 charges, x1.5 when mastered).").defineInRange("argonautDamagePerCharge", 8.0, 0.0, 10000.0);
        ARGONAUT_MAGICULE_PER_CHARGE = b.defineInRange("argonautMagiculePerCharge", 200.0, 0.0, 1.0E9);
        ARGONAUT_COOLDOWN = b.comment("Cooldown after release (ticks).").defineInRange("argonautCooldown", 200, 0, 1_000_000);
        ARGONAUT_FIREBOLT_MERGE = b.comment("Firebolt is a mode of Argonaut (docs/argonaut_rework.md): Argonaut and Hero's Argonaut gain a rapid-cast Firebolt mode, a charged release carries fire, and a Firebolt holder is moved to Argonaut at login.",
                "false = the old DanMachi: Firebolt is its own skill, Argonaut has only the charged strike, nothing is migrated (holders migrated earlier get Firebolt back at their next login).")
                .define("argonautFireboltMerge", true);
        ARGONAUT_WIKI_CANON = b.comment("Argonaut follows the DanMachi wiki (docs/argonaut_rework.md section 3): two more modes (Charged Firebolt, Argo Vesta), the charge costs stamina as well as magicules, a hit breaks a charge, and only Argo Vesta and the Afterglow beam burn what they hit.",
                "false = Argonaut as it was in 4.4.0 (the plain charged strike burns, no extra modes, no stamina cost, a hit does not break the charge). Needs argonautFireboltMerge.")
                .define("argonautWikiCanon", true);
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
        GOD_TIER_AFFECTS_PLAYERS = b.comment("Can All Fiction instantly kill or erase PLAYERS? If false, players take a share of max health instead.").define("affectsPlayers", false);
        BOSS_HEALTH_THRESHOLD = b.comment("Anything with at least this much max health (or tagged c:bosses) counts as a boss and can't be instantly killed.").defineInRange("bossHealthThreshold", 300.0, 1.0, 1.0E9);
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
        EP_NEEDS_MASTERY = b.comment("EP scaling needs mastery: the EP part of an addon hit grows with the casting skill's mastery (x1 with none, the full EP scale once mastered). false = the old damage, the full EP scale at any mastery.")
                .define("epNeedsMastery", true);
        LATE_GAME_SCALING = b.comment("Late-game scaling: every addon hit a player deals through the balance law grows with that player's max magicule (x1 at the baseline, +1.25 per tenfold above it, at most lateGameScalingCap). false = the old damage.")
                .define("lateGameScaling", true);
        LATE_GAME_BASELINE = b.comment("Late-game scaling: the max magicule at which the factor is x1 (below it, x1 too).").defineInRange("lateGameScalingBaseline", 10_000.0, 1.0, 1.0E12);
        LATE_GAME_CAP = b.comment("Late-game scaling: the largest factor (reached at baseline x 10,000 with the defaults). 1 = no scaling.").defineInRange("lateGameScalingCap", 6.0, 1.0, 100.0);
        GRIMOIRE_DAMAGE_MULT = b.comment("Grimoire spell damage multiplier (every page that hits through GrimoireBook.hurt), on top of the late-game scaling. 1.0 = the old damage.")
                .defineInRange("grimoireDamageMultiplier", 1.75, 0.0, 10.0);
        GRIMOIRE_UNIQUE_LOOKS = b.comment("Each owner's grimoire gets its own cosmetic variation (cover, trim metal, thickness, clasp, colour drift). Off = every grimoire of a type looks identical.").define("uniqueLooks", true);
        b.pop();

        b.push("magic");
        WEAPON_DRAINS_TARGET = b.comment("Elsdocia and the Demon Dweller used to take a share of the TARGET's magicules on every hit (2 percent and 1.5 percent of its maximum). false (default) = they still charge from hits (the charge their techniques use) but take nothing from the target, player or mob. true = the old drain.").define("weaponsDrainTargetMagicule", false);
        LIGHT_BONE_JA = b.comment("Light Magic and Bone Magic follow the Black Clover wiki spell lists (the remake): Light gets the Bright Judgment Whip, Heaven-Splitting Flash, Arrows of Judgment, Lamp of Avior and the Absolute Monarch of Divine Brilliance, Bone gets Bone Bullets, Eternal Fangs, Demon Sword Dainsleif and Spatial Rupture.",
                "false = the old pages of both books (every old page id stays either way).").define("lightBoneJa", true);
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

        b.push("bosses");
        BOSS_GLOBAL_BUFFS = b.comment("Mod Boss Global Buff Engine: permanent Tensura barriers, regeneration and theme skills on this mod's bosses only.").define("globalBuffs", true);
        BOSS_BUFF_MULTIPLIER = b.comment("Scales the engine's effect levels and Lucius's damage cuts (0 turns them off, 1 = as designed).").defineInRange("buffMultiplier", 1.0, 0.0, 3.0);
        b.pop();

        b.push("armour");
        ARMOUR_UPKEEP_SCALE = b.comment("Multiplies the magicule a worn armour or form costs every second (0 = free).").defineInRange("upkeepScale", 1.0, 0.0, 10.0);
        ARMOUR_BONUS_SCALE = b.comment("Multiplies the stat bonuses of a worn armour or form (damage cut, attack, speed).").defineInRange("bonusScale", 1.0, 0.0, 3.0);
        DIVE_ABSORB_SCALE = b.comment("Multiplies the ambient mana a Spirit Dive takes in every second (0 = none, the dive then only drains).").defineInRange("diveAbsorbScale", 1.0, 0.0, 5.0);
        DEVIL_FORM_LIMIT_SECONDS = b.comment("Devil Union (the Anti-Magic Black Form): the strict time limit in seconds, then the form shatters (default 5 minutes).").defineInRange("devilFormLimitSeconds", 300, 20, 36000);
        DEVIL_FORM_AFTERMATH_SECONDS = b.comment("How long the user is incapacitated by muscle pain after the limit broke the union (slowness, weakness, mining fatigue; never hunger). 0 = none.")
                .defineInRange("devilFormAftermathSeconds", 1800, 0, 86400);
        b.pop();

        b.push("ultimates_3_46");
        NEN_ENABLED = b.comment("Nen (Hunter x Hunter Ultimate, 3.46.0). false = the skill stays registered but every mode refuses and its damage hooks do nothing.").define("nenEnabled", true);
        NEN_VFX = b.comment("Nen's own effects (3.48.5: flowing aura, En sphere, Gyo eyes, hatsu impact and bolt). false = the particle puffs Nen had before.").define("nenVfx", true);
        NEN_VFX_V2 = b.comment("Nen's remade effects (3.57.0: layered flame aura, Ren burst, En lattice sphere, Gyo eyes, fist, homing bullets, chains, needle, field). false = the 3.48.5 look (nenVfx must be true for either).").define("nenVfxV2", true);
        ALL_FICTION_REBUILT = b.comment("All Fiction rebuild (3.46.0, ten modes). false = the old two modes only (Undo and Fiction) and the old death-refusal; the new modes refuse.").define("allFictionRebuilt", true);
        b.pop();

        b.push("ultimates_3_47");
        GATE_ENABLED = b.comment("Gate of Babylon (Fate Ultimate, 3.47.0). false = the skill stays registered but every mode refuses and its damage hook does nothing.").define("gateEnabled", true);
        KI_ENABLED = b.comment("Ki (Dragon Ball Ultimate, 3.47.0). false = the skill stays registered but every mode refuses and its damage hook does nothing.").define("kiEnabled", true);
        DB_ENABLED = b.comment("The 15 Dragon Ball skills (3.56.0). false = they stay registered but every mode refuses and the passives do nothing.").define("dragonBallEnabled", true);
        BEAST_SKILLS_ENABLED = b.comment("The 54 Beast Will skills (3.59.0, a Unique and an Ultimate for each of the 27 wills). false = they stay registered but every mode refuses, the passives do nothing and they do not evolve.").define("beastSkillsEnabled", true);
        BEAST_SKILL_GRANT_ON_MASTERY = b.comment("Mastering a Beast Will to 100 % grants the Unique skill of that beast once (3.59.0). false = the skills come only from the admin command or /tensura edit.").define("beastSkillGrantOnMastery", true);
        DB_LEGACY_SKILLS = b.comment("Dragon Ball rework, phase 1. The 15 old Dragon Ball skills (30 ids, 3.56.0) are REMOVED by default: false = no command, no evolution, no use, and they are taken from players at login (the ids stay registered so old worlds load). true = the old behaviour (dragonBallEnabled still applies).").define("dragonBallLegacySkills", false);
        SHINRA_REWORK = b.comment("Shinra rework (docs/shinra_rework.md). true = Hell Walker is the Shinra Kusakabe skill (the dash, kick and Rapid-Man Kick on the wheel, the devil's grin and Adolla Link under low health, fire immunity), Devil's Footprints is folded into it at login and Hell Walker evolves into Adolla Burst. false = the old behaviour: Hell Walker is only the toggle, no wheel, no folding, Hell Walker evolves into Hell Conqueror.").define("shinraRework", true);
        SHINRA_CANON = b.comment("Shinra canon pass (docs/shinra_rework.md). true = the Shinra skill follows the Fire Force wiki: Rapid, Hellfire (Corna), Adolla Link and Adolla Grace join the wheel (unlocked by tier), Rapid boosts the Rapid-Man Kick, the low-health passive is Hysterical Strength (blue flames) and the Devil's Grin is only a look. false = the first rework (shinraRework) exactly as shipped. Needs shinraRework.").define("shinraCanon", true);
        b.pop();

        b.push("guide_fixes");
        TENSURA_SKILL_DAMAGE_SOURCE = b.comment("Rebuilt skills (Gate of Babylon, Ki, Nen, Adolla, Firebolt, the chant Ultimates ...) hit through Tensura's skill damage source, so Tensura resistances, nullifications and skill kill credit apply. false = the old vanilla magic / player-attack sources. The damage amount and the PvP cap are the same either way.")
                .define("tensuraSkillDamageSource", true);
        GF_CAST_EVENT = b.comment("Guide P2 #6: a grimoire cast posts a cancellable GrimoireCastEvent.Pre before it is paid and a GrimoireCastEvent.Post after it, and the features that follow a cast (quests, relics, character flair, Solomon's cast log ...) listen to the Post event. false = the old direct calls in GrimoireBook.castPage, no event is posted.")
                .define("grimoireCastEvent", true);
        GF_DIRTY_ON_CHANGE = b.comment("Guide P3 #14: a held grimoire page and Anti-Magic Power mark the skill dirty (a sync to the client) only when a value changed. false = the old behaviour (every held tick, every set).")
                .define("markDirtyOnlyOnChange", true);
        b.pop();

        b.push("ultimateClasses");
        UC_FLAT_COSTS = b.comment("The addon skills (Gate of Babylon, Nen, Ki, The Visionary, Firebolt ...) pay through Tensura (getMagiculeCost + EnergyHelper.isOutOfEnergy) at a price that grows with max magicule. true = the old flat price, written straight to the magicule pool.")
                .define("flatCosts", false);
        UC_CLASS_MASTERY = b.comment("A classified Ultimate takes its class's maxMastery points to master (existing mastery is moved at the same fraction at login). false = Tensura's own Ultimate mastery.").define("classMastery", true);
        UC_MASTERED_TIER = b.comment("A mastered classified Ultimate reads the ULTIMATE_MASTERED tier (its mastered numbers and the x1.2 mastery bonus). false = the old tier (always ULTIMATE).").define("masteredTier", true);
        UC_MILESTONES = b.comment("Mastery milestones of a classified Ultimate: 25 % range x1.2, 50 % second effect, 75 % cost x0.8 and cooldown x0.75, 100 % mastered; each announced once. false = none.").define("milestones", true);
        UC_CLASS_DAMAGE = b.comment("A classified hit scales with the class-capped EP scaling times the late-game factor, at most epCap x lateGameShare (BalanceLaw.damage with a class). false = the unclassified scaling.").define("classDamage", true);
        UC_LATE_SHARE = b.comment("Combined cap of a classified hit: epScale(class) x late-game factor is at most epCap x this (3 = x24 Lord, x30 King, x36 God / Dragon; the unclassified top is x72).")
                .defineInRange("lateGameShare", 3.0, 1.0, 6.0);
        UC_PLAIN_COST_PCT = b.comment("Unclassified addon skills (Uniques, Extras): a cast whose flat price is plainCostReference costs this % of max magicule late game (never below its flat price). 0 = flat prices.")
                .defineInRange("plainCostPercent", 1.0, 0.0, 50.0);
        UC_PLAIN_COST_REF = b.comment("Unclassified addon skills: the flat price that maps to plainCostPercent.").defineInRange("plainCostReference", 1_000.0, 1.0, 1.0E9);
        for (com.newuniverse.nusmp.skill.rules.UltimateClass c : com.newuniverse.nusmp.skill.rules.UltimateClass.values()) {
            int i = c.ordinal();
            b.push(c.key());
            UC_EP_CAP[i] = b.comment("Damage ceiling of the EP scaling for this class (1 to 12).").defineInRange("epCap", c.epCap, 1.0, com.newuniverse.nusmp.skill.rules.UltimateClass.EP_CAP_MAX);
            UC_COST_PCT[i] = b.comment("Late game, a cast whose flat price is costReference costs this % of max magicule.").defineInRange("costPercent", c.costPct, 0.0, 50.0);
            UC_COST_REF[i] = b.comment("The flat price that maps to costPercent (every cast keeps its own flat price as the floor).").defineInRange("costReference", c.costRef, 1.0, 1.0E9);
            UC_MAX_MASTERY[i] = b.comment("Mastery points to master a skill of this class.").defineInRange("maxMastery", c.maxMastery, 100, 1_000_000);
            UC_KILL_BELOW[i] = b.comment("EP-ratio instakill gate (fraction of the caster's EP; never bosses or players); 0 = none.").defineInRange("killBelow", c.killBelow, 0.0, 0.95);
            UC_SHRED_BELOW[i] = b.comment("EP-ratio HP / MP shred gate (fraction of the caster's EP); 0 = none.").defineInRange("shredBelow", c.shredBelow, 0.0, 0.95);
            b.pop();
        }
        b.pop();

        b.push("summonerRework");
        SUMMONER_REWORK = b.comment("Summoner rework: Allen's Hawk Eye, Spirit Card, Strengthening and Synthesis are modes / passives of the Summoner, opened by its mastery (Bird Card 15 %, Strengthening 30 %, Synthesis 45 %, Spirit Card 60 %, stat reflection 75 %); the Summoner of Beginnings has them all. The old Hawk Eye and Spirit Card skills are carried into the Summoner at login. false = the old Summoner (Beast Card, Stone Card, Recall; Legion and Wraith for the Lord) and the old Hawk Eye / Spirit Card toggles.")
                .define("summonerRework", true);
        SUMMONER_MASTERY_PERCENT = b.comment("Summoner rework: percent of the maximum mastery one card cast earns (so the modes open after a playable number of casts; 0.5 = about 200 casts to master). Ignored when summonerRework is false (then a cast earns one point, as before).")
                .defineInRange("summonerMasteryPercent", 0.5, 0.0, 5.0);
        SUMMONER_CANON = b.comment("Summoner canon pass (Hell Mode wiki): modes open with Allen's Summoning levels (Insect and Beast 1, Synthesis and Bird 2, Strengthening and Grass 3, Stone and Storage 4, Fish 5, Awakening and Spirit 6, Dragon and Deputize 7), the Insect, Fish, Spirit, Dragon, Angel, Awakening, Deputize and Storage modes exist, Strengthening stays on the summon, cards are named Beast H / Wally, the owner always takes the buffs of the summons he keeps. false = the 4.5.0 Summoner (nine modes, guessed unlock order). Needs summonerRework.")
                .define("summonerCanon", true);
        b.push("ultimates_fiction_god");
        ALL_FICTION_GOD = b.comment("All Fiction god-class rework (sixteen modes: Deny Skill, Overwrite Rule, Reversal, Mass Fiction, Nothing Sticks, Rewrite Time, the Retcon passive) and Book Maker's lead-in modes (Equalize, Glimpse of the Fiction). false = the ten-mode All Fiction and the two-mode Book Maker of 3.46.0 (needs allFictionRebuilt for All Fiction).").define("allFictionGod", true);
        ALL_FICTION_CANON = b.comment("All Fiction / Book Maker canon pass (Medaka Box wiki, docs/all_fiction_rework.md): All Fiction cannot erase another Minus (a player holding Book Maker or any Ultimate), Undo and Erase Effects refuse when there is nothing to erase, new modes Erase Presence and April Fiction (a timed erase); Book Maker's screw does almost no damage and drags the target's attack and armour down toward yours for a few seconds, plus the mastered mode Leg Eating Forest. false = the guess-era behaviour of allFictionGod (needs allFictionGod).").define("allFictionCanon", true);
        b.pop();

        b.push("transformations");
        TRANSFORM_RESET_ON_DEATH = b.comment("Every transformation, form, mode armour and look of a player ends when the player dies and nothing resumes in the new body (core.xform.TransformGuard: persistent form flags, form modifiers, the render layers and the form skills' own switches are cleared on death and again on respawn).",
                        "false = the old behaviour exactly: the guard does nothing, each system keeps what it did on its own.")
                .define("resetOnDeath", true);
        b.pop();

        SPEC = b.build();
    }

    private NUConfig() {}

    // ------------------------------------------------------------------ Ultimate classes (guarded: the default before the SERVER config is loaded, e.g. on a client menu)
    private static <T> T live(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** The All Fiction god-class modes and Book Maker's lead-in modes (default on before the SERVER config is loaded, as the rebuild is). */
    public static boolean allFictionGod() { return live(ALL_FICTION_GOD); }
    /** transformations.resetOnDeath: forms end on death and nothing resumes (default on, also before the SERVER config is loaded). */
    public static boolean transformResetOnDeath() { return live(TRANSFORM_RESET_ON_DEATH); }
    /** The Medaka Box canon pass on top of the god-class modes (default on before the SERVER config is loaded). */
    public static boolean allFictionCanon() { return live(ALL_FICTION_CANON); }

    public static boolean shinraRework() { return live(SHINRA_REWORK); }
    public static boolean shinraCanon() { return live(SHINRA_REWORK) && live(SHINRA_CANON); }
    public static boolean summonerRework() { return live(SUMMONER_REWORK); }
    public static boolean summonerCanon() { return live(SUMMONER_CANON) && live(SUMMONER_REWORK); }
    public static double summonerMasteryPercent() { return Math.max(0.0, Math.min(5.0, live(SUMMONER_MASTERY_PERCENT))); }
    public static boolean gfCastEvent() { return live(GF_CAST_EVENT); }
    public static boolean gfDirtyOnChange() { return live(GF_DIRTY_ON_CHANGE); }
    public static boolean ucFlatCosts() { return live(UC_FLAT_COSTS); }
    public static boolean ucClassMastery() { return live(UC_CLASS_MASTERY); }
    public static boolean ucMasteredTier() { return live(UC_MASTERED_TIER); }
    public static boolean ucMilestones() { return live(UC_MILESTONES); }
    public static boolean ucClassDamage() { return live(UC_CLASS_DAMAGE); }
    public static double ucLateShare() { return com.newuniverse.nusmp.skill.rules.UltimateClass.clampLateShare(live(UC_LATE_SHARE)); }
    public static double ucPlainCostPct() { return com.newuniverse.nusmp.skill.rules.UltimateClass.clampCostPct(live(UC_PLAIN_COST_PCT)); }
    public static double ucPlainCostRef() { return com.newuniverse.nusmp.skill.rules.UltimateClass.clampCostRef(live(UC_PLAIN_COST_REF)); }
    public static double ucEpCap(com.newuniverse.nusmp.skill.rules.UltimateClass c) { return com.newuniverse.nusmp.skill.rules.UltimateClass.clampEpCap(live(UC_EP_CAP[c.ordinal()])); }
    public static double ucCostPct(com.newuniverse.nusmp.skill.rules.UltimateClass c) { return com.newuniverse.nusmp.skill.rules.UltimateClass.clampCostPct(live(UC_COST_PCT[c.ordinal()])); }
    public static double ucCostRef(com.newuniverse.nusmp.skill.rules.UltimateClass c) { return com.newuniverse.nusmp.skill.rules.UltimateClass.clampCostRef(live(UC_COST_REF[c.ordinal()])); }
    public static int ucMaxMastery(com.newuniverse.nusmp.skill.rules.UltimateClass c) { return com.newuniverse.nusmp.skill.rules.UltimateClass.clampMaxMastery(live(UC_MAX_MASTERY[c.ordinal()])); }
    public static double ucKillBelow(com.newuniverse.nusmp.skill.rules.UltimateClass c) { return com.newuniverse.nusmp.skill.rules.UltimateClass.clampRatio(live(UC_KILL_BELOW[c.ordinal()])); }
    public static double ucShredBelow(com.newuniverse.nusmp.skill.rules.UltimateClass c) { return com.newuniverse.nusmp.skill.rules.UltimateClass.clampRatio(live(UC_SHRED_BELOW[c.ordinal()])); }
}
