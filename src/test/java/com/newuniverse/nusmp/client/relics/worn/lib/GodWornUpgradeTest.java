package com.newuniverse.nusmp.client.relics.worn.lib;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.relics.upgrade.UpgradeFxData;
import com.newuniverse.nusmp.relics.upgrade.UpgradeCaps;
import com.newuniverse.nusmp.vfx.VfxShape;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the God worn upgrade (1.102.0, slice god_worn) replaces the look of 24 relics with much bigger models, their own skins and glow maps and effect tables, and promises the owner
 * measurable things (5x the quads, 4x the painted texels, a LOD of 15%, a held form of 2500 vertices, effect layers, an effect for every act, a fallback to the 1.101 look). This test
 * reads the real files and checks every promise, so a regenerated or edited asset cannot silently break it: the catalog, the models (parsed with the game's parser), the vertex caps of
 * the per-grade cap table (and that the Python table and the Java constants agree), the skins and glow maps, the effect tables, the ratio file, the old files that must stay.
 */
class GodWornUpgradeTest {
    static final Path ASSETS = Path.of("src/main/resources/assets/nusmp");
    static final Path WORN = ASSETS.resolve("relic_worn");
    static final Path UPG = ASSETS.resolve("visual_upgrade");
    static final Path SKINS = ASSETS.resolve("textures/relic_worn/god");
    static final Path WAVES = Path.of("src/main/java/com/newuniverse/nusmp/relics/wave");
    static final Path DOCS = Path.of("docs/visual_upgrade");

    /** The 24 God grade worn relics of the slice. */
    static final List<String> IDS = List.of("flammes_founder_cloak", "mantle_of_the_overlord", "pactbreaker_shroud", "shadow_monarchs_mantle", "shadows_slime_cloak", "silent_winter_mantle",
            "six_faced_dragons_scale", "urek_mazinos_chain_mantle", "excaliburs_scabbard_avalon", "silvered_sash_of_reflections", "berserker_armor_shard", "orsteds_dragon_god_armor",
            "greaves_of_the_wind_walker", "white_whistle", "hand_of_vecna", "aerie_courier_gloves", "bloodbound_gauntlets", "incinerator_arm", "eye_of_vecna", "basilica_halo", "godking_crown",
            "limitless_blindfold", "odins_all_seeing_eye", "ras_dawn_crown");

    static UpgradeCatalog catalog() throws IOException {
        return UpgradeCatalog.parse(Files.readAllLines(UPG.resolve("catalog.txt")));
    }

    static WornMesh mesh(String model) throws IOException {
        return WornMesh.parse(model, Files.readAllLines(WORN.resolve(model + ".txt")));
    }

    static int vertices(List<WornCatalog.Piece> pieces) throws IOException {
        int v = 0;
        for (WornCatalog.Piece p : pieces) v += mesh(p.model()).solidVertices();
        return v;
    }

    static String waveSource(String id) throws IOException {
        String key = switch (id) {
            case "hand_of_vecna" -> "Wave30Tune.ID_HAND";
            case "eye_of_vecna" -> "Wave30Tune.ID_EYE";
            default -> "\"" + id + "\"";
        };
        try (Stream<Path> s = Files.list(WAVES)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).toList()) {
                String t = Files.readString(p);
                int at = t.indexOf("RelicDef.of(" + key + ",");
                if (at < 0) continue;
                int end = t.indexOf("out.accept", at + 10);
                return t.substring(at, end < 0 ? t.length() : end);
            }
        }
        return null;
    }

    @Test
    void theCatalogNamesEveryRelicOfTheSliceWithWornLodAndHeldPieces() throws IOException {
        UpgradeCatalog c = catalog();
        assertEquals(IDS.size(), c.size());
        for (String id : IDS) {
            assertTrue(c.has(id), id + " is in the upgrade catalog");
            assertFalse(c.up(id).isEmpty(), id + " has worn pieces");
            assertFalse(c.lod(id).isEmpty(), id + " has a LOD");
            assertFalse(c.held(id).isEmpty(), id + " has a held form");
        }
    }

    @Test
    void everyRelicIsAGodGradeWornRelicOfTheRightSlot() throws IOException {
        for (String id : IDS) {
            String src = waveSource(id);
            assertNotNull(src, id + " is defined in a wave source");
            assertTrue(src.contains("Grade.GOD"), id + " is GOD grade");
            assertTrue(src.matches("(?s).*RelicSlot\\.(HEAD|CHEST|FEET|NECK|BACK|BELT|HANDS)\\b.*"), id + " is a worn relic");
        }
    }

    @Test
    void everyModelParsesKeepsTheCapsAndNamesItsSkin() throws IOException {
        UpgradeCatalog c = catalog();
        for (String id : IDS) {
            int full = 0;
            for (WornCatalog.Piece p : c.up(id)) {
                WornMesh m = mesh(p.model());
                assertTrue(m.solidVertices() <= UpgradeCaps.GOD_MODEL_VERTICES, p.model() + " has " + m.solidVertices() + " vertices");
                assertEquals(id, m.skin, p.model() + " names the skin of its relic");
                full += m.solidVertices();
            }
            assertTrue(full <= UpgradeCaps.GOD_ENSEMBLE_VERTICES, id + " wears " + full + " vertices");
            int lod = vertices(c.lod(id)), held = vertices(c.held(id));
            assertTrue(lod > 0 && lod <= UpgradeCaps.GOD_LOD_FRACTION * full, id + ": the LOD has " + lod + " of " + full + " vertices");
            assertTrue(held > 0 && held <= UpgradeCaps.GOD_HELD_VERTICES, id + ": the held form has " + held + " vertices");
            for (WornCatalog.Piece p : c.lod(id)) assertEquals(id, mesh(p.model()).skin);
            for (WornCatalog.Piece p : c.held(id)) assertEquals(id, mesh(p.model()).skin);
        }
    }

    @Test
    void theCapTableOfThePythonGeneratorAndTheJavaConstantsAgree() throws IOException {
        String py = Files.readString(Path.of("tools/visual_upgrade/god_worn/caps.py"));
        Matcher m = Pattern.compile("\"GOD\":\\s*\\{([^}]*)\\}").matcher(py);
        assertTrue(m.find(), "the GOD row of the cap table");
        String row = m.group(1);
        assertTrue(row.contains("\"model_vertices\": " + UpgradeCaps.GOD_MODEL_VERTICES), row);
        assertTrue(row.contains("\"ensemble_vertices\": " + UpgradeCaps.GOD_ENSEMBLE_VERTICES), row);
        assertTrue(row.contains("\"held_vertices\": " + UpgradeCaps.GOD_HELD_VERTICES), row);
        assertTrue(row.contains("\"lod_fraction\": " + UpgradeCaps.GOD_LOD_FRACTION), row);
        assertTrue(row.contains("\"lod_distance\": " + (int) UpgradeCaps.LOD_DISTANCE), row);
        assertTrue(row.contains("\"additive_cutoff\": " + (int) UpgradeCaps.ADDITIVE_CUTOFF), row);
        assertTrue(UpgradeCaps.GOD_MODEL_VERTICES <= 5000, "within the shared WornMeshTest limit");
    }

    @Test
    void everySkinAndGlowMapExistsAndTheGlowMapReallyGlows() throws IOException {
        for (String id : IDS) {
            BufferedImage a = ImageIO.read(SKINS.resolve(id + ".png").toFile()), g = ImageIO.read(SKINS.resolve(id + "_glow.png").toFile());
            assertNotNull(a, id + " skin");
            assertNotNull(g, id + " glow map");
            assertEquals(512, a.getWidth());
            assertEquals(512, a.getHeight());
            assertEquals(512, g.getWidth());
            assertEquals(512, g.getHeight());
            int opaque = 0, bright = 0;
            for (int y = 0; y < 512; y += 2) {
                for (int x = 0; x < 512; x += 2) {
                    if ((a.getRGB(x, y) >>> 24) > 0) opaque++;
                    int rgb = g.getRGB(x, y);
                    if (Math.max((rgb >> 16) & 255, Math.max((rgb >> 8) & 255, rgb & 255)) > 120) bright++;
                }
            }
            assertTrue(opaque > 256 * 256 * 0.6, id + " skin is painted (" + opaque + " of 65536 samples)");
            assertTrue(bright > 300, id + " glow map has light in it (" + bright + ")");
        }
    }

    @Test
    void everyRelicHasEightAmbientLayersAndAnActEffectForEveryAct() throws IOException {
        Set<String> kinds = new HashSet<>(UpgradeFxData.KINDS);
        for (String id : IDS) {
            UpgradeFxData d = UpgradeFxData.parse(Files.readString(UPG.resolve(id + ".json")));
            assertEquals(id, d.id());
            assertTrue(d.problems().isEmpty(), id + ": " + d.problems());
            assertTrue(d.layers().size() >= 8, id + " has " + d.layers().size() + " layers");
            Set<String> used = new HashSet<>();
            for (UpgradeFxData.Layer l : d.layers()) used.add(l.kind());
            assertTrue(used.size() >= 4 && kinds.containsAll(used), id + " uses several kinds of layer: " + used);
            String src = waveSource(id);
            Matcher m = Pattern.compile("Act\\.of\\(\"([a-z0-9_]+)\"").matcher(src);
            int acts = 0;
            while (m.find()) {
                acts++;
                List<UpgradeFxData.ActFx> fx = d.actFx(m.group(1));
                assertFalse(fx.isEmpty(), id + " act " + m.group(1) + " has a visual");
                for (UpgradeFxData.ActFx a : fx) {
                    if (!a.shape().isEmpty()) assertNotNull(VfxShape.valueOf(a.shape()), "a shape of the VFX system");
                    assertTrue(a.count() <= 120 && a.duration() <= 120, id + " act " + m.group(1) + " stays bounded");
                }
            }
            assertTrue(acts >= 2, id + " has acts");
            assertTrue(d.acts().keySet().stream().allMatch(k -> src.contains("Act.of(\"" + k + "\"")), id + ": every table entry is a real act");
        }
    }

    @Test
    void theRatiosOfTheAfterFileMeetTheGodTargets() throws IOException {
        JsonObject base = JsonParser.parseString(Files.readString(DOCS.resolve("god_worn_baseline.json"))).getAsJsonObject().getAsJsonObject("items");
        JsonObject after = JsonParser.parseString(Files.readString(DOCS.resolve("god_worn_after.json"))).getAsJsonObject().getAsJsonObject("items");
        UpgradeCatalog c = catalog();
        for (String id : IDS) {
            JsonObject b = base.getAsJsonObject(id), a = after.getAsJsonObject(id);
            int bq = b.get("quads").getAsInt(), aq = a.get("quads").getAsInt();
            assertTrue(aq >= Math.max(900, 5 * bq), id + ": " + aq + " quads against " + bq);
            int realQuads = 0;
            for (WornCatalog.Piece p : c.up(id)) realQuads += mesh(p.model()).quads;
            assertEquals(realQuads, aq, id + ": the after file counts the quads of the real files");
            assertTrue(a.get("painted_texels").getAsInt() >= 4 * b.get("painted_texels").getAsInt(), id + ": painted texels");
            assertTrue(a.get("glow_map_texels").getAsInt() > 0, id + ": a glow map");
            assertTrue(a.get("ambient_layers").getAsInt() >= 3, id + ": ambient layers");
            assertTrue(a.get("animation_groups").getAsInt() >= 1, id + ": an idle animation");
            assertTrue(a.get("V").getAsDouble() >= 5.0 * b.get("V").getAsDouble(), id + ": V " + a.get("V") + " against " + b.get("V"));
            for (var e : a.getAsJsonObject("checks").entrySet()) assertTrue(e.getValue().getAsBoolean(), id + " check " + e.getKey());
        }
    }

    @Test
    void theOldLooksStayOnDiskAndTheFlagOffFallsBackToThem() throws IOException {
        WornCatalog old = WornCatalog.parse(Files.readAllLines(WORN.resolve("catalog.txt")));
        UpgradeCatalog off = UpgradeCatalog.EMPTY;
        List<String> models = new ArrayList<>();
        for (String id : IDS) {
            assertFalse(off.has(id), "no upgrade with the flag off");
            String src = waveSource(id);
            String slot = Pattern.compile("RelicSlot\\.(\\w+)").matcher(src).results().findFirst().orElseThrow().group(1);
            Matcher lk = Pattern.compile("Look\\.(\\w+)").matcher(src);
            String look = lk.find() ? lk.group(1) : WornCatalog.defaultLook(slot);
            List<WornCatalog.Piece> pieces = old.piecesFor(id, slot, look);
            assertFalse(pieces.isEmpty(), id + " still resolves to its 1.101 worn models");
            for (WornCatalog.Piece p : pieces) {
                assertTrue(Files.exists(WORN.resolve(p.model() + ".txt")), id + ": the old model " + p.model() + " is still there");
                models.add(p.model());
            }
        }
        assertTrue(Files.exists(Path.of("src/main/resources/assets/nusmp/textures/relic_worn/atlas.png")), "the old atlas stays");
        assertTrue(models.size() >= 40);
    }

    @Test
    void theUpgradeCatalogParserReadsPiecesAnchorsAndMirrorsAndIgnoresJunk() {
        UpgradeCatalog c = UpgradeCatalog.parse(List.of("# c", "", "up a m1 m2@rarm m2@larm~", "lod a m1_lod", "held a m1_held", "up b", "other x y z", "up c only_one"));
        assertEquals(2, c.size(), "a line with no piece and an unknown record are ignored");
        assertEquals(3, c.up("a").size());
        assertEquals(WornAnchor.RIGHT_ARM, c.up("a").get(1).anchor());
        assertTrue(c.up("a").get(2).mirror());
        assertEquals("m1_lod", c.lod("a").get(0).model());
        assertEquals("m1_held", c.held("a").get(0).model());
        assertTrue(c.up("nobody").isEmpty() && c.lod("nobody").isEmpty() && c.held("nobody").isEmpty());
        assertFalse(UpgradeCatalog.EMPTY.has("a"));
    }

    @Test
    void theNewFlagsAndTheSkinRecordAreParsed() {
        String q = "q 0 0 %d 0 0 0 0 0 0 -1 0   1 0 0 0 0 0 -1 0   1 1 0 0 0 0 -1 0   0 1 0 0 0 0 -1 0".replace("   ", " ");
        WornMesh m = WornMesh.parse("t", List.of("model t", "anchor body", "skin demo", String.format(q, 8), String.format(q, 16), String.format(q, 1), String.format(q, 0), String.format(q, 2)));
        assertEquals("demo", m.skin);
        assertEquals(5, m.quads);
        assertEquals(3, m.emitQuads, "LIT, ADD and EMIT quads are the ones the additive pass draws");
        assertEquals(1, m.shellQuads);
        assertNull(WornMesh.parse("t", List.of("model t", "anchor body", String.format(q, 0))).skin, "an old file has no skin");
    }

    @Test
    void everyFileTheUpgradeAddsIsListedOrAUpgradeOrLodOrHeldModel() throws IOException {
        UpgradeCatalog c = catalog();
        Set<String> listed = new HashSet<>();
        for (String id : c.ids()) {
            for (WornCatalog.Piece p : c.up(id)) listed.add(p.model());
            for (WornCatalog.Piece p : c.lod(id)) listed.add(p.model());
            for (WornCatalog.Piece p : c.held(id)) listed.add(p.model());
        }
        try (Stream<Path> s = Files.list(WORN)) {
            for (Path p : s.toList()) {
                String n = p.getFileName().toString();
                if (n.startsWith("gw_") && n.endsWith(".txt")) assertTrue(listed.contains(n.substring(0, n.length() - 4)), n + " is named by the upgrade catalog");
            }
        }
    }
}
