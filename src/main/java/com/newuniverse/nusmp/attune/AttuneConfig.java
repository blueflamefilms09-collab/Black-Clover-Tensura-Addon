package com.newuniverse.nusmp.attune;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: whether meditation attunement exists at all, how many powers a player may hold at once, how long they wait between changes and how hard attuned powers hit change gameplay, so they are SERVER
 * settings: {@code nusmp-attune-server.toml} (registered by {@link AttuneBoot}). {@code enabled = false} restores the old behaviour exactly: no screen, no rejection, no suppression, no factor.
 * The defaults of the factors are explained in docs/attunement.md (the mod's King-titled Ultimate skills hit 60 to 150 raw; a Beast Will ability is 3 to 6 raw, a Spirit spell 5 to 12).
 */
public final class AttuneConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, SUPPRESS, EQUIP_LIMIT;
    public static final ModConfigSpec.IntValue SLOTS, COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue BEAST, DJINN, DEVIL, SPIRIT, RESONANCE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("attune");
        ENABLED = b.comment("Meditation attunement: /meditate opens the screen, attuned powers stack, clashing elements reject each other. false = the old behaviour exactly (nothing of this runs).").define("enabled", true);
        SUPPRESS = b.comment("Two powers out together whose elements clash: the weaker is suppressed (it loses its damage raise) until the player meditates again. false = no suppression.").define("suppressClashes", true);
        SLOTS = b.comment("Meditation slots: how many powers a player may attune at once (two of one system allowed; at most 2).").defineInRange("slots", 2, 1, AttuneRules.MAX_SLOTS);
        COOLDOWN_TICKS = b.comment("Ticks between two changes of the attunement (600 = 30 s).").defineInRange("cooldownTicks", 600, 0, 72000);
        BEAST = b.comment("Damage factor of a player while an attuned Beast Will form is held (the largest factor of the systems out is used, never a product; the 3.74.0 power raise is not added on top).").defineInRange("beastWillFactor", 25.0, 1.0, 1000.0);
        DJINN = b.comment("Damage factor while an attuned Djinn is out (Lesser Summon and up).").defineInRange("djinnFactor", 25.0, 1.0, 1000.0);
        DEVIL = b.comment("Damage factor while an attuned Devil is let in (its attribute acts).").defineInRange("devilFactor", 12.0, 1.0, 1000.0);
        SPIRIT = b.comment("Damage factor while an attuned Spirit is out.").defineInRange("spiritFactor", 12.0, 1.0, 1000.0);
        RESONANCE = b.comment("Extra damage per resonating pair of attuned powers that are out (0.10 = +10%; at most +30% in all).").defineInRange("resonancePerPair", 0.10, 0.0, 0.30);
        EQUIP_LIMIT = b.comment("The equip limit (3.95.0): a player can use at most two Beast Wills / Djinns / Devils / Spirits at once (more need meditating), and never a Devil with a Spirit; the extras are blocked and lose their damage raise. false = no limit (the 3.93.0 behaviour).").define("equipLimit", true);
        b.pop();
        SPEC = b.build();
    }

    private AttuneConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static boolean suppress() { return get(SUPPRESS); }
    public static boolean equipLimit() { return get(EQUIP_LIMIT); }
    public static int slots() { return AttuneRules.slots(get(SLOTS)); }
    public static int cooldownTicks() { return get(COOLDOWN_TICKS); }
    public static double resonance() { return get(RESONANCE); }

    public static double factor(int system) {
        return switch (system) {
            case AttuneRules.BEAST -> get(BEAST);
            case AttuneRules.DJINN -> get(DJINN);
            case AttuneRules.DEVIL -> get(DEVIL);
            case AttuneRules.SPIRIT -> get(SPIRIT);
            default -> 1.0;
        };
    }
}
