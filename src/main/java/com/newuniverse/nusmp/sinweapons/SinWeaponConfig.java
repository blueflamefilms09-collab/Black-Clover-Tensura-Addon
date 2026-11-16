package com.newuniverse.nusmp.sinweapons;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: whether the right-click techniques and Envy's Covet of the Berserk weapons exist, and how hard they hit, change gameplay, so they are SERVER settings ({@code nusmp-sinweapons-server.toml}, registered by
 * {@link SinWeaponBoot}). false = the five weapons are plain melee weapons (the old behaviour of "no such items").
 */
public final class SinWeaponConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue TECHNIQUES, COVET;
    public static final ModConfigSpec.IntValue DAMAGE_PERCENT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("sinweapons");
        TECHNIQUES = b.comment("The right-click techniques (Dead Weight, Green-Eyed Shot, Vain Thrust, Spinning Ring, Grand Cross). false = plain melee weapons.").define("techniques", true);
        COVET = b.comment("The Black Gunblade of Envy takes one beneficial effect from a creature it hits (at most every 2 seconds) and lends it to its wielder for up to 10 seconds. false = no theft.").define("covet", true);
        DAMAGE_PERCENT = b.comment("Strength of the techniques in percent (100 = as listed).").defineInRange("damagePercent", 100, 10, 1000);
        b.pop();
        SPEC = b.build();
    }

    private SinWeaponConfig() {}

    public static boolean techniques() { return TECHNIQUES.get(); }
    public static boolean covet() { return COVET.get(); }
    public static double damagePercent() { return DAMAGE_PERCENT.get(); }
}
