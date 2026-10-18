package com.newuniverse.nusmp.heroweapon;

import java.util.EnumMap;
import java.util.Map;

/**
 * Why: the core draws a little of every weapon in its own colour (the gem of the HUD, the charge-up of a technique's wind-up, the ring of a shock), and the colour
 * must be one value per weapon, not a guess in each place. The four Holy Weapons follow the jewels of the series as far as they are known (the Shield's green
 * stone, the Sword's red, the Spear's blue, the Bow's gold; from memory, the art builders' gems may differ and this table is the one place to follow them);
 * the Seven Star and Vassal Weapons get distinct hues of their own so two wielders on one screen can be told apart. Colours are 0xRRGGBB.
 */
public final class HeroColors {
    private HeroColors() {}

    private static final Map<HeroKind, Integer> RGB = new EnumMap<>(HeroKind.class);

    private static void c(HeroKind k, int rgb) { RGB.put(k, rgb); }

    static {
        c(HeroKind.SHIELD, 0x3FD0A4);
        c(HeroKind.SWORD, 0xE8504A);
        c(HeroKind.SPEAR, 0x4DA3FF);
        c(HeroKind.BOW, 0xF2C94C);
        c(HeroKind.HAMMER, 0xC98A4B);
        c(HeroKind.CLAW, 0xD8D8E8);
        c(HeroKind.AXE, 0xB5532E);
        c(HeroKind.WHIP, 0xC04BA8);
        c(HeroKind.STAFF, 0x7B8CFF);
        c(HeroKind.GAUNTLET, 0xE08A2E);
        c(HeroKind.PROJECTILE, 0x6FD05B);
        c(HeroKind.CARRIAGE, 0xE6C27A);
        c(HeroKind.FAN, 0xFF9EC4);
        c(HeroKind.SCYTHE, 0x8A4BD6);
        c(HeroKind.KATANA, 0xA9D3F5);
        c(HeroKind.BOOK, 0xB28A5A);
        c(HeroKind.MIRROR, 0x9FE3F0);
        c(HeroKind.INSTRUMENT, 0xE9B84A);
        c(HeroKind.HARPOON, 0x2E86C1);
        c(HeroKind.SEWING_SET, 0xE87BA1);
        c(HeroKind.CHAIN, 0x7D8795);
    }

    /** The colour of a weapon (0xRRGGBB); a pale gold for an unknown one. */
    public static int rgb(HeroKind kind) {
        Integer v = kind == null ? null : RGB.get(kind);
        return v != null ? v : 0xE8C468;
    }

    /** The same with full alpha, as the effect system and the GUI want it (0xAARRGGBB). */
    public static int argb(HeroKind kind) { return 0xFF000000 | rgb(kind); }
}
