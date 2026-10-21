package com.newuniverse.nusmp.visual.grip;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.relics.item.WeaponClasses;
import com.newuniverse.nusmp.visual.genesis.VuMesh;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.111.0 (docs/weapon_grip.md): the grip table against every shipped weapon mesh. For every mesh of weapon_meshes, weapon_meshes_sz, weapon_meshes_vu and weapon_meshes_vu_sz:
 * <ul>
 *   <li>it has a grip class (weapon_classes.json lists it, or assets/nusmp/weapon_grips.json {@code classOf} does): a new weapon with only a keyword guess fails here;</li>
 *   <li>its grip point resolves and lies within the mesh's bounds;</li>
 *   <li>after the placement and the shift, the grip point is at the hand under the real display block of its model json, in all four hand contexts (third and first person,
 *       right and left hand), computed with an independent step-by-step chain: exactly in third person; in first person at the sword-hilt spot, or, where that would take the
 *       weapon out of view, no worse than the old placement;</li>
 *   <li>the finder gives what tools/weapon_grip/grip_golden.json says (the Python twin of the finder wrote it).</li>
 * </ul>
 * No Minecraft types: the meshes are read here the way WeaponRenderer and VuMesh read them.
 */
class GripTableTest {
    static final Path RES = Path.of("src", "main", "resources", "assets", "nusmp");
    static final Path GOLDEN = Path.of("tools", "weapon_grip", "grip_golden.json");
    static final String[][] DIRS = {{"weapon_meshes", ""}, {"weapon_meshes_sz", "sz/"}, {"weapon_meshes_vu", "vu/"}, {"weapon_meshes_vu_sz", "vu_sz/"},
            {"weapon_meshes/up", "up/"}, {"weapon_meshes_sz/up", "sz_up/"}};
    /** HandWrap.WRAPPED (joml is not on the test class path): worn on the hand, so no grip. */
    static final java.util.Set<String> WORN_ON_THE_HAND = java.util.Set.of("vassal_gauntlet", "vassal_claw");
    static final String[] CTX_KEYS = {"thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand"};

    /** One mesh as the game reads it. */
    record Mesh(String key, String id, boolean vu, float[] place, List<float[]> body, float gripLine, float[] explicit) {}

    /** The class table of assets/nusmp/weapon_classes.json (rows and the reviewed id to class map), the way WeaponRenderer reads it. */
    static WeaponClasses classes() throws IOException {
        JsonObject root = JsonParser.parseString(Files.readString(RES.resolve("weapon_classes.json"))).getAsJsonObject();
        java.util.Map<String, WeaponClasses.Cls> table = new java.util.HashMap<>();
        for (var e : root.getAsJsonObject("classes").entrySet()) {
            JsonObject c = e.getValue().getAsJsonObject();
            table.put(e.getKey(), new WeaponClasses.Cls(e.getKey(), c.get("min").getAsFloat(), c.get("max").getAsFloat(), c.get("head_min").getAsFloat(), c.get("grip").getAsFloat()));
        }
        java.util.Map<String, String> weapons = new java.util.HashMap<>();
        for (var e : root.getAsJsonObject("weapons").entrySet()) weapons.put(e.getKey(), e.getValue().getAsString());
        return new WeaponClasses(table, weapons);
    }

    static GripTable table() throws IOException {
        return GripTable.parse(Files.readString(RES.resolve("weapon_grips.json")));
    }

    static String idOf(String file) {
        String id = file.substring(0, file.length() - 4);
        for (String suffix : new String[]{"_lod", "_orbit"}) if (id.endsWith(suffix)) return id.substring(0, id.length() - suffix.length());
        return id;
    }

    static List<Mesh> meshes() throws IOException {
        List<Mesh> out = new ArrayList<>();
        for (String[] d : DIRS) {
            Path dir = RES.resolve(d[0]);
            if (!Files.isDirectory(dir)) continue;
            List<Path> files;
            try (Stream<Path> s = Files.list(dir)) {
                files = s.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
            }
            for (Path p : files) {
                String file = p.getFileName().toString();
                boolean vu = d[0].contains("_vu");
                out.add(vu ? readVu(d[1] + file.substring(0, file.length() - 4), idOf(file), p) : readOld(d[1] + file.substring(0, file.length() - 4), idOf(file), p));
            }
        }
        return out;
    }

    static Mesh readOld(String key, String id, Path p) throws IOException {
        float[] place = new float[5];
        float gripLine = Float.NaN;
        float[] explicit = null;
        List<float[]> l0 = new ArrayList<>(), l2 = new ArrayList<>();
        for (String line : Files.readAllLines(p)) {
            String[] v = line.trim().split("\\s+");
            switch (v[0]) {
                case "place" -> { for (int i = 0; i < 5; i++) place[i] = Float.parseFloat(v[i + 1]); }
                case "grip" -> gripLine = Float.parseFloat(v[1]);
                case "gripxyz" -> explicit = new float[]{Float.parseFloat(v[1]), Float.parseFloat(v[2]), Float.parseFloat(v[3])};
                case "q", "qn" -> {
                    int layer = Integer.parseInt(v[1]);
                    if (layer != 0 && layer != 2) continue;
                    int st = v[0].equals("q") ? 5 : 8;
                    for (int k = 0; k < 4; k++) {
                        int o = 2 + k * st;
                        (layer == 0 ? l0 : l2).add(new float[]{Float.parseFloat(v[o]), Float.parseFloat(v[o + 1]), Float.parseFloat(v[o + 2])});
                    }
                }
                default -> { }
            }
        }
        return new Mesh(key, id, false, place, l0.isEmpty() ? l2 : l0, gripLine, explicit);
    }

