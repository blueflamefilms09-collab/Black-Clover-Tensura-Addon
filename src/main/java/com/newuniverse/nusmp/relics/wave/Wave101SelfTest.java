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
 * The self-test {@code relic.genesis_new} (run by the CI server smoke test, see {@link SelfTests}): the 15 relics of wave 101 are all registered as Genesis grade relics, the seven Millennium Items
 * sit in seven different slots and need no attunement (so all seven can be worn together), the eight others are of eight franchises with their slots, each has passives and acts, the three
 * weapons are 3D main-hand relics with Tensura engravings, a passive rule and a base damage inside the Genesis range, and the files each needs are in the jar (sprite, both item models, and for a
 * weapon its mesh and skins). It also checks the pure rules of the set bonus on the loaded classes.
 */
final class Wave101SelfTest {
    private Wave101SelfTest() {}

    static void register() { SelfTests.register("relic.genesis_new", server -> run()); }

    private static final RelicSlot[] SLOTS = {RelicSlot.NECK, RelicSlot.HEAD, RelicSlot.HANDS, RelicSlot.MAIN_HAND, RelicSlot.BELT, RelicSlot.CHARM, RelicSlot.CHEST,
            RelicSlot.MAIN_HAND, RelicSlot.MAIN_HAND, RelicSlot.BACK, RelicSlot.CHARM, RelicSlot.BELT, RelicSlot.HEAD, RelicSlot.CHEST, RelicSlot.CHARM};

    static SelfTests.Result run() {
        List<String> bad = new ArrayList<>();
        int genesis = 0, weapons = 0;
        java.util.Set<RelicSlot> millenniumSlots = new java.util.HashSet<>();
        for (int i = 0; i < Wave101.IDS.length; i++) {
            String id = Wave101.IDS[i];
            RelicDef d = Relics.byId(id);
            if (d == null) {
                bad.add(id + " is not registered");
                continue;
            }
            if (d.grade() == Grade.GENESIS) genesis++;
            else bad.add(id + " has the grade " + d.grade());
            if (d.slot() != SLOTS[i]) bad.add(id + " is in the slot " + d.slot() + ", wanted " + SLOTS[i]);
            if (d.passives().isEmpty()) bad.add(id + " has no passives");
            if (d.acts().size() < 2 || d.acts().size() > 3) bad.add(id + " has " + d.acts().size() + " acts");
            if (Wave101Rules.inSet(id)) {
                millenniumSlots.add(d.slot());
                if (d.needsAttunement()) bad.add(id + " needs attunement, so the whole set could not be worn together");
            }
            String[] files = {"textures/item/" + id + ".png", "models/item/" + id + ".json", "models/item/" + id + "_icon.json"};
            for (String file : files) if (Wave101SelfTest.class.getResource("/assets/nusmp/" + file) == null) bad.add(id + ": " + file + " is not in the jar");
            if (d.slot() == RelicSlot.MAIN_HAND) {
                weapons++;
                if (!d.hasModel3d()) bad.add(id + " has no 3D model");
                if (d.engravings().isEmpty()) bad.add(id + " has no Tensura engravings");
                if (!Wave101Passives.RULES.containsKey(id)) bad.add(id + " has no passive rule");
                int[] range = GradeDamage.range("genesis");
                if (d.meleeDamage() < range[0] || d.meleeDamage() > range[1]) bad.add(id + " damage " + d.meleeDamage() + " is outside " + range[0] + "-" + range[1]);
                for (String file : new String[]{"weapon_meshes/" + id + ".txt", "textures/entity/weapon/" + id + ".png", "textures/entity/weapon/" + id + "_glow.png"}) {
                    if (Wave101SelfTest.class.getResource("/assets/nusmp/" + file) == null) bad.add(id + ": " + file + " is not in the jar");
                }
            }
        }
        if (millenniumSlots.size() != 7) bad.add("the seven Millennium Items sit in " + millenniumSlots.size() + " different slots, wanted seven");
        if (Wave101Rules.SET.length != 7 || Wave101Rules.power(7) <= Wave101Rules.power(1)) bad.add("the set bonus rules are not what the doc says");
        if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problems, first: " + bad.get(0));
        if (genesis != 15 || weapons != 3) return SelfTests.Result.fail("counts " + genesis + " Genesis, " + weapons + " weapons (wanted 15 and 3)");
        return SelfTests.Result.ok("15 Genesis relics: seven Millennium Items in seven slots, eight others, " + weapons + " weapons");
    }
}
