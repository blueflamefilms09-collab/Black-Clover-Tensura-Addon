package com.newuniverse.nusmp.arc.zagred;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Why: the table of Words is read by the boss, the screen, the verdicts and the language file, so ids, keys and control flags must stay consistent. */
class ZagredWordTest {
    @Test
    void idsAreUniqueLowerCaseAndFindable() {
        Set<String> ids = new HashSet<>();
        for (ZagredWord w : ZagredWord.values()) {
            assertTrue(ids.add(w.id()), "unique id " + w.id());
            assertEquals(w.id(), w.id().toLowerCase(java.util.Locale.ROOT));
            assertSame(w, ZagredWord.byId(w.id()));
            assertFalse(w.title().isBlank());
            assertEquals(w.title(), w.title().toUpperCase(java.util.Locale.ROOT), "titles are shown in capitals");
            assertFalse(w.hint().isBlank());
            assertTrue(w.hint().length() <= 60, "the counter fits under the title: " + w.hint());
        }
        assertNull(ZagredWord.byId("nope"));
        assertNull(ZagredWord.byId(null));
    }

    @Test
    void colourAndKeys() {
        for (ZagredWord w : ZagredWord.values()) {
            assertTrue(w.rgb() >= 0 && w.rgb() <= 0xFFFFFF);
            assertEquals(0xFF000000 | w.rgb(), w.argb());
            assertEquals("message.nusmp.zagred.word." + w.id(), w.titleKey());
            assertEquals(w.titleKey() + ".hint", w.hintKey());
        }
    }

    @Test
    void theBindingWordsAreHardControlAndTheSteelOnesAreStoppedByAShield() {
        assertTrue(ZagredWord.HALT.hardControl());
        assertTrue(ZagredWord.SEAL.hardControl());
        assertTrue(ZagredWord.COWER.hardControl(), "terror is a control too");
        for (ZagredWord w : ZagredWord.values())
            if (w != ZagredWord.HALT && w != ZagredWord.SEAL && w != ZagredWord.COWER) assertFalse(w.hardControl(), w + " is not a hard control");
        assertTrue(ZagredWord.SWORDS.physical());
        assertTrue(ZagredWord.TRIDENT.physical());
        assertFalse(ZagredWord.REDACT.physical());
        assertFalse(ZagredWord.FALL.physical());
    }

    @Test
    void antiMagicShrugsOffTheWordsOfTheMindAndNothingElse() {
        assertTrue(ZagredWord.HALT.antiMagicExempt());
        assertTrue(ZagredWord.SEAL.antiMagicExempt());
        assertTrue(ZagredWord.COWER.antiMagicExempt());
        assertTrue(ZagredWord.BANISH.antiMagicExempt());
        assertFalse(ZagredWord.SWORDS.antiMagicExempt());
        assertFalse(ZagredWord.FALL.antiMagicExempt());
        assertFalse(ZagredWord.TRIDENT.antiMagicExempt());
        assertFalse(ZagredWord.OVERWRITE.antiMagicExempt());
    }
}
