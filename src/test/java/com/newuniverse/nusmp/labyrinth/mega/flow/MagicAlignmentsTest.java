package com.newuniverse.nusmp.labyrinth.mega.flow;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the table that says "this cast is Light, this one is Dark" decides who is boosted and who is drained in the Holy floors, and its order of
 * rules (a deliberate entry beats a keyword, whole words only, dark beats light, the most specific id with a deliberate entry decides alone) is the
 * thing the owner will read in the doc and rely on. These tests pin every rule on small hand-made tables, so a change to the order, to the
 * word-splitting or to the dark-wins tie fails here with the rule's name instead of showing up as a Light mage being drained in play. The shipped
 * file and the real magic families are checked in {@code AlignmentDataTest}.
 */
class MagicAlignmentsTest {
    private static List<Object> list(String... words) { return new ArrayList<>(List.of(words)); }

    private static Map<String, Object> pairs(String... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    /** A table from keyword lists and alternating exact id / alignment pairs; any complaint from the parser fails the test. */
    private static MagicAlignments table(List<Object> light, List<Object> dark, String... exact) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("light", light);
        json.put("dark", dark);
        json.put("exact", pairs(exact));
        MagicAlignments.Parsed p = MagicAlignments.parse(json);
        assertTrue(p.problems().isEmpty(), "unexpected complaints: " + p.problems());
        return p.table();
    }

    private static MagicAlignments basic() {
        return table(list("light", "holy", "angel"), list("dark", "demon", "devil", "fallen", "curse"),
                "lightning", "light", "curse_warding", "neutral", "poison", "dark", "devil_union", "dark", "tensura:megiddo", "light");
    }

    // ---------------------------------------------------------------- words
    @Test
    void tokensSplitAtEverythingButLettersAndDigits() {
        assertEquals(List.of("demon", "light"), MagicAlignments.tokens("demon_light"));
        assertEquals(List.of("tensura", "dark", "cube"), MagicAlignments.tokens("tensura:dark_cube"));
        assertEquals(List.of("ice", "wedge"), MagicAlignments.tokens("Ice Wedge"));
        assertEquals(List.of("merged", "flame", "water"), MagicAlignments.tokens("merged:flame+water"));
        assertEquals(List.of("doom", "s", "gate", "2"), MagicAlignments.tokens("doom_s_gate/2"));
        assertEquals(List.of(), MagicAlignments.tokens("___"));
        assertEquals(List.of(), MagicAlignments.tokens(null));
    }

    @Test
    void keywordsMatchWholeWordsNeverSubstrings() {
        MagicAlignments t = basic();
        assertEquals(Alignment.LIGHT, t.classify("light_arrow"));
        assertEquals(Alignment.NEUTRAL, t.classify("lighten"), "lighten is not light");
        assertEquals(Alignment.NEUTRAL, t.classify("flashlight"), "flashlight is not light");
        assertEquals(Alignment.NEUTRAL, t.classify("lightning_lance"), "lightning is not light unless an entry says so");
        assertEquals(Alignment.NEUTRAL, t.classify("darkest"), "a longer word is a different word");
        assertEquals(Alignment.DARK, t.classify("dark_cube"));
        assertEquals(Alignment.DARK, t.classify("DARK-CUBE"), "case and dashes do not matter");
    }

    @Test
    void darkBeatsLightWhenBothMatch() {
        MagicAlignments t = basic();
        assertEquals(Alignment.DARK, t.classify("demon_light"));
        assertEquals(Alignment.DARK, t.classify("light_demon"));
        assertEquals(Alignment.DARK, t.classify("fallen_angel_gate"), "angel is light, fallen is dark: dark wins");
        assertEquals(Alignment.DARK, t.classify("holy_curse"));
    }

    // ---------------------------------------------------------------- the order of rules
    @Test
    void anExactEntryBeatsEveryKeyword() {
        MagicAlignments t = basic();
        assertEquals(Alignment.NEUTRAL, t.classify("curse_warding"), "the exact entry says neutral although curse is a dark word");
        assertEquals(Alignment.DARK, t.classify("poison"), "an exact entry needs no keyword");
        assertEquals(Alignment.LIGHT, t.classify("lightning"));
        assertEquals(Alignment.LIGHT, t.classify("tensura:lightning"), "the path is tried when the full id has no entry of its own");
        assertEquals(Alignment.NEUTRAL, t.classify("curse_warding", "x_dark"), "the first id with an entry decides, so a later keyword is never heard");
    }

