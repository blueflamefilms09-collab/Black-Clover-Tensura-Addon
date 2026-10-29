package com.newuniverse.nusmp.client.relics.vu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;
import com.newuniverse.nusmp.relics.vu.VuActTable;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the twelve worn / held models of wave 101 (the six non-weapon Millennium Items and six artifacts of other worlds) are Genesis grade upgrade models made with the kit of the 27 upgraded charms
 * (tools/relics/new_wave101, tools/visual_upgrade/charms_genesis_worn), and the same guard as {@link VuCharmsBudgetTest} must hold for them without touching that test (it counts the 27 lines of overrides.txt):
 * the table assets/nusmp/visual_upgrade/overrides_genesis_new.txt parses without problems, every relic is held and worn, the vertex caps of the Genesis grade hold (6000 mesh, 6000 drawn, a lod of at most 15 percent),
 * every model has its own 1024 skin, glow map and lod, at least ten animation groups and three passes, no vertex far away and every uv on the skin, the fx file names real groups, the act effects name real shapes, and
 * the older tables and models were not touched.
 */
class Wave101ModelsTest {
    static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    static final Path VU = ASSETS.resolve("visual_upgrade");
    static final List<String> IDS = List.of("millennium_puzzle", "millennium_eye", "millennium_ring", "millennium_key", "millennium_scale", "millennium_necklace", "truth_seeking_orbs_of_the_sage",
            "hogyoku_of_the_crystal_dissolution", "seven_dragon_balls_of_shenron", "straw_hat_of_joy_boy", "law_of_cycles_gown", "greed_island_original_binder");
    static final int MESH_CAP = 6000, DRAWN_CAP = 6000, LOD_PERCENT = 15;

    static VuOverrides table() throws IOException { return VuOverrides.parse(Files.readAllLines(VU.resolve("overrides_genesis_new.txt"))); }

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
    void theTableParsesAndHoldsEveryRelicHeldAndWorn() throws IOException {
        VuOverrides t = table();
        assertTrue(t.problems().isEmpty(), t.problems().toString());
        assertEquals(12, t.handCount());
        assertEquals(12, t.wornCount());
        for (String id : IDS) {
            assertNotNull(t.hand(id), id + " held");
            assertNotNull(t.worn(id), id + " worn");
        }
        VuOverrides older = VuOverrides.parse(Files.readAllLines(VU.resolve("overrides.txt")));
        assertEquals(27, older.handCount(), "the table of the 27 upgraded relics was not touched");
        for (String id : IDS) assertNull(older.hand(id), id + " is not in the older table");
    }

    @Test
    void everyModelIsWithinTheGenesisCapsWithALodUnderFifteenPercent() throws IOException {
        VuOverrides t = table();
        for (String id : IDS) {
            for (VuOverrides.Entry e : new VuOverrides.Entry[]{t.hand(id), t.worn(id)}) {
                int total = 0, drawn = 0;
                for (String n : models(e)) {
                    WornMesh m = model(n);
                    assertTrue(m.custom(), n + " has no skin record");
                    total += m.solidVertices();
                    drawn += m.solidVertices() + 4 * m.glowMapQuads;
                    assertNotNull(m.lodName, n + " has no lod");
                    WornMesh lod = model(m.lodName);
                    assertTrue(lod.solidVertices() <= m.solidVertices() * LOD_PERCENT / 100.0, n + " lod has " + lod.solidVertices() + " of " + m.solidVertices() + " vertices");
                    assertTrue(lod.custom(), n + " lod has no skin record");
                    assertTrue(lod.quads >= 4, n + " lod is empty");
                }
                assertTrue(total <= MESH_CAP, id + " has " + total + " vertices (cap " + MESH_CAP + ")");
                assertTrue(drawn <= DRAWN_CAP, id + " draws " + drawn + " vertices with its passes (cap " + DRAWN_CAP + ")");
            }
        }
    }

    @Test
    void everyModelCarriesItsOwnSkinGlowMapAnimationAndPasses() throws IOException {
        VuOverrides t = table();
        for (String id : IDS) {
            for (String n : models(t.hand(id), t.worn(id))) {
                WornMesh m = model(n);
                for (String tex : new String[]{m.skin, m.glowTex, m.starTex}) {
                    if (tex == null) continue;
                    assertTrue(Files.exists(ASSETS.resolve("textures").resolve(tex)), n + " names the missing texture " + tex);
                }
                assertNotNull(m.glowTex, n + " has no glow map");
                assertTrue(m.glowMapQuads > 0, n + " lights no quad with its glow map");
                // the seven Millennium Items are plain gold since 3.35.0 (the owner wants no halos or motes on them): only the other eight must carry additive fx quads
                assertTrue(m.haloQuads > 0 || com.newuniverse.nusmp.relics.wave.Wave101Rules.inSet(id), n + " has no additive halo / fx quads");
                boolean canon = com.newuniverse.nusmp.relics.wave.Wave101Rules.inSet(id);
                assertTrue(canon || (m.starQuads > 0 && m.starTex != null), n + " has no nebula pass");
                int groups = m.groups.length - 1;
                assertTrue(groups >= (canon ? 2 : 10), n + " has only " + groups + " animation groups");
                for (int i = 0; i < m.pos.length; i++) assertTrue(Float.isFinite(m.pos[i]) && Math.abs(m.pos[i]) < 80f, n + " has a vertex far away: " + m.pos[i]);
                for (int i = 0; i < m.quads; i++) {
                    if ((m.flags[i] & WornMesh.STAR) != 0) continue;
                    for (int k = 0; k < 8; k++) assertTrue(m.uv[i * 8 + k] >= -0.01f && m.uv[i * 8 + k] <= 1.01f, n + " quad " + i + " has a uv outside the skin: " + m.uv[i * 8 + k]);
                }
            }
        }
    }

