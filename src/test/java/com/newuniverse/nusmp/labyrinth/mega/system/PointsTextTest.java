package com.newuniverse.nusmp.labyrinth.mega.system;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the new texts live in {@link PointsText} (the English the code falls back on) and in {@code tools/system/lang_points.json} (the fragment the owner merges
 * into the game's lang file; {@code en_us.json} itself is not edited). If the two ever differ, a player sees one text in a fresh checkout and another after the
 * merge, so this test pins them as identical, key for key and word for word. It also pins the English fallback chain ({@link SystemText#english} finds these keys),
 * that every key has the module's prefix and a non-empty text, that every numbered placeholder is one the code actually passes, and the number format of the
 * effect lines ({@code +0.2}, {@code +1%}).
 */
class PointsTextTest {
    private static final Path LANG = Paths.get("tools", "system", "lang_points.json");

    @Test void theFragmentFileSaysExactlyWhatTheCodeSays() throws IOException {
        Map<String, Object> file = MiniJson.object(Files.readString(LANG, StandardCharsets.UTF_8));
        assertEquals(PointsText.all().size(), file.size(), "same number of keys");
        for (Map.Entry<String, String> e : PointsText.all().entrySet()) {
            assertTrue(file.containsKey(e.getKey()), "missing in the fragment: " + e.getKey());
            assertEquals(e.getValue(), file.get(e.getKey()), e.getKey());
        }
        assertEquals(java.util.List.copyOf(PointsText.all().keySet()), java.util.List.copyOf(file.keySet()), "same order, so a diff is readable");
    }

    @Test void everyKeyHasTheModulePrefixOrIsAKeyBindingAndAnEnglishText() {
        for (Map.Entry<String, String> e : PointsText.all().entrySet()) {
            assertTrue(e.getKey().startsWith(SystemText.PREFIX) || e.getKey().startsWith("key.nusmp."), e.getKey());
            assertFalse(e.getValue().isBlank(), e.getKey());
            assertEquals(e.getValue(), SystemText.english(e.getKey()), "SystemText.english finds " + e.getKey());
        }
    }

    @Test void noKeyClashesWithAnOlderKey() {
        for (String k : PointsText.all().keySet()) assertFalse(SystemText.all().containsKey(k), "already in SystemText: " + k);
    }

    @Test void placeholdersAreNumberedAndWithinWhatTheCallSitesPass() {
        assertTrue(PointsText.all().get(PointsText.TOAST_FREE).contains("%1$s"));
        assertTrue(PointsText.all().get(PointsText.BADGE).contains("%1$s") && PointsText.all().get(PointsText.BADGE).contains("%2$s"));
        String effect = PointsText.all().get(PointsText.EFFECT);
        for (String a : new String[]{"%1$s", "%2$s", "%3$s", "%4$s"}) assertTrue(effect.contains(a), a);
        String nocap = PointsText.all().get(PointsText.EFFECT_NOCAP);
        for (String a : new String[]{"%1$s", "%2$s", "%3$s"}) assertTrue(nocap.contains(a), a);
        assertFalse(nocap.contains("%4$s"));
        for (String v : PointsText.all().values()) assertFalse(v.contains("%s") || v.contains("%d"), "an unnumbered placeholder in: " + v);
    }

    @Test void everyStatHasAFullName() {
        for (Stat s : Stat.values()) {
            String key = PointsText.statFullKey(s);
            assertNotNull(PointsText.englishOrNull(key), s.name());
        }
        assertNull(PointsText.englishOrNull("not.a.key"));
        assertNull(PointsText.englishOrNull(null));
    }

    @Test void amountsAreWrittenTheWayAPlayerReadsThem() {
        assertEquals("+0.2", PointsText.amount(StatTable.Op.ADD_VALUE, 0.2));
        assertEquals("+6", PointsText.amount(StatTable.Op.ADD_VALUE, 6.0));
        assertEquals("+0.25", PointsText.amount(StatTable.Op.ADD_VALUE, 0.25));
        assertEquals("+1%", PointsText.amount(StatTable.Op.ADD_MULTIPLIED_BASE, 0.01));
        assertEquals("+25%", PointsText.amount(StatTable.Op.ADD_MULTIPLIED_BASE, 0.25));
        assertEquals("+2%", PointsText.amount(StatTable.Op.ADD_MULTIPLIED_TOTAL, 0.02));
        assertEquals("-0.5", PointsText.amount(StatTable.Op.ADD_VALUE, -0.5));
        assertEquals("-10%", PointsText.amount(StatTable.Op.ADD_MULTIPLIED_BASE, -0.1));
        assertEquals("+0", PointsText.amount(StatTable.Op.ADD_VALUE, 0.0));
        assertEquals("+20", PointsText.amount(StatTable.Op.ADD_VALUE, 20.0));
        assertEquals("+60%", PointsText.amount(StatTable.Op.ADD_MULTIPLIED_BASE, 0.6));
        assertEquals("+0.33", PointsText.amount(StatTable.Op.ADD_VALUE, 1.0 / 3.0));
    }
}
