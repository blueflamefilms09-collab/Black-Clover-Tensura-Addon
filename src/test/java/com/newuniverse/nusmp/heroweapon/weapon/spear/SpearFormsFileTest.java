package com.newuniverse.nusmp.heroweapon.weapon.spear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.FormsFileCheck;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.FormsFileCheck.Spec;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the Legendary Spear's forms file is the tree the owner's "absorbing materials unlocks new forms" runs on, and nothing can be played here, so the file
 * is checked against every rule of design.md section 6 (8 to 10 ordinary forms and 3 to 4 curse forms, every parent exists, requirements of common things
 * at the first tier and a real task at the top, stats inside their budgets, only this weapon's skills, one ultimate at most per form, every skill and every
 * passive in use) and against the Spear's own: Time Reversal is in a form that has the Memory of the Hours, the Lust forms carry the Lust passives.
 * A deliberately broken copy proves the checker rejects what it should.
 */
class SpearFormsFileTest {
    private static final String FILE = "src/main/resources/data/nusmp/hero/forms/legendary_spear.json";

    private static Spec spec() {
        Set<String> ultimates = new HashSet<>();
        for (SkillRow r : SpearNumbers.ROWS) if (r.kind() == SkillRow.Kind.ULTIMATE) ultimates.add(r.id());
        return new Spec(FILE, "legendary_spear", SpearIds.ALL, ultimates, SpearIds.PASSIVES, "lust", false, 8, 10, 3, 4);
    }

    @Test
    void theShippedFileKeepsEveryRule() throws Exception {
        List<String> bad = FormsFileCheck.problems(spec(), FormsFileCheck.read(FILE), FormsFileCheck.vanillaItems());
        assertTrue(bad.isEmpty(), "problems: " + bad);
    }

    @Test
    void theTreeHasTenOrdinaryFormsAndFourLustForms() throws Exception {
        JsonArray forms = FormsFileCheck.read(FILE).getAsJsonArray("forms");
        int ordinary = 0, curse = 0;
        for (JsonElement e : forms) {
            if (FormsFileCheck.bool(e.getAsJsonObject(), "curse")) curse++;
            else ordinary++;
        }
        assertEquals(10, ordinary);
        assertEquals(4, curse);
    }

    @Test
    void timeReversalIsOnlyInAFormWithTheMemoryOfTheHours() throws Exception {
        boolean found = false;
        for (JsonElement e : FormsFileCheck.read(FILE).getAsJsonArray("forms")) {
            JsonObject f = e.getAsJsonObject();
            boolean has = false;
            for (JsonElement s : FormsFileCheck.obj(f, "skills").asMap().values()) if (SpearIds.TIME_REVERSAL.equals(s.getAsString())) has = true;
            if (!has) continue;
            found = true;
            boolean memory = false;
            for (JsonElement p : FormsFileCheck.arr(f, "passives")) if (SpearIds.TIME_MEMORY.equals(p.getAsString())) memory = true;
            assertTrue(memory, FormsFileCheck.str(f, "id") + " lists Time Reversal but not time_memory, so nothing would be remembered");
            assertTrue(FormsFileCheck.num(f, "tier") >= 5, "Time Reversal is the ultimate of a high form");
        }
        assertTrue(found, "some form has Time Reversal");
    }

    @Test
    void thePassivesAreOnTheFormsThatUseThem() throws Exception {
        for (JsonElement e : FormsFileCheck.read(FILE).getAsJsonArray("forms")) {
            JsonObject f = e.getAsJsonObject();
            Set<String> passives = new HashSet<>(), skills = new HashSet<>();
            for (JsonElement p : FormsFileCheck.arr(f, "passives")) passives.add(p.getAsString());
            for (JsonElement s : FormsFileCheck.obj(f, "skills").asMap().values()) skills.add(s.getAsString());
            String id = FormsFileCheck.str(f, "id");
            boolean curse = FormsFileCheck.bool(f, "curse");
            if (passives.contains(SpearIds.PORTAL_ECHO)) assertTrue(skills.contains(SpearIds.PORTAL_SPEAR), id + " has Portal Echo but no Portal Spear");
            if (passives.contains(SpearIds.LONGSHAFT)) assertTrue(skills.contains(SpearIds.PIERCING_THRUST), id + " has Longshaft but no thrust");
            if (passives.contains(SpearIds.LUST_FERVOR) || passives.contains(SpearIds.RAVENOUS_AIM)) assertTrue(curse, id + " has a Lust passive but is not a Lust form");
            if (curse) assertTrue(passives.contains(SpearIds.LUST_FERVOR) && passives.contains(SpearIds.RAVENOUS_AIM), id + " is a Lust form without both Lust passives");
            if (skills.contains(SpearIds.TEMPTATION) || skills.contains(SpearIds.RAPTURE) || skills.contains(SpearIds.THORN_CHAINS) || skills.contains(SpearIds.LUSTS_EMBRACE))
                assertTrue(curse, id + " has a Lust technique but is not a Lust form");
        }
    }

