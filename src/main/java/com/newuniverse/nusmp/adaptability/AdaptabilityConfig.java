package com.newuniverse.nusmp.adaptability;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the SERVER settings of the Adaptability skill ({@code serverconfig/nusmp-adaptability-server.toml}, 3.70.0). Every number changes gameplay, so every number is a server setting (rule 9).
 * Each is clamped again when it is read, so a hand-edited file cannot go past a bound; unloaded (a test, an early call) reads the default. {@code enabled = false} turns the skill's effects off.
 */
public final class AdaptabilityConfig {
    private AdaptabilityConfig() {}

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.DoubleValue PER_HIT, MIN_MULT, LEARN, LEARN_MASTERED, GLOBAL, GLOBAL_MASTERED, SELF_BASE, SELF_MASTERED, SELF_MAX;
    private static final ModConfigSpec.IntValue STACKS, STACKS_MASTERED, SEVERANCE_HITS, INTERVAL, MAX_SKILLS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("adaptability");
        ENABLED = b.comment("false = Adaptability does nothing (no damage reduction, no faster learning, no mastery).").define("enabled", true);
        b.push("damage");
        PER_HIT = b.comment("Damage taken off per stack of one kind of damage (0.007142857 = about 0.71% per hit taken of that kind).").defineInRange("damageReductionPerHit", 0.007142857, 0.0, 1.0);
        MIN_MULT = b.comment("The lowest the damage of one kind can fall: 0.75 = never below 75% of the hit (at most 25% off).").defineInRange("minimumDamageMultiplier", 0.75, 0.0, 1.0);
        STACKS = b.comment("Most stacks of one kind of damage before the skill is mastered.").defineInRange("maxStacksNormal", 100, 1, 100000);
        STACKS_MASTERED = b.comment("Most stacks of one kind of damage once the skill is mastered.").defineInRange("maxStacksMastered", 100, 1, 100000);
        SEVERANCE_HITS = b.comment("Severance hits taken before the body adapts: the severance is cleared and blocked for good.").defineInRange("severanceHitsForImmunity", 25, 1, 100000);
        b.pop();
        b.push("learning");
        LEARN = b.comment("Multiplier on the learning progress of resistance skills while Adaptability is on.").defineInRange("resistanceLearningMultiplier", 3.45, 1.0, 100000.0);
        LEARN_MASTERED = b.comment("The same once Adaptability is mastered.").defineInRange("resistanceLearningMultiplierMastered", 5.85, 1.0, 100000.0);
        INTERVAL = b.comment("Ticks between the mastery points every learned skill gets while Adaptability is on (100 = 5 seconds).").defineInRange("globalMasteryIntervalTicks", 100, 20, 72000);
        GLOBAL = b.comment("Mastery points each learned skill gets every interval.").defineInRange("globalMasteryBonus", 0.5, 0.0, 100000.0);
        GLOBAL_MASTERED = b.comment("The same once Adaptability is mastered.").defineInRange("globalMasteryBonusMastered", 2.0, 0.0, 100000.0);
        MAX_SKILLS = b.comment("Most learned skills paid in one pass (a long list is paid in turns), so the cost per player stays fixed.").defineInRange("maxSkillsPerPass", 24, 1, 200);
        SELF_BASE = b.comment("Mastery Adaptability gives itself each time it learns a new stack.").defineInRange("adaptiveMasteryBaseBonus", 1.0, 0.0, 100000.0);
        SELF_MASTERED = b.comment("Extra self-mastery per learned stack once mastered.").defineInRange("adaptiveMasteryMasteredBonus", 2.0, 0.0, 100000.0);
        SELF_MAX = b.comment("Most self-mastery from one learned stack.").defineInRange("adaptiveMasteryMaxBonus", 6.0, 0.0, 100000.0);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private static boolean on(ModConfigSpec.BooleanValue v, boolean def) { try { return v.get(); } catch (RuntimeException notLoaded) { return def; } }
    private static double num(ModConfigSpec.DoubleValue v, double def) { try { return v.get(); } catch (RuntimeException notLoaded) { return def; } }
    private static int whole(ModConfigSpec.IntValue v, int def) { try { return v.get(); } catch (RuntimeException notLoaded) { return def; } }

    public static boolean enabled() { return on(ENABLED, true); }
    public static double perHit() { return num(PER_HIT, 0.007142857); }
    public static double minimumMultiplier() { return num(MIN_MULT, 0.75); }
    public static int maxStacks(boolean mastered) { return AdaptabilityRules.maxStacks(mastered, whole(STACKS, 100), whole(STACKS_MASTERED, 100)); }
    public static int severanceHits() { return whole(SEVERANCE_HITS, 25); }
    public static double learning(boolean mastered) { return mastered ? num(LEARN_MASTERED, 5.85) : num(LEARN, 3.45); }
    public static int interval() { return whole(INTERVAL, 100); }
    public static double globalBonus(boolean mastered) { return AdaptabilityRules.globalBonus(mastered, num(GLOBAL, 0.5), num(GLOBAL_MASTERED, 2.0)); }
    public static int maxSkills() { return whole(MAX_SKILLS, 24); }
    public static double selfMastery(boolean mastered) { return AdaptabilityRules.selfMastery(mastered, num(SELF_BASE, 1.0), num(SELF_MASTERED, 2.0), num(SELF_MAX, 6.0)); }
}
