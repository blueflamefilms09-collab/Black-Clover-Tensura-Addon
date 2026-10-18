package com.newuniverse.nusmp.heroweapon.weapon.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.FormsRules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Why: the Book's forms file is data nobody can open in a game here, so the rules of docs/heroweapon/design.md section 6 are checked on the file itself (the
 * shared {@link FormsRules}) and the shape the design promises is pinned: five forms, the Revised Edition needing BOTH the Pipe Book and the Animal Needle
 * Book, every form with the volume's three fixed keys (read, read, Libraria), and the decks each form's passives build exactly the pages the text promises.
 */
class BookFormsTest {
    private static JsonObject file() throws Exception {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/nusmp/hero/forms/vassal_book.json"))).getAsJsonObject();
    }

    private static JsonObject form(JsonObject root, String id) {
        for (JsonElement f : root.getAsJsonArray("forms")) if (f.getAsJsonObject().get("id").getAsString().equals(id)) return f.getAsJsonObject();
        throw new AssertionError("no form " + id);
    }

    private static List<String> passives(JsonObject form) {
        List<String> l = new ArrayList<>();
        for (JsonElement p : form.getAsJsonArray("passives")) l.add(p.getAsString());
        return l;
    }

    @Test
    void theFileObeysTheFormsRules() throws Exception {
        List<String> bad = FormsRules.check(file(), HeroKind.BOOK, Set.copyOf(BookText.skillIds()), Set.of(BookNumbers.LIBRARIA), Set.copyOf(BookText.passiveIds()), 3, 5);
        assertTrue(bad.isEmpty(), "forms file problems: " + bad);
    }

    @Test
    void theTreeHasADiamondAndATopForm() throws Exception {
        JsonObject root = file();
        assertEquals("vassal_book", root.get("weapon").getAsString());
        assertEquals(5, root.getAsJsonArray("forms").size());
        assertEquals(2, form(root, "revised_edition").getAsJsonArray("parents").size());
        assertEquals("revised_edition", form(root, "library_tome").getAsJsonArray("parents").get(0).getAsString());
        assertEquals(3, form(root, "library_tome").get("tier").getAsInt());
    }

    @Test
    void everyFormReadsTheSameWayAndHasLibraria() throws Exception {
        for (JsonElement f : file().getAsJsonArray("forms")) {
            JsonObject o = f.getAsJsonObject(), skills = o.getAsJsonObject("skills");
            String id = o.get("id").getAsString();
            assertEquals(BookNumbers.READ_COMPOSITION, skills.get("use").getAsString(), id);
            assertEquals(BookNumbers.READ_VERSE, skills.get("sneak").getAsString(), id);
            assertEquals(BookNumbers.TURN_PAGE, skills.get("key1").getAsString(), id);
            assertEquals(BookNumbers.LIBRARIA, skills.get("key3").getAsString(), id);
            assertTrue(passives(o).contains(BookNumbers.P_CHAPTERS), id + " has the chapters");
        }
    }

    @Test
    void thePassivesBuildTheDecksTheTextPromises() throws Exception {
        JsonObject root = file();
        assertEquals(List.of("bird", "needles"), BookDecks.offense(passives(form(root, "base"))));
        assertEquals(List.of("wall", "verse"), BookDecks.support(passives(form(root, "base"))));
        assertEquals(List.of("wall", "verse", "cage"), BookDecks.support(passives(form(root, "pipe_book"))));
        assertEquals(List.of("bird", "needles"), BookDecks.offense(passives(form(root, "pipe_book"))));
        assertEquals(List.of("bird", "needles", "feathers"), BookDecks.offense(passives(form(root, "animal_needle_book"))));
        assertEquals(List.of("bird", "needles", "feathers"), BookDecks.offense(passives(form(root, "revised_edition"))));
        assertEquals(List.of("wall", "verse", "cage"), BookDecks.support(passives(form(root, "revised_edition"))));
        assertTrue(passives(form(root, "revised_edition")).contains(BookNumbers.P_REVISED));
        assertTrue(passives(form(root, "library_tome")).contains(BookNumbers.P_GRAND));
        assertTrue(passives(form(root, "library_tome")).contains(BookNumbers.P_REVISED), "the top form keeps what the Revised Edition gave");
        assertFalse(passives(form(root, "pipe_book")).contains(BookNumbers.P_REVISED));
    }

    @Test
    void theKeyTwoTechniqueIsTheFormsOwnBurst() throws Exception {
        JsonObject root = file();
        assertEquals(BookNumbers.PAPER_BURST, form(root, "base").getAsJsonObject("skills").get("key2").getAsString());
        assertEquals(BookNumbers.CONSTRICTION, form(root, "pipe_book").getAsJsonObject("skills").get("key2").getAsString());
        assertEquals(BookNumbers.LIFE_FORCE_BURST, form(root, "animal_needle_book").getAsJsonObject("skills").get("key2").getAsString());
        assertEquals(BookNumbers.LIFE_FORCE_BURST, form(root, "library_tome").getAsJsonObject("skills").get("key2").getAsString());
    }
}