    @Test
    void theCurseSeriesRisesInPriceAndPower() throws Exception {
        double lastCps = 0, lastAttack = 0;
        for (JsonElement e : FormsFileCheck.read(FILE).getAsJsonArray("forms")) {
            JsonObject f = e.getAsJsonObject();
            if (!FormsFileCheck.bool(f, "curse")) continue;
            double cps = FormsFileCheck.num(f, "corruptionPerSecond"), attack = FormsFileCheck.obj(f, "stats").has("attack_damage") ? FormsFileCheck.obj(f, "stats").get("attack_damage").getAsDouble() : 0;
            assertTrue(cps > lastCps, FormsFileCheck.str(f, "id") + ": each Lust form costs more corruption than the one before");
            assertTrue(attack > lastAttack, FormsFileCheck.str(f, "id") + ": each Lust form hits harder than the one before");
            lastCps = cps;
            lastAttack = attack;
        }
    }

    @Test
    void theSpearReachesFartherTheDeeperTheTreeGoes() throws Exception {
        Map<String, Double> reach = new HashMap<>();
        double top = 0, topLust = 0;
        for (JsonElement e : FormsFileCheck.read(FILE).getAsJsonArray("forms")) {
            JsonObject f = e.getAsJsonObject();
            JsonObject stats = FormsFileCheck.obj(f, "stats");
            double r = stats.has("reach") ? stats.get("reach").getAsDouble() : 0.0;
            String id = FormsFileCheck.str(f, "id");
            reach.put(id, r);
            for (JsonElement p : FormsFileCheck.arr(f, "parents")) assertTrue(r >= reach.get(p.getAsString()) - 1e-9, id + " reaches less far than its parent " + p.getAsString());
            if (FormsFileCheck.bool(f, "curse")) topLust = Math.max(topLust, r);
            else top = Math.max(top, r);
        }
        assertTrue(top >= 1.5, "the owner asked for the longest reach of any melee weapon: at least 1.5 blocks more at the top, this one has " + top);
        assertTrue(topLust >= 1.5, "the Lust Spear is just as long: " + topLust);
        assertTrue(top <= 2.0 && topLust <= 2.0, "the budget of design.md section 6 allows at most 2.0");
    }

    @Test
    void theCheckerRejectsABrokenFile() throws Exception {
        Set<String> vanilla = FormsFileCheck.vanillaItems();
        JsonObject root = FormsFileCheck.read(FILE);
        assertTrue(FormsFileCheck.problems(spec(), root, vanilla).isEmpty());

        JsonObject wrongItem = FormsFileCheck.read(FILE);
        wrongItem.getAsJsonArray("forms").get(1).getAsJsonObject().getAsJsonArray("requires").get(0).getAsJsonObject().addProperty("item", "minecraft:no_such_item");
        assertFalse(FormsFileCheck.problems(spec(), wrongItem, vanilla).isEmpty(), "an unknown item must be caught");

        JsonObject otherWeapon = FormsFileCheck.read(FILE);
        otherWeapon.getAsJsonArray("forms").get(1).getAsJsonObject().getAsJsonObject("skills").addProperty("key3", "sword.hundred_blades");
        assertFalse(FormsFileCheck.problems(spec(), otherWeapon, vanilla).isEmpty(), "a skill of another weapon must be caught");

        JsonObject noParent = FormsFileCheck.read(FILE);
        noParent.getAsJsonArray("forms").get(2).getAsJsonObject().getAsJsonArray("parents").add("ghost_form");
        assertFalse(FormsFileCheck.problems(spec(), noParent, vanilla).isEmpty(), "a missing parent must be caught");

        JsonObject tooStrong = FormsFileCheck.read(FILE);
        tooStrong.getAsJsonArray("forms").get(1).getAsJsonObject().getAsJsonObject("stats").addProperty("attack_damage", 40.0);
        assertFalse(FormsFileCheck.problems(spec(), tooStrong, vanilla).isEmpty(), "a stat above its budget must be caught");

        JsonObject twoUltimates = FormsFileCheck.read(FILE);
        twoUltimates.getAsJsonArray("forms").get(1).getAsJsonObject().getAsJsonObject("skills").addProperty("key2", "spear.brionac");
        twoUltimates.getAsJsonArray("forms").get(1).getAsJsonObject().getAsJsonObject("skills").addProperty("key3", "spear.time_reversal");
        assertFalse(FormsFileCheck.problems(spec(), twoUltimates, vanilla).isEmpty(), "two ultimates in one form must be caught");

        JsonObject unknownPassive = FormsFileCheck.read(FILE);
        unknownPassive.getAsJsonArray("forms").get(1).getAsJsonObject().getAsJsonArray("passives").add("invisibility_cloak");
        assertFalse(FormsFileCheck.problems(spec(), unknownPassive, vanilla).isEmpty(), "a passive nobody implements must be caught");
    }
}
