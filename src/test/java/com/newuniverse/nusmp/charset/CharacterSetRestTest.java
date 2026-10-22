package com.newuniverse.nusmp.charset;

import com.newuniverse.nusmp.corpse.SoulCorpseAccess;
import com.newuniverse.nusmp.grimoire.CanonBook;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The thirteen sets of docs/magic/character_spells_rest.md as data: they all load, belong to the book of their character's magic, keep the old lock lines reachable, make the signature spells
 * wait for the spell before them, and keep the Soul Corpse fix (docs/magic/soul_corpse_fix.md) true for Rades.
 */
class CharacterSetRestTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/character_spells");
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    static final List<String> REST = List.of("charlotte", "jack", "rill", "dorothy", "zenon", "vanica", "dante", "lucius", "rades", "gadjah", "mars", "kahono", "kiato");

    private static CharacterSetRegistry shipped() {
        CharacterSetRegistry r = CharacterSetRegistry.load(CharacterSetRegistry.SHIPPED, id -> {
            try { return Files.readString(DIR.resolve(id + ".json")); } catch (IOException e) { return null; }
        });
        assertTrue(r.problems().isEmpty(), "the shipped files have problems: " + r.problems());
        return r;
    }

    private static final class F implements RuleFacts {
        double mastery;
        final Map<String, Integer> uses = new HashMap<>();
        final Set<String> open = new HashSet<>(), flags = new HashSet<>();
        @Override public double mastery() { return mastery; }
        @Override public int uses(String p) { return uses.getOrDefault(p, 0); }
        @Override public boolean pageOpen(String p) { return open.contains(p); }
        @Override public int trust() { return 0; }
        @Override public boolean flag(String n) { return flags.contains(n); }
    }

    @Test
    void everyCharacterOfThisBuilderIsShippedAndLoads() {
        CharacterSetRegistry r = shipped();
        for (String id : REST) {
            assertTrue(CharacterSetRegistry.SHIPPED.contains(id), id + " is not in SHIPPED");
            assertNotNull(r.of(id), id + " did not load");
        }
        assertEquals(13, REST.size());
    }

    @Test
    void theBookIsTheBookOfTheCharactersMagic() {
        CharacterSetRegistry r = shipped();
        for (String id : REST) {
            CanonBook c = CanonBook.byId(id);
            assertNotNull(c, id);
            CharacterSet s = r.of(id);
            assertEquals(c.magic, s.magic(), id + ": the file's magic is the CanonBook's magic");
            assertEquals("book_" + c.magic.toLowerCase(), s.book(), id + ": the book path follows the magic");
        }
    }

    @Test
    void theThreeCanonBooksThatMovedToTheirOwnAttribute() {
        assertEquals("BRIAR", CanonBook.CHARLOTTE.magic);
        assertEquals("SOUL", CanonBook.LUCIUS.magic);
        assertEquals("MINERAL", CanonBook.MARS.magic);
    }

    @Test
    void everyFileHasEnglishOrJapaneseSourcesAndEveryNoteIsFilled() {
        for (String id : REST) {
            CharacterSet s = shipped().of(id);
            assertTrue(s.sources().containsKey("en_wiki") || s.sources().containsKey("ja_search"), id + ": no wiki source");
            assertFalse(s.notes().isBlank(), id + ": no notes");
            for (CharacterSet.Spell sp : s.spells()) {
                assertFalse(sp.note().isBlank(), id + ":" + sp.page() + " has no note");
                assertFalse(sp.name().isBlank(), id + ":" + sp.page());
            }
        }
    }

    @Test
    void aNewGrimoireOpensOneOrTwoSpellsAtTheStart() {
        CharacterSetRegistry r = shipped();
        F fresh = new F();
        for (String id : REST) {
            long open = r.of(id).spells().stream().filter(sp -> sp.rule().test(fresh)).count();
            assertTrue(open >= 1 && open <= 2, id + ": a new grimoire opens " + open + " spells");
        }
    }

    @Test
    void everyEarlierSpellOpensBeforeTheLaterOnesThatWaitForIt() {
        // a page that waits for another page's uses or opening never opens by mastery alone
        CharacterSetRegistry r = shipped();
        for (String id : REST) {
            F top = new F();
            top.mastery = 1.0;
            for (CharacterSet.Spell sp : r.of(id).spells()) {
                if (!sp.rule().pages().isEmpty()) assertFalse(sp.rule().test(top), id + ":" + sp.page() + " opens at full mastery without the spell it waits for");
            }
        }
    }

    @Test
    void theSignatureSpellsWaitForTheSpellBeforeThem() {
        CharacterSetRegistry r = shipped();
        F f = new F();
        f.mastery = 1.0;
        f.flags.add("mana_zone");
        CharacterSet.Spell queen = r.of("charlotte").spell("queen_of_briars");
        assertFalse(queen.rule().test(f));
        f.uses.put("corpse_hunting_briar_tree", 2);
        assertTrue(queen.rule().test(f));
        assertFalse(r.of("charlotte").spell("queen_of_briars").rule().test(lowMastery(f)), "and not before the mana zone or the top of the bar");
        F g = new F();
        g.mastery = 1.0;
        assertFalse(r.of("zenon").spell("demon_sword_dainsleif").rule().test(g));
        g.uses.put("ossuary", 3);
        assertTrue(r.of("zenon").spell("demon_sword_dainsleif").rule().test(g));
        assertFalse(r.of("zenon").spell("spatial_rupture").rule().test(g));
        g.uses.put("demon_sword_dainsleif", 3);
        assertTrue(r.of("zenon").spell("spatial_rupture").rule().test(g));
        F h = new F();
        h.mastery = 1.0;
        assertFalse(r.of("jack").spell("lunatic_slash").rule().test(h));
        h.uses.put("death_scythe", 3);
        assertTrue(r.of("jack").spell("lunatic_slash").rule().test(h));
        F k = new F();
        k.mastery = 1.0;
        assertFalse(r.of("gadjah").spell("apocalypra_astrauza").rule().test(k));
        k.uses.put("tenjiolia", 3);
        assertTrue(r.of("gadjah").spell("apocalypra_astrauza").rule().test(k));
        k.mastery = 0.5;
        assertFalse(r.of("gadjah").spell("apocalypra_astrauza").rule().test(k), "the Ultimate Magic needs the mana zone or the top of the bar");
    }

    private static F lowMastery(F from) {
        F x = new F();
        x.mastery = 0.5;
        x.uses.putAll(from.uses);
        return x;
    }

    @Test
    void theOldLockLinesOfTheseCharactersStayReachable() throws IOException {
        // a page locked to a character in CharacterSpells must be in his set when the set is for the same book (else nobody could open it)
        String locks = Files.readString(SRC.resolve("book/CharacterSpells.java"));
        Matcher m = Pattern.compile("lock\\(\"(book_\\w+)\", \"(\\w+)\", ((?:CanonBook\\.\\w+(?:, )?)+)\\)").matcher(locks);
        CharacterSetRegistry r = shipped();
        int checked = 0;
        while (m.find()) {
            for (String who : m.group(3).split(",\\s*")) {
                String canon = who.replace("CanonBook.", "").trim().toLowerCase();
                if (!REST.contains(canon)) continue;
                CharacterSet s = r.of(canon);
                if (!s.book().equals(m.group(1))) continue;
                assertTrue(s.has(m.group(2)), canon + ": the lock on " + m.group(1) + ":" + m.group(2) + " would leave the page unreachable");
                checked++;
            }
        }
        assertTrue(checked >= 8, "locks checked: " + checked);
    }

    @Test
    void theSoulCorpseFixStaysTrueForRades() {
        CharacterSet rades = shipped().of("rades");
        for (String g : SoulCorpseAccess.GENERIC_SUMMONS) assertFalse(rades.spell(g).exclusive(), g + " is open to any Soul Corpse holder");
        for (String g : SoulCorpseAccess.RADES_ONLY) assertTrue(rades.spell(g).exclusive(), g + " is Rades's");
        // his rules are mastery only and never ask more than the caps of the fix, so the self-test magic.soul_corpse_cast still opens his six pages at full mastery
        for (Map.Entry<String, Double> e : SoulCorpseAccess.MASTERY_CAP.entrySet()) {
            F f = new F();
            f.mastery = e.getValue();
            assertTrue(rades.spell(e.getKey()).rule().test(f), e.getKey() + " asks more than the cap of the Soul Corpse fix");
        }
        for (String g : SoulCorpseAccess.RADES_ONLY) {
            F f = new F();
            f.mastery = 1.0;
            assertTrue(rades.spell(g).rule().test(f), g + " opens at full mastery");
        }
        for (String s : SoulCorpseAccess.STARTER) assertTrue(rades.spell(s).rule().test(new F()), s + " is a starter");
    }

    @Test
    void noPageIsExclusiveToTwoCharactersAndTheSharedPagesAreNot() {
        CharacterSetRegistry r = shipped();
        Map<String, String> owner = new HashMap<>();
        for (CharacterSet s : r.all()) for (CharacterSet.Spell sp : s.spells()) if (sp.exclusive()) {
            String key = s.book() + "|" + sp.page();
            assertFalse(owner.containsKey(key) && !owner.get(key).equals(s.canon()), key + " is exclusive to two characters");
            owner.put(key, s.canon());
        }
        for (String shared : List.of("pulsaranta", "chain", "lance_volley", "attribute_guard", "gravity_sphere", "crushing_weight"))
            for (String id : REST) { CharacterSet s = r.of(id); if (s.has(shared)) assertFalse(s.spell(shared).exclusive(), id + ":" + shared + " is a shared page"); }
    }

    @Test
    void theFlourishTableHasTwoLayersForEveryCharacterAndNobodyElse() throws IOException {
        String shapes = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/vfx/VfxShape.java"));
        for (String id : REST) assertEquals(2, FlourishTable.layersOf(id).size(), id);
        assertEquals(13, FlourishTable.characters().size());
        assertTrue(FlourishTable.layersOf("yuno").isEmpty(), "the pilots are untouched");
        assertTrue(FlourishTable.layersOf(null).isEmpty());
        assertTrue(FlourishTable.layersOf("").isEmpty());
        for (String id : REST) for (FlourishTable.Layer l : FlourishTable.layersOf(id)) {
            assertTrue(l.ticks() > 0 && l.ticks() <= 60, id + " " + l.shape());
            assertTrue(l.power() > 0 && l.power() <= 2.5f, id + " " + l.shape());
            assertTrue(Pattern.compile("\\b" + l.shape() + "\\b[,;(]").matcher(shapes).find(), id + ": VfxShape has no " + l.shape());
        }
    }
}
