package com.newuniverse.nusmp.client.playermodel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.playermodel.GsArmorFit.Box;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The armour fit of the Goku and Sinbad looks (docs/boss_redesign_goku_sinbad.md, "Armour overlap"), checked with no game. Part one: no outer-layer cube of any {@code pmx_gs_*} geo file (main, fine, fx,
 * hair, hair_fine) sits inside the body cube of its bone or shares a face plane with the body (z-fight), and the skin's second layer (hat, jacket, sleeves, pants), where a cube may sit
 * inside the +0.25 / +0.5 envelope (the kit's hair shell floats 0.05 off the skull on purpose, dvlib.HG), is empty in every look so it hides nothing; the checker itself is proved on made-up cubes first, so a
 * pass means something. Part two: the Djinn Equip pieces the Sinbad looks replace are only ever pieces the look really has geometry for, are real pieces, keep the wings, tail, halo and gauntlets
 * of the player's set, and the wiring (option, mask, old behaviour with mask 0) is in the source.
 */
class GsArmorFitTest {
    private static final String GEO = "src/main/resources/assets/anime_multiverse/geo/entity/";
    private static final String DJINN = "src/main/java/com/newuniverse/nusmp/client/relics/djinn/";
    private static final String[] BONES = {"x_head", "x_body", "x_arm_r", "x_arm_l", "x_leg_r", "x_leg_l"};

    private static String read(String path) throws Exception { return Files.readString(Path.of(path)); }

    /** The box of a Bedrock cube (rotation taken into account through its eight corners). */
    static Box box(JsonObject c) {
        double[] o = d3(c.getAsJsonArray("origin")), s = d3(c.getAsJsonArray("size"));
        double[] lo = o.clone(), hi = {o[0] + s[0], o[1] + s[1], o[2] + s[2]};
        if (c.has("rotation")) {
            double[] p = d3(c.getAsJsonArray("pivot")), r = d3(c.getAsJsonArray("rotation"));
            double rx = Math.toRadians(r[0]), ry = Math.toRadians(r[1]), rz = Math.toRadians(r[2]);
            double[] mn = {1e9, 1e9, 1e9}, mx = {-1e9, -1e9, -1e9};
            for (int i = 0; i < 8; i++) {
                double x = ((i & 1) == 0 ? lo[0] : hi[0]) - p[0], y = ((i & 2) == 0 ? lo[1] : hi[1]) - p[1], z = ((i & 4) == 0 ? lo[2] : hi[2]) - p[2];
                double x1 = x * Math.cos(rz) - y * Math.sin(rz), y1 = x * Math.sin(rz) + y * Math.cos(rz);
                double x2 = x1 * Math.cos(ry) + z * Math.sin(ry), z2 = -x1 * Math.sin(ry) + z * Math.cos(ry);
                double y3 = y1 * Math.cos(rx) - z2 * Math.sin(rx), z3 = y1 * Math.sin(rx) + z2 * Math.cos(rx);
                double[] q = {x2 + p[0], y3 + p[1], z3 + p[2]};
                for (int k = 0; k < 3; k++) { mn[k] = Math.min(mn[k], q[k]); mx[k] = Math.max(mx[k], q[k]); }
            }
            lo = mn; hi = mx;
        }
        return new Box(lo[0], lo[1], lo[2], hi[0], hi[1], hi[2]);
    }

    private static double[] d3(JsonArray a) { return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()}; }

    private static List<File> geoFiles() {
        File[] fs = new File(GEO).listFiles((d, n) -> n.startsWith("pmx_gs_") && n.endsWith(".geo.json"));
        assertNotNull(fs, "the geo folder is there");
        Arrays.sort(fs);
        return List.of(fs);
    }

    private static JsonArray bones(File f) throws Exception {
        return JsonParser.parseString(Files.readString(f.toPath())).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
    }

    // ------------------------------------------------------------------------------------------------ part one: the cubes

    @Test
    void theCheckerCatchesWhatItIsMeantToCatch() {
        Box body = GsArmorFit.base("x_body", false);
        assertTrue(GsArmorFit.sitsInside(new Box(-3, 14, -1, 3, 20, 1), body), "a cube buried in the body");
        assertTrue(GsArmorFit.sitsInside(new Box(-4, 12, -2, 4, 24, 2), body), "the body itself");
        assertTrue(GsArmorFit.sitsInside(new Box(-4.2, 12.1, -2.2, 4.2, 23.9, 2.2), GsArmorFit.inflated(body, GsArmorFit.SKIN_LAYER)), "buried in the jacket layer");
        assertFalse(GsArmorFit.sitsInside(new Box(-4.7, 23.2, -3.2, 4.7, 24.4, 3.2), body), "a collar that wraps the body");
        assertTrue(GsArmorFit.zFights(new Box(-3, 14, -2, 3, 20, 3), body, 0.02), "a front face on the body's front plane while crossing it");
        assertTrue(GsArmorFit.zFights(new Box(-4.01, 14, -3, 4.01, 20, 3), body, 0.02), "a side face 0.01 off the body's side plane");
        assertFalse(GsArmorFit.zFights(new Box(-3, 14, -4, 3, 20, -2), body, 0.02), "a cube that only touches the front from outside");
        assertFalse(GsArmorFit.zFights(new Box(-4.3, 14, -3, 4.3, 20, 3), body, 0.02), "a wrap 0.3 clear of the side planes");
        assertFalse(GsArmorFit.zFights(new Box(-3, 14, -2.5, 3, 20, -2.3), body, 0.02), "a plate in front, clear of the body");
    }

    @Test
    void noOuterLayerCubeOfAGokuOrSinbadLookSitsInsideItsBody() throws Exception {
        List<String> bad = new ArrayList<>();
        int cubes = 0, files = 0;
        for (File f : geoFiles()) {
            files++;
            for (JsonElement be : bones(f)) {
                JsonObject b = be.getAsJsonObject();
                Box base = GsArmorFit.base(b.get("name").getAsString(), false);
                if (base == null || !b.has("cubes")) continue;
                for (JsonElement ce : b.getAsJsonArray("cubes")) {
                    Box c = box(ce.getAsJsonObject());
                    cubes++;
                    if (GsArmorFit.sitsInside(c, base)) bad.add(f.getName() + " " + b.get("name").getAsString() + " buried in the body: " + c);
                    else if (GsArmorFit.zFights(c, base, 0.02)) bad.add(f.getName() + " " + b.get("name").getAsString() + " shares a face plane with the body: " + c);
                }
            }
        }
        assertTrue(files >= 16 * 3, "the geo files of the 16 looks are read (" + files + ")");
        assertTrue(cubes > 900, "and their cubes (" + cubes + ")");
        assertTrue(bad.isEmpty(), "outer layers that clip the body: " + bad);
    }

    @Test
    void theSecondSkinLayerOfEveryLookIsEmptySoNothingHidesTheCubes() throws Exception {
        for (String id : GsLooks.GOKU) assertEmptyOverlay(id);
        for (String id : GsLooks.SINBAD) assertEmptyOverlay(id);
    }

    private static void assertEmptyOverlay(String id) throws Exception {
        java.awt.image.BufferedImage im = javax.imageio.ImageIO.read(new File("src/main/resources/assets/anime_multiverse/textures/entity/playermodel/" + id + ".png"));
        int s = im.getWidth() / 64;
        int[][] regions = {{32, 0, 64, 16}, {16, 32, 40, 48}, {40, 32, 56, 48}, {48, 48, 64, 64}, {0, 32, 16, 48}, {0, 48, 16, 64}};   // hat, jacket, the two sleeves, the two pants
        for (int[] r : regions)
            for (int y = r[1] * s; y < r[3] * s; y++)
                for (int x = r[0] * s; x < r[2] * s; x++)
                    assertFalse(im.getRGB(x, y) >>> 24 > 8, id + ": a painted pixel of the second skin layer at " + x + "," + y);
    }

    // ------------------------------------------------------------------------------------------------ part two: the armour pieces

    private static Set<String> pieceNames() throws Exception {
        String src = read(DJINN + "DjinnMeshes.java");
        Matcher m = Pattern.compile("public enum Piece \\{(.*?)public String file", Pattern.DOTALL).matcher(src);
        assertTrue(m.find(), "the Piece enum is in DjinnMeshes");
        String body = m.group(1).replaceAll("/\\*\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
        java.util.HashSet<String> out = new java.util.HashSet<>();
        for (String t : body.split("[,;]")) {
            String n = t.trim();
            if (n.matches("[A-Z][A-Z0-9_]*")) out.add(n.toLowerCase());
        }
        return out;
    }

    @Test
    void everyHiddenPieceIsARealPieceAndTheSetKeepsItsBackPieces() throws Exception {
        Set<String> real = pieceNames();
        assertTrue(real.containsAll(Set.of("helm", "chest", "wing_in_l", "tail_seg", "halo", "gauntlet_r", "weapon")), "the enum was read: " + real);
        assertTrue(real.containsAll(GsArmorFit.hideable()), "hideable names are real pieces");
        assertTrue(GsArmorFit.hiddenPieces(0).isEmpty(), "no djinn, nothing hidden");
        assertTrue(GsArmorFit.hiddenPieces(99).isEmpty() && GsArmorFit.hiddenPieces(-1).isEmpty());
        for (int i = 1; i < GsLooks.SINBAD.size(); i++) {
            Set<String> h = GsArmorFit.hiddenPieces(i);
            assertTrue(real.containsAll(h), GsLooks.SINBAD.get(i) + " hides only real pieces: " + h);
            assertTrue(h.containsAll(Set.of("chest", "belt", "helm", "vambrace_l", "vambrace_r", "greave_l", "greave_r")), GsLooks.SINBAD.get(i) + " crown, collar, belt, cuffs, sandals");
            for (String keep : List.of("gauntlet_l", "gauntlet_r", "wing_in_l", "wing_out_r", "tail_seg", "tail_tip", "halo", "backdrop", "mantle", "weapon", "ears"))
                assertFalse(h.contains(keep), GsLooks.SINBAD.get(i) + " keeps " + keep);
        }
        assertFalse(GsArmorFit.hiddenPieces(3).contains("pauldron_l"), "Zepar's look has no shoulder plates");
        assertFalse(GsArmorFit.hiddenPieces(7).contains("pauldron_r"), "Furfur's look has no shoulder plates");
        assertTrue(GsArmorFit.hiddenPieces(1).contains("pauldron_l") && GsArmorFit.hiddenPieces(1).contains("horn_l"), "Baal: shoulders and antlers");
        assertFalse(GsArmorFit.hiddenPieces(2).contains("horn_l"), "only Baal's antlers stand in for the horns");
    }

    @Test
    void aLookOnlyReplacesWhatItReallyWears() throws Exception {
        for (int i = 1; i < GsLooks.SINBAD.size(); i++) {
            File f = new File(GEO + "pmx_" + GsLooks.SINBAD.get(i) + ".geo.json");
            assertTrue(f.isFile(), f.getPath());
            java.util.HashMap<String, Integer> cubesOf = new java.util.HashMap<>();
            for (JsonElement be : bones(f)) {
                JsonObject b = be.getAsJsonObject();
                if (b.has("cubes")) cubesOf.merge(b.get("name").getAsString(), b.getAsJsonArray("cubes").size(), Integer::sum);
            }
            for (String piece : GsArmorFit.hiddenPieces(i)) {
                String bone = GsArmorFit.boneOf(piece);
                assertNotNull(bone, piece);
                assertTrue(cubesOf.getOrDefault(bone, 0) >= 2, GsLooks.SINBAD.get(i) + " hides " + piece + " but its look has next to nothing on " + bone);
            }
        }
    }

    @Test
    void theWiringIsInTheSourceAndTheOldBehaviourIsOneSwitchAway() throws Exception {
        String layer = read(DJINN + "DjinnArmorLayer.java"), boss = read(DJINN + "DjinnBossLayer.java"), opt = read("src/main/java/com/newuniverse/nusmp/client/playermodel/PlayerModelOptions.java");
        assertTrue(opt.contains("\"bossArmorFit\", true") && opt.contains("bossArmorFit()"), "a client option, default on");
        assertTrue(layer.contains("int hide) {") && layer.contains("draw(pose, buffers, light, p, m, s, partial, 0);"), "the player's own call keeps mask 0: every piece stays");
        assertTrue(layer.contains("hideMask & (1 << piece.ordinal())"), "the mask is read per piece");
        assertTrue(boss.contains("!PlayerModelOptions.bossArmorFit()") && boss.contains("hideMask(s.idx != 0 ? s.idx : s.prevIdx)"), "option off = mask 0 = the old drawing");
        assertFalse(read(DJINN + "DjinnArmorLayer.java").contains("Minecraft.getInstance().options"), "nothing new reaches the client options from the layer");
    }
}
