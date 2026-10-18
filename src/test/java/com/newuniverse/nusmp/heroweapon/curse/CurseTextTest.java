package com.newuniverse.nusmp.heroweapon.curse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.Sin;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: the curse's words are the weapon's voice, and the language fragment is written from {@link CurseText}, so the table must be complete and clean: every sin
 * has the same number of whispers, no line is empty or too long for the action bar, every key is unique, the text is plain ASCII (the language fragment is
 * ASCII-escaped and a stray symbol would turn into a code on the screen), and no format string asks for more arguments than the game code passes.
 */
class CurseTextTest {
    @Test
    void everySinHasItsOwnWhispers() {
        Set<String> seen = new HashSet<>();
        for (Sin sin : Sin.values()) {
            assertEquals(CurseText.WHISPER_COUNT, CurseText.whispers(sin).size(), sin + " has the same number of whispers as the others");
            for (int i = 0; i < CurseText.WHISPER_COUNT; i++) {
                String line = CurseText.whispers(sin).get(i);
                assertFalse(line.isBlank(), sin + " " + i);
                assertTrue(line.length() <= 80, "short enough for the action bar next to the weapon's name: " + line);
                assertEquals(line, CurseText.english(CurseText.whisperKey(sin, i)), "the key table carries the line");
                assertTrue(seen.add(line), "no line is used twice: " + line);
            }
        }
        assertEquals(Sin.values().length * CurseText.WHISPER_COUNT, seen.size());
    }

    @Test
    void theKeyTableIsCleanAscii() {
        Set<String> keys = new HashSet<>();
        for (Map.Entry<String, String> e : CurseText.all().entrySet()) {
            assertTrue(keys.add(e.getKey()), "unique key " + e.getKey());
            assertFalse(e.getValue().isBlank(), e.getKey());
            for (char c : (e.getKey() + e.getValue()).toCharArray()) assertTrue(c >= 32 && c < 127, "plain ASCII in " + e.getKey() + ": char " + (int) c);
            assertTrue(e.getKey().startsWith("hero.curse.") || e.getKey().startsWith("key.nusmp.hero_") || e.getKey().startsWith("item.nusmp.") || e.getKey().startsWith("tooltip.nusmp."),
                    "key in our own namespace: " + e.getKey());
        }
        assertTrue(CurseText.all().size() > 60, "whispers and messages: " + CurseText.all().size());
    }

    @Test
    void formatStringsAskForTheArgumentsTheCodePasses() {
        assertEquals(2, count(CurseText.english(CurseText.OFFER_BAR)));
        assertEquals(5, count(CurseText.english(CurseText.OFFER_CHAT)));
        assertEquals(1, count(CurseText.english(CurseText.EMBRACE_TITLE)));
        assertEquals(1, count(CurseText.english(CurseText.EMBRACE_SUBTITLE)));
        assertEquals(3, count(CurseText.english(CurseText.EMBRACE_CHAT)));
        assertEquals(2, count(CurseText.english(CurseText.EMBRACE_CHAT_NO_FORM)));
        assertEquals(1, count(CurseText.english(CurseText.OVERRUN_WARN)));
        assertEquals(1, count(CurseText.english(CurseText.PURIFY_REFUND)));
        assertEquals(1, count(CurseText.english(CurseText.REST)));
        assertEquals(2, count(CurseText.english(CurseText.ENVY_TAKE)));
        assertEquals(1, count(CurseText.english(CurseText.ENVY_REFUSE)));
        assertEquals(1, count(CurseText.english(CurseText.ENVY_STRIP)));
        assertEquals(1, count(CurseText.english(CurseText.HUD_OFFER)));
        assertEquals(1, count(CurseText.english(CurseText.HUD_HOLD)));
        assertEquals(0, count(CurseText.english(CurseText.LAPSE)));
        assertEquals(0, count(CurseText.english(CurseText.OFFER_KEY3)));
    }

    @Test
    void anUnknownKeyComesBackAsItself() {
        assertEquals("not.ours", CurseText.english("not.ours"));
        assertEquals(0, CurseText.whisperIndex(0));
        assertEquals(1, CurseText.whisperIndex(1));
        assertEquals(0, CurseText.whisperIndex(CurseText.WHISPER_COUNT));
        assertEquals(CurseText.WHISPER_COUNT - 1, CurseText.whisperIndex(-1), "a counter below zero still lands on a line");
    }

    private static int count(String format) {
        int n = 0;
        for (int i = 0; i + 1 < format.length(); i++) if (format.charAt(i) == '%' && format.charAt(i + 1) == 's') n++;
        return n;
    }
}
