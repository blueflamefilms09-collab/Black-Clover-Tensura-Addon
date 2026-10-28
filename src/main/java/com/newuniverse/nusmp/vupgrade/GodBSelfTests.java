package com.newuniverse.nusmp.vupgrade;

import com.newuniverse.nusmp.mesh.MeshText;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.vupgrade.godb.UpgradeMath;
import com.newuniverse.nusmp.client.vupgrade.godb.UpgradeSpec;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Why: the visual upgrade of the God melee weapons (release 1.102.0, slice god_melee_b) is mostly files, and a file that is missing from the jar is only noticed by a player. The self-test
 * {@code visual.god_melee_b} (run by the CI server smoke test; it touches no client class, so it runs on the dedicated server) checks inside the booted mod that for every one of the nine
 * weapons the data table, the three meshes (full, level of detail, orbiting piece) and the three skins (body, core glow, vein glow) are in the jar and agree with each other (the level
 * of detail has at most 15% of the quads), that the old mesh and skin the {@code visual.upgrade=false} fallback draws are still shipped, and that the switch picks the old mesh when it is off,
 * when there is no table or when the upgraded mesh does not load.
 */
public final class GodBSelfTests {
    private GodBSelfTests() {}

    public static final List<String> IDS = List.of("aurliels_seraph_halberd", "chronarchs_pendulum_sabre", "deepwatch_harpoon", "orials_starfall_staff", "reflected_kings_mirrorblade",
            "rimeheart_greatblade", "sanguine_wardens_cleaver", "valkarins_stormlance", "paradox");

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("visual.god_melee_b", GodBSelfTests::run);
    }

    private static SelfTests.Result run(MinecraftServer server) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String id : IDS) {
            String table = text("/assets/nusmp/visual_upgrade/" + id + ".json");
            if (table == null) return SelfTests.Result.fail(id + ": the data table is not in the jar");
            UpgradeSpec s = UpgradeSpec.parse(JsonParser.parseString(table).getAsJsonObject());
            if (!s.item().equals(id)) return SelfTests.Result.fail(id + ": the table names " + s.item());
            int body = quads("/assets/nusmp/" + "weapon_meshes/" + s.mesh() + ".txt");
            int lod = quads("/assets/nusmp/weapon_meshes/" + s.lodMesh() + ".txt");
            int orbit = quads("/assets/nusmp/weapon_meshes/" + s.orbitMesh() + ".txt");
            if (body < 600) return SelfTests.Result.fail(id + ": the upgraded mesh has only " + body + " quads");
            if (lod <= 0 || lod > 0.15 * body) return SelfTests.Result.fail(id + ": the level-of-detail mesh has " + lod + " of " + body + " quads");
            if (orbit <= 0 && s.orbitPieces() > 0) return SelfTests.Result.fail(id + ": the orbiting piece is empty");
            for (String t : new String[]{"", "_glow", "_glow2"}) {
                if (!exists("/assets/nusmp/textures/entity/weapon/" + s.mesh() + t + ".png")) return SelfTests.Result.fail(id + ": the skin " + s.mesh() + t + ".png is missing");
            }
            if (!exists("/assets/nusmp/weapon_meshes/" + id + ".txt") || !exists("/assets/nusmp/textures/entity/weapon/" + id + ".png")) return SelfTests.Result.fail(id + ": the old mesh or skin (the fallback) is gone");
            // the switch
            if (!UpgradeMath.meshFor(true, true, true, id, s.mesh()).equals(s.mesh())) return SelfTests.Result.fail(id + ": the switch does not pick the upgraded mesh");
            if (!UpgradeMath.meshFor(false, true, true, id, s.mesh()).equals(id)) return SelfTests.Result.fail(id + ": visual.upgrade=false does not fall back to the old mesh");
            if (!UpgradeMath.meshFor(true, false, true, id, s.mesh()).equals(id) || !UpgradeMath.meshFor(true, true, false, id, s.mesh()).equals(id)) return SelfTests.Result.fail(id + ": a missing table or mesh does not fall back");
            // the maths stays finite and inside its range
            float[] o = new float[5];
            for (UpgradeSpec.Orbiter ob : s.orbiters()) {
                UpgradeMath.orbit(ob, 0, 123.4f, s.axisY0(), s.axisY1(), o);
                for (float v : o) if (Float.isNaN(v) || Float.isInfinite(v)) return SelfTests.Result.fail(id + ": an orbit position is not finite");
            }
            float w = UpgradeMath.wave(50f, 77f, s.veinSpeed(), s.veinWavelength(), s.veinBase(), s.veinPeak());
            if (w < s.veinBase() - 1e-3f || w > s.veinPeak() + 1e-3f) return SelfTests.Result.fail(id + ": the vein wave leaves its range");
            sb.append(id.charAt(0)).append(body).append('/').append(lod).append(' ');
        }
        return SelfTests.Result.ok("9 upgraded weapons: tables, meshes (body/lod quads) and skins present, old files kept, fallback and level of detail hold: " + sb.toString().trim());
    }

    private static boolean exists(String path) {
        try (InputStream in = GodBSelfTests.class.getResourceAsStream(path)) {
            return in != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static String text(String path) throws IOException {
        try (InputStream in = GodBSelfTests.class.getResourceAsStream(path)) {
            if (in == null) return null;
            try (BufferedReader r = MeshText.reader(in)) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        }
    }

    /** The body (layer 0 and 1) quads of a mesh file in the jar. */
    static int quads(String path) throws IOException {
        String t = text(path);
        if (t == null) return -1;
        int n = 0;
        for (String line : t.split("\n")) {
            if (line.startsWith("qn 0 ") || line.startsWith("qn 1 ") || line.startsWith("q 0 ") || line.startsWith("q 1 ")) n++;
        }
        return n;
    }
}
