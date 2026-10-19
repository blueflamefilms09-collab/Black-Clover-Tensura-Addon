package com.newuniverse.nusmp.client.ui.scale;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the pure rules of the GUI Size system: clamping, override resolution, fit-to-window, the mouse transform round trip, config parsing, the registry. */
class GuiScaleTest {
    private static final double EPS = 1e-9;

    @Test
    void clampKeepsHalfToDoubleAndRepairsGarbage() {
        assertEquals(0.5, GuiScaleMath.clamp(0.1), EPS);
        assertEquals(2.0, GuiScaleMath.clamp(9.0), EPS);
        assertEquals(1.25, GuiScaleMath.clamp(1.25), EPS);
        assertEquals(1.0, GuiScaleMath.clamp(Double.NaN), EPS);
        assertEquals(1.0, GuiScaleMath.clamp(Double.POSITIVE_INFINITY), EPS);
    }

    @Test
    void snapRoundsToTheStepAndStaysInRange() {
        assertEquals(1.25, GuiScaleMath.snap(1.2400001), EPS);
        assertEquals(1.0, GuiScaleMath.snap(1.01), EPS);
        assertEquals(2.0, GuiScaleMath.snap(3.0), EPS);
        assertEquals(0.5, GuiScaleMath.snap(0.0), EPS);
    }

    @Test
    void overrideWinsOverGlobalAndZeroFollowsGlobal() {
        assertEquals(1.5, GuiScaleMath.resolve(1.0, 1.5), EPS);
        assertEquals(1.2, GuiScaleMath.resolve(1.2, 0.0), EPS);
        assertEquals(1.2, GuiScaleMath.resolve(1.2, -1.0), EPS);
        assertEquals(1.2, GuiScaleMath.resolve(1.2, Double.NaN), EPS);
        assertEquals(0.5, GuiScaleMath.resolve(1.2, 0.2), EPS);          // a tiny positive override is a real (clamped) override
        assertTrue(GuiScaleMath.hasOverride(0.7));
        assertFalse(GuiScaleMath.hasOverride(0.0));
    }

    @Test
    void fitToWindowNeverEnlargesPastTheWindow() {
        // a 340 x 236 panel in a 480 x 270 window can grow to 270 / 236 = 1.14
        assertEquals(1.1, GuiScaleMath.effective(2.0, true, 340, 236, 480, 270), EPS);
        assertEquals(2.0, GuiScaleMath.effective(2.0, false, 340, 236, 480, 270), EPS);
        // a full-window screen (design 0) cannot grow, but can shrink
        assertEquals(1.0, GuiScaleMath.effective(1.7, true, 0, 0, 480, 270), EPS);
        assertEquals(0.75, GuiScaleMath.effective(0.75, true, 0, 0, 480, 270), EPS);
        // a window already smaller than the design is not shrunk against the player's will
        assertEquals(1.0, GuiScaleMath.effective(1.0, true, 600, 400, 480, 270), EPS);
        assertTrue(GuiScaleMath.fitLimit(340, 236, 480, 270) >= 1.0);
        // the drawn size never exceeds the limit
        for (double asked = 0.5; asked <= 2.0; asked += 0.05) {
            double s = GuiScaleMath.effective(asked, true, 230, 190, 427, 240);
            assertTrue(230 * s <= 427 + 1e-6 && 190 * s <= 240 + 1e-6 || s <= 1.0 + 1e-9, "scale " + s + " for " + asked);
        }
    }

    @Test
    void mouseTransformRoundTrips() {
        double[] pivots = {0, 213.5, 480};
        double[] scales = {0.5, 0.75, 1.0, 1.35, 2.0};
        for (double p : pivots) for (double s : scales) for (double m = -10; m < 500; m += 37.3) {
            assertEquals(m, GuiScaleMath.toScreen(GuiScaleMath.toLocal(m, p, s), p, s), 1e-9);
            assertEquals(m, GuiScaleMath.toLocal(GuiScaleMath.toScreen(m, p, s), p, s), 1e-9);
        }
        // the pivot itself never moves, and a point twice as far from the centre at 2x is half as far in local space
        assertEquals(240.0, GuiScaleMath.toLocal(240.0, 240.0, 1.7), EPS);
        assertEquals(240.0 + 50.0, GuiScaleMath.toLocal(240.0 + 100.0, 240.0, 2.0), EPS);
        assertEquals(5.0, GuiScaleMath.deltaToLocal(10.0, 2.0), EPS);
        assertTrue(GuiScaleMath.isIdentity(1.0));
        assertFalse(GuiScaleMath.isIdentity(1.05));
    }

