package com.newuniverse.nusmp.relics.vu;

import com.newuniverse.nusmp.client.relics.vu.VuFx;
import com.newuniverse.nusmp.client.relics.vu.VuOverrides;
import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The self-test {@code visual.charms_genesis_worn} (run by the CI server smoke test, see {@link SelfTests}): every relic of the visual upgrade of the charms, the Infinity Gauntlet, the
 * two crowns and the Gate of Babylon crest is a registered relic, has its override lines, its models (full, lod, worn), the skin, glow map and star texture the models name, its fx
 * file and its act effects, and the older fallback is still there (the override table is the only thing that points at the new models, so {@code visual.upgrade=false} is the old look).
 * It reads only files in the jar and pure classes, so it runs on a dedicated server.
 */
public final class VuSelfTests {
    private VuSelfTests() {}

    private static final String ASSETS = "/assets/nusmp/";

    public static void register() { SelfTests.register("visual.charms_genesis_worn", server -> run()); }

    private static List<String> lines(String path) throws IOException {
        List<String> out = new ArrayList<>();
        try (InputStream in = VuSelfTests.class.getResourceAsStream(path)) {
            if (in == null) return null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String l;
                while ((l = r.readLine()) != null) out.add(l);
            }
        }
        return out;
    }

    static SelfTests.Result run() throws IOException {
        List<String> ov = lines(ASSETS + "visual_upgrade/overrides.txt");
        if (ov == null) return SelfTests.Result.fail("assets/nusmp/visual_upgrade/overrides.txt is not in the jar");
        VuOverrides table = VuOverrides.parse(ov);
        if (!table.problems().isEmpty()) return SelfTests.Result.fail("overrides.txt: " + table.problems().get(0));
        List<String> acts = lines("/data/nusmp/visual_upgrade/act_vfx.txt");
        if (acts == null) return SelfTests.Result.fail("data/nusmp/visual_upgrade/act_vfx.txt is not in the jar");
        VuActTable actTable = VuActTable.parse(acts);
        if (!actTable.problems().isEmpty()) return SelfTests.Result.fail("act_vfx.txt: " + actTable.problems().get(0));
        Set<String> known = new HashSet<>();
        for (RelicDef d : Relics.all()) known.add(d.id());
        int models = 0, relics = 0;
        for (String id : table.handIds()) {
            relics++;
            if (!known.contains(id)) return SelfTests.Result.fail("the override names an unregistered relic " + id);
            if (table.worn(id) == null) return SelfTests.Result.fail(id + " has a hand line and no worn line");
            Set<String> names = new HashSet<>();
            for (String p : table.hand(id).pieces()) names.add(name(p));
            for (String p : table.worn(id).pieces()) names.add(name(p));
            for (String n : names) {
                SelfTests.Result bad = model(n);
                if (bad != null) return bad;
                models++;
            }
            List<String> fx = lines(ASSETS + "visual_upgrade/fx/" + id + ".json");
            if (fx == null) return SelfTests.Result.fail(id + " has no fx file");
            VuFx f = VuFx.parse(String.join("\n", fx));
            if (!f.problems().isEmpty()) return SelfTests.Result.fail(id + " fx: " + f.problems().get(0));
            if (f.layers.size() < 6) return SelfTests.Result.fail(id + " has only " + f.layers.size() + " ambient layers");
            if (!actTable.has(id)) return SelfTests.Result.fail(id + " has no act effects");
            RelicDef def = null;
            for (RelicDef d : Relics.all()) if (d.id().equals(id)) def = d;
            for (Act a : def.acts()) {
                if (actTable.steps(id, a.key()).isEmpty()) return SelfTests.Result.fail(id + " act " + a.key() + " has no effect");
            }
        }
        if (relics != 27) return SelfTests.Result.fail("expected 27 upgraded relics, found " + relics);
        for (String key : actTable.keys()) {                       // every explicit act of the table is an act the relic really has
            int slash = key.indexOf('/');
            String id = key.substring(0, slash), act = key.substring(slash + 1);
            if (act.equals("*")) continue;
            RelicDef def = null;
            for (RelicDef d : Relics.all()) if (d.id().equals(id)) def = d;
            if (def == null) return SelfTests.Result.fail("act_vfx.txt names the unregistered relic " + id);
            boolean found = false;
            for (Act a : def.acts()) if (a.key().equals(act)) found = true;
            if (!found) return SelfTests.Result.fail("act_vfx.txt gives " + id + " an effect for " + act + ", which is not one of its acts");
        }
        // the fallback: with the option off the table answers nothing, so the older catalogs decide
        for (String id : table.handIds()) {
            if (table.handIf(false, id) != null || table.wornIf(false, id) != null) return SelfTests.Result.fail("the fallback still answers for " + id);
            if (table.handIf(true, id) == null || table.wornIf(true, id) == null) return SelfTests.Result.fail("the upgrade answers nothing for " + id);
        }
        return SelfTests.Result.ok(relics + " relics, " + models + " models, fx and act effects in the jar, fallback answers nothing");
    }

    private static String name(String piece) {
        String s = piece;
        if (s.endsWith("~")) s = s.substring(0, s.length() - 1);
        int at = s.indexOf('@');
        return at < 0 ? s : s.substring(0, at);
    }

    /** null when the model, its lod and its textures exist; else the failure. */
    private static SelfTests.Result model(String name) throws IOException {
        if (!name.startsWith("vu/")) return SelfTests.Result.fail("an upgraded relic names a library model " + name);
        List<String> l = lines(ASSETS + "visual_upgrade/models/" + name.substring(3) + ".txt");
        if (l == null) return SelfTests.Result.fail("model " + name + " is not in the jar");
        WornMesh m = WornMesh.parse(name, l);
        if (!m.custom() || m.glowTex == null) return SelfTests.Result.fail(name + " names no skin / glow map");
        for (String tex : new String[]{m.skin, m.glowTex, m.starTex}) {
            if (tex == null) continue;
            try (InputStream in = VuSelfTests.class.getResourceAsStream(ASSETS + "textures/" + tex)) {
                if (in == null) return SelfTests.Result.fail(name + " names the missing texture " + tex);
            }
        }
        if (m.lodName == null) return SelfTests.Result.fail(name + " has no lod model");
        if (lines(ASSETS + "visual_upgrade/models/" + m.lodName.substring(3) + ".txt") == null) return SelfTests.Result.fail(name + " names the missing lod " + m.lodName);
        return null;
    }
}
