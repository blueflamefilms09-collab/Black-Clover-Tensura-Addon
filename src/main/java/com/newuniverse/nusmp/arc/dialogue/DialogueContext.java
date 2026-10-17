package com.newuniverse.nusmp.arc.dialogue;

import java.util.Locale;

/**
 * Why: a character does not say one thing; what it says depends on what the player came for. The six situations of the owner's brief (the greeting, a
 * hint about the story, the shop, a quest offered, a quest finished, the farewell) are an enum so a dialogue file can only name a situation that exists,
 * the talk logic can ask "does this character have anything to say here?" and the order of the enum is the order lines are numbered in for the language
 * file. Only ever APPEND entries (the ids are written in the data files of every character).
 */
public enum DialogueContext {
    GREETING("greeting"), HINT("hint"), SHOP("shop"), QUEST_OFFER("quest_offer"), QUEST_DONE("quest_done"), FAREWELL("farewell");

    private final String id;

    DialogueContext(String id) {
        this.id = id;
    }

    /** The name used as a key of the {@code lines} object in a dialogue file. */
    public String id() { return id; }

    /** The context with this name, or null for an unknown one (so a typo in a data file is reported, not guessed). */
    public static DialogueContext fromId(String id) {
        if (id == null) return null;
        String s = id.trim().toLowerCase(Locale.ROOT);
        for (DialogueContext c : values()) if (c.id.equals(s)) return c;
        return null;
    }
}
