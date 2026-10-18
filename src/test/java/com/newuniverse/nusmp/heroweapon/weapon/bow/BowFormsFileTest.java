package com.newuniverse.nusmp.heroweapon.weapon.bow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.FormsFileCheck;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.FormsFileCheck.Spec;
import com.newuniverse.nusmp.heroweapon.weapon.pierce.SkillRow;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the Legendary Bow's forms file is the tree the owner's "absorbing materials unlocks new forms" runs on, and nothing can be played here, so the file
 * is checked against every rule of design.md section 6 (8 to 10 ordinary forms and 3 to 4 curse forms, every parent exists, requirements of common things
 * at the first tier and a real task at the top, stats inside their budgets, only this weapon's skills, one ultimate at most per form, every skill and every
 * passive in use) and against the Bow's own: no form uses the use slot (the use key is the draw), the Pride forms carry the Pride arrows and the armour
 * technique. A deliberately broken copy proves the checker rejects what it should.
 */
class BowFormsFileTest {
    private static final String FILE = "src/main/resources/data/nusmp/hero/forms/legendary_bow.json";

    private static Spec spec() {
        Set<String> ultimates = new HashSet<>();
        for (SkillRow r : BowNumbers.ROWS) if (r.kind() == SkillRow.Kind.ULTIMATE) ultimates.add(r.id());
        return new Spec(FILE, "legendary_bow", BowIds.ALL, ultimates, BowIds.PASSIVES, "pride", true, 8, 10, 3, 4);
    }

    @Test
    void theShippedFileKeepsEveryRule() throws Exception {
        List<String> bad = FormsFileCheck.problems(spec(), FormsFileCheck.read(FILE), FormsFileCheck.vanillaItems());
        assertTrue(bad.isEmpty(), "problems: " + bad);
    }

    @Test
    void theTreeHasTenOrdinaryFormsAndThreePrideForms() throws Exception {
        JsonArray forms = FormsFileCheck.read(FILE).getAsJsonArray("forms");
        int ordinary = 0, curse = 0;
        for (JsonElement e : forms) {
            if (FormsFileCheck.bool(e.getAsJsonObject(), "curse")) curse++;
            else ordinary++;
        }
        assertEquals(10, ordinary);
        assertEquals(3, curse);
    }

    @Test
    void nothingUsesTheDrawKeyAndThePridePassivesStayInTheProudForms() throws Exception {
        for (JsonElement e : FormsFileCheck.read(FILE).getAsJsonArray("forms")) {
            JsonObject f = e.getAsJsonObject();
            String id = FormsFileCheck.str(f, "id");
            assertFalse(FormsFileCheck.obj(f, "skills").has("use"), id + " uses the use slot");
            Set<String> passives = new HashSet<>(), skills = new HashSet<>();
            for (JsonElement p : FormsFileCheck.arr(f, "passives")) passives.add(p.getAsString());
            for (JsonElement s : FormsFileCheck.obj(f, "skills").asMap().values()) skills.add(s.getAsString());
            boolean curse = FormsFileCheck.bool(f, "curse");
            if (passives.contains(BowIds.PRIDE_ARROWS)) assertTrue(curse, id + " has Pride Arrows but is not a Pride form");
            if (curse) {
                assertTrue(passives.contains(BowIds.PRIDE_ARROWS), id + " is a Pride form without Pride Arrows");
                assertTrue(skills.contains(BowIds.LAW_FANATIC) && skills.contains(BowIds.BULL_OF_PHALARIS), id + " lacks the armour or the bull");
            }
            for (String s : BowIds.CURSE) if (skills.contains(s)) assertTrue(curse, id + " has a Pride technique but is not a Pride form");
        }
    }

