package com.newuniverse.nusmp.client.geo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.devil.WorldDevil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * docs/wings_on_back.md, checked with no game: the convention of a wing worn on a player ({@link WingFrame}) holds for every worn wing of the mod, the wings of the report (devil forms, Black Clover boss looks, the
 * Dark, Flame, Lightning, Water, Spirit Dive and Valkyrie armour wings) were forward before and are on the back after, the fix is an exact mirror (same wing, behind), the wings that were right are not touched, the
 * option is a CLIENT config that defaults on with the old drawing one switch away, and the wings that are posed by code (the generic Weg wings of a double-magic devil, the Djinn, the mode armours, the relic wings,
 * the old devil meshes) hinge behind the torso and sweep back.
 */
class WingsOnBackTest {
    private static final String GEO = "src/main/resources/assets/anime_multiverse/geo/entity/";
    private static final String JAVA = "src/main/java/com/newuniverse/nusmp/";

    private static GeoModelData load(String name) throws Exception {
        try (Reader r = Files.newBufferedReader(Path.of(GEO + name + ".geo.json"))) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            return GeoModelData.parse("anime_multiverse:geo/entity/" + name + ".geo.json", o);
        }
    }

    private static GeoModelData parse(String name, String json) {
        return GeoModelData.parse("anime_multiverse:geo/entity/" + name + ".geo.json", JsonParser.parseString(json).getAsJsonObject());
    }

    private static String read(String path) throws Exception { return Files.readString(Path.of(path)); }

    /** The worn families: every player-model extras file, and the armour / form wing pieces drawn on a player by the auras. Prop and boss-entity wings (spirits, the foundry dragons ...) have their own frame and are not worn. */
    private static final Pattern WORN = Pattern.compile("pmx_.*|(dark|flame|lightning|water)_arm_wing_[lr]|spiritdive_arm_.*wing.*|sdj_.*_arm_wing_[lr]|(nvl|ndg|nbk)_arm_wings2?|bwf_.*_arm_wings"
            + "|wing_back|corundum_wings|paladin_wings|avatar_wing_\\d|soul_armour");

    /** The convention (docs/wings_on_back.md section 1): behind the torso plane, never ending in front of the hinge, a spread wing's tip out past the hinge. */
    private static String breaks(WingFrame.Group g) {
        if (g.pivot()[2] < WingFrame.MIN_PIVOT_Z) return null;                            // not a back piece (chest emblem, arm blade): outside the convention
        if (g.centroid()[2] <= WingFrame.BACK_PLANE_Z) return "centroid z " + g.centroid()[2] + " is not behind the torso plane";
        if (g.dz() < WingFrame.FORWARD_DZ) return "centroid is " + g.dz() + " px in front of the hinge";
        if (g.tipDz() < WingFrame.FORWARD_TIP_DZ) return "the tip ends " + g.tipDz() + " px in front of the hinge";
        boolean spread = Math.abs(g.centroid()[0]) - Math.abs(g.pivot()[0]) >= 3.0;
        if (spread && Math.abs(g.tip()[0]) <= Math.abs(g.pivot()[0])) return "the tip is not out past the hinge";
        return null;
    }

    // ================================================================ the maths
    private static final String CARD = """
            {"minecraft:geometry":[{"description":{"texture_width":64,"texture_height":48},"bones":[
              {"name":"x_body","pivot":[0,24,0]},
              {"name":"wing_l","parent":"x_body","pivot":[2.2,22,2.8],"cubes":[
                {"origin":[2.2,3.65,2.8],"size":[30,22.5,0],"pivot":[2.2,22,2.8],"rotation":[0,%s,14],"uv":{"north":{"uv":[0,0],"uv_size":[30,22]}}}]},
              {"name":"wing_r","parent":"x_body","pivot":[-2.2,22,2.8],"cubes":[
                {"origin":[-32.2,3.65,2.8],"size":[30,22.5,0],"pivot":[-2.2,22,2.8],"rotation":[0,%s,-14],"uv":{"north":{"uv":[0,0],"uv_size":[30,22]}}}]}]}]}
            """;

    @Test
    void theGeneratorsSignWasTheBug_leftWingPlusYSweepsToTheFront_minusYToTheBack() {
        List<WingFrame.Group> before = WingFrame.groups(parse("t_forward", CARD.formatted("38", "-38")));
        assertEquals(2, before.size());
        for (WingFrame.Group g : before) {
            assertTrue(g.sweepsForward(), g.root() + " with the generator's sign must sweep forward: dz " + g.dz());
            assertTrue(g.tipDz() < -10, "the tip is far in front of the hinge: " + g.tipDz());
        }
        List<WingFrame.Group> good = WingFrame.groups(parse("t_back", CARD.formatted("-38", "38")));
        for (WingFrame.Group g : good) {
            assertFalse(g.sweepsForward(), g.root());
            assertTrue(g.dz() > 5 && g.tipDz() > 10, g.root() + " sweeps back: dz " + g.dz() + " tip " + g.tipDz());
            assertNull(breaks(g), g.root());
        }
    }

    @Test
    void fixIsAnExactMirrorOfTheWingBehindItsHinge() {
        GeoModelData m = parse("t_forward", CARD.formatted("38", "-38"));
        GeoModelData fixed = WingFrame.fix(m);
        assertNotNull(fixed);
        List<WingFrame.Group> a = WingFrame.groups(m), b = WingFrame.groups(fixed);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            WingFrame.Group x = a.get(i), y = b.get(i);
            assertEquals(x.pivot()[2], y.pivot()[2], 1e-6, "the hinge does not move");
            assertEquals(x.centroid()[0], y.centroid()[0], 1e-4, "same x");
            assertEquals(x.centroid()[1], y.centroid()[1], 1e-4, "same height");
            assertEquals(2 * x.pivot()[2] - x.centroid()[2], y.centroid()[2], 1e-4, "z mirrored in the hinge plane");
            assertEquals(Math.hypot(Math.hypot(x.tip()[0] - x.pivot()[0], x.tip()[1] - x.pivot()[1]), x.tipDz()),
                    Math.hypot(Math.hypot(y.tip()[0] - y.pivot()[0], y.tip()[1] - y.pivot()[1]), y.tipDz()), 1e-4, "same size");
            assertFalse(y.sweepsForward());
            assertNull(breaks(y));
        }
        assertNull(WingFrame.fix(fixed), "a fixed model has nothing left to turn (idempotent)");
        assertTrue(fixed.bones.get("wing_l").zMirrored && fixed.bones.get("wing_r").zMirrored, "the animation's x / y turn mirrors with the wing");
        assertFalse(fixed.bones.get("x_body").zMirrored, "the body is not a wing");
    }

    @Test
    void theOriginalIsNeverChangedSoTheOptionCanGoBackOff() {
        GeoModelData m = parse("t_forward", CARD.formatted("38", "-38"));
        float ry = m.bones.get("wing_l").cubes.get(0).ry, oz = m.bones.get("wing_l").cubes.get(0).oz;
        GeoModelData twin = m.onBack();
        assertNotSame(m, twin, "a forward wing makes a copy");
        assertSame(twin, m.onBack(), "worked out once");
        assertEquals(ry, m.bones.get("wing_l").cubes.get(0).ry, "the original cube keeps its turn");
        assertEquals(oz, m.bones.get("wing_l").cubes.get(0).oz);
        assertEquals(-ry, twin.bones.get("wing_l").cubes.get(0).ry, "the copy has the sweep the other way");
        assertSame(twin, twin.onBack(), "the copy does not look for a copy of itself");
        GeoModelData ok = parse("t_back", CARD.formatted("-38", "38"));
        assertSame(ok, ok.onBack(), "a wing that is already on the back is drawn as it is: no copy");
    }

    @Test
    void aPieceThatIsNotAtTheBackIsNeverTurned() {
        String chest = CARD.replace("2.8", "-1.5");                                      // hinge and cards in front of the torso plane: an emblem, not a wing
        assertNull(WingFrame.fix(parse("t_chest", chest.formatted("38", "-38"))));
    }

    @Test
    void aWingFileIsRecognisedByItsNameAndAnArmsFileIsNot() {
        for (String n : new String[]{"dark_arm_wing_l", "water_arm_wing_r", "spiritdive_arm_fire_wingbud_l", "spiritdive_arm_saint_wing_r", "nvl_arm_wings2", "bwf_dragon_arm_wings", "corundum_wings"})
            assertTrue(WingFrame.isWingFile("anime_multiverse:geo/entity/" + n + ".geo.json"), n);
        for (String n : new String[]{"pmx_foundry_deva_wingmaiden", "pmx_dv_lucifero", "wing_back", "wing_chest", "wing_greave_l", "pmx_foundry_hero_society_wingsuit_scout"})
            assertFalse(WingFrame.isWingFile(n + ".geo.json"), n);
        assertTrue(WingFrame.isWingBone("wing1_l") && WingFrame.isWingBone("x_wing_r2") && WingFrame.isWingBone("wingA_L") && !WingFrame.isWingBone("x_body") && !WingFrame.isWingBone("arm_l"));
    }

    // ================================================================ the files
    private static List<String> wornFiles() {
        File[] fs = new File(GEO).listFiles((d, n) -> n.endsWith(".geo.json"));
        assertNotNull(fs, "the test runs from the repository root");
        List<String> names = new ArrayList<>();
        for (File f : fs) {
            String n = f.getName().substring(0, f.getName().length() - ".geo.json".length());
            if (WORN.matcher(n).matches()) names.add(n);
        }
        names.sort(null);
        return names;
    }

    /** name -> [wing groups, groups forward before the fix, groups that break the convention after it]. */
    private static Map<String, int[]> survey(List<String> breakingAfter) throws Exception {
        Map<String, int[]> out = new TreeMap<>();
        for (String n : wornFiles()) {
            GeoModelData m = load(n);
            List<WingFrame.Group> raw = WingFrame.groups(m);
            if (raw.isEmpty()) continue;
            int fwd = 0;
            for (WingFrame.Group g : raw) if (g.sweepsForward()) fwd++;
            GeoModelData after = m.onBack();
            int bad = 0;
            for (WingFrame.Group g : WingFrame.groups(after)) {
                String why = breaks(g);
                if (why != null) {
                    bad++;
                    breakingAfter.add(n + " " + g.root() + ": " + why);
                }
            }
            out.put(n, new int[]{raw.size(), fwd, bad});
        }
        return out;
    }

    @Test
    void everyWornWingOfTheModFollowsTheConventionAfterTheFix() throws Exception {
        List<String> bad = new ArrayList<>();
        Map<String, int[]> s = survey(bad);
        int groups = 0, forward = 0;
        for (int[] v : s.values()) { groups += v[0]; forward += v[1]; }
        assertTrue(s.size() >= 120 && groups >= 250, "the test should have seen the mod's worn wings: " + s.size() + " files, " + groups + " wings");
        assertTrue(forward >= 60, "the wings of the report are in the files (" + forward + " forward wings): if this fails the files were regenerated with the right sign, lower the number");
        assertTrue(bad.isEmpty(), "wings that still break the convention after the fix: " + bad);
    }

    @Test
    void theFormsOfTheReportWereForwardAndAreFixed() throws Exception {
        // every devil of the roster that has wings (the Devil Union and every other stage of the form wear them): forward in the file, on the back after the fix
        List<String> winged = new ArrayList<>();
        for (int i = 0; i < WorldDevil.count(); i++) {
            String id = WorldDevil.at(i).id();
            File f = new File(GEO + "pmx_dv_" + id + ".geo.json");
            if (!f.isFile()) continue;
            GeoModelData m = load("pmx_dv_" + id);
            List<WingFrame.Group> raw = WingFrame.groups(m);
            if (raw.isEmpty()) continue;
            winged.add(id);
            for (WingFrame.Group g : raw) assertTrue(g.sweepsForward(), "devil " + id + " " + g.root() + " is forward in the file: dz " + g.dz());
            for (WingFrame.Group g : WingFrame.groups(m.onBack())) assertNull(breaks(g), "devil " + id + " " + g.root());
        }
        for (String must : new String[]{"liebe", "lucifero", "beelzebub", "astaroth", "megicula", "lucifugus", "sebastian", "daemon_noir", "daemon_blanc", "daemon_jaune", "daemon_violet",
                "daemon_rouge", "daemon_vert", "daemon_bleu", "kin_of_lucifer", "god_hand", "etherious"})
            assertTrue(winged.contains(must), must + " is a winged devil of the roster and must be covered: " + winged);
        // the Black Clover looks that share the generator (the forms of the Devil Avatar, Lucifero's king, Zagred, the Zogratis forms, Heath Grice, Morris)
        for (String n : new String[]{"pmx_bc_lucifero", "pmx_bc_lucifero_form2", "pmx_bc_devil_avatar", "pmx_bc_devil_avatar_form2", "pmx_bc_devil_avatar_megicula", "pmx_bc_zagred_form2",
                "pmx_bc_heath_grice_form2", "pmx_bc_morris_libardirt_form2", "pmx_bc_dante_zogratis_form2", "pmx_bc_vanica_zogratis_form2", "pmx_bc_zenon_zogratis_form2"}) {
            GeoModelData m = load(n);
            List<WingFrame.Group> raw = WingFrame.groups(m);
            assertFalse(raw.isEmpty(), n);
            for (WingFrame.Group g : raw) assertTrue(g.sweepsForward(), n + " " + g.root());
            for (WingFrame.Group g : WingFrame.groups(m.onBack())) assertNull(breaks(g), n + " " + g.root());
        }
        // the armour and form wings of the auras
        for (String n : new String[]{"dark_arm_wing_l", "dark_arm_wing_r", "flame_arm_wing_l", "flame_arm_wing_r", "lightning_arm_wing_l", "lightning_arm_wing_r", "water_arm_wing_l", "water_arm_wing_r",
                "spiritdive_arm_fire_wing_l", "spiritdive_arm_fire_wing_r", "spiritdive_arm_fire_wingbud_l", "spiritdive_arm_saint_wing_l", "spiritdive_arm_saint_wing_r", "nvl_arm_wings", "nvl_arm_wings2", "ndg_arm_wings", "nbk_arm_wings"}) {
            GeoModelData m = load(n);
            List<WingFrame.Group> raw = WingFrame.groups(m);
            assertFalse(raw.isEmpty(), n);
            for (WingFrame.Group g : raw) assertTrue(g.sweepsForward(), n + " " + g.root() + " dz " + g.dz());
            for (WingFrame.Group g : WingFrame.groups(m.onBack())) assertNull(breaks(g), n + " " + g.root());
        }
    }

    @Test
    void theWingsThatWereRightAreNotTouched() throws Exception {
        for (String n : new String[]{"pmx_lucifero", "pmx_lucifero_form2", "pmx_king", "pmx_zagred_legacy", "pmx_dg_imp", "pmx_dg_angelic_knight", "pmx_abyssal_solar_p3", "pmx_heath_grice_form2",
                "corundum_wings", "wing_back", "paladin_wings", "avatar_wing_0", "bwf_dragon_arm_wings", "sdj_fire_c_arm_wing_l", "sdj_wind_c_arm_wing_r", "ndg_arm_wings2", "nbk_arm_wings2", "soul_armour"}) {
            GeoModelData m = load(n);
            assertFalse(WingFrame.groups(m).isEmpty(), n);
            assertSame(m, m.onBack(), n + " already follows the convention: no copy, drawn as the file says");
        }
    }

    // ================================================================ the wings that are posed by code, and the meshes
    private static List<Double> numbers(String src, String regex) {
        List<Double> out = new ArrayList<>();
        Matcher m = Pattern.compile(regex).matcher(src);
        while (m.find()) out.add(Double.parseDouble(m.group(1)));
        return out;
    }

    @Test
    void theWegWingsOfADoubleMagicDevilHingeBehindTheTorsoAndSweepBack() throws Exception {
        // client/devil/DevilModels.Wings (the generic bat wings of the Weg layer, the Devil Union and every double-magic devil without a look of its own): hinge z 2.6, yaw -(0.2 + 0.3 spread) on the left
        String s = read(JAVA + "client/devil/DevilModels.java");
        assertTrue(s.contains("PartPose.offset(s * 2.2f, 2.2f, 2.6f)"), "hinge at the shoulder blade, 2.6 px behind the centre");
        assertTrue(s.contains("float yaw = -(0.2f + 0.3f * sp)"), "left wing: a negative yaw sweeps the tip back");
        assertTrue(s.contains("sd.wing().yRot = s * yaw"), "the right wing mirrors it");
        // and the layer puts it on the body part, not on the head or the arms
        String w = read(JAVA + "client/devil/WegLayer.java");
        assertTrue(w.contains("getParentModel().body.translateAndRotate(pose);") && w.contains("wings.render("), "the Weg wings hang on the body");
    }

    @Test
    void theDjinnModeArmourAndRelicWingsHingeBehindTheTorso() throws Exception {
        String dj = read(JAVA + "client/relics/djinn/DjinnArmorLayer.java");
        Matcher m = Pattern.compile("pose\\.translate\\(side \\* ([0-9.]+)f, ([0-9.]+)f, ([0-9.]+)f \\+ out").matcher(dj);
        assertTrue(m.find(), "the Djinn wing hinge");
        assertTrue(Double.parseDouble(m.group(3)) >= 2.0, "Djinn hinge z " + m.group(3));
        assertTrue(dj.contains("Axis.YP.rotationDegrees(-side * sweep)"), "Djinn: the left wing (side +1) turns by a negative yaw, the tip goes back");
        String wind = read(JAVA + "client/mode/ModeArmorWindV2.java");
        assertTrue(numbers(wind, "pose\\.translate\\(sg \\* [0-9.]+f, [0-9.]+f, ([0-9.]+)f\\)").stream().allMatch(z -> z >= 2.0) && wind.contains("sg * 1.8f, 2.5f, 2.7f"), "Wind v2 hinge z 2.7");
        String mode = read(JAVA + "client/mode/ModeArmorLayer.java");
        assertTrue(mode.contains("pose.translate(side * 1.6f, 2.0f, 2.3f);") && mode.contains("Axis.YP.rotationDegrees(-sweep)"), "mode armour wing cards: hinge z 2.3, swept back");
        // the worn relic library: every wing group of the wings_* models is hinged at z >= 2.4 and its mass is behind the hinge
        for (String n : new String[]{"wing_demon", "wings_dragon", "wings_feathered", "wings_moth", "wings_valk", "wings_wind"}) {
            Map<Integer, Double> hinge = new TreeMap<>(), sum = new TreeMap<>();
            Map<Integer, Integer> cnt = new TreeMap<>();
            for (String line : Files.readAllLines(Path.of("src/main/resources/assets/anime_multiverse/relic_worn/" + n + ".txt"))) {
                String[] v = line.trim().split("\\s+");
                if (v[0].equals("grp")) hinge.put(Integer.parseInt(v[1]), Double.parseDouble(v[6]));
                else if (v[0].equals("q")) {
                    int g = Integer.parseInt(v[1]);
                    for (int i = 0; i < 4; i++) { sum.merge(g, Double.parseDouble(v[4 + i * 8 + 2]), Double::sum); cnt.merge(g, 1, Integer::sum); }
                }
            }
            assertFalse(hinge.isEmpty(), n);
            for (Map.Entry<Integer, Double> e : hinge.entrySet()) {
                assertTrue(e.getValue() >= 2.0, n + " group " + e.getKey() + " hinge z " + e.getValue());
                assertTrue(sum.get(e.getKey()) / cnt.get(e.getKey()) >= e.getValue(), n + " group " + e.getKey() + " is behind its hinge");
            }
        }
    }

    @Test
    void theOldDevilMeshWingsAreBehindTheTorso() throws Exception {
        // entity_meshes/devil_<id>.txt (the figure the option devilPlayerModel=false brings back): mesh frame, y up, FRONT +z, so behind the torso is z < -2
        File[] fs = new File("src/main/resources/assets/anime_multiverse/entity_meshes").listFiles((d, n) -> n.startsWith("devil_") && n.endsWith(".txt") && !n.endsWith("_lod.txt"));
        assertNotNull(fs);
        int seen = 0;
        for (File f : fs) {
            Map<String, Double> pivot = new TreeMap<>();
            String cur = null;
            double sum = 0;
            int n = 0;
            for (String line : Files.readAllLines(f.toPath())) {
                String[] v = line.trim().split("\\s+");
                if (v[0].equals("pivot") && v[1].startsWith("wing")) pivot.put(v[1], Double.parseDouble(v[5]));
                else if (v[0].equals("part")) cur = v[1];
                else if (v[0].equals("qn") && cur != null && cur.startsWith("wing") && v[1].equals("0")) {
                    for (int i = 0; i < 4; i++) { sum += Double.parseDouble(v[2 + i * 8 + 2]); n++; }
                }
            }
            if (n == 0) continue;
            seen++;
            assertTrue(sum / n <= -2.0, f.getName() + " wing mass z " + sum / n + " must lie behind the torso (z <= -2 in the mesh frame)");
            for (double z : pivot.values()) assertTrue(z <= -2.0, f.getName() + " wing hinge z " + z);
        }
        assertTrue(seen >= 10, "the devil meshes with wings: " + seen);
    }

    // ================================================================ the option
    @Test
    void theOptionIsAClientConfigDefaultOnAndOffRestoresTheOldDrawing() throws Exception {
        String cfg = read(JAVA + "client/geo/WingsOnBackConfig.java");
        assertTrue(cfg.contains("ModConfig.Type.CLIENT") && cfg.contains("\"nusmp-wingsonback-client.toml\""), "a looks setting: CLIENT only");
        assertTrue(cfg.contains("b.push(\"visual\")") && cfg.contains(".define(\"wingsOnBack\", true)"), "visual.wingsOnBack, default true");
        assertTrue(cfg.contains("WingFrame.setOnBack(true)"), "a config that is not loaded yet gives the default");
        assertTrue(read(JAVA + "NUSMP.java").contains("WingsOnBackConfig.init(modEventBus, modContainer)"), "registered once from the client constructor");
        String draw = read(JAVA + "client/geo/GeoDraw.java");
        assertTrue(draw.contains("if (space == Space.PLAYER && WingFrame.isOnBack()) m = m.onBack();"), "off = the model as the file says");
        assertTrue(WingFrame.isOnBack(), "the default is on");
        // nothing but the client draw code reaches the helper (the server never loads it)
        try (var w = Files.walk(Path.of(JAVA))) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java"))::iterator) {
                String rel = Path.of(JAVA).relativize(p).toString().replace('\\', '/');
                if (rel.startsWith("client/") || rel.equals("NUSMP.java")) continue;
                assertFalse(Files.readString(p).contains("WingFrame") || Files.readString(p).contains("WingsOnBackConfig"), rel + " must not use the client wing helper");
            }
        }
    }

    @Test
    void theHotPathAllocatesNothingAndTheHelperHasNoGameClasses() throws Exception {
        String f = read(JAVA + "client/geo/WingFrame.java");
        assertFalse(f.contains("net.minecraft") || f.contains("com.mojang") || f.contains("neoforged"), "WingFrame is pure");
        String m = read(JAVA + "client/geo/GeoModelData.java");
        String onBack = m.substring(m.indexOf("public GeoModelData onBack()"), m.indexOf("void markBackChecked()"));
        assertFalse(onBack.contains(" new "), "onBack() allocates nothing after the first call (the copy is made once by WingFrame.fix)");
        String d = read(JAVA + "client/geo/GeoDraw.java");
        String bone = d.substring(d.indexOf("private static void bone("), d.indexOf("/** File rotation"));
        assertFalse(bone.contains(" new "), "the bone loop allocates nothing");
        assertTrue(Arrays.asList(f.split("\n")).stream().noneMatch(l -> l.contains("Minecraft.getInstance")));
    }
}
