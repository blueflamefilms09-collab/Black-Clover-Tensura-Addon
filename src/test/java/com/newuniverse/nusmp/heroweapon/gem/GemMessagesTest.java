package com.newuniverse.nusmp.heroweapon.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Why: the English sentences of the acquisition system live in {@link GemMessages} as the fallback the game uses and are written from there into the lang
 * fragment the lead merges. This test keeps the list honest: no empty text, the placeholders the code passes arguments for are really there, every
 * refusal and interruption has a message, every Seal has a name, and, once the fragment has been merged into the shipped {@code en_us.json}, the shipped
 * text is the one in the code (before the merge the file simply lacks the keys, which is not an error).
 */
class GemMessagesTest {
    private static final Path LANG = Path.of("src/main/resources/assets/nusmp/lang/en_us.json");

    private static int placeholders(String text) {
        int n = 0;
        for (int i = text.indexOf("%s"); i >= 0; i = text.indexOf("%s", i + 2)) n++;
        return n;
    }

    @Test
    void everyTextIsFilledAndUsesOnlyPercentS() {
        for (Map.Entry<String, String> e : GemMessages.all().entrySet()) {
            assertFalse(e.getValue().isBlank(), e.getKey() + " has no text");
            String stripped = e.getValue().replace("%s", "").replace("%%", "");
            assertFalse(stripped.contains("%"), e.getKey() + " uses a placeholder the game does not know: " + e.getValue());
        }
    }

    @Test
    void theArgumentsTheCodePassesHaveAPlaceholder() {
        assertEquals(1, placeholders(GemMessages.en(GemMessages.BOUND)));
        assertEquals(2, placeholders(GemMessages.en(GemMessages.REFUSED_SEAL)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.REFUSED_GEM)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.CLOSED_ABOVE)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.START)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.COUNT)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.TITLE)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.SUBTITLE_HOLDER)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.CHOSEN)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.TIP_SEAL_KIND)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.TIP_SEAL_LORE)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.TIP_SEAL_HOLDER)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.TIP_USE)));
        assertEquals(2, placeholders(GemMessages.en(GemMessages.GAVE)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.NO_SEAL)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.UNKNOWN_WEAPON)));
        assertEquals(1, placeholders(GemMessages.en(GemMessages.HOLDERS_HEAD)));
        assertEquals(0, placeholders(GemMessages.en(GemMessages.NONE_FREE)), "the owner's line is fixed text");
    }

    @Test
    void theNoneFreeLineIsTheOwnersWording() {
        assertEquals("The legendary weapons have already chosen their heroes.", GemMessages.en(GemMessages.NONE_FREE));
    }

    @Test
    void everyRefusalAndEveryInterruptionHasAMessage() {
        for (GemRules.Denial d : GemRules.Denial.values()) {
            if (d == GemRules.Denial.NONE) continue;
            for (boolean sealed : new boolean[] {false, true}) {
                String key = GemMessages.denialKey(d, sealed);
                assertTrue(GemMessages.all().containsKey(key), "no message for " + d + " (sealed " + sealed + "): " + key);
            }
        }
        for (GemRitual.Interrupt i : GemRitual.Interrupt.values()) {
            String key = GemMessages.interruptKey(i);
            assertTrue(GemMessages.all().containsKey(key), "no message for " + i + ": " + key);
        }
    }

    @Test
    void everyItemHasAName() {
        for (String id : GemCatalog.allItemIds()) assertTrue(GemMessages.all().containsKey("item.nusmp." + id), "no name for " + id);
        for (HeroKind k : GemCatalog.sealKinds()) assertEquals(GemCatalog.sealName(k), GemMessages.en("item.nusmp." + GemCatalog.sealId(k)));
        assertEquals("Calling Gem", GemMessages.en(GemMessages.ITEM_HOLY));
    }

    @Test
    void anUnknownKeyShowsItselfInsteadOfNothing() {
        assertEquals("hero.msg.gem.no_such_thing", GemMessages.en("hero.msg.gem.no_such_thing"));
    }

    @Test
    void theShippedLangFileSaysWhatTheCodeSaysOnceTheFragmentIsMerged() throws Exception {
        if (!Files.exists(LANG)) return;
        JsonObject lang;
        try (Reader r = Files.newBufferedReader(LANG, StandardCharsets.UTF_8)) {
            lang = JsonParser.parseReader(r).getAsJsonObject();
        }
        int present = 0;
        for (String key : GemMessages.all().keySet()) if (lang.has(key)) present++;
        if (present == 0) return;                                                         // the lead has not merged the fragment yet
        for (Map.Entry<String, String> e : GemMessages.all().entrySet()) {
            assertTrue(lang.has(e.getKey()), "the merged lang file lacks " + e.getKey());
            assertEquals(e.getValue(), lang.get(e.getKey()).getAsString(), "the merged text of " + e.getKey() + " differs from the code's");
        }
    }
}