    @Test
    void theMostSpecificIdWithAnExactEntryDecidesAlone() {
        MagicAlignments t = basic();
        assertEquals(Alignment.DARK, t.classify("devil_union", "lightning"), "a page entry beats its family's entry");
        assertEquals(Alignment.LIGHT, t.classify("strike", "lightning"), "the family's entry decides when the page has none");
        assertEquals(Alignment.NEUTRAL, t.classify("curse_cleanse", "curse_warding"), "the family's exact entry beats the page's dark keyword");
        assertEquals(Alignment.NEUTRAL, t.classify("curse_strike", "curse_warding"));
        assertEquals(Alignment.DARK, t.classify("anything", "poison", "lightning"), "the first id with an entry wins over later entries");
        assertEquals(Alignment.LIGHT, t.classify("lightning", "poison"), "order matters: first listed, first heard");
    }

    @Test
    void keywordsOfAllIdsAreWeighedTogetherWhenNoneHasAnEntry() {
        MagicAlignments t = basic();
        assertEquals(Alignment.LIGHT, t.classify("beam", "holy_flame"));
        assertEquals(Alignment.DARK, t.classify("holy_beam", "demon_flame"), "light in one id, dark in another: dark wins");
        assertEquals(Alignment.NEUTRAL, t.classify("flame_bullet", "flame"));
        assertEquals(Alignment.DARK, t.classify("flame_bullet", "dark"), "the family's keyword colours a plain page");
    }

    @Test
    void namespacesAreIgnoredForKeywordsButTriedForExactEntries() {
        MagicAlignments t = basic();
        assertEquals(Alignment.NEUTRAL, t.classify("darkmod:flame_ball"), "a namespace that happens to say dark colours nothing");
        assertEquals(Alignment.NEUTRAL, t.classify("dark:flame_ball"), "not even a mod that is called exactly dark");
        assertEquals(Alignment.NEUTRAL, t.classify("holy:flame_ball"), "nor one that is called holy");
        assertEquals(Alignment.DARK, t.classify("tensura:dark_cube"));
        assertEquals(Alignment.LIGHT, t.classify("tensura:megiddo"), "the exact key may carry the namespace");
        assertEquals(Alignment.NEUTRAL, t.classify("other:megiddo"), "a different namespace is a different id");
    }

    @Test
    void idsAreNormalised() {
        MagicAlignments t = basic();
        assertEquals("demon_light", MagicAlignments.normalize("  Demon-Light "));
        assertEquals("demon_light", MagicAlignments.normalize("DEMON LIGHT"));
        assertEquals("", MagicAlignments.normalize(null));
        assertEquals(Alignment.DARK, t.classify("POISON"));
        assertEquals(Alignment.LIGHT, t.classify(" Lightning "));
    }

    @Test
    void mergedMagicsAreAskedPartByPartAndTheStrongestPartSpeaks() {
        MagicAlignments t = basic();
        assertEquals(Alignment.LIGHT, t.classify("merged:flame+lightning"));
        assertEquals(Alignment.DARK, t.classify("merged:lightning+poison"), "dark outranks light");
        assertEquals(Alignment.NEUTRAL, t.classify("merged:flame+water"));
        assertEquals(Alignment.NEUTRAL, t.classify("merged:curse_warding+flame"), "an exact neutral part is not a curse");
        assertEquals(Alignment.DARK, t.classify("merged:curse_warding+demon"), "a dark part colours the merge");
        assertEquals(Alignment.NEUTRAL, t.classify("merged:flame+curse_warding", "curse_cleanse"),
                "an exact neutral part is exact whatever its place in the merge, so the page's dark keyword is never heard");
        assertEquals(Alignment.NEUTRAL, t.classify("merged:curse_warding+flame", "curse_cleanse"));
        assertEquals(Alignment.LIGHT, t.classify("merged:flame+lightning", "demon_strike"), "an exact light part decides alone, before the next id's keyword");
        assertEquals(Alignment.NEUTRAL, t.classify("merged:"), "a merge of nothing is nothing");
    }

    @Test
    void nullAndBlankIdsAreNeutralAndSkipped() {
        MagicAlignments t = basic();
        assertEquals(Alignment.NEUTRAL, t.classify((String[]) null));
        assertEquals(Alignment.NEUTRAL, t.classify());
        assertEquals(Alignment.NEUTRAL, t.classify((String) null));
        assertEquals(Alignment.NEUTRAL, t.classify("", "  "));
        assertEquals(Alignment.DARK, t.classify(null, "", "poison"), "blank ids are skipped, not fatal");
    }

    @Test
    void anEmptyTableIsNeutralEverywhere() {
        MagicAlignments e = MagicAlignments.EMPTY;
        assertTrue(e.isEmpty());
        for (String id : new String[] {"light", "dark", "holy", "demon_light", "merged:light+dark", ""}) assertEquals(Alignment.NEUTRAL, e.classify(id), id);
    }

