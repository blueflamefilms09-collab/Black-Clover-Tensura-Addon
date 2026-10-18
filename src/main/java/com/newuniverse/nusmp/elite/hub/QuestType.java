package com.newuniverse.nusmp.elite.hub;

import java.util.Locale;

/**
 * Why: the brief asks for "daily and progression quests" and the two behave differently in the one place that matters, the book-keeping: a daily quest is on the board for one day and may be done
 * once per day (it resets with the world's day count), a progression quest is done once for good and unlocks the next of its chain. Saved by name, never by ordinal.
 */
public enum QuestType {
    DAILY, PROGRESSION;

    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The type with this name (case-insensitive), or null. */
    public static QuestType fromId(String s) {
        if (s == null) return null;
        String t = s.trim();
        for (QuestType q : values()) if (q.name().equalsIgnoreCase(t)) return q;
        return null;
    }
}
