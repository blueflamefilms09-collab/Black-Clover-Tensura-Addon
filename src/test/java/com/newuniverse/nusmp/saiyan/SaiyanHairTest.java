package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.book.NoelleFormRules;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the hair table and its growth / sway maths (docs/saiyan_hair.md) are plain arithmetic, so every form's hair, the bounds and the Blender builder's colour table are pinned here with no game. */
class SaiyanHairTest {
    @Test
    void everyFormHasAHairOfAKnownShapeAndSaneColours() {
        Set<String> ids = new HashSet<>();
        for (SaiyanForms.Form f : SaiyanForms.ALL) {
            SaiyanHair.Hair h = SaiyanHair.of(f.id());
            assertNotNull(h, f.id() + " has no hair");
            assertTrue(SaiyanHair.shapeIndex(h.shape()) >= 0, f.id() + ": shape " + h.shape());
            assertTrue((h.tint() & 0xFFFFFF) != 0 && (h.glow() & 0xFFFFFF) != 0, f.id());
            assertTrue(h.tint() >>> 24 == 0 && h.glow() >>> 24 == 0, f.id() + ": plain RGB, no alpha");
            assertTrue(h.glowK() >= 0f && h.glowK() <= 1.5f, f.id());
            assertEquals(SaiyanHair.meshId(h.shape()), h.meshId());
            assertTrue(ids.add(f.id()));
        }
        assertEquals(SaiyanForms.ALL.size(), SaiyanHair.all().size(), "no hair without a form");
        assertNull(SaiyanHair.of("base"));
        assertNull(SaiyanHair.of(null));
        Set<String> used = new HashSet<>();
        for (SaiyanHair.Hair h : SaiyanHair.all()) used.add(h.shape());
        assertEquals(new HashSet<>(java.util.List.of(SaiyanHair.SHAPES)), used, "every shape is worn by a form, every form wears a listed shape");
    }

    @Test
    void theAnimeOrderOfTheGoldIsBrighterThenDeeper() {
        // Super Saiyan gold, Full Power and 2 brighter, 3 deeper (more orange): the owner's "a little different for each form"
        float[] ss = lum(SaiyanHair.of("super_saiyan")), fp = lum(SaiyanHair.of("super_saiyan_full_power")), s2 = lum(SaiyanHair.of("super_saiyan_2")), s3 = lum(SaiyanHair.of("super_saiyan_3"));
        assertTrue(fp[0] > ss[0] && s2[0] > fp[0], "brighter with each step");
        assertTrue(s3[1] < ss[1], "Super Saiyan 3 is deeper (less green: more orange)");
        assertTrue(!SaiyanHair.of("super_saiyan").shape().equals(SaiyanHair.of("super_saiyan_2").shape()), "Super Saiyan 2 has its own, more jagged shape");
        assertEquals("ss3", SaiyanHair.of("super_saiyan_3").shape(), "Super Saiyan 3 has the long mane");
        assertEquals("ss4", SaiyanHair.of("super_saiyan_4_limit_breaker").shape());
        assertTrue(!SaiyanHair.of("kaioken").lit() && !SaiyanHair.of("super_saiyan_4").lit() && SaiyanHair.of("super_saiyan").lit(), "black hair is not self-lit, gold is");
    }

    private static float[] lum(SaiyanHair.Hair h) { return new float[]{(h.tint() >> 16 & 255) + (h.tint() >> 8 & 255) + (h.tint() & 255), h.tint() >> 8 & 255}; }

    @Test
    void theBlenderBuildersColourTableIsTheJavaTable() throws IOException {
        String py = Files.readString(Path.of("tools/blender/saiyan_hair.py"));
        Matcher m = Pattern.compile("\"([a-z0-9_]+)\": \\(\"([a-z0-9]+)\", \"([0-9A-F]{6})\", \"([0-9A-F]{6})\", ([0-9.]+)\\),").matcher(py);
        int n = 0;
        while (m.find()) {
            SaiyanHair.Hair h = SaiyanHair.of(m.group(1));
            assertNotNull(h, m.group(1));
            assertEquals(h.shape(), m.group(2), m.group(1));
            assertEquals(h.tint(), Integer.parseInt(m.group(3), 16), m.group(1));
            assertEquals(h.glow(), Integer.parseInt(m.group(4), 16), m.group(1));
            assertEquals(h.glowK(), Float.parseFloat(m.group(5)), 1e-6, m.group(1));
            n++;
        }
        assertEquals(SaiyanHair.all().size(), n, "the preview table of the builder lists every form");
    }

