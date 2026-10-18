package com.newuniverse.nusmp.client.aura;

import com.newuniverse.nusmp.aura.Aura;
import net.minecraft.util.Mth;

/**
 * 1.93.0 Earth Magic and its kin: the painter of the six temporary armours (Sandstone, Rock, Red Ochre, Stone, Earth, Mineral) on {@code Aura.WIKI}.
 * Registered AFTER {@code WikiAura}: it answers its own six styles ({@link EarthArmorStyles}) and hands every other style to the painter that was
 * registered before it, so the old wiki armour of the other magics is untouched. The sets themselves are {@link EarthArmorKit} pieces.
 */
public final class EarthAura {
    private EarthAura() {}

    static final EarthArmorKit.Set SANDSTONE = new EarthArmorKit.Set("sandstone", 0xFFD98A);
    static final EarthArmorKit.Set ROCK = new EarthArmorKit.Set("rock", 0xFF9640);
    static final EarthArmorKit.Set RED_OCHRE = new EarthArmorKit.Set("red_ochre", 0xFF7A3C);
    static final EarthArmorKit.Set STONE = new EarthArmorKit.Set("stone", 0x96E6FF);
    static final EarthArmorKit.Set EARTH = new EarthArmorKit.Set("earth", 0xD2E65A);
    static final EarthArmorKit.Set MINERAL = new EarthArmorKit.Set("mineral", 0x78DCFF);

    /** Called once by AuraRegistry on the client. */
    public static void register() {
        AuraPainter prev = PlayerAuraClient.painter(Aura.WIKI);
        PlayerAuraClient.register(Aura.WIKI, c -> { if (EarthArmorStyles.owns(c.style())) paint(c); else if (prev != null) prev.paint(c); });
    }

    static void paint(AuraContext c) {
        float pulse = 0.55f + 0.25f * Mth.sin(c.age() * 0.11f);
        switch (c.style()) {
            case EarthArmorStyles.SANDSTONE -> EarthArmorKit.paint(c, SANDSTONE, pulse);
            case EarthArmorStyles.ROCK -> EarthArmorKit.paint(c, ROCK, pulse);
            case EarthArmorStyles.RED_OCHRE -> EarthArmorKit.paint(c, RED_OCHRE, pulse);
            case EarthArmorStyles.STONE -> EarthArmorKit.paint(c, STONE, pulse);
            case EarthArmorStyles.EARTH -> EarthArmorKit.paint(c, EARTH, pulse);
            case EarthArmorStyles.MINERAL -> EarthArmorKit.paint(c, MINERAL, pulse);
            default -> { }
        }
    }
}
