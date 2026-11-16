package com.newuniverse.nusmp.beastwill;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (docs/body_parts_pass.md): "Fix the Fenrir wolf head to be smaller and not so giant" and "Rework the Hydra heads, because they're too close together". This reads the real generated geo files and pins
 * the head size against the player's 8 px head (the whole head piece, mane / ears / horns included, at most 2.05x wide and tall for every reworked will, Fenrir at most 1.75x), that the hydra, typhon and
 * yamata no orochi necks fan out (every pair of neck directions at least 35 degrees apart, the head count kept at 4 / 4 / 7), that each head has its own jaw bone, and that the old models are kept as
 * {@code _v2} for the client setting that restores them. Pure (file reads); the working directory is the project root.
 */
class BeastWillHeadFitTest {
    private static final Path GEO = Path.of("src/main/resources/assets/anime_multiverse/geo/entity");

    private static JsonObject geo(String name) throws Exception {
        return JsonParser.parseString(Files.readString(GEO.resolve(name + ".geo.json"))).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
    }

    /** width (x) and height (y) in px of every cube of a geo file */
    private static float[] extent(JsonObject g) {
        float[] lo = {1e9f, 1e9f}, hi = {-1e9f, -1e9f};
        for (JsonElement be : g.getAsJsonArray("bones")) {
            JsonObject b = be.getAsJsonObject();
            if (!b.has("cubes")) continue;
            for (JsonElement ce : b.getAsJsonArray("cubes")) {
                JsonObject c = ce.getAsJsonObject();
                for (int i = 0; i < 2; i++) {
                    float o = c.getAsJsonArray("origin").get(i).getAsFloat(), s = c.getAsJsonArray("size").get(i).getAsFloat();
                    lo[i] = Math.min(lo[i], o);
                    hi[i] = Math.max(hi[i], o + s);
                }
            }
        }
        return new float[]{hi[0] - lo[0], hi[1] - lo[1]};
    }

    private static float[] pivot(JsonObject g, String bone) {
        for (JsonElement be : g.getAsJsonArray("bones")) {
            JsonObject b = be.getAsJsonObject();
            if (b.get("name").getAsString().equals(bone)) {
                JsonArray p = b.getAsJsonArray("pivot");
                return new float[]{p.get(0).getAsFloat(), p.get(1).getAsFloat(), p.get(2).getAsFloat()};
            }
        }
        throw new AssertionError("no bone " + bone);
    }

    @Test
    void reworkedHeadsStayCloseToThePlayersHeadSize() throws Exception {
        for (String stem : BeastWillHeadFit.REWORKED) {
            if (!Files.exists(GEO.resolve("bwf_" + stem + "_arm_head.geo.json"))) continue;       // the many-headed wills carry their heads on the body piece
            float[] e = extent(geo("bwf_" + stem + "_arm_head"));
            float max = stem.equals("fenrir") ? BeastWillHeadFit.FENRIR_MAX : BeastWillHeadFit.SILHOUETTE_MAX;
            assertTrue(e[0] <= max * BeastWillHeadFit.PLAYER_HEAD && e[1] <= max * BeastWillHeadFit.PLAYER_HEAD, stem + " head piece " + e[0] + " x " + e[1] + " px is above " + max + "x the player's head");
        }
        float[] fenrir = extent(geo("bwf_fenrir_arm_head")), before = extent(geo("bwf_fenrir_v2_arm_head"));
        assertTrue(Math.max(fenrir[0], fenrir[1]) < Math.max(before[0], before[1]) - 4f, "Fenrir's head must be clearly smaller than before");
    }

    @Test
    void theManyHeadedWillsFanTheirNecksApartAndKeepTheirHeadCount() throws Exception {
        String[] stems = {"hydra", "typhon", "yamata_no_orochi"};
        int[] heads = {4, 4, 7};
        for (int s = 0; s < stems.length; s++) {
            JsonObject g = geo("bwf_" + stems[s] + "_arm_body");
            List<float[]> dirs = new ArrayList<>();
            for (int i = 0; i < heads[s]; i++) {
                float[] a = pivot(g, "neck" + i), j = pivot(g, "jaw" + i);                          // each head has its own neck chain and its own jaw bone
                pivot(g, "neckU" + i);
                float[] d = {j[0] - a[0], j[1] - a[1], j[2] - a[2]};
                float len = (float) Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
                assertTrue(len > 16f, stems[s] + " neck " + i + " is too short: " + len);
                dirs.add(new float[]{d[0] / len, d[1] / len, d[2] / len});
            }
            assertFalse(hasBone(g, "neck" + heads[s]), stems[s] + " has more necks than heads");
            for (int i = 0; i < dirs.size(); i++) {
                for (int k = i + 1; k < dirs.size(); k++) {
                    float[] p = dirs.get(i), q = dirs.get(k);
                    double deg = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, p[0] * q[0] + p[1] * q[1] + p[2] * q[2]))));
                    assertTrue(deg >= BeastWillHeadFit.FAN_MIN_DEGREES, stems[s] + " necks " + i + " and " + k + " are only " + deg + " degrees apart");
                }
            }
        }
    }

    private static boolean hasBone(JsonObject g, String name) {
        for (JsonElement be : g.getAsJsonArray("bones")) if (be.getAsJsonObject().get("name").getAsString().equals(name)) return true;
        return false;
    }

    @Test
    void theOldModelsAreKeptAsV2ForTheClassicHeadsSetting() throws Exception {
        for (String stem : BeastWillHeadFit.REWORKED) {
            for (String piece : BeastWillFormTable.parts(stem).split(" ")) {
                if (piece.equals("acq")) continue;
                if (!Files.exists(GEO.resolve("bwf_" + stem + "_arm_" + piece.toLowerCase(java.util.Locale.ROOT) + ".geo.json"))) continue;
                assertTrue(Files.exists(GEO.resolve("bwf_" + stem + "_v2_arm_" + piece.toLowerCase(java.util.Locale.ROOT) + ".geo.json")), stem + " " + piece + " has no _v2 copy");
            }
        }
        assertEquals(4, countNecks(geo("bwf_hydra_v2_arm_body")));
        assertEquals(7, countNecks(geo("bwf_yamata_no_orochi_v2_arm_body")));
    }

    private static int countNecks(JsonObject g) {
        int n = 0;
        for (JsonElement be : g.getAsJsonArray("bones")) if (be.getAsJsonObject().get("name").getAsString().matches("neck\\d+")) n++;
        return n;
    }

    @Test
    void theSuffixRulePicksTheRightSet() {
        assertEquals("_v1", BeastWillHeadFit.suffix(true, true, "fenrir"));
        assertEquals("_v1", BeastWillHeadFit.suffix(true, false, "lion"));
        assertEquals("_v2", BeastWillHeadFit.suffix(false, true, "fenrir"));
        assertEquals("", BeastWillHeadFit.suffix(false, true, "dragon"));                           // a will the pass did not touch has no _v2 set
        assertEquals("", BeastWillHeadFit.suffix(false, false, "fenrir"));
        assertTrue(BeastWillHeadFit.reworked("hydra") && !BeastWillHeadFit.reworked("kraken"));
    }
}
