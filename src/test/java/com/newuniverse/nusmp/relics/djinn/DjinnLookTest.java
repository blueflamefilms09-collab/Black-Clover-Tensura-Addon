package com.newuniverse.nusmp.relics.djinn;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 3.75.0: the look of the eight kinds of Djinn magic names real effects and real particle themes, and no two elements look alike. */
class DjinnLookTest {
    private static final Path ROOT = Path.of("src/main");

    @Test void everyMomentNamesAnEffectThatExists() throws Exception {
        String src = Files.readString(ROOT.resolve("java/com/newuniverse/nusmp/vfx/VfxShape.java"));
        Set<String> shapes = new HashSet<>();
        Matcher m = Pattern.compile("\\b([A-Z][A-Z0-9_]{2,})\\b").matcher(src);
        while (m.find()) shapes.add(m.group(1));
        for (Affinity a : Affinity.values())
            for (DjinnLook.Moment mo : DjinnLook.Moment.values())
                assertTrue(shapes.contains(DjinnLook.shape(a, mo)), a + " " + mo + " names " + DjinnLook.shape(a, mo) + ", which VfxShape does not have");
    }

    @Test void everyElementHasItsOwnThemeAndTheThemeExists() throws Exception {
        String themes = Files.readString(ROOT.resolve("resources/data/nusmp/particle_themes.json"));
        Set<String> seen = new HashSet<>();
        for (Affinity a : Affinity.values()) {
            assertTrue(themes.contains("\"" + DjinnLook.theme(a) + "\""), "no particle theme " + DjinnLook.theme(a));
            assertTrue(seen.add(DjinnLook.theme(a)), "two elements share " + DjinnLook.theme(a));
        }
    }

    @Test void noTwoElementsShareTheirImpactOrTheirStrike() {
        Set<String> impact = new HashSet<>(), cast = new HashSet<>();
        for (Affinity a : Affinity.values()) {
            assertTrue(impact.add(DjinnLook.shape(a, DjinnLook.Moment.IMPACT)), "impact shared: " + a);
            assertTrue(cast.add(DjinnLook.shape(a, DjinnLook.Moment.CAST)), "cast shared: " + a);
        }
    }

    @Test void aDjinnsColourLeadsToItsElementOrToNothing() {
        int own = 0;
        for (Djinn d : Djinn.values()) {
            for (int rgb : new int[] {d.color(), d.glow()}) {
                Affinity a = DjinnLook.affinityOf(rgb);
                assertTrue(a == null || a == d.affinity(), d + ": colour " + Integer.toHexString(rgb) + " leads to " + a);
            }
            if (DjinnLook.affinityOf(d.color()) == d.affinity()) own++;
        }
        assertTrue(own >= Djinn.values().length - 2, "only " + own + " body colours resolve");
        assertNull(DjinnLook.affinityOf(0x123456));
        assertNull(DjinnLook.affinityOf(0x5A146E), "the dark Djinn's violet must stay the black Rukh");
    }

    @Test void theMomentsAreShort() {
        for (DjinnLook.Moment m : DjinnLook.Moment.values()) assertTrue(DjinnLook.ticks(m) > 0 && DjinnLook.ticks(m) <= 80);
    }
}