    @Test
    void theSniperHasItsOwnSightAndTheFarthestFormsKeepIt() throws Exception {
        boolean piercer = false;
        for (JsonElement e : FormsFileCheck.read(FILE).getAsJsonArray("forms")) {
            JsonObject f = e.getAsJsonObject();
            Set<String> passives = new HashSet<>(), skills = new HashSet<>();
            for (JsonElement p : FormsFileCheck.arr(f, "passives")) passives.add(p.getAsString());
            for (JsonElement s : FormsFileCheck.obj(f, "skills").asMap().values()) skills.add(s.getAsString());
            if (passives.contains(BowIds.EAGLE_EYE)) {
                assertTrue(skills.contains(BowIds.PIERCING_SHOT) || skills.contains(BowIds.STARFALL_VOLLEY) || skills.contains(BowIds.METEOR_BOW),
                        FormsFileCheck.str(f, "id") + " has Eagle Eye but no long-range technique");
                piercer = true;
            }
        }
        assertTrue(piercer, "some form has Eagle Eye");
    }

    @Test
    void theCurseSeriesRisesInPriceAndPower() throws Exception {
        double lastCps = 0, lastAttack = 0;
        for (JsonElement e : FormsFileCheck.read(FILE).getAsJsonArray("forms")) {
            JsonObject f = e.getAsJsonObject();
            if (!FormsFileCheck.bool(f, "curse")) continue;
            double cps = FormsFileCheck.num(f, "corruptionPerSecond"), attack = FormsFileCheck.obj(f, "stats").get("attack_damage").getAsDouble();
            assertTrue(cps > lastCps, FormsFileCheck.str(f, "id") + ": each Pride form costs more corruption than the one before");
            assertTrue(attack > lastAttack, FormsFileCheck.str(f, "id") + ": each Pride form draws harder than the one before");
            lastCps = cps;
            lastAttack = attack;
        }
    }

    @Test
    void theStrongestFormNeverBreaksTheOrdinaryArrowBudget() throws Exception {
        double most = 0;
        for (JsonElement e : FormsFileCheck.read(FILE).getAsJsonArray("forms")) {
            JsonObject st = FormsFileCheck.obj(e.getAsJsonObject(), "stats");
            if (st.has("attack_damage")) most = Math.max(most, st.get("attack_damage").getAsDouble());
        }
        assertTrue(DrawMath.raw(1.0, most, BowNumbers.LAW_BONUS, BowNumbers.PRIDE_BONUS) <= BowNumbers.BASIC_MAX_RAW + 1e-9, "draw strength " + most + " must not lift a plain arrow past the budget");
    }

    @Test
    void theCheckerRejectsABrokenFile() throws Exception {
        Set<String> vanilla = FormsFileCheck.vanillaItems();
        assertTrue(FormsFileCheck.problems(spec(), FormsFileCheck.read(FILE), vanilla).isEmpty());

        JsonObject usesTheDraw = FormsFileCheck.read(FILE);
        usesTheDraw.getAsJsonArray("forms").get(1).getAsJsonObject().getAsJsonObject("skills").addProperty("use", "bow.mark_arrow");
        assertFalse(FormsFileCheck.problems(spec(), usesTheDraw, vanilla).isEmpty(), "the use slot of a bow must be caught");

        JsonObject wrongItem = FormsFileCheck.read(FILE);
        wrongItem.getAsJsonArray("forms").get(1).getAsJsonObject().getAsJsonArray("requires").get(0).getAsJsonObject().addProperty("item", "tensura:no_such_ingot");
        assertFalse(FormsFileCheck.problems(spec(), wrongItem, vanilla).isEmpty(), "an unknown Tensura item must be caught");

        JsonObject otherSin = FormsFileCheck.read(FILE);
        otherSin.getAsJsonArray("forms").get(10).getAsJsonObject().addProperty("sin", "wrath");
        assertFalse(FormsFileCheck.problems(spec(), otherSin, vanilla).isEmpty(), "the wrong sin must be caught");

        JsonObject tooMany = FormsFileCheck.read(FILE);
        tooMany.getAsJsonArray("forms").get(1).getAsJsonObject().getAsJsonArray("requires").get(0).getAsJsonObject().addProperty("count", 99);
        assertFalse(FormsFileCheck.problems(spec(), tooMany, vanilla).isEmpty(), "a count above 64 below the top must be caught");

        JsonObject cycle = FormsFileCheck.read(FILE);
        cycle.getAsJsonArray("forms").get(1).getAsJsonObject().getAsJsonArray("parents").add("sagittarius");
        assertFalse(FormsFileCheck.problems(spec(), cycle, vanilla).isEmpty(), "a parent that comes later must be caught");
    }
}
