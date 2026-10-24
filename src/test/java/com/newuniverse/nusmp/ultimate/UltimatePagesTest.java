package com.newuniverse.nusmp.ultimate;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the page of an Ultimate is appended to a book through three small edits of shared files (the hook in GrimoireBook, the gate in pageBlock, the count in the 15-spells self-test) and one boot line in the mod
 * constructor; if any of them is lost or reordered the feature silently disappears (or, worse, the page moves in front of an older page). This test reads the real sources and pins them: the Ultimate wraps
 * everything else so it is the LAST family page (no mode moves), the cache is rebuilt when the setting flips, the gate is asked first, the page costs nothing in the grimoire flow, the armour-policy new-pages file
 * lists exactly the registry, the lang fragment covers every page and every message, and the Time Magic is not mentioned. Pure (file reads, Gson); the working directory is the project root.
 */
class UltimatePagesTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path SRC = ROOT.resolve("src/main/java/com/newuniverse/nusmp");

    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    @Test
    void theUltimateWrapsEveryOtherListSoItIsTheLastFamilyPage() throws IOException {
        String book = read(SRC.resolve("book/GrimoireBook.java"));
        int u = book.indexOf("UltimatePages.append(getRegistryName().getPath(), com.newuniverse.nusmp.book.pages15.Pages15.append(");
        assertTrue(u > 0, "the Ultimate hook must be OUTSIDE the 15-spells hook (it is the last page of the book)");
        int p15 = book.indexOf("Pages15.append(", u);
        int basic = book.indexOf("BasicPages.append(", u);
        int rest = book.indexOf("CharacterPagesRest.append(", u);
        assertTrue(p15 > u && basic > p15 && rest > basic, "order: Ultimate( Pages15( BasicPages( CharacterPagesRest( family )))): no older page moves");
        assertTrue(book.contains("restUltimate == ultimate"), "the cached family list is rebuilt when ultimate.enabled flips");
        assertTrue(book.contains("ultimate != builtUltimate"), "the page list is rebuilt when ultimate.enabled flips");
        assertEquals(1, count(book, "UltimatePages.append("), "one hook");
    }

    @Test
    void theGateIsAskedBeforeAnyOtherBlock() throws IOException {
        String book = read(SRC.resolve("book/GrimoireBook.java"));
        int m = book.indexOf("public String pageBlock(");
        assertTrue(m > 0);
        String body = book.substring(m, book.indexOf("}", book.indexOf("return CharacterSpells.block(", m)));
        assertTrue(body.indexOf("UltimatePages.block(") > 0 && body.indexOf("UltimatePages.block(") < body.indexOf("CharacterSpells.block("), body);
    }

    @Test
    void theFifteenSpellsSelfTestKnowsAboutTheExtraPage() throws IOException {
        String t = read(SRC.resolve("book/pages15/Pages15SelfTests.java"));
        assertTrue(t.contains("b.familyCount() - n - com.newuniverse.nusmp.ultimate.UltimatePages.extraCount(path)"), "its 'first' index must leave the Ultimate out");
    }

    @Test
    void theModConstructorBootsItExactlyOnce() throws IOException {
        String nusmp = read(SRC.resolve("NUSMP.java"));
        assertEquals(1, count(nusmp, "UltimateBoot.init(modEventBus, modContainer)"));
    }

    @Test
    void thePageCostsNothingInTheGrimoireFlowAndIsAppendedOnlyWhenOn() throws IOException {
        String pages = read(SRC.resolve("ultimate/UltimatePages.java"));
        assertTrue(pages.contains("new BookPage(def.id(), def.name(), def.name(), 0, 0, 0,"), "cost 0, floor 0, cooldown 0 in the grimoire's own flow");
        assertTrue(pages.contains("!enabled()) return base"), "switched off = the same list object, the books as before");
        assertTrue(pages.contains("Pages15Rules.MAX_FAMILY"), "never past the 64 pages the unlock mask holds");
        assertTrue(pages.contains("UltimateRegistry.forBook(bookPath)"));
        assertFalse(pages.contains("TIME_"), "no Time Magic");
    }

    @Test
    void theNewPagesFileListsExactlyTheRegistryAndNoOtherFileDoes() throws IOException {
        Path dir = ROOT.resolve("src/test/resources/armour_policy");
        JsonObject o = JsonParser.parseString(read(dir.resolve("ultimate_new_pages.json"))).getAsJsonObject();
        Map<String, List<String>> listed = new LinkedHashMap<>();
        for (var e : o.entrySet()) {
            List<String> ids = new ArrayList<>();
            JsonArray a = e.getValue().getAsJsonArray();
            a.forEach(x -> ids.add(x.getAsString()));
            listed.put(e.getKey(), ids);
        }
        List<String> all = new ArrayList<>();
        listed.values().forEach(all::addAll);
        List<String> want = new ArrayList<>();
        for (UltimateDef d : UltimateRegistry.all()) want.add(d.id());
        assertEquals(new java.util.TreeSet<>(want), new java.util.TreeSet<>(all));
        assertEquals(want.size(), all.size(), "no id twice");
        for (UltimateDef d : UltimateRegistry.all()) assertEquals(List.of(d.id()), listed.get(d.magic()), d.magic());
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "pages15_batch*_new_pages.json")) {
            for (Path f : ds) for (String id : all) assertFalse(read(f).contains("\"" + id + "\""), f.getFileName() + " lists " + id);
        }
        String policy = read(ROOT.resolve("src/main/resources/data/nusmp/armour_policy.json"));
        for (String id : all) assertFalse(policy.contains(id), "the armour policy names " + id + ": an Ultimate never equips an armour");
    }

    @Test
    void theLangFragmentCoversEveryPageAndEveryMessage() throws IOException {
        JsonObject o = JsonParser.parseString(read(ROOT.resolve("tools/lang/ultimate.json"))).getAsJsonObject();
        for (UltimateDef d : UltimateRegistry.all()) {
            assertTrue(o.has(d.langKey()), "no lang text " + d.langKey());
            String t = o.get(d.langKey()).getAsString();
            assertTrue(t.length() >= 120 && t.contains("original") && t.contains("cooldown") && t.contains("ALL"), d.langKey() + ": " + t);
        }
        for (UltimateRules.Cancel c : UltimateRules.Cancel.values()) assertTrue(o.has("nusmp.ultimate.cancel." + c.name().toLowerCase(java.util.Locale.ROOT)), "no message for the cancel " + c);
        assertTrue(o.has("nusmp.ultimate.gathering") && o.has("nusmp.ultimate.warning"));
        assertTrue(Files.isRegularFile(ROOT.resolve("tools/lang/merge_ultimate.py")));
        for (var e : o.entrySet()) assertTrue(e.getKey().startsWith("nusmp.page.") || e.getKey().startsWith("nusmp.ultimate."), e.getKey());
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int at = s.indexOf(what); at >= 0; at = s.indexOf(what, at + 1)) n++;
        return n;
    }
}
