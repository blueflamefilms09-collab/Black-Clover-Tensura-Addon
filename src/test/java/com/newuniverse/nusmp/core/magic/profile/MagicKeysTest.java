package com.newuniverse.nusmp.core.magic.profile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MagicKeysTest {
    @Test
    void restrictedNamesAreExactlyTheEightThatWereHardCodedBefore() {
        // the switch in MagicType.restricted() before 1.27.0
        List<String> before = List.of("ANTI_MAGIC", "KOTODAMA", "DEMON_BEAST", "CURSE", "DEMON_FIRE", "DEMON_ICE", "DEMON_LIGHT", "DEMON_WATER");
        assertEquals(before.size(), MagicKeys.RESTRICTED.size());
        for (String n : before) assertTrue(MagicKeys.isRestricted(n), n);
        assertFalse(MagicKeys.isRestricted("FLAME"));
        assertFalse(MagicKeys.isRestricted("WING"));
        assertFalse(MagicKeys.isRestricted(null));
        assertFalse(MagicKeys.isRestricted(""));
        assertTrue(MagicKeys.isRestricted("  demon fire "), "any spelling of the name");
    }

    @Test
    void restrictedSetCanNotBeEdited() {
        try { MagicKeys.RESTRICTED.add("FLAME"); fail("the restricted set must be read-only"); }
        catch (UnsupportedOperationException expected) { /* good */ }
    }

    @Test
    void canonicalIdsAreStable() {
        assertEquals("ICE_WEDGE", MagicKeys.canonical("ice wedge"));
        assertEquals("ICE_WEDGE", MagicKeys.canonical(" Ice-Wedge "));
        assertEquals("merged:flame+water", MagicKeys.canonical("MERGED:Flame+Water"));
        assertEquals("", MagicKeys.canonical(null));
        assertTrue(MagicKeys.isMerged("merged:a+b"));
        assertTrue(MagicKeys.isMerged(" Merged:a+b"));
        assertFalse(MagicKeys.isMerged("FLAME"));
        assertFalse(MagicKeys.isMerged(null));
    }

    @Test
    void prettifyNeverReturnsBlank() {
        assertEquals("Ice Wedge", MagicKeys.prettify("ICE_WEDGE"));
        assertEquals("Flame-Water", MagicKeys.prettify("merged:flame+water"));
        assertEquals("Unknown Magic", MagicKeys.prettify(null));
        assertEquals("Unknown Magic", MagicKeys.prettify("   "));
        assertEquals("Unknown Magic", MagicKeys.prettify("merged:"));
    }

    @Test
    void pathsAndKeys() {
        assertEquals("textures/skill/grimoire/ice_wedge.png", MagicKeys.iconPath("ICE_WEDGE"));
        assertEquals("textures/skill/icons/water_buff.png", MagicKeys.buffIconPath("WATER"));
        assertEquals("textures/skill/icons/water_ultimate.png", MagicKeys.ultimateIconPath("water"));
        assertEquals("book_fire", MagicKeys.bookPath("FLAME"), "the one book with another registry id");
        assertEquals("book_air", MagicKeys.bookPath("AIR"));
        assertEquals("nusmp.skill.book_air", MagicKeys.bookNameKey("book_air"));
        assertEquals("nusmp.skill.book_air.description", MagicKeys.bookDescriptionKey("book_air"));
        assertEquals("IMITATION", MagicKeys.auraName("COPY"));
        assertEquals("BONE", MagicKeys.auraName("bone"));
    }
}
