package com.newuniverse.nusmp.boss.bar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The tint pair and rage flag the server sends for each boss. */
class BossBarThemesTest {
    @Test
    void luciusIsGoldAndTurnsDeepPurpleWithTheGates() {
        BossBarThemes.Tint open = BossBarThemes.tint("lucius_zogratis", 0, 0, 0);
        assertEquals(BossBarThemes.LUCIUS_GOLD, open.primary());
        assertEquals(BossBarThemes.LUCIUS_GOLD_LIGHT, open.secondary());
        BossBarThemes.Tint gates = BossBarThemes.tint("lucius_zogratis", 0, 0, 4);
        assertEquals(BossBarThemes.LUCIUS_PURPLE, gates.primary());
        assertEquals(BossBarThemes.LUCIUS_VIOLET, gates.secondary());
        assertEquals(open, BossBarThemes.tint("lucius_zogratis", 0, 0, 1), "no drift before 60%");
        int mid = BossBarThemes.tint("lucius_zogratis", 0, 0, 3).primary();
        assertNotEquals(open.primary(), mid);
        assertNotEquals(gates.primary(), mid);
    }

    @Test
    void floorBossesHaveTheirOwnPalettes() {
        assertEquals(BossBarThemes.GOLIATH_STEEL, BossBarThemes.tint("goliath", 1, 2, 0).primary());
        assertEquals(BossBarThemes.GOLIATH_HOT, BossBarThemes.tint("goliath", 1, 2, 4).primary());
        assertEquals(BossBarThemes.TIDE_BLUE, BossBarThemes.tint("charybdis_spawn", 1, 2, 2).primary());
        assertEquals(BossBarThemes.TIDE_FOAM_LIGHT, BossBarThemes.tint("charybdis_spawn", 1, 2, 4).secondary());
        assertEquals(BossBarThemes.ORDER_GOLD, BossBarThemes.tint("god_of_order", 1, 2, 0).primary());
        assertEquals(BossBarThemes.ORDER_OPEN, BossBarThemes.tint("god_of_order", 1, 2, 4).primary());
    }

    @Test
    void otherBossesUseTheirDevilsColoursAndBrightenWithTheGates() {
        BossBarThemes.Tint a = BossBarThemes.tint("dante_zogratis", 0x5B2A8C, 0xB58CFF, 2);
        assertEquals(0x5B2A8C, a.primary());
        assertEquals(0xB58CFF, a.secondary());
        BossBarThemes.Tint g = BossBarThemes.tint("dante_zogratis", 0x5B2A8C, 0xB58CFF, 4);
        assertNotEquals(0x5B2A8C, g.primary());
        assertEquals(0xB58CFF, g.secondary());
        assertEquals(a, BossBarThemes.tint(null, 0x5B2A8C, 0xB58CFF, 2));
        assertEquals(g, BossBarThemes.tint("devil_avatar", 0x5B2A8C, 0xB58CFF, 99), "phase is clamped");
    }

    @Test
    void rageIsTheGatesOrAnOffGridPower() {
        for (int p : new int[]{20, 40, 60, 80}) assertFalse(BossBarThemes.rage(p), "" + p);
        for (int p : new int[]{30, 50, 70, 100}) assertTrue(BossBarThemes.rage(p), "" + p);
    }

    @Test
    void colourBlendIsExactAtTheEndsAndClamped() {
        assertEquals(0x102030, BossBarColors.lerp(0x102030, 0xF0E0D0, 0f));
        assertEquals(0xF0E0D0, BossBarColors.lerp(0x102030, 0xF0E0D0, 1f));
        assertEquals(0x808080, BossBarColors.lerp(0x000000, 0xFFFFFF, 0.5f));
        assertEquals(0xF0E0D0, BossBarColors.lerp(0x102030, 0xF0E0D0, 9f));
        assertEquals(0x102030, BossBarColors.lerp(0xFF102030, 0xF0E0D0, -1f), "alpha bits dropped");
    }
}
