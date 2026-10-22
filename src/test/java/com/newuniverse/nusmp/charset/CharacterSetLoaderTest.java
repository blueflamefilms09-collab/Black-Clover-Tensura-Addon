package com.newuniverse.nusmp.charset;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The schema of docs/magic/character_spells.md: a good file loads, and every kind of mistake is reported (so a typo can never hide a page by accident). */
class CharacterSetLoaderTest {
    static String file(String spells) {
        return "{\"schema\":1,\"canon\":\"yuno\",\"magic\":\"WIND\",\"book\":\"book_wind\",\"sources\":{\"w\":\"wiki\"},\"spells\":[" + spells + "]}";
    }

    static String spell(String page, String acquire) {
        return "{\"page\":\"" + page + "\",\"name\":\"" + page + "\",\"origin\":\"anime\",\"source\":\"w\",\"acquire\":" + acquire + "}";
    }

    private static List<String> problems(String json) {
        List<String> bad = new ArrayList<>();
        CharacterSetLoader.parse(json, bad);
        return bad;
    }

    @Test
    void aGoodFileLoadsAndKeepsItsOrder() {
        CharacterSet s = CharacterSetLoader.parse(file(spell("a", "{\"type\":\"start\"}") + "," + spell("b", "{\"type\":\"page_uses\",\"page\":\"a\",\"count\":2}")));
        assertEquals("yuno", s.canon());
        assertEquals("book_wind", s.book());
        assertEquals(List.of("a", "b"), s.spells().stream().map(CharacterSet.Spell::page).toList());
        assertEquals(new SpellRule.PageUses("a", 2), s.spell("b").rule());
        assertFalse(s.spell("a").exclusive());
    }

    @Test
    void everyRuleTypeParses() {
        String all = "{\"type\":\"all\",\"of\":[{\"type\":\"mastery\",\"min\":0.5},{\"type\":\"trust\",\"min\":30},{\"type\":\"flag\",\"name\":\"saint_stage\"},"
                + "{\"type\":\"any\",\"of\":[{\"type\":\"page_open\",\"page\":\"a\"},{\"type\":\"page_uses\",\"page\":\"spirit_dive\",\"count\":1}]}]}";
        CharacterSet s = CharacterSetLoader.parse(file(spell("a", "{\"type\":\"start\"}") + "," + spell("b", all)));
        assertTrue(s.spell("b").rule() instanceof SpellRule.All);
    }

    @Test
    void syntaxErrorsAndMissingPartsAreReported() {
        assertFalse(problems("{ nope").isEmpty());
        assertFalse(problems("[]").isEmpty());
        assertFalse(problems("{\"schema\":1}").isEmpty());
        assertTrue(problems(file("")).stream().anyMatch(p -> p.contains("at least one spell")));
    }

    @Test
    void aWrongSchemaIsReported() {
        assertTrue(problems(file(spell("a", "{\"type\":\"start\"}")).replace("\"schema\":1", "\"schema\":2")).stream().anyMatch(p -> p.contains("schema")));
    }

    @Test
    void badRulesAreReported() {
        assertTrue(problems(file(spell("a", "{\"type\":\"mastery\",\"min\":1.5}"))).stream().anyMatch(p -> p.contains("mastery must be between")));
        assertTrue(problems(file(spell("a", "{\"type\":\"flag\",\"name\":\"nope\"}"))).stream().anyMatch(p -> p.contains("unknown flag")));
        assertTrue(problems(file(spell("a", "{\"type\":\"teleport\"}"))).stream().anyMatch(p -> p.contains("unknown acquire type")));
        assertTrue(problems(file(spell("a", "{\"type\":\"page_uses\",\"page\":\"spirit_dive\",\"count\":0}"))).stream().anyMatch(p -> p.contains("count")));
        assertTrue(problems(file(spell("a", "{\"type\":\"trust\",\"min\":101}"))).stream().anyMatch(p -> p.contains("trust")));
        assertTrue(problems(file(spell("a", "{\"type\":\"all\",\"of\":[]}"))).stream().anyMatch(p -> p.contains("at least one")));
        assertFalse(problems(file("{\"page\":\"a\",\"name\":\"a\",\"origin\":\"anime\",\"source\":\"w\"}")).isEmpty());
    }

    @Test
    void aRuleNamingAPageOutsideTheSetIsReported() {
        assertTrue(problems(file(spell("a", "{\"type\":\"page_open\",\"page\":\"ghost\"}"))).stream().anyMatch(p -> p.contains("not in this set")));
        assertTrue(problems(file(spell("a", "{\"type\":\"page_open\",\"page\":\"a\"}"))).stream().anyMatch(p -> p.contains("its own page")));
    }

    @Test
    void theUniversalSpiritPagesMayBeNamed() {
        assertTrue(problems(file(spell("a", "{\"type\":\"page_uses\",\"page\":\"spirit_dive\",\"count\":1}"))).isEmpty());
    }

    @Test
    void aCycleBetweenPagesIsReported() {
        String json = file(spell("a", "{\"type\":\"page_open\",\"page\":\"b\"}") + "," + spell("b", "{\"type\":\"page_uses\",\"page\":\"a\",\"count\":1}"));
        assertTrue(problems(json).stream().anyMatch(p -> p.contains("depends on itself")));
    }

    @Test
    void duplicatesBadIdsOriginsAndSourcesAreReported() {
        assertTrue(problems(file(spell("a", "{\"type\":\"start\"}") + "," + spell("a", "{\"type\":\"start\"}"))).stream().anyMatch(p -> p.contains("twice")));
        assertTrue(problems(file(spell("A b", "{\"type\":\"start\"}"))).stream().anyMatch(p -> p.contains("lower-case")));
        assertTrue(problems(file(spell("a", "{\"type\":\"start\"}").replace("\"anime\"", "\"movie\""))).stream().anyMatch(p -> p.contains("origin")));
        assertTrue(problems(file(spell("a", "{\"type\":\"start\"}").replace("\"w\"", "\"zzz\""))).stream().anyMatch(p -> p.contains("not declared")));
    }

    @Test
    void parseThrowsWithEveryProblemInTheMessage() {
        try {
            CharacterSetLoader.parse(file(spell("a", "{\"type\":\"mastery\",\"min\":2}")));
            throw new AssertionError("expected a failure");
        } catch (IllegalArgumentException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("mastery"));
        }
    }

    @Test
    void ruleNestingHasALimit() {
        String deep = "{\"type\":\"start\"}";
        for (int k = 0; k < 9; k++) deep = "{\"type\":\"all\",\"of\":[" + deep + "]}";
        assertTrue(problems(file(spell("a", deep))).stream().anyMatch(p -> p.contains("nested too deeply")));
    }
}
