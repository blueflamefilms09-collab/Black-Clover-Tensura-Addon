package com.newuniverse.nusmp.sylph;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** The client cost rules of the Sylph and the quad counts of the generated models, which must equal the numbers the doc states and stay inside the owner's budget. */
class SylphLodTest {
    private static final Path GEO = Path.of("src/main/resources/assets/nusmp/geo/entity");

    private static int quads(String model) throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(GEO.resolve(model + ".geo.json"))).getAsJsonObject();
        JsonObject geo = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        int n = 0;
        for (JsonElement be : geo.getAsJsonArray("bones")) {
            JsonArray cubes = be.getAsJsonObject().getAsJsonArray("cubes");
            for (JsonElement ce : cubes) n += ce.getAsJsonObject().getAsJsonObject("uv").size();
        }
        return n;
    }

    @Test
    void theGeneratedModelsHaveExactlyTheQuadCountsOfTheTable() throws IOException {
        for (int lod = 0; lod < 3; lod++) {
            String suffix = lod == 0 ? "" : "_lod" + lod;
            assertEquals(SylphLod.SMALL_BODY[lod], quads("sylph_small" + suffix), "small level " + lod);
            assertEquals(SylphLod.POWERED_BODY[lod], quads("sylph_powered" + suffix), "powered level " + lod);
        }
        assertEquals(SylphLod.WINGS_QUADS[SylphLod.SMALL], quads("sylph_small_wings"));
        assertEquals(SylphLod.WINGS_QUADS[SylphLod.POWERED], quads("sylph_powered_wings"));
        assertEquals(SylphLod.FX_QUADS[SylphLod.SMALL], quads("sylph_small_fx"));
        assertEquals(SylphLod.FX_QUADS[SylphLod.POWERED], quads("sylph_powered_fx"));
    }

    @Test
    void everyLevelStaysInsideTheOwnersBudget() {
        for (int form : new int[]{SylphLod.SMALL, SylphLod.POWERED}) {
            boolean powered = form == SylphLod.POWERED;
            int full = SylphLod.cost(form, SylphLod.FULL), half = SylphLod.cost(form, SylphLod.HALF), low = SylphLod.cost(form, SylphLod.LOW);
            assertTrue(full <= (powered ? 1000 : 800), "full detail: " + full);
            assertTrue(half <= (powered ? 450 : 350), "level 1: " + half);
            assertTrue(low <= 140, "level 2: " + low);
            assertTrue(full > half && half > low, "every level is cheaper than the one above");
            assertEquals(0, SylphLod.cost(form, SylphLod.CULL));
        }
    }

    @Test
    void theLevelFollowsTheDistanceAndTheEffectQuality() {
        // the small fairy covers few pixels: her bands are shorter than the powered form's
        assertEquals(SylphLod.FULL, SylphLod.level(SylphLod.SMALL, 8 * 8, 1f));
        assertEquals(SylphLod.HALF, SylphLod.level(SylphLod.SMALL, 20 * 20, 1f));
        assertEquals(SylphLod.LOW, SylphLod.level(SylphLod.SMALL, 50 * 50, 1f));
        assertEquals(SylphLod.CULL, SylphLod.level(SylphLod.SMALL, 70 * 70, 1f), "the small form is culled past 64 blocks");
        assertEquals(SylphLod.FULL, SylphLod.level(SylphLod.POWERED, 25 * 25, 1f));
        assertEquals(SylphLod.HALF, SylphLod.level(SylphLod.POWERED, 50 * 50, 1f));
        assertEquals(SylphLod.LOW, SylphLod.level(SylphLod.POWERED, 100 * 100, 1f));
        assertEquals(SylphLod.CULL, SylphLod.level(SylphLod.POWERED, 120 * 120, 1f), "the powered form is culled past 112 blocks");
        assertTrue(SylphLod.SMALL_RANGE[0] < SylphLod.POWERED_RANGE[0] && SylphLod.SMALL_RANGE[2] < SylphLod.POWERED_RANGE[2]);
        assertEquals(SylphLod.HALF, SylphLod.level(SylphLod.SMALL, 8 * 8, 0.5f), "a weaker machine drops a level sooner");
        assertEquals(SylphLod.level(SylphLod.SMALL, 20 * 20, 0.5f), SylphLod.level(SylphLod.SMALL, 20 * 20, 0.1f), "the quality factor is clamped to 0.5");
        assertEquals(SylphLod.FULL, SylphLod.level(SylphLod.SMALL, 0, 7f), "and to 1");
    }

    @Test
    void theFrameBudgetDowngradesAndThenCulls() {
        SylphLod.QuadBudget b = new SylphLod.QuadBudget();
        long frame = SylphLod.frameKey(100, 0.25f);
        int full = SylphLod.cost(SylphLod.SMALL, SylphLod.FULL);
        int drawnFull = 0, drawnLower = 0, culled = 0;
        for (int i = 0; i < 60; i++) {
            int l = SylphLod.fit(SylphLod.SMALL, SylphLod.FULL, b, frame, false);
            if (l == SylphLod.FULL) drawnFull++; else if (l == SylphLod.CULL) culled++; else drawnLower++;
        }
        assertTrue(b.used() <= SylphLod.FRAME_QUADS, "never above the frame budget");
        assertEquals(SylphLod.FRAME_QUADS / full, drawnFull);
        assertTrue(drawnLower > 0 && culled > 0, "the rest are drawn lower and then not at all");
        int l = SylphLod.fit(SylphLod.SMALL, SylphLod.FULL, b, SylphLod.frameKey(101, 0.25f), false);
        assertEquals(SylphLod.FULL, l, "a new frame starts with an empty budget");
    }

    @Test
    void theViewersOwnSylphIsNeverDroppedToNothing() {
        SylphLod.QuadBudget b = new SylphLod.QuadBudget();
        long frame = SylphLod.frameKey(5, 0f);
        for (int i = 0; i < SylphLod.FRAME_QUADS; i++) assertTrue(b.take(frame, 1, SylphLod.FRAME_QUADS));     // a crowd uses the whole budget
        assertFalse(b.take(frame, 1, SylphLod.FRAME_QUADS));
        assertEquals(SylphLod.CULL, SylphLod.fit(SylphLod.SMALL, SylphLod.FULL, b, frame, false), "a stranger is culled now");
        int own = SylphLod.fit(SylphLod.SMALL, SylphLod.FULL, b, frame, true);
        assertEquals(SylphLod.LOW, own, "the owner's own is drawn at the lowest level");
        assertEquals(SylphLod.CULL, SylphLod.fit(SylphLod.SMALL, SylphLod.CULL, b, frame, true), "but one that is out of range stays out of range");
    }

    @Test
    void theWholeCrowdOfThePolicyFitsTheFrameBudget() {
        // the worst case the doc states: the viewer's own two (small + powered) at full detail plus strangers at the lowest level
        int own = SylphLod.cost(SylphLod.SMALL, SylphLod.FULL) + SylphLod.cost(SylphLod.POWERED, SylphLod.FULL);
        assertTrue(own < SylphLod.FRAME_QUADS / 2);
        assertEquals(3200, SylphLod.FRAME_QUADS);
    }
}
