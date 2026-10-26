package com.newuniverse.nusmp.skilltree.rarity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: every text of the rarity (the five names, the tooltip line, the "Mythic" tag, the refusal "You have already comprehended this path.") is read with a translatable key and an English fallback, and the
 * lang fragment the lead merges ({@code tools/lang/skillbooks_rarity.json}) must say the very same words, or the merged file would show other words than the game falls back to. The fragment must be flat,
 * and carry a name for every rarity under the key {@code rarity.nusmp.<id>}, so a new rarity appended to the enum cannot ship without its text.
 */
class RarityLangTest {
    private static final Path FRAGMENT = Path.of("tools/lang/skillbooks_rarity.json");

    private static JsonObject fragment() throws IOException {
        return JsonParser.parseString(Files.readString(FRAGMENT, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void theFragmentIsFlat() throws IOException {
        JsonObject f = fragment();
        assertFalse(f.keySet().isEmpty());
        for (String k : f.keySet()) assertTrue(f.get(k).isJsonPrimitive() && f.get(k).getAsJsonPrimitive().isString(), "flat key -> text: " + k);
    }

    @Test
    void everyRarityHasItsNameUnderItsKeyWithTheFallbackWords() throws IOException {
        JsonObject f = fragment();
        for (BookRarity r : BookRarity.values()) {
            assertTrue(f.has(r.langKey()), "no text for " + r.langKey());
            assertEquals(r.label(), f.get(r.langKey()).getAsString(), r.langKey());
        }
    }

    @Test
    void theRefusalAndTheTooltipLinesSaySameWordsAsTheCode() throws IOException {
        JsonObject f = fragment();
        String use = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/skilltree/item/BookUse.java"), StandardCharsets.UTF_8);
        assertTrue(use.contains("ALREADY_COMPREHENDED = \"You have already comprehended this path.\""), "the fallback constant of the code");
        assertEquals("You have already comprehended this path.", f.get("message.nusmp.skillbook.already_comprehended").getAsString());
        assertEquals("Rarity: %s", f.get("tooltip.nusmp.skill_book.rarity").getAsString());
        assertEquals("Mythic", f.get("tooltip.nusmp.skill_book.mythic_tag").getAsString());
    }

    @Test
    void theRefusalKeyIsUsedByTheUseLogicWithTheConstantAsFallback() throws IOException {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/skilltree/item/BookUse.java"), StandardCharsets.UTF_8);
        assertTrue(src.contains("\"message.nusmp.skillbook.already_comprehended\", ALREADY_COMPREHENDED"), "the refusal is translatable with the fallback");
        assertTrue(src.contains("UI_TOAST_CHALLENGE_COMPLETE"), "a successful reading plays the challenge-complete toast sound");
    }

    @Test
    void theOlderKeyIsStillInTheMainLangFile() throws IOException {
        JsonObject main = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/nusmp/lang/en_us.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        assertTrue(main.has("message.nusmp.skillbook.already_owned"), "the old refusal text stays (kept, unused)");
    }
}