    @Test
    void growthSwapsAtTheFlashAndEndsSmoothly() {
        assertEquals(0f, SaiyanHair.grow(0f, 1000f, true), 1e-6, "the player's own hair while the energy gathers");
        assertEquals(0f, SaiyanHair.grow(SaiyanLook.GATHER_TICKS - 2f, 1000f, true), 1e-6);
        assertEquals(1f, SaiyanHair.grow(SaiyanLook.GATHER_TICKS + 8f, 1000f, true), 1e-6, "the form's hair at the burst");
        assertEquals(1f, SaiyanHair.grow(0f, 1000f, false), 1e-6, "no charge-up: the hair is there at once");
        assertEquals(0f, SaiyanHair.grow(500f, 0f, true), 1e-6, "gone when the form ends");
        float prev = -1f;
        for (float t = 0f; t < 40f; t += 0.25f) {
            float g = SaiyanHair.grow(t, 1000f, true);
            assertTrue(g >= prev - 1e-6f, "growth never goes back while the form is on");
            prev = g;
        }
        assertEquals(0.4f, SaiyanHair.scale(0f), 1e-5);
        assertEquals(1f, SaiyanHair.scale(1f), 1e-5);
        for (float g = 0f; g <= 1f; g += 0.01f) assertTrue(SaiyanHair.scale(g) >= 0.39f && SaiyanHair.scale(g) <= 1.0001f, "scale " + g);
    }

    @Test
    void swayStaysSmallAndTheTipSwingsMost() {
        for (float age = 0f; age < 600f; age += 0.5f) {
            for (int s = 0; s <= SaiyanHair.TAIL_TIP; s++) {
                assertTrue(Math.abs(SaiyanHair.swayX(s, age, 1f)) <= 0.6f && Math.abs(SaiyanHair.swayZ(s, age)) <= 0.3f, "slot " + s);
            }
        }
        float run = SaiyanHair.swayX(SaiyanHair.TAIL_TIP, 0f, 1f) - SaiyanHair.swayX(SaiyanHair.TAIL_TIP, 0f, 0f);
        assertTrue(run > 0.1f, "running streams the hanging parts back");
        assertEquals(3f, SaiyanHair.loose("ui"), 1e-6);
        assertEquals(1f, SaiyanHair.loose("ss"), 1e-6);
        assertTrue(0.03f * SaiyanHair.loose("ui") < 0.1f, "even the floating hair only moves a little");
        assertEquals(SaiyanHair.swayX(SaiyanHair.CROWN, 3f, 0f), SaiyanHair.swayX(SaiyanHair.CROWN, 3f, 1f), 1e-6, "the crown stays put when running");
    }

    @Test
    void distanceLevelsAndTheCrowdBudgetKeepTheCostBounded() {
        assertEquals(NoelleFormRules.Lod.STANDARD, SaiyanHair.wanted(23 * 23));
        assertEquals(NoelleFormRules.Lod.FAR, SaiyanHair.wanted(25 * 25));
        assertEquals(NoelleFormRules.Lod.FAR, SaiyanHair.wanted(47 * 47));
        assertEquals(NoelleFormRules.Lod.NONE, SaiyanHair.wanted(49 * 49));
        // worst case per frame: four players at full detail, six far, nobody else
        assertEquals(NoelleFormRules.Lod.FAR, NoelleFormRules.budgeted(NoelleFormRules.Lod.STANDARD, 0, NoelleFormRules.MAX_FULL_PER_FRAME, 0));
        assertEquals(NoelleFormRules.Lod.NONE, NoelleFormRules.budgeted(NoelleFormRules.Lod.FAR, 0, 0, NoelleFormRules.MAX_FAR_PER_FRAME));
        assertTrue(NoelleFormRules.MAX_FULL_PER_FRAME == 4 && NoelleFormRules.MAX_FAR_PER_FRAME == 6);
    }
}
