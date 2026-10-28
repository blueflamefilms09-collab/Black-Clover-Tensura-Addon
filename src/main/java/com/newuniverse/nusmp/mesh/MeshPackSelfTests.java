package com.newuniverse.nusmp.mesh;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.visual.genesis.VuCatalog;
import com.newuniverse.nusmp.visual.genesis.VuMesh;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 3.32.0 self-test {@code mesh.pack} of the CI server smoke test: inside the booted mod, real meshes of the jar are packed in memory, read back through the same
 * {@link MeshText} the loaders use and parse to the same quads as the plain text, and the packed form is far smaller. Plain data only: runs on a dedicated server.
 */
public final class MeshPackSelfTests {
    private MeshPackSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("mesh.pack", server -> run());
    }

    private static byte[] resource(String path) throws Exception {
        try (InputStream in = MeshPackSelfTests.class.getResourceAsStream("/assets/nusmp/" + path)) {
            return in == null ? null : in.readAllBytes();
        }
    }

    private static SelfTests.Result run() throws Exception {
        int files = 0;
        long text = 0, packed = 0;
        for (String id : VuCatalog.IDS) {
            for (String path : new String[]{"weapon_meshes_vu/" + id + ".txt", "weapon_meshes/" + id + ".txt"}) {
                byte[] raw = resource(path);
                if (raw == null) continue;
                String plain = MeshPack.text(raw);                                         // the jar may already hold the packed form
                byte[] bin = MeshPack.pack(plain);
                if (!MeshPack.isPacked(bin)) return SelfTests.Result.fail(path + ": pack() has no header");
                String diff = MeshPack.firstDifference(plain, MeshPack.unpack(bin));
                if (diff != null) return SelfTests.Result.fail(path + " does not survive packing: " + diff);
                if (path.startsWith("weapon_meshes_vu/")) {                                // the real loader's parser sees the same mesh
                    VuMesh a = VuMesh.parse(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(plain.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)));
                    VuMesh b = VuMesh.parse(MeshText.reader(new ByteArrayInputStream(bin)));
                    for (int l = 0; l < VuMesh.LAYERS; l++)
                        if (a.quads(l) != b.quads(l)) return SelfTests.Result.fail(path + ": layer " + l + " has " + a.quads(l) + " quads, packed " + b.quads(l));
                    if (a.groups.size() != b.groups.size() || a.flares.size() != b.flares.size() || !a.skin.equals(b.skin)) return SelfTests.Result.fail(path + ": header lines changed");
                    for (int g = 0; g < a.groups.size(); g++)
                        for (int l = 0; l < VuMesh.LAYERS; l++)
                            for (int i = 0; i < a.groups.get(g).count[l]; i++)
                                if (a.groups.get(g).verts[l][i] != b.groups.get(g).verts[l][i]) return SelfTests.Result.fail(path + ": a vertex value changed");
                }
                // the plain-text path of the shim keeps working (the source tree and the tests read plain files)
                String back = MeshText.text(new ByteArrayInputStream(plain.getBytes(StandardCharsets.UTF_8)));
                if (!back.equals(plain)) return SelfTests.Result.fail(path + ": plain text did not pass through MeshText unchanged");
                files++;
                text += plain.length();
                packed += bin.length;
                break;
            }
        }
        if (files == 0) return SelfTests.Result.fail("no weapon mesh found to pack");
        if (packed * 5 > text) return SelfTests.Result.fail("packing saved too little: " + text + " chars -> " + packed + " bytes");
        return SelfTests.Result.ok(files + " real meshes packed and read back identically (" + (text >> 10) + " KB of text -> " + (packed >> 10) + " KB)");
    }
}
