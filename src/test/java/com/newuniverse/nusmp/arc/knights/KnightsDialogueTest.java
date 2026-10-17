package com.newuniverse.nusmp.arc.knights;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.dialogue.DialogueBook;
import com.newuniverse.nusmp.arc.dialogue.DialogueContext;
import com.newuniverse.nusmp.arc.dialogue.DialogueLine;
import com.newuniverse.nusmp.arc.dialogue.DialogueSituation;
import com.newuniverse.nusmp.arc.dialogue.TalkPlan;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a character that falls silent at one point of the story, or promises a quest nothing can finish, is a bug nobody notices until a player meets it, so the
 * sixteen dialogue files of the other Magic Knight squads and the court are checked as data: every book parses with no problem, greets a newcomer, has something
 * to say at EVERY point of the story (never silent), points at each saga, only names champions that exist, and promises no quest or shop.
 */
class KnightsDialogueTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/arc/dialogue");

    /** The champions a line may name: the five openers (fixed by the Saga 1 briefs), Zagred (his entity id is kept), the Triad and Lucius (existing bosses). */
    private static final Set<String> KNOWN_CHAMPIONS = Set.of(KnightsGates.REVCHI, KnightsGates.HEATH, KnightsGates.LOTUS, KnightsGates.MARS, KnightsGates.VETTO,
            "zagred", "dante", "vanica", "zenon", "lucius");
    private static final List<String> SAGAS = List.of("elf_reincarnation", "spade_raid", "wizard_king", "seven_deadly_sins", "danmachi");

    private static DialogueBook book(ArcCharacter c) throws IOException {
        Path p = DIR.resolve(c.id() + ".json");
        assertTrue(Files.isRegularFile(p), "missing dialogue file " + p);
        DialogueBook.Parsed parsed = DialogueBook.parse(c.id(), Files.readString(p, StandardCharsets.UTF_8));
        assertTrue(parsed.ok(), c.id() + ": " + parsed.problems());
        assertTrue(parsed.problems().isEmpty(), c.id() + ": " + parsed.problems());
        return parsed.book();
    }

    private static DialogueSituation sit(int stage, String saga, List<String> defeated, boolean first) {
        return DialogueSituation.of(stage, saga, defeated, 0, first);
    }

    /** Points along the story a player can be at (stage = champions beaten, saga = the earliest saga with a champion left). */
    private static List<DialogueSituation> storyPoints() {
        List<DialogueSituation> out = new ArrayList<>();
        out.add(sit(0, "elf_reincarnation", List.of(), false));
        out.add(sit(1, "elf_reincarnation", List.of(), false));
        out.add(sit(2, "elf_reincarnation", List.of("heath_grice"), false));
        out.add(sit(5, "elf_reincarnation", List.of("revchi_salik", "heath_grice", "lotus_whomalt", "mars", "vetto"), false));
        out.add(sit(8, "spade_raid", List.of("revchi_salik", "heath_grice", "vetto", "zagred"), false));
        out.add(sit(12, "spade_raid", List.of("heath_grice", "vetto", "zagred", "dante", "vanica"), false));
        out.add(sit(15, "wizard_king", List.of("heath_grice", "vetto", "zagred", "dante", "vanica", "zenon"), false));
        out.add(sit(20, "seven_deadly_sins", List.of("heath_grice", "vetto", "zagred", "dante", "vanica", "zenon", "lucius"), false));
        out.add(sit(30, "danmachi", List.of("heath_grice", "vetto", "zagred", "dante", "vanica", "zenon", "lucius"), false));
        return out;
    }

    private static int fitting(DialogueBook b, DialogueContext c, DialogueSituation s) {
        int n = 0;
        for (DialogueLine l : b.lines(c)) if (l.matches(s)) n++;
        return n;
    }

    @Test
    void everyCharacterHasABookThatParsesWithNoProblem() throws IOException {
        for (ArcCharacter c : KnightsCast.ALL) assertTrue(book(c).size() >= 18, c.id() + " has too few lines");
    }

    @Test
    void theFilesBelongToTheRecordsAndNothingElseIsInTheirName() throws IOException {
        Set<String> mine = new HashSet<>();
        for (ArcCharacter c : KnightsCast.ALL) mine.add(c.id());
        for (ArcCharacter c : KnightsCast.ALL) assertEquals(c.id(), book(c).characterId());
        assertEquals(16, mine.size());
    }

    @Test
    void greetingsWelcomeANewcomerAndSeeAReturnerOff() throws IOException {
        DialogueSituation newcomer = sit(0, "elf_reincarnation", List.of(), true);    // the situation the arc.dialogue self-test asks about
        DialogueSituation veteran = sit(9, "spade_raid", List.of("heath_grice", "vetto"), true);
        for (ArcCharacter c : KnightsCast.ALL) {
            DialogueBook b = book(c);
            int firstOnly = 0, returning = 0, forVeterans = 0;
            for (DialogueLine l : b.lines(DialogueContext.GREETING)) {
                if (l.first() == DialogueLine.FIRST_ONLY) firstOnly++;
                if (l.first() == DialogueLine.FIRST_NEVER) returning++;
                if (l.first() == DialogueLine.FIRST_ONLY && l.minStage() != DialogueLine.NO_MIN) forVeterans++;
            }
            assertTrue(b.lines(DialogueContext.GREETING).size() >= 4, c.id() + " needs four greetings at least");
            assertTrue(firstOnly >= 3, c.id() + " needs three ways to meet a stranger");
            assertTrue(returning >= 2, c.id() + " needs two ways to see a friend again");
            assertTrue(forVeterans >= 1, c.id() + " should notice a stranger who already has a name in the dungeon");
            assertEquals(DialogueContext.GREETING, TalkPlan.next(true, 0, x -> b.has(x, newcomer)), c.id() + " must greet a newcomer");
            assertTrue(b.has(DialogueContext.GREETING, veteran), c.id() + " greets a veteran stranger");
            assertEquals(DialogueContext.HINT, TalkPlan.next(false, 0, x -> b.has(x, sit(3, "elf_reincarnation", List.of(), false))), c.id() + " talks about the road once met");
        }
    }

    @Test
    void aCharacterIsNeverSilentAndNeverRepeatsAtOnce() throws IOException {
        for (ArcCharacter c : KnightsCast.ALL) {
            DialogueBook b = book(c);
            for (DialogueSituation s : storyPoints()) {
                assertTrue(fitting(b, DialogueContext.HINT, s) >= 3, c.id() + " has fewer than three hints at stage " + s.stage() + " in " + s.saga());
                assertTrue(fitting(b, DialogueContext.FAREWELL, s) >= 3, c.id() + " has fewer than three farewells at stage " + s.stage());
            }
        }
    }

    @Test
    void everyCharacterPointsAtTheStoryOfEachSagaAndAtTheFirstSteps() throws IOException {
        for (ArcCharacter c : KnightsCast.ALL) {
            DialogueBook b = book(c);
            for (String saga : SAGAS) {
                boolean found = false;
                for (DialogueLine l : b.lines(DialogueContext.HINT)) if (l.sagas().contains(saga)) found = true;
                assertTrue(found, c.id() + " has no hint written for the " + saga + " saga");
            }
            boolean fresh = false;
            for (DialogueLine l : b.lines(DialogueContext.HINT)) if (l.maxStage() == 0) fresh = true;
            assertTrue(fresh, c.id() + " has no hint for a player who has beaten nothing yet");
        }
    }

    @Test
    void hintsShareOnePriorityOtherwiseTheMostSpecificLineWouldBeSaidForEver() throws IOException {
        for (ArcCharacter c : KnightsCast.ALL) {
            DialogueBook b = book(c);
            Integer priority = null;
            for (DialogueLine l : b.lines(DialogueContext.HINT)) {
                if (priority == null) priority = l.priority();
                assertEquals(priority.intValue(), l.priority(), c.id() + ": '" + l.text() + "' has another priority than its neighbours");
            }
        }
    }

    @Test
    void nobodyPromisesAQuestOrAShopThatNothingCanDeliver() throws IOException {
        for (ArcCharacter c : KnightsCast.ALL) {
            DialogueBook b = book(c);
            assertEquals(0, b.lines(DialogueContext.QUEST_OFFER).size(), c.id() + ": an offered quest nobody can finish");
            assertEquals(0, b.lines(DialogueContext.QUEST_DONE).size(), c.id() + ": a finished quest nobody can set");
            assertEquals(0, b.lines(DialogueContext.SHOP).size(), c.id() + ": a shop nothing opens");
        }
    }

    @Test
    void conditionsNameOnlyChampionsAndSagasThatExist() throws IOException {
        Set<String> sagas = new HashSet<>();
        for (ArcSaga s : ArcSaga.values()) sagas.add(s.id());
        for (ArcCharacter c : KnightsCast.ALL) {
            for (DialogueLine l : book(c).all()) {
                for (String id : l.defeated()) assertTrue(KNOWN_CHAMPIONS.contains(id), c.id() + " names the champion '" + id + "'");
                for (String id : l.notDefeated()) assertTrue(KNOWN_CHAMPIONS.contains(id), c.id() + " names the champion '" + id + "'");
                for (String id : l.sagas()) assertTrue(sagas.contains(id), c.id() + " names the saga '" + id + "'");
            }
        }
    }

    @Test
    void linesAreShortPlainAndNeverRepeated() throws IOException {
        for (ArcCharacter c : KnightsCast.ALL) {
            Set<String> seen = new HashSet<>();
            for (DialogueLine l : book(c).all()) {
                String t = l.text();
                assertTrue(t.length() <= 200, c.id() + ": a line of " + t.length() + " characters does not fit a chat line well: " + t);
                assertTrue(t.length() >= 3 && t.chars().anyMatch(Character::isLetter), c.id() + ": a line with no word in it: " + t);          // Nozel's whole farewell is 'Go.'
                assertTrue(t.chars().allMatch(ch -> ch >= 32 && ch < 127), c.id() + ": a character outside plain ASCII in: " + t);
                assertFalse(t.contains("%") || t.contains("\""), c.id() + ": a percent sign or a double quote in: " + t);
                assertTrue(seen.add(t), c.id() + " says '" + t + "' twice");
            }
        }
    }

    @Test
    void theVoiceIsTheRecordsColour() throws IOException {
        for (ArcCharacter c : KnightsCast.ALL) assertEquals(c.color() & 0xFFFFFF, book(c).voice(), c.id() + ": the chat colour and the record disagree");
    }

    @Test
    void everyKeyedLineHasItsKeyInTheGeneratorsNumbering() throws IOException {
        for (ArcCharacter c : KnightsCast.ALL) {
            List<DialogueLine> all = book(c).all();
            for (int i = 0; i < all.size(); i++) assertEquals("arc.dialogue." + c.id() + "." + (i + 1), all.get(i).key(), c.id() + " line " + (i + 1) + " is missing its key (run tools/gen_arc_dialogue_lang.py --write-keys)");
        }
    }
}
