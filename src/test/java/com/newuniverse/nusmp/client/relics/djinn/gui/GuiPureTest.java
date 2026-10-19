package com.newuniverse.nusmp.client.relics.djinn.gui;

import com.newuniverse.nusmp.relics.djinn.Affinity;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.Djinn.Look;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The pure decisions of the active Djinn panel: palette, curves, tuning, themes, layout, the emblem raster. */
class GuiPureTest {
    private static final int[] COLOURS = {0x000000, 0xFFFFFF, 0x0000FF, 0x101040, 0x3C8CFF, 0xFF5A14, 0x2F55D8, 0x808080, 0x00FF00, 0x400000, 0xFFE070};

    // ---------------------------------------------------------------- palette
    @Test
    void textIsAlwaysBrightAgainstThePanelAndTheBodyColourIsLifted() {
        for (int c : COLOURS)
            for (int g : COLOURS)
                for (Affinity a : Affinity.values())
                    for (boolean dark : new boolean[]{false, true}) {
                        GuiPalette p = GuiPalette.of(c, g, a.tint, dark);
                        assertTrue(GuiColor.luma(p.text()) - GuiColor.luma(p.deep()) >= 0.5f, "text contrast " + Integer.toHexString(c) + "/" + Integer.toHexString(g));
                        assertTrue(GuiColor.luma(p.primary()) >= 0.29f, "primary lifted");
                        assertTrue(GuiColor.luma(p.glow()) >= 0.57f, "glow lifted");
                        assertTrue(GuiColor.luma(p.deep()) <= 0.30f, "the panel stays dark");
                        assertTrue(GuiColor.luma(p.textDim()) >= 0.57f, "dim text still readable");
                    }
    }

    @Test
    void theTwelveDjinnHaveTwelveDifferentPalettes() {
        Set<Integer> primaries = new HashSet<>();
        for (Djinn d : Djinn.values()) {
            GuiPalette p = GuiTheme.of(d, false).pal;
            primaries.add(p.primary());
            assertTrue(!GuiPalette.of(d.color(), d.glow(), d.affinity().tint, true).equals(p), "the dark palette differs from the light one");
        }
        assertEquals(Djinn.values().length, primaries.size(), "every Djinn has its own primary colour");
    }

    @Test
    void colourMathStaysInRange() {
        assertEquals(0x808080, GuiColor.mix(0x000000, 0xFFFFFF, 0.5f));
        assertEquals(0xFFFFFF, GuiColor.add(0xF0F0F0, 0x202020));
        assertEquals(0x80, GuiColor.fade(0xFF000000, 0.5f) >>> 24);
        assertTrue(GuiColor.luma(GuiColor.lift(0x000000, 0.6f)) >= 0.59f);
        assertTrue(GuiColor.luma(GuiColor.sink(0xFFFFFF, 0.4f)) <= 0.41f);
        assertEquals(0xFF123456, GuiColor.argb(0x123456, 1f));
    }

    // ---------------------------------------------------------------- curves
    @Test
    void curvesStayInTheirRanges() {
        for (float t = -5; t < 400; t += 0.37f) {
            assertTrue(GuiAnim.ramp(t, 0f, 14f) >= 0f && GuiAnim.ramp(t, 0f, 14f) <= 1f);
            float f = GuiAnim.flap(t, 0.05f);
            assertTrue(f >= 0f && f <= 1f, "flap " + f);
            int fr = GuiAnim.frame(f, Emblem.FRAMES);
            assertTrue(fr >= 0 && fr < Emblem.FRAMES);
            float b = GuiAnim.breathe(t, 40f, 0.2f, 0.9f);
            assertTrue(b >= 0.2f - 1e-5f && b <= 0.9f + 1e-5f);
            float tr = GuiAnim.tri(t);
            assertTrue(tr >= 0f && tr <= 1f);
            float n = GuiAnim.noise(t, 3);
            assertTrue(n >= 0f && n <= 1f);
        }
        assertEquals(1f, GuiAnim.easeOutBack(1f), 1e-5f);
        assertEquals(0f, GuiAnim.easeOutBack(0f), 1e-5f);
        assertTrue(GuiAnim.easeOutBack(0.7f) > 1f, "the unfolding overshoots once");
    }

