package com.newuniverse.nusmp.spiritdark;

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
class SpiritDarkLodTest {
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
            assertEquals(SpiritDarkLod.COMPANION_QUADS[lod], quads("spirit_dark_companion" + suffix), "companion level " + lod);
            assertEquals(SpiritDarkLod.POWERED_QUADS[lod], quads("spirit_dark_powered" + suffix), "powered level " + lod);
        }
        assertEquals(SpiritDarkLod.COMPANION_FX_QUADS, quads("spirit_dark_companion_fx"));
        assertEquals(SpiritDarkLod.POWERED_FX_QUADS, quads("spirit_dark_powered_fx"));
        assertEquals(SpiritDarkLod.COMPANION_FACE_QUADS, quads("spirit_dark_companion_face"));
        assertEquals(SpiritDarkLod.COMPANION_FACE_QUADS, quads("spirit_dark_companion_faceglow"));
        assertEquals(SpiritDarkLod.POWERED_FACE_QUADS, quads("spirit_dark_powered_face"));
        assertEquals(SpiritDarkLod.POWERED_FACE_QUADS, quads("spirit_dark_powered_faceglow"));
    }

    @Test
    void theBudgetsOfTheOwnerAreMet() {
        assertTrue(SpiritDarkLod.cost(false, SpiritDarkLod.FULL) <= 700, "companion at full detail: up to about 700 quads, is " + SpiritDarkLod.cost(false, SpiritDarkLod.FULL));
        assertTrue(SpiritDarkLod.cost(true, SpiritDarkLod.FULL) <= 900, "powered at full detail: up to about 900 quads, is " + SpiritDarkLod.cost(true, SpiritDarkLod.FULL));
        int c1 = SpiritDarkLod.cost(false, SpiritDarkLod.HALF), p1 = SpiritDarkLod.cost(true, SpiritDarkLod.HALF);
        assertTrue(c1 >= 200 && c1 <= 340, "lod 1 companion about 300: " + c1);
        assertTrue(p1 >= 330 && p1 <= 440, "lod 1 powered about 400: " + p1);
        assertTrue(SpiritDarkLod.cost(false, SpiritDarkLod.LOW) <= 120 && SpiritDarkLod.cost(true, SpiritDarkLod.LOW) <= 120, "lod 2 at most 120");
        for (boolean powered : new boolean[]{false, true}) {
            assertTrue(SpiritDarkLod.cost(powered, SpiritDarkLod.FULL) > SpiritDarkLod.cost(powered, SpiritDarkLod.HALF));
            assertTrue(SpiritDarkLod.cost(powered, SpiritDarkLod.HALF) > SpiritDarkLod.cost(powered, SpiritDarkLod.LOW));
            assertEquals(0, SpiritDarkLod.cost(powered, SpiritDarkLod.CULL));
        }
        assertEquals(3200, SpiritDarkLod.FRAME_QUADS);
        assertEquals(64.0, SpiritDarkLod.COMPANION_RANGE[2]);
        assertEquals(112.0, SpiritDarkLod.POWERED_RANGE[2]);
    }

    @Test
    void theLevelFollowsTheDistanceAndTheEffectQuality() {
        assertEquals(SpiritDarkLod.FULL, SpiritDarkLod.level(false, 10 * 10, 1f));
        assertEquals(SpiritDarkLod.HALF, SpiritDarkLod.level(false, 30 * 30, 1f));
        assertEquals(SpiritDarkLod.LOW, SpiritDarkLod.level(false, 60 * 60, 1f));
        assertEquals(SpiritDarkLod.CULL, SpiritDarkLod.level(false, 65 * 65, 1f), "the companion is culled beyond 64 blocks");
        assertEquals(SpiritDarkLod.LOW, SpiritDarkLod.level(true, 110 * 110, 1f));
        assertEquals(SpiritDarkLod.CULL, SpiritDarkLod.level(true, 113 * 113, 1f), "the powered form is culled beyond 112 blocks");
        assertEquals(SpiritDarkLod.HALF, SpiritDarkLod.level(false, 10 * 10, 0.5f), "a weaker machine drops a level sooner");
        assertEquals(SpiritDarkLod.level(false, 20 * 20, 0.5f), SpiritDarkLod.level(false, 20 * 20, 0.1f), "the quality factor is clamped to 0.5");
        assertEquals(SpiritDarkLod.FULL, SpiritDarkLod.level(false, 0, 7f), "and to 1");
    }

    @Test
    void theFrameBudgetDowngradesThenCullsAndNeverTouchesTheOwnersCloseSpirit() {
        SpiritDarkLod.QuadBudget b = new SpiritDarkLod.QuadBudget();
        long frame = SpiritDarkLod.frameKey(100, 0.25f);
        int full = SpiritDarkLod.cost(true, SpiritDarkLod.FULL);
        int drawnFull = 0, drawnLower = 0, culled = 0;
        for (int i = 0; i < 60; i++) {
            int l = SpiritDarkLod.fit(true, SpiritDarkLod.FULL, false, b, frame);
            if (l == SpiritDarkLod.FULL) drawnFull++; else if (l == SpiritDarkLod.CULL) culled++; else drawnLower++;
        }
        assertTrue(b.used() <= SpiritDarkLod.FRAME_QUADS, "never above the frame budget (others): " + b.used());
        assertEquals(SpiritDarkLod.FRAME_QUADS / full, drawnFull);
        assertTrue(drawnLower > 0 && culled > 0, "the rest are drawn lower and then not at all");
        assertEquals(SpiritDarkLod.FULL, SpiritDarkLod.fit(true, SpiritDarkLod.FULL, true, b, frame), "the owner's own close spirit keeps full detail even when the budget is spent");
        assertEquals(SpiritDarkLod.CULL, SpiritDarkLod.fit(true, SpiritDarkLod.FULL, false, b, frame), "and the others then give way");
        assertEquals(SpiritDarkLod.FULL, SpiritDarkLod.fit(true, SpiritDarkLod.FULL, false, b, SpiritDarkLod.frameKey(101, 0.25f)), "a new frame starts with an empty budget");
        assertEquals(SpiritDarkLod.CULL, SpiritDarkLod.fit(true, SpiritDarkLod.CULL, true, b, frame), "far away is far away, even for the owner");
        assertTrue(SpiritDarkLod.ownerClose(true, 5 * 5) && !SpiritDarkLod.ownerClose(true, 20 * 20) && !SpiritDarkLod.ownerClose(false, 1));
        assertTrue(SpiritDarkLod.worstCaseFrameQuads() <= SpiritDarkLod.FRAME_QUADS + 1500, "the most one frame can spend: " + SpiritDarkLod.worstCaseFrameQuads());
    }

    @Test
    void theServerWideCapsAreTheOnesTheDocStates() {
        assertEquals(48, SpiritDarkRules.DEFAULT_MAX_SPIRITS, "48 spirits server wide");
        assertEquals(6, SpiritDarkRules.DEFAULT_MAX_POWERED, "6 powered forms server wide");
        assertEquals(1, SpiritDarkRules.POWERED_PER_PLAYER, "one powered form per player");
        assertEquals(1, SpiritDarkRules.SPIRITS_PER_PLAYER);
    }
}
