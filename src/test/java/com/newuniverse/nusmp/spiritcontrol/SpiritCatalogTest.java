package com.newuniverse.nusmp.spiritcontrol;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the numbers of every spirit sit in one pure catalogue, so it is checked against the real source files: eight spirits with unique ids, at most four abilities each with unique words, every spell inside
 * sane bounds (a starter-page sized hit, a price in units, a cooldown, a range), every effect shape a real VfxShape constant, every magic a real MagicType, and the Spirit Lord's type mapping.
 */
class SpiritCatalogTest {
    private static final Path ROOT = Path.of("src/main/java/com/newuniverse/nusmp");

    private static Set<String> constants(String file) throws IOException {
        String src = Files.readString(ROOT.resolve(file), StandardCharsets.UTF_8);
        Set<String> out = new HashSet<>();
        Matcher m = Pattern.compile("(?m)^\\s+([A-Z][A-Z0-9_]+)\\s*[,;(]").matcher(src);
        while (m.find()) out.add(m.group(1));
        return out;
    }

    @Test
    void eightSpiritsWithUniqueIdsAndAtMostFourAbilitiesEach() {
        Set<String> ids = new HashSet<>();
        for (SpiritCatalog.Entry e : SpiritCatalog.all()) {
            assertTrue(ids.add(e.id()), "duplicate id " + e.id());
            assertTrue(e.abilities().size() <= PageGate.MAX_ABILITIES, e.id());
            Set<String> words = new HashSet<>();
            for (SpiritAbilityDef d : e.abilities()) assertTrue(words.add(d.id()), e.id() + " lists " + d.id() + " twice");
            assertFalse(e.spells().isEmpty(), e.id() + " has no spell");
            assertFalse(e.magics().isEmpty(), e.id());
        }
        assertEquals(8, ids.size());
        assertEquals(Set.of("salamander", "sylph", "undine", "gnome", "spirit_light", "spirit_dark", "spirit_space", "lord"), ids);
    }

    @Test
    void spaceHasFourAbilitiesAndTwoTakeASlot() {
        assertEquals(4, SpiritCatalog.SPACE_SPIRIT.abilities().size());
        long slots = SpiritCatalog.SPACE_SPIRIT.abilities().stream().filter(SpiritAbilityDef::takesSlot).count();
        assertEquals(2, slots, "the portal and the waypoint");
        assertEquals(3, SpiritCatalog.LIGHT_SPIRIT.abilities().size(), "beam, pulse, seraph");
        assertEquals(2, SpiritCatalog.DARK_SPIRIT.abilities().size(), "step, demon form");
        assertEquals(0, SpiritCatalog.LORD.abilities().size());
        for (SpiritAbilityDef d : SpiritCatalog.SPACE_SPIRIT.abilities()) assertEquals(0.0, d.pageCostPercent(), "the Space spirit charges its own mana: " + d.id());
    }

    @Test
    void everySpellIsInsideTheBounds() {
        for (SpiritCatalog.Entry e : SpiritCatalog.all()) {
            for (SpiritSpellDef s : e.spells()) {
                assertTrue(s.raw() >= 3f && s.raw() <= 15f, e.id() + "/" + s.id() + " raw " + s.raw() + " is a starter or mid page sized hit");
                assertTrue(s.units() >= 5 && s.units() <= 30, e.id() + "/" + s.id() + " units");
                assertTrue(s.cooldownTicks() >= 20 && s.cooldownTicks() <= 400, e.id() + "/" + s.id() + " cooldown");
                assertTrue(s.range() >= 8 && s.range() <= 24, e.id() + "/" + s.id() + " range");
            }
        }
        for (SpiritSpellDef s : SpiritCatalog.ANTI_SPELLS) assertTrue(s.raw() <= 15f);
    }

    @Test
    void everyShapeIsARealVfxShapeAndEveryMagicARealMagicType() throws IOException {
        Set<String> shapes = constants("vfx/VfxShape.java");
        Set<String> magics = constants("blackclover/MagicType.java");
        for (SpiritCatalog.Entry e : SpiritCatalog.all()) {
            for (SpiritSpellDef s : e.spells()) assertTrue(shapes.contains(s.shape()), e.id() + "/" + s.id() + " names the missing shape " + s.shape());
            for (String m : e.magics()) assertTrue(magics.contains(m), e.id() + " names the missing magic " + m);
        }
        for (SpiritSpellDef s : SpiritCatalog.ANTI_SPELLS) assertTrue(shapes.contains(s.shape()), s.shape());
    }

    @Test
    void theSpiritLordSpellsAndMagicFollowTheBondType() {
        assertEquals(SpiritCatalog.FIRE_SPELLS, SpiritCatalog.lordSpells("Salamander"));
        assertEquals(SpiritCatalog.WATER_SPELLS, SpiritCatalog.lordSpells("undine"));
        assertEquals(SpiritCatalog.WIND_SPELLS, SpiritCatalog.lordSpells("SYLPH"));
        assertEquals(SpiritCatalog.EARTH_SPELLS, SpiritCatalog.lordSpells("Gnome"));
        assertEquals(SpiritCatalog.ANTI_SPELLS, SpiritCatalog.lordSpells("Anti"));
        assertEquals(SpiritCatalog.FIRE_SPELLS, SpiritCatalog.lordSpells(null));
        assertEquals("FLAME", SpiritCatalog.lordMagic("Salamander"));
        assertEquals("WATER", SpiritCatalog.lordMagic("Undine"));
        assertEquals("WIND", SpiritCatalog.lordMagic("Sylph"));
        assertEquals("EARTH", SpiritCatalog.lordMagic("Gnome"));
        assertEquals("ANTI_MAGIC", SpiritCatalog.lordMagic("Anti"));
        assertEquals(null, SpiritCatalog.lordMagic("-"));
        assertNotNull(SpiritCatalog.LORD.magics());
        assertTrue(SpiritCatalog.LORD.magics().containsAll(Set.of("FLAME", "WATER", "WIND", "EARTH", "ANTI_MAGIC")));
    }
}