    @Test
    void noiseIsRepeatableAndSpread() {
        assertEquals(GuiAnim.rnd(77, 3), GuiAnim.rnd(77, 3));
        double sum = 0;
        for (int i = 0; i < 4000; i++) {
            float r = GuiAnim.rnd(i);
            assertTrue(r >= 0f && r < 1f);
            sum += r;
        }
        assertTrue(Math.abs(sum / 4000 - 0.5) < 0.03, "roughly uniform");
    }

    @Test
    void thePerimeterWalkStaysOnTheBorderAndCloses() {
        int[] p = new int[2];
        for (float d = -300; d < 900; d += 1.3f) {
            GuiAnim.perimeter(d, GuiLayout.W, GuiLayout.H, p);
            boolean onEdge = p[0] == 0 || p[1] == 0 || p[0] == GuiLayout.W - 1 || p[1] == GuiLayout.H - 1;
            assertTrue(onEdge && p[0] >= 0 && p[1] >= 0 && p[0] < GuiLayout.W && p[1] < GuiLayout.H, "point " + p[0] + "," + p[1]);
        }
        int[] q = new int[2];
        GuiAnim.perimeter(5f, GuiLayout.W, GuiLayout.H, p);
        GuiAnim.perimeter(5f + 2 * (GuiLayout.W + GuiLayout.H), GuiLayout.W, GuiLayout.H, q);
        assertEquals(p[0], q[0]);
        assertEquals(p[1], q[1]);
    }

    @Test
    void theEmblemRevealsMorePhaseByPhaseAndEnergyRises() {
        for (int ph = 0; ph < 4; ph++) {
            assertTrue(GuiAnim.revealOf(ph + 1) >= GuiAnim.revealOf(ph), "reveal grows with the phase");
            assertTrue(GuiAnim.energy(ph + 1) >= GuiAnim.energy(ph), "energy grows with the phase");
        }
        assertEquals(GuiAnim.revealOf(1), GuiAnim.reveal(3, 1, 0f), 1e-5f);
        assertEquals(GuiAnim.revealOf(3), GuiAnim.reveal(3, 1, 100f), 1e-5f);
        float mid = GuiAnim.reveal(3, 1, 7f);
        assertTrue(mid > GuiAnim.revealOf(1) && mid < GuiAnim.revealOf(3), "the change is eased, not a jump");
        assertEquals(1f, GuiAnim.intro(GuiAnim.INTRO_TICKS + 5), 1e-6f);
    }

    // ---------------------------------------------------------------- tuning and themes
    @Test
    void theTwelveFirstDjinnAreTunedByHandWithDistinctAccents() {
        Set<GuiAccent> seen = new HashSet<>();
        String[] ids = {"baal", "amon", "vinea", "focalor", "zagan", "zepar", "furfur", "marchosias", "sitri", "bifron", "paimon", "vassago"};
        for (String id : ids) {
            assertTrue(GuiAccent.isTuned(id), id);
            seen.add(GuiAccent.tuneOf(id, null).accent());
        }
        assertEquals(12, seen.size(), "twelve different signature shapes");
        for (Djinn d : Djinn.values()) assertNotNull(GuiAccent.tuneOf(d.id(), d.affinity()), d.id());
    }

