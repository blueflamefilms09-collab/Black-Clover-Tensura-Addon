package com.newuniverse.nusmp.engraving.codex;

import com.newuniverse.nusmp.franchise.FranchiseData;
import com.newuniverse.nusmp.franchise.FranchiseRules;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/** The entry builder, with the shipped data: every engraving of the mod is in the book exactly once, named, described, with its facts and its carriers. */
class CodexEntriesTest {

    @Test
    void everyEnchantmentOfTheModAppearsExactlyOnce() throws Exception {
        CodexEntries.Book book = CodexTestData.book(CodexTestData.english());
        Map<String, Integer> seen = new HashMap<>();
        for (CodexEntries.Entry e : book.entries()) seen.merge(e.key(), 1, Integer::sum);
        for (String id : CodexTestData.allEnchantmentIds()) assertEquals(1, seen.getOrDefault("nusmp:" + id, 0).intValue(), "nusmp:" + id + " must be in the book exactly once");
        for (Map.Entry<String, Integer> e : seen.entrySet()) assertEquals(1, e.getValue().intValue(), "duplicate " + e.getKey());
        FranchiseData.Table table = CodexTestData.table();
        assertEquals(69, table.engravings().size(), "the franchise table");
        long franchise = book.entries().stream().filter(e -> e.kind() == CodexEntries.Kind.FRANCHISE).count();
        assertEquals(table.engravings().size(), franchise);
        long mod = book.entries().stream().filter(e -> e.kind() == CodexEntries.Kind.MOD).count();
        assertEquals(CodexTestData.ownIds().size(), mod);
        assertTrue(CodexTestData.ownIds().contains("anti_magic") && CodexTestData.ownIds().contains("legacy_of_the_wizard_kings"));
        // every file of data/nusmp/enchantment is either a franchise engraving of the table or one of the mod's own
        for (String id : CodexTestData.allEnchantmentIds()) assertTrue(table.engravings().containsKey(id) || CodexTestData.ownIds().contains(id), id);
    }

    @Test
    void everyEntryHasANameATextAndAGroup() throws Exception {
        CodexEntries.Book book = CodexTestData.book(CodexTestData.english());
        Set<String> groups = new HashSet<>();
        for (CodexEntries.Group g : book.groups()) assertTrue(groups.add(g.id()), "duplicate group " + g.id());
        int total = 0;
        for (CodexEntries.Group g : book.groups()) total += g.count();
        assertEquals(book.entries().size(), total, "the counts of the groups add up");
        for (CodexEntries.Entry e : book.entries()) {
            assertFalse(e.name().isBlank(), e.key() + " name");
            assertTrue(e.text().length() > 30, e.key() + " text: " + e.text());
            assertFalse(e.scope().isBlank(), e.key() + " scope");
            assertTrue(groups.contains(e.groupId()), e.key() + " group " + e.groupId());
            assertTrue(e.search().contains(e.name().toLowerCase()), e.key() + " search text holds the name");
            assertEquals(e.search(), e.search().toLowerCase(), "the search text is lower case");
        }
    }

    @Test
    void theTextComesFromTheDataEvenWithoutAnyLanguageFile() throws Exception {
        CodexEntries.Book book = CodexTestData.book(k -> null);                       // a server: no mod language file
        for (CodexEntries.Entry e : book.entries()) {
            assertFalse(e.name().isBlank(), e.key());
            assertFalse(e.text().isBlank(), e.key());
        }
        CodexEntries.Entry falna = book.entries().stream().filter(e -> e.key().equals("nusmp:fr_falna")).findFirst().orElseThrow();
        assertEquals("Falna", falna.name(), "the name falls back to the table's name");
        assertTrue(falna.text().contains("kill stacks"), "the fallback text says what the effect is");
    }

    @Test
    void theLangTextIsTheEngravingsOwnDescription() throws Exception {
        Map<String, String> lang = CodexTestData.lang();
        CodexEntries.Book book = CodexTestData.book(lang::get);
        for (CodexEntries.Entry e : book.entries()) {
            if (e.kind() == CodexEntries.Kind.TENSURA) continue;
            String path = e.key().substring("nusmp:".length());
            assertEquals(lang.get("enchantment.nusmp." + path), e.name(), e.key() + " name");
            String desc = lang.get("enchantment.nusmp." + path + ".desc");
            assertEquals(e.kind() == CodexEntries.Kind.FRANCHISE ? CodexFacts.trimBoilerplate(desc) : desc, e.text(), e.key() + " text");
            assertTrue(desc.startsWith(e.text()), e.key() + ": the book's text is the description, only its generated closing sentences left off");
            assertFalse(e.text().contains("Works while") || e.text().contains("Effects are capped"), e.key() + " keeps boilerplate");
        }
    }

