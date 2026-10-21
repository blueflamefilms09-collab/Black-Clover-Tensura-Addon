package com.newuniverse.nusmp.visual.grip;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The self-test {@code item.weapon_grip} of the CI server smoke test (docs/weapon_grip.md): inside the booted mod, the grip table of assets/nusmp/weapon_grips.json parses,
 * every class it names exists, every upgraded weapon states its grip point, and a sample of weapons (the owner's Elsdocia, a katana, a dagger, a spear, a hammer, a staff, a bow, a
 * held item) finds a grip inside its mesh and lands it in the hand in all four hand contexts under the real display block of its model json. The same maths as the JUnit tests
 * ({@link GripFinder}, {@link GripMath}); touches no client class and no game object, so it runs on the dedicated server.
 */
public final class WeaponGripSelfTests {
    private WeaponGripSelfTests() {}

    private static boolean registered;

    /** Weapons whose meshes are read in full (the whole set is 190 MB; the JUnit data test covers every mesh). */
    private static final String[] SAMPLE = {"elsdocia", "demon_slasher_katana", "goblin_fang_dagger", "legendary_spear", "doom_hammer", "staff_of_moonlight", "legendary_bow",
            "glutton_chalice", "buster_sword", "djinn_weapon_baal"};

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("item.weapon_grip", WeaponGripSelfTests::run);
    }

    private static String text(String path) throws Exception {
        try (InputStream in = WeaponGripSelfTests.class.getResourceAsStream("/assets/nusmp/" + path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static float[] arr(JsonObject e, String k, float fill) {
        float[] v = {fill, fill, fill};
        JsonElement a = e.get(k);
        if (a != null && a.isJsonArray()) {
            JsonArray ja = a.getAsJsonArray();
            for (int i = 0; i < 3 && i < ja.size(); i++) v[i] = ja.get(i).getAsFloat();
        }
        return v;
    }

    /** vanilla's chain written out step by step: where a cube point lands relative to the hand under an entry (applied mirrored for the left hand). */
    private static double[] land(GripMath.Display d, boolean left, double px, double py, double pz) {
        double ry = left ? -d.ry() : d.ry(), rz = left ? -d.rz() : d.rz(), tx = (left ? -d.tx() : d.tx()) / 16.0;
        double x = (px - 0.5) * d.scale(), y = (py - 0.5) * d.scale(), z = (pz - 0.5) * d.scale();
        double c = Math.cos(Math.toRadians(rz)), s = Math.sin(Math.toRadians(rz)), x1 = x * c - y * s, y1 = x * s + y * c;
        c = Math.cos(Math.toRadians(ry));
        s = Math.sin(Math.toRadians(ry));
        double x2 = x1 * c + z * s, z2 = -x1 * s + z * c;
        c = Math.cos(Math.toRadians(d.rx()));
        s = Math.sin(Math.toRadians(d.rx()));
        return new double[]{x2 + tx, y1 * c - z2 * s + d.ty() / 16.0, y1 * s + z2 * c + d.tz() / 16.0};
    }

    private static GripMath.Display display(JsonObject model, String key) {
        JsonObject disp = model.getAsJsonObject("display");
        JsonElement e = disp == null ? null : disp.get(key);
        if (e == null || !e.isJsonObject()) return GripMath.Display.NONE;
        JsonObject o = e.getAsJsonObject();
        float[] r = arr(o, "rotation", 0), t = arr(o, "translation", 0), s = arr(o, "scale", 1);
        return new GripMath.Display(r[0], r[1], r[2], Math.max(-80f, Math.min(80f, t[0])), Math.max(-80f, Math.min(80f, t[1])), Math.max(-80f, Math.min(80f, t[2])), Math.max(-4f, Math.min(4f, s[0])));
    }

    private static SelfTests.Result run(MinecraftServer server) throws Exception {
        String gripsText = text("weapon_grips.json"), classesText = text("weapon_classes.json");
        if (gripsText == null || classesText == null) return SelfTests.Result.fail("weapon_grips.json or weapon_classes.json is missing");
        GripTable table = GripTable.parse(gripsText);
        JsonObject classes = JsonParser.parseString(classesText).getAsJsonObject();
        JsonObject rows = classes.getAsJsonObject("classes"), weapons = classes.getAsJsonObject("weapons");
        List<String> problems = new ArrayList<>();
        for (var e : table.classOf().entrySet()) if (!rows.has(e.getValue())) problems.add(e.getKey() + ": unknown grip class " + e.getValue());
        for (var e : table.fractions().entrySet()) if (!(e.getValue() >= 0f && e.getValue() <= 1f)) problems.add(e.getKey() + ": fraction " + e.getValue());
        for (String vu : new String[]{"elsdocia", "demon_slayer_sword", "blackrazor", "void_origin_katana", "starsplitter_hammer", "starforged_aeon_bow", "nekrathuls_pact_scythe"})
            if (table.point(vu) == null) problems.add(vu + ": an upgraded weapon needs its grip point in weapon_grips.json");
        if (!problems.isEmpty()) return SelfTests.Result.fail(String.join("; ", problems.subList(0, Math.min(4, problems.size()))));

        GripTable.Classes cls = new GripTable.Classes() {
            @Override public boolean isListed(String id) { return weapons.has(id) || weapons.has(GripTable.baseId(id)); }

            @Override public String className(String id) {
                if (weapons.has(id)) return weapons.get(id).getAsString();
                return weapons.has(GripTable.baseId(id)) ? weapons.get(GripTable.baseId(id)).getAsString() : "item";
            }
        };
        int landed = 0;
        for (String id : SAMPLE) {
            if (!table.hasGripClass(id, cls)) return SelfTests.Result.fail(id + ": no grip class");
            String mesh = text("weapon_meshes/" + id + ".txt");
            if (mesh == null) return SelfTests.Result.fail(id + ": no mesh");
            float[] place = new float[5];
            List<float[]> body = new ArrayList<>(), glow = new ArrayList<>();
            float line = Float.NaN;
            for (String l : mesh.split("\\R")) {
                String[] v = l.trim().split("\\s+");
                if (v[0].equals("place")) for (int i = 0; i < 5; i++) place[i] = Float.parseFloat(v[i + 1]);
                else if (v[0].equals("grip") && v.length > 1) line = Float.parseFloat(v[1]);
                else if ((v[0].equals("q") || v[0].equals("qn")) && (v[1].equals("0") || v[1].equals("2"))) {
                    int st = v[0].equals("q") ? 5 : 8;
                    for (int k = 0; k < 4; k++) {
                        int o = 2 + k * st;
                        (v[1].equals("0") ? body : glow).add(new float[]{Float.parseFloat(v[o]), Float.parseFloat(v[o + 1]), Float.parseFloat(v[o + 2])});
                    }
                }
            }
            if (body.isEmpty()) body = glow;
            String gc = table.gripClass(id, cls);
            if (!rows.has(gc)) return SelfTests.Result.fail(id + ": class " + gc + " is not in the class table");
            float[] explicit = null;                                                      // the old meshes: the finder (a point of the table is for the upgraded mesh)
            GripFinder.Grip g = GripState.find(body, gc, rows.getAsJsonObject(gc).get("grip").getAsFloat(), table.fraction(id), line, false, explicit);
            if (g == null) return SelfTests.Result.fail(id + ": no grip found");
            float[] box = GripState.box(body);
            if (g.x() < box[0] - 1e-3f || g.x() > box[1] + 1e-3f || g.y() < box[2] - 1e-3f || g.y() > box[3] + 1e-3f || g.z() < box[4] - 1e-3f || g.z() > box[5] + 1e-3f)
                return SelfTests.Result.fail(id + ": the grip is outside the mesh");
            String modelText = text("models/item/" + id + ".json");
            JsonObject model = modelText == null ? new JsonObject() : JsonParser.parseString(modelText).getAsJsonObject();
            GripMath.Display[] d = {display(model, "thirdperson_righthand"), display(model, "thirdperson_lefthand"), display(model, "firstperson_righthand"), display(model, "firstperson_lefthand")};
            GripState st = new GripState(g, d, box);
            for (int ctx = GripState.TP_RIGHT; ctx <= GripState.TP_LEFT; ctx++) {
                float[] s = st.shift(ctx, place[0], place[1], place[2], place[3], place[4]);
                float[] p = GripMath.cubePoint(place[0], place[1], place[2], place[3], place[4], g.x(), g.y(), g.z());
                double[] w = land(d[ctx], ctx == GripState.TP_LEFT, p[0] + s[0], p[1] + s[1], p[2] + s[2]);
                if (Math.sqrt(w[0] * w[0] + w[1] * w[1] + w[2] * w[2]) > 2e-4) return SelfTests.Result.fail(id + ": the hand misses the grip in context " + ctx);
                landed++;
            }
            for (int ctx = GripState.FP_RIGHT; ctx <= GripState.FP_LEFT; ctx++) {
                float[] s = st.shift(ctx, place[0], place[1], place[2], place[3], place[4]);
                boolean left = ctx == GripState.FP_LEFT;
                if (GripMath.inView(place, box, new float[3], d[ctx], left) && !GripMath.inView(place, box, new float[]{s[0], s[1], s[2]}, d[ctx], left))
                    return SelfTests.Result.fail(id + ": the first-person shift takes the weapon out of view");
                landed++;
            }
        }
        return SelfTests.Result.ok(SAMPLE.length + " weapons, " + landed + " hand placements land on the grip; " + table.classOf().size() + " classes, " + table.points().size() + " points, "
                + table.fractions().size() + " fractions in the table");
    }
}
