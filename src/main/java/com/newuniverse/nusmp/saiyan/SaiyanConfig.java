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
}
