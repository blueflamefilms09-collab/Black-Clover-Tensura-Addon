package com.newuniverse.nusmp.visual.demonslayer;

import com.newuniverse.nusmp.client.DemonSlayerMesh;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.perf.VfxQuality;
import com.newuniverse.nusmp.visual.genesis.VuMesh;
import com.newuniverse.nusmp.visual.genesis.VuPose;
import com.newuniverse.nusmp.visual.genesis.VuRules;
import com.newuniverse.nusmp.visual.genesis.VuSpec;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The self-test {@code visual.demon_slayer} of the CI server smoke test: inside the booted mod, the Genesis Demon-Slayer ships its upgraded assets (mesh, level-of-detail mesh, skins, effect
 * data) next to the old ones (the pixel model, its skin, the icon models: all kept), the meshes sit inside the caps of the performance guard, the placement is the old renderer's, the effect
 * data has what a Genesis item needs (six ambient layers, a full idle set, a draw animation, a swing trail, a signature burst for each of its three acts, a looped sound id), the shards and
 * the bloom carry the stack-driven animations, and the flag fallback, the distance rules and the fracture / resonance maths decide as they should. Touches no client class that needs the
 * game (the old mesh class is plain geometry), so it runs on the dedicated server.
 */
public final class DsSelfTests {
    private DsSelfTests() {}

    public static final String ID = DsCatalog.ID;

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("visual.demon_slayer", DsSelfTests::run);
    }

    static InputStream open(String path) { return DsSelfTests.class.getResourceAsStream("/assets/nusmp/" + path); }

    private static SelfTests.Result run(MinecraftServer server) throws Exception {
        if (VuRules.useUpgrade(false, true) || VuRules.useUpgrade(true, false) || !VuRules.useUpgrade(true, true)) return SelfTests.Result.fail("the flag fallback decides wrongly");
        if (VuRules.useLod(100.0, false, VuRules.LOD_NEAR) || !VuRules.useLod(150.0, false, VuRules.LOD_NEAR) || VuRules.useLod(1e9, true, VuRules.LOD_NEAR)) return SelfTests.Result.fail("the LOD distance rule is wrong");
        if (VuRules.haloAllowed(VfxQuality.LOW) || !VuRules.haloAllowed(VfxQuality.MEDIUM) || VuRules.particleBudget(VfxQuality.LOW) != 0) return SelfTests.Result.fail("the quality rules are wrong");
        // the old model, its skins and the icon stay (add the new, keep the old)
        for (String old : new String[]{"textures/entity/demon_slayer_sword.png", "textures/entity/demon_slayer_bloom.png", "textures/entity/demon_slayer_bloom_glow.png",
                "models/item/demon_slayer_sword.json", "models/item/demon_slayer_sword_icon.json"}) {
            try (InputStream in = open(old)) {
                if (in == null) return SelfTests.Result.fail(old + ": removed (the old look must stay for visual.upgrade = false)");
            }
        }
        if (DemonSlayerMesh.fracture(15f) <= 0f || DemonSlayerMesh.fracture(0f) != 0f) return SelfTests.Result.fail("the old fracture curve changed");
        // the new assets
        VuSpec spec;
        try (InputStream in = open("visual_upgrade/" + ID + ".json")) {
            if (in == null) return SelfTests.Result.fail("no effect data");
            spec = VuSpec.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        VuMesh m = read("weapon_meshes_vu/" + ID + ".txt"), lod = read("weapon_meshes_vu/" + ID + "_lod.txt");
        if (m == null) return SelfTests.Result.fail("no upgraded mesh");
        if (lod == null) return SelfTests.Result.fail("no level-of-detail mesh");
        if (m.quads(VuMesh.BODY) < 2500) return SelfTests.Result.fail("only " + m.quads(VuMesh.BODY) + " body quads");
        if (m.weldedVertices(VuMesh.BODY) > VuRules.MAX_VERTICES_GENESIS) return SelfTests.Result.fail("over the vertex cap");
        if (lod.quads(VuMesh.BODY) > VuRules.LOD_RATIO_MAX * m.quads(VuMesh.BODY)) return SelfTests.Result.fail("the LOD mesh is over 15 percent");
        float[] want = DsCatalog.PLACE;
        float[] got = {m.ox, m.oy, m.angle, m.scale, m.ymin};
        for (int i = 0; i < 5; i++) if (Math.abs(want[i] - got[i]) > 1e-4f * Math.max(1f, Math.abs(want[i]))) return SelfTests.Result.fail("the placement differs from the old renderer's in field " + i);
        for (String suffix : new String[]{".png", "_glow.png", "_veins.png"}) {
            try (InputStream in = open("textures/entity/weapon_vu/" + m.skin + suffix)) {
                if (in == null) return SelfTests.Result.fail("no skin " + m.skin + suffix);
            }
        }
        if (spec.layers.size() < 6) return SelfTests.Result.fail("fewer than 6 ambient effect layers");
        if (spec.idle.size() < 6) return SelfTests.Result.fail("fewer than 6 idle animations");
        if (spec.draw == null || spec.swing == null) return SelfTests.Result.fail("no draw animation or swing trail");
        if (spec.soundLoop == null || !spec.soundLoop.startsWith("nusmp:")) return SelfTests.Result.fail("no looped sound id");
        for (String act : DsCatalog.ACTS) if (spec.act(act) == null) return SelfTests.Result.fail("act " + act + " has no signature burst");
        for (String g : DsCatalog.SHARDS) if (!hasKind(spec, g, DsDynamics.FRACTURE)) return SelfTests.Result.fail("shard " + g + " has no fracture animation");
        for (String g : DsCatalog.CRACKS) if (!hasKind(spec, g, DsDynamics.FRACTURE_GLOW)) return SelfTests.Result.fail("crack light " + g + " has no fracture_glow animation");
        for (String g : DsCatalog.BLOOM) if (!hasKind(spec, g, DsDynamics.RESONANCE)) return SelfTests.Result.fail("group " + g + " has no resonance animation");
        // the stack-driven maths: a shard drifts out with the fracture and not without; the bloom grows and brightens with the resonance
        VuPose p = new VuPose().reset();
        DsDynamics.apply(spec.groups.get("s2"), p, 0f, 0f, 4);
        if (!p.identity()) return SelfTests.Result.fail("a shard moved without a fracture");
        DsDynamics.apply(spec.groups.get("s2"), p, 0f, 1f, 0);
        if (!(p.oy > 1f)) return SelfTests.Result.fail("a shard did not drift out at full fracture");
        VuPose lo = new VuPose().reset(), hi = new VuPose().reset();
        DsDynamics.apply(spec.groups.get("clover"), lo, 0f, 0f, 0);
        DsDynamics.apply(spec.groups.get("clover"), hi, 0f, 0f, 4);
        if (!(hi.scale > lo.scale)) return SelfTests.Result.fail("the clover did not grow with the resonance");
        return SelfTests.Result.ok(m.quads(VuMesh.BODY) + " body quads, " + m.weldedVertices(VuMesh.BODY) + " welded vertices, LOD " + lod.quads(VuMesh.BODY) + ", " + spec.layers.size() + " ambient layers, old model kept");
    }

    private static boolean hasKind(VuSpec spec, String group, String kind) {
        VuSpec.Group g = spec.groups.get(group);
        if (g == null) return false;
        for (VuSpec.Anim a : g.anim) if (a.kind.equals(kind)) return true;
        return false;
    }

    private static VuMesh read(String path) throws Exception {
        try (InputStream in = open(path)) {
            if (in == null) return null;
            return VuMesh.parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
        }
    }
}