    @Test
    void everyEnchantmentHasLangNameAndDescription() throws Exception {
        Map<String, String> lang = CodexTestData.lang();
        for (String id : CodexTestData.allEnchantmentIds()) {
            assertTrue(lang.containsKey("enchantment.nusmp." + id), id + " has no name");
            String desc = lang.get("enchantment.nusmp." + id + ".desc");
            assertTrue(desc != null && desc.length() > 40, id + " has no description");
        }
        for (FranchiseData.Franchise f : CodexTestData.table().franchises()) assertTrue(lang.containsKey("franchise.nusmp." + f.id()), f.id());
        for (String key : new String[]{"item.nusmp.engraving_codex", "nusmp.engraving_codex.all", "nusmp.engraving_codex.search", "nusmp.engraving_codex.found", "nusmp.engraving_codex.more",
                "nusmp.engraving_codex.group.nusmp", "nusmp.engraving_codex.group.tensura", "nusmp.engraving_codex.cmd.given", "nusmp.engraving_codex.cmd.where", "nusmp.gui_scale.gui.engraving_codex"})
            assertTrue(lang.containsKey(key), key);
    }

    @Test
    void theLangTextStillSaysWhatTheDataSaysSoItNeverGoesStale() throws Exception {
        Map<String, String> lang = CodexTestData.lang();
        for (FranchiseData.Engraving en : CodexTestData.table().engravings().values()) {
            String desc = lang.get("enchantment.nusmp." + en.id() + ".desc");
            for (FranchiseData.Effect e : en.effects()) {
                if (e.num().containsKey("cd")) assertTrue(desc.contains(CodexFacts.seconds(e.d("cd", 0))), en.id() + ": the cooldown " + CodexFacts.seconds(e.d("cd", 0)) + " is not in the text: " + desc);
                if (e.num().containsKey("window")) assertTrue(desc.contains(CodexFacts.seconds(e.d("window", 0))), en.id() + ": the window is not in the text");
                if (e.num().containsKey("pct")) assertTrue(desc.contains(CodexFacts.percent(e.d("pct", 0))), en.id() + ": " + CodexFacts.percent(e.d("pct", 0)) + " is not in the text: " + desc);
                if (e.num().containsKey("max")) assertTrue(desc.contains("up to " + e.i("max", 0)), en.id() + ": the stack limit is not in the text");
                if (e.num().containsKey("hits")) assertTrue(desc.contains(e.i("hits", 0) + "th hit"), en.id() + ": the hit count is not in the text");
            }
            assertTrue(desc.contains(CodexFacts.scopeWords(en.scope()).startsWith("Main") ? "main hand" : en.scope().equals("held") ? "either hand" : "worn as armour"), en.id() + " scope words");
        }
    }

    @Test
    void foundOnListsComeFromTheItemTablesAndAreSorted() throws Exception {
        CodexEntries.Book book = CodexTestData.book(CodexTestData.english());
        CodexEntries.Entry anti = book.entries().stream().filter(e -> e.key().equals("nusmp:anti_magic")).findFirst().orElseThrow();
        assertTrue(anti.foundOn().contains("Item demon_slayer_sword") && anti.foundOn().contains("Item nakagami_edge"), "the anti-magic mark is on the Demon-Slayer and the Nakagami: " + anti.foundOn());
        CodexEntries.Entry legacy = book.entries().stream().filter(e -> e.key().equals("nusmp:legacy_of_the_wizard_kings")).findFirst().orElseThrow();
        assertEquals(List.of("Item elsdocia"), legacy.foundOn());
        CodexEntries.Entry swift = book.entries().stream().filter(e -> e.key().equals("tensura:swift")).findFirst().orElseThrow();
        assertTrue(swift.foundOn().size() > 5, "swift is on many weapons");
        List<String> sorted = new java.util.ArrayList<>(swift.foundOn());
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        assertEquals(sorted, swift.foundOn(), "found-on names are sorted");
        // a franchise engraving lists the items of its franchise
        CodexEntries.Entry surge = book.entries().stream().filter(e -> e.key().equals("nusmp:fr_grimoire_surge")).findFirst().orElseThrow();
        assertFalse(surge.foundOn().isEmpty(), "Black Clover has mapped items");
        assertEquals(new TreeSet<>(surge.foundOn()).size(), surge.foundOn().size(), "no duplicates");
        // every franchise engraving lists exactly the items the franchise map gives its franchise; a franchise with no mapped item yet (the map is the truth) lists none and says so
        for (CodexEntries.Entry e : book.entries()) {
            if (e.kind() != CodexEntries.Kind.FRANCHISE) continue;
            long mapped = com.newuniverse.nusmp.franchise.FranchiseMap.map().items().values().stream().filter(f -> f.equals(e.groupId())).count();
            assertEquals(mapped, (long) e.foundOn().size(), e.key() + " lists the items of " + e.groupId());
        }
        CodexEntries.Entry bankai = book.entries().stream().filter(e -> e.key().equals("nusmp:fr_bankai")).findFirst().orElseThrow();
        if (bankai.foundOn().isEmpty()) assertTrue(CodexEntries.foundOnLine(bankai.foundOn(), 6, null, null, null).endsWith("carries it yet"));
    }

