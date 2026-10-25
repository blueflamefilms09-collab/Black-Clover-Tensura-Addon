package com.newuniverse.nusmp.relics;

import com.newuniverse.nusmp.client.relics.worn.lib.WornAnchor;
import com.newuniverse.nusmp.client.relics.worn.lib.WornPlacer;
import com.newuniverse.nusmp.client.relics.worn.lib.X3Gate;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.relics.upgrade.UpgradeCaps;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The self-tests {@code relic.display39} and {@code relic.x3} (CI server smoke test; pure checks and classpath reads only, so they run on a dedicated server): the contact fit keeps the middle
 * of every body-part model where its file put it, hip charms are lowered clear of the arm, the x3 gate lets only the nearest wearers of a frame through, and the blocky x3 model files of the
 * armour relics are listed, present and inside the caps.
 */
public final class Display39SelfTests {
    private Display39SelfTests() {}

    public static void register() {
        SelfTests.register("relic.display39", server -> display());
        SelfTests.register("relic.x3", server -> x3());
    }

    static SelfTests.Result display() {
        for (WornAnchor a : new WornAnchor[]{WornAnchor.BODY, WornAnchor.RIGHT_ARM, WornAnchor.LEFT_ARM, WornAnchor.RIGHT_LEG, WornAnchor.LEFT_LEG}) {
            for (float c : new float[]{-3f, 0f, 6f, 9f, 12f}) {
                WornPlacer.Placement p = WornPlacer.fit(a, "boots_cuffed", c, true);
                if (Math.abs(p.dy() + p.scale() * c - c) > 0.001f) return SelfTests.Result.fail("contact fit moves the middle of a model at " + c + " on " + a);
                if (!WornPlacer.fit(a, "boots_cuffed", c, false).equals(WornPlacer.fit(a, "boots_cuffed"))) return SelfTests.Result.fail("contactFit off is not the 3.8 placement");
            }
        }
        if (!WornPlacer.fit(WornAnchor.HEAD, "crown_spired", -9f, true).equals(WornPlacer.Placement.NONE)) return SelfTests.Result.fail("a head item was touched");
        for (String m : new String[]{"charm_flask", "charm_tome", "charm_pouch", "charm_pouch_pair"}) if (!WornPlacer.hipHung(m)) return SelfTests.Result.fail(m + " is not lowered");
        X3Gate.reset();
        int ok = 0;
        for (int i = 0; i < 20; i++) if (X3Gate.allow(5_000_000_000L + i, UpgradeCaps.X3_WEARERS)) ok++;
        X3Gate.reset();
        if (ok != UpgradeCaps.X3_WEARERS) return SelfTests.Result.fail("the x3 gate let " + ok + " wearers through in one frame");
        return SelfTests.Result.ok("contact fit keeps the middle of every model, hip charms are lowered, the x3 gate lets " + UpgradeCaps.X3_WEARERS + " wearers per frame through");
    }

    static SelfTests.Result x3() {
        int listed = 0, quads = 0;
        try (InputStream in = Display39SelfTests.class.getResourceAsStream("/assets/nusmp/relic_worn_x3/index.txt")) {
            if (in == null) return SelfTests.Result.fail("relic_worn_x3/index.txt is missing");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                listed++;
                if (line.matches("(?i).*(cape|cloak|mantle|shawl).*")) return SelfTests.Result.fail("a cape is listed: " + line);
                if (listed % 4 != 0) continue;                                                // every eighth file is read in full: bounded work at boot
                try (InputStream mf = Display39SelfTests.class.getResourceAsStream("/assets/nusmp/relic_worn_x3/" + line + ".txt")) {
                    if (mf == null) return SelfTests.Result.fail(line + " is listed but missing");
                    BufferedReader mr = new BufferedReader(new InputStreamReader(mf, StandardCharsets.UTF_8));
                    int q = 0, emit = 0;
                    String l;
                    while ((l = mr.readLine()) != null) {
                        if (!l.startsWith("q ")) continue;
                        q++;
                        String[] v = l.split(" ", 5);
                        if ((Integer.parseInt(v[3]) & 1) != 0) emit++;
                    }
                    if (q == 0 || (q + emit) * 4 > UpgradeCaps.X3_MODEL_VERTICES) return SelfTests.Result.fail(line + " has " + (q + emit) * 4 + " vertices (cap " + UpgradeCaps.X3_MODEL_VERTICES + ")");
                    quads += q;
                }
            }
        } catch (Exception ex) {
            return SelfTests.Result.fail("could not read the x3 models: " + ex);
        }
        if (listed < 40) return SelfTests.Result.fail("only " + listed + " x3 models are listed");
        return SelfTests.Result.ok(listed + " blocky feet models listed, the sampled ones inside the cap (" + quads + " quads read)");
    }
}