    static Mesh readVu(String key, String id, Path p) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(p)) {
            VuMesh m = VuMesh.parse(in);
            List<float[]> body = GripState.vuBody(m, true);
            if (body.isEmpty()) body = GripState.vuBody(m, false);
            return new Mesh(key, id, true, new float[]{m.ox, m.oy, m.angle, m.scale, m.ymin}, body, Float.NaN, null);
        }
    }

    /** The four hand display entries of the model json of an id (then its base id), NONE where missing. */
    static GripMath.Display[] displays(String id) throws IOException {
        GripMath.Display[] out = {GripMath.Display.NONE, GripMath.Display.NONE, GripMath.Display.NONE, GripMath.Display.NONE};
        for (String cand : new String[]{id, GripTable.baseId(id)}) {
            Path p = RES.resolve("models/item/" + cand + ".json");
            if (!Files.exists(p)) continue;
            JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
            JsonElement disp = o.get("display");
            if (disp != null && disp.isJsonObject()) {
                for (int i = 0; i < 4; i++) {
                    JsonElement e = disp.getAsJsonObject().get(CTX_KEYS[i]);
                    if (e == null || !e.isJsonObject()) continue;
                    out[i] = new GripMath.Display(arr(e, "rotation", 0)[0], arr(e, "rotation", 0)[1], arr(e, "rotation", 0)[2],
                            clamp(arr(e, "translation", 0)[0], 80f), clamp(arr(e, "translation", 0)[1], 80f), clamp(arr(e, "translation", 0)[2], 80f), Math.max(-4f, Math.min(4f, arr(e, "scale", 1)[0])));
                }
            }
            break;
        }
        return out;
    }

    static float clamp(float v, float m) { return Math.max(-m, Math.min(m, v)); }

    static float[] arr(JsonElement e, String k, float fill) {
        float[] v = {fill, fill, fill};
        JsonElement a = e.getAsJsonObject().get(k);
        if (a != null && a.isJsonArray()) for (int i = 0; i < 3 && i < a.getAsJsonArray().size(); i++) v[i] = a.getAsJsonArray().get(i).getAsFloat();
        return v;
    }

    @Test
    void everyWeaponMeshHasAGripClass() throws IOException {
        GripTable t = table();
        WeaponClasses classes = classes();
        List<String> missing = new ArrayList<>();
        for (Mesh m : meshes()) if (!t.hasGripClass(m.id(), classes)) missing.add(m.key());
        assertTrue(missing.isEmpty(), "weapon meshes without a grip class (list them in weapon_classes.json, or in assets/nusmp/weapon_grips.json classOf via tools/weapon_grip/grip_table.py): " + missing);
    }

    @Test
    void aNewWeaponWithOnlyAKeywordGuessFailsTheCheck() throws IOException {
        WeaponClasses classes = classes();
        GripTable t = table();
        assertFalse(t.hasGripClass("brand_new_sword_of_testing", classes), "a keyword guess (sword -> longsword) is not a reviewed grip class");
        assertFalse(GripTable.EMPTY.hasGripClass("another_new_axe", classes));
        assertTrue(t.hasGripClass("elsdocia", classes));
        assertTrue(t.hasGripClass("djinn_weapon_baal", classes), "the Djinn weapons are listed in the grip table");
        assertTrue(t.hasGripClass("bianca_pull2", classes), "a state mesh takes its base weapon's class");
    }

    @Test
    void everyUpgradedMeshStatesItsGripPoint() throws IOException {
        GripTable t = table();
        List<String> missing = new ArrayList<>();
        for (Mesh m : meshes()) if (m.vu() && !m.key().startsWith("vu_sz/") && t.point(m.id()) == null) missing.add(m.key());
        assertTrue(missing.isEmpty(), "an upgraded mesh needs a grip point in weapon_grips.json (its effect groups blur a profile): " + missing);
    }

    @Test
    void everyGripPointIsWithinTheMeshAndTheHandLandsOnItInEveryContext() throws IOException {
        GripTable t = table();
        WeaponClasses classes = classes();
        int checked = 0;
        for (Mesh m : meshes()) {
            String cls = t.gripClass(m.id(), classes);
            if ("shield".equals(cls) || com.newuniverse.nusmp.visual.shields.ShieldSet.isShieldMesh(m.id()) || WORN_ON_THE_HAND.contains(m.id())) continue;      // a shield sits on vanilla's frame, a glove has its own origin
            assertFalse(m.body().isEmpty(), m.key() + " has a body");
            WeaponClasses.Cls row = classes.named(cls);
            assertNotNull(row, m.key() + " class " + cls);
            float[] explicit = m.explicit() != null ? m.explicit() : m.vu() ? t.point(m.id()) : null;
            GripFinder.Grip g = GripState.find(m.body(), cls, row.grip(), t.fraction(m.id()), m.gripLine(), false, explicit);
            assertNotNull(g, m.key());
            float[] box = GripState.box(m.body());
            float eps = 1e-3f;
            assertTrue(g.x() >= box[0] - eps && g.x() <= box[1] + eps && g.y() >= box[2] - eps && g.y() <= box[3] + eps && g.z() >= box[4] - eps && g.z() <= box[5] + eps,
                    m.key() + ": grip (" + g.x() + ", " + g.y() + ", " + g.z() + ") is outside the mesh box " + java.util.Arrays.toString(box));
            assertTrue(g.fraction() >= -eps && g.fraction() <= 1f + eps, m.key() + " fraction " + g.fraction());
            if (GripFinder.HANDLE_MODE.contains(cls)) assertTrue(g.fraction() <= GripFinder.FRAC_MAX + eps, m.key() + ": a sword-like grip is in the lower half, got " + g.fraction());
            GripState st = new GripState(g, displays(m.id()), box);
            float[] place = m.place();
            for (int ctx = 0; ctx < 4; ctx++) {
                float[] s = st.shift(ctx, place[0], place[1], place[2], place[3], place[4]);
                float[] gc = GripMath.cubePoint(place[0], place[1], place[2], place[3], place[4], g.x(), g.y(), g.z());
                boolean left = ctx == GripState.TP_LEFT || ctx == GripState.FP_LEFT;
                GripMath.Display d = st.display(ctx);
                double[] w = GripMathTest.applyEntry(d, left, gc[0] + s[0], gc[1] + s[1], gc[2] + s[2]);
                if (ctx < GripState.FP_RIGHT) {
                    assertEquals(0.0, Math.sqrt(w[0] * w[0] + w[1] * w[1] + w[2] * w[2]), 2e-4, m.key() + " ctx " + ctx + ": the hand is on the grip point");
                } else {
                    // first person: the grip is at the sword-hilt spot, or (where that takes the weapon out of view) the weapon is as visible as before
                    float[] full = GripMath.shift(place, new float[]{g.x(), g.y(), g.z()}, GripMath.firstPersonTarget(d, left));
                    float share = full[0] == 0f && full[1] == 0f && full[2] == 0f ? 1f : (s[0] / full[0] + 0f);
                    boolean atSpot = Math.abs(s[0] - full[0]) < 1e-4f && Math.abs(s[1] - full[1]) < 1e-4f && Math.abs(s[2] - full[2]) < 1e-4f;
                    if (!atSpot) {
                        boolean oldOk = GripMath.inView(place, box, new float[3], d, left);
                        assertTrue(!oldOk || GripMath.inView(place, box, new float[]{s[0], s[1], s[2]}, d, left), m.key() + " ctx " + ctx + ": a reduced first-person shift keeps the weapon in view");
                    }
                    assertTrue(share == share, "no NaN");
                }
                checked++;
            }
        }
        assertTrue(checked > 1500, "the check ran on the shipped meshes: " + checked);
    }

    @Test
    void theFinderMatchesTheGoldenFileOfThePythonTwin() throws IOException {
        GripTable t = table();
        WeaponClasses classes = classes();
        JsonObject golden = JsonParser.parseString(Files.readString(GOLDEN)).getAsJsonObject();
        int compared = 0;
        List<String> off = new ArrayList<>();
        for (Mesh m : meshes()) {
            if (m.key().endsWith("_lod") || m.key().endsWith("_orbit")) continue;      // a level-of-detail mesh uses the point of its full mesh; an orbiting piece has no hand
            JsonElement e = golden.get(m.key());
            if (e == null) {
                off.add(m.key() + " (not in the golden file: run tools/weapon_grip/grip_table.py --write)");
                continue;
            }
            String cls = t.gripClass(m.id(), classes);
            WeaponClasses.Cls row = classes.named(cls);
            float[] explicit = m.explicit() != null ? m.explicit() : m.vu() ? t.point(m.id()) : null;
            GripFinder.Grip g = GripState.find(m.body(), cls, row.grip(), t.fraction(m.id()), m.gripLine(), false, explicit);
            JsonObject o = e.getAsJsonObject();
            if (!o.get("class").getAsString().equals(cls) || Math.abs(o.get("fraction").getAsFloat() - g.fraction()) > 0.006f || Math.abs(o.get("y").getAsFloat() - g.y()) > 0.02f * Math.max(1f, Math.abs(g.y()))) {
                off.add(m.key() + ": java " + cls + " " + g.fraction() + " / " + g.y() + " vs golden " + o);
            }
            compared++;
        }
        assertTrue(off.isEmpty(), "the Java finder and tools/weapon_grip/grip_lib.py disagree: " + off);
        assertTrue(compared > 400, "compared " + compared);
    }
}