    @Test
    void theGroupsRunOwnThenTensuraThenTheFranchisesInTheTablesOrder() throws Exception {
        CodexEntries.Book book = CodexTestData.book(CodexTestData.english());
        assertEquals(CodexEntries.GROUP_MOD, book.groups().get(0).id());
        assertEquals(CodexEntries.GROUP_TENSURA, book.groups().get(1).id());
        FranchiseData.Table table = CodexTestData.table();
        assertEquals(2 + table.franchises().size(), book.groups().size());
        for (int i = 0; i < table.franchises().size(); i++) assertEquals(table.franchises().get(i).id(), book.groups().get(i + 2).id());
        // entries follow the groups' order
        int last = -1;
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < book.groups().size(); i++) order.put(book.groups().get(i).id(), i);
        for (CodexEntries.Entry e : book.entries()) {
            int o = order.get(e.groupId());
            assertTrue(o >= last, "entries are grouped in the contents order");
            last = o;
        }
    }

    @Test
    void everyFranchiseHasAShortLabelForTheContentsList() throws Exception {
        CodexEntries.Book book = CodexTestData.book(CodexTestData.english());
        for (FranchiseData.Franchise f : CodexTestData.table().franchises()) {
            String s = CodexNames.table().get(f.id());
            assertTrue(s != null && !s.isBlank() && s.length() <= 20, f.id() + " short label: " + s);
        }
        for (String id : CodexNames.table().keySet()) assertTrue(CodexTestData.table().franchise(id) != null, "short label for an unknown franchise " + id);
        for (CodexEntries.Group g : book.groups()) {
            assertFalse(g.shortName().isBlank(), g.id());
            assertTrue(g.shortName().length() <= g.name().length() || g.kind() != CodexEntries.Kind.FRANCHISE, g.id() + " the short name is not longer than the name");
            if (g.kind() == CodexEntries.Kind.FRANCHISE) assertEquals(CodexNames.table().get(g.id()), g.shortName());
        }
        assertEquals("Mod enchantments", book.groups().get(0).shortName());
        assertEquals("Tensura engravings", book.groups().get(1).shortName());
        assertEquals("Tensura (Slime)", CodexNames.parse("{\"short\":{\"tensura\":\"Tensura (Slime)\"}}").get("tensura"));
    }

    @Test
    void tensuraEntriesAreHonestAboutWhoseNumbersTheyAre() throws Exception {
        CodexEntries.Book book = CodexTestData.book(CodexTestData.english());
        int n = 0;
        for (CodexEntries.Entry e : book.entries()) {
            if (e.kind() != CodexEntries.Kind.TENSURA) continue;
            n++;
            assertTrue(e.facts().stream().anyMatch(f -> f.contains("Tensura") && f.contains("numbers")), e.key() + " must carry the credit line");
            assertFalse(e.text().matches(".*\\d.*"), e.key() + " invents a number: " + e.text());
        }
        assertTrue(n >= 25, "the Tensura engravings the mod names: " + n);
    }

    @Test
    void foundOnLineTruncatesWithPlusMore() {
        List<String> names = List.of("A", "B", "C", "D", "E", "F", "G", "H");
        assertEquals("Found on: A, B, C, +5 more", CodexEntries.foundOnLine(names, 3, null, null, null));
        assertEquals("Found on: A, B, C, D, E, F, G, H", CodexEntries.foundOnLine(names, 8, null, null, null));
        assertEquals("Found on: A, B, C, D, E, F, G, H", CodexEntries.foundOnLine(names, 20, null, null, null));
        assertEquals("Gefunden: A, +7 mehr", CodexEntries.foundOnLine(names, 1, "Gefunden", "+%d mehr", null));
        assertEquals("Found on: no item of this mod carries it yet", CodexEntries.foundOnLine(List.of(), 6, null, null, null));
        assertEquals("Found on: A, +7 more", CodexEntries.foundOnLine(names, 0, null, null, null), "at least one name is always shown");
    }

    @Test
    void prettyMakesANameFromAnId() {
        assertEquals("Magic Weapon", CodexEntries.pretty("magic_weapon"));
        assertEquals("Swift", CodexEntries.pretty("swift"));
        assertEquals("A B", CodexEntries.pretty("_a__b_"));
    }

    @Test
    void factsSayTheCooldownsAndCapsInWordsFromTheData() throws Exception {
        FranchiseData.Table table = CodexTestData.table();
        FranchiseData.Engraving surge = table.engravings().get("fr_grimoire_surge");
        List<String> facts = CodexFacts.of(surge);
        assertTrue(facts.contains("Cooldown: 5 s"), facts.toString());
        assertTrue(facts.contains("The bonus counts for 3 s after the cast"), facts.toString());
        assertTrue(facts.stream().anyMatch(f -> f.startsWith("All engraving damage bonuses") && f.contains("+" + CodexFacts.percent(FranchiseRules.ATTACK_CAP))), facts.toString());
        assertTrue(facts.stream().anyMatch(f -> f.startsWith("Magicule back: max 5%")), facts.toString());
        assertTrue(facts.get(facts.size() - 1).startsWith("Levels 1 to " + FranchiseRules.MAX_LEVEL), "the level rule ends the list");
        FranchiseData.Engraving falna = table.engravings().get("fr_falna");
        List<String> f2 = CodexFacts.of(falna);
        assertTrue(f2.contains("Up to 5 stacks") && f2.contains("The stacks fade 60 s after the last kill"), f2.toString());
        FranchiseData.Engraving argonaut = table.engravings().get("fr_argonaut");
        assertTrue(CodexFacts.of(argonaut).stream().anyMatch(f -> f.startsWith("Fires on every 6th hit")), CodexFacts.of(argonaut).toString());
        FranchiseData.Engraving wind = table.engravings().get("fr_tortoise_ward");
        assertTrue(CodexFacts.of(wind).stream().anyMatch(f -> f.startsWith("All engraving damage reductions together: max 20%")), CodexFacts.of(wind).toString());
        for (FranchiseData.Engraving en : table.engravings().values()) {
            List<String> all = CodexFacts.of(en);
            assertTrue(all.size() >= 2, en.id() + " has facts");
            assertEquals(new HashSet<>(all).size(), all.size(), en.id() + " has duplicate facts");
            for (FranchiseData.Effect e : en.effects()) if (e.num().containsKey("cd")) assertTrue(all.contains("Cooldown: " + CodexFacts.seconds(e.d("cd", 0))), en.id() + " cooldown");
        }
    }

    @Test
    void numberWordsFormatLikeTheLangText() {
        assertEquals("5 s", CodexFacts.seconds(100));
        assertEquals("0.5 s", CodexFacts.seconds(10));
        assertEquals("1.5 s", CodexFacts.seconds(30));
        assertEquals("1.2%", CodexFacts.percent(0.012));
        assertEquals("12%", CodexFacts.percent(0.12));
        assertEquals("0.4%", CodexFacts.percent(0.004));
        assertEquals("1.5", CodexFacts.trim(1.5));
        assertEquals("8", CodexFacts.trim(8.0));
        assertEquals("Main hand only", CodexFacts.scopeWords("main"));
        assertEquals("Either hand", CodexFacts.scopeWords("held"));
        assertEquals("Held or worn anywhere", CodexFacts.scopeWords("any"));
        assertTrue(CodexFacts.isCap("A heal is never more than 2 health") && CodexFacts.isCap("Levels 1 to 3: +25% per level; cooldowns and caps do not grow") && !CodexFacts.isCap("Cooldown: 5 s"));
        // the generated closing sentences come off, and only those
        String surge = "A grimoire answers a clear call. A spell cast returns 1.2% of your maximum magicule (once every 5 s). Works while the item is in either hand. Effects are capped; stronger items carry a higher level (+25% per level).";
        assertEquals("A grimoire answers a clear call. A spell cast returns 1.2% of your maximum magicule (once every 5 s).", CodexFacts.trimBoilerplate(surge));
        assertEquals("Each Black Clover magic among Elsdocia's owners becomes one ability of the sword, kept for good.", CodexFacts.trimBoilerplate("Each Black Clover magic among Elsdocia's owners becomes one ability of the sword, kept for good."));
        assertEquals("Works while the item is in your main hand.", CodexFacts.trimBoilerplate("Works while the item is in your main hand."), "a text that is only the sentence is left alone");
        assertEquals("", CodexFacts.trimBoilerplate(null));
    }

    @Test
    void theShippedFileNamesAreWhereTheCodeLooks() {
        assertTrue(new File(CodexTestData.RES, "data/nusmp/engraving_codex/tensura_engravings.json").isFile());
        assertTrue(new File(CodexTestData.RES, "data/nusmp/franchise/engravings.json").isFile());
    }
}
