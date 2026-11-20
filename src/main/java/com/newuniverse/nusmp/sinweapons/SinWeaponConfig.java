package com.newuniverse.nusmp.sinweapons;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: whether the right-click techniques and Envy's Covet of the Berserk weapons exist, and how hard they hit, change gameplay, so they are SERVER settings ({@code nusmp-sinweapons-server.toml}, registered by
 * {@link SinWeaponBoot}). false = the five weapons are plain melee weapons (the old behaviour of "no such items").
 */
public final class SinWeaponConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue TECHNIQUES, COVET, SOUL_BOUND, ABILITIES, THEMED_VFX;
    public static final ModConfigSpec.IntValue DAMAGE_PERCENT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("sinweapons");
        TECHNIQUES = b.comment("The right-click techniques (Dead Weight, Green-Eyed Shot, Vain Thrust, Spinning Ring, Grand Cross). false = plain melee weapons.").define("techniques", true);
        COVET = b.comment("The Black Gunblade of Envy takes one beneficial effect from a creature it hits (at most every 2 seconds) and lends it to its wielder for up to 10 seconds. false = no theft.").define("covet", true);
        SOUL_BOUND = b.comment("A weapon binds to the first player who carries it (and gives them its skill); its right-click technique then needs its owner. false = anyone can use the technique, as before the skills.").define("soulBound", true);
        DAMAGE_PERCENT = b.comment("Strength of the techniques in percent (100 = as listed).").defineInRange("damagePercent", 100, 10, 1000);
        ABILITIES = b.comment("The extra abilities of each weapon (docs/sin_weapons_abilities.md): two more techniques (skill modes 3 and 4 of its Lord skill) and one passive on plain hits. false = only the right-click technique, the plain hits and the Manifest / Store mode, as before.").define("abilities", true);
        THEMED_VFX = b.comment("true: each weapon and technique shows its own effect (one small effect packet per use, drawn by the client). false = the old plain particle puff of every technique and hit. Looks only; the client setting classicVfx in nusmp-sinweapons-vfx-client.toml hides the drawn effect for one player.").define("themedVfx", true);
        b.pop();
        SPEC = b.build();
    }

    private SinWeaponConfig() {}

    public static boolean techniques() { return TECHNIQUES.get(); }
    public static boolean covet() { return COVET.get(); }
    public static boolean soulBound() { return SOUL_BOUND.get(); }
    public static double damagePercent() { return DAMAGE_PERCENT.get(); }
    public static boolean abilities() { return ABILITIES.get(); }
    public static boolean themedVfx() { return THEMED_VFX.get(); }
}
