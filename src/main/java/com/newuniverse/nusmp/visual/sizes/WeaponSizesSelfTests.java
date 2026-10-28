package com.newuniverse.nusmp.visual.sizes;

import com.newuniverse.nusmp.mesh.MeshPack;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The self-test {@code visual.weapon_sizes} of the CI server smoke test: inside the booted mod, every dagger and katana of the reviewed class map ships its thickened mesh copies
 * (old, upgrade and Genesis meshes, with their level-of-detail meshes), the old files are still there, and each copy is inside the size ranges of assets/nusmp/weapon_sizes.json
 * (length, blade width and thickness in third and first person, a grip that fits a hand, no corner through the first-person camera). The same arithmetic as the JUnit data tests
 * ({@link WeaponSizeRule}); touches no client class, so it runs on the dedicated server.
 */
public final class WeaponSizesSelfTests {
    private WeaponSizesSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("visual.weapon_sizes", WeaponSizesSelfTests::run);
    }

    private static String text(String path) throws Exception {
        try (InputStream in = WeaponSizesSelfTests.class.getResourceAsStream("/assets/nusmp/" + path)) {
            return in == null ? null : MeshPack.text(in.readAllBytes());
        }
    }

    private static List<String> lines(String path) throws Exception {
        String t = text(path);
        return t == null ? null : Arrays.asList(t.split("\\R"));
    }

    private static double[] triple(JsonObject entry, String field) {
        JsonArray a = entry.getAsJsonArray(field);
        return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    private static SelfTests.Result run(MinecraftServer server) throws Exception {
        if (!"weapon_meshes_sz/a.txt".equals(WeaponSizeRule.szPath("weapon_meshes/a.txt")) || !"weapon_meshes_vu_sz/a.txt".equals(WeaponSizeRule.szPath("weapon_meshes_vu/a.txt"))
                || !"weapon_meshes_sz/up/a.txt".equals(WeaponSizeRule.szPath("weapon_meshes/up/a.txt")) || WeaponSizeRule.szPath("models/item/a.json") != null)
            return SelfTests.Result.fail("the copy path mapping is wrong");
        String rulesText = text("weapon_sizes.json"), classesText = text("weapon_classes.json");
        if (rulesText == null || classesText == null) return SelfTests.Result.fail("weapon_sizes.json or weapon_classes.json is missing");
        WeaponSizeRule.Rules rules = WeaponSizeRule.parseRules(rulesText);
        JsonObject weapons = JsonParser.parseString(classesText).getAsJsonObject().getAsJsonObject("weapons");
        String[][] dirs = {{"weapon_meshes", "weapon_meshes_sz"}, {"weapon_meshes/up", "weapon_meshes_sz/up"}, {"weapon_meshes_vu", "weapon_meshes_vu_sz"}};
        int ids = 0, meshes = 0;
        List<String> problems = new ArrayList<>();
        for (var e : weapons.entrySet()) {
            String id = e.getKey(), cls = e.getValue().getAsString();
            if ((!cls.equals("dagger") && !cls.equals("katana")) || rules.excluded.contains(id)) continue;
            WeaponSizeRule.Limits lim = rules.limits.get(cls);
            String model = text("models/item/" + id + ".json");
            if (model == null) return SelfTests.Result.fail(id + ": no model json");
            JsonObject display = JsonParser.parseString(model).getAsJsonObject().getAsJsonObject("display");
            double s3 = display.getAsJsonObject("thirdperson_righthand").getAsJsonArray("scale").get(0).getAsDouble();
            JsonObject fp = display.getAsJsonObject("firstperson_righthand");
            double s1 = fp.getAsJsonArray("scale").get(0).getAsDouble();
            int found = 0;
            for (String[] d : dirs) {
                List<String> old = lines(d[0] + "/" + id + ".txt");
                if (old == null) continue;
                List<String> sz = lines(d[1] + "/" + id + ".txt");
                if (sz == null) return SelfTests.Result.fail(id + ": no thickened copy in " + d[1]);
                if (lines(d[0] + "/" + id + "_lod.txt") != null && lines(d[1] + "/" + id + "_lod.txt") == null) return SelfTests.Result.fail(id + ": no thickened level-of-detail copy in " + d[1]);
                problems.addAll(WeaponSizeRule.problems(id + " [" + d[0] + "]", WeaponSizeRule.measure(sz), lim, s3, s1, triple(fp, "rotation"), triple(fp, "translation")));
                WeaponSizeRule.Measure mo = WeaponSizeRule.measure(old), mn = WeaponSizeRule.measure(sz);
                if (mn.length < mo.length - 1e-6 || mn.bladeWidth < mo.bladeWidth - 1e-9) problems.add(id + " [" + d[0] + "]: the copy is shorter or thinner than the old mesh");
                found++;
                meshes++;
            }
            if (found == 0) return SelfTests.Result.fail(id + ": no mesh at all");
            ids++;
        }
        if (ids < 24) return SelfTests.Result.fail("only " + ids + " daggers and katanas found");
        if (!problems.isEmpty()) return SelfTests.Result.fail(problems.size() + " size problems, first: " + problems.get(0));
        return SelfTests.Result.ok(ids + " daggers and katanas, " + meshes + " thickened meshes, all inside the size ranges");
    }
}
