package com.newuniverse.nusmp.arc.dialogue;

import java.util.function.Predicate;

/**
 * Why: when a player right-clicks a character the game must decide WHAT the character talks about, and that decision (greet a stranger, tell of a
 * finished quest once, offer a quest once, otherwise give a hint) is the heart of how a character feels alive, so it is a small pure rule with tests
 * rather than an if-chain inside an entity class. The quest stages are the shared language between this rule and whatever hands out the quests:
 * {@link #QUEST_NONE} nothing yet, {@link #QUEST_OFFERED} the character has asked for something, {@link #QUEST_DONE} the player did it (set by the quest
 * system), {@link #QUEST_TOLD} the character has thanked them.
 */
public final class TalkPlan {
    private TalkPlan() {}

    public static final int QUEST_NONE = 0, QUEST_OFFERED = 1, QUEST_DONE = 2, QUEST_TOLD = 3;

    /**
     * The topic of this conversation, or null when the character has nothing to say at all.
     *
     * @param first      the first meeting
     * @param questStage the quest stage with this character
     * @param available  whether the character has a line for a context that the player's situation allows
     */
    public static DialogueContext next(boolean first, int questStage, Predicate<DialogueContext> available) {
        if (first && available.test(DialogueContext.GREETING)) return DialogueContext.GREETING;
        if (questStage == QUEST_DONE && available.test(DialogueContext.QUEST_DONE)) return DialogueContext.QUEST_DONE;
        if (questStage == QUEST_NONE && available.test(DialogueContext.QUEST_OFFER)) return DialogueContext.QUEST_OFFER;
        if (available.test(DialogueContext.HINT)) return DialogueContext.HINT;
        if (available.test(DialogueContext.GREETING)) return DialogueContext.GREETING;
        return null;
    }

    /** The quest stage after the character spoke about {@code said}: an offer is made once, a finished quest is thanked once. */
    public static int afterSaying(DialogueContext said, int questStage) {
        if (said == DialogueContext.QUEST_OFFER && questStage == QUEST_NONE) return QUEST_OFFERED;
        if (said == DialogueContext.QUEST_DONE && questStage == QUEST_DONE) return QUEST_TOLD;
        return questStage;
    }
}
