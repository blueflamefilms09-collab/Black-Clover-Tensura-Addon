package com.newuniverse.nusmp.client.relics.djinn.gui;

import com.newuniverse.nusmp.relics.djinn.Affinity;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.Djinn.Look;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The whole panel drawn onto a recording canvas: nothing leaves the panel, every state and Djinn paints, the animation moves, Djinn differ. */
class GuiPainterTest {

    /** Records every call as a running hash and checks the rectangles stay inside the panel. */
    static final class Rec implements Canvas {
        long hash = 7;
        int fills, texts, minX = 1 << 20, minY = 1 << 20, maxX = -(1 << 20), maxY = -(1 << 20), badAlpha;

        public void fill(int x1, int y1, int x2, int y2, int argb) {
            fills++;
            minX = Math.min(minX, x1);
            minY = Math.min(minY, y1);
            maxX = Math.max(maxX, x2);
            maxY = Math.max(maxY, y2);
            if (x2 <= x1 || y2 <= y1 || (argb >>> 24) == 0) badAlpha++;
            hash = hash * 31 + x1 * 7 + y1 * 13 + x2 * 17 + y2 * 19 + argb;
        }

        public void text(String s, int x, int y, int argb) {
            texts++;
            hash = hash * 31 + s.hashCode() + x + y * 3 + argb;
            assertTrue(!s.isEmpty());
            assertTrue((argb >>> 24) > 8, "text alpha that Minecraft would draw opaque");
        }

        public int textWidth(String s) { return s.length() * 6; }
    }

    static GuiState state(int phase, float time) {
        GuiState s = new GuiState();
        s.phase = phase;
        s.prev = phase;
        s.time = time;
        s.magoi = 60;
        s.assim = 300;
        s.trust = 33;
        s.timeMax = phase == 1 ? 400 : phase == 3 ? 1800 : 60;
        s.timeLeft = s.timeMax / 2;
        return s;
    }

    static Rec paint(GuiTheme th, GuiState st) {
        Rec r = new Rec();
        new DjinnGuiPainter().paint(r, th, st, 0, 0);
        return r;
    }

    /** Every Djinn of the table plus hand made ones: every magic with every look, so a Djinn the table does not have yet is covered. */
    static List<GuiSpec> specs() {
        List<GuiSpec> out = new ArrayList<>();
        for (Djinn d : Djinn.values()) out.add(GuiSpec.of(d));
        Look.Horns[] hs = Look.Horns.values();
        Look.Wings[] ws = Look.Wings.values();
        Look.Tail[] ts = Look.Tail.values();
        Look.Halo[] hl = Look.Halo.values();
        int i = 0;
        for (Affinity a : Affinity.values())
            for (int k = 0; k < 6; k++, i++)
                out.add(new GuiSpec("synthetic_" + i, "Synthetic" + i, "A very very long title that cannot fit the column", "Form", 0x202020 + i * 0x0B1C27 % 0xDFDFDF, 0xFFFFFF - i * 0x050301,
                        a, new Look(hs[i % hs.length], ws[(i / 2) % ws.length], ts[i % ts.length], hl[(i / 3) % hl.length], i % 2 == 0)));
        out.add(new GuiSpec("no_affinity", "Nobody", null, "", 0x000000, 0xFFFFFF, null, null));
        return out;
    }

    @Test
    void everyDjinnPaintsInEveryStateInsideThePanel() {
        for (GuiSpec spec : specs())
            for (boolean dark : new boolean[]{false, true}) {
                GuiTheme th = GuiTheme.of(spec, dark);
                for (int phase = 0; phase <= 4; phase++)
                    for (float time : new float[]{0f, 3.7f, 61f, 1234.5f, 999999f}) {
                        GuiState st = state(phase, time);
                        if (phase == 0) { st.lockLeft = 2000; st.coolLeft = 9000; }
                        Rec r = paint(th, st);
                        String ctx = spec.id() + " dark=" + dark + " phase=" + phase + " t=" + time;
                        assertTrue(r.fills > 100, "something is drawn " + ctx);
                        assertTrue(r.fills < 2600, "cheap enough: " + r.fills + " fills " + ctx);
                        assertTrue(r.minX >= 0 && r.minY >= 0 && r.maxX <= GuiLayout.W && r.maxY <= GuiLayout.H, "inside the panel " + ctx);
                        assertEquals(0, r.badAlpha, "no empty or invisible fills " + ctx);
                        assertTrue(r.texts >= 3, "name, phase and trust " + ctx);
                    }
            }
    }

    @Test
    void oddStatesNeverBreakThePainter() {
        GuiTheme th = GuiTheme.of(Djinn.AMON, false);
        float[] odd = {-50f, 0f, 1f, 1e6f, Float.MAX_VALUE / 1e30f};
        for (float v : odd) {
            GuiState st = state(3, 100f);
            st.timeLeft = v;
            st.timeMax = v;
            st.lockLeft = v;
            st.lockMax = v;
            st.coolLeft = v;
            st.coolMax = v;
            st.phaseAge = v;
            st.introAge = v;
            st.pulse = Math.min(1f, Math.abs(v));
            st.magoi = (int) v;
            st.assim = (int) v;
            st.trust = (int) v;
            Rec r = paint(th, st);
            assertTrue(r.minX >= 0 && r.maxX <= GuiLayout.W && r.minY >= 0 && r.maxY <= GuiLayout.H, "inside for " + v);
        }
        for (int phase : new int[]{-1, 5, 99}) paint(th, state(phase, 10f));
    }

