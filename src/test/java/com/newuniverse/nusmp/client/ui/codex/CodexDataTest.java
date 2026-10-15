package com.newuniverse.nusmp.client.ui.codex;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The pure core of the data-driven Codex: the JSON format, the pack merge, the ordering, the "???" rule and the small text rules. */
class CodexDataTest {
    private static CodexEntryData parse(String section, String id, String json) {
        CodexEntryParser.Result r = CodexEntryParser.parse(section, id, json);
        assertTrue(r.ok(), r.error());
        return r.entry();
    }

    private static CodexEntryData e(String section, String id, String title, int order) {
        return new CodexEntryData(section, id, title, "t", "", "", order, false);
    }

    // ---------------------------------------------------------------- parsing
    @Test
    void parsesAllFields() {
        CodexEntryData d = parse("rules", "nusmp:relics", "{\"title\":\"Relics\",\"text\":\"Only here.\",\"keys\":\"item:nusmp:a|mob:nusmp:b\",\"icon\":\"item:nusmp:foo\",\"order\":7}");
        assertEquals("rules", d.section());
        assertEquals("nusmp:relics", d.id());
        assertEquals("Relics", d.title());
        assertEquals("Only here.", d.text());
        assertEquals("item:nusmp:a|mob:nusmp:b", d.keys());
        assertEquals("item:nusmp:foo", d.icon());
        assertEquals(7, d.order());
        assertFalse(d.remove());
    }

    @Test
    void missingFieldsGetDefaultsAndTheTitleFallsBackToTheFileName() {
        CodexEntryData d = parse("lore", "mymod:the_old_gate", "{\"text\":\"x\"}");
        assertEquals("The Old Gate", d.title());
        assertEquals("", d.keys());
        assertEquals("", d.icon());
        assertEquals(0, d.order());
        CodexEntryData nested = parse("lore", "mymod:deep/dir/second_page", "{}");
        assertEquals("Second Page", nested.title());
    }

    @Test
    void orderMayBeANumberOrANumericStringAndGarbageIsZero() {
        assertEquals(5, parse("s", "a:b", "{\"order\":5}").order());
        assertEquals(6, parse("s", "a:b", "{\"order\":\"6\"}").order());
        assertEquals(0, parse("s", "a:b", "{\"order\":\"soon\"}").order());
        assertEquals(0, parse("s", "a:b", "{\"order\":[1]}").order());
    }

    @Test
    void badFilesGiveAnErrorNotAnException() {
        assertFalse(CodexEntryParser.parse("s", "a:b", "{not json").ok());
        assertFalse(CodexEntryParser.parse("s", "a:b", "[1,2]").ok());
        assertFalse(CodexEntryParser.parse("s", "a:b", "").ok());
        assertFalse(CodexEntryParser.parse("s", "a:b", null).ok());
        assertFalse(CodexEntryParser.parse("", "a:b", "{}").ok());
        assertFalse(CodexEntryParser.parse("s", "", "{}").ok());
        assertNotNull(CodexEntryParser.parse("s", "a:b", "{not json").error());
    }

    @Test
    void removeFlagIsRead() {
        assertTrue(parse("s", "a:b", "{\"remove\":true}").remove());
        assertFalse(parse("s", "a:b", "{\"remove\":\"maybe\"}").remove());
    }

    // ---------------------------------------------------------------- merging
    @Test
    void theLaterPackOverridesAnEarlierEntryOfTheSameId() {
        List<CodexEntryData> in = new ArrayList<>();
        in.add(new CodexEntryData("rules", "nusmp:x", "Old title", "old", "", "", 1, false));
        in.add(new CodexEntryData("rules", "nusmp:y", "Other", "o", "", "", 2, false));
        in.add(new CodexEntryData("rules", "nusmp:x", "New title", "new", "", "", 1, false));
        Map<String, List<CodexEntryData>> m = CodexMerger.merge(in);
        assertEquals(2, m.get("rules").size());
        assertEquals("New title", m.get("rules").get(0).title());
        assertEquals("new", m.get("rules").get(0).text());
    }

    @Test
    void sameNameInAnotherNamespaceIsAnotherEntry() {
        Map<String, List<CodexEntryData>> m = CodexMerger.merge(List.of(e("rules", "nusmp:x", "A", 0), e("rules", "othermod:x", "B", 0)));
        assertEquals(2, m.get("rules").size());
    }

    @Test
    void aRemoveEntryDeletesAndASectionWithNothingLeftDisappears() {
        List<CodexEntryData> in = new ArrayList<>();
        in.add(e("lore", "nusmp:a", "A", 0));
        in.add(new CodexEntryData("lore", "nusmp:a", "", "", "", "", 0, true));
        in.add(e("rules", "nusmp:b", "B", 0));
        Map<String, List<CodexEntryData>> m = CodexMerger.merge(in);
        assertFalse(m.containsKey("lore"));
        assertTrue(m.containsKey("rules"));
    }

    @Test
    void aRemovedEntryCanComeBackFromAnEvenLaterPack() {
        List<CodexEntryData> in = new ArrayList<>();
        in.add(e("s", "n:a", "first", 0));
        in.add(new CodexEntryData("s", "n:a", "", "", "", "", 0, true));
        in.add(e("s", "n:a", "third", 0));
        assertEquals("third", CodexMerger.merge(in).get("s").get(0).title());
    }

