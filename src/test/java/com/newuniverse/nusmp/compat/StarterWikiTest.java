package com.newuniverse.nusmp.compat;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The starter Wiki book is handed out once, and not at all when the setting is off (4.78.0). */
class StarterWikiTest {
    @Test
    void givenOnceThenNever() {
        CompoundTag t = new CompoundTag();
        assertTrue(StarterWiki.shouldGive(true, t));
        t.putBoolean(StarterWiki.GIVEN, true);
        assertFalse(StarterWiki.shouldGive(true, t));
    }

    @Test
    void settingOffGivesNothing() {
        assertFalse(StarterWiki.shouldGive(false, new CompoundTag()));
    }
}
