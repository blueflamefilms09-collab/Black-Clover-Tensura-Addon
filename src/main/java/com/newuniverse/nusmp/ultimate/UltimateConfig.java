package com.newuniverse.nusmp.ultimate;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the SERVER settings of Ultimate Magic ({@code serverconfig/nusmp-ultimate-server.toml}). Every value changes gameplay, so every value is a server setting (rule 9); each one is clamped again by
 * {@link UltimateRules} when it is read, so a hand-edited file cannot go past a bound. {@code enabled = false} restores the books exactly as they were before this feature: the Ultimate pages are not
 * appended to any grimoire and a cast in progress is cancelled.
 */
public final class UltimateConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, SNEAK_CANCEL, EXHAUST_AFTER, HIT_PLAYERS;
    public static final ModConfigSpec.IntValue MAX_SIMULTANEOUS, COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue WINDUP_SCALE, RADIUS_SCALE, DAMAGE_SCALE, INTERRUPT_FRACTION, GATE_STRICTNESS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("ultimate");
        ENABLED = b.comment("Ultimate Magic: one last page per flagship grimoire (Fire, Water, Wind, Earth, Lightning, Light, Dark, Spatial) with a long wind-up and a huge area release. false = the books as they were before.")
                .define("enabled", true);
        MAX_SIMULTANEOUS = b.comment("How many Ultimates may be gathering or releasing at the same time on the whole server (1 to 8).")
                .defineInRange("maxSimultaneous", UltimateRules.DEFAULT_SIMULTANEOUS, 1, UltimateRules.MAX_SIMULTANEOUS);
        COOLDOWN_TICKS = b.comment("Cooldown of one player after a released Ultimate, in ticks (6000 = 5 minutes is the shortest allowed).")
                .defineInRange("cooldownTicks", UltimateRules.DEFAULT_COOLDOWN_TICKS, UltimateRules.MIN_COOLDOWN_TICKS, UltimateRules.MAX_COOLDOWN_TICKS);
        WINDUP_SCALE = b.comment("Multiplier on the wind-up of every Ultimate (the result stays between 100 and 400 ticks).")
                .defineInRange("windupScale", 1.0, UltimateRules.MIN_SCALE, UltimateRules.MAX_SCALE);
        RADIUS_SCALE = b.comment("Multiplier on the radius of every Ultimate (the result stays between 6 and 24 blocks).")
                .defineInRange("radiusScale", 1.0, UltimateRules.MIN_SCALE, UltimateRules.MAX_SCALE);
        DAMAGE_SCALE = b.comment("Multiplier on the raw damage before the grimoire's balance law (and its player hit cap) scales it. 0 = only the debuffs.")
                .defineInRange("damageScale", 1.0, 0.0, UltimateRules.MAX_SCALE);
        INTERRUPT_FRACTION = b.comment("Damage taken during the wind-up, as a fraction of max health, that breaks the cast (nothing is spent when it breaks).")
                .defineInRange("interruptFraction", UltimateRules.DEFAULT_INTERRUPT, UltimateRules.MIN_INTERRUPT, UltimateRules.MAX_INTERRUPT);
        SNEAK_CANCEL = b.comment("Holding sneak for three quarters of a second during the wind-up lets the Ultimate go (nothing is spent).")
                .define("sneakCancel", true);
        EXHAUST_AFTER = b.comment("After a release the caster has burned ALL their mana, so the mana law's exhaustion applies (slowness, weakness, a blackout that never kills by default).")
                .define("exhaustAfter", true);
        HIT_PLAYERS = b.comment("Players may be hit by an Ultimate (they still must be foes: PvP setting, teams and allies are respected). false = mobs only.")
                .define("hitPlayers", true);
        GATE_STRICTNESS = b.comment("Multiplier on the EP the page's gate asks (1,000,000 EP by default; floor 500,000). The rank and the full mastery bar always stay.")
                .defineInRange("gateStrictness", 1.0, UltimateGate.MIN_STRICTNESS, UltimateGate.MAX_STRICTNESS);
        b.pop();
        SPEC = b.build();
    }

    private UltimateConfig() {}

    public static boolean enabled() {
        try { return ENABLED.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static int maxSimultaneous() {
        try { return UltimateRules.simultaneousCap(MAX_SIMULTANEOUS.get()); } catch (RuntimeException notLoaded) { return UltimateRules.DEFAULT_SIMULTANEOUS; }
    }

    public static int cooldownTicks() {
        try { return UltimateRules.cooldownTicks(COOLDOWN_TICKS.get()); } catch (RuntimeException notLoaded) { return UltimateRules.DEFAULT_COOLDOWN_TICKS; }
    }

    public static double windupScale() { return scale(WINDUP_SCALE); }

    public static double radiusScale() { return scale(RADIUS_SCALE); }

    public static double damageScale() {
        try { return UltimateRules.clampDouble(DAMAGE_SCALE.get(), 0.0, UltimateRules.MAX_SCALE); } catch (RuntimeException notLoaded) { return 1.0; }
    }

    public static double interruptFraction() {
        try { return UltimateRules.interruptFraction(INTERRUPT_FRACTION.get()); } catch (RuntimeException notLoaded) { return UltimateRules.DEFAULT_INTERRUPT; }
    }

    public static boolean sneakCancel() {
        try { return SNEAK_CANCEL.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static boolean exhaustAfter() {
        try { return EXHAUST_AFTER.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static boolean hitPlayers() {
        try { return HIT_PLAYERS.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static double gateStrictness() {
        try { return UltimateGate.clampStrictness(GATE_STRICTNESS.get()); } catch (RuntimeException notLoaded) { return 1.0; }
    }

    private static double scale(ModConfigSpec.DoubleValue v) {
        try { return UltimateRules.clampDouble(v.get(), UltimateRules.MIN_SCALE, UltimateRules.MAX_SCALE); } catch (RuntimeException notLoaded) { return 1.0; }
    }
}
