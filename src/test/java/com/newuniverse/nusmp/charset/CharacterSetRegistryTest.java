package com.newuniverse.nusmp.charset;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The visibility rules of the registry and the five shipped pilot files (Yuno, Noelle, Fuegoleon, Asta, Yami). */
class CharacterSetRegistryTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/character_spells");

    private static CharacterSetRegistry shipped() {
        CharacterSetRegistry r = CharacterSetRegistry.load(CharacterSetRegistry.SHIPPED, id -> {
            try { return Files.readString(DIR.resolve(id + ".json")); } catch (IOException e) { return null; }
        });
        assertTrue(r.problems().isEmpty(), "the shipped files have problems: " + r.problems());
        return r;
    }

    @Test
    void everyShippedFileLoadsWithNoProblem() {
        CharacterSetRegistry r = shipped();
        assertEquals(CharacterSetRegistry.SHIPPED.size(), r.all().size());
    }

    @Test
    void everyFileInTheDirectoryIsListedAndEveryListedFileExists() throws IOException {
        List<String> onDisk = new ArrayList<>();
        try (Stream<Path> s = Files.list(DIR)) { s.filter(p -> p.toString().endsWith(".json")).forEach(p -> onDisk.add(p.getFileName().toString().replace(".json", ""))); }
        for (String id : onDisk) assertTrue(CharacterSetRegistry.SHIPPED.contains(id), id + ".json is not in CharacterSetRegistry.SHIPPED");
        for (String id : CharacterSetRegistry.SHIPPED) assertTrue(onDisk.contains(id), id + " is listed but has no file");
    }

    @Test
    void aFileWithAProblemIsSkippedAndReported() {
        CharacterSetRegistry r = CharacterSetRegistry.load(List.of("yuno", "ghost"), id -> id.equals("yuno") ? "{ broken" : null);
        assertEquals(0, r.all().size());
        assertEquals(2, r.problems().stream().filter(p -> p.startsWith("yuno:") || p.startsWith("ghost:")).map(p -> p.substring(0, 5)).distinct().count());
    }

    @Test
    void aFileNamedForAnotherCharacterIsRejected() {
        String text = CharacterSetLoaderTest.file(CharacterSetLoaderTest.spell("a", "{\"type\":\"start\"}"));
        CharacterSetRegistry r = CharacterSetRegistry.load(List.of("noelle"), id -> text);
        assertEquals(0, r.all().size());
        assertTrue(r.problems().get(0).contains("declares canon"));
    }

    @Test
    void aCharacterWithASetSeesOnlyHisPagesAndNeverModeZeroHidden() {
        CharacterSetRegistry r = shipped();
        assertTrue(r.hidden("yuno", "book_wind", "gust_lane", false, true), "Gust Lane was never Yuno's");
        assertTrue(r.hidden("yuno", "book_wind", "slicing_wind_emperor", false, true));
        assertFalse(r.hidden("yuno", "book_wind", "spirit_of_zephyr", false, true));
        assertFalse(r.hidden("yuno", "book_wind", "sylphs_breath", false, true));
        assertFalse(r.hidden("yuno", "book_wind", "gust_lane", true, true), "mode 0 is the book's default page and is never hidden");
        assertTrue(r.hidden("noelle", "book_water", "aqua_javelin", false, true));
        assertTrue(r.hidden("yami", "book_dark", "dark_cloak", false, true));
        assertTrue(r.hidden("fuegoleon", "book_fire", "calidos_brachium", false, true), "Mereoleona's spell is not Fuegoleon's");
        assertFalse(r.hidden("asta", "book_anti_magic", "black_meteorite", false, true));
    }

    @Test
    void aCharacterWithoutASetKeepsTheWholeBook() {
        CharacterSetRegistry r = shipped();
        assertFalse(r.hidden("mereoleona", "book_fire", "calidos_brachium", false, true));
        assertFalse(r.hidden("mereoleona", "book_fire", "sol_linea", false, true), "an unfinished character keeps today's grimoire, even for a spell another character owns");
        assertFalse(r.hidden("kaiser", "book_wind", "spirit_storm", false, true));
    }

    @Test
    void theBasicBookHidesTheSignatureSpellsOfTheCharacters() {
        CharacterSetRegistry r = shipped();
        for (String page : new String[]{"spirit_of_zephyr", "spirit_storm", "sylphs_breath", "towering_tornado", "spirits_hushed_dance"})
            assertTrue(r.hidden("", "book_wind", page, false, true), page + " must not be in the basic Wind book");
        assertTrue(r.hidden(null, "book_fire", "salamanders_breath", false, true));
        assertTrue(r.hidden("", "book_fire", "sol_linea", false, true));
        assertFalse(r.hidden("", "book_wind", "gust_lane", false, true), "the basic book's own pages stay");
        assertFalse(r.hidden("", "book_wind", "kamaitachi", false, true), "the starter is not exclusive");
        assertFalse(r.hidden("", "book_wind", "spirit_storm", false, false), "with the setting off the basic book lists them as before");
        assertFalse(r.hidden("", "book_wind", "spirit_storm", true, true));
        assertFalse(r.hidden("", "book_anti_magic", "black_meteorite", false, true), "the Anti-Magic book is Asta's alone: nothing in it is exclusive");
    }

    @Test
    void aSetOfAnotherBookDoesNotHideThisBook() {
        CharacterSetRegistry r = shipped();
        assertFalse(r.hidden("yuno", "book_fire", "calderos", false, true));
    }

    @Test
    void exclusiveOwnersAreKnown() {
        CharacterSetRegistry r = shipped();
        assertEquals("yuno", r.exclusiveOwner("book_wind", "spirit_of_zephyr"));
        assertEquals("noelle", r.exclusiveOwner("book_water", "point_blank_roar"));
        assertNull(r.exclusiveOwner("book_wind", "gust_lane"));
        assertNull(r.exclusiveOwner("book_wind", "kamaitachi"));
    }

    @Test
    void twoCharactersCannotBothOwnAPage() {
        CharacterSetRegistry r = new CharacterSetRegistry();
        CharacterSet a = CharacterSetLoader.parse(CharacterSetLoaderTest.file(CharacterSetLoaderTest.spell("p", "{\"type\":\"start\"}").replace("\"origin\"", "\"exclusive\":true,\"origin\"")));
        assertTrue(r.add(a));
        CharacterSet b = CharacterSetLoader.parse(CharacterSetLoaderTest.file(CharacterSetLoaderTest.spell("p", "{\"type\":\"start\"}").replace("\"origin\"", "\"exclusive\":true,\"origin\"")).replace("\"yuno\"", "\"luck\""));
        assertFalse(r.add(b));
        assertFalse(r.problems().isEmpty());
    }

    @Test
    void theOpenVerdictFollowsTheRuleAndIsNullWithoutASet() {
        CharacterSetRegistry r = shipped();
        SpellRuleTest.F f = new SpellRuleTest.F();
        f.mastery = 1.0;
        assertNull(r.opens("kaiser", "book_wind", "spirit_storm", f), "no set: the old mastery order decides");
        assertEquals(Boolean.FALSE, r.opens("yuno", "book_wind", "spirit_of_zephyr", f), "a full grimoire without a Spirit Dive");
        assertEquals(Boolean.FALSE, r.opens("yuno", "book_wind", "gust_lane", f), "not his page");
        f.uses.put("spirit_dive", 2);
        assertEquals(Boolean.TRUE, r.opens("yuno", "book_wind", "spirit_of_zephyr", f));
        assertEquals(Boolean.TRUE, r.opens("YUNO", "book_wind", "kamaitachi", f), "the id is case-insensitive");
    }

    @Test
    void yunoGetsHisSpellsInTheAnimeOrder() {
        CharacterSet y = shipped().of("yuno");
        List<String> order = y.spells().stream().map(CharacterSet.Spell::page).toList();
        assertTrue(order.indexOf("sylphs_breath") < order.indexOf("spirit_storm"), "the bond comes before the Dive spells");
        assertTrue(order.indexOf("spirit_storm") < order.indexOf("spirit_of_zephyr"));
        assertTrue(order.indexOf("spirit_of_zephyr") < order.indexOf("liable_tempest"));
        assertTrue(order.indexOf("spirit_of_zephyr") < order.indexOf("spirit_of_boreas"));
        assertEquals("anime_only", y.spell("liable_tempest").origin(), "Liable Tempest is anime-only");
        // none of the Dive spells opens from mastery alone
        SpellRuleTest.F f = new SpellRuleTest.F();
        f.mastery = 1.0;
        f.trust = 100;
        f.flags.addAll(List.of("spirit_bond", "saint_stage"));
        for (String p : new String[]{"spirit_storm", "spirits_hushed_dance", "spirit_of_zephyr", "liable_tempest", "spirit_of_boreas"})
            assertFalse(y.spell(p).rule().test(f), p + " must wait for the Spirit Dive");
        assertTrue(y.spell("sylphs_breath").rule().test(f));
    }

    @Test
    void noPilotSpellOpensAtTheStartExceptTheStarters() {
        CharacterSetRegistry r = shipped();
        SpellRuleTest.F fresh = new SpellRuleTest.F();
        for (CharacterSet s : r.all()) {
            long open = s.spells().stream().filter(sp -> sp.rule().test(fresh)).count();
            assertTrue(open >= 1 && open <= 2, s.canon() + ": a new grimoire opens " + open + " spells");
        }
    }

    @Test
    void everyPilotHasSourcesAndNotes() {
        for (CharacterSet s : shipped().all()) {
            assertFalse(s.sources().isEmpty(), s.canon());
            assertNotNull(s.notes(), s.canon());
            for (CharacterSet.Spell sp : s.spells()) assertFalse(sp.note().isBlank(), s.canon() + ":" + sp.page() + " has no note");
        }
    }

    @Test
    void trackedPagesAreTheOnesRulesNameAndTheDive() {
        CharacterSetRegistry r = shipped();
        assertTrue(r.tracked("yuno").contains("spirit_dive"));
        assertTrue(r.tracked("yami").contains("black_moon"));
        assertTrue(r.tracked("kaiser").isEmpty());
        assertTrue(r.tracked("asta").size() <= 8, "the grimoire's tag stays small");
    }
}
