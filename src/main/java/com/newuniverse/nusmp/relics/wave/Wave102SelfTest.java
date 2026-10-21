package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.GradeDamage;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The self-test {@code relic.god_special_new} (run by the CI server smoke test, see {@link SelfTests}): the 29 relics of wave 102 are all registered, nine of God grade and twenty of Special
 * grade; each weapon is a 3D main-hand relic with Tensura engravings, a passive rule, acts and a base damage inside the owner's range for its grade; each piece of gear is a worn relic of
 * its grade with a passive or an act; and the files each needs are in the jar (item models and sprite for all, mesh and skins for a weapon, the effect table for a God piece of gear).
 */
final class Wave102SelfTest {
    private Wave102SelfTest() {}

    static void register() { SelfTests.register("relic.god_special_new", server -> run()); }

    static SelfTests.Result run() {
        List<String> bad = new ArrayList<>();
        int god = 0, special = 0;
        for (String id : Wave102.IDS) {
            RelicDef d = Relics.byId(id);
            if (d == null) {
                bad.add(id + " is not registered");
                continue;
            }
            Grade g = d.grade();
            if (g == Grade.GOD) god++;
            else if (g == Grade.SPECIAL) special++;
            else bad.add(id + " has the grade " + g);
            boolean weapon = d.slot() == RelicSlot.MAIN_HAND;
            if (d.acts().isEmpty()) bad.add(id + " has no act");
            if (weapon) {
                if (!d.hasModel3d()) bad.add(id + " is a weapon without a 3D model");
                if (d.engravings().isEmpty()) bad.add(id + " has no Tensura engravings");
                if (!Wave102Passives.RULES.containsKey(id)) bad.add(id + " has no passive rule");
                int[] range = GradeDamage.range(g.name().toLowerCase(Locale.ROOT));
                if (d.meleeDamage() < range[0] || d.meleeDamage() > range[1]) bad.add(id + " damage " + d.meleeDamage() + " is outside " + range[0] + "-" + range[1]);
                for (String file : new String[]{"weapon_meshes/" + id + ".txt", "textures/entity/weapon/" + id + ".png", "textures/entity/weapon/" + id + "_glow.png"}) {
                    if (Wave102SelfTest.class.getResource("/assets/nusmp/" + file) == null) bad.add(id + ": " + file + " is not in the jar");
                }
            } else if (d.passives().isEmpty()) {
                bad.add(id + " is a piece of gear without a passive");
            }
            if (d.needsAttunement() != (g == Grade.GOD)) bad.add(id + " attunement is wrong for its grade");
            for (String file : new String[]{"textures/item/" + id + ".png", "models/item/" + id + ".json", "models/item/" + id + "_icon.json"}) {
                if (Wave102SelfTest.class.getResource("/assets/nusmp/" + file) == null) bad.add(id + ": " + file + " is not in the jar");
            }
            if (!weapon && g == Grade.GOD && Wave102SelfTest.class.getResource("/assets/nusmp/visual_upgrade/" + id + ".json") == null) bad.add(id + ": its effect table is not in the jar");
        }
        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problems, first: " + bad.get(0));
        if (god != 9 || special != 20) return SelfTests.Result.fail("grades " + god + " God / " + special + " Special (wanted 9 / 20)");
        return SelfTests.Result.ok(Wave102.IDS.length + " relics: " + god + " God, " + special + " Special");
    }
}
