package com.newuniverse.nusmp.requip;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a piece of an armour is drawn on the model part the table says (head, body, an arm, a leg) and, if it moves, plays the clip the table says; a name that resolves to no part, a model without its
 * root bone, a clip naming a bone the model does not have, or a heavy piece that forges before a light one would only show in game as a missing or odd piece. This test checks, from the files:
 * every piece name resolves to the part it belongs to (and the right side of it), every model has its {@code root} bone and every bone is linked, every animated piece has an animation file whose clip
 * is {@code idle} and names real bones, a heavier piece never forges before a lighter one, and the solid plates stand at least 2 percent off the skin of the part they are drawn on (so the skin never
 * shows through and the plate never z-fights with it), for the wide arm as well as the slim one (the arm pieces are narrowed to 80 percent about the arm's pivot on a slim arm).
 */
class ErzaArmourPiecesTest {
    private static final File ASSETS = new File("src/main/resources/assets/nusmp");

    private static JsonObject json(String rel) throws Exception {
        try (Reader r = new FileReader(new File(ASSETS, rel))) { return JsonParser.parseReader(r).getAsJsonObject(); }
    }

    /** The part a piece belongs to, from its name alone. */
    static ArmourSets.Anchor expected(String name) {
        return switch (name) {
            case "helm", "halo", "ribbon" -> ArmourSets.Anchor.HEAD;
            case "chest", "backplate", "gorget", "belt", "skirt", "cape", "tail", "backdrop", "wing_r", "wing_l" -> ArmourSets.Anchor.BODY;
            default -> {
                if (name.endsWith("_r") && (name.startsWith("pauldron") || name.startsWith("vambrace") || name.startsWith("gauntlet"))) yield ArmourSets.Anchor.ARM_R;
                if (name.endsWith("_l") && (name.startsWith("pauldron") || name.startsWith("vambrace") || name.startsWith("gauntlet"))) yield ArmourSets.Anchor.ARM_L;
                if (name.endsWith("_r") && (name.startsWith("cuisse") || name.startsWith("greave") || name.startsWith("sabaton"))) yield ArmourSets.Anchor.LEG_R;
                if (name.endsWith("_l") && (name.startsWith("cuisse") || name.startsWith("greave") || name.startsWith("sabaton"))) yield ArmourSets.Anchor.LEG_L;
                yield null;
            }
        };
    }

    @Test
    void everyPieceNameResolvesToItsPart() {
        for (Armour a : Armour.values()) for (ArmourSets.Piece p : ArmourSets.of(a)) {
            ArmourSets.Anchor want = expected(p.name());
            assertNotNull(want, a.id() + "/" + p.name() + " resolves to no model part");
            assertEquals(want, p.anchor(), a.id() + "/" + p.name() + " is anchored on the wrong part");
            assertEquals(want, ArmourSets.anchorOfName(p.name()), a.id() + "/" + p.name() + ": the game's own name rule disagrees");
        }
    }

    @Test
    void everyModelHasItsRootBoneAndLinkedBonesAndEveryClipNamesRealBones() throws Exception {
        for (Armour a : Armour.values()) for (ArmourSets.Piece p : ArmourSets.of(a)) {
            String id = ArmourSets.modelId(a, p);
            JsonObject g = json("geo/entity/" + id + ".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            Set<String> bones = new HashSet<>();
            for (JsonElement b : g.getAsJsonArray("bones")) bones.add(b.getAsJsonObject().get("name").getAsString());
            assertTrue(bones.contains("root"), id + " has no root bone");
            for (JsonElement b : g.getAsJsonArray("bones")) {
                JsonObject bo = b.getAsJsonObject();
                if (bo.has("parent")) assertTrue(bones.contains(bo.get("parent").getAsString()), id + ": bone " + bo.get("name") + " has an unknown parent");
            }
            File anim = new File(ASSETS, "animations/entity/" + id + ".animation.json");
            if (p.clip() == null) continue;
            assertTrue(anim.isFile(), id + " is animated but has no animation file");
            JsonObject clips = json("animations/entity/" + id + ".animation.json").getAsJsonObject("animations");
            assertTrue(clips.has(p.clip()), id + " has no clip " + p.clip());
            for (Map.Entry<String, JsonElement> e : clips.getAsJsonObject(p.clip()).getAsJsonObject("bones").entrySet())
                assertTrue(bones.contains(e.getKey()), id + ": the clip moves the unknown bone " + e.getKey());
            assertTrue(clips.getAsJsonObject(p.clip()).get("loop").getAsBoolean(), id + ": the idle clip must loop");
        }
    }

    @Test
    void aHeavierPieceNeverForgesBeforeALighterOne() {
        for (Armour a : Armour.values()) {
            for (ArmourSets.Piece x : ArmourSets.of(a)) for (ArmourSets.Piece y : ArmourSets.of(a)) {
                if (ArmourSets.weightOf(x.name()) > ArmourSets.weightOf(y.name()))
                    assertTrue(x.delay() >= y.delay(), a.id() + ": " + x.name() + " (weight " + ArmourSets.weightOf(x.name()) + ", delay " + x.delay() + ") forges before the lighter " + y.name() + " (" + y.delay() + ")");
            }
            int heaviestLast = 0;
            float last = ArmourSets.maxDelay(a);
            for (ArmourSets.Piece p : ArmourSets.of(a)) if (p.delay() == last) heaviestLast = Math.max(heaviestLast, ArmourSets.weightOf(p.name()));
            assertTrue(heaviestLast >= 5, a.id() + ": the last piece to forge is a light one");
        }
    }

    /** Bounding box of a model's boxes in the file frame, with the cube rotations applied (Rz . Ry . Rx about the cube's pivot). */
    static double[] bounds(JsonObject geo) {
        double[] b = {1e9, 1e9, 1e9, -1e9, -1e9, -1e9};
        JsonObject g = geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        for (JsonElement be : g.getAsJsonArray("bones")) for (JsonElement ce : be.getAsJsonObject().getAsJsonArray("cubes")) {
            JsonObject c = ce.getAsJsonObject();
            JsonArray o = c.getAsJsonArray("origin"), s = c.getAsJsonArray("size");
            double[] lo = {o.get(0).getAsDouble(), o.get(1).getAsDouble(), o.get(2).getAsDouble()};
            double[] sz = {s.get(0).getAsDouble(), s.get(1).getAsDouble(), s.get(2).getAsDouble()};
            double[][] m = null;
            double[] pv = null;
            if (c.has("rotation")) {
                JsonArray r = c.getAsJsonArray("rotation"), pp = c.getAsJsonArray("pivot");
                m = rot(Math.toRadians(r.get(0).getAsDouble()), Math.toRadians(r.get(1).getAsDouble()), Math.toRadians(r.get(2).getAsDouble()));
                pv = new double[]{pp.get(0).getAsDouble(), pp.get(1).getAsDouble(), pp.get(2).getAsDouble()};
            }
            for (int i = 0; i < 8; i++) {
                double[] p = {lo[0] + ((i & 1) != 0 ? sz[0] : 0), lo[1] + ((i & 2) != 0 ? sz[1] : 0), lo[2] + ((i & 4) != 0 ? sz[2] : 0)};
                if (m != null) {
                    double[] d = {p[0] - pv[0], p[1] - pv[1], p[2] - pv[2]};
                    p = new double[]{pv[0] + m[0][0] * d[0] + m[0][1] * d[1] + m[0][2] * d[2], pv[1] + m[1][0] * d[0] + m[1][1] * d[1] + m[1][2] * d[2], pv[2] + m[2][0] * d[0] + m[2][1] * d[1] + m[2][2] * d[2]};
                }
                for (int k = 0; k < 3; k++) { b[k] = Math.min(b[k], p[k]); b[k + 3] = Math.max(b[k + 3], p[k]); }
            }
        }
        return b;
    }

    private static double[][] rot(double a, double b, double c) {
        double ca = Math.cos(a), sa = Math.sin(a), cb = Math.cos(b), sb = Math.sin(b), cc = Math.cos(c), sc = Math.sin(c);
        return new double[][]{{cc * cb, cc * sb * sa - sc * ca, cc * sb * ca + sc * sa}, {sc * cb, sc * sb * sa + cc * ca, sc * sb * ca - cc * sa}, {-sb, cb * sa, cb * ca}};
    }

    @Test
    void theSolidPlatesStandAtLeastTwoPercentOffTheSkin() throws Exception {
        for (Armour a : Armour.values()) {
            // the trunk is a front plate (chest) and a back plate (backplate): together they must stand 2 percent off the body's front and back, each alone 2 percent wider than the body's 8
            double[] front = bounds(json("geo/entity/" + a.geoKey() + "_arm_chest.geo.json")), back = bounds(json("geo/entity/" + a.geoKey() + "_arm_backplate.geo.json"));
            assertTrue(Math.min(front[2], back[2]) <= -2 * 1.02 && Math.max(front[5], back[5]) >= 2 * 1.02, a.id() + ": the chest and back plates do not stand off the body's front and back");
            assertTrue(front[3] - front[0] >= 8 * 1.02 && back[3] - back[0] >= 8 * 1.02, a.id() + ": the chest or the back plate is narrower than the body");
            for (String piece : new String[]{"helm", "belt", "gorget"}) {
                double[] b = bounds(json("geo/entity/" + a.geoKey() + "_arm_" + piece + ".geo.json"));
                assertTrue(b[3] - b[0] >= 8 * 1.02, a.id() + "/" + piece + " is " + (b[3] - b[0]) + " wide, the skin is 8");
                double depth = piece.equals("helm") ? 8 : 4;
                assertTrue(b[5] - b[2] >= depth * 1.02, a.id() + "/" + piece + " is " + (b[5] - b[2]) + " deep, the skin is " + depth);
            }
            for (String piece : new String[]{"pauldron", "vambrace", "gauntlet"}) {
                for (String side : new String[]{"r", "l"}) {
                    double[] b = bounds(json("geo/entity/" + a.geoKey() + "_arm_" + piece + "_" + side + ".geo.json"));
                    // the wide arm is 4 wide at x -8 .. -4 (right); the slim arm (the piece narrowed to 80 percent about x = -5) 3 wide: both must be covered with a margin
                    double outer = side.equals("r") ? -b[0] : b[3];
                    assertTrue(outer >= 8 * 1.02, a.id() + "/" + piece + "_" + side + " reaches only " + outer + " from the body's middle, the wide arm's outer face is at 8");
                    assertTrue(b[5] - b[2] >= 4 * 1.02, a.id() + "/" + piece + "_" + side + " is " + (b[5] - b[2]) + " deep, the arm is 4");
                    double slimOuter = side.equals("r") ? -(-5 + 0.8 * (b[0] + 5)) : (5 + 0.8 * (b[3] - 5));
                    assertTrue(slimOuter >= 7 * 1.02, a.id() + "/" + piece + "_" + side + " on a slim arm reaches " + slimOuter + ", the slim arm's outer face is at 7");
                }
            }
            for (String piece : new String[]{"cuisse", "greave", "sabaton"}) {
                for (String side : new String[]{"r", "l"}) {
                    double[] b = bounds(json("geo/entity/" + a.geoKey() + "_arm_" + piece + "_" + side + ".geo.json"));
                    double outer = side.equals("r") ? -b[0] : b[3];
                    assertTrue(outer >= 3.9 * 1.02, a.id() + "/" + piece + "_" + side + " reaches only " + outer + ", the leg's outer face is at 3.9");
                    assertTrue(b[5] - b[2] >= 4 * 1.02, a.id() + "/" + piece + "_" + side + " is " + (b[5] - b[2]) + " deep, the leg is 4");
                }
            }
        }
    }
}
