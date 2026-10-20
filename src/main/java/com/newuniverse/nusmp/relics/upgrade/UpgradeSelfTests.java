package com.newuniverse.nusmp.relics.upgrade;

import com.newuniverse.nusmp.client.relics.worn.lib.UpgradeCatalog;
import com.newuniverse.nusmp.client.relics.worn.lib.WornCatalog;
import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The self-test {@code visual.god_worn} (run by the CI server smoke test, see {@link SelfTests}): every God relic the visual upgrade names exists as a registered God grade relic and has
 * its upgraded models (worn, low detail, held), its skin and glow map, and its effect table in the jar, the models parse and keep the vertex caps (the God worn cap table, the 15% LOD, the
 * 2500 vertex held form), and the fallback holds: with the {@code visual.upgrade} flag off (an empty upgrade catalog) every one of them still resolves to its 1.101 worn models. Reads the
 * jar only, no client class that touches the game, so it runs on a dedicated server.
 */
public final class UpgradeSelfTests {
    private UpgradeSelfTests() {}

    /** The God grade caps of the upgrade (the same numbers as tools/visual_upgrade/god_worn/caps.py; GodWornUpgradeTest checks the two agree). */
    public static final int MODEL_VERTICES = UpgradeCaps.GOD_MODEL_VERTICES, ENSEMBLE_VERTICES = UpgradeCaps.GOD_ENSEMBLE_VERTICES, HELD_VERTICES = UpgradeCaps.GOD_HELD_VERTICES;
    public static final double LOD_FRACTION = UpgradeCaps.GOD_LOD_FRACTION;

    private static final String WORN = "/assets/nusmp/relic_worn/", UPG = "/assets/nusmp/visual_upgrade/", SKINS = "/assets/nusmp/textures/relic_worn/god/";

    public static void register() { SelfTests.register("visual.god_worn", server -> run()); }

    private static List<String> lines(String path) throws IOException {
        List<String> out = new ArrayList<>();
        try (InputStream in = UpgradeSelfTests.class.getResourceAsStream(path)) {
            if (in == null) return out;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String l;
                while ((l = r.readLine()) != null) out.add(l);
            }
        }
        return out;
    }

    private static int vertices(List<WornCatalog.Piece> pieces, List<String> problems, String what) throws IOException {
        int total = 0;
        for (WornCatalog.Piece p : pieces) {
            List<String> ls = lines(WORN + p.model() + ".txt");
            if (ls.isEmpty()) {
                problems.add(what + ": model " + p.model() + " is missing");
                continue;
            }
            WornMesh m = WornMesh.parse(p.model(), ls);
            if (m.solidVertices() > MODEL_VERTICES) problems.add(what + ": " + p.model() + " has " + m.solidVertices() + " vertices (cap " + MODEL_VERTICES + ")");
            if (m.skin == null) problems.add(what + ": " + p.model() + " names no skin");
            total += m.solidVertices();
        }
        return total;
    }

    static SelfTests.Result run() throws IOException {
        List<String> up = lines(UPG + "catalog.txt");
        if (up.isEmpty()) return SelfTests.Result.fail("the visual upgrade catalog is not in the jar");
        UpgradeCatalog cat = UpgradeCatalog.parse(up);
        if (cat.size() == 0) return SelfTests.Result.fail("the visual upgrade catalog names no relic");
        WornCatalog old = WornCatalog.parse(lines(WORN + "catalog.txt"));
        UpgradeCatalog flagOff = UpgradeCatalog.EMPTY;
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (String id : cat.ids()) {
            RelicDef def = Relics.byId(id);
            if (def == null) { problems.add(id + ": not a registered relic"); continue; }
            if (def.grade() != Grade.GOD) problems.add(id + ": grade " + def.grade() + ", not GOD");
            int full = vertices(cat.up(id), problems, id + " up");
            int lod = vertices(cat.lod(id), problems, id + " lod");
            int held = vertices(cat.held(id), problems, id + " held");
            if (full > ENSEMBLE_VERTICES) problems.add(id + ": worn ensemble " + full + " vertices (cap " + ENSEMBLE_VERTICES + ")");
            if (held > HELD_VERTICES) problems.add(id + ": held form " + held + " vertices (cap " + HELD_VERTICES + ")");
            if (lod == 0 || lod > LOD_FRACTION * full) problems.add(id + ": lod " + lod + " vertices of " + full);
            if (UpgradeSelfTests.class.getResource(SKINS + id + ".png") == null || UpgradeSelfTests.class.getResource(SKINS + id + "_glow.png") == null) problems.add(id + ": skin or glow map missing");
            UpgradeFxData fx = UpgradeFxData.forId(id);
            if (fx == null) problems.add(id + ": no effect table");
            else for (String pr : fx.problems()) problems.add(id + ": " + pr);
            // the flag off: the upgrade catalog answers nothing and the 1.101 catalog still dresses the relic
            if (flagOff.has(id) || old.piecesFor(id, def.slot().name(), def.look().name()).isEmpty()) problems.add(id + ": no 1.101 look to fall back to");
            checked++;
        }
        if (!problems.isEmpty()) return SelfTests.Result.fail(problems.size() + " problems, first: " + problems.get(0));
        return SelfTests.Result.ok(checked + " God relics: models, skins, effect tables and the visual.upgrade fallback are in order");
    }
}
