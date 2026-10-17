package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.arc.ArcCharacter;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the six friends of the Dungeon speak through data files, and the whole point of those files is that a player hears the right thing at the right time: a
 * stranger is greeted, a hint follows the story (the next monster, never one already beaten), a request is offered only before its monster has fallen and thanked
 * only after, and the friends that sell nothing do not have shop lines that promise it. This test reads the real files the mod ships and asks the real dialogue
 * rules, so a typo in a boss id or a condition on the wrong side shows up here and not as a silent line that nobody ever hears.
 */
class DanMachiDialogueTest {
    private static final Path DIR = Path.of("src/main/resources/data/nusmp/arc/dialogue");
    private static final List<String> BOSSES = List.of("minotaur", "black_goliath", "amphisbaena", "udaeus");
    private static final Set<String> SHOPS = Set.of("welf_crozzo", "liliruca_arde");

    private static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (ArcCharacter c : DanMachiNpcs.all()) out.add(c.id());
        return out;
    }

    private static DialogueBook book(String id) throws IOException {
        DialogueBook.Parsed p = DialogueBook.parse(id, Files.readString(DIR.resolve(id + ".json"), StandardCharsets.UTF_8));
        assertTrue(p.ok(), id + ": " + p.problems());
        assertTrue(p.problems().isEmpty(), id + ": " + p.problems());
        return p.book();
    }

    private static DialogueSituation after(int bosses, int quest, boolean first) {
        return DialogueSituation.of(bosses, "danmachi", BOSSES.subList(0, bosses), quest, first);
    }

    @Test
    void theSixFilesAreTheSixRecordsAndNoOther() throws IOException {
        assertEquals(6, ids().size());
        assertEquals(6, new HashSet<>(ids()).size(), "distinct ids");
        for (String id : ids()) assertTrue(Files.isRegularFile(DIR.resolve(id + ".json")), id);
        for (ArcCharacter c : DanMachiNpcs.all()) {
            assertTrue(c.isNpc(), c.id());
            assertEquals("DanMachi", c.source());
        }
    }

    @Test
    void everyFriendGreetsAStrangerWithNoConditionButTheFirstMeeting() throws IOException {
        for (String id : ids()) {
            DialogueBook b = book(id);
            DialogueSituation newcomer = DialogueSituation.of(0, "elf_reincarnation", List.of(), 0, true);
            assertTrue(b.has(DialogueContext.GREETING, newcomer), id + " greets a stranger who is in any saga");
            assertNotNull(TalkPlan.next(true, 0, c -> b.has(c, newcomer)));
            long unconditional = b.lines(DialogueContext.GREETING).stream().filter(l -> l.first() == DialogueLine.FIRST_ONLY && l.minStage() == DialogueLine.NO_MIN
                    && l.maxStage() == DialogueLine.NO_MAX && l.sagas().isEmpty() && l.defeated().isEmpty() && l.notDefeated().isEmpty()).count();
            assertTrue(unconditional >= 1, id + " has a first-meeting greeting that depends on nothing else");
        }
    }

    @Test
    void everyFriendHasGreetingsHintsAndFarewellsInEnoughNumber() throws IOException {
        for (String id : ids()) {
            DialogueBook b = book(id);
            assertTrue(b.lines(DialogueContext.GREETING).size() >= 4, id + " greetings");
            assertTrue(b.lines(DialogueContext.GREETING).stream().anyMatch(l -> l.first() == DialogueLine.FIRST_NEVER), id + " greets an old friend too");
            assertTrue(b.lines(DialogueContext.HINT).size() >= 8, id + " hints");
            assertTrue(b.lines(DialogueContext.FAREWELL).size() >= 3, id + " farewells");
        }
    }

    /** The hint lines of the story (those that name a boss) that the player of this stage can hear. */
    private static List<DialogueLine> storyHints(DialogueBook b, DialogueSituation s) {
        List<DialogueLine> out = new ArrayList<>();
        for (DialogueLine l : b.lines(DialogueContext.HINT)) if ((!l.defeated().isEmpty() || !l.notDefeated().isEmpty()) && l.matches(s)) out.add(l);
        return out;
    }

    @Test
    void theHintsFollowTheFourMonstersInOrderAndNeverPointAtOneAlreadyBeaten() throws IOException {
        for (String id : ids()) {
            DialogueBook b = book(id);
            for (int beaten = 0; beaten <= BOSSES.size(); beaten++) {
                List<DialogueLine> hints = storyHints(b, after(beaten, 0, false));
                assertFalse(hints.isEmpty(), id + " has a hint for a player who has beaten " + beaten);
                for (DialogueLine l : hints) {
                    for (String d : l.defeated()) assertTrue(BOSSES.indexOf(d) < beaten, id + ": a line that needs " + d + " is heard after " + beaten);
                    for (String n : l.notDefeated()) assertTrue(BOSSES.indexOf(n) >= beaten, id + ": a line about " + n + " is heard after it fell");
                }
            }
        }
    }

    @Test
    void noLineNamesAnyMonsterButTheFour() throws IOException {
        for (String id : ids()) {
            for (DialogueLine l : book(id).all()) {
                assertTrue(BOSSES.containsAll(l.defeated()), id + ": " + l.defeated());
                assertTrue(BOSSES.containsAll(l.notDefeated()), id + ": " + l.notDefeated());
            }
        }
    }

    @Test
    void aRequestIsOfferedBeforeItsMonsterFallsAndThankedAfterwards() throws IOException {
        for (QuestBook.Request r : QuestBook.requests()) {
            DialogueBook b = book(r.npcId());
            int index = BOSSES.indexOf(r.bossId());
            assertTrue(index >= 0, r.bossId());
            DialogueSituation before = DialogueSituation.of(index, "danmachi", BOSSES.subList(0, index), TalkPlan.QUEST_NONE, false);
            DialogueSituation fell = DialogueSituation.of(index + 1, "danmachi", BOSSES.subList(0, index + 1), TalkPlan.QUEST_NONE, false);
            DialogueSituation done = DialogueSituation.of(index + 1, "danmachi", BOSSES.subList(0, index + 1), TalkPlan.QUEST_DONE, false);
            DialogueSituation notYet = DialogueSituation.of(index, "danmachi", BOSSES.subList(0, index), TalkPlan.QUEST_DONE, false);
            assertTrue(b.has(DialogueContext.QUEST_OFFER, before), r.npcId() + " offers the request before the fall");
            assertFalse(b.has(DialogueContext.QUEST_OFFER, fell), r.npcId() + " never offers a request that is already done");
            assertTrue(b.has(DialogueContext.QUEST_DONE, done), r.npcId() + " thanks the player after the fall");
            assertFalse(b.has(DialogueContext.QUEST_DONE, notYet), r.npcId() + " never thanks for a monster that still stands");
            assertEquals(DialogueContext.QUEST_OFFER, TalkPlan.next(false, TalkPlan.QUEST_NONE, c -> b.has(c, before)), r.npcId());
            assertEquals(DialogueContext.QUEST_DONE, TalkPlan.next(false, TalkPlan.QUEST_DONE, c -> b.has(c, done)), r.npcId());
        }
    }

    @Test
    void friendsWithoutARequestOfferNothingAndThankNothing() throws IOException {
        Set<String> askers = new HashSet<>();
        for (QuestBook.Request r : QuestBook.requests()) askers.add(r.npcId());
        assertEquals(4, askers.size(), "four friends ask for one monster each");
        for (String id : ids()) {
            if (askers.contains(id)) continue;
            DialogueBook b = book(id);
            assertTrue(b.lines(DialogueContext.QUEST_OFFER).isEmpty(), id);
            assertTrue(b.lines(DialogueContext.QUEST_DONE).isEmpty(), id);
        }
    }

    @Test
    void onlyTheSmithAndTheSupporterHaveShopLines() throws IOException {
        for (String id : ids()) {
            DialogueBook b = book(id);
            if (SHOPS.contains(id)) assertTrue(b.lines(DialogueContext.SHOP).size() >= 2, id + " shop lines");
            else assertTrue(b.lines(DialogueContext.SHOP).isEmpty(), id + " has no shop and so no shop lines");
        }
    }

    @Test
    void everyLineIsPlainAsciiAndKeyed() throws IOException {
        for (String id : ids()) {
            List<DialogueLine> all = book(id).all();
            for (int i = 0; i < all.size(); i++) {
                DialogueLine l = all.get(i);
                assertFalse(l.text().isBlank(), id + " line " + (i + 1));
                for (char ch : l.text().toCharArray()) assertTrue(ch < 128, id + " line " + (i + 1) + " has a character the language file would have to escape");
                assertEquals("arc.dialogue." + id + "." + (i + 1), l.key(), id + " line " + (i + 1));
            }
        }
    }
}