    @Test
    void anyOtherDjinnGetsAStableDerivedTuningFromItsAffinityPool() {
        String[] ids = {"bael", "agares", "valefor", "barbatos", "gusion", "vepar", "shax", "buer", "andrealphus", "kimaris", "amdusias", "belial", "decarabia", "seere",
                "dantalion", "andromalius", "glasya_labolas", "haagenti", "crocell", "furcas", "balam", "alloces", "camio", "murmur", "a_future_one", ""};
        for (Affinity a : Affinity.values())
            for (String id : ids) {
                GuiAccent.Tune t = GuiAccent.tuneOf(id, a), again = GuiAccent.tuneOf(id, a);
                assertEquals(t, again, "stable");
                boolean inPool = false;
                for (GuiAccent c : GuiAccent.pool(a)) inPool |= c == t.accent();
                assertTrue(inPool, id + " on " + a);
                assertTrue(t.corner() >= 0 && t.corner() <= 3 && t.rune() >= 0 && t.rune() <= 2 && Math.abs(t.spin()) == 1 && t.tempo() > 0.5f && t.tempo() < 1.6f);
            }
        Set<GuiAccent> seen = new HashSet<>();
        for (String id : ids) seen.add(GuiAccent.tuneOf(id, Affinity.LIGHT).accent());
        assertTrue(seen.size() >= 2, "Djinn of one magic do not all share one shape");
        assertNotNull(GuiAccent.tuneOf(null, null));
    }

    @Test
    void everyAffinityHasItsOwnFrameAndAMissingOneStillGetsOne() {
        Set<GuiMotif> motifs = new HashSet<>();
        for (Affinity a : Affinity.values()) motifs.add(GuiMotif.of(a));
        assertEquals(Affinity.values().length, motifs.size(), "one frame theme per magic");
        assertNotNull(GuiMotif.of(null));
        assertEquals(GuiMotif.EMBERS, GuiMotif.of(Affinity.HEAT));
        assertEquals(GuiMotif.ARCS, GuiMotif.of(Affinity.LIGHTNING));
        assertEquals(GuiMotif.RIPPLE, GuiMotif.of(Affinity.WATER));
        assertEquals(GuiMotif.STREAKS, GuiMotif.of(Affinity.WIND));
        assertEquals(GuiMotif.VINES, GuiMotif.of(Affinity.LIFE));
        assertEquals(GuiMotif.CRACKS, GuiMotif.of(Affinity.STRENGTH));
        assertEquals(GuiMotif.RINGS, GuiMotif.of(Affinity.SOUND));
        assertEquals(GuiMotif.RAYS, GuiMotif.of(Affinity.LIGHT));
        for (GuiMotif m : GuiMotif.values())
            for (float e : new float[]{0f, 0.3f, 1f, 1.4f, 5f}) {
                int n = GuiParticles.count(m, e);
                assertTrue(n >= 4 && n <= m.particles * 2, m + " " + e);
            }
    }

    // ---------------------------------------------------------------- layout
    @Test
    void thePanelAlwaysFitsOnScreenAndBarsFillProportionally() {
        for (GuiLayout.Anchor a : GuiLayout.Anchor.values())
            for (int[] size : new int[][]{{427, 240}, {320, 180}, {240, 135}, {700, 400}}) {
                int x = GuiLayout.originX(a, size[0]), y = GuiLayout.originY(a, size[1]);
                assertTrue(x >= 0 && y >= 0, a + " " + size[0]);
                if (size[0] >= GuiLayout.W + 2 * GuiLayout.EDGE) assertTrue(x + GuiLayout.W <= size[0], "fits wide " + a);
                if (size[1] >= GuiLayout.H + GuiLayout.LIFT) assertTrue(y + GuiLayout.H <= size[1], "fits high " + a);
            }
        assertEquals(GuiLayout.originX(GuiLayout.Anchor.BOTTOM_CENTER, 400), GuiLayout.originX(null, 400));
        assertEquals(0, GuiLayout.fillWidth(100, 0f));
        assertEquals(0, GuiLayout.fillWidth(100, Float.NaN));
        assertEquals(100, GuiLayout.fillWidth(100, 7f));
        assertEquals(50, GuiLayout.fillWidth(100, 0.5f));
        assertTrue(GuiLayout.COL_X + GuiLayout.COL_W < GuiLayout.W);
        assertTrue(GuiLayout.FOOT_Y + 8 < GuiLayout.H - 3, "the footer text clears the bottom border art");
    }