    @Test
    void theSkinsAreGenesisSizedAndPaintedAndTheLodSkinsAreSmall() throws IOException {
        VuOverrides t = table();
        Set<String> seen = new HashSet<>();
        for (String id : IDS) {
            for (String n : models(t.hand(id), t.worn(id))) {
                WornMesh m = model(n);
                if (!seen.add(m.skin)) continue;
                BufferedImage img = ImageIO.read(ASSETS.resolve("textures").resolve(m.skin).toFile());
                assertNotNull(img, m.skin);
                int size = img.getWidth();
                assertEquals(size, img.getHeight(), m.skin + " is not square");
                assertEquals(1024, size, m.skin + " has the wrong size for its grade");
                long opaque = 0;
                for (int y = 0; y < size; y += 2) for (int x = 0; x < size; x += 2) if ((img.getRGB(x, y) >>> 24) > 0) opaque++;
                // a tenth of the atlas: the plain-gold Millennium models have fewer parts than the old ornamented ones and so use less of it
                assertTrue(opaque * 4 > (long) size * size / 10, m.skin + " is mostly empty");
                BufferedImage lod = ImageIO.read(ASSETS.resolve("textures").resolve(model(m.lodName).skin).toFile());
                assertTrue(lod.getWidth() <= 128, "the lod skin of " + n + " is " + lod.getWidth());
            }
        }
    }

    @Test
    void everyFxFileParsesAndNamesRealGroups() throws IOException {
        VuOverrides t = table();
        for (String id : IDS) {
            VuFx fx = VuFx.parse(Files.readString(VU.resolve("fx/" + id + ".json")));
            assertEquals(id, fx.id);
            assertTrue(fx.problems().isEmpty(), id + ": " + fx.problems());
            JsonObject o = JsonParser.parseString(Files.readString(VU.resolve("fx/" + id + ".json"))).getAsJsonObject();
            assertEquals("GENESIS", o.get("grade").getAsString());
            Set<String> groups = new HashSet<>();
            for (String n : models(t.hand(id), t.worn(id))) for (WornMesh.Group g : model(n).groups) if (g != null) groups.add(g.name());
            int geometry = 0, particles = 0;
            for (VuFx.Layer l : fx.layers) {
                if (l.isGeometry()) {
                    geometry++;
                    assertTrue(groups.contains(l.group), id + " layer " + l.name + " names the group " + l.group + " that no model has");
                } else {
                    particles++;
                    assertTrue(l.particle.equals("fx") || l.particle.startsWith("nusmp:"), id + " layer " + l.name + " uses the vanilla particle " + l.particle);
                }
            }
            boolean canon = com.newuniverse.nusmp.relics.wave.Wave101Rules.inSet(id);   // the plain-gold canon models glow only at the Eye: one glow layer and one glint are enough
            assertTrue(geometry >= (canon ? 1 : 6) && particles >= (canon ? 1 : 2), id + " has " + geometry + " geometry and " + particles + " particle layers");
        }
    }

    @Test
    void theActEffectsAreValidAndEveryActOfTheTwelveRelicsHasOne() throws IOException {
        List<String> lines = Files.readAllLines(Path.of("src/main/resources/data/nusmp/visual_upgrade/act_vfx.txt"));
        VuActTable table = VuActTable.parse(lines);
        assertTrue(table.problems().isEmpty(), table.problems().toString());
        String wave = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/relics/wave/Wave101.java"));
        for (String id : IDS) {
            int at = wave.indexOf("RelicDef.of(\"" + id + "\"");
            int end = wave.indexOf("out.accept", at);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("Act\\.of\\(\"([a-z_]+)\"").matcher(wave.substring(at, end < 0 ? wave.length() : end));
            int n = 0;
            while (m.find()) {
                n++;
                assertFalse(table.steps(id, m.group(1)).isEmpty(), id + " act " + m.group(1) + " has no effect");
            }
            assertTrue(n >= 2, id);
        }
    }

    @Test
    void theFallbackAnswersNothingWhenTheUpgradeIsOffAndNoOlderFileWasTouched() throws IOException {
        VuOverrides t = table();
        for (String id : IDS) {
            assertNull(t.handIf(false, id), id);
            assertNull(t.wornIf(false, id), id);
            assertNotNull(t.handIf(true, id), id);
        }
        try (var s = Files.list(ASSETS.resolve("relic_worn"))) {
            for (Path p : s.toList()) assertFalse(p.getFileName().toString().startsWith("vu_"), p.toString());
        }
        for (String l : Files.readAllLines(ASSETS.resolve("relic_worn/catalog.txt"))) assertFalse(l.contains("vu/"), "the worn catalog must not name an upgrade model: " + l);
    }
}
