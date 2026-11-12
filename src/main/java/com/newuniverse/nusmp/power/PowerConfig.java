package com.newuniverse.nusmp.power;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the 3.74.0 power raise changes gameplay, so it is a SERVER config of its own, {@code nusmp-power-server.toml}, section {@code [power]}: one factor per system (default 25, the owner's
 * number), what a boosted wielder takes, and the master switch that restores 3.73.0 exactly. Plain {@code define} / {@code defineInRange}; readers answer the default while the spec is not loaded.
 */
public final class PowerConfig {
    private PowerConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue DJINN, BEAST_WILL, VASSAL, TAKEN, LEGENDARY_MARGIN;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("power");
        ENABLED = b.comment("Master switch of the 3.74.0 power raise. Off: Djinns, Beast Wills and Vassal Weapons hit exactly as in 3.73.0.").define("enabled", true);
        DJINN = b.comment("Factor on every point of damage a bearer deals while their Djinn is out (Lesser Summon, Half Equip, Full Equip, Extreme Magic): blows, spells, the metal vessel's techniques. 1 = off.")
                .defineInRange("djinnFactor", 25.0, 1.0, 1000.0);
        BEAST_WILL = b.comment("Factor on every point of damage a player deals while a Beast Will phase is held (Acquire or Integrate). 1 = off.")
                .defineInRange("beastWillFactor", 25.0, 1.0, 1000.0);
        VASSAL = b.comment("Factor on every point of damage a wielder deals with a Seven Star or Vassal Weapon out (not the four Legendary Weapons; those have holyExtraFactor in nusmp-heroweapon-server.toml).",
                "It multiplies on top of the hero weapon buff. 1 = off.")
                .defineInRange("vassalFactor", 25.0, 1.0, 1000.0);
        LEGENDARY_MARGIN = b.comment("3.78.1: the four Legendary Weapons (Shield, Sword, Spear, Bow) always stay on top: a wielder with one out gets vassalFactor times this margin.",
                "It can not be set under 1.1, so no setting puts a Vassal Weapon ahead of a Legendary one. (The hero weapon buff's holyExtraFactor still multiplies on top.)")
                .defineInRange("legendaryMargin", 1.5, PowerRules.MIN_LEGENDARY_MARGIN, 100.0);
        TAKEN = b.comment("Factor on the damage a wielder TAKES while a Djinn is out or a Beast Will phase is held (0.5 = half). 1 = off. Vassal wielders already have damageTakenFactor of the hero weapons.")
                .defineInRange("damageTakenFactor", 0.5, 0.0, 1.0);
        b.pop();
        SPEC = b.build();
    }

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }
    public static double djinn() { return get(DJINN); }
    public static double beastWill() { return get(BEAST_WILL); }
    public static double vassal() { return get(VASSAL); }
    public static double taken() { return get(TAKEN); }
    public static double legendary() { return PowerRules.legendary(vassal(), get(LEGENDARY_MARGIN)); }
}
