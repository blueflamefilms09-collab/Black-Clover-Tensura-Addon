package com.newuniverse.nusmp.arc.zagred;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Why: a verdict is the one place a player can be punished for what they did, so what counts as an answer and who is spared is pinned here. */
class ZagredAnswersTest {
    static ZagredAnswers.Snapshot snap(boolean air, double moved, boolean shield, boolean sees, boolean anti, double gaze) {
        return new ZagredAnswers.Snapshot(air, moved, shield, sees, anti, gaze);
    }

    static final ZagredAnswers.Snapshot IDLE = snap(false, 0, false, true, false, 0);

    @Test
    void eachAnswerHasItsOwnThreshold() {
        assertFalse(ZagredAnswers.met(ZagredAnswers.Answer.KEEP_MOVING, snap(false, ZagredAnswers.MOVE_NEEDED - 0.01, false, true, false, 0)));
        assertTrue(ZagredAnswers.met(ZagredAnswers.Answer.KEEP_MOVING, snap(false, ZagredAnswers.MOVE_NEEDED, false, true, false, 0)));
        assertFalse(ZagredAnswers.met(ZagredAnswers.Answer.JUMP, IDLE));
        assertTrue(ZagredAnswers.met(ZagredAnswers.Answer.JUMP, snap(true, 0, false, true, false, 0)));
        assertFalse(ZagredAnswers.met(ZagredAnswers.Answer.SHIELD, IDLE));
        assertTrue(ZagredAnswers.met(ZagredAnswers.Answer.SHIELD, snap(false, 0, true, true, false, 0)));
        assertFalse(ZagredAnswers.met(ZagredAnswers.Answer.BREAK_SIGHT, IDLE));
        assertTrue(ZagredAnswers.met(ZagredAnswers.Answer.BREAK_SIGHT, snap(false, 0, false, false, false, 0)));
        assertFalse(ZagredAnswers.met(ZagredAnswers.Answer.ANTI_MAGIC, IDLE));
        assertTrue(ZagredAnswers.met(ZagredAnswers.Answer.ANTI_MAGIC, snap(false, 0, false, true, true, 0)));
        assertFalse(ZagredAnswers.met(ZagredAnswers.Answer.MEET_GAZE, snap(false, 0, false, true, false, ZagredAnswers.GAZE_NEEDED - 0.01)));
        assertTrue(ZagredAnswers.met(ZagredAnswers.Answer.MEET_GAZE, snap(false, 0, false, true, false, ZagredAnswers.GAZE_NEEDED)));
    }

    @Test
    void nothingMeetsAnAnswerWithoutFacts() {
        for (ZagredAnswers.Answer a : ZagredAnswers.Answer.values()) {
            assertFalse(ZagredAnswers.met(a, null));
            assertFalse(ZagredAnswers.met(null, IDLE));
        }
        assertFalse(ZagredAnswers.spared(ZagredWord.HALT, null));
        assertFalse(ZagredAnswers.spared(null, IDLE));
    }

    @Test
    void everyVerdictWordHasAnAnswerThePlayerIsTold() {
        for (ZagredWord w : ZagredVerdict.POOL) {
            assertFalse(ZagredAnswers.answersOf(w).isEmpty(), w + " has an answer");
            assertNotNull(ZagredAnswers.told(w));
            assertFalse(ZagredAnswers.told(w).hint().isBlank());
        }
        assertTrue(ZagredAnswers.answersOf(ZagredWord.OVERWRITE).isEmpty(), "the ultimate is not a verdict");
        assertNull(ZagredAnswers.told(ZagredWord.REDACT));
        assertTrue(ZagredAnswers.answersOf(null).isEmpty());
    }

    @Test
    void theVerdictAnswersMatchTheStory() {
        assertEquals(ZagredAnswers.Answer.KEEP_MOVING, ZagredAnswers.told(ZagredWord.HALT));
        assertEquals(ZagredAnswers.Answer.JUMP, ZagredAnswers.told(ZagredWord.FALL));
        assertEquals(ZagredAnswers.Answer.BREAK_SIGHT, ZagredAnswers.told(ZagredWord.SEAL));
        assertEquals(ZagredAnswers.Answer.SHIELD, ZagredAnswers.told(ZagredWord.SWORDS));
        assertEquals(ZagredAnswers.Answer.MEET_GAZE, ZagredAnswers.told(ZagredWord.COWER));
    }

    @Test
    void sparedMeansTheRightAnswerOrAntiMagicAgainstTheWordsOfTheMind() {
        assertFalse(ZagredAnswers.spared(ZagredWord.HALT, IDLE));
        assertTrue(ZagredAnswers.spared(ZagredWord.HALT, snap(false, 2, false, true, false, 0)));
        assertTrue(ZagredAnswers.spared(ZagredWord.HALT, snap(false, 0, false, true, true, 0)), "anti-magic holds the line against the bind");
        assertTrue(ZagredAnswers.spared(ZagredWord.SEAL, snap(false, 0, false, true, true, 0)));
        assertTrue(ZagredAnswers.spared(ZagredWord.COWER, snap(false, 0, false, true, true, 0)));
        assertFalse(ZagredAnswers.spared(ZagredWord.FALL, snap(false, 0, false, true, true, 0)), "a wave of stone is not magic of the mind: jump it");
        assertFalse(ZagredAnswers.spared(ZagredWord.SWORDS, snap(false, 0, false, true, true, 0)), "swords are steel: raise the shield");
        assertTrue(ZagredAnswers.spared(ZagredWord.SWORDS, snap(false, 0, true, true, false, 0)));
        assertTrue(ZagredAnswers.spared(ZagredWord.FALL, snap(true, 0, false, true, false, 0)));
    }

    @Test
    void theAnswersHaveLanguageKeys() {
        for (ZagredAnswers.Answer a : ZagredAnswers.Answer.values()) assertTrue(a.hintKey().startsWith("message.nusmp.zagred.answer."));
        assertEquals("message.nusmp.zagred.answer.keep_moving", ZagredAnswers.Answer.KEEP_MOVING.hintKey());
    }
}
