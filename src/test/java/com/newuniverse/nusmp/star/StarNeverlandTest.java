package com.newuniverse.nusmp.star;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: Black Neverland now follows the manga (Yuno's Star + Sylph Union Magic with Asta's anti-magic, ch. 381 to 383): the heir mark stays the base gate, the Sylph bond and the anti-magic source are server flags
 * behind {@code star.neverlandCanonGate}, and the anti-magic dome fades after 80 of its 100 ticks. The decision table is pure, so every combination of the three facts and the switch is pinned here, together
 * with the numbers the doc quotes and the wiring of the cast (read from the source, since nobody can play it here).
 */
class StarNeverlandTest {
    private static StarNeverlandRules.Plan plan(boolean heir, boolean sylph, boolean source, boolean gate) { return StarNeverlandRules.plan(heir, sylph, source, gate); }

    @Test
    void theHeirMarkIsAlwaysTheBaseGateWhateverTheSwitchSays() {
        for (boolean sylph : new boolean[]{false, true}) {
            for (boolean source : new boolean[]{false, true}) {
                for (boolean gate : new boolean[]{false, true}) {
                    StarNeverlandRules.Plan p = plan(false, sylph, source, gate);
                    assertFalse(p.ok(), "a non-heir never casts it");
                    assertEquals(StarNeverlandRules.NOT_HEIR, p.problem());
                    assertFalse(p.antiMagic());
                }
            }
        }
    }

    @Test
    void withTheCanonGateTheHeirNeedsTheSylphBondAndTheAntiMagicSource() {
        assertEquals(StarNeverlandRules.NEEDS_SYLPH, plan(true, false, false, true).problem());
        assertEquals(StarNeverlandRules.NEEDS_SYLPH, plan(true, false, true, true).problem());
        assertEquals(StarNeverlandRules.NEEDS_ANTI_MAGIC, plan(true, true, false, true).problem());
        StarNeverlandRules.Plan ok = plan(true, true, true, true);
        assertTrue(ok.ok());
        assertNull(ok.problem());
        assertTrue(ok.antiMagic(), "the canon dome cancels magic");
    }

    @Test
    void relaxingTheGateNeedsOnlyTheHeirMarkAndKeepsTheAntiMagicPartForWhoeverHasTheSource() {
        StarNeverlandRules.Plan plain = plan(true, false, false, false);
        assertTrue(plain.ok());
        assertFalse(plain.antiMagic(), "no source: phase 1's plain dome");
        assertTrue(plan(true, true, false, false).ok() && !plan(true, true, false, false).antiMagic());
        assertTrue(plan(true, false, true, false).ok() && plan(true, false, true, false).antiMagic(), "a source without the Sylph bond still gets the anti-magic dome when the gate is off");
        assertTrue(plan(true, true, true, false).antiMagic());
    }

    @Test
    void theCanonGateIsOnByDefaultAndUsesAFixedKey() {
        assertTrue(StarNeverlandRules.DEFAULT_CANON_GATE);
        assertEquals("neverlandCanonGate", StarNeverlandRules.CONFIG_KEY);
        assertEquals("sylph_bond", StarNeverlandRules.FLAG_SYLPH);
        assertEquals("anti_magic_source", StarNeverlandRules.FLAG_ANTI_MAGIC);
    }

    @Test
    void theAntiMagicFadesAfterEightyTicksAndTheDomeHasFivePulses() {
        assertEquals(5, StarNeverlandRules.pulses());
        assertEquals(4, StarNeverlandRules.cancellingPulses(), "ages 0, 20, 40 and 60 cancel; the pulse at 80 is plain");
        assertTrue(StarNeverlandRules.cancelsAt(0) && StarNeverlandRules.cancelsAt(79));
        assertFalse(StarNeverlandRules.cancelsAt(80) || StarNeverlandRules.cancelsAt(100));
        assertTrue(StarNeverlandRules.jamsAt(0) && !StarNeverlandRules.jamsAt(20) && StarNeverlandRules.jamsAt(40) && !StarNeverlandRules.jamsAt(60), "a jam on every second pulse");
        assertTrue(StarNeverlandRules.FADE_AFTER < StarNeverlandRules.TICKS);
    }

    @Test
    void theNumbersStayInsideTheZoneKitAndTheAuraEconomy() {
        assertEquals(8.0, StarNeverlandRules.RADIUS, 1e-9);
        assertEquals(100, StarNeverlandRules.TICKS, "Pages15 zones are at most 100 ticks");
        assertEquals(20, StarNeverlandRules.STEP);
        assertTrue(StarNeverlandRules.STEP >= 4, "the zone step floor");
        assertEquals(6, StarNeverlandRules.RIM_STARS, "the manga's six anti-magic stars (the look draws six)");
        assertTrue(StarNeverlandRules.AURA_FRAC > 0.06 && StarNeverlandRules.AURA_FRAC < 0.12, "dearer than Black Hurricane (6 percent), cheaper than Black Asta (12)");
        assertTrue(StarNeverlandRules.AURA_FLOOR >= 25);
        assertTrue(StarNeverlandRules.SILENCE_TICKS <= StarNeverlandRules.STEP * 2, "silence does not outlast the next pulse by much");
        assertTrue(StarNeverlandRules.JAM_SECONDS <= 5, "temporary for players");
    }

    // ------------------------------------------------------------------------------------------------ the source
    private static final Path JAVA = Path.of("").toAbsolutePath().resolve("src/main/java/com/newuniverse/nusmp");

    private static String code(String rel) throws IOException {
        return Files.readString(JAVA.resolve(rel), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }

    @Test
    void theCastReadsTheFlagsOnTheServerPaysAfterTheZoneAndOnlyCancelsInsideTheFade() throws IOException {
        String c = code("star/StarCasts.java");
        int at = c.indexOf("public static BookPage.Cast blackNeverland()");
        assertTrue(at > 0);
        String cast = c.substring(at);
        assertTrue(cast.contains("gate(c, StarRules.BLACK_NEVERLAND)"), "the switch and the heir mark come first");
        assertTrue(cast.contains("StarNeverlandRules.plan(true, StarGame.sylphBond(p), StarGame.antiMagicSource(p), StarConfig.neverlandCanonGate())"), "the flags and the setting decide");
        assertTrue(cast.contains("c.zone(100, 20,"), "one zone of 100 ticks in steps of 20");
        assertTrue(cast.indexOf("c.zone(100, 20,") < cast.indexOf("EnergyBridge.aura("), "the Aura is paid after the zone started (a busy zone slot costs nothing)");
        assertTrue(cast.contains("StarNeverlandRules.cancelsAt(age)") && cast.contains("Nullification.jamAll") && cast.contains("Nullification.shatterBarriers"), "the anti-magic part is gated by the fade");
        assertTrue(cast.contains("SpellRuntime.dissolveBolts(level, centre, radius, p, 6)"), "phase 1's bolts end (at most 6) is kept");
        assertFalse(cast.contains("new Thread") || cast.contains("getChunk(") || cast.contains("Minecraft.getInstance"));
        String g = code("star/StarGame.java");
        assertTrue(g.contains("public static boolean sylphBond(ServerPlayer p)") && g.contains("public static boolean antiMagicSource(ServerPlayer p)"), "both flags take the server player");
        String cfg = code("star/StarConfig.java");
        assertTrue(cfg.contains("StarNeverlandRules.CONFIG_KEY") && cfg.contains("NEVERLAND_CANON_GATE") && cfg.contains("neverlandCanonGate()"), "the server setting exists");
        assertNotNull(StarNeverlandRules.NEEDS_SYLPH);
    }
}
