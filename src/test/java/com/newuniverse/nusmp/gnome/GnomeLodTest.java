package com.newuniverse.nusmp.gnome;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** The client cost rules of the Gnome: the distance bands, the frame budget, the owner's own companion, and the quads counted from the generated model files (so the doc's numbers are the files'). */
class GnomeLodTest {
    private static final Path GEO = Path.of("src/main/resources/assets/nusmp/geo/entity");

    private static int quads(String id) throws IOException {
        JsonObject g = JsonParser.parseString(Files.readString(GEO.resolve(id + ".geo.json"))).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        int n = 0;
        for (JsonElement b : g.getAsJsonArray("bones"))
            for (JsonElement c : b.getAsJsonObject().getAsJsonArray("cubes")) n += c.getAsJsonObject().getAsJsonObject("uv").size();
        return n;
    }

    @Test
    void theQuadCountsInTheCodeAreTheQuadsOfTheFiles() throws IOException {
        String[] forms = {"gnome_companion", "gnome_full"};
        for (int f = 0; f < 2; f++) {
            assertEquals(GnomeLod.BODY[f][0], quads(forms[f]), forms[f] + " level 0");
            assertEquals(GnomeLod.BODY[f][1], quads(forms[f] + "_lod1"), forms[f] + " level 1");
            assertEquals(GnomeLod.BODY[f][2], quads(forms[f] + "_lod2"), forms[f] + " level 2");
            assertEquals(GnomeLod.FX[f][0], quads(forms[f] + "_fx"), forms[f] + " glow-only model");
            assertEquals(GnomeLod.FX[f][1], quads(forms[f] + "_fx_lod1"), forms[f] + " glow-only model, near level 1");
            assertEquals(0, GnomeLod.FX[f][2], "no glow pass on the low level");
        }
    }

    @Test
    void theBudgetsOfTheBriefHold() {
        assertTrue(GnomeLod.cost(GnomeLod.COMPANION, GnomeLod.LOD_FULL) <= GnomeLod.COMPANION_BUDGET, "companion at full detail: " + GnomeLod.cost(0, 0));
        assertTrue(GnomeLod.cost(GnomeLod.FULL, GnomeLod.LOD_FULL) <= GnomeLod.FULL_BUDGET, "giant at full detail: " + GnomeLod.cost(1, 0));
        assertTrue(GnomeLod.cost(GnomeLod.COMPANION, GnomeLod.LOD_HALF) <= 350, "companion level 1 about 350");
        assertTrue(GnomeLod.cost(GnomeLod.FULL, GnomeLod.LOD_HALF) <= 450, "giant level 1 about 450");
        for (int f = 0; f < 2; f++) assertTrue(GnomeLod.cost(f, GnomeLod.LOD_LOW) <= 140, "level 2 at most 140 quads");
        assertEquals(0, GnomeLod.cost(GnomeLod.FULL, GnomeLod.CULL));
        assertEquals(3200, GnomeLod.FRAME_QUADS);
    }

    @Test
    void theLevelFollowsTheDistanceAndTheCullDistancesAre64And112() {
        assertEquals(GnomeLod.LOD_FULL, GnomeLod.level(GnomeLod.COMPANION, 5 * 5, 1f));
        assertEquals(GnomeLod.LOD_HALF, GnomeLod.level(GnomeLod.COMPANION, 20 * 20, 1f));
        assertEquals(GnomeLod.LOD_LOW, GnomeLod.level(GnomeLod.COMPANION, 60 * 60, 1f));
        assertEquals(GnomeLod.CULL, GnomeLod.level(GnomeLod.COMPANION, 65 * 65, 1f), "culled beyond 64 blocks");
        assertEquals(GnomeLod.LOD_LOW, GnomeLod.level(GnomeLod.FULL, 110 * 110, 1f));
        assertEquals(GnomeLod.CULL, GnomeLod.level(GnomeLod.FULL, 113 * 113, 1f), "culled beyond 112 blocks");
        assertEquals(GnomeLod.LOD_HALF, GnomeLod.level(GnomeLod.FULL, 40 * 40, 1f));
        assertEquals(GnomeLod.CULL, GnomeLod.level(GnomeLod.FULL, 60 * 60, 0.5f), "a weaker effect quality culls sooner");
        assertEquals(GnomeLod.level(GnomeLod.FULL, 20 * 20, 1f), GnomeLod.level(GnomeLod.FULL, 20 * 20, 7f), "the quality factor is clamped");
    }

    @Test
    void theFrameBudgetDegradesTheCrowdAndNeverDropsTheOwnersCloseCompanion() {
        GnomeLod.QuadBudget b = new GnomeLod.QuadBudget();
        long key = GnomeLod.frameKey(100, 0.25f);
        int drawn = 0, degraded = 0, culled = 0;
        for (int i = 0; i < 40; i++) {                                                       // a crowd of giants all asking for the best level
            int l = GnomeLod.fit(GnomeLod.FULL, GnomeLod.LOD_FULL, b, key, false);
            if (l == GnomeLod.LOD_FULL) drawn++; else if (l == GnomeLod.CULL) culled++; else degraded++;
        }
        assertTrue(drawn > 0 && degraded > 0 && culled > 0, "full " + drawn + " degraded " + degraded + " culled " + culled);
        assertTrue(b.used() <= GnomeLod.FRAME_QUADS, "the shared budget holds: " + b.used());
        int owner = GnomeLod.fit(GnomeLod.COMPANION, GnomeLod.LOD_FULL, b, key, true);
        assertEquals(GnomeLod.LOD_FULL, owner, "the owner's own close companion is drawn at its level whatever the crowd took");
        assertEquals(GnomeLod.CULL, GnomeLod.fit(GnomeLod.COMPANION, GnomeLod.LOD_FULL, b, key, false), "a stranger's does not get past the full budget");
        long next = GnomeLod.frameKey(101, 0.25f);
        assertEquals(GnomeLod.LOD_FULL, GnomeLod.fit(GnomeLod.FULL, GnomeLod.LOD_FULL, b, next, false), "the budget starts again next frame");
        assertEquals(GnomeLod.CULL, GnomeLod.fit(GnomeLod.FULL, GnomeLod.CULL, b, next, true), "a culled one stays culled even for its owner");
    }

    @Test
    void theGiantIsDrawnAboutSixBlocksAndTheCompanionAboutOneAndAHalf() throws IOException {
        for (String id : new String[]{"gnome_full", "gnome_companion"}) {
            JsonObject g = JsonParser.parseString(Files.readString(GEO.resolve(id + ".geo.json"))).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            double top = 0;
            for (JsonElement b : g.getAsJsonArray("bones"))
                for (JsonElement c : b.getAsJsonObject().getAsJsonArray("cubes")) {
                    JsonObject cube = c.getAsJsonObject();
                    double y0 = cube.getAsJsonArray("origin").get(1).getAsDouble(), h = cube.getAsJsonArray("size").get(1).getAsDouble();
                    top = Math.max(top, y0 + h);
                }
            double blocks = top / 16.0 * (id.equals("gnome_full") ? GnomeLod.FULL_SCALE : GnomeLod.COMPANION_SCALE);
            if (id.equals("gnome_full")) assertTrue(blocks >= 5.0 && blocks <= 7.0, "the full form is 5 to 7 blocks: " + blocks);
            else assertTrue(blocks >= 1.2 && blocks <= 1.7, "the companion is about 1.2 to 1.6 blocks: " + blocks);
        }
    }
}