    // ---------------------------------------------------------------- emblem
    @Test
    void everyLookMakesAnEmblemInsideTheMedallionDisc() {
        for (Look.Horns h : Look.Horns.values())
            for (Look.Wings w : Look.Wings.values())
                for (boolean ears : new boolean[]{false, true}) {
                    Emblem e = Emblem.of(new Look(h, w, Look.Tail.NONE, Look.Halo.NONE, ears));
                    assertTrue(e.body.length > 20, "a body for " + h + "/" + w);
                    check(e.body, 1 << Emblem.P_BODY | 1 << Emblem.P_HORN | 1 << Emblem.P_EAR);
                    boolean hornsDrawn = false, earsDrawn = false;
                    for (int r : e.body) {
                        hornsDrawn |= Emblem.part(r) == Emblem.P_HORN;
                        earsDrawn |= Emblem.part(r) == Emblem.P_EAR;
                    }
                    assertEquals(h != Look.Horns.NONE, hornsDrawn, "horns " + h);
                    assertEquals(ears, earsDrawn, "ears");
                    Set<Integer> shapes = new HashSet<>();
                    for (int f = 0; f < Emblem.FRAMES; f++) {
                        check(e.wings[f], 1 << Emblem.P_WING | 1 << Emblem.P_WING2);
                        assertEquals(w != Look.Wings.NONE, e.wings[f].length > 0, "wings " + w + " frame " + f);
                        shapes.add(java.util.Arrays.hashCode(e.wings[f]));
                    }
                    if (w != Look.Wings.NONE) assertTrue(shapes.size() >= 3, "the wing beats through different shapes: " + w);
                }
        assertNotNull(Emblem.of(null));
        assertSame(Emblem.of(Djinn.BAAL.look()), Emblem.of(Djinn.BAAL.look()), "cached");
    }

    private static void check(int[] runs, int partMask) {
        float r2 = Emblem.DISC_R * Emblem.DISC_R;
        for (int r : runs) {
            int x = Emblem.x(r), y = Emblem.y(r), len = Emblem.len(r);
            assertTrue(len >= 1 && x + len <= Emblem.GW && y < Emblem.GH, "run in the grid");
            assertTrue((partMask >> Emblem.part(r) & 1) != 0, "known part");
            for (int i = 0; i < len; i++) {
                float dx = x + i + 0.5f - Emblem.GW / 2f, dy = y + 0.5f - Emblem.GH / 2f;
                assertTrue(dx * dx + dy * dy <= r2 + 0.01f, "cell inside the disc");
            }
        }
    }

    @Test
    void differentLooksGiveDifferentSilhouettes() {
        Set<Integer> shapes = new HashSet<>();
        for (Djinn d : Djinn.values()) shapes.add(java.util.Arrays.hashCode(Emblem.of(d.look()).body) * 31 + java.util.Arrays.hashCode(Emblem.of(d.look()).wings[1]));
        Set<Look> looks = new HashSet<>();
        for (Djinn d : Djinn.values()) looks.add(new Look(d.look().horns(), d.look().wings(), Look.Tail.NONE, Look.Halo.NONE, d.look().ears()));
        assertEquals(looks.size(), shapes.size(), "one silhouette per distinct horns/wings/ears");
    }

    @Test
    void timeTextIsCachedAndClamped() {
        assertEquals("0s", DjinnGuiPainter.time(-4));
        assertEquals("42s", DjinnGuiPainter.time(42));
        assertEquals("1:05", DjinnGuiPainter.time(65));
        assertEquals("5:00", DjinnGuiPainter.time(300));
        assertEquals("30:00", DjinnGuiPainter.time(99999));
        assertSame(DjinnGuiPainter.time(77), DjinnGuiPainter.time(77));
    }
}
