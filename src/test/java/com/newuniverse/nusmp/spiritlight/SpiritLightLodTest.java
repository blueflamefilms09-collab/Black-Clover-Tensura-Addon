package com.newuniverse.nusmp.spiritlight;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** The client cost rules of the two forms and the quad counts of the generated models, which must equal the numbers the doc states. */
class SpiritLightLodTest {
    private static final Path GEO = Path.of("src/main/resources/assets/nusmp/geo/entity");

    static int quads(String model) throws IOException {
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
            assertEquals(SpiritLightLod.CUB_QUADS[lod], quads("spiritlight_cub" + suffix), "cub level " + lod);
            assertEquals(SpiritLightLod.SERAPH_QUADS[lod], quads("spiritlight_seraph" + suffix), "seraph level " + lod);
        }
        assertEquals(SpiritLightLod.CUB_FX_QUADS, quads("spiritlight_cub_fx"));
        assertEquals(SpiritLightLod.SERAPH_FX_QUADS, quads("spiritlight_seraph_fx"));
    }

    @Test
    void theModelsStayInsideTheOwnersBudgets() {
        assertTrue(SpiritLightLod.fullCost(false) <= SpiritLightLod.CUB_MAX_QUADS, "the cub at full detail up to about 600 quads");
        assertTrue(SpiritLightLod.fullCost(true) <= SpiritLightLod.SERAPH_MAX_QUADS, "the seraph at full detail up to about 1000 quads");
        assertEquals(600, SpiritLightLod.CUB_MAX_QUADS);
        assertEquals(1000, SpiritLightLod.SERAPH_MAX_QUADS);
        assertTrue(SpiritLightLod.CUB_QUADS[1] <= 280 && SpiritLightLod.SERAPH_QUADS[1] <= 450, "the half level: about 280 and 450");
        assertTrue(SpiritLightLod.CUB_QUADS[2] <= 120 && SpiritLightLod.SERAPH_QUADS[2] <= 120, "the low level: 120 at most");
        assertEquals(3200, SpiritLightLod.FRAME_QUADS);
        assertEquals(64.0, SpiritLightLod.CUB_RANGE[2], "the cub is culled past 64 blocks");
        assertEquals(112.0, SpiritLightLod.SERAPH_RANGE[2], "the seraph is culled past 112 blocks");
    }

    @Test
    void everyLevelIsCheaperThanTheOneAboveAndTheEffectsStayFewQuads() {
        for (boolean seraph : new boolean[]{false, true}) {
            assertTrue(SpiritLightLod.cost(seraph, SpiritLightLod.FULL) > SpiritLightLod.cost(seraph, SpiritLightLod.HALF));
            assertTrue(SpiritLightLod.cost(seraph, SpiritLightLod.HALF) > SpiritLightLod.cost(seraph, SpiritLightLod.LOW));
            assertEquals(0, SpiritLightLod.cost(seraph, SpiritLightLod.CULL));
        }
        assertTrue(SpiritLightLod.BEAM_QUADS <= 6 && SpiritLightLod.PULSE_QUADS <= 8, "a beam is a few quads");
        assertTrue(SpiritLightLod.CUB_RANGE[0] < SpiritLightLod.CUB_RANGE[1] && SpiritLightLod.CUB_RANGE[1] < SpiritLightLod.CUB_RANGE[2]);
        assertTrue(SpiritLightLod.SERAPH_RANGE[0] < SpiritLightLod.SERAPH_RANGE[1] && SpiritLightLod.SERAPH_RANGE[1] < SpiritLightLod.SERAPH_RANGE[2]);
    }

    @Test
    void theLevelFollowsTheDistanceAndTheEffectQuality() {
        assertEquals(SpiritLightLod.FULL, SpiritLightLod.level(false, 10 * 10, 1f));
        assertEquals(SpiritLightLod.HALF, SpiritLightLod.level(false, 30 * 30, 1f));
        assertEquals(SpiritLightLod.LOW, SpiritLightLod.level(false, 50 * 50, 1f));
        assertEquals(SpiritLightLod.CULL, SpiritLightLod.level(false, 70 * 70, 1f));
        assertEquals(SpiritLightLod.FULL, SpiritLightLod.level(true, 30 * 30, 1f));
        assertEquals(SpiritLightLod.LOW, SpiritLightLod.level(true, 100 * 100, 1f));
        assertEquals(SpiritLightLod.CULL, SpiritLightLod.level(true, 113 * 113, 1f));
        assertEquals(SpiritLightLod.HALF, SpiritLightLod.level(false, 10 * 10, 0.5f), "a weaker machine drops a level sooner");
        assertEquals(SpiritLightLod.level(false, 30 * 30, 0.5f), SpiritLightLod.level(false, 30 * 30, 0.1f), "the quality factor is clamped to 0.5");
        assertEquals(SpiritLightLod.FULL, SpiritLightLod.level(false, 0, 7f), "and to 1");
    }

    @Test
    void theFrameBudgetDowngradesAndThenCullsButNeverTheOwnersOwnCloseSpirit() {
        SpiritLightLod.QuadBudget b = new SpiritLightLod.QuadBudget();
        long frame = SpiritLightLod.frameKey(100, 0.25f);
        int full = SpiritLightLod.cost(true, SpiritLightLod.FULL);
        int drawnFull = 0, drawnLower = 0, culled = 0;
        for (int i = 0; i < 60; i++) {
            int l = SpiritLightLod.fit(true, SpiritLightLod.FULL, b, frame, false);
            if (l == SpiritLightLod.FULL) drawnFull++; else if (l == SpiritLightLod.CULL) culled++; else drawnLower++;
        }
        assertTrue(b.used() <= SpiritLightLod.FRAME_QUADS, "the others never go above the frame budget");
        assertEquals(SpiritLightLod.FRAME_QUADS / full, drawnFull);
        assertTrue(drawnLower > 0 && culled > 0, "the rest are drawn lower and then not at all");
        // the owner's own close spirit: always at the level its distance asks for, counted so the others give way
        int own = SpiritLightLod.fit(true, SpiritLightLod.FULL, b, frame, true);
        assertEquals(SpiritLightLod.FULL, own, "never degraded");
        assertTrue(b.used() > SpiritLightLod.FRAME_QUADS - 1, "it is counted");
        assertEquals(SpiritLightLod.CULL, SpiritLightLod.fit(false, SpiritLightLod.FULL, b, frame, false), "and nothing else fits after it");
        assertEquals(SpiritLightLod.FULL, SpiritLightLod.fit(true, SpiritLightLod.FULL, b, SpiritLightLod.frameKey(101, 0.25f), false), "a new frame starts with an empty budget");
        assertEquals(SpiritLightLod.CULL, SpiritLightLod.fit(false, SpiritLightLod.CULL, b, frame, true), "a form past its range is not drawn, owner or not");
    }

    @Test
    void onlyTheLocalPlayersCloseSpiritIsExempt() {
        assertTrue(SpiritLightLod.ownClose(true, 5 * 5));
        assertFalse(SpiritLightLod.ownClose(true, 9 * 9), "the owner's own, but far");
        assertFalse(SpiritLightLod.ownClose(false, 1), "another player's, however close");
    }

    @Test
    void aBeamOrARingOnlyDrawsWhileTheBudgetHoldsIt() {
        SpiritLightLod.QuadBudget b = new SpiritLightLod.QuadBudget();
        long frame = SpiritLightLod.frameKey(5, 0f);
        int n = 0;
        while (SpiritLightLod.fitSmall(b, frame, SpiritLightLod.BEAM_QUADS)) n++;
        assertEquals(SpiritLightLod.FRAME_QUADS / SpiritLightLod.BEAM_QUADS, n);
    }
}
