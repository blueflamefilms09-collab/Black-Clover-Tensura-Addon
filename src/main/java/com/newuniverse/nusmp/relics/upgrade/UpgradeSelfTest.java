package com.newuniverse.nusmp.relics.upgrade;

import com.newuniverse.nusmp.client.upgrade.UpgradeConfig;
import com.newuniverse.nusmp.client.upgrade.UpgradeSpec;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.relics.Act;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The self-test {@code visual.god_melee_a} (run by the CI server smoke test, see {@link SelfTests}): every God melee (a) weapon of the visual upgrade has its upgraded mesh, low-detail
 * mesh, skins, veins map and effect spec in the jar; the spec parses and has a signature for every act of the registered relic (the same act keys, unique signature ids); the shipped
 * mesh, skin and glow it falls back to are still there; and the client option that switches the upgrade off answers (it never throws before the config is loaded).
 */
public final class UpgradeSelfTest {
    private UpgradeSelfTest() {}

    public static void register() { SelfTests.register("visual.god_melee_a", server -> run()); }

    private static boolean has(String file) { return UpgradeSelfTest.class.getResource("/assets/nusmp/" + file) != null; }

    static SelfTests.Result run() {
        List<String> bad = new ArrayList<>();
        Set<Integer> sigs = new HashSet<>();
        int acts = 0;
        for (String id : UpgradeItems.GOD_MELEE_A) {
            for (String f : new String[]{"weapon_meshes/up/" + id + ".txt", "weapon_meshes/up/" + id + "_lod.txt", "textures/entity/weapon/up/" + id + ".png", "textures/entity/weapon/up/" + id + "_glow.png",
                    "textures/entity/weapon/up/" + id + "_veins.png", "textures/entity/weapon/up/" + id + "_lod.png", "textures/entity/weapon/up/" + id + "_lod_glow.png", "visual_upgrade/" + id + ".json",
                    "weapon_meshes/" + id + ".txt", "textures/entity/weapon/" + id + ".png", "textures/entity/weapon/" + id + "_glow.png"}) {      // the last three: what visual.upgrade = false falls back to
                if (!has(f)) bad.add(id + ": " + f + " is not in the jar");
            }
            RelicDef def = Relics.byId(id);
            if (def == null) {
                bad.add(id + " is not registered");
                continue;
            }
            try (Reader in = new InputStreamReader(UpgradeSelfTest.class.getResourceAsStream("/assets/nusmp/visual_upgrade/" + id + ".json"), StandardCharsets.UTF_8)) {
                UpgradeSpec spec = UpgradeSpec.parse(in);
                if (spec.ambient.size() < 3 || spec.passes.size() < 3) bad.add(id + ": the spec has " + spec.ambient.size() + " ambient layers / " + spec.passes.size() + " passes");
                for (Act a : def.acts()) {
                    UpgradeSpec.Act s = spec.actByKey(a.key());
                    if (s == null) bad.add(id + ": no signature for the act " + a.key());
                    else {
                        acts++;
                        if (s.id() != UpgradeItems.signatureId(id, a.key())) bad.add(id + "/" + a.key() + ": wrong signature id");
                        if (!sigs.add(s.id())) bad.add(id + "/" + a.key() + ": signature id used twice");
                    }
                }
            } catch (Exception e) {
                bad.add(id + ": the spec does not parse: " + e);
            }
        }
        try {
            UpgradeConfig.enabled();
            UpgradeConfig.density();
        } catch (RuntimeException e) {
            bad.add("the visual.upgrade option threw before the config was loaded: " + e);
        }
        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problems, first: " + bad.get(0));
        return SelfTests.Result.ok(UpgradeItems.GOD_MELEE_A.size() + " weapons, " + acts + " act signatures");
    }
}
