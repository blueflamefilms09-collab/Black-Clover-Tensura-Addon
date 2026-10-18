package com.newuniverse.nusmp.heroweapon.weapon.strike;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: {@link FormsCheck} is the only judge of the four forms files this builder can run before the game does, so it must accept a correct file and refuse each way a file can be
 * wrong. A small valid file is built here, then broken one rule at a time (a negative control per rule): every break must be reported, and the valid file must report nothing.
 */
class FormsCheckTest {
    private static final FormsCheck.Known KNOWN = new FormsCheck.Known("vassal_test", Set.of("test.a", "test.b", "test.ult"), Set.of("test.ult"), Set.of("p_one", "p_two"), 2, 3, false);

    private static final String BASE = "{\"id\":\"base\",\"name\":\"Small Test\",\"desc\":\"x\",\"tier\":0,\"parents\":[],\"requires\":[],\"mastery\":0,\"stats\":{},\"abilityBonus\":{},"
            + "\"skills\":{\"use\":\"test.a\"},\"passives\":[\"p_one\"],\"look\":\"base\",\"curse\":false,\"sin\":null,\"corruptionPerSecond\":0}";
    private static final String SECOND = "{\"id\":\"second\",\"name\":\"Second\",\"desc\":\"x\",\"tier\":1,\"parents\":[\"base\"],\"requires\":[{\"item\":\"minecraft:cobblestone\",\"count\":48}],"
            + "\"mastery\":100,\"stats\":{\"armor\":1.0,\"attack_speed\":-0.5},\"abilityBonus\":{\"armor\":0.25},"
            + "\"skills\":{\"use\":\"test.a\",\"sneak\":\"test.b\",\"key1\":\"test.ult\"},\"passives\":[\"p_two\"],\"look\":\"base\",\"curse\":false,\"sin\":null,\"corruptionPerSecond\":0}";
    private static final String DARK = "{\"id\":\"dark\",\"name\":\"Dark\",\"desc\":\"x\",\"tier\":2,\"parents\":[\"second\"],\"requires\":[{\"item\":\"minecraft:crying_obsidian\",\"count\":8}],"
            + "\"mastery\":150,\"stats\":{},\"abilityBonus\":{},\"skills\":{\"use\":\"test.a\"},\"passives\":[],\"look\":\"base\",\"curse\":true,\"sin\":\"wrath\",\"corruptionPerSecond\":5}";

    private static String file(String... forms) { return "{\"weapon\":\"vassal_test\",\"forms\":[" + String.join(",", forms) + "]}"; }

    private static List<String> check(String json) { return FormsCheck.problems(JsonParser.parseString(json).getAsJsonObject(), KNOWN); }

    private static String broken(String form, String from, String to) {
        assertTrue(form.contains(from), "the sample must contain " + from);
        return form.replace(from, to);
    }

    @Test
    void aCorrectFilePasses() {
        assertEquals(List.of(), check(file(BASE, SECOND)));
    }

    @Test
    void eachRuleCanSayNo() {
        assertFalse(check(file(BASE, SECOND).replace("vassal_test", "other")).isEmpty(), "wrong weapon id");
        assertFalse(check(file(broken(BASE, "\"id\":\"base\"", "\"id\":\"start\""), SECOND)).isEmpty(), "first form must be base");
        assertFalse(check(file(BASE, broken(SECOND, "\"id\":\"second\"", "\"id\":\"Second Form\""))).isEmpty(), "id must be snake case");
        assertFalse(check(file(BASE, broken(SECOND, "\"parents\":[\"base\"]", "\"parents\":[\"nothing\"]"))).isEmpty(), "missing parent");
        assertFalse(check(file(BASE, broken(SECOND, "\"tier\":1", "\"tier\":0"))).isEmpty(), "parent must sit on a lower tier");
        assertFalse(check(file(BASE, broken(SECOND, "\"count\":48", "\"count\":3"))).isEmpty(), "count below 4");
        assertFalse(check(file(BASE, broken(SECOND, "\"count\":48", "\"count\":65"))).isEmpty(), "count above 64 on a low tier");
        assertFalse(check(file(BASE, broken(SECOND, "{\"item\":\"minecraft:cobblestone\",\"count\":48}", ""))).isEmpty(), "no requirement");
        assertFalse(check(file(BASE, broken(SECOND, "\"item\":\"minecraft:cobblestone\"", "\"item\":\"cobblestone\""))).isEmpty(), "not namespaced");
        assertFalse(check(file(BASE, broken(SECOND, "\"item\":\"minecraft:cobblestone\"", "\"item\":\"minecraft:cobblestone\",\"tag\":\"minecraft:logs\""))).isEmpty(), "item and tag together");
        assertFalse(check(file(BASE, broken(SECOND, "\"mastery\":100", "\"mastery\":20"))).isEmpty(), "mastery below 40");
        assertFalse(check(file(BASE, broken(SECOND, "\"mastery\":100", "\"mastery\":401"))).isEmpty(), "mastery above 400");
        assertFalse(check(file(BASE, broken(SECOND, "\"armor\":1.0", "\"armor\":9.0"))).isEmpty(), "armor above the budget");
        assertFalse(check(file(BASE, broken(SECOND, "\"attack_speed\":-0.5", "\"attack_speed\":-3.5"))).isEmpty(), "attack speed below -3");
        assertFalse(check(file(BASE, broken(SECOND, "\"attack_speed\":-0.5", "\"flying\":1.0"))).isEmpty(), "unknown stat key");
        assertFalse(check(file(BASE, broken(SECOND, "\"abilityBonus\":{\"armor\":0.25}", "\"abilityBonus\":{\"armor\":2.5}"))).isEmpty(), "bonus above a quarter of the budget");
        assertFalse(check(file(BASE, broken(SECOND, "\"key1\":\"test.ult\"", "\"key9\":\"test.ult\""))).isEmpty(), "unknown slot");
        assertFalse(check(file(BASE, broken(SECOND, "\"sneak\":\"test.b\"", "\"sneak\":\"test.zzz\""))).isEmpty(), "unknown skill");
        assertFalse(check(file(BASE, broken(SECOND, "\"sneak\":\"test.b\"", "\"sneak\":\"test.a\""))).isEmpty(), "the same skill on two slots");
        assertFalse(check(file(BASE, broken(SECOND, "\"p_two\"", "\"p_zzz\""))).isEmpty(), "unknown passive");
        assertFalse(check(file(BASE, broken(SECOND, "\"look\":\"base\"", "\"look\":\"shiny\""))).isEmpty(), "unknown look");
        assertFalse(check(file(BASE, broken(SECOND, "\"curse\":false,\"sin\":null", "\"curse\":false,\"sin\":\"wrath\""))).isEmpty(), "an ordinary form with a sin");
    }

