package com.newuniverse.nusmp.client.relics.vu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the visual upgrade of the 27 charms, gauntlet, crowns and crest must stay inside the performance guard of the performance pass (a hand item at most 6000 vertices for Genesis, 2500 for
 * God, a lod model of at most 15% of them, nothing allocated per frame) and every item must really carry what the upgrade promises (its own skin, a glow map, a lod, idle animation groups,
 * ambient layers). The numbers are read from the model files the generator wrote, with the same parser the game uses, against the caps table of assets/nusmp/visual_upgrade/caps_charms_genesis_worn.txt.
 * "Vertices" are the vertices of the model's quads; "drawn" adds the quads redrawn with the glow map (each costs a second pass).
 */
class VuCharmsBudgetTest {
    static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    static final Path VU = ASSETS.resolve("visual_upgrade");

    record Caps(int mesh, int drawn, int lodPercent) {}

    static Map<String, Caps> caps() throws IOException {
        Map<String, Caps> out = new HashMap<>();
        for (String l : Files.readAllLines(VU.resolve("caps_charms_genesis_worn.txt"))) {
            String t = l.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            String[] v = t.split("\\s+");
            out.put(v[0], new Caps(Integer.parseInt(v[1]), Integer.parseInt(v[2]), Integer.parseInt(v[3])));
        }
        return out;
    }

    static VuOverrides table() throws IOException { return VuOverrides.parse(Files.readAllLines(VU.resolve("overrides.txt"))); }

    static String grade(String id) throws IOException {
        JsonObject o = JsonParser.parseString(Files.readString(VU.resolve("fx/" + id + ".json"))).getAsJsonObject();
        return o.get("grade").getAsString();
    }

    static String name(String piece) {
        String s = piece.endsWith("~") ? piece.substring(0, piece.length() - 1) : piece;
        int at = s.indexOf('@');
        return at < 0 ? s : s.substring(0, at);
    }

    static WornMesh model(String vuName) throws IOException {
        assertTrue(vuName.startsWith("vu/"), vuName + " is not an upgrade model");
        return WornMesh.parse(vuName, Files.readAllLines(VU.resolve("models/" + vuName.substring(3) + ".txt")));
    }

    static Set<String> models(VuOverrides.Entry... entries) {
        Set<String> out = new HashSet<>();
        for (VuOverrides.Entry e : entries) for (String p : e.pieces()) out.add(name(p));
        return out;
    }

    @Test
    void thereAreTwentySevenUpgradedRelicsAndTheCapsTableCoversBothGrades() throws IOException {
        VuOverrides t = table();
        assertEquals(27, t.handCount());
        assertEquals(27, t.wornCount());
        Map<String, Caps> c = caps();
        assertEquals(2500, c.get("GOD").mesh());
        assertEquals(6000, c.get("GENESIS").mesh());
        int god = 0, genesis = 0;
        for (String id : t.handIds()) {
            if (grade(id).equals("GOD")) god++;
            else genesis++;
        }
        assertEquals(18, god);
        assertEquals(9, genesis);
    }

    @Test
    void everyModelIsWithinTheVertexCapsAndHasALodUnderFifteenPercent() throws IOException {
        VuOverrides t = table();
        Map<String, Caps> caps = caps();
        for (String id : t.handIds()) {
            Caps c = caps.get(grade(id));
            for (VuOverrides.Entry e : new VuOverrides.Entry[]{t.hand(id), t.worn(id)}) {
                int total = 0, drawn = 0;
                for (String n : models(e)) {
                    WornMesh m = model(n);
                    assertTrue(m.custom(), n + " has no skin record");
                    total += m.solidVertices();
                    drawn += m.solidVertices() + 4 * m.glowMapQuads;
                    assertNotNull(m.lodName, n + " has no lod");
                    WornMesh lod = model(m.lodName);
                    assertTrue(lod.solidVertices() <= m.solidVertices() * c.lodPercent() / 100.0, n + " lod has " + lod.solidVertices() + " of " + m.solidVertices() + " vertices (cap " + c.lodPercent() + "%)");
                    assertTrue(lod.custom(), n + " lod has no skin record");
                    assertTrue(lod.quads >= 4, n + " lod is empty");
                }
                assertTrue(total <= c.mesh(), id + " has " + total + " vertices (cap " + c.mesh() + ")");
                assertTrue(drawn <= c.drawn(), id + " draws " + drawn + " vertices with its passes (cap " + c.drawn() + ")");
            }
        }
    }

