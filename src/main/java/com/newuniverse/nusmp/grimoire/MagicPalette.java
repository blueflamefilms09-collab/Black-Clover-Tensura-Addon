package com.newuniverse.nusmp.grimoire;

import com.newuniverse.nusmp.blackclover.MagicType;

/** Cover colour (render layer 0) and aura colour (layer 3) for every magic type, so each existing grimoire keeps the identity it had. */
public final class MagicPalette {
    private MagicPalette() {}

    /** Cover leather colour, RGB. */
    public static int primary(MagicType m) {
        return switch (m) {
            case FLAME -> 0xB02A1C; case EXPLOSION -> 0xD2601A; case MAGMA -> 0x9A2E12;
            case WATER -> 0x2453B0; case ICE -> 0x7FC4E8; case MERCURY -> 0x8E94A6; case MIST -> 0x9FB0C4;
            case WIND -> 0x2F8859; case STAR -> 0x232663; case STORM -> 0x4F8F94;
            case EARTH -> 0x6B4A2A; case PLANT -> 0x3F7A33; case SAND -> 0xC2A565;
            case LIGHT -> 0xE3C660; case LIGHTNING -> 0xD4B13A; case SWORD -> 0x7F8896;
            case DARK -> 0x2E2540; case SHADOW -> 0x3F3F4C; case POISON -> 0x7A9A3A;
            case SPATIAL -> 0x52318A; case MIRROR -> 0xA9C4D8; case GRAVITY -> 0x38205A;
            case TIME -> 0x8A6A2A; case SEALING -> 0x6E2330;
            case REINFORCEMENT -> 0xC8A04A; case BEAST -> 0xB8A070; case BONE -> 0xD6CBB0; case BLOOD -> 0x7D0C16;
            case CREATION -> 0xD9D6CC; case COPY -> 0x5F6A80; case ILLUSION -> 0x8E4AB0; case DREAM -> 0xD898C4;
            case ANTI_MAGIC -> 0x2A2626;
            case STEEL -> 0x7C8592; case THREAD -> 0x9A162D;
        };
    }

    /** Emissive aura colour, RGB. Anti-Magic is a dark, light-eating glow on purpose. */
    public static int aura(MagicType m) {
        return switch (m) {
            case FLAME -> 0xFF4A1F; case EXPLOSION -> 0xFF8A2A; case MAGMA -> 0xFF5A14;
            case WATER -> 0x3F8CFF; case ICE -> 0x9FE8FF; case MERCURY -> 0xC8D0E0; case MIST -> 0xD0DCEC;
            case WIND -> 0x4DFF9A; case STAR -> 0x7A8CFF; case STORM -> 0x38E0D0;
            case EARTH -> 0xB07A3C; case PLANT -> 0x7CE04A; case SAND -> 0xE8C878;
            case LIGHT -> 0xFFE680; case LIGHTNING -> 0x5AE6FF; case SWORD -> 0xC8D2E6;
            case DARK -> 0xA04CFF; case SHADOW -> 0x6A5AA8; case POISON -> 0xB4E040;
            case SPATIAL -> 0xB070FF; case MIRROR -> 0xC0F0FF; case GRAVITY -> 0x8A4CD0;
            case TIME -> 0xE8C050; case SEALING -> 0xFF5A78;
            case REINFORCEMENT -> 0xFFC040; case BEAST -> 0xFFB060; case BONE -> 0xF0EAD0; case BLOOD -> 0xE0182C;
            case CREATION -> 0xFFFFFF; case COPY -> 0xA0B0D0; case ILLUSION -> 0xD070FF; case DREAM -> 0xFFA8E0;
            case ANTI_MAGIC -> 0x2A1A33;
            case STEEL -> 0xB8C4D8; case THREAD -> 0xFF4060;
        };
    }
}
