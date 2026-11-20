package com.newuniverse.nusmp.visual.sizes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.mesh.MeshPack;
import net.minecraft.server.MinecraftServer;

import java.io.InputStream;

/**
 * The self-test {@code visual.bow_size} of the CI server smoke test: inside the booted mod, every bow-class weapon of weapon_classes.json is in {@link BowSize}'s table and has its mesh, the factor
 * of the setting off is exactly 1 (the classic path), and a bow of every tier lands in its band in a third-person hand and under the camera cap in a first-person hand. Touches no client class
 * (BowSize is pure), so it runs on the dedicated server (docs/heroweapon/bow_size.md).
 */
public final class BowSizeSelfTests {
    private BowSizeSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("visual.bow_size", BowSizeSelfTests::run);
    }

    private static String text(String path) throws Exception {
        try (InputStream in = BowSizeSelfTests.class.getResourceAsStream("/assets/anime_multiverse/" + path)) {
            return in == null ? null : MeshPack.text(in.readAllBytes());
        }
    }

    private static SelfTests.Result run(MinecraftServer server) throws Exception {
        String classes = text("weapon_classes.json");
        if (classes == null) return SelfTests.Result.fail("weapon_classes.json is missing");
        int bows = 0;
        for (var e : JsonParser.parseString(classes).getAsJsonObject().getAsJsonObject("weapons").entrySet()) {
            if (!"bow".equals(e.getValue().getAsString())) continue;
            bows++;
            if (BowSize.tier(e.getKey()) == null) return SelfTests.Result.fail(e.getKey() + " is a bow-class weapon that is not in BowSize's table");
            if (text("weapon_meshes/" + e.getKey() + ".txt") == null) return SelfTests.Result.fail(e.getKey() + " has no weapon mesh");
        }
        if (bows < 21) return SelfTests.Result.fail("only " + bows + " bow-class weapons found");
        for (String id : BowSize.ids()) {
            BowSize.Tier t = BowSize.tier(id);
            float rest = 1.2f;                                                  // a vanilla-sized mesh: the worst case the factor has to lift
            float tp = BowSize.apparent(id, rest, rest, false, true, 1f), fp = BowSize.apparent(id, rest, rest, true, true, 1f);
            if (tp < t.min || tp > t.max) return SelfTests.Result.fail(id + " reads " + tp + " in a third-person hand, band " + t.min + " to " + t.max);
            if (fp > BowSize.FP_MAX_APPARENT + 1e-4f || BowSize.apparent(id, rest, rest, true, true, BowSize.SCALE_MAX) > BowSize.FP_MAX_APPARENT + 1e-4f) return SelfTests.Result.fail(id + " passes the first-person camera cap");
            if (BowSize.factor(false, 2f, BowSize.baseFactor(id, rest), rest, false) != 1f) return SelfTests.Result.fail(id + ": the classic path does not give back factor 1");
        }
        if (BowSize.baseFactor("magebane_longsword", 2f) != 0f) return SelfTests.Result.fail("a non-bow got a factor");
        return SelfTests.Result.ok(bows + " bow-class weapons, " + BowSize.ids().size() + " in the size table, bands and the classic path hold");
    }
}
