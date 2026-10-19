package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.GradeDamage;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;

import java.util.ArrayList;
import java.util.List;

/**
 * The self-test {@code relic.new197} (run by the CI server smoke test, see {@link SelfTests}): the 35 weapons of wave 71 are all registered, each is a main-hand relic of
 * its grade with a 3D model, Tensura engravings, a passive rule, two or three acts and a base damage inside the owner's range for its grade, and the files its model needs are
 * in the jar (mesh, skin, glow skin, sprite, both item models).
 */
final class Wave71SelfTest {
    private Wave71SelfTest() {}

    static void register() { SelfTests.register("relic.new197", server -> run()); }

    static SelfTests.Result run() {
        List<String> bad = new ArrayList<>();
        int legend = 0, god = 0, genesis = 0;
        for (String id : Wave71.IDS) {
            RelicDef d = Relics.byId(id);
            if (d == null) {
                bad.add(id + " is not registered");
                continue;
            }
            Grade g = d.grade();
            if (g == Grade.LEGEND) legend++;
            else if (g == Grade.GOD) god++;
            else if (g == Grade.GENESIS) genesis++;
            else bad.add(id + " has the grade " + g);
            if (d.slot() != RelicSlot.MAIN_HAND || !d.hasModel3d()) bad.add(id + " is not a 3D main-hand weapon");
            if (d.engravings().isEmpty()) bad.add(id + " has no Tensura engravings");
            if (d.acts().size() < 2 || d.acts().size() > 3) bad.add(id + " has " + d.acts().size() + " acts");
            if (d.passives().isEmpty() || !Wave71Passives.RULES.containsKey(id)) bad.add(id + " has no passive rule");
            int[] range = GradeDamage.range(g.name().toLowerCase(java.util.Locale.ROOT));
            if (d.meleeDamage() < range[0] || d.meleeDamage() > range[1]) bad.add(id + " damage " + d.meleeDamage() + " is outside " + range[0] + "-" + range[1]);
            for (String file : new String[]{"weapon_meshes/" + id + ".txt", "textures/entity/weapon/" + id + ".png", "textures/entity/weapon/" + id + "_glow.png",
                    "textures/item/" + id + ".png", "models/item/" + id + ".json", "models/item/" + id + "_icon.json"}) {
                if (Wave71SelfTest.class.getResource("/assets/nusmp/" + file) == null) bad.add(id + ": " + file + " is not in the jar");
            }
        }
        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problems, first: " + bad.get(0));
        if (legend < 12 || god < 12 || genesis < 10) return SelfTests.Result.fail("grades " + legend + "/" + god + "/" + genesis + " (wanted at least 12/12/10)");
        return SelfTests.Result.ok(Wave71.IDS.length + " weapons: " + legend + " Legend, " + god + " God, " + genesis + " Genesis");
    }
}
