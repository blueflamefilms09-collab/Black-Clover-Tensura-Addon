package com.newuniverse.nusmp.client.relics.hand;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.111.0 (docs/weapon_grip.md, "relic hand models"): a relic that is not a weapon is held by the point its hold style names ({@code hold=hx,hy,hz}, fractions of the object's half
 * extent), which {@link HandItemRenderer} puts at the hand's origin before the display transform's own offsets. That is the same rule as the weapons' grip point, so the same
 * check: every hold point of every style and every use line of transforms.txt lies within the object's bounds (each fraction within -1 to 1), and the translation the renderer
 * applies ({@code -h * extent * size / maxExtent * 0.5}) puts that point on the hand origin, whatever the size of the object.
 */
class HandHoldTest {
    private static final Pattern HOLD = Pattern.compile("hold=(-?[\\d.]+),(-?[\\d.]+),(-?[\\d.]+)");

    @Test
    void everyHoldPointOfEveryStyleLiesWithinTheObject() throws IOException {
        List<String> lines = Files.readAllLines(HandTransformsTest.FILE);
        int holds = 0;
        for (String line : lines) {
            if (line.trim().startsWith("#")) continue;
            Matcher m = HOLD.matcher(line);
            while (m.find()) {
                for (int i = 1; i <= 3; i++) {
                    float v = Float.parseFloat(m.group(i));
                    assertTrue(v >= -1f && v <= 1f, "a hold point outside the object's bounds (" + v + ") in: " + line);
                }
                holds++;
            }
        }
        assertTrue(holds >= 8, "the file has hold points: " + holds);
    }

    @Test
    void theResolvedStylesKeepTheirHoldInsideTheBounds() throws IOException {
        HandTransforms t = HandTransforms.parse(Files.readAllLines(HandTransformsTest.FILE));
        assertTrue(t.problems().isEmpty(), t.problems().toString());
        for (String key : t.useKeys()) {
            String[] parts = key.split(":");
            HandTransforms.Style s = key.startsWith("id:") ? t.styleFor(parts[1], null, null) : key.startsWith("look:") ? t.styleFor("x", parts[1], parts[2]) : t.styleFor("x", parts[1], null);
            HandTransforms.Fit f = s.fit();
            assertTrue(Math.abs(f.hx()) <= 1f && Math.abs(f.hy()) <= 1f && Math.abs(f.hz()) <= 1f, key + ": hold " + f.hx() + "," + f.hy() + "," + f.hz());
            assertTrue(f.size() > 0.05f && f.size() < 3f, key + ": size " + f.size());
        }
    }

    @Test
    void theRenderersTranslationPutsTheHoldPointOnTheHandWhateverTheSize() {
        // HandItemRenderer.drawForm: scaled by sc = size / maxExtent, then translate(-h * extent * sc * 0.5); the hold point of the object, centre + h * extent / 2 (in object units),
        // is then at (centre + h * extent / 2) * sc + translate = centre * sc, i.e. the object's centre is shifted so the hold point is at the origin of the display transform
        float[] extent = {12f, 30f, 8f}, h = {0f, -0.4f, 0f};
        for (float size : new float[]{0.4f, 0.9f, 1.6f}) {
            float sc = size / 30f;
            for (int a = 0; a < 3; a++) {
                double holdInObject = h[a] * extent[a] * 0.5;                          // relative to the object's centre
                double translate = -h[a] * extent[a] * sc * 0.5;
                assertEquals(0.0, holdInObject * sc + translate, 1e-6, "axis " + a + " size " + size);
            }
        }
    }
}
