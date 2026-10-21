package com.newuniverse.nusmp.vupgrade;

import com.google.gson.JsonParser;
import com.newuniverse.nusmp.client.vupgrade.godb.UpgradeMath;
import com.newuniverse.nusmp.client.vupgrade.godb.UpgradeSpec;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponItems;
import com.newuniverse.nusmp.relics.djinn.weapon.DjinnWeaponRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Why: the visual upgrade of the 36 dark Djinn weapons (release 1.108.0, slice djinn_dark_weapons) is mostly files, and a file missing from the jar is only noticed by a player. The self-test
 * {@code visual.djinn_dark_weapons} (run by the CI server smoke test; it touches no client class, so it runs on the dedicated server) walks the 36 rows of the {@link Djinn} roster and checks,
 * inside the booted mod, that the dark weapon item of each is registered, that its data table, its three meshes (full, level of detail, orbiting piece) and its three skins (body, core glow,
 * vein glow) are in the jar and agree with each other (the level of detail has at most 15% of the quads, the table says 'fx' for every particle layer: the mod's own particles), that the old
 * mesh and skin the {@code visual.upgrade=false} fallback draws are still shipped, and that the switch picks the old mesh when it is off, when there is no table or when the upgraded mesh does
 * not load (the same {@link UpgradeMath#meshFor} the renderer uses). The tables are read by the God melee (b) code ({@code client/vupgrade/godb}); nothing here is a second engine.
 */
public final class DjinnDarkSelfTests {
    private DjinnDarkSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("visual.djinn_dark_weapons", DjinnDarkSelfTests::run);
    }

    private static SelfTests.Result run(MinecraftServer server) throws IOException {
        int n = 0;
        StringBuilder sb = new StringBuilder();
        for (Djinn d : Djinn.values()) {
            String id = DjinnWeaponRules.itemPath(d, true);
            if (!BuiltInRegistries.ITEM.getKey(DjinnWeaponItems.item(d, true)).getPath().equals(id)) return SelfTests.Result.fail(id + ": the dark weapon item is not registered under that id");
            String table = text("/assets/nusmp/visual_upgrade/" + id + ".json");
            if (table == null) return SelfTests.Result.fail(id + ": the data table is not in the jar");
            UpgradeSpec s = UpgradeSpec.parse(JsonParser.parseString(table).getAsJsonObject());
            if (!s.item().equals(id)) return SelfTests.Result.fail(id + ": the table names " + s.item());
            int body = GodBSelfTests.quads("/assets/nusmp/weapon_meshes/" + s.mesh() + ".txt");
            int lod = GodBSelfTests.quads("/assets/nusmp/weapon_meshes/" + s.lodMesh() + ".txt");
            int orbit = GodBSelfTests.quads("/assets/nusmp/weapon_meshes/" + s.orbitMesh() + ".txt");
            if (body < 600) return SelfTests.Result.fail(id + ": the upgraded mesh has only " + body + " quads");
            if (lod <= 0 || lod > 0.15 * body) return SelfTests.Result.fail(id + ": the level-of-detail mesh has " + lod + " of " + body + " quads");
            if (orbit <= 0 && s.orbitPieces() > 0) return SelfTests.Result.fail(id + ": the orbiting piece is empty");
            for (String t : new String[]{"", "_glow", "_glow2"}) {
                if (!exists("/assets/nusmp/textures/entity/weapon/" + s.mesh() + t + ".png")) return SelfTests.Result.fail(id + ": the skin " + s.mesh() + t + ".png is missing");
            }
            if (!exists("/assets/nusmp/weapon_meshes/" + id + ".txt") || !exists("/assets/nusmp/textures/entity/weapon/" + id + ".png")) return SelfTests.Result.fail(id + ": the old mesh or skin (the fallback) is gone");
            for (UpgradeSpec.Layer l : s.layers()) {
                if (!l.particle().equals("fx")) return SelfTests.Result.fail(id + ": the particle layer " + l.kind() + " names '" + l.particle() + "', not the mod's own particle (fx)");
            }
            if (!UpgradeMath.meshFor(true, true, true, id, s.mesh()).equals(s.mesh())) return SelfTests.Result.fail(id + ": the switch does not pick the upgraded mesh");
            if (!UpgradeMath.meshFor(false, true, true, id, s.mesh()).equals(id)) return SelfTests.Result.fail(id + ": visual.upgrade=false does not fall back to the old mesh");
            if (!UpgradeMath.meshFor(true, false, true, id, s.mesh()).equals(id) || !UpgradeMath.meshFor(true, true, false, id, s.mesh()).equals(id)) return SelfTests.Result.fail(id + ": a missing table or mesh does not fall back");
            float[] o = new float[5];
            for (UpgradeSpec.Orbiter ob : s.orbiters()) {
                UpgradeMath.orbit(ob, 0, 123.4f, s.axisY0(), s.axisY1(), o);
                for (float v : o) if (Float.isNaN(v) || Float.isInfinite(v)) return SelfTests.Result.fail(id + ": an orbit position is not finite");
            }
            n++;
            if (n <= 3) sb.append(d.id()).append(' ').append(body).append('/').append(lod).append(' ');
        }
        return SelfTests.Result.ok(n + " dark Djinn weapons: item, table, meshes (body/lod quads), skins and the fx particle layers present, old files kept, fallback and level of detail hold (first: " + sb.toString().trim() + ")");
    }

    private static boolean exists(String path) {
        try (InputStream in = DjinnDarkSelfTests.class.getResourceAsStream(path)) {
            return in != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static String text(String path) throws IOException {
        try (InputStream in = DjinnDarkSelfTests.class.getResourceAsStream(path)) {
            if (in == null) return null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        }
    }
}
