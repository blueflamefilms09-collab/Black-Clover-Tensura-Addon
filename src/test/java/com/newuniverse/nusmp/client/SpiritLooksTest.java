package com.newuniverse.nusmp.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the canon sizes of the Great Spirits are numbers in two places that must agree: how big the renderer draws each model (client.SpiritLooks) and how tall the model is as authored
 * (tools/gen_sylph_fairy.py, tools/gen_undine_mermaid.py). Sylph is a TINY fairy (about 0.4 of a block), Undine a GIANT mermaid (3 to 4 blocks from the crown of her hair down), Salamander a
 * MASSIVE dragon. The test reads the real geo files and checks that size times scale lands in the canon range, and checks the pure clip and colour numbers (the annoyed hop, the thin-film hue).
 * Pure (Gson, java.nio); the working directory is the project root.
 */
class SpiritLooksTest {
    private static final Path ASSETS = Path.of("").toAbsolutePath().resolve("src/main/resources/assets/nusmp");

    /** The highest and lowest y (model pixels) of any cube of a geo file, bone rotations ignored. */
    private static float[] extent(String model) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(ASSETS.resolve("geo/entity/" + model + ".geo.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray bones = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (JsonElement b : bones) {
            JsonElement cubes = b.getAsJsonObject().get("cubes");
            if (cubes == null) continue;
            for (JsonElement c : cubes.getAsJsonArray()) {
                JsonObject o = c.getAsJsonObject();
                float y = o.getAsJsonArray("origin").get(1).getAsFloat(), h = o.getAsJsonArray("size").get(1).getAsFloat();
                lo = Math.min(lo, y);
                hi = Math.max(hi, y + h);
            }
        }
        return new float[]{lo, hi};
    }

    @Test
    void sylphIsATinyFairy() throws IOException {
        float top = extent("spirit_sylph_fairy")[1];
        float blocks = top * SpiritLooks.scale("SYLPH") / 16f;
        assertTrue(blocks > 0.30f && blocks < 0.50f, "Sylph is about 0.4 of a block tall, was " + blocks);
    }

    @Test
    void undineIsAGiantMermaid() throws IOException {
        float top = extent("spirit_undine_mermaid")[1];
        float blocks = top * SpiritLooks.scale("UNDINE") / 16f;
        assertTrue(blocks >= 3.0f && blocks <= 4.0f, "Undine stands 3 to 4 blocks from the crown of her hair, was " + blocks);
    }

    @Test
    void salamanderGrowsAndTheEarthGiantStays() {
        assertEquals(1.3f, SpiritLooks.scale("SALAMANDER"), 1e-6f);
        assertEquals(1f, SpiritLooks.scale("GNOME"), 1e-6f);
        assertTrue(SpiritLooks.scale("SYLPH") < SpiritLooks.scale("GNOME"));
        assertTrue(SpiritLooks.CANON);
    }

    @Test
    void theAnnoyedHopRepeatsWhileSheWaits() {
        assertEquals(0f, SpiritLooks.annoyedAt(0f, 0f), 1e-6f);
        assertEquals(0.5f, SpiritLooks.annoyedAt(0.5f, 0f), 1e-6f);
        assertEquals(-1f, SpiritLooks.annoyedAt(SpiritLooks.ANNOYED_LENGTH + 0.01f, 0f), 1e-6f);
        assertEquals(-1f, SpiritLooks.annoyedAt(4f, 0f), 1e-6f);
        assertEquals(0.25f, SpiritLooks.annoyedAt(SpiritLooks.ANNOYED_EVERY + 0.25f, 0f), 1e-4f);       // it repeats
        assertEquals(0.25f, SpiritLooks.annoyedAt(-SpiritLooks.ANNOYED_EVERY + 0.25f, 0f), 1e-4f);       // and never throws on a negative clock
        assertTrue(SpiritLooks.annoyedAt(1f, 0.5f) < 0f, "a phase shifts the cycle");
        int hopping = 0;
        for (int tick = 0; tick < 20 * 90; tick++) if (SpiritLooks.annoyedAt(tick / 20f, 0f) >= 0f) hopping++;
        assertTrue(hopping > 0 && hopping < 20 * 90 / 4, "she hops now and then, not all the time");
    }

    @Test
    void theIridescenceStaysGreenToPink() {
        for (float age = 0f; age < 400f; age += 3.7f) {
            float h = SpiritLooks.filmHue(age);
            assertTrue(h >= 0.30f - 1e-4f && h <= 0.92f + 1e-4f, "hue " + h);
            int argb = SpiritLooks.filmArgb(age, 255);
            assertEquals(255, argb >>> 24);
        }
        assertEquals(0x80, SpiritLooks.filmArgb(10f, 0x80) >>> 24);
        // the ends of the run: green and pink
        float lo = 1f, hi = 0f;
        for (float age = 0f; age < 400f; age += 0.5f) { lo = Math.min(lo, SpiritLooks.filmHue(age)); hi = Math.max(hi, SpiritLooks.filmHue(age)); }
        assertTrue(lo < 0.32f && hi > 0.90f, "the hue sweeps the whole range, " + lo + " .. " + hi);
    }

    @Test
    void hsvConvertsTheCorners() {
        assertEquals(0xFF0000, SpiritLooks.hsv(0f, 1f, 1f));
        assertEquals(0x00FF00, SpiritLooks.hsv(1f / 3f, 1f, 1f));
        assertEquals(0x0000FF, SpiritLooks.hsv(2f / 3f, 1f, 1f));
        assertEquals(0xFFFFFF, SpiritLooks.hsv(0.4f, 0f, 1f));
        assertEquals(0x000000, SpiritLooks.hsv(0.4f, 1f, 0f));
        assertEquals(0x808080, SpiritLooks.greyArgb(0.5f) & 0xFFFFFF);
        assertEquals(0xFF000000, SpiritLooks.greyArgb(-3f));
        assertEquals(0xFFFFFFFF, SpiritLooks.greyArgb(9f));
    }
}
