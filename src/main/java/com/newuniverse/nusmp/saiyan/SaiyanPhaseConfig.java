package com.newuniverse.nusmp.saiyan;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the SERVER settings of the Dragon Ball rework (phases 2 to 6 and the Giant Ki Avatar), in a file of their own ({@code nusmp-saiyan-phase2-server.toml}) so {@link SaiyanConfig} is not touched. Every value
 * changes gameplay, so every value is a server setting (rule 9) and every getter clamps again, so a hand-edited file can not pass a bound. The section {@code owner_decisions} holds the open questions of
 * docs/dragon_ball/phase_report.md: each one is a switch whose DEFAULT is the safest answer from the plan (Kaio-ken stays as it was, no ape forms, no new key, no awakening block, Battle Power and not Existence
 * Points), so the game behaves as before until the owner decides. {@code phase.enabled = false} switches every new technique off at once.
 */
public final class SaiyanPhaseConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue TECH_ON, KAIO_EXT, APE_ON, BRANCH_KEY, BLOCK_AWAKEN, RITUAL_EP, GOD_MASK, REAL_RACE, RITUAL_ON, GIFTS_ON, AVATAR_ON, AVATAR_FORM, TENSURA_SRC, GRAVITY_ON;
    private static final ModConfigSpec.DoubleValue RACE_SCALE, PRIMAL_BP, AUTO_BP,
            STR_KI, STR_ST, STR_DMG, STR_RANGE, STR_KB, VAN_KI, VAN_ST, VAN_DIST, RSH_KI, RSH_ST, RSH_DMG, CNT_KI, CNT_ST, CNT_FACTOR, BRK_KI, BRK_ST, BRK_DMG, BRK_RAD, INS_KI, INS_ST, INS_SPEED, INS_ASPD,
            SPH_KI, SPH_ST, SPH_DMG, SPH_RANGE, SPH_SPEED, SPH_RAD, WAV_KI, WAV_COST, WAV_DMG, WAV_RANGE, REI_DRAIN, REI_DMG, REI_DEF, AFT_COST,
            AVA_KI, AVA_DRAIN, AVA_SEC, AVA_SCALE, AVA_DMG, AVA_REACH, RIT_BP, RIT_EP, GIFT_MIN;
    private static final ModConfigSpec.IntValue AUTO_ZENKAI, STR_CD, STR_WIND, VAN_CD, RSH_CD, CNT_CD, CNT_WINDOW, BRK_CD, INS_CD, INS_SEC, SPH_CD, WAV_CD, WAV_MIN, WAV_MAX, GRV_SEC, AVA_CD,
            RIT_HELPERS, RIT_SEC, RIT_MAX, RIT_FAIL_CD, GIFT_TARGETS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("phase");
        TECH_ON = b.comment("The techniques of the Dragon Ball rework (Meteor Strike, Vanishing Step, Aerial Rush, Warrior's Counter, Ground Breaker, Combat Instinct, Charged Ki Sphere, Ki Wave, Ki Reinforcement, Afterimage, Gravity Training, the Giant Ki Avatar). false = none can be used.").define("enabled", true);
        TENSURA_SRC = b.comment("The Ki techniques of the rework (Ki Sphere, Ki Wave, Giant Ki Avatar) hurt through Tensura's own magic damage type, so a target's Tensura resistances count. false = plain indirect magic damage like the older Ki attacks.").define("tensuraDamageSource", true);

        b.push("owner_decisions");
        KAIO_EXT = b.comment("OWNER DECISION 1a: Kaio-ken x10 and x20 (the spec lists them, the repo had x2 to x4). false = Kaio-ken as before.").define("kaiokenExtraStages", false);
        APE_ON = b.comment("OWNER DECISION 1b: the Oozaru and Golden Great Ape forms (the owner said no ape forms in 3.65.0, the spec lists them). false = they can not be unlocked or entered. They need a full moon at night (or the operator flag artificial_moon).").define("apeForms", false);
        RITUAL_EP = b.comment("OWNER DECISION 2: the Super Saiyan God ritual asks for Tensura Existence Points (ritual.existencePoints) instead of Battle Power (ritual.battlePower). false = Battle Power, the repo's number system.").define("ritualUsesExistencePoints", false);
        BRANCH_KEY = b.comment("OWNER DECISION 4: the branch key (spec: G, but G is Ki Flight here, so the key is unbound by default) switches between the form branches. false = the server ignores the ask.").define("branchKey", false);
        BLOCK_AWAKEN = b.comment("OWNER DECISION 5: keep Saiyans from the True Demon Lord / True Hero awakening. NOT BUILT: the Tensura event could not be found in this build, so true only logs a note once a Saiyan is online. false = nothing.").define("blockAwakening", false);
        b.pop();

        b.push("race");
        REAL_RACE = b.comment("Saiyan as a race with four stages (Saiyan, Primal Saiyan, Divine Saiyan, Autonomous Martial God): stages are earned, their buffs are added by the multiverse race buffs, the status panel shows the stage. false = the Saiyan flag alone, as before.").define("realRace", false);
        RACE_SCALE = b.comment("Multiplier on the stage buffs (0 = none).").defineInRange("buffScale", 1.0, 0.0, 2.0);
        PRIMAL_BP = b.comment("Stage 2 (Primal Saiyan): the Battle Power needed, besides the Super Saiyan 3 form.").defineInRange("primalBattlePower", 400_000.0, 100.0, SaiyanRules.BP_HARD_CAP);
        AUTO_BP = b.comment("Stage 4 (Autonomous Martial God): the Battle Power needed, besides the Perfected Ultra Instinct form and the Zenkai below.").defineInRange("autonomousBattlePower", 8_000_000.0, 100.0, SaiyanRules.BP_HARD_CAP);
        AUTO_ZENKAI = b.comment("Stage 4: the Zenkai needed.").defineInRange("autonomousZenkai", 5, 0, 1000);
        b.pop();

        b.push("physical");
        STR_KI = b.comment("Meteor Strike: Ki per use, percent of the maximum.").defineInRange("meteorStrikeCostPercent", 4.0, 0.0, 100.0);
        STR_ST = b.comment("Meteor Strike: stamina per use.").defineInRange("meteorStrikeStamina", 15.0, 0.0, 1000.0);
        STR_DMG = b.comment("Meteor Strike: damage before the balance law (a charged blow, then knockback).").defineInRange("meteorStrikeDamage", 20.0, 0.0, 10000.0);
        STR_RANGE = b.comment("Meteor Strike: how far (blocks) the foe it lunges at can be.").defineInRange("meteorStrikeRange", 6.0, 2.0, 12.0);
        STR_KB = b.comment("Meteor Strike: knockback strength (a boss is never thrown).").defineInRange("meteorStrikeKnockback", 1.4, 0.0, 4.0);
        STR_CD = b.comment("Meteor Strike cooldown in ticks.").defineInRange("meteorStrikeCooldownTicks", 160, 1, 12000);
        STR_WIND = b.comment("Meteor Strike: ticks of charging before the blow.").defineInRange("meteorStrikeWindupTicks", 8, 1, 40);
        VAN_KI = b.comment("Vanishing Step: Ki per use.").defineInRange("vanishCostPercent", 3.0, 0.0, 100.0);
        VAN_ST = b.comment("Vanishing Step: stamina per use.").defineInRange("vanishStamina", 10.0, 0.0, 1000.0);
        VAN_DIST = b.comment("Vanishing Step: blocks of the dash (at most 16; blocks and walls stop it).").defineInRange("vanishDistance", 8.0, 2.0, 16.0);
        VAN_CD = b.comment("Vanishing Step cooldown in ticks.").defineInRange("vanishCooldownTicks", 100, 1, 12000);
        RSH_KI = b.comment("Aerial Rush: Ki per use.").defineInRange("aerialRushCostPercent", 4.0, 0.0, 100.0);
        RSH_ST = b.comment("Aerial Rush: stamina per use.").defineInRange("aerialRushStamina", 12.0, 0.0, 1000.0);
        RSH_DMG = b.comment("Aerial Rush: damage of each of the (at most three) foes it passes.").defineInRange("aerialRushDamage", 14.0, 0.0, 10000.0);
        RSH_CD = b.comment("Aerial Rush cooldown in ticks.").defineInRange("aerialRushCooldownTicks", 140, 1, 12000);
        CNT_KI = b.comment("Warrior's Counter: Ki per use.").defineInRange("counterCostPercent", 2.0, 0.0, 100.0);
        CNT_ST = b.comment("Warrior's Counter: stamina per use.").defineInRange("counterStamina", 10.0, 0.0, 1000.0);
        CNT_FACTOR = b.comment("Warrior's Counter: the damage given back is the blocked blow times this.").defineInRange("counterFactor", 2.0, 0.0, 10.0);
        CNT_CD = b.comment("Warrior's Counter cooldown in ticks.").defineInRange("counterCooldownTicks", 160, 1, 12000);
        CNT_WINDOW = b.comment("Warrior's Counter: ticks the stance is up.").defineInRange("counterWindowTicks", 12, 2, 60);
        BRK_KI = b.comment("Ground Breaker: Ki per use.").defineInRange("groundBreakerCostPercent", 5.0, 0.0, 100.0);
        BRK_ST = b.comment("Ground Breaker: stamina per use.").defineInRange("groundBreakerStamina", 20.0, 0.0, 1000.0);
        BRK_DMG = b.comment("Ground Breaker: damage of the shockwave before the balance law (up to twice that after a long fall). No block is ever broken.").defineInRange("groundBreakerDamage", 25.0, 0.0, 10000.0);
        BRK_RAD = b.comment("Ground Breaker: radius (blocks) of the shockwave on a short fall (at most 8 after a long one).").defineInRange("groundBreakerRadius", 4.0, 1.0, 8.0);
        BRK_CD = b.comment("Ground Breaker cooldown in ticks.").defineInRange("groundBreakerCooldownTicks", 240, 1, 12000);
        INS_KI = b.comment("Combat Instinct: Ki per use.").defineInRange("instinctCostPercent", 3.0, 0.0, 100.0);
        INS_ST = b.comment("Combat Instinct: stamina per use.").defineInRange("instinctStamina", 10.0, 0.0, 1000.0);
        INS_SPEED = b.comment("Combat Instinct: movement speed bonus while it lasts.").defineInRange("instinctSpeed", 0.20, 0.0, 1.0);
        INS_ASPD = b.comment("Combat Instinct: attack speed bonus while it lasts.").defineInRange("instinctAttackSpeed", 0.15, 0.0, 1.0);
        INS_SEC = b.comment("Combat Instinct: seconds it lasts.").defineInRange("instinctSeconds", 10, 1, 60);
        INS_CD = b.comment("Combat Instinct cooldown in ticks.").defineInRange("instinctCooldownTicks", 400, 1, 12000);
        b.pop();

        b.push("ki");
        SPH_KI = b.comment("Charged Ki Sphere: Ki per use.").defineInRange("sphereCostPercent", 10.0, 0.0, 100.0);
        SPH_ST = b.comment("Charged Ki Sphere: stamina per use.").defineInRange("sphereStamina", 20.0, 0.0, 1000.0);
        SPH_DMG = b.comment("Charged Ki Sphere: damage of the blast before the balance law.").defineInRange("sphereDamage", 45.0, 0.0, 100000.0);
        SPH_RANGE = b.comment("Charged Ki Sphere: how far (blocks) it flies.").defineInRange("sphereRange", 40.0, 8.0, 96.0);
        SPH_SPEED = b.comment("Charged Ki Sphere: blocks a tick (slow: it lands after a short flight).").defineInRange("sphereSpeed", 0.8, 0.2, 2.0);
        SPH_RAD = b.comment("Charged Ki Sphere: blast radius in blocks.").defineInRange("sphereRadius", 3.5, 1.0, 8.0);
        SPH_CD = b.comment("Charged Ki Sphere cooldown in ticks.").defineInRange("sphereCooldownTicks", 300, 1, 12000);
        WAV_KI = b.comment("Ki Wave: Ki to release it, percent of the maximum.").defineInRange("waveCostPercent", 3.0, 0.0, 100.0);
        WAV_COST = b.comment("Ki Wave: Ki per second while it channels, percent of the maximum.").defineInRange("waveChannelCostPercentPerSecond", 3.0, 0.0, 50.0);
        WAV_DMG = b.comment("Ki Wave: damage of each beat (every four ticks) before the balance law.").defineInRange("waveDamage", 6.0, 0.0, 10000.0);
        WAV_RANGE = b.comment("Ki Wave: range in blocks.").defineInRange("waveRange", 40.0, 8.0, 96.0);
        WAV_MIN = b.comment("Ki Wave: ticks of charging below which the release fizzles.").defineInRange("waveMinChargeTicks", 10, 0, 100);
        WAV_MAX = b.comment("Ki Wave: the longest it channels, in ticks (at most 100).").defineInRange("waveMaxChannelTicks", 60, 10, 100);
        WAV_CD = b.comment("Ki Wave cooldown in ticks.").defineInRange("waveCooldownTicks", 400, 1, 12000);
        REI_DRAIN = b.comment("Ki Reinforcement: Ki per second while on, percent of the maximum.").defineInRange("reinforceDrainPercentPerSecond", 0.6, 0.0, 20.0);
        REI_DMG = b.comment("Ki Reinforcement: bonus to the Saiyan's blows (0.2 = +20%).").defineInRange("reinforceDamage", 0.20, 0.0, 1.5);
        REI_DEF = b.comment("Ki Reinforcement: defence bonus (0.15 = damage taken divided by 1.15).").defineInRange("reinforceDefense", 0.15, 0.0, 1.5);
        AFT_COST = b.comment("Afterimage Technique: Ki per second while it is on, percent of the maximum.").defineInRange("afterimageCostPercentPerSecond", 0.3, 0.0, 20.0);
        b.pop();

        b.push("gravity");
        GRAVITY_ON = b.comment("Gravity Training: a toggle that weighs the Saiyan down and gives training points under the daily caps. false = off.").define("enabled", true);
        GRV_SEC = b.comment("Seconds of gravity training that adapt the body by one level (levels 1 to 10).").defineInRange("secondsPerLevel", 300, 10, 100000);
        b.pop();

        b.push("avatar");
        AVATAR_ON = b.comment("Giant Ki Avatar (Perfected / True Ultra Instinct): a translucent silver construct that fights for a short time. false = off.").define("enabled", true);
        AVATAR_FORM = b.comment("Only the Perfected and the True Ultra Instinct forms can raise it. false = any awakened Saiyan.").define("needsUltraInstinct", true);
        AVA_KI = b.comment("Giant Ki Avatar: Ki to raise it, percent of the maximum.").defineInRange("costPercent", 25.0, 0.0, 100.0);
        AVA_DRAIN = b.comment("Giant Ki Avatar: Ki per second while it stands, percent of the maximum.").defineInRange("drainPercentPerSecond", 2.0, 0.0, 50.0);
        AVA_SEC = b.comment("Giant Ki Avatar: the longest it stands, in seconds (at most 45).").defineInRange("seconds", 20.0, 1.0, 45.0);
        AVA_SCALE = b.comment("Giant Ki Avatar: its height in blocks as drawn (at most 12).").defineInRange("scale", 8.0, 3.0, 12.0);
        AVA_DMG = b.comment("Giant Ki Avatar: damage of one swing before the balance law (a swing a second, at most six foes).").defineInRange("swingDamage", 40.0, 0.0, 100000.0);
        AVA_REACH = b.comment("Giant Ki Avatar: how far (blocks) in front of the Saiyan a swing reaches.").defineInRange("reach", 8.0, 2.0, 16.0);
        AVA_CD = b.comment("Giant Ki Avatar cooldown in ticks.").defineInRange("cooldownTicks", 3600, 1, 72000);
        b.pop();

        b.push("ritual");
        RITUAL_ON = b.comment("The Super Saiyan God ritual (/saiyan ritual): helpers stand in a ring and sneak, the caster is given Super Saiyan God and the Divine stage. false = off.").define("enabled", true);
        RIT_HELPERS = b.comment("Other players needed in the ring (the spec: five besides the caster; at most 8).").defineInRange("helpers", 5, 1, 8);
        RIT_SEC = b.comment("Seconds the ring must hold.").defineInRange("seconds", 10, 2, 30);
        RIT_BP = b.comment("Battle Power the caster needs (used unless owner_decisions.ritualUsesExistencePoints).").defineInRange("battlePower", 1_000_000.0, 100.0, SaiyanRules.BP_HARD_CAP);
        RIT_EP = b.comment("Tensura Existence Points the caster needs when owner_decisions.ritualUsesExistencePoints is on (the spec: 100,000,000).").defineInRange("existencePoints", 1.0e8, 1.0, 1.0e15);
        RIT_MAX = b.comment("The most rituals running on the whole server at once.").defineInRange("maxAtOnce", 3, 1, 20);
        RIT_FAIL_CD = b.comment("Seconds before a caster can try again after a broken circle.").defineInRange("retrySeconds", 60, 0, 3600);
        GIFTS_ON = b.comment("The Divine Saiyan gift: the caster's tame animals nearby are blessed (regeneration and resistance) and the stage buffs rise. false = only the form.").define("gifts", true);
        GIFT_TARGETS = b.comment("The most tame animals one ritual blesses (at most 8).").defineInRange("giftTargets", 8, 0, 8);
        GIFT_MIN = b.comment("Minutes the blessing lasts.").defineInRange("giftMinutes", 5.0, 0.5, 60.0);
        b.pop();

        b.push("senses");
        GOD_MASK = b.comment("God Ki hides from the Saiyan Senses radar of a Saiyan who has no God Ki himself (Super Saiyan God forms and the Divine stage). false = the radar sees everyone as before.").define("godKiMask", true);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private SaiyanPhaseConfig() {}

    private static boolean on(ModConfigSpec.BooleanValue v, boolean def) { try { return v.get(); } catch (RuntimeException notLoaded) { return def; } }

    private static double num(ModConfigSpec.DoubleValue v, double def, double lo, double hi) {
        try { return SaiyanRules.clamp(v.get(), lo, hi); } catch (RuntimeException notLoaded) { return def; }
    }

    private static int whole(ModConfigSpec.IntValue v, int def, int lo, int hi) {
        try { return (int) SaiyanRules.clamp(v.get(), lo, hi); } catch (RuntimeException notLoaded) { return def; }
    }

    // ---- master and owner decisions
    public static boolean enabled() { return on(TECH_ON, true); }
    public static boolean tensuraSource() { return on(TENSURA_SRC, true); }
    public static boolean kaiokenExtra() { return on(KAIO_EXT, false); }
    public static boolean apeForms() { return on(APE_ON, false); }
    public static boolean ritualUsesEp() { return on(RITUAL_EP, false); }
    public static boolean branchKey() { return on(BRANCH_KEY, false); }
    public static boolean blockAwakening() { return on(BLOCK_AWAKEN, false); }
    public static boolean godKiMask() { return on(GOD_MASK, true); }

    // ---- race
    public static boolean realRace() { return on(REAL_RACE, false); }
    public static double raceScale() { return num(RACE_SCALE, 1.0, 0, 2); }
    public static SaiyanRaceRules.Need raceNeed() {
        return new SaiyanRaceRules.Need(num(PRIMAL_BP, 400_000.0, 100, SaiyanRules.BP_HARD_CAP), num(AUTO_BP, 8_000_000.0, 100, SaiyanRules.BP_HARD_CAP), whole(AUTO_ZENKAI, 5, 0, 1000));
    }

    // ---- physical
    public static double strikeCost() { return num(STR_KI, 4.0, 0, 100); }
    public static double strikeStamina() { return num(STR_ST, 15.0, 0, 1000); }
    public static double strikeDamage() { return num(STR_DMG, 20.0, 0, 10000); }
    public static double strikeRange() { return num(STR_RANGE, 6.0, 2, 12); }
    public static double strikeKnockback() { return num(STR_KB, 1.4, 0, 4); }
    public static int strikeCooldown() { return whole(STR_CD, 160, 1, 12000); }
    public static int strikeWind() { return whole(STR_WIND, 8, 1, 40); }
    public static double vanishCost() { return num(VAN_KI, 3.0, 0, 100); }
    public static double vanishStamina() { return num(VAN_ST, 10.0, 0, 1000); }
    public static double vanishDistance() { return SaiyanPhaseRules.vanishDistance(num(VAN_DIST, 8.0, 2, 16)); }
    public static int vanishCooldown() { return whole(VAN_CD, 100, 1, 12000); }
    public static double rushCost() { return num(RSH_KI, 4.0, 0, 100); }
    public static double rushStamina() { return num(RSH_ST, 12.0, 0, 1000); }
    public static double rushDamage() { return num(RSH_DMG, 14.0, 0, 10000); }
    public static int rushCooldown() { return whole(RSH_CD, 140, 1, 12000); }
    public static double counterCost() { return num(CNT_KI, 2.0, 0, 100); }
    public static double counterStamina() { return num(CNT_ST, 10.0, 0, 1000); }
    public static double counterFactor() { return num(CNT_FACTOR, 2.0, 0, 10); }
    public static int counterCooldown() { return whole(CNT_CD, 160, 1, 12000); }
    public static int counterWindow() { return whole(CNT_WINDOW, 12, 2, 60); }
    public static double breakerCost() { return num(BRK_KI, 5.0, 0, 100); }
    public static double breakerStamina() { return num(BRK_ST, 20.0, 0, 1000); }
    public static double breakerDamage() { return num(BRK_DMG, 25.0, 0, 10000); }
    public static double breakerRadius() { return num(BRK_RAD, 4.0, 1, 8); }
    public static int breakerCooldown() { return whole(BRK_CD, 240, 1, 12000); }
    public static double instinctCost() { return num(INS_KI, 3.0, 0, 100); }
    public static double instinctStamina() { return num(INS_ST, 10.0, 0, 1000); }
    public static double instinctSpeed() { return num(INS_SPEED, 0.20, 0, 1); }
    public static double instinctAttackSpeed() { return num(INS_ASPD, 0.15, 0, 1); }
    public static int instinctSeconds() { return whole(INS_SEC, 10, 1, 60); }
    public static int instinctCooldown() { return whole(INS_CD, 400, 1, 12000); }

    // ---- Ki
    public static double sphereCost() { return num(SPH_KI, 10.0, 0, 100); }
    public static double sphereStamina() { return num(SPH_ST, 20.0, 0, 1000); }
    public static double sphereDamage() { return num(SPH_DMG, 45.0, 0, 100000); }
    public static double sphereRange() { return num(SPH_RANGE, 40.0, 8, 96); }
    public static double sphereSpeed() { return num(SPH_SPEED, 0.8, 0.2, 2); }
    public static double sphereRadius() { return num(SPH_RAD, 3.5, 1, 8); }
    public static int sphereCooldown() { return whole(SPH_CD, 300, 1, 12000); }
    public static double waveCost() { return num(WAV_KI, 3.0, 0, 100); }
    public static double waveChannelCost() { return num(WAV_COST, 3.0, 0, 50); }
    public static double waveDamage() { return num(WAV_DMG, 6.0, 0, 10000); }
    public static double waveRange() { return num(WAV_RANGE, 40.0, 8, 96); }
    public static int waveMinCharge() { return whole(WAV_MIN, 10, 0, 100); }
    public static int waveMaxChannel() { return whole(WAV_MAX, 60, 10, 100); }
    public static int waveCooldown() { return whole(WAV_CD, 400, 1, 12000); }
    public static double reinforceDrain() { return SaiyanPhaseRules.reinforceDrain(num(REI_DRAIN, 0.6, 0, 20)); }
    public static double reinforceDamage() { return num(REI_DMG, 0.20, 0, 1.5); }
    public static double reinforceDefense() { return num(REI_DEF, 0.15, 0, 1.5); }
    public static double afterimageCost() { return num(AFT_COST, 0.3, 0, 20); }

    // ---- gravity, avatar, ritual
    public static boolean gravityEnabled() { return on(GRAVITY_ON, true); }
    public static int gravitySecondsPerLevel() { return whole(GRV_SEC, 300, 10, 100000); }
    public static boolean avatarEnabled() { return on(AVATAR_ON, true); }
    public static boolean avatarNeedsForm() { return on(AVATAR_FORM, true); }
    public static double avatarCost() { return num(AVA_KI, 25.0, 0, 100); }
    public static double avatarDrain() { return num(AVA_DRAIN, 2.0, 0, 50); }
    public static int avatarTicks() { return SaiyanPhaseRules.avatarTicks(num(AVA_SEC, 20.0, 1, 45)); }
    public static double avatarScale() { return SaiyanPhaseRules.avatarScale(num(AVA_SCALE, 8.0, 3, 12)); }
    public static double avatarDamage() { return num(AVA_DMG, 40.0, 0, 100000); }
    public static double avatarReach() { return num(AVA_REACH, 8.0, 2, 16); }
    public static int avatarCooldown() { return whole(AVA_CD, 3600, 1, 72000); }
    public static boolean ritualEnabled() { return on(RITUAL_ON, true); }
    public static int ritualHelpers() { return SaiyanRitualRules.helpers(whole(RIT_HELPERS, 5, 1, 8)); }
    public static int ritualTicks() { return SaiyanRitualRules.ticks(whole(RIT_SEC, 10, 2, 30) * 20); }
    public static double ritualBp() { return num(RIT_BP, 1_000_000.0, 100, SaiyanRules.BP_HARD_CAP); }
    public static double ritualEp() { return num(RIT_EP, 1.0e8, 1, 1.0e15); }
    public static int ritualMax() { return whole(RIT_MAX, 3, 1, 20); }
    public static int ritualRetryTicks() { return whole(RIT_FAIL_CD, 60, 0, 3600) * 20; }
    public static boolean giftsEnabled() { return on(GIFTS_ON, true); }
    public static int giftTargets() { return whole(GIFT_TARGETS, 8, 0, SaiyanPhaseRules.MAX_GIFT_TARGETS); }
    public static int giftTicks() { return (int) (num(GIFT_MIN, 5.0, 0.5, 60) * 1200.0); }
}
