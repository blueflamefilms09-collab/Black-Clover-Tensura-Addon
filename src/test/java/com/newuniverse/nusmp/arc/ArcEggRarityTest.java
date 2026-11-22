package com.newuniverse.nusmp.arc;

import net.minecraft.world.item.Rarity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArcEggRarityTest {
    @Test
    void friendlyIsCommonAndBossesClimbWithTier() {
        assertEquals(Rarity.COMMON, ArcEggRarity.of(false, 5));
        assertEquals(Rarity.UNCOMMON, ArcEggRarity.of(true, 1));
        assertEquals(Rarity.RARE, ArcEggRarity.of(true, 2));
        assertEquals(Rarity.RARE, ArcEggRarity.of(true, 3));
        assertEquals(Rarity.EPIC, ArcEggRarity.of(true, 4));
        assertEquals(Rarity.EPIC, ArcEggRarity.of(true, 5));
    }
}