    @Test
    void everyModelCarriesItsOwnSkinGlowMapAndAnimation() throws IOException {
        VuOverrides t = table();
        for (String id : t.handIds()) {
            boolean genesis = grade(id).equals("GENESIS");
            for (String n : models(t.hand(id), t.worn(id))) {
                WornMesh m = model(n);
                for (String tex : new String[]{m.skin, m.glowTex, m.starTex}) {
                    if (tex == null) continue;
                    Path p = ASSETS.resolve("textures").resolve(tex);
                    assertTrue(Files.exists(p), n + " names the missing texture " + tex);
                }
                assertNotNull(m.glowTex, n + " has no glow map");
                assertTrue(m.glowMapQuads > 0, n + " lights no quad with its glow map");
                assertTrue(m.haloQuads > 0, n + " has no additive halo / fx quads");
                if (m.starQuads > 0) assertNotNull(m.starTex, n + " has star quads and no star texture");
                int groups = m.groups.length - 1;
                assertTrue(groups >= (genesis ? 10 : 6), n + " has only " + groups + " animation groups");
                int passes = 1 + 1 + (m.starQuads > 0 ? 1 : 0) + (m.haloQuads > 0 ? 1 : 0);
                assertTrue(passes >= (genesis ? 3 : 3), n + " has only " + passes + " passes");
                for (int i = 0; i < m.pos.length; i++) assertTrue(Float.isFinite(m.pos[i]) && Math.abs(m.pos[i]) < 80f, n + " has a vertex far away: " + m.pos[i]);
                for (int i = 0; i < m.quads; i++) {
                    if ((m.flags[i] & WornMesh.STAR) != 0) continue;
                    for (int k = 0; k < 8; k++) assertTrue(m.uv[i * 8 + k] >= -0.01f && m.uv[i * 8 + k] <= 1.01f, n + " quad " + i + " has a uv outside the skin: " + m.uv[i * 8 + k]);
                }
            }
        }
    }

    @Test
    void theSkinsArePaintedAndTheLodSkinsAreSmall() throws IOException {
        VuOverrides t = table();
        Set<String> seen = new HashSet<>();
        for (String id : t.handIds()) {
            boolean genesis = grade(id).equals("GENESIS");
            for (String n : models(t.hand(id), t.worn(id))) {
                WornMesh m = model(n);
                if (!seen.add(m.skin)) continue;
                BufferedImage img = ImageIO.read(ASSETS.resolve("textures").resolve(m.skin).toFile());
                assertNotNull(img, m.skin);
                int size = img.getWidth();
                assertEquals(size, img.getHeight(), m.skin + " is not square");
                assertEquals(genesis ? 1024 : 512, size, m.skin + " has the wrong size for its grade");
                long opaque = 0;
                for (int y = 0; y < size; y += 2) for (int x = 0; x < size; x += 2) if ((img.getRGB(x, y) >>> 24) > 0) opaque++;
                assertTrue(opaque * 4 > (long) size * size / 5, m.skin + " is mostly empty: " + opaque * 4 + " opaque texels of " + (long) size * size);
                BufferedImage lod = ImageIO.read(ASSETS.resolve("textures").resolve(model(m.lodName).skin).toFile());
                assertTrue(lod.getWidth() <= 128, "the lod skin of " + n + " is " + lod.getWidth());
            }
        }
    }

    @Test
    void everyFxFileParsesNamesRealGroupsAndKeepsTheLayerCounts() throws IOException {
        VuOverrides t = table();
        for (String id : t.handIds()) {
            VuFx fx = VuFx.parse(Files.readString(VU.resolve("fx/" + id + ".json")));
            assertEquals(id, fx.id);
            assertTrue(fx.problems().isEmpty(), id + ": " + fx.problems());
            Set<String> groups = new HashSet<>();
            for (String n : models(t.hand(id), t.worn(id))) for (WornMesh.Group g : model(n).groups) if (g != null) groups.add(g.name());
            boolean genesis = grade(id).equals("GENESIS");
            int geometry = 0, particles = 0;
            for (VuFx.Layer l : fx.layers) {
                if (l.isGeometry()) {
                    geometry++;
                    assertTrue(groups.contains(l.group), id + " layer " + l.name + " names the group " + l.group + " that no model has");
                } else {
                    particles++;
                }
            }
            assertTrue(geometry + particles >= (genesis ? 6 : 3), id + " has only " + (geometry + particles) + " ambient layers");
            assertTrue(geometry >= 2 && particles >= 2, id + " needs geometry and particle layers (" + geometry + ", " + particles + ")");
            assertTrue(fx.ambientSound.startsWith("nusmp:relic.ambient."), id + " has no ambient sound id");
        }
    }

    @Test
    void theFallbackAnswersNothingWhenTheUpgradeIsOff() throws IOException {
        VuOverrides t = table();
        for (String id : t.handIds()) {
            assertNull(t.handIf(false, id), id);
            assertNull(t.wornIf(false, id), id);
            assertNotNull(t.handIf(true, id), id);
            assertNotNull(t.wornIf(true, id), id);
        }
    }

    @Test
    void noOlderModelFileWasTouchedByTheUpgradeFiles() throws IOException {
        // the upgrade never writes into the library directories: its models live in visual_upgrade/models, so the older files and catalogs stay exactly as they were
        try (var s = Files.list(ASSETS.resolve("relic_worn"))) {
            for (Path p : s.toList()) assertFalse(p.getFileName().toString().startsWith("vu_"), p.toString());
        }
        try (var s = Files.list(ASSETS.resolve("relic_hand"))) {
            for (Path p : s.toList()) assertFalse(p.getFileName().toString().startsWith("vu_"), p.toString());
        }
        List<String> cat = Files.readAllLines(ASSETS.resolve("relic_worn/catalog.txt"));
        for (String l : cat) assertFalse(l.contains("vu/"), "the worn catalog must not name an upgrade model: " + l);
        for (String l : Files.readAllLines(ASSETS.resolve("relic_hand/catalog.txt"))) assertFalse(l.contains("vu/"), "the hand catalog must not name an upgrade model: " + l);
    }
}