    @Test
    void anUnreachableSkillAndATooSmallTreeAreReported() {
        String noUlt = broken(SECOND, ",\"key1\":\"test.ult\"", "");
        assertTrue(check(file(BASE, noUlt)).stream().anyMatch(s -> s.contains("test.ult")), "test.ult is used by no form");
        JsonObject onlyBase = JsonParser.parseString(file(BASE, SECOND)).getAsJsonObject();
        onlyBase.getAsJsonArray("forms").remove(1);
        assertTrue(FormsCheck.problems(onlyBase, KNOWN).stream().anyMatch(s -> s.contains("ordinary forms")), "one form is below the minimum of two");
    }

    @Test
    void twoUltimatesInOneFormAreRefused() {
        FormsCheck.Known two = new FormsCheck.Known("vassal_test", Set.of("test.a", "test.b", "test.ult"), Set.of("test.ult", "test.b"), Set.of("p_one", "p_two"), 2, 3, false);
        assertFalse(FormsCheck.problems(JsonParser.parseString(file(BASE, SECOND)).getAsJsonObject(), two).isEmpty(), "test.b and test.ult are both ultimates on one form");
    }

    @Test
    void aCurseFormNeedsASinAndTheWeaponMustAllowIt() {
        FormsCheck.Known allowing = new FormsCheck.Known("vassal_test", KNOWN.skillIds(), KNOWN.ultimateIds(), KNOWN.passives(), 2, 3, true);
        assertEquals(List.of(), FormsCheck.problems(JsonParser.parseString(file(BASE, SECOND, DARK)).getAsJsonObject(), allowing));
        assertFalse(check(file(BASE, SECOND, DARK)).isEmpty(), "this weapon carries no curse series");
        assertFalse(FormsCheck.problems(JsonParser.parseString(file(BASE, SECOND, broken(DARK, "\"sin\":\"wrath\"", "\"sin\":\"vanity\""))).getAsJsonObject(), allowing).isEmpty(), "unknown sin");
        assertFalse(FormsCheck.problems(JsonParser.parseString(file(BASE, SECOND, broken(DARK, "\"corruptionPerSecond\":5", "\"corruptionPerSecond\":0"))).getAsJsonObject(), allowing).isEmpty(), "a curse form must corrupt");
    }

    @Test
    void aCurseFormMayAskForNothingButAnOrdinaryFormMayNot() {
        FormsCheck.Known allowing = new FormsCheck.Known("vassal_test", KNOWN.skillIds(), KNOWN.ultimateIds(), KNOWN.passives(), 2, 3, true);
        String free = broken(DARK, "\"requires\":[{\"item\":\"minecraft:crying_obsidian\",\"count\":8}]", "\"requires\":[]");
        assertEquals(List.of(), FormsCheck.problems(JsonParser.parseString(file(BASE, SECOND, free)).getAsJsonObject(), allowing));
        assertFalse(check(file(BASE, broken(SECOND, "\"requires\":[{\"item\":\"minecraft:cobblestone\",\"count\":48}]", "\"requires\":[]"))).isEmpty(), "an ordinary form needs something");
    }

    @Test
    void aTierIsOneDeeperThanItsDeepestParent() {
        assertFalse(check(file(BASE, broken(SECOND, "\"tier\":1", "\"tier\":2"))).isEmpty(), "a tier may not skip a level");
        assertFalse(check(file(BASE, broken(SECOND, "\"tier\":1", "\"tier\":0"))).isEmpty(), "nor sit level with its parent");
    }

    @Test
    void theSameRequirementTwiceIsRefused() {
        String twice = broken(SECOND, "\"requires\":[{\"item\":\"minecraft:cobblestone\",\"count\":48}]",
                "\"requires\":[{\"item\":\"minecraft:cobblestone\",\"count\":48},{\"item\":\"minecraft:cobblestone\",\"count\":16}]");
        assertFalse(check(file(BASE, twice)).isEmpty());
    }
}
