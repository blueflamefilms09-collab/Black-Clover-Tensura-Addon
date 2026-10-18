package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the checker guards the forms files of five weapons, so it is proved itself, with a good sample and one wrong sample for every rule it enforces (the negative controls). */
class FormsFileTest {
    private static final FormsFile.Expect WANT = new FormsFile.Expect("vassal_test", Set.of("test.a", "test.b", "test.ult"), Set.of("test.ult"), Set.of("envy"), 2, 4, 0, 2);

    private static final String GOOD = """
            { "weapon": "vassal_test", "forms": [
              { "id": "base", "name": "Plain", "desc": "d", "tier": 0, "parents": [], "requires": [], "mastery": 0,
                "stats": { "armor": 1.0 }, "abilityBonus": {}, "skills": { "use": "test.a" }, "passives": [], "look": "base", "curse": false },
              { "id": "second", "name": "Second", "desc": "d", "tier": 1, "parents": ["base"],
                "requires": [ { "item": "minecraft:glass", "count": 32 }, { "tag": "minecraft:wool", "count": 16 } ], "mastery": 80,
                "stats": { "armor": 2.0, "movement_speed": 0.05 }, "abilityBonus": { "armor": 0.5 },
                "skills": { "use": "test.a", "key1": "test.b", "key3": "test.ult" }, "passives": ["calm"], "look": "base", "curse": false },
              { "id": "dark", "name": "Dark", "desc": "d", "tier": 1, "parents": ["base"],
                "requires": [ { "item": "minecraft:emerald", "count": 24 } ], "mastery": 120,
                "stats": {}, "abilityBonus": {}, "skills": { "use": "test.a" }, "passives": [], "look": "base", "curse": true, "sin": "envy", "corruptionPerSecond": 4 }
            ] }
            """;

    private static JsonObject parse(String text) { return JsonParser.parseString(text).getAsJsonObject(); }

    private static boolean flags(String json, String word) {
        for (String s : FormsFile.check(parse(json), WANT)) if (s.contains(word)) return true;
        return false;
    }

    private static String broken(String from, String to) {
        assertTrue(GOOD.contains(from), "the sample does not contain: " + from);
        return GOOD.replace(from, to);
    }

    @Test
    void theGoodSamplePasses() {
        List<String> bad = FormsFile.check(parse(GOOD), WANT);
        assertTrue(bad.isEmpty(), bad.toString());
    }

    @Test
    void theItemsTagsAndSkillsAreListedForTheRegistryChecks() {
        assertEquals(List.of("minecraft:glass", "minecraft:emerald"), FormsFile.itemIds(parse(GOOD)));
        assertEquals(List.of("minecraft:wool"), FormsFile.tags(parse(GOOD)));
        assertEquals(List.of("test.a", "test.b", "test.ult"), FormsFile.skillIds(parse(GOOD)));
    }

    @Test
    void theFirstFormMustBeTheBase() {
        assertTrue(flags(broken("\"id\": \"base\"", "\"id\": \"first\""), "base"));
    }

    @Test
    void aParentMustBeAnEarlierForm() {
        assertTrue(flags(broken("\"parents\": [\"base\"]", "\"parents\": [\"ghost\"]"), "ghost"));
    }

    @Test
    void aTierMustBeOneDeeperThanItsDeepestParent() {
        assertTrue(flags(broken("\"tier\": 1", "\"tier\": 2"), "tier 2 but"));
    }

    @Test
    void requirementCountsAreFourToSixtyFourBelowTheTopTiers() {
        assertTrue(flags(broken("\"count\": 32", "\"count\": 3"), "count"));
        assertTrue(flags(broken("\"count\": 32", "\"count\": 65"), "count"));
        assertTrue(flags(broken("\"count\": 32", "\"count\": 4"), "count") == false);
    }