    @Test
    void entriesSortByOrderThenTitleThenIdAndSectionsAlphabetically() {
        List<CodexEntryData> in = List.of(e("zeta", "n:1", "x", 0), e("alpha", "n:b", "Banana", 5), e("alpha", "n:a", "apple", 5),
                e("alpha", "n:c", "Zed", 1), e("alpha", "n:d", "apple", 5), e("Beta", "n:e", "e", 0));
        Map<String, List<CodexEntryData>> m = CodexMerger.merge(in);
        assertEquals(List.of("alpha", "Beta", "zeta"), new ArrayList<>(m.keySet()));
        List<String> ids = new ArrayList<>();
        for (CodexEntryData d : m.get("alpha")) ids.add(d.id());
        assertEquals(List.of("n:c", "n:a", "n:d", "n:b"), ids);
    }

    @Test
    void nullsAndHalfEmptyEntriesAreIgnored() {
        List<CodexEntryData> in = new ArrayList<>();
        in.add(null);
        in.add(e("", "n:a", "t", 0));
        in.add(e("s", "", "t", 0));
        assertTrue(CodexMerger.merge(in).isEmpty());
    }

    // ---------------------------------------------------------------- the holder
    @Test
    void replaceInstallsAnImmutableSnapshotAndBumpsTheGeneration() {
        int g = CodexData.get().generation();
        CodexData.replace(CodexMerger.merge(List.of(e("rules", "n:a", "A", 0))));
        assertEquals(g + 1, CodexData.get().generation());
        assertEquals(1, CodexData.get().sections().get("rules").size());
        try {
            CodexData.get().sections().get("rules").add(e("rules", "n:z", "Z", 0));
            fail("the snapshot must be read-only");
        } catch (UnsupportedOperationException expected) {
            // good
        }
        CodexData.replace(Map.of());
        assertEquals(g + 2, CodexData.get().generation());
        assertTrue(CodexData.get().sections().isEmpty());
    }

    private static void fail(String m) { throw new AssertionError(m); }

    // ---------------------------------------------------------------- the ??? rule and the text rules
    @Test
    void anEntryWithoutKeysIsAlwaysShownAndOneWithKeysFollowsDiscovery() {
        assertTrue(CodexData.shown("", k -> false));
        assertTrue(CodexData.shown("  ", k -> false));
        assertTrue(CodexData.shown(null, k -> false));
        assertFalse(CodexData.shown("item:nusmp:a|mob:nusmp:b", k -> false));
        Set<String> known = Set.of("item:nusmp:a|mob:nusmp:b");
        assertTrue(CodexData.shown("item:nusmp:a|mob:nusmp:b", known::contains));
    }

    @Test
    void prettifyAndGlyph() {
        assertEquals("Spirit Lords", CodexData.prettify("spirit_lords"));
        assertEquals("Magic Rules", CodexData.prettify("magic-rules"));
        assertEquals("A B", CodexData.prettify("a/b"));
        assertEquals("", CodexData.prettify(null));
        assertEquals("R", CodexData.glyph("Rules"));
        assertEquals("S", CodexData.glyph("  spells"));
        assertEquals("?", CodexData.glyph(""));
        assertEquals("?", CodexData.glyph(null));
    }

    @Test
    void langKeysAreRecognisedButPlainTextIsNot() {
        assertTrue(CodexData.looksLikeLangKey("nusmp.codex.rule.relics.title"));
        assertTrue(CodexData.looksLikeLangKey("block.nusmp.some_block"));
        assertFalse(CodexData.looksLikeLangKey("Relics belong to the Labyrinth"));
        assertFalse(CodexData.looksLikeLangKey("Done."));
        assertFalse(CodexData.looksLikeLangKey("hello"));
        assertFalse(CodexData.looksLikeLangKey(""));
        assertFalse(CodexData.looksLikeLangKey(null));
        assertEquals("nusmp.codex.section.rules", CodexData.sectionKey("rules"));
    }

    @Test
    void iconIdsComeOnlyFromTheItemPrefix() {
        assertEquals("nusmp:foo", CodexData.iconItemId("item:nusmp:foo"));
        assertEquals("nusmp:foo", CodexData.iconItemId("ITEM: nusmp:foo"));
        assertNull(CodexData.iconItemId("block:nusmp:foo"));
        assertNull(CodexData.iconItemId("item:"));
        assertNull(CodexData.iconItemId(""));
        assertNull(CodexData.iconItemId(null));
    }

    // ---------------------------------------------------------------- the stock rule files are valid
    @Test
    void everyStockRuleFileParsesAndHasATitleAndText() throws Exception {
        java.nio.file.Path dir = java.nio.file.Path.of("src/main/resources/assets/nusmp/codex/rules");
        if (!java.nio.file.Files.isDirectory(dir)) return;                       // run from another working directory: nothing to check
        int n = 0;
        try (var files = java.nio.file.Files.list(dir)) {
            for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) files::iterator) {
                if (!p.toString().endsWith(".json")) continue;
                String name = p.getFileName().toString().replace(".json", "");
                CodexEntryData d = parse("rules", "nusmp:" + name, java.nio.file.Files.readString(p));
                assertFalse(d.title().isBlank(), name);
                assertFalse(d.text().isBlank(), name);
                n++;
            }
        }
        assertTrue(n >= 10, "the stock rules are there");
    }
}
