package com.newuniverse.nusmp.particle.fx;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a relic's code only knows its colour, and {@link FxHue} turns the colour into the relic element theme (red = fire, blue = water, green = nature, gold = light, ...) so that two relics
 * with two colours spawn different particles. The colour wheel has to answer sensibly at its landmarks, never answer an element the theme table does not have, and keep the old Rukh rule
 * for the dark flavour (luminance under 0.35).
 */
class FxHueTest {
    @Test
    void theLandmarksOfTheColourWheelAnswerTheirElement() {
        assertEquals("fire", FxHue.element(0xFF7A2E));
        assertEquals("fire", FxHue.element(0xFF9A00));
        assertEquals("blood", FxHue.element(0xA8182B));
        assertEquals("light", FxHue.element(0xFFE45E));
        assertEquals("light", FxHue.element(0xFFD700));
        assertEquals("nature", FxHue.element(0x55FF55));
        assertEquals("nature", FxHue.element(0x2E8B57));
        assertEquals("ice", FxHue.element(0x55FFFF));
        assertEquals("water", FxHue.element(0x3F8FFF));
        assertEquals("arcane", FxHue.element(0x7A3FFF));
        assertEquals("dream", FxHue.element(0xFF77FF));
        assertEquals("dark", FxHue.element(0x101018));
        assertEquals("dark", FxHue.element(0x000000));
        assertEquals("holy", FxHue.element(0xFFFFFF));
        assertEquals("metal", FxHue.element(0xAAAAAA));
    }

    @Test
    void everyAnswerIsARelicElementThemeOfTheTable() throws IOException {
        ParticleThemes.Table t = ParticleThemes.parse(JsonParser.parseString(Files.readString(Path.of("").toAbsolutePath().resolve("src/main/resources/data/nusmp/particle_themes.json"), StandardCharsets.UTF_8)));
        Set<String> seen = new HashSet<>();
        for (int r = 0; r < 256; r += 17)
            for (int g = 0; g < 256; g += 17)
                for (int b = 0; b < 256; b += 17) {
                    String id = FxHue.themeId((r << 16) | (g << 8) | b);
                    assertTrue(t.has(id), id + " is not a theme");
                    seen.add(id);
                }
        assertTrue(seen.size() >= 9, "the wheel only reaches " + seen);
    }

    @Test
    void darkColoursTakeTheDarkFlavourLikeTheOldRukhRule() {
        assertEquals(FxTone.DARK, FxHue.tone(0x000000));
        assertEquals(FxTone.DARK, FxHue.tone(0x301040));
        assertEquals(FxTone.LIGHT, FxHue.tone(0xFFFFFF));
        assertEquals(FxTone.LIGHT, FxHue.tone(0xFF7A2E));
        assertEquals(FxTone.LIGHT, FxHue.tone(0x55FF55));
    }
}
