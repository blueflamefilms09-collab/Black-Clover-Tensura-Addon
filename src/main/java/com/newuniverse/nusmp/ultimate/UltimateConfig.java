package com.newuniverse.nusmp.ultimate;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the SERVER settings of Ultimate Magic ({@code serverconfig/nusmp-ultimate-server.toml}). Every value changes gameplay, so every value is a server setting (rule 9); each one is clamped again by
 * {@link UltimateRules} when it is read, so a hand-edited file cannot go past a bound. {@code enabled = false} restores the books exactly as they were before this feature: the Ultimate pages are not
 * appended to any grimoire and a cast in progress is cancelled.
 */
public final class UltimateConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, SNEAK_CANCEL, EXHAUST_AFTER, HIT_PLAYERS, KINGDOM_ANIMATIONS, SHOW_IN_CHARACTER_GRIMOIRES;
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
        KINGDOM_ANIMATIONS = b.comment("The wind-up, hold and release body animation of an Ultimate depends on the caster's kingdom (Clover, Diamond, Spade, Heart); a caster without a kingdom keeps the old Mana Zone pose.",
                "false = every caster plays the old single looping pose, as before. The animation is only drawn by the clients; nothing about the cast itself changes.")
                .define("kingdomAnimations", true);
        SHOW_IN_CHARACTER_GRIMOIRES = b.comment("A grimoire bound to a character (Yuno, Noelle, Asta ...) whose spell set does not list the Ultimate page used to hide it. true = it shows the Ultimate of the character's OWN attribute(s)",
                "(his book or books); the Ultimates of other attributes stay hidden there. false = the old behaviour (hidden in every character grimoire that has a spell set). Plain and basic grimoires always show it.")
                .define("showInCharacterGrimoires", true);
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

    public static boolean kingdomAnimations() {
        try { return KINGDOM_ANIMATIONS.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static boolean showInCharacterGrimoires() {
        try { return SHOW_IN_CHARACTER_GRIMOIRES.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static double gateStrictness() {
        try { return UltimateGate.clampStrictness(GATE_STRICTNESS.get()); } catch (RuntimeException notLoaded) { return 1.0; }
    }

    private static double scale(ModConfigSpec.DoubleValue v) {
        try { return UltimateRules.clampDouble(v.get(), UltimateRules.MIN_SCALE, UltimateRules.MAX_SCALE); } catch (RuntimeException notLoaded) { return 1.0; }
    }
}
