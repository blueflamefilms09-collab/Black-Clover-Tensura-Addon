package com.newuniverse.nusmp.saiyan;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the SERVER settings of the Saiyan system ({@code serverconfig/nusmp-saiyan-server.toml}, 3.63.0). Every value changes gameplay, so every value is a server setting (rule 9); each one is clamped
 * again when it is read, so a hand-edited file cannot go past a bound. {@code enabled = false} restores the game as it was: no Saiyan bonus, no Battle Power gain, no Ki attack, the modifiers are removed.
 * The racial table is the spec's starting table (a balance target, not final); every entry is a fraction (0.35 = +35%) that grows with Battle Power ({@code bonusGrowth}) up to its own cap.
 */
public final class SaiyanConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED, HIT_PLAYERS, PLAYERS_GIVE_BP, USE_EP;
    private static final ModConfigSpec.DoubleValue KI_BASE, STRENGTH, SPEED, DURABILITY, ATTACK_SPEED, JUMP, STAMINA_CAP, KI_CAPACITY, KNOCKBACK, COMBAT_XP, GROWTH,
            CAP_STRENGTH, CAP_SPEED, CAP_DURABILITY, CAP_ATTACK_SPEED, CAP_JUMP, CAP_STAMINA, CAP_KI_CAPACITY, CAP_KNOCKBACK, CAP_COMBAT_XP, STAMINA_BASE, STAMINA_REGEN,
            KI_REGEN_PERCENT, GAIN_PER_KILL, DAILY_FRACTION, DAILY_FLOOR, VICTIM_SCALE, EP_WEIGHT, MIN_RATIO, MAX_BP,
            BLAST_COST, BLAST_DAMAGE, BLAST_RANGE, BARRAGE_COST, BARRAGE_STAMINA, BARRAGE_SHARE, KI_DAMAGE_GAIN;
    private static final ModConfigSpec.IntValue MIN_VICTIM_AGE, REPEAT_LIMIT, REPEAT_WINDOW, BLAST_COOLDOWN, BARRAGE_COOLDOWN, BARRAGE_RAYS, AWAKEN_SECONDS, BLAST_STAMINA_X10;

    // ---- 3.65.0: Stage 2 (flight, senses, passives), the Goku path and the forms
    private static final ModConfigSpec.BooleanValue FLIGHT_ON, ZENKAI_ON, GOKU_ON, FORMS_ON;
    private static final ModConfigSpec.DoubleValue FLIGHT_SPEED, FLIGHT_KI, FLIGHT_START, BURST_COST, BURST_STAMINA, BURST_SPEED, SENSE_RADIUS, SENSE_COST, SUPPRESS_COST,
            REFLEX_BASE, REFLEX_MAX, REFLEX_STAMINA, ADAPT_MAX, ZENKAI_NEAR, ZENKAI_RECOVER, ZENKAI_RATIO, ZENKAI_GAIN, ZENKAI_DECAY, TRAIN_SCALE,
            AWAKEN_BP, HARD_RATIO, KAME_DAMAGE, KAME_RANGE, KAME_COST, KAME_CHARGE, KAME_CHARGE_KI, RSP_DAMAGE, RSP_COST, RSP_STAMINA, FORM_DAMAGE, FORM_DRAIN;
    private static final ModConfigSpec.IntValue HOVER_SECONDS, BURST_COOLDOWN, SENSE_COOLDOWN, SENSE_SECONDS, REFLEX_COOLDOWN, ADAPT_HITS, ADAPT_RESET, ZENKAI_COOLDOWN, ZENKAI_PER_DAY,
            AWAKEN_TECH, AWAKEN_FLIGHT, AWAKEN_SENSE, AWAKEN_HARD, AWAKEN_ZENKAI, TRIAL_KILLS, KAME_COOLDOWN, KAME_MIN_TICKS, RSP_WINDOW, RSP_GAP, RSP_COOLDOWN, RSP_PAPER_MAX, FORM_MASTERY_DAY;
    private static final ModConfigSpec.IntValue[] FLIGHT_TIER_SECONDS = new ModConfigSpec.IntValue[4], SENSE_TIER_SCANS = new ModConfigSpec.IntValue[3];
    /** Per form: {minBattlePower, damage, defense, kiDrainPercentPerSecond}. */
    private static final java.util.Map<String, ModConfigSpec.DoubleValue[]> FORM_CFG = new java.util.LinkedHashMap<>();
    // ---- 3.66.0: the four new techniques
    private static final ModConfigSpec.BooleanValue TECH_ON;
    private static final ModConfigSpec.DoubleValue IT_RANGE, IT_BLINK, IT_COST, BAR_POOL, BAR_COST, SB_DAMAGE, SB_RADIUS, SB_RANGE, SB_CHARGE, SB_SOURCE_RADIUS, SB_COST, SB_CHARGE_KI,
            MET_DAMAGE, MET_COST, MET_STAMINA, MET_RANGE;
    // ---- 3.71.0: Dragon Fist, God Bind, Ki Deflection, Energy Transfer
    private static final ModConfigSpec.BooleanValue TECH3_ON;
    private static final ModConfigSpec.DoubleValue DRG_DAMAGE, DRG_COST, DRG_STAMINA, DRG_RANGE, DRG_UNLOCK, BND_RANGE, BND_COST, BND_SECONDS, BND_BOSS, BND_PLAYER, DEF_COST, DEF_RADIUS, DEF_BOOST, ENG_COST, ENG_LOSS, ENG_CAP, ENG_RANGE;
    private static final ModConfigSpec.IntValue DRG_STEPS, DRG_COOLDOWN, BND_COOLDOWN, DEF_SECONDS, DEF_COOLDOWN, ENG_COOLDOWN;
    private static final ModConfigSpec.IntValue IT_COOLDOWN, BAR_SECONDS, BAR_COOLDOWN, SB_COOLDOWN, SB_MIN_TICKS, SB_TRAVEL_MAX, MET_HITS, MET_INTERVAL, MET_COOLDOWN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("saiyan");
        ENABLED = b.comment("Saiyans: a warrior race with Ki, Battle Power and Ki attacks. false = the feature is off: no bonus, no Battle Power gain, no Ki attack.").define("enabled", true);
        b.push("race");
        STRENGTH = b.comment("Physical Strength: bonus to melee damage (0.35 = +35%). Done with a damage calculation, not an attribute.").defineInRange("strength", 0.35, 0.0, 5.0);
        SPEED = b.comment("Physical Speed: bonus to movement speed (a small, capped attribute modifier).").defineInRange("speed", 0.20, 0.0, 2.0);
        DURABILITY = b.comment("Physical Durability: damage taken is divided by (1 + this). Done with a damage calculation.").defineInRange("durability", 0.30, 0.0, 5.0);
        ATTACK_SPEED = b.comment("Attack speed bonus.").defineInRange("attackSpeed", 0.15, 0.0, 2.0);
        JUMP = b.comment("Jump height bonus.").defineInRange("jump", 0.25, 0.0, 2.0);
        STAMINA_CAP = b.comment("Stamina capacity bonus over the base stamina.").defineInRange("staminaCapacity", 0.40, 0.0, 5.0);
        KI_CAPACITY = b.comment("Ki capacity bonus: Ki IS the player's Tensura magicule pool, so this raises the maximum magicule (a Tensura attribute modifier) while the player is a Saiyan.").defineInRange("kiCapacity", 0.30, 0.0, 5.0);
        KI_BASE = b.comment("Flat Ki every Saiyan has on top of the percentage above (the spec's \"Ki capacity 100\"): added to the maximum magicule, so a Saiyan with a tiny pool still has Ki.").defineInRange("kiBase", 100.0, 0.0, 1.0e7);
        KNOCKBACK = b.comment("Knockback resistance (0.15 = 15% of knockback ignored).").defineInRange("knockbackResistance", 0.15, 0.0, 1.0);
        COMBAT_XP = b.comment("Combat experience gain: more Battle Power from each qualifying kill.").defineInRange("combatExperience", 0.20, 0.0, 5.0);
        GROWTH = b.comment("How much the racial bonuses grow with Battle Power: bonus = base * (1 + bonusGrowth * growth), growth 0..1 over the six ranks. 0 = fixed bonuses.").defineInRange("bonusGrowth", 1.0, 0.0, 10.0);
        b.push("caps");
        CAP_STRENGTH = b.comment("The most each bonus can ever reach, whatever the base and the growth are.").defineInRange("strength", 1.5, 0.0, 10.0);
        CAP_SPEED = b.defineInRange("speed", 0.5, 0.0, 2.0);
        CAP_DURABILITY = b.defineInRange("durability", 1.5, 0.0, 10.0);
        CAP_ATTACK_SPEED = b.defineInRange("attackSpeed", 0.5, 0.0, 2.0);
        CAP_JUMP = b.defineInRange("jump", 0.8, 0.0, 2.0);
        CAP_STAMINA = b.defineInRange("staminaCapacity", 2.0, 0.0, 10.0);
        CAP_KI_CAPACITY = b.defineInRange("kiCapacity", 2.0, 0.0, 10.0);
        CAP_KNOCKBACK = b.defineInRange("knockbackResistance", 0.8, 0.0, 1.0);
        CAP_COMBAT_XP = b.defineInRange("combatExperience", 1.0, 0.0, 10.0);
        b.pop();
        b.pop();
        b.push("resources");
        STAMINA_BASE = b.comment("Base stamina points before the racial bonus.").defineInRange("staminaBase", 100.0, 10.0, 10000.0);
        STAMINA_REGEN = b.comment("Stamina regained per second (a third of that while sprinting).").defineInRange("staminaRegenPerSecond", 6.0, 0.0, 1000.0);
        KI_REGEN_PERCENT = b.comment("Extra Ki regained per second, in percent of the maximum, on top of Tensura's own magicule regeneration (only after the Ki has awakened). 0 = none.").defineInRange("kiRegenPercentPerSecond", 2.0, 0.0, 50.0);
        AWAKEN_SECONDS = b.comment("Ki Awakening: seconds of sneaking still (on the ground, not hurt for five seconds) that awaken the Ki.").defineInRange("awakenSeconds", 10, 1, 300);
        b.pop();
        b.push("battlepower");
        MAX_BP = b.comment("The highest Battle Power any Saiyan can reach.").defineInRange("max", 1.0e9, 100.0, SaiyanRules.BP_HARD_CAP);
        GAIN_PER_KILL = b.comment("Share of the current Battle Power one full-value kill gives (0.01 = 1%).").defineInRange("gainPerKill", 0.01, 0.0, 0.5);
        DAILY_FRACTION = b.comment("Most Battle Power that can be gained in one game day, as a share of the current value.").defineInRange("dailyCapFraction", 0.10, 0.0, 1.0);
        DAILY_FLOOR = b.comment("... but never less than this many points a day (so a new Saiyan can start).").defineInRange("dailyCapFloor", 20.0, 0.0, 1.0e9);
        MIN_RATIO = b.comment("A creature weaker than this share of the Saiyan's own Battle Power gives nothing.").defineInRange("minRatio", 0.25, 0.0, 0.95);
        VICTIM_SCALE = b.comment("Scales a creature's health, attack and armour into Battle Power units.").defineInRange("victimPowerScale", 2.0, 0.0, 1000.0);
        USE_EP = b.comment("Count the victim's Tensura Existence Points in how hard it is. false = health, attack and armour only.").define("useTensuraEp", true);
        EP_WEIGHT = b.comment("Share of the victim's Tensura Existence Points added to its power.").defineInRange("epWeight", 0.05, 0.0, 100.0);
        MIN_VICTIM_AGE = b.comment("A creature must have lived this many ticks to give Battle Power (stops spawn-and-kill farms). 600 = 30 seconds.").defineInRange("minVictimAgeTicks", 600, 0, 72000);
        REPEAT_LIMIT = b.comment("Kills of the same creature type that halve its value (1 / (1 + kills / limit)).").defineInRange("repeatLimit", 5, 1, 1000);
        REPEAT_WINDOW = b.comment("Ticks a repeat-kill memory lasts (12000 = ten minutes).").defineInRange("repeatWindowTicks", 12000, 20, 1_728_000);
        PLAYERS_GIVE_BP = b.comment("Killing another player gives Battle Power. Off by default (no farming friends).").define("playersGiveBattlePower", false);
        b.pop();
        b.push("kiattacks");
        BLAST_COST = b.comment("Ki Blast cost, in percent of the maximum Ki.").defineInRange("blastCostPercent", 2.0, 0.0, 100.0);
        BLAST_DAMAGE = b.comment("Ki Blast base damage before the balance law (which scales it by the caster's Existence Points).").defineInRange("blastDamage", 6.0, 0.0, 1000.0);
        BLAST_RANGE = b.comment("Ki Blast range in blocks.").defineInRange("blastRange", 24.0, 4.0, 64.0);
        BLAST_COOLDOWN = b.comment("Ki Blast cooldown in ticks.").defineInRange("blastCooldownTicks", 10, 1, 1200);
        BLAST_STAMINA_X10 = b.comment("Ki Blast stamina cost, in tenths of a point.").defineInRange("blastStaminaTenths", 10, 0, 10000);
        BARRAGE_COST = b.comment("Rapid Ki Barrage cost, in percent of the maximum Ki.").defineInRange("barrageCostPercent", 6.0, 0.0, 100.0);
        BARRAGE_STAMINA = b.comment("Rapid Ki Barrage stamina cost.").defineInRange("barrageStamina", 10.0, 0.0, 1000.0);
        BARRAGE_RAYS = b.comment("Rapid Ki Barrage: how many small blasts (each one raycast; at most 8).").defineInRange("barrageRays", 5, 1, 8);
        BARRAGE_SHARE = b.comment("Each barrage blast deals this share of a Ki Blast.").defineInRange("barrageShare", 0.35, 0.0, 2.0);
        BARRAGE_COOLDOWN = b.comment("Rapid Ki Barrage cooldown in ticks.").defineInRange("barrageCooldownTicks", 60, 1, 2400);
        KI_DAMAGE_GAIN = b.comment("How much Ki attack damage grows with Battle Power: base * (1 + gain * growth).").defineInRange("kiDamageGrowth", 1.5, 0.0, 20.0);
        HIT_PLAYERS = b.comment("Ki attacks may hit other players (they must still be foes: PvP setting, teams and protected claims are respected). false = creatures only.").define("hitPlayers", true);
        b.pop();

        // ------------------------------------------------------------ 3.65.0: Ki Flight
        b.push("flight");
        FLIGHT_ON = b.comment("Ki Flight (key G). false = no Saiyan can fly with Ki.").define("enabled", true);
        FLIGHT_SPEED = b.comment("Multiplier on the flying speed of every tier (creative flight is 0.05; the result is cut at 0.30). 1 = the tiers as designed.").defineInRange("speedScale", 1.0, 0.0, 3.0);
        FLIGHT_KI = b.comment("Multiplier on the Ki flight costs per second.").defineInRange("kiScale", 1.0, 0.0, 10.0);
        FLIGHT_START = b.comment("Ki cost of taking off, in percent of the maximum.").defineInRange("startCostPercent", 1.0, 0.0, 100.0);
        HOVER_SECONDS = b.comment("A Novice can only hover this many seconds before landing.").defineInRange("hoverSeconds", 10, 1, 300);
        FLIGHT_TIER_SECONDS[0] = b.comment("Flight seconds (really moving) that earn each tier: Apprentice, Adept, Expert, Master.").defineInRange("apprenticeSeconds", 120, 1, 1_000_000);
        FLIGHT_TIER_SECONDS[1] = b.defineInRange("adeptSeconds", 600, 1, 1_000_000);
        FLIGHT_TIER_SECONDS[2] = b.defineInRange("expertSeconds", 1800, 1, 1_000_000);
        FLIGHT_TIER_SECONDS[3] = b.defineInRange("masterSeconds", 5400, 1, 1_000_000);
        BURST_COST = b.comment("Master burst flight: Ki cost in percent of the maximum.").defineInRange("burstCostPercent", 4.0, 0.0, 100.0);
        BURST_STAMINA = b.comment("Master burst flight: stamina cost.").defineInRange("burstStamina", 15.0, 0.0, 1000.0);
        BURST_COOLDOWN = b.comment("Master burst flight cooldown in ticks.").defineInRange("burstCooldownTicks", 80, 1, 2400);
        BURST_SPEED = b.comment("Master burst flight: blocks per tick of the dash (1.8 = 36 blocks a second).").defineInRange("burstSpeed", 1.8, 0.5, 3.0);
        b.pop();

        // ------------------------------------------------------------ Saiyan Senses and Ki Suppression
        b.push("senses");
        SENSE_RADIUS = b.comment("The largest radius the senses can reach whatever the tier (tiers: 16, 32, 64, 128 blocks).").defineInRange("maxRadius", 128.0, 16.0, 256.0);
        SENSE_COST = b.comment("Ki cost of one sensing, in percent of the maximum.").defineInRange("costPercent", 1.0, 0.0, 100.0);
        SENSE_COOLDOWN = b.comment("Ticks between two sensings.").defineInRange("cooldownTicks", 100, 1, 2400);
        SENSE_SECONDS = b.comment("Seconds the radar stays on after a sensing (it refreshes every two seconds).").defineInRange("durationSeconds", 10, 2, 60);
        SENSE_TIER_SCANS[0] = b.comment("Sensings that earn each tier: Intermediate, Advanced, Master.").defineInRange("intermediateScans", 30, 1, 1_000_000);
        SENSE_TIER_SCANS[1] = b.defineInRange("advancedScans", 150, 1, 1_000_000);
        SENSE_TIER_SCANS[2] = b.defineInRange("masterScans", 500, 1, 1_000_000);
        SUPPRESS_COST = b.comment("Ki Suppression: Ki per second while it is on, in percent of the maximum.").defineInRange("suppressCostPercentPerSecond", 0.2, 0.0, 50.0);
        b.pop();

        // ------------------------------------------------------------ Warrior's Reflex, Battle Adaptation, Zenkai
        b.push("passives");
        REFLEX_BASE = b.comment("Warrior's Reflex: dodge chance at the lowest Battle Power (0.03 = 3%). Never above 75% from any source.").defineInRange("reflexBase", 0.03, 0.0, 0.75);
        REFLEX_MAX = b.comment("Warrior's Reflex: dodge chance at the highest rank.").defineInRange("reflexMax", 0.15, 0.0, 0.75);
        REFLEX_STAMINA = b.comment("Stamina one dodge costs.").defineInRange("reflexStamina", 8.0, 0.0, 1000.0);
        REFLEX_COOLDOWN = b.comment("Ticks before the next reflex dodge.").defineInRange("reflexCooldownTicks", 40, 1, 2400);
        ADAPT_MAX = b.comment("Battle Adaptation: the most resistance to one kind of damage (0.15 = 15%).").defineInRange("adaptationMax", 0.15, 0.0, 0.5);
        ADAPT_HITS = b.comment("Hits of one kind of damage that reach the full resistance.").defineInRange("adaptationHits", 10, 1, 1000);
        ADAPT_RESET = b.comment("Ticks without damage after which the adaptation is gone (the fight is over).").defineInRange("adaptationResetTicks", 600, 20, 72000);
        ZENKAI_ON = b.comment("Zenkai: a permanent Battle Power gain for surviving a near-death against a dangerous foe. false = off.").define("zenkaiEnabled", true);
        ZENKAI_NEAR = b.comment("A blow that leaves you at or under this share of your health is a near-death.").defineInRange("zenkaiNearDeath", 0.20, 0.01, 0.9);
        ZENKAI_RECOVER = b.comment("You must recover to this share of your health, unhurt for five seconds, for the Zenkai to count.").defineInRange("zenkaiRecover", 0.60, 0.05, 1.0);
        ZENKAI_RATIO = b.comment("The attacker's power must be at least this share of your Battle Power (a weak foe is no dangerous fight).").defineInRange("zenkaiMinRatio", 0.5, 0.0, 10.0);
        ZENKAI_GAIN = b.comment("Share of the current Battle Power one Zenkai gives (0.05 = 5%), before the diminishing returns.").defineInRange("zenkaiGain", 0.05, 0.0, 1.0);
        ZENKAI_DECAY = b.comment("Each Zenkai already received divides the next by (1 + this * count).").defineInRange("zenkaiDecay", 0.25, 0.0, 10.0);
        ZENKAI_COOLDOWN = b.comment("Ticks between two Zenkai (24000 = one game day).").defineInRange("zenkaiCooldownTicks", 24000, 0, 1_728_000);
        ZENKAI_PER_DAY = b.comment("Zenkai per game day at most.").defineInRange("zenkaiPerDay", 1, 0, 10);
        TRAIN_SCALE = b.comment("Multiplier on the daily caps of the four training kinds (physical, Ki, martial, divine).").defineInRange("trainingScale", 1.0, 0.0, 5.0);
        b.pop();

        // ------------------------------------------------------------ the Goku path
        b.push("goku");
        GOKU_ON = b.comment("Son Goku, King of Martial Arts: the Ultimate Skill a Saiyan can awaken (Kamehameha, Rock Scissors 'N' Paper and the higher forms). false = off.").define("enabled", true);
        AWAKEN_BP = b.comment("Awakening: the Battle Power needed (100000 = Legendary Warrior).").defineInRange("awakenBattlePower", 100_000.0, 100.0, SaiyanRules.BP_HARD_CAP);
        AWAKEN_TECH = b.comment("Awakening: universal techniques that must be mastered (of the five: Ki Blast, Rapid Ki Barrage, Ki Flight, Saiyan Senses, Ki Suppression).").defineInRange("awakenTechniques", 5, 1, 5);
        AWAKEN_FLIGHT = b.comment("Awakening: the Ki Flight tier needed (0 Novice .. 4 Master). Advanced Ki control = Adept flight (2) and Intermediate senses (1).").defineInRange("awakenFlightTier", 2, 0, 4);
        AWAKEN_SENSE = b.comment("Awakening: the Saiyan Senses tier needed (0 Beginner .. 3 Master).").defineInRange("awakenSenseTier", 1, 0, 3);
        AWAKEN_HARD = b.comment("Awakening: challenging foes beaten with martial arts.").defineInRange("awakenHardMartialKills", 1, 0, 1000);
        AWAKEN_ZENKAI = b.comment("Awakening: Zenkai (surviving a major battle against a stronger enemy) needed.").defineInRange("awakenZenkai", 1, 0, 1000);
        TRIAL_KILLS = b.comment("Trial of the Martial King: hard martial victories needed.").defineInRange("trialVictories", 10, 0, 1000);
        HARD_RATIO = b.comment("A foe is challenging when its power is at least this share of your Battle Power (and it still teaches you something).").defineInRange("challengingRatio", 0.4, 0.0, 10.0);
        KAME_DAMAGE = b.comment("Kamehameha: damage of a full charge before the balance law.").defineInRange("kamehamehaDamage", 40.0, 0.0, 10000.0);
        KAME_RANGE = b.comment("Kamehameha: range in blocks of a full charge.").defineInRange("kamehamehaRange", 56.0, 8.0, 96.0);
        KAME_COST = b.comment("Kamehameha: Ki of the release for a full charge, in percent of the maximum.").defineInRange("kamehamehaCostPercent", 12.0, 0.0, 100.0);
        KAME_CHARGE = b.comment("Kamehameha: seconds of a full charge before mastery.").defineInRange("kamehamehaChargeSeconds", 6.0, 1.0, 30.0);
        KAME_CHARGE_KI = b.comment("Kamehameha: Ki per second while charging, in percent of the maximum.").defineInRange("kamehamehaChargeCostPercentPerSecond", 1.0, 0.0, 50.0);
        KAME_COOLDOWN = b.comment("Kamehameha cooldown in ticks.").defineInRange("kamehamehaCooldownTicks", 100, 1, 2400);
        KAME_MIN_TICKS = b.comment("A release before this many ticks fizzles.").defineInRange("kamehamehaMinTicks", 10, 0, 200);
        RSP_DAMAGE = b.comment("Rock, Scissors 'N' Paper: base damage of a blow before the balance law.").defineInRange("rspDamage", 8.0, 0.0, 10000.0);
        RSP_COST = b.comment("Rock, Scissors 'N' Paper: Ki per part, in percent of the maximum.").defineInRange("rspCostPercent", 1.5, 0.0, 100.0);
        RSP_STAMINA = b.comment("Rock, Scissors 'N' Paper: stamina per part.").defineInRange("rspStamina", 4.0, 0.0, 1000.0);
        RSP_WINDOW = b.comment("Ticks in which the next part must follow the last one.").defineInRange("rspWindowTicks", 30, 6, 200);
        RSP_GAP = b.comment("Ticks that must pass before the next part counts (the inputs are timed).").defineInRange("rspMinGapTicks", 6, 1, 100);
        RSP_COOLDOWN = b.comment("Ticks before a new combo can start after Paper.").defineInRange("rspCooldownTicks", 40, 1, 1200);
        RSP_PAPER_MAX = b.comment("Most foes the Paper shockwave hits.").defineInRange("rspPaperTargets", 8, 1, 16);
        b.pop();

        // ------------------------------------------------------------ the forms
        b.push("forms");
        FORMS_ON = b.comment("Saiyan forms (Kaio-ken, Super Saiyan and the rest of the roster). false = no form can be entered.").define("enabled", true);
        FORM_DAMAGE = b.comment("Multiplier on every form's damage bonus.").defineInRange("damageScale", 1.0, 0.0, 5.0);
        FORM_DRAIN = b.comment("Multiplier on every form's Ki drain.").defineInRange("drainScale", 1.0, 0.0, 10.0);
        FORM_MASTERY_DAY = b.comment("Seconds of real fighting that count toward form mastery per game day (no idle farming).").defineInRange("masteryCapSecondsPerDay", 1800, 60, 86400);
        for (SaiyanForms.Form f : SaiyanForms.ALL) {
            b.push(f.id());
            FORM_CFG.put(f.id(), new ModConfigSpec.DoubleValue[] {
                    b.comment("Battle Power needed to unlock " + f.id() + ".").defineInRange("minBattlePower", f.minBp(), SaiyanRules.START_BP, SaiyanRules.BP_HARD_CAP),
                    b.comment("Damage bonus (0.75 = +75%).").defineInRange("damage", f.damage(), 0.0, 10.0),
                    b.comment("Defence bonus (0.35 = damage taken divided by 1.35).").defineInRange("defense", f.defense(), 0.0, 5.0),
                    b.comment("Ki drain per second, in percent of the maximum Ki.").defineInRange("kiDrainPercentPerSecond", f.drain(), 0.0, 100.0)});
            b.pop();
        }
        b.pop();

        // ------------------------------------------------------------ 3.66.0: Instant Transmission, Spirit Bomb, Ki Barrier, Meteor Combination
        b.push("techniques");
        TECH_ON = b.comment("Instant Transmission, Spirit Bomb, Ki Barrier and Meteor Combination. false = none of the four can be used.").define("enabled", true);
        IT_RANGE = b.comment("Instant Transmission: the farthest creature (blocks) it can lock onto.").defineInRange("instantTransmissionRange", 64.0, 8.0, 128.0);
        IT_BLINK = b.comment("Instant Transmission: the farthest blink (blocks) when nothing is locked onto. Kept small: the blink reads blocks along the line.").defineInRange("instantTransmissionBlink", 24.0, 4.0, 48.0);
        IT_COST = b.comment("Instant Transmission: Ki per use, in percent of the maximum.").defineInRange("instantTransmissionCostPercent", 6.0, 0.0, 100.0);
        IT_COOLDOWN = b.comment("Instant Transmission cooldown in ticks.").defineInRange("instantTransmissionCooldownTicks", 160, 1, 6000);
        BAR_POOL = b.comment("Ki Barrier: the damage the barrier soaks before Battle Power growth (up to three times this at the top).").defineInRange("barrierPool", 60.0, 1.0, 100000.0);
        BAR_SECONDS = b.comment("Ki Barrier: seconds it stands if it is not broken.").defineInRange("barrierSeconds", 8, 1, 60);
        BAR_COST = b.comment("Ki Barrier: Ki per use, in percent of the maximum.").defineInRange("barrierCostPercent", 5.0, 0.0, 100.0);
        BAR_COOLDOWN = b.comment("Ki Barrier cooldown in ticks.").defineInRange("barrierCooldownTicks", 400, 1, 12000);
        SB_DAMAGE = b.comment("Spirit Bomb: damage of a full charge before the balance law.").defineInRange("spiritBombDamage", 120.0, 0.0, 100000.0);
        SB_RADIUS = b.comment("Spirit Bomb: blast radius in blocks of a full charge.").defineInRange("spiritBombRadius", 8.0, 2.0, 16.0);
        SB_RANGE = b.comment("Spirit Bomb: how far the bomb can be thrown (blocks).").defineInRange("spiritBombRange", 48.0, 8.0, 96.0);
        SB_CHARGE = b.comment("Spirit Bomb: seconds of a full charge for a lone Saiyan (friends nearby make it faster).").defineInRange("spiritBombChargeSeconds", 8.0, 1.0, 60.0);
        SB_SOURCE_RADIUS = b.comment("Spirit Bomb: how far (blocks) living friends feed the bomb.").defineInRange("spiritBombFeedRadius", 24.0, 4.0, 48.0);
        SB_COST = b.comment("Spirit Bomb: Ki of the throw for a full charge, in percent of the maximum.").defineInRange("spiritBombCostPercent", 15.0, 0.0, 100.0);
        SB_CHARGE_KI = b.comment("Spirit Bomb: Ki per second while gathering, in percent of the maximum.").defineInRange("spiritBombChargeCostPercentPerSecond", 0.5, 0.0, 50.0);
        SB_COOLDOWN = b.comment("Spirit Bomb cooldown in ticks.").defineInRange("spiritBombCooldownTicks", 600, 1, 24000);
        SB_MIN_TICKS = b.comment("A throw before this many ticks of gathering fizzles.").defineInRange("spiritBombMinTicks", 40, 0, 400);
        SB_TRAVEL_MAX = b.comment("Spirit Bomb: the longest the thrown bomb travels before it lands, in ticks.").defineInRange("spiritBombTravelMaxTicks", 60, 10, 200);
        MET_HITS = b.comment("Meteor Combination: strikes in the rush (at most 16).").defineInRange("meteorHits", 8, 1, 16);
        MET_DAMAGE = b.comment("Meteor Combination: damage of an ordinary strike before the balance law (the last strike is three times this).").defineInRange("meteorDamage", 6.0, 0.0, 10000.0);
        MET_COST = b.comment("Meteor Combination: Ki per use, in percent of the maximum.").defineInRange("meteorCostPercent", 6.0, 0.0, 100.0);
        MET_STAMINA = b.comment("Meteor Combination: stamina per use.").defineInRange("meteorStamina", 20.0, 0.0, 1000.0);
        MET_RANGE = b.comment("Meteor Combination: how far (blocks) the foe it locks onto can be.").defineInRange("meteorRange", 16.0, 4.0, 48.0);
        MET_INTERVAL = b.comment("Meteor Combination: ticks between strikes.").defineInRange("meteorIntervalTicks", 3, 1, 20);
        MET_COOLDOWN = b.comment("Meteor Combination cooldown in ticks.").defineInRange("meteorCooldownTicks", 300, 1, 12000);
        b.pop();

        // ------------------------------------------------------------ 3.71.0: Dragon Fist, God Bind, Ki Deflection, Energy Transfer
        b.push("techniques3");
        TECH3_ON = b.comment("Dragon Fist, God Bind, Ki Deflection and Energy Transfer. false = none of the four can be used.").define("enabled", true);
        DRG_DAMAGE = b.comment("Dragon Fist: damage of one step before the balance law (the last blow is three times this).").defineInRange("dragonFistDamage", 30.0, 0.0, 10000.0);
        DRG_COST = b.comment("Dragon Fist: Ki per use, in percent of the maximum.").defineInRange("dragonFistCostPercent", 8.0, 0.0, 100.0);
        DRG_STAMINA = b.comment("Dragon Fist: stamina per use.").defineInRange("dragonFistStamina", 30.0, 0.0, 1000.0);
        DRG_RANGE = b.comment("Dragon Fist: how close (blocks) the foe must be.").defineInRange("dragonFistRange", 5.0, 2.0, 12.0);
        DRG_UNLOCK = b.comment("Dragon Fist: martial training points a Saiyan needs before it can be used (a day gives at most 60).").defineInRange("dragonFistUnlockTraining", 15.0, 0.0, 10000.0);
        DRG_STEPS = b.comment("Dragon Fist: steps of the dragon's rise, two ticks each (at most 8).").defineInRange("dragonFistSteps", 6, 2, 8);
        DRG_COOLDOWN = b.comment("Dragon Fist cooldown in ticks.").defineInRange("dragonFistCooldownTicks", 400, 1, 12000);
        BND_RANGE = b.comment("God Bind: how far (blocks) the foe it locks onto can be.").defineInRange("godBindRange", 24.0, 4.0, 48.0);
        BND_COST = b.comment("God Bind: Ki per use, in percent of the maximum.").defineInRange("godBindCostPercent", 10.0, 0.0, 100.0);
        BND_SECONDS = b.comment("God Bind: seconds an ordinary foe is held (at most 30).").defineInRange("godBindSeconds", 6.0, 0.5, 30.0);
        BND_BOSS = b.comment("God Bind: the share of the time a boss is held (it breaks free sooner).").defineInRange("godBindBossShare", 0.25, 0.0, 1.0);
        BND_PLAYER = b.comment("God Bind: the share of the time a player is held.").defineInRange("godBindPlayerShare", 0.5, 0.0, 1.0);
        BND_COOLDOWN = b.comment("God Bind cooldown in ticks.").defineInRange("godBindCooldownTicks", 600, 1, 12000);
        DEF_COST = b.comment("Ki Deflection: Ki per use, in percent of the maximum.").defineInRange("kiDeflectionCostPercent", 4.0, 0.0, 100.0);
        DEF_SECONDS = b.comment("Ki Deflection: seconds the window stays open.").defineInRange("kiDeflectionSeconds", 4, 1, 20);
        DEF_RADIUS = b.comment("Ki Deflection: how far (blocks) from the Saiyan a projectile is turned.").defineInRange("kiDeflectionRadius", 4.0, 1.0, 8.0);
        DEF_BOOST = b.comment("Ki Deflection: the speed of a turned projectile compared with its own.").defineInRange("kiDeflectionSpeedBoost", 1.2, 0.5, 3.0);
        DEF_COOLDOWN = b.comment("Ki Deflection cooldown in ticks.").defineInRange("kiDeflectionCooldownTicks", 300, 1, 12000);
        ENG_COST = b.comment("Energy Transfer: Ki given per use, in percent of the giver's maximum.").defineInRange("energyTransferCostPercent", 20.0, 0.0, 100.0);
        ENG_LOSS = b.comment("Energy Transfer: the share of the given Ki that is lost on the way (0.2 = the ally gets 80%).").defineInRange("energyTransferLoss", 0.2, 0.0, 0.9);
        ENG_CAP = b.comment("Energy Transfer: the most an ally can receive from one use, in percent of the ally's maximum.").defineInRange("energyTransferReceiveCapPercent", 30.0, 0.0, 100.0);
        ENG_RANGE = b.comment("Energy Transfer: how close (blocks) the ally must be.").defineInRange("energyTransferRange", 8.0, 2.0, 24.0);
        ENG_COOLDOWN = b.comment("Energy Transfer cooldown in ticks.").defineInRange("energyTransferCooldownTicks", 600, 1, 12000);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private SaiyanConfig() {}

    private static boolean on(ModConfigSpec.BooleanValue v, boolean def) { try { return v.get(); } catch (RuntimeException notLoaded) { return def; } }

    private static double num(ModConfigSpec.DoubleValue v, double def, double lo, double hi) {
        try { return SaiyanRules.clamp(v.get(), lo, hi); } catch (RuntimeException notLoaded) { return def; }
    }

    private static int whole(ModConfigSpec.IntValue v, int def, int lo, int hi) {
        try { return (int) SaiyanRules.clamp(v.get(), lo, hi); } catch (RuntimeException notLoaded) { return def; }
    }

    public static boolean enabled() { return on(ENABLED, true); }
    public static boolean hitPlayers() { return on(HIT_PLAYERS, true); }
    public static boolean playersGiveBp() { return on(PLAYERS_GIVE_BP, false); }
    public static boolean useEp() { return on(USE_EP, true); }

    /** The racial table as set in the file (fractions). */
    public static SaiyanRules.Table base() {
        return new SaiyanRules.Table(num(STRENGTH, 0.35, 0, 5), num(SPEED, 0.20, 0, 2), num(DURABILITY, 0.30, 0, 5), num(ATTACK_SPEED, 0.15, 0, 2), num(JUMP, 0.25, 0, 2),
                num(STAMINA_CAP, 0.40, 0, 5), num(KI_CAPACITY, 0.30, 0, 5), num(KNOCKBACK, 0.15, 0, 1), num(COMBAT_XP, 0.20, 0, 5));
    }

    public static SaiyanRules.Table caps() {
        return new SaiyanRules.Table(num(CAP_STRENGTH, 1.5, 0, 10), num(CAP_SPEED, 0.5, 0, 2), num(CAP_DURABILITY, 1.5, 0, 10), num(CAP_ATTACK_SPEED, 0.5, 0, 2), num(CAP_JUMP, 0.8, 0, 2),
                num(CAP_STAMINA, 2.0, 0, 10), num(CAP_KI_CAPACITY, 2.0, 0, 10), num(CAP_KNOCKBACK, 0.8, 0, 1), num(CAP_COMBAT_XP, 1.0, 0, 10));
    }

    /** The racial bonuses of a Saiyan with this Battle Power. */
    public static SaiyanRules.Table bonuses(double bp) { return base().at(num(GROWTH, 1.0, 0, 10), bp, caps()); }

    public static double kiBase() { return num(KI_BASE, 100.0, 0, 1.0e7); }
    public static double staminaBase() { return num(STAMINA_BASE, 100, 10, 10000); }
    public static double staminaRegen() { return num(STAMINA_REGEN, 6, 0, 1000); }
    public static double kiRegenPercent() { return num(KI_REGEN_PERCENT, 2.0, 0, 50); }
    public static int awakenSeconds() { return whole(AWAKEN_SECONDS, 10, 1, 300); }

    public static double maxBp() { return num(MAX_BP, 1.0e9, 100, SaiyanRules.BP_HARD_CAP); }
    public static double gainPerKill() { return num(GAIN_PER_KILL, 0.01, 0, 0.5); }
    public static double dailyFraction() { return num(DAILY_FRACTION, 0.10, 0, 1); }
    public static double dailyFloor() { return num(DAILY_FLOOR, 20, 0, 1.0e9); }
    public static double minRatio() { return num(MIN_RATIO, 0.25, 0, 0.95); }
    public static double victimScale() { return num(VICTIM_SCALE, 2.0, 0, 1000); }
    public static double epWeight() { return num(EP_WEIGHT, 0.05, 0, 100); }
    public static int minVictimAge() { return whole(MIN_VICTIM_AGE, 600, 0, 72000); }
    public static int repeatLimit() { return whole(REPEAT_LIMIT, 5, 1, 1000); }
    public static int repeatWindow() { return whole(REPEAT_WINDOW, 12000, 20, 1_728_000); }

    public static double blastCost() { return num(BLAST_COST, 2.0, 0, 100); }
    public static double blastDamage() { return num(BLAST_DAMAGE, 6.0, 0, 1000); }
    public static double blastRange() { return num(BLAST_RANGE, 24.0, 4, 64); }
    public static int blastCooldown() { return whole(BLAST_COOLDOWN, 10, 1, 1200); }
    public static double blastStamina() { return whole(BLAST_STAMINA_X10, 10, 0, 10000) / 10.0; }
    public static double barrageCost() { return num(BARRAGE_COST, 6.0, 0, 100); }
    public static double barrageStamina() { return num(BARRAGE_STAMINA, 10.0, 0, 1000); }
    public static int barrageRays() { return whole(BARRAGE_RAYS, 5, 1, 8); }
    public static double barrageShare() { return num(BARRAGE_SHARE, 0.35, 0, 2); }
    public static int barrageCooldown() { return whole(BARRAGE_COOLDOWN, 60, 1, 2400); }
    public static double kiDamageGrowth() { return num(KI_DAMAGE_GAIN, 1.5, 0, 20); }

    // ------------------------------------------------------------------ 3.65.0 getters (each clamped again on read)
    public static boolean flightEnabled() { return on(FLIGHT_ON, true); }
    public static double flightSpeedScale() { return num(FLIGHT_SPEED, 1.0, 0, 3); }
    public static double flightKiScale() { return num(FLIGHT_KI, 1.0, 0, 10); }
    public static double flightStart() { return num(FLIGHT_START, 1.0, 0, 100); }
    public static int hoverSeconds() { return whole(HOVER_SECONDS, 10, 1, 300); }
    public static double burstCost() { return num(BURST_COST, 4.0, 0, 100); }
    public static double burstStamina() { return num(BURST_STAMINA, 15.0, 0, 1000); }
    public static int burstCooldown() { return whole(BURST_COOLDOWN, 80, 1, 2400); }
    public static double burstSpeed() { return num(BURST_SPEED, 1.8, 0.5, 3.0); }

    /** Flight seconds that earn each of the five tiers (the first is 0), kept rising. */
    public static int[] flightSeconds() {
        int[] d = SaiyanFlightRules.SECONDS;
        int[] out = new int[5];
        for (int i = 1; i < 5; i++) out[i] = Math.max(out[i - 1] + 1, whole(FLIGHT_TIER_SECONDS[i - 1], d[i], 1, 1_000_000));
        return out;
    }

    public static double senseMaxRadius() { return num(SENSE_RADIUS, 128.0, 16, 256); }
    public static double senseCost() { return num(SENSE_COST, 1.0, 0, 100); }
    public static int senseCooldown() { return whole(SENSE_COOLDOWN, 100, 1, 2400); }
    public static int senseSeconds() { return whole(SENSE_SECONDS, 10, 2, 60); }
    public static double suppressCost() { return num(SUPPRESS_COST, 0.2, 0, 50); }

    /** Sensings that earn each of the four tiers (the first is 0), kept rising. */
    public static int[] senseScans() {
        int[] d = SaiyanSenseRules.USES;
        int[] out = new int[4];
        for (int i = 1; i < 4; i++) out[i] = Math.max(out[i - 1] + 1, whole(SENSE_TIER_SCANS[i - 1], d[i], 1, 1_000_000));
        return out;
    }

    public static double reflexBase() { return num(REFLEX_BASE, 0.03, 0, 0.75); }
    public static double reflexMax() { return num(REFLEX_MAX, 0.15, 0, 0.75); }
    public static double reflexStamina() { return num(REFLEX_STAMINA, 8.0, 0, 1000); }
    public static int reflexCooldown() { return whole(REFLEX_COOLDOWN, 40, 1, 2400); }
    public static double adaptMax() { return num(ADAPT_MAX, 0.15, 0, 0.5); }
    public static int adaptHits() { return whole(ADAPT_HITS, 10, 1, 1000); }
    public static int adaptReset() { return whole(ADAPT_RESET, 600, 20, 72000); }
    public static boolean zenkaiEnabled() { return on(ZENKAI_ON, true); }
    public static double zenkaiNear() { return num(ZENKAI_NEAR, 0.20, 0.01, 0.9); }
    public static double zenkaiRecover() { return num(ZENKAI_RECOVER, 0.60, 0.05, 1.0); }
    public static double zenkaiRatio() { return num(ZENKAI_RATIO, 0.5, 0, 10); }
    public static double zenkaiGain() { return num(ZENKAI_GAIN, 0.05, 0, 1); }
    public static double zenkaiDecay() { return num(ZENKAI_DECAY, 0.25, 0, 10); }
    public static int zenkaiCooldown() { return whole(ZENKAI_COOLDOWN, 24000, 0, 1_728_000); }
    public static int zenkaiPerDay() { return whole(ZENKAI_PER_DAY, 1, 0, 10); }
    public static double trainingScale() { return num(TRAIN_SCALE, 1.0, 0, 5); }

    public static boolean techEnabled() { return on(TECH_ON, true); }
    public static double itRange() { return num(IT_RANGE, 64.0, 8, 128); }
    public static double itBlink() { return num(IT_BLINK, 24.0, 4, 48); }
    public static double itCost() { return num(IT_COST, 6.0, 0, 100); }
    public static int itCooldown() { return whole(IT_COOLDOWN, 160, 1, 6000); }
    public static double barrierPool() { return num(BAR_POOL, 60.0, 1, 100000); }
    public static int barrierSeconds() { return whole(BAR_SECONDS, 8, 1, 60); }
    public static double barrierCost() { return num(BAR_COST, 5.0, 0, 100); }
    public static int barrierCooldown() { return whole(BAR_COOLDOWN, 400, 1, 12000); }
    public static double sbDamage() { return num(SB_DAMAGE, 120.0, 0, 100000); }
    public static double sbRadius() { return num(SB_RADIUS, 8.0, 2, 16); }
    public static double sbRange() { return num(SB_RANGE, 48.0, 8, 96); }
    public static double sbChargeSeconds() { return num(SB_CHARGE, 8.0, 1, 60); }
    public static double sbFeedRadius() { return num(SB_SOURCE_RADIUS, 24.0, 4, 48); }
    public static double sbCost() { return num(SB_COST, 15.0, 0, 100); }
    public static double sbChargeCost() { return num(SB_CHARGE_KI, 0.5, 0, 50); }
    public static int sbCooldown() { return whole(SB_COOLDOWN, 600, 1, 24000); }
    public static int sbMinTicks() { return whole(SB_MIN_TICKS, 40, 0, 400); }
    public static int sbTravelMax() { return whole(SB_TRAVEL_MAX, 60, 10, 200); }
    public static int meteorHits() { return whole(MET_HITS, 8, 1, 16); }
    public static double meteorDamage() { return num(MET_DAMAGE, 6.0, 0, 10000); }
    public static double meteorCost() { return num(MET_COST, 6.0, 0, 100); }
    public static double meteorStamina() { return num(MET_STAMINA, 20.0, 0, 1000); }
    public static double meteorRange() { return num(MET_RANGE, 16.0, 4, 48); }
    public static int meteorInterval() { return whole(MET_INTERVAL, 3, 1, 20); }
    public static int meteorCooldown() { return whole(MET_COOLDOWN, 300, 1, 12000); }

    public static boolean tech3Enabled() { return on(TECH3_ON, true); }
    public static double dragonDamage() { return num(DRG_DAMAGE, 30.0, 0, 10000); }
    public static double dragonCost() { return num(DRG_COST, 8.0, 0, 100); }
    public static double dragonStamina() { return num(DRG_STAMINA, 30.0, 0, 1000); }
    public static double dragonRange() { return num(DRG_RANGE, 5.0, 2, 12); }
    public static double dragonUnlock() { return num(DRG_UNLOCK, 15.0, 0, 10000); }
    public static int dragonSteps() { return whole(DRG_STEPS, 6, 2, 8); }
    public static int dragonCooldown() { return whole(DRG_COOLDOWN, 400, 1, 12000); }
    public static double bindRange() { return num(BND_RANGE, 24.0, 4, 48); }
    public static double bindCost() { return num(BND_COST, 10.0, 0, 100); }
    public static double bindSeconds() { return num(BND_SECONDS, 6.0, 0.5, 30); }
    public static double bindBossShare() { return num(BND_BOSS, 0.25, 0, 1); }
    public static double bindPlayerShare() { return num(BND_PLAYER, 0.5, 0, 1); }
    public static int bindCooldown() { return whole(BND_COOLDOWN, 600, 1, 12000); }
    public static double deflectCost() { return num(DEF_COST, 4.0, 0, 100); }
    public static int deflectSeconds() { return whole(DEF_SECONDS, 4, 1, 20); }
    public static double deflectRadius() { return num(DEF_RADIUS, 4.0, 1, 8); }
    public static double deflectBoost() { return num(DEF_BOOST, 1.2, 0.5, 3); }
    public static int deflectCooldown() { return whole(DEF_COOLDOWN, 300, 1, 12000); }
    public static double energyCost() { return num(ENG_COST, 20.0, 0, 100); }
    public static double energyLoss() { return num(ENG_LOSS, 0.2, 0, 0.9); }
    public static double energyCap() { return num(ENG_CAP, 30.0, 0, 100); }
    public static double energyRange() { return num(ENG_RANGE, 8.0, 2, 24); }
    public static int energyCooldown() { return whole(ENG_COOLDOWN, 600, 1, 12000); }

    public static boolean gokuEnabled() { return on(GOKU_ON, true); }
    public static double challengingRatio() { return num(HARD_RATIO, 0.4, 0, 10); }

    /** What the Goku awakening asks for, as set by the server. */
    public static SaiyanUltimates.Need gokuNeed() {
        return new SaiyanUltimates.Need(num(AWAKEN_BP, 100_000.0, 100, SaiyanRules.BP_HARD_CAP), whole(AWAKEN_TECH, 5, 1, 5), whole(AWAKEN_FLIGHT, 2, 0, 4), whole(AWAKEN_SENSE, 1, 0, 3),
                whole(AWAKEN_HARD, 1, 0, 1000), whole(AWAKEN_ZENKAI, 1, 0, 1000), whole(TRIAL_KILLS, 10, 0, 1000));
    }

    public static double kameDamage() { return num(KAME_DAMAGE, 40.0, 0, 10000); }
    public static double kameRange() { return num(KAME_RANGE, 56.0, 8, 96); }
    public static double kameCost() { return num(KAME_COST, 12.0, 0, 100); }
    public static double kameChargeSeconds() { return num(KAME_CHARGE, 6.0, 1, 30); }
    public static double kameChargeCost() { return num(KAME_CHARGE_KI, 1.0, 0, 50); }
    public static int kameCooldown() { return whole(KAME_COOLDOWN, 100, 1, 2400); }
    public static int kameMinTicks() { return whole(KAME_MIN_TICKS, 10, 0, 200); }
    public static double rspDamage() { return num(RSP_DAMAGE, 8.0, 0, 10000); }
    public static double rspCost() { return num(RSP_COST, 1.5, 0, 100); }
    public static double rspStamina() { return num(RSP_STAMINA, 4.0, 0, 1000); }
    public static int rspWindow() { return whole(RSP_WINDOW, 30, 6, 200); }
    public static int rspGap() { return whole(RSP_GAP, 6, 1, 100); }
    public static int rspCooldown() { return whole(RSP_COOLDOWN, 40, 1, 1200); }
    public static int rspPaperMax() { return whole(RSP_PAPER_MAX, 8, 1, 16); }

    public static boolean formsEnabled() { return on(FORMS_ON, true); }
    public static double formDamageScale() { return num(FORM_DAMAGE, 1.0, 0, 5); }
    public static double formDrainScale() { return num(FORM_DRAIN, 1.0, 0, 10); }
    public static int formMasteryCap() { return whole(FORM_MASTERY_DAY, 1800, 60, 86400); }

    private static ModConfigSpec.DoubleValue[] formCfg(SaiyanForms.Form f) { return FORM_CFG.get(f.id()); }

    public static double formMinBp(SaiyanForms.Form f) { return num(formCfg(f)[0], f.minBp(), SaiyanRules.START_BP, SaiyanRules.BP_HARD_CAP); }
    public static double formDamage(SaiyanForms.Form f) { return num(formCfg(f)[1], f.damage(), 0, 10) * formDamageScale(); }
    public static double formDefense(SaiyanForms.Form f) { return num(formCfg(f)[2], f.defense(), 0, 5); }
    public static double formDrain(SaiyanForms.Form f) { return num(formCfg(f)[3], f.drain(), 0, 100) * formDrainScale(); }
}
