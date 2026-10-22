package com.newuniverse.nusmp.prop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.particle.fx.FxCatalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * CI self-test {@code magic.recomb_anime} (server smoke test): the anime look of Recombination Magic is complete and the older look is still there. It reads only resources and pure rules, so it
 * runs on a dedicated server (no client class is touched): the four new models (bones linked, cube caps, per-face uvs inside the texture, texture and glow map of the declared size, every clip the
 * painter asks for), the four older models still shipped, the particle ids the look spawns exist in the particle catalog, the pure rules answer sanely (the clip table, the stride, the footfalls),
 * and the four prop behaviours are still registered.
 */
public final class RecombAnimeSelfTest {
    private RecombAnimeSelfTest() {}

    public static void register() {
        SelfTests.register("magic.recomb_anime", RecombAnimeSelfTest::run);
    }

    private static JsonObject json(String path) throws IOException {
        try (InputStream in = RecombAnimeSelfTest.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** Width and height of a PNG from its IHDR chunk (no image library). */
    private static int[] pngSize(String path) throws IOException {
        try (InputStream in = RecombAnimeSelfTest.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing resource " + path);
            byte[] h = in.readNBytes(24);
            if (h.length < 24 || h[1] != 'P' || h[2] != 'N' || h[3] != 'G') throw new IOException("not a png: " + path);
            int w = ((h[16] & 255) << 24) | ((h[17] & 255) << 16) | ((h[18] & 255) << 8) | (h[19] & 255);
            int hh = ((h[20] & 255) << 24) | ((h[21] & 255) << 16) | ((h[22] & 255) << 8) | (h[23] & 255);
            return new int[]{w, hh};
        }
    }

    private static SelfTests.Result run(net.minecraft.server.MinecraftServer server) {
        try {
            for (int m = 0; m < RecombAnimeRules.MODELS.length; m++) {
                String id = "recombanime_" + RecombAnimeRules.MODELS[m];
                JsonObject geo = json("/assets/nusmp/geo/entity/" + id + ".geo.json").getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                JsonObject desc = geo.getAsJsonObject("description");
                int tw = desc.get("texture_width").getAsInt(), th = desc.get("texture_height").getAsInt();
                int cubes = 0;
                java.util.Set<String> bones = new java.util.HashSet<>();
                for (var b : geo.getAsJsonArray("bones")) bones.add(b.getAsJsonObject().get("name").getAsString());
                for (var be : geo.getAsJsonArray("bones")) {
                    JsonObject b = be.getAsJsonObject();
                    if (b.has("parent") && !bones.contains(b.get("parent").getAsString())) return SelfTests.Result.fail(id + ": bone " + b.get("name").getAsString() + " has an unknown parent");
                    if (!b.has("cubes")) continue;
                    for (var ce : b.getAsJsonArray("cubes")) {
                        cubes++;
                        JsonObject uv = ce.getAsJsonObject().get("uv").isJsonObject() ? ce.getAsJsonObject().getAsJsonObject("uv") : null;
                        if (uv == null) continue;
                        for (var f : uv.entrySet()) {
                            var u = f.getValue().getAsJsonObject().getAsJsonArray("uv");
                            var s = f.getValue().getAsJsonObject().getAsJsonArray("uv_size");
                            if (u.get(0).getAsFloat() < 0 || u.get(1).getAsFloat() < 0 || u.get(0).getAsFloat() + s.get(0).getAsFloat() > tw + 0.01f || u.get(1).getAsFloat() + s.get(1).getAsFloat() > th + 0.01f)
                                return SelfTests.Result.fail(id + ": a face uv lies outside the " + tw + " x " + th + " texture");
                        }
                    }
                }
                if (cubes > RecombAnimeRules.MAX_CUBES[m]) return SelfTests.Result.fail(id + " has " + cubes + " cubes, the cap is " + RecombAnimeRules.MAX_CUBES[m]);
                for (String suffix : new String[]{".png", "_glow.png"}) {
                    int[] size = pngSize("/assets/nusmp/textures/entity/" + id + suffix);
                    if (size[0] != tw || size[1] != th) return SelfTests.Result.fail(id + suffix + " is " + size[0] + " x " + size[1] + ", the model says " + tw + " x " + th);
                }
                JsonObject anims = json("/assets/nusmp/animations/entity/" + id + ".animation.json").getAsJsonObject("animations");
                for (String clip : RecombAnimeRules.CLIPS[m]) {
                    if (!anims.has(clip)) return SelfTests.Result.fail(id + " has no clip " + clip);
                    for (var e : anims.getAsJsonObject(clip).getAsJsonObject("bones").entrySet())
                        if (!bones.contains(e.getKey())) return SelfTests.Result.fail(id + ": clip " + clip + " moves the unknown bone " + e.getKey());
                }
            }
            for (String old : new String[]{"bull", "room", "halo", "drill"})
                if (RecombAnimeSelfTest.class.getResource("/assets/nusmp/geo/entity/recombination_" + old + ".geo.json") == null) return SelfTests.Result.fail("the older model recombination_" + old + " is gone");
            for (String id : RecombAnimeRules.PARTICLE_IDS)
                if (FxCatalog.byId(id) == null) return SelfTests.Result.fail("the particle " + id + " is not in the catalog");
            for (PropKind k : new PropKind[]{PropKind.RECOMBINATION_1, PropKind.RECOMBINATION_2, PropKind.RECOMBINATION_3, PropKind.RECOMBINATION_4})
                if (MagicProps.behavior(k) == null) return SelfTests.Result.fail("no behaviour for " + k);
            if (RecombAnimeRules.clip(0, 0, 5f, 1200f, false) != RecombAnimeRules.Clip.ASSEMBLE) return SelfTests.Result.fail("the rules do not start with the assembly");
            if (RecombAnimeRules.clip(RecombAnimeRules.RAM, 3, 100f, 1200f, true) != RecombAnimeRules.Clip.PUNCH) return SelfTests.Result.fail("the rules do not punch in the ram state");
            if (RecombAnimeRules.clip(0, 0, 1190f, 1200f, true) != RecombAnimeRules.Clip.COLLAPSE) return SelfTests.Result.fail("the rules do not collapse at the end");
            if (RecombAnimeRules.cycleBlocks(0.84f) <= 0.5f) return SelfTests.Result.fail("the stride is too short");
            if (RecombAnimeRules.footfalls(0.2f, 0.3f) != 1 || RecombAnimeRules.footfalls(0.7f, 0.8f) != 2) return SelfTests.Result.fail("the footfalls are not at 0.25 and 0.75 of the cycle");
        } catch (IOException | RuntimeException e) {
            return SelfTests.Result.fail(String.valueOf(e));
        }
        return SelfTests.Result.ok("4 anime models, 4 older models kept, " + RecombAnimeRules.PARTICLE_IDS.length + " particle ids, 4 behaviours");
    }
}