    @Test
    void aRequirementNamesExactlyOneOfItemAndTag() {
        assertTrue(flags(broken("{ \"item\": \"minecraft:glass\", \"count\": 32 }", "{ \"item\": \"minecraft:glass\", \"tag\": \"minecraft:wool\", \"count\": 32 }"), "exactly one"));
    }

    @Test
    void aFormAfterTheBaseNeedsOneToFourRequirements() {
        assertTrue(flags(broken("\"requires\": [ { \"item\": \"minecraft:emerald\", \"count\": 24 } ]", "\"requires\": []"), "requirements"));
    }

    @Test
    void masteryIsFortyToFourHundred() {
        assertTrue(flags(broken("\"mastery\": 80", "\"mastery\": 39"), "mastery"));
        assertTrue(flags(broken("\"mastery\": 80", "\"mastery\": 401"), "mastery"));
    }

    @Test
    void statsStayInsideTheBudgetAndAbilityBonusInsideAQuarterOfIt() {
        assertTrue(flags(broken("\"armor\": 2.0", "\"armor\": 8.5"), "armor"));
        assertTrue(flags(broken("\"armor\": 0.5 }", "\"armor\": 2.5 }"), "armor"));
        assertTrue(flags(broken("\"movement_speed\": 0.05", "\"mana\": 0.05"), "unknown key"));
    }

    @Test
    void aSlotMustBeKnownAndItsSkillMustExist() {
        assertTrue(flags(broken("\"key1\": \"test.b\"", "\"key9\": \"test.b\""), "slot"));
        assertTrue(flags(broken("\"key1\": \"test.b\"", "\"key1\": \"other.b\""), "not a skill"));
    }

    @Test
    void aFormHoldsAtMostOneUltimate() {
        Set<String> two = Set.of("test.ult", "test.a");
        FormsFile.Expect both = new FormsFile.Expect("vassal_test", Set.of("test.a", "test.b", "test.ult"), two, Set.of("envy"), 2, 4, 0, 2);
        boolean found = false;
        for (String s : FormsFile.check(parse(GOOD), both)) found |= s.contains("ultimates");
        assertTrue(found);
    }

    @Test
    void aCurseFormNeedsAKnownSinAndCorruptionAndOthersNeitherOfThem() {
        assertTrue(flags(broken("\"sin\": \"envy\"", "\"sin\": \"wrath\""), "sin"));
        assertTrue(flags(broken("\"corruptionPerSecond\": 4", "\"corruptionPerSecond\": 0"), "corruptionPerSecond"));
        assertTrue(flags(broken("\"stats\": { \"armor\": 1.0 }", "\"sin\": \"envy\", \"stats\": { \"armor\": 1.0 }"), "no sin"));
    }

    @Test
    void theLookIsBaseOrCurse() {
        assertTrue(flags(broken("\"look\": \"base\", \"curse\": true", "\"look\": \"red\", \"curse\": true"), "look"));
    }

    @Test
    void everySkillOfTheWeaponMustBeInSomeForm() {
        FormsFile.Expect more = new FormsFile.Expect("vassal_test", Set.of("test.a", "test.b", "test.ult", "test.lost"), Set.of("test.ult"), Set.of("envy"), 2, 4, 0, 2);
        boolean found = false;
        for (String s : FormsFile.check(parse(GOOD), more)) found |= s.contains("test.lost");
        assertTrue(found);
    }

    @Test
    void theNumberOfFormsIsChecked() {
        FormsFile.Expect few = new FormsFile.Expect("vassal_test", Set.of("test.a", "test.b", "test.ult"), Set.of("test.ult"), Set.of("envy"), 3, 4, 2, 3);
        int n = 0;
        for (String s : FormsFile.check(parse(GOOD), few)) if (s.contains("forms, expected")) n++;
        assertEquals(2, n, "both the ordinary and the curse count are outside their range");
    }

    @Test
    void aWrongWeaponNameIsRefused() {
        assertTrue(flags(broken("\"weapon\": \"vassal_test\"", "\"weapon\": \"vassal_other\""), "names the weapon"));
    }
}
