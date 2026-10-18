package com.newuniverse.nusmp.heroweapon.weapon.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

/**
 * Why: the weapons' own forms tests are only as good as {@link FormsFileCheck}, so the checker has a negative control: a small file that passes, and the same
 * file broken one way at a time, each of which must be reported. Without it a checker that accepted everything would let a wrong skill id or an impossible
 * requirement through to the game.
 */
class FormsFileCheckTest {
    private static final FormsFileCheck.Rules RULES = new FormsFileCheck.Rules("vassal_demo", Set.of("demo.a", "demo.b", "demo.c", "demo.ult"), Set.of("demo.ult"), Set.of("p1", "p2"));

    private static String form(String id, int tier, String parents, String requires, String mastery, String skills, String passives, String extra) {
        return "{\"id\":\"" + id + "\",\"name\":\"N " + id + "\",\"desc\":\"D\",\"tier\":" + tier + ",\"parents\":" + parents + ",\"requires\":" + requires + ",\"mastery\":" + mastery
                + ",\"stats\":{\"max_health\":2.0},\"abilityBonus\":{},\"skills\":" + skills + ",\"passives\":" + passives + ",\"look\":\"base\",\"curse\":false" + extra + "}";
    }

    private static final String SKILLS_A = "{\"use\":\"demo.a\",\"sneak\":\"demo.b\",\"key1\":\"demo.c\"}";
    private static final String BASE = form("base", 0, "[]", "[]", "0", SKILLS_A, "[\"p1\"]", "");
    private static final String ONE = form("one", 1, "[\"base\"]", "[{\"item\":\"minecraft:stick\",\"count\":32}]", "100", "{\"use\":\"demo.a\",\"sneak\":\"demo.b\",\"key1\":\"demo.c\"}", "[\"p1\",\"p2\"]", "");
    private static final String TWO = form("two", 2, "[\"one\"]", "[{\"tag\":\"minecraft:logs\",\"count\":16}]", "150", "{\"use\":\"demo.a\",\"sneak\":\"demo.b\",\"key1\":\"demo.c\"}", "[\"p1\"]", "");
    private static final String TOP = form("top", 3, "[\"two\"]", "[{\"item\":\"minecraft:diamond\",\"count\":64},{\"item\":\"minecraft:emerald\",\"count\":64}]", "300",
            "{\"use\":\"demo.a\",\"sneak\":\"demo.b\",\"key1\":\"demo.c\",\"key2\":\"demo.ult\"}", "[\"p1\"]", "");

    private static String file(String... forms) { return "{\"weapon\":\"vassal_demo\",\"forms\":[" + String.join(",", forms) + "]}"; }

    private static boolean mentions(List<String> bad, String word) {
        for (String s : bad) if (s.contains(word)) return true;
        return false;
    }

    @Test
    void aGoodFilePasses() {
        assertEquals(List.of(), FormsFileCheck.check(file(BASE, ONE, TWO, TOP), RULES));
    }

    @Test
    void eachBreakageIsReported() {
        assertTrue(mentions(FormsFileCheck.check("not json", RULES), "not a JSON object"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE, TWO, TOP).replace("vassal_demo", "other"), RULES), "weapon"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE), RULES), "3 to 5 forms"), "two forms are too few");
        assertTrue(mentions(FormsFileCheck.check(file(ONE, BASE, TWO, TOP), RULES), "first form"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE, TWO, TOP.replace("demo.ult", "demo.unknown")), RULES), "not one of this weapon"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE, TWO, TOP.replace("\"key1\":\"demo.c\"", "\"key9\":\"demo.c\"")), RULES), "slot key9"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE, TWO, TOP.replace("\"parents\":[\"two\"]", "\"parents\":[\"nowhere\"]")), RULES), "does not exist"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE, TWO, TOP.replace("\"tier\":3", "\"tier\":2")), RULES), "lower tier"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE.replace("\"count\":32", "\"count\":2"), TWO, TOP), RULES), "outside 4.."));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE.replace("\"count\":32", "\"count\":300"), TWO, TOP), RULES), "outside 4.."));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE.replace("\"count\":32", "\"count\":5"), TWO, TOP), RULES), "tier 1 form asks for"), "a tier 1 form for 5 items is too cheap");
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE, TWO, TOP.replace("\"count\":64},{\"item\":\"minecraft:emerald\",\"count\":64}", "\"count\":8}")), RULES), "real task"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE.replace("\"item\":\"minecraft:stick\"", "\"item\":\"Stick\""), TWO, TOP), RULES), "namespaced"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE.replace("\"item\":\"minecraft:stick\"", "\"item\":\"minecraft:stick\",\"tag\":\"c:x\""), TWO, TOP), RULES), "exactly one"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE.replace("\"max_health\":2.0", "\"max_health\":40.0"), TWO, TOP), RULES), "stats.max_health"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE.replace("\"abilityBonus\":{}", "\"abilityBonus\":{\"max_health\":8.0}"), TWO, TOP), RULES), "abilityBonus.max_health"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE.replace("\"max_health\":2.0", "\"magic\":2.0"), TWO, TOP), RULES), "unknown stat"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE, TWO, TOP.replace("[\"p1\"]", "[\"p9\"]")), RULES), "not implemented"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE, TWO, TOP.replace("\"look\":\"base\"", "\"look\":\"curse\"")), RULES), "base look"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE, TWO, TOP.replace("\"curse\":false", "\"curse\":true")), RULES), "no curse series"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE, TWO, TOP.replace("\"mastery\":300", "\"mastery\":9")), RULES), "mastery"));
        assertTrue(mentions(FormsFileCheck.check(file(BASE.replace("\"requires\":[]", "\"requires\":[{\"item\":\"minecraft:stick\",\"count\":8}]"), ONE, TWO, TOP), RULES), "base form has tier 0"));
    }

    @Test
    void unusedSkillsAndPassivesAreReported() {
        String noP2 = file(BASE, ONE.replace("[\"p1\",\"p2\"]", "[\"p1\"]"), TWO, TOP);
        assertTrue(mentions(FormsFileCheck.check(noP2, RULES), "passive p2"), "p2 is implemented but never listed");
        String noUlt = file(BASE, ONE, TWO, TOP.replace(",\"key2\":\"demo.ult\"", ""));
        assertTrue(mentions(FormsFileCheck.check(noUlt, RULES), "skill demo.ult"), "the ultimate is built but never listed");
    }

    @Test
    void twoUltimatesInOneFormAreReported() {
        Set<String> two = Set.of("demo.ult", "demo.ult2");
        FormsFileCheck.Rules rules = new FormsFileCheck.Rules("vassal_demo", Set.of("demo.a", "demo.b", "demo.c", "demo.ult", "demo.ult2"), two, Set.of("p1", "p2"));
        String top = TOP.replace("\"key2\":\"demo.ult\"", "\"key2\":\"demo.ult\",\"key3\":\"demo.ult2\"");
        assertTrue(mentions(FormsFileCheck.check(file(BASE, ONE, TWO, top), rules), "2 ultimates"));
    }
}
