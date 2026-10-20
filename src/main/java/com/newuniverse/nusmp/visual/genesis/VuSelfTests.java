package com.newuniverse.nusmp.visual.genesis;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.perf.VfxQuality;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The self-test {@code visual.genesis_weapons} of the CI server smoke test: inside the booted mod, every weapon of the slice ships its upgraded assets (mesh, level-of-detail
 * mesh, skin, effect data, the old mesh kept), the meshes parse and sit inside the caps of the performance guard, the effect data has the layers the brief asks for, and the
 * flag fallback and the distance rules decide as they should. Touches no client class, so it runs on the dedicated server.
 */
public final class VuSelfTests {
    private VuSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("visual.genesis_weapons", VuSelfTests::run);
    }

    static InputStream open(String path) { return VuSelfTests.class.getResourceAsStream("/assets/nusmp/" + path); }

    private static SelfTests.Result run(MinecraftServer server) throws Exception {
        if (VuRules.useUpgrade(false, true) || VuRules.useUpgrade(true, false) || !VuRules.useUpgrade(true, true)) return SelfTests.Result.fail("the flag fallback decides wrongly");
        if (VuRules.useLod(100.0, false, VuRules.LOD_NEAR) || !VuRules.useLod(150.0, false, VuRules.LOD_NEAR) || VuRules.useLod(1e9, true, VuRules.LOD_NEAR)) return SelfTests.Result.fail("the LOD distance rule is wrong");
        if (VuRules.haloAllowed(VfxQuality.LOW) || !VuRules.haloAllowed(VfxQuality.MEDIUM) || VuRules.particleBudget(VfxQuality.LOW) != 0) return SelfTests.Result.fail("the quality rules are wrong");
        int items = 0, quads = 0;
        for (String id : VuCatalog.IDS) {
            try (InputStream old = open("weapon_meshes/" + id + ".txt")) {
                if (old == null) return SelfTests.Result.fail(id + ": the old mesh was removed (it must stay)");
            }
            VuSpec spec;
            try (InputStream in = open("visual_upgrade/" + id + ".json")) {
                if (in == null) return SelfTests.Result.fail(id + ": no effect data");
                spec = VuSpec.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            if (spec.layers.size() < 6) return SelfTests.Result.fail(id + ": fewer than 6 ambient effect layers");
            for (String mesh : VuCatalog.meshes(id)) {
                VuMesh m = read("weapon_meshes_vu/" + mesh + ".txt");
                VuMesh lod = read("weapon_meshes_vu/" + mesh + "_lod.txt");
                if (m == null) return SelfTests.Result.fail(mesh + ": no upgraded mesh");
                if (lod == null) return SelfTests.Result.fail(mesh + ": no level-of-detail mesh");
                if (m.weldedVertices(VuMesh.BODY) > VuRules.MAX_VERTICES_GENESIS) return SelfTests.Result.fail(mesh + ": over the vertex cap");
                if (lod.quads(VuMesh.BODY) > VuRules.LOD_RATIO_MAX * m.quads(VuMesh.BODY)) return SelfTests.Result.fail(mesh + ": the LOD mesh is over 15 percent");
                try (InputStream skin = open("textures/entity/weapon_vu/" + m.skin + ".png")) {
                    if (skin == null) return SelfTests.Result.fail(mesh + ": no skin " + m.skin);
                }
                quads += m.quads(VuMesh.BODY);
            }
            items++;
        }
        return SelfTests.Result.ok(items + " upgraded weapons, " + quads + " body quads, all inside the caps");
    }

    private static VuMesh read(String path) throws Exception {
        try (InputStream in = open(path)) {
            if (in == null) return null;
            return VuMesh.parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
        }
    }
}
