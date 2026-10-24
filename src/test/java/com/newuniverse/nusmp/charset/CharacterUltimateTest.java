package com.newuniverse.nusmp.charset;

import com.newuniverse.nusmp.ultimate.UltimateDef;
import com.newuniverse.nusmp.ultimate.UltimateRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a grimoire bound to a character used to hide the Ultimate page (the character's set does not list it). The server setting {@code ultimate.showInCharacterGrimoires} (default true) makes such a grimoire show the
 * Ultimate of the character's OWN attribute(s) and keep the Ultimates of the other attributes hidden; off = the old answer everywhere. This pins the pure rule ({@link CharacterSetRegistry#hidden(String, String, String, boolean, boolean, boolean, boolean)}
 * and {@code opens}) against the REAL shipped character files of characters from several kingdoms and every registered Ultimate. Pure, no game.
 */
class CharacterUltimateTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/character_spells");

    private static CharacterSetRegistry shipped() {
        CharacterSetRegistry r = CharacterSetRegistry.load(CharacterSetRegistry.SHIPPED, id -> {
            try { return Files.readString(DIR.resolve(id + ".json")); } catch (IOException e) { return null; }
        });
        assertTrue(r.problems().isEmpty(), "the shipped files have problems: " + r.problems());
        return r;
    }

    private static boolean hidden(CharacterSetRegistry r, String canon, UltimateDef d, boolean show) {
        return r.hidden(canon, d.bookPath(), d.id(), false, true, true, show);
    }

    @Test
    void aCharacterGrimoireShowsTheUltimateOfItsOwnBookAndHidesTheOthers() {
        CharacterSetRegistry r = shipped();
        int own = 0, other = 0;
        for (CharacterSet set : r.all()) {
            for (UltimateDef d : UltimateRegistry.all()) {
                boolean owns = r.ownsBook(set.canon(), d.bookPath());
                assertEquals(!owns, hidden(r, set.canon(), d, true), set.canon() + " / " + d.id() + ": own book shows, every other attribute's Ultimate stays hidden");
                if (owns) own++; else other++;
            }
        }
        assertTrue(own >= 3, "several characters own the book of an Ultimate (found " + own + ")");
        assertTrue(other > own, "most Ultimates belong to other attributes");
    }

    @Test
    void settingOffGivesTheOldAnswerForEveryCharacterAndEveryUltimate() {
        CharacterSetRegistry r = shipped();
        for (CharacterSet set : r.all()) {
            for (UltimateDef d : UltimateRegistry.all()) {
                boolean old = r.hidden(set.canon(), d.bookPath(), d.id(), false, true);
                assertEquals(old, hidden(r, set.canon(), d, false), set.canon() + " / " + d.id());
                if (set.book().equals(d.bookPath())) assertTrue(old, "the old rule hid the Ultimate of " + set.canon() + "'s own book (his set does not list it)");
            }
        }
    }

    @Test
    void charactersOfSeveralKingdomsSeeTheirOwnUltimate() {
        CharacterSetRegistry r = shipped();
        // Clover (Yuno wind, Noelle water, Fuegoleon fire, Yami dark) and the Black Bulls (Luck lightning, Finral spatial): each sees the Ultimate of the book of his set and no other
        String[][] cases = {{"yuno", "ultimate_wind"}, {"noelle", "ultimate_water"}, {"fuegoleon", "ultimate_fire"}, {"luck", "ultimate_lightning"}, {"finral", "ultimate_spatial"}, {"yami", "ultimate_dark"}};
        for (String[] c : cases) {
            UltimateDef d = UltimateRegistry.byId(c[1]);
            assertTrue(r.of(c[0]) != null, c[0] + " has a set");
            assertEquals(d.bookPath(), r.of(c[0]).book(), c[0] + "'s set is the book of " + d.id());
            assertFalse(hidden(r, c[0], d, true), c[0] + " must see " + d.id());
            for (UltimateDef o : UltimateRegistry.all()) if (!r.ownsBook(c[0], o.bookPath())) assertTrue(hidden(r, c[0], o, true), c[0] + " must not see " + o.id());
        }
        assertFalse(hidden(r, "yuno", UltimateRegistry.byId("ultimate_wind"), true));
        assertTrue(hidden(r, "yuno", UltimateRegistry.byId("ultimate_fire"), true));
        assertTrue(hidden(r, "yuno", UltimateRegistry.byId("ultimate_wind"), false), "off: the old rule hides it");
        assertFalse(hidden(r, "yuno", UltimateRegistry.byId("ultimate_fire"), false), "off: the old rule never touched the other books");
    }

    @Test
    void aSecondBookOfTheSameCharacterCounts() {
        CharacterSetRegistry r = shipped();
        // Charmy's set is the Cotton book; the Food book holds a page reserved for her (BasicSignatures): both are her attributes
        assertTrue(r.booksOf("charmy").contains("book_cotton"));
        assertTrue(r.booksOf("charmy").contains("book_food"));
        assertTrue(r.booksOf("gordon").contains(r.of("gordon").book()));
        assertEquals(Set.of(), r.booksOf("nobody"));
        assertEquals(Set.of(), r.booksOf(null));
        assertEquals(Set.of(), r.booksOf(""));
    }

    @Test
    void theBasicGrimoireAndACharacterWithoutAFileAreNotTouched() {
        CharacterSetRegistry r = shipped();
        for (UltimateDef d : UltimateRegistry.all()) {
            for (boolean show : new boolean[]{true, false}) {
                assertFalse(r.hidden("", d.bookPath(), d.id(), false, true, true, show), "the basic grimoire shows " + d.id());
                assertFalse(r.hidden(null, d.bookPath(), d.id(), false, true, true, show), "a grimoire bound to nobody shows " + d.id());
                assertFalse(r.hidden("floga", d.bookPath(), d.id(), false, true, true, show), "a character without a file keeps the whole book: " + d.id());
            }
        }
    }

    @Test
    void modeZeroAndOrdinaryPagesAreNeverChangedByTheSetting() {
        CharacterSetRegistry r = shipped();
        CharacterSet yuno = r.of("yuno");
        for (boolean show : new boolean[]{true, false}) {
            assertFalse(r.hidden("yuno", yuno.book(), "anything", true, true, true, show), "mode 0 is never hidden");
            // an ordinary page (not an Ultimate) gets the old answer whatever the setting says
            assertEquals(r.hidden("yuno", yuno.book(), "not_in_his_set", false, true), r.hidden("yuno", yuno.book(), "not_in_his_set", false, true, false, show));
            for (CharacterSet.Spell s : yuno.spells()) assertFalse(r.hidden("yuno", yuno.book(), s.page(), false, true, false, show), s.page());
        }
    }

    @Test
    void theUltimateOfAnOwnBookOpensByTheOrdinaryMasteryOrderNotBySetAndOnlyWhenOn() {
        CharacterSetRegistry r = shipped();
        UltimateDef wind = UltimateRegistry.byId("ultimate_wind"), fire = UltimateRegistry.byId("ultimate_fire");
        assertNull(r.opens("yuno", wind.bookPath(), wind.id(), null, true, true), "null = no set verdict: the mastery order and the Ultimate gate decide");
        assertEquals(Boolean.FALSE, r.opens("yuno", wind.bookPath(), wind.id(), null, true, false), "off: the old verdict, 'not in his set'");
        assertNull(r.opens("yuno", fire.bookPath(), fire.id(), null, true, true), "another book: the set has no say");
        assertNull(r.opens("floga", wind.bookPath(), wind.id(), null, true, true));
        assertNull(r.opens("", wind.bookPath(), wind.id(), null, true, true));
    }

    @Test
    void theWiringIsInPlace() throws IOException {
        String sets = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/charset/CharacterSets.java"));
        assertTrue(sets.contains("UltimateRegistry.isUltimatePage(pg.id()), com.newuniverse.nusmp.ultimate.UltimateConfig.showInCharacterGrimoires()"), "hidden asks the setting");
        assertEquals(2, count(sets, "UltimateConfig.showInCharacterGrimoires()"), "hidden and opens");
        String cfg = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/ultimate/UltimateConfig.java"));
        assertTrue(cfg.contains("define(\"showInCharacterGrimoires\", true)"), "default true, a SERVER setting of the ultimate file");
        String selfTest = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/charset/CharacterRestSelfTests.java"));
        assertTrue(selfTest.contains("isUltimatePage(b.page(m).id()) && listed =="), "the older self-test leaves the Ultimate page out of its listed == hidden check");
    }

    private static int count(String s, String what) {
        int n = 0;
        for (int at = s.indexOf(what); at >= 0; at = s.indexOf(what, at + 1)) n++;
        return n;
    }
}