    @Test
    void anchorsPickTheNinePivots() {
        assertEquals(0f, GuiScaleMath.Anchor.TOP_LEFT.pivotX(400), 0f);
        assertEquals(200f, GuiScaleMath.Anchor.BOTTOM_CENTER.pivotX(400), 0f);
        assertEquals(300f, GuiScaleMath.Anchor.BOTTOM_CENTER.pivotY(300), 0f);
        assertEquals(400f, GuiScaleMath.Anchor.TOP_RIGHT.pivotX(400), 0f);
        assertEquals(150f, GuiScaleMath.Anchor.CENTER.pivotY(300), 0f);
        assertEquals(9, GuiScaleMath.Anchor.values().length);
    }

    @Test
    void configValuesParse() {
        assertEquals(1.25, GuiScaleMath.parse("1.25"), EPS);
        assertEquals(1.25, GuiScaleMath.parse(" 1,25 "), EPS);
        assertEquals(2, GuiScaleMath.parse(2), EPS);
        assertTrue(Double.isNaN(GuiScaleMath.parse("big")));
        assertTrue(Double.isNaN(GuiScaleMath.parse(null)));
        assertEquals("125%", GuiScaleMath.percent(1.25));

        Map<String, Object> raw = new HashMap<>();
        raw.put("codex", "1.5");
        raw.put("boss_bar", 0.1);
        raw.put("hub_quest", "rubbish");
        raw.put("not_a_gui", 3.0);
        Map<String, Double> parsed = GuiScaleRegistry.parseOverrides(raw);
        assertEquals(1.5, parsed.get("codex"), EPS);
        assertEquals(0.5, parsed.get("boss_bar"), EPS);
        assertEquals(0.0, parsed.get("hub_quest"), EPS);
        assertEquals(0.0, parsed.get("hub_shop"), EPS);
        assertFalse(parsed.containsKey("not_a_gui"));
        assertEquals(GuiScaleRegistry.all().size(), parsed.size());
    }

    @Test
    void registryIsConsistentWithTheSources() throws IOException {
        Set<String> ids = new HashSet<>();
        Path root = Path.of("src/main/java");
        String all = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/ui/GuiScale.java"));
        assertNotNull(all);
        for (GuiScaleRegistry.Entry e : GuiScaleRegistry.all()) {
            assertTrue(ids.add(e.id()), "duplicate id " + e.id());
            assertTrue(e.id().matches("[a-z][a-z0-9_]*"), "bad id " + e.id());
            if (e.kind() == GuiScaleRegistry.Kind.SCREEN) {
                Path src = root.resolve(e.className().replace('.', '/') + ".java");
                assertTrue(Files.exists(src), "screen class has no source: " + e.className());
                String text = Files.readString(src);
                assertTrue(text.contains("extends Screen") || text.contains("extends AbstractContainerScreen"), e.className() + " is not a screen");
                assertEquals(e.className().substring(e.className().lastIndexOf('.') + 1), src.getFileName().toString().replace(".java", ""));
            } else if (e.mode() == GuiScaleRegistry.Mode.LAYER) {
                boolean found = false;
                try (var files = Files.walk(root)) {
                    for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".java"))::iterator) {
                        if (Files.readString(f).contains("\"" + e.layer() + "\"")) { found = true; break; }
                    }
                }
                assertTrue(found, "no layer registered as " + e.layer());
            }
        }
        assertEquals(e("codex"), GuiScaleRegistry.byClass("com.newuniverse.nusmp.client.ui.CodexScreen"));
        assertEquals(e("quest_tracker"), GuiScaleRegistry.byLayer("hub_quest_tracker"));
        assertTrue(GuiScaleRegistry.exempt("com.newuniverse.nusmp.client.mega.system.SystemEditScreen"));
        assertFalse(GuiScaleRegistry.exempt("com.newuniverse.nusmp.client.ui.CodexScreen"));
    }

    private static GuiScaleRegistry.Entry e(String id) {
        return GuiScaleRegistry.byId(id);
    }
}