    @Test
    void explainNamesTheRuleThatSpoke() {
        MagicAlignments t = basic();
        MagicAlignments.Verdict v = t.explain("curse_cleanse", "curse_warding");
        assertEquals(Alignment.NEUTRAL, v.alignment());
        assertTrue(v.rule().contains("exact entry 'curse_warding'"), v.rule());
        v = t.explain("demon_light");
        assertEquals(Alignment.DARK, v.alignment());
        assertTrue(v.rule().contains("dark wins"), v.rule());
        v = t.explain("holy_bolt");
        assertTrue(v.rule().contains("keyword 'holy' (light)"), v.rule());
        v = t.explain("flame");
        assertEquals(Alignment.NEUTRAL, v.alignment());
        assertNotNull(v.rule());
        assertTrue(t.explain("merged:flame+poison").rule().startsWith("merged magic"), t.explain("merged:flame+poison").rule());
    }

    // ---------------------------------------------------------------- parsing
    @Test
    void parseKeepsWhatIsGoodAndReportsEverythingElse() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("why", "ignored");
        json.put("light", list("light", "two words", "Holy", "light", "ho-ly"));
        json.put("dark", list("dark", "light"));
        json.put("exact", pairs("Poison", "dark", "bad id!", "dark", "x", "purple", "poison", "light"));
        json.put("extra", "x");
        MagicAlignments.Parsed p = MagicAlignments.parse(json);
        List<String> all = p.problems();
        assertTrue(all.stream().anyMatch(s -> s.contains("unknown key 'extra'")), all.toString());
        assertTrue(all.stream().anyMatch(s -> s.contains("'two words' is not one word")), all.toString());
        assertTrue(all.stream().anyMatch(s -> s.contains("'ho-ly'") || s.contains("ho_ly")), "a dash inside a word is not a word: " + all);
        assertTrue(all.stream().anyMatch(s -> s.contains("'light' is listed twice")), all.toString());
        assertTrue(all.stream().anyMatch(s -> s.contains("'light' is in both lists")), all.toString());
        assertTrue(all.stream().anyMatch(s -> s.contains("'bad id!' is not an id")), all.toString());
        assertTrue(all.stream().anyMatch(s -> s.contains("'x' must be")), all.toString());
        assertTrue(all.stream().anyMatch(s -> s.contains("'poison' is listed twice")), all.toString());
        MagicAlignments t = p.table();
        assertEquals(List.of("light", "holy"), new ArrayList<>(t.lightKeywords()), "the good words are kept, in order");
        assertEquals(List.of("dark", "light"), new ArrayList<>(t.darkKeywords()));
        assertEquals(Alignment.LIGHT, t.exactEntries().get("poison"), "the later duplicate wins");
        assertEquals(1, t.exactEntries().size());
    }

    @Test
    void parseNeverThrowsOnOddInput() {
        assertTrue(MagicAlignments.parse(null).table().isEmpty());
        assertEquals(1, MagicAlignments.parse(null).problems().size());
        Map<String, Object> odd = new LinkedHashMap<>();
        odd.put("light", "not a list");
        odd.put("dark", new ArrayList<Object>(java.util.Arrays.asList(1.5, null, Boolean.TRUE, List.of("nested"))));
        odd.put("exact", List.of("not", "an", "object"));
        MagicAlignments.Parsed p = MagicAlignments.parse(odd);
        assertTrue(p.table().isEmpty());
        assertTrue(p.problems().size() >= 6, p.problems().toString());
        Map<String, Object> noneAtAll = new LinkedHashMap<>();
        MagicAlignments.Parsed q = MagicAlignments.parse(noneAtAll);
        assertTrue(q.problems().isEmpty(), "an empty object is a valid, empty table");
        assertTrue(q.table().isEmpty());
        Map<String, Object> exactNull = new LinkedHashMap<>();
        Map<String, Object> ex = new LinkedHashMap<>();
        ex.put("light", null);
        exactNull.put("exact", ex);
        assertEquals(1, MagicAlignments.parse(exactNull).problems().size(), "a null value is a problem, not a crash");
    }

    @Test
    void exactValueNoneReadsAsNeutral() {
        MagicAlignments t = table(list(), list("curse"), "curse_warding", "none");
        assertEquals(Alignment.NEUTRAL, t.classify("curse_cleanse", "curse_warding"));
    }

    @Test
    void theParsedTableIsReadOnly() {
        MagicAlignments t = basic();
        boolean threw = false;
        try { t.lightKeywords().add("x"); } catch (UnsupportedOperationException e) { threw = true; }
        assertTrue(threw, "the keyword set must not be editable from outside");
        threw = false;
        try { t.exactEntries().put("x", Alignment.DARK); } catch (UnsupportedOperationException e) { threw = true; }
        assertTrue(threw, "the exact map must not be editable from outside");
    }
}