    @Test
    void theSummonIntroUnfoldsAndFadesIn() {
        GuiTheme th = GuiTheme.of(Djinn.BAAL, false);
        int last = -1;
        Set<Long> frames = new HashSet<>();
        for (int age = 0; age <= GuiAnim.INTRO_TICKS + 2; age += 2) {
            GuiState st = state(3, 100f);
            st.introAge = age;
            Rec r = paint(th, st);
            frames.add(r.hash);
            assertTrue(r.minX >= 0 && r.maxX <= GuiLayout.W && r.minY >= 0 && r.maxY <= GuiLayout.H);
            if (age >= GuiAnim.INTRO_TICKS) last = r.fills;
        }
        assertTrue(frames.size() >= 12, "the intro changes every frame");
        GuiState done = state(3, 100f);
        done.introAge = 5000f;
        assertEquals(last > 0, paint(th, done).fills > 0);
        GuiState first = state(3, 100f);
        first.introAge = 0f;
        assertEquals(0, paint(th, first).fills, "nothing yet on the very first frame of the intro");
    }

    @Test
    void theSamePictureTwiceButItMovesWithTime() {
        for (Djinn d : Djinn.values()) {
            GuiTheme th = GuiTheme.of(d, false);
            assertEquals(paint(th, state(3, 321f)).hash, paint(th, state(3, 321f)).hash, d + " is deterministic");
            Set<Long> frames = new HashSet<>();
            for (int t = 0; t < 40; t++) frames.add(paint(th, state(3, 100f + t)).hash);
            assertTrue(frames.size() >= 36, d + " animates: " + frames.size() + " different frames of 40");
            Set<Long> idle = new HashSet<>();
            for (int t = 0; t < 40; t++) idle.add(paint(th, state(0, 100f + t)).hash);
            assertTrue(idle.size() >= 20, d + " has an idle animation too: " + idle.size());
        }
    }

    @Test
    void theFullEquipLooksDistinctFromTheOtherPhases() {
        for (Djinn d : Djinn.values()) {
            GuiTheme th = GuiTheme.of(d, false);
            Set<Long> phases = new HashSet<>();
            for (int ph = 0; ph <= 4; ph++) phases.add(paint(th, state(ph, 200f)).hash);
            assertEquals(5, phases.size(), d + ": five phases, five pictures");
            assertTrue(paint(th, state(3, 200f)).fills * 100 >= paint(th, state(0, 200f)).fills * 95, d + ": the Full Equip draws about as much as the sealed panel or more (a look with few wing / tail rectangles, such as Decarabia, draws up to 2 percent less; the pictures themselves differ, checked above)");
        }
    }

    @Test
    void everyDjinnHasItsOwnPanel() {
        Set<Long> pics = new HashSet<>();
        List<GuiSpec> all = specs();
        for (GuiSpec s : all) pics.add(paint(GuiTheme.of(s, false), state(3, 150f)).hash);
        assertEquals(all.size(), pics.size(), "no two Djinn share a picture");
        for (GuiSpec s : all) assertTrue(paint(GuiTheme.of(s, true), state(3, 150f)).hash != paint(GuiTheme.of(s, false), state(3, 150f)).hash, s.id() + " dark differs");
    }

    @Test
    void theTimerShowsInTheBarAndTheTextAndExtremeChargesUp() {
        GuiTheme th = GuiTheme.of(Djinn.ZEPAR, false);
        GuiState a = state(3, 100f), b = state(3, 100f);
        a.timeLeft = 1700;
        b.timeLeft = 100;
        assertTrue(a.timerFrac() > b.timerFrac());
        assertTrue(paint(th, a).hash != paint(th, b).hash);
        GuiState x = state(4, 100f);
        x.timeLeft = 60;
        assertEquals(0f, x.charge(), 1e-6f);
        x.timeLeft = 0;
        assertEquals(1f, x.charge(), 1e-6f);
        assertEquals(1f, state(2, 0f).timerFrac(), 1e-6f);
        assertEquals(0f, state(0, 0f).timerFrac(), 1e-6f);
    }

    @Test
    void labelsComeFromTheLangLookupWithFallbacks() {
        GuiLabels l = GuiLabels.from((k, fb) -> k.endsWith("phase.3") ? "Equipement" : fb);
        assertEquals("Equipement", l.phase[3]);
        assertEquals("Lesser Summon", l.phase[1]);
        assertEquals("Trust", l.trust);
        DjinnGuiPainter p = new DjinnGuiPainter();
        p.setLabels(l);
        p.setLabels(null);
        Rec r = new Rec();
        p.paint(r, GuiTheme.of(Djinn.VINEA, true), state(3, 5f), 10, 20);
        assertTrue(r.minX >= 10 && r.minY >= 20, "origin offsets are applied");
        assertTrue(r.maxX <= 10 + GuiLayout.W && r.maxY <= 20 + GuiLayout.H);
    }
}
