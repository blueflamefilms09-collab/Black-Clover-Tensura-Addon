package com.newuniverse.nusmp.firecast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** The client cost rules of the Salamander forms and the quad counts of the generated models, which must equal the numbers the doc states. */
class FireCastLodTest {
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
            assertEquals(FireCastLod.SALAMANDER_QUADS[lod], quads("firecast_salamander" + suffix), "salamander level " + lod);
            assertEquals(FireCastLod.DRAGON_QUADS[lod], quads("firecast_dragon" + suffix), "dragon level " + lod);
        }
        assertEquals(FireCastLod.SALAMANDER_FX_QUADS, quads("firecast_salamander_fx"));
        assertEquals(FireCastLod.DRAGON_FX_QUADS, quads("firecast_dragon_fx"));
    }

    /** The owner-approved raised limits: the full / half / low models stay under the ceilings of the doc (small 700 / 300 / 120, dragon 900 / 380 / 120). */
    @Test
    void theModelsStayUnderTheOwnerApprovedCeilings() {
        assertTrue(FireCastLod.SALAMANDER_QUADS[0] <= FireCastLod.MAX_FULL_SALAMANDER, "small full " + FireCastLod.SALAMANDER_QUADS[0]);
        assertTrue(FireCastLod.DRAGON_QUADS[0] <= FireCastLod.MAX_FULL_DRAGON, "dragon full " + FireCastLod.DRAGON_QUADS[0]);
        assertTrue(FireCastLod.SALAMANDER_QUADS[1] <= FireCastLod.MAX_HALF_SALAMANDER, "small half " + FireCastLod.SALAMANDER_QUADS[1]);
        assertTrue(FireCastLod.DRAGON_QUADS[1] <= FireCastLod.MAX_HALF_DRAGON, "dragon half " + FireCastLod.DRAGON_QUADS[1]);
        assertTrue(FireCastLod.SALAMANDER_QUADS[2] <= FireCastLod.MAX_LOW && FireCastLod.DRAGON_QUADS[2] <= FireCastLod.MAX_LOW, "low levels");
        assertEquals(700, FireCastLod.MAX_FULL_SALAMANDER);
        assertEquals(900, FireCastLod.MAX_FULL_DRAGON);
        assertEquals(3200, FireCastLod.FRAME_QUADS, "the shared frame cap of the raised limits");
        assertEquals(64.0, FireCastLod.SALAMANDER_RANGE[2]);
        assertEquals(112.0, FireCastLod.DRAGON_RANGE[2]);
    }

    @Test
    void theCastersOwnFormIsNeverDroppedByTheFrameBudget() {
        FireCastLod.QuadBudget b = new FireCastLod.QuadBudget();
        long frame = FireCastLod.frameKey(500, 0.5f);
        b.force(frame, FireCastLod.FRAME_QUADS);                                           // a frame already full of other forms
        assertEquals(FireCastLod.CULL, FireCastLod.fit(true, FireCastLod.FULL, b, frame, false), "someone else's form is culled");
        assertEquals(FireCastLod.CULL, FireCastLod.fit(false, FireCastLod.FULL, b, frame, false));
        assertEquals(FireCastLod.LOW, FireCastLod.fit(true, FireCastLod.FULL, b, frame, true), "the caster's own dragon is drawn at the low level anyway");
        assertEquals(FireCastLod.LOW, FireCastLod.fit(false, FireCastLod.HALF, b, frame, true), "and the own small form");
        assertTrue(b.used() <= FireCastLod.FRAME_QUADS + 2 * FireCastLod.MAX_LOW, "a bounded overshoot (two low forms at most)");
        assertEquals(FireCastLod.CULL, FireCastLod.fit(false, FireCastLod.CULL, new FireCastLod.QuadBudget(), frame, true), "beyond the cull distance even the own form is not drawn");
    }

    @Test
    void everyLevelIsCheaperThanTheOneAboveAndTheFullOneStaysSmall() {
        for (boolean dragon : new boolean[]{false, true}) {
            assertTrue(FireCastLod.cost(dragon, FireCastLod.FULL) > FireCastLod.cost(dragon, FireCastLod.HALF));
            assertTrue(FireCastLod.cost(dragon, FireCastLod.HALF) > FireCastLod.cost(dragon, FireCastLod.LOW));
            assertEquals(0, FireCastLod.cost(dragon, FireCastLod.CULL));
            assertTrue(FireCastLod.cost(dragon, FireCastLod.FULL) <= (dragon ? FireCastLod.MAX_FULL_DRAGON : FireCastLod.MAX_FULL_SALAMANDER), "a full form is under the approved ceiling");
        }
    }

    @Test
    void theLevelFollowsTheDistanceAndTheEffectQuality() {
        assertEquals(FireCastLod.FULL, FireCastLod.level(false, 10 * 10, 1f));
        assertEquals(FireCastLod.HALF, FireCastLod.level(false, 20 * 20, 1f));
        assertEquals(FireCastLod.LOW, FireCastLod.level(false, 40 * 40, 1f));
        assertEquals(FireCastLod.LOW, FireCastLod.level(false, 60 * 60, 1f));
        assertEquals(FireCastLod.CULL, FireCastLod.level(false, 70 * 70, 1f));
        assertEquals(FireCastLod.HALF, FireCastLod.level(false, 10 * 10, 0.5f), "a weaker machine drops a level sooner");
        assertEquals(FireCastLod.LOW, FireCastLod.level(true, 100 * 100, 1f));
        assertEquals(FireCastLod.CULL, FireCastLod.level(true, 120 * 120, 1f));
        assertEquals(FireCastLod.FULL, FireCastLod.level(true, 25 * 25, 1f));
        assertEquals(FireCastLod.level(false, 20 * 20, 0.5f), FireCastLod.level(false, 20 * 20, 0.1f), "the quality factor is clamped to 0.5");
        assertEquals(FireCastLod.FULL, FireCastLod.level(false, 0, 7f), "and to 1");
    }

    @Test
    void theFrameBudgetDowngradesAndThenCulls() {
        FireCastLod.QuadBudget b = new FireCastLod.QuadBudget();
        long frame = FireCastLod.frameKey(100, 0.25f);
        int full = FireCastLod.cost(true, FireCastLod.FULL);
        int drawnFull = 0, drawnLower = 0, culled = 0;
        for (int i = 0; i < 40; i++) {
            int l = FireCastLod.fit(true, FireCastLod.FULL, b, frame);
            if (l == FireCastLod.FULL) drawnFull++; else if (l == FireCastLod.CULL) culled++; else drawnLower++;
        }
        assertTrue(b.used() <= FireCastLod.FRAME_QUADS, "never above the frame budget");
        assertEquals(FireCastLod.FRAME_QUADS / full, drawnFull);
        assertTrue(culled > 0, "the rest are not drawn at all once not even the low level fits");
        FireCastLod.QuadBudget room = new FireCastLod.QuadBudget();
        long f2 = FireCastLod.frameKey(102, 0.5f);
        room.force(f2, FireCastLod.FRAME_QUADS - FireCastLod.cost(true, FireCastLod.HALF));                  // room for exactly one half form
        assertEquals(FireCastLod.HALF, FireCastLod.fit(true, FireCastLod.FULL, room, f2), "a form that does not fit at its level is drawn at the next lower one that does");
        assertEquals(FireCastLod.CULL, FireCastLod.fit(false, FireCastLod.FULL, room, f2), "then the budget is spent");
        int l = FireCastLod.fit(true, FireCastLod.FULL, b, FireCastLod.frameKey(101, 0.25f));
        assertEquals(FireCastLod.FULL, l, "a new frame starts with an empty budget");
    }

    @Test
    void theSpiralNeverExceedsItsPointCap() {
        for (int level = -2; level < 9; level++) for (double d : new double[]{0, 100, 30 * 30, 60 * 60}) assertTrue(FireCastLod.spiralPoints(level, d) <= FireCastLod.SPIRAL_MAX_POINTS);
        assertTrue(FireCastLod.spiralPoints(4, 0) > FireCastLod.spiralPoints(1, 0), "it grows with the level");
        assertTrue(FireCastLod.spiralPoints(4, 30 * 30) < FireCastLod.spiralPoints(4, 0), "and thins at range");
        assertTrue(3 * FireCastLod.SPIRAL_MAX_POINTS + 2 <= 80, "sprites, ribbons and two halos: the quad count the doc states");
    }
}
