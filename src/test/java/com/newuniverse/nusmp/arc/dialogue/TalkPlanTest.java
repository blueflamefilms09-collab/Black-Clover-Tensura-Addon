package com.newuniverse.nusmp.arc.dialogue;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** What a character talks about: a stranger is greeted, a quest is offered once and thanked once, otherwise a hint. */
class TalkPlanTest {
    private static final Set<DialogueContext> ALL = EnumSet.allOf(DialogueContext.class);

    @Test
    void aStrangerIsGreetedFirst() {
        assertEquals(DialogueContext.GREETING, TalkPlan.next(true, TalkPlan.QUEST_NONE, ALL::contains));
        assertEquals(DialogueContext.GREETING, TalkPlan.next(true, TalkPlan.QUEST_DONE, ALL::contains), "even a finished quest waits for the introduction");
    }

    @Test
    void aQuestIsOfferedOnceThanksGivenOnceThenHints() {
        int quest = TalkPlan.QUEST_NONE;
        DialogueContext said = TalkPlan.next(false, quest, ALL::contains);
        assertEquals(DialogueContext.QUEST_OFFER, said);
        quest = TalkPlan.afterSaying(said, quest);
        assertEquals(TalkPlan.QUEST_OFFERED, quest);
        said = TalkPlan.next(false, quest, ALL::contains);
        assertEquals(DialogueContext.HINT, said, "an offered quest is not offered again");
        assertEquals(TalkPlan.QUEST_OFFERED, TalkPlan.afterSaying(said, quest));
        quest = TalkPlan.QUEST_DONE;
        said = TalkPlan.next(false, quest, ALL::contains);
        assertEquals(DialogueContext.QUEST_DONE, said);
        quest = TalkPlan.afterSaying(said, quest);
        assertEquals(TalkPlan.QUEST_TOLD, quest);
        assertEquals(DialogueContext.HINT, TalkPlan.next(false, quest, ALL::contains));
    }

    @Test
    void whatIsMissingIsSkipped() {
        assertEquals(DialogueContext.HINT, TalkPlan.next(false, TalkPlan.QUEST_NONE, c -> c == DialogueContext.HINT || c == DialogueContext.GREETING));
        assertEquals(DialogueContext.GREETING, TalkPlan.next(false, TalkPlan.QUEST_NONE, c -> c == DialogueContext.GREETING), "no hint: the greeting is repeated");
        assertEquals(DialogueContext.HINT, TalkPlan.next(true, TalkPlan.QUEST_NONE, c -> c == DialogueContext.HINT), "no greeting: straight to the hint");
        assertNull(TalkPlan.next(true, TalkPlan.QUEST_NONE, c -> c == DialogueContext.SHOP), "only a shop line: nothing to say in a chat");
        assertNull(TalkPlan.next(false, TalkPlan.QUEST_NONE, c -> false));
    }

    @Test
    void sayingOtherTopicsChangesNoQuest() {
        for (DialogueContext c : new DialogueContext[]{DialogueContext.GREETING, DialogueContext.HINT, DialogueContext.SHOP, DialogueContext.FAREWELL})
            for (int q = 0; q <= 3; q++) assertEquals(q, TalkPlan.afterSaying(c, q));
        assertEquals(TalkPlan.QUEST_DONE, TalkPlan.afterSaying(DialogueContext.QUEST_OFFER, TalkPlan.QUEST_DONE), "an offer line cannot undo a finished quest");
    }
}
