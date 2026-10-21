package com.newuniverse.nusmp.client.relics.vu;

import com.newuniverse.nusmp.client.relics.worn.lib.WornAnchor;
import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;
import com.newuniverse.nusmp.client.relics.worn.lib.WornPlacer;
import com.newuniverse.nusmp.relics.upgrade.UpgradeCaps;
import com.newuniverse.nusmp.vfx.deck.DeckSummonGeometry;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the deck relics are worn as a duel disk on the left forearm (1.110.0, docs/visual_upgrade/duel_disk.md). Nothing can be seen here, so the real model files are measured with the parser the
 * game uses: every deck relic has a disk model worn and held, the disk is anchored on the left arm and stands clear of the sleeve layer of the arm, the vertex budget of its grade holds (God
 * 2500 with the floating cards, the Special grade the legacy 2600), the lod is at most 15 percent, the textures exist, the floating cards of the Deck of Many Things are still there quad for
 * quad (the owner's request: keep the effect), and the older models and tables were not touched.
 */
class DuelDiskModelsTest {
    static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    static final Path VU = ASSETS.resolve("visual_upgrade");
    static final Path MODELS = VU.resolve("models");
    static final float FIT = WornPlacer.BODY_SCALE;                       // the worn library draws a relic piece at this scale about its anchor

    static final Map<String, Integer> CAP_DRAWN = Map.of("deck_of_many_things", UpgradeCaps.lookup(UpgradeCaps.SLICE_CHARMS_GENESIS, "GOD").ensembleVertices(),
            "pact_of_the_deck", UpgradeCaps.LEGACY_ENSEMBLE_VERTICES);
    static final Map<String, Integer> SKIN = Map.of("deck_of_many_things", 512, "pact_of_the_deck", 256);

    static VuOverrides table() throws IOException { return VuOverrides.parse(Files.readAllLines(VU.resolve("overrides_duel_disk.txt"))); }

    static String name(String piece) {
        String s = piece.endsWith("~") ? piece.substring(0, piece.length() - 1) : piece;
        int at = s.indexOf('@');
        return at < 0 ? s : s.substring(0, at);
    }

    static WornMesh model(String vuName) throws IOException {
        assertTrue(vuName.startsWith("vu/"), vuName);
        return WornMesh.parse(vuName, Files.readAllLines(MODELS.resolve(vuName.substring(3) + ".txt")));
    }

    @Test
    void everyDeckRelicHasADiskWornAndHeld() throws IOException {
        VuOverrides t = table();
        assertTrue(t.problems().isEmpty(), t.problems().toString());
        for (String id : new String[]{"deck_of_many_things", "pact_of_the_deck"}) {
            assertNotNull(t.worn(id), id + " worn");
            assertNotNull(t.hand(id), id + " held");
            boolean wornDisk = false, handDisk = false;
            for (String p : t.worn(id).pieces()) wornDisk |= name(p).contains("duel_disk");
            for (String p : t.hand(id).pieces()) handDisk |= name(p).contains("duel_disk");
            assertTrue(wornDisk && handDisk, id + " is a duel disk worn and held");
        }
    }

    @Test
    void theDiskIsOnTheLeftForearmAndClearOfTheSleeve() throws IOException {
        VuOverrides t = table();
        float[] box = WornMesh.overlayBox(WornAnchor.LEFT_ARM);
        for (String id : new String[]{"deck_of_many_things", "pact_of_the_deck"}) {
            for (String p : t.worn(id).pieces()) {
                WornMesh m = model(name(p));
                assertEquals(WornAnchor.LEFT_ARM, m.anchor, name(p) + " is anchored on the left arm");
                assertSame(m, m.clearOf(WornAnchor.LEFT_ARM, false, FIT, 0f, 0f, 0f), name(p) + " needs no pushing out of the sleeve layer: it already stands clear");
                float minY = 1e9f, maxY = -1e9f;
                for (int g = 0; g < m.groups.length; g++) {
                    String gn = m.groups[g] == null ? "" : m.groups[g].name();
                    boolean orbiting = gn.equals("fate_cards") || gn.equals("fate_cards_far") || gn.equals("pact_cards");
                    for (int q = m.groupStart[g]; q < m.groupStart[g + 1]; q++) {
                        if ((m.flags[q] & (WornMesh.HALO | WornMesh.STAR)) != 0 || orbiting) continue;
                        for (int c = 0; c < 4; c++) {
                            float x = m.pos[q * 12 + c * 3] * FIT, y = m.pos[q * 12 + c * 3 + 1] * FIT, z = m.pos[q * 12 + c * 3 + 2] * FIT;
                            boolean inside = x > box[0] + 0.01f && x < box[1] - 0.01f && y > box[2] && y < box[3] && z > box[4] + 0.01f && z < box[5] - 0.01f;
                            assertFalse(inside, name(p) + " has a vertex inside the sleeve layer at " + x + "," + y + "," + z);
                            minY = Math.min(minY, y);
                            maxY = Math.max(maxY, y);
                            assertTrue(x > -2.2f && x < 7.0f, name(p) + " stays by the arm in x: " + x);
                            assertTrue(z > -6.6f && z < 3.6f, name(p) + " stays by the arm in z: " + z);
                        }
                    }
                }
                if (name(p).contains("duel_disk")) {
                    assertTrue(minY > 1.0f && maxY < 10.3f, name(p) + " lies on the arm (the forearm, y 1..10.3): " + minY + ".." + maxY);
                }
            }
        }
    }

    @Test
    void theVertexBudgetOfTheGradeHolds() throws IOException {
        VuOverrides t = table();
        for (String id : CAP_DRAWN.keySet()) {
            for (VuOverrides.Entry e : new VuOverrides.Entry[]{t.worn(id), t.hand(id)}) {
                int total = 0;
                for (String p : e.pieces()) {
                    WornMesh m = model(name(p));
                    total += m.totalVertices();
                    assertTrue(m.totalVertices() <= CAP_DRAWN.get(id), name(p) + " is one model file under the cap of its grade");
                }
                assertTrue(total <= CAP_DRAWN.get(id), id + " " + e.kind() + " draws " + total + " vertices, the cap of its grade is " + CAP_DRAWN.get(id));
            }
        }
        assertEquals(2500, CAP_DRAWN.get("deck_of_many_things").intValue(), "the God grade of the Charms / Genesis slice");
    }

    @Test
    void theLodIsAtMostFifteenPercentAndTheTexturesExist() throws IOException {
        VuOverrides t = table();
        for (String id : CAP_DRAWN.keySet()) {
            for (String p : t.worn(id).pieces()) {
                String n = name(p);
                if (!n.contains("duel_disk")) continue;
                WornMesh m = model(n);
                assertNotNull(m.lodName, n + " has a lod");
                WornMesh lod = model(m.lodName);
                assertTrue(lod.quads * 100 <= m.quads * 15, n + " lod " + lod.quads + " of " + m.quads + " quads");
                for (String tex : new String[]{m.skin, m.glowTex, m.starTex, lod.skin, lod.glowTex}) {
                    if (tex == null) continue;
                    assertTrue(Files.exists(ASSETS.resolve("textures").resolve(tex)), n + " names the missing texture " + tex);
                }
                BufferedImage skin = ImageIO.read(ASSETS.resolve("textures").resolve(m.skin).toFile());
                assertEquals(SKIN.get(id).intValue(), skin.getWidth(), id + " skin size by grade");
                assertTrue(m.glowMapQuads > 0, n + " has lit parts (the life points, the zones)");
                assertTrue(m.groups.length > 6, n + " has animated groups (the fan, the life points, the top card ...)");
            }
        }
        WornMesh god = model("vu/duel_disk_deck_of_many_things_worn"), special = model("vu/duel_disk_pact_of_the_deck_worn");
        assertTrue(god.starQuads > 0 && god.haloQuads > 0, "the God grade has the nebula inlay and the halo shapes");
        assertEquals(0, special.starQuads, "the Special grade has no nebula");
        assertTrue(special.haloQuads < god.haloQuads, "and fewer halo shapes (only the small lens flare)");
    }

    @Test
    void theDiskKeepsTheNumbersOfTheSummonEffect() throws IOException {
        WornMesh m = model("vu/duel_disk_deck_of_many_things_worn");
        WornMesh.Group fan = null;
        for (WornMesh.Group g : m.groups) if (g != null && g.name().equals("card_fan")) fan = g;
        assertNotNull(fan, "the blade fan is a group");
        assertEquals(DeckSummonGeometry.HUB_X, fan.px() * FIT, 0.01, "the hub of the model is where the summon effect lands its card");
        assertEquals(DeckSummonGeometry.HUB_YD, fan.py() * FIT, 0.01);
        assertEquals(DeckSummonGeometry.HUB_Z, fan.pz() * FIT, 0.01);
    }

    @Test
    void theFloatingCardsOfTheDeckOfManyThingsAreKeptQuadForQuad() throws IOException {
        List<String> old = Files.readAllLines(MODELS.resolve("deck_of_many_things_worn.txt"));
        List<String> now = Files.readAllLines(MODELS.resolve("deck_of_many_things_cards_worn.txt"));
        Map<String, String[]> oldGroups = groups(old), newGroups = groups(now);
        for (String g : Set.of("fate_cards", "fate_cards_far")) {
            assertTrue(newGroups.containsKey(g), g + " exists in the new piece");
            String[] a = oldGroups.get(g), b = newGroups.get(g);
            assertEquals(a[3], b[3], g + " keeps its motion kind");
            for (int i = 7; i <= 9; i++) assertEquals(a[i], b[i], g + " keeps its animation numbers");
        }
        List<String[]> oq = quadsOf(old, oldGroups, Set.of("fate_cards", "fate_cards_far")), nq = quadsOf(now, newGroups, Set.of("fate_cards", "fate_cards_far"));
        assertEquals(108, oq.size());
        assertEquals(oq.size(), nq.size(), "the same number of quads");
        float dx = Float.NaN, dy = Float.NaN, dz = Float.NaN;
        for (int i = 0; i < oq.size(); i++) {
            String[] a = oq.get(i), b = nq.get(i);
            assertEquals(a[2], b[2], "slot");
            assertEquals(a[3], b[3], "flags");
            for (int c = 0; c < 4; c++) {
                for (int k = 3; k < 8; k++) assertEquals(a[4 + c * 8 + k], b[4 + c * 8 + k], "uv and normal of quad " + i);
                float ex = Float.parseFloat(b[4 + c * 8]) - Float.parseFloat(a[4 + c * 8]), ey = Float.parseFloat(b[5 + c * 8]) - Float.parseFloat(a[5 + c * 8]), ez = Float.parseFloat(b[6 + c * 8]) - Float.parseFloat(a[6 + c * 8]);
                if (Float.isNaN(dx)) {
                    dx = ex;
                    dy = ey;
                    dz = ez;
                }
                assertEquals(dx, ex, 0.002, "the cards were only moved, all by the same offset");
                assertEquals(dy, ey, 0.002);
                assertEquals(dz, ez, 0.002);
            }
        }
        // the pieces of the table: the disk first (it carries the effects of the first piece), the cards beside it
        VuOverrides t = table();
        assertTrue(t.worn("deck_of_many_things").pieces().stream().anyMatch(p -> name(p).equals("vu/deck_of_many_things_cards_worn")), "the cards are a worn piece of the Deck of Many Things");
        assertTrue(t.hand("deck_of_many_things").pieces().stream().anyMatch(p -> name(p).equals("vu/deck_of_many_things_cards_hand")), "and a held piece");
        // the ambient layers of the older fx file still name the card groups
        String fx = Files.readString(VU.resolve("fx/deck_of_many_things.json"));
        assertTrue(fx.contains("\"fate_cards\"") && fx.contains("\"fate_cards_far\""));
    }

    @Test
    void theOlderModelsAndTablesAreKept() throws IOException {
        String old = Files.readString(VU.resolve("overrides.txt"));
        assertTrue(old.contains("worn deck_of_many_things vu/deck_of_many_things_worn"), "the older override line stays");
        assertTrue(Files.exists(MODELS.resolve("deck_of_many_things.txt")) && Files.exists(MODELS.resolve("deck_of_many_things_worn.txt")));
        assertEquals(8, groups(Files.readAllLines(MODELS.resolve("deck_of_many_things_worn.txt"))).size(), "the older worn model still has its eight groups");
        assertTrue(Files.exists(ASSETS.resolve("relic_worn/charm_orbit.txt")), "the library model the Pact of the Deck wore stays");
        assertTrue(Files.exists(ASSETS.resolve("relic_hand/pact_cards.txt")), "and the model it was held as");
    }

    @Test
    void anArmMountedModelIgnoresThePlaceAmongItsSlotMates() {
        for (int i = 0; i < 6; i++) {
            assertEquals(WornPlacer.Placement.NONE, WornPlacer.place("CHARM", i, "vu/duel_disk_deck_of_many_things_worn"));
            assertEquals(WornPlacer.Placement.NONE, WornPlacer.place("CHARM", i, "vu/deck_of_many_things_cards_worn"));
        }
        assertEquals(WornPlacer.place("CHARM", 2), WornPlacer.place("CHARM", 2, "charm_orbit"), "other models are placed as before");
        assertTrue(WornPlacer.armMounted("vu/duel_disk_pact_of_the_deck_worn"));
        assertFalse(WornPlacer.armMounted("amulet_crest"));
    }

    // ------------------------------------------------------------ file helpers
    static Map<String, String[]> groups(List<String> lines) {
        Map<String, String[]> out = new java.util.LinkedHashMap<>();
        for (String l : lines) {
            String[] v = l.trim().split("\\s+");
            if (v.length > 9 && v[0].equals("grp")) out.put(v[2], v);
        }
        return out;
    }

    static List<String[]> quadsOf(List<String> lines, Map<String, String[]> groups, Set<String> names) {
        Set<String> ids = new java.util.HashSet<>();
        for (String n : names) ids.add(groups.get(n)[1]);
        List<String[]> out = new ArrayList<>();
        for (String l : lines) {
            String[] v = l.trim().split("\\s+");
            if (v.length == 36 && v[0].equals("q") && ids.contains(v[1])) out.add(v);
        }
        return out;
    }
}
