package com.newuniverse.nusmp.spiritcontrol;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the grimoire's spirit pages (appended at the very end of every book) and the rule for when a player sees them, as pure data and functions so they are unit tested. There are six: {@code spirit_call}
 * (Summon or Release the spirit, a toggle), {@code spirit_combat} (the combat mode toggle) and up to four ability pages {@code spirit_ability_1..4} whose names come from the spirit that answers (Space has
 * four abilities, Light three, Darkness two, the others one). A page is shown only while a spirit of the book's magic answers the player (it is out of reach otherwise: an unusable page is hidden and skipped
 * by the page switch), and an ability page only while that spirit has an ability with that number.
 */
public final class PageGate {
    private PageGate() {}

    public static final int MAX_ABILITIES = 4;
    public static final String CALL = "spirit_call", COMBAT = "spirit_combat", ABILITY_PREFIX = "spirit_ability_";

    /** The id of the ability page with this 0-based number. */
    public static String abilityId(int index) { return ABILITY_PREFIX + (index + 1); }

    /** All six page ids in book order. */
    public static List<String> pageIds() {
        List<String> out = new ArrayList<>();
        out.add(CALL);
        out.add(COMBAT);
        for (int i = 0; i < MAX_ABILITIES; i++) out.add(abilityId(i));
        return out;
    }

    public static boolean isPage(String id) { return id != null && (id.equals(CALL) || id.equals(COMBAT) || abilityIndex(id) >= 0); }

    /** The toggles act on the key press itself (no chant, no cooldown), like Summon Grimoire and the Mana Zone. */
    public static boolean isInstant(String id) { return CALL.equals(id) || COMBAT.equals(id); }

    /** The 0-based ability number of an ability page id, or -1. */
    public static int abilityIndex(String id) {
        if (id == null || !id.startsWith(ABILITY_PREFIX) || id.length() != ABILITY_PREFIX.length() + 1) return -1;
        int d = id.charAt(ABILITY_PREFIX.length()) - '1';
        return d >= 0 && d < MAX_ABILITIES ? d : -1;
    }

    /** Is the page shown, given how many spirits of the book's magic answer the player and how many abilities the first of them has? */
    public static boolean visible(String pageId, int answeringSpirits, int abilityCount) {
        if (answeringSpirits <= 0 || !isPage(pageId)) return false;
        int a = abilityIndex(pageId);
        return a < 0 || a < abilityCount;
    }

    public static String callLabel(String spiritName, boolean out) { return (out ? "Release " : "Summon ") + spiritName; }

    public static String combatLabel(boolean on) { return "Spirit Combat Mode: " + (on ? "ON" : "OFF"); }
}
