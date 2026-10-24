package com.newuniverse.nusmp.companion.talk;

import java.util.Set;

/**
 * Why: the limits of the talk screen and of every packet it sends, as one pure class so the server, the builder of a view and the tests all use the same numbers. The SERVER owns the conversation:
 * a view is a list of options with ids the server chose; a client can only send back an id that is on the view it was last sent, from within {@link #REACH} blocks, to a living entity, and not
 * faster than {@link #PICK_GAP_TICKS}. An option id is at most {@link #MAX_ID} characters of plain text; a view has at most {@link #MAX_OPTIONS} options; the ids beginning {@code k:} belong to the
 * bond system, {@code g:} to the growth panel and {@code x:} to the screen itself, so a companion's own topics may not use them.
 */
public final class TalkRules {
    private TalkRules() {}

    public static final int MAX_ID = 40, MAX_OPTIONS = 40;
    /** The four tabs, in the order they are drawn. */
    public static final int TALK = 0, INTERACT = 1, BOND = 2, POUCH = 3, TABS = 4;
    /** A talk is only possible this close (blocks); beyond it a pick never reaches the companion and the screen is closed. */
    public static final double REACH = 8.0;
    /** Minimum gap between two picks of one player, and between two openings (ticks). */
    public static final int PICK_GAP_TICKS = 4, OPEN_GAP_TICKS = 10;
    /** Longest id the packets will read (a little over the limit so a too-long id is read and then refused, not mistaken for a broken packet). */
    public static final int WIRE_ID = MAX_ID + 8;
    /** The longest name, sub line, text or option label of a view, counted in plain characters, after which it is cut (a lang key is short; this only stops a pathological component). */
    public static final int MAX_TEXT = 600;

    public static final String BOND_PREFIX = "k:", GROWTH_PREFIX = "g:", SCREEN_PREFIX = "x:";

    /** Is the id in the space of the bond system, the growth panel or the screen? */
    public static boolean reserved(String id) {
        return id != null && (id.startsWith(BOND_PREFIX) || id.startsWith(GROWTH_PREFIX) || id.startsWith(SCREEN_PREFIX));
    }

    /**
     * Why an id is not allowed, or null when it is. {@code system} is true for the code that may use the reserved prefixes (the bond system); a companion's own topics pass false.
     */
    public static String idProblem(String id, boolean system) {
        if (id == null || id.isEmpty()) return "an option id is empty";
        if (id.length() > MAX_ID) return "an option id is longer than " + MAX_ID + " characters: " + id.length();
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c <= 32 || c == 127 || c == '§') return "an option id has a space, a control character or a colour code";
        }
        if (!system && reserved(id)) return "ids beginning k:, g: or x: are reserved (" + id + ")";
        return null;
    }

    public static boolean validTab(int tab) { return tab >= 0 && tab < TABS; }

    /** The tab number a packet carries, kept inside the valid ones (an unknown tab reads as Talk). */
    public static int cleanTab(int tab) { return validTab(tab) ? tab : TALK; }

    /** Is the player close enough to talk (squared distance)? */
    public static boolean inReach(double distanceSqr) { return distanceSqr <= REACH * REACH; }

    /** May the server act on a pick: the id is well formed (the reserved prefixes are allowed here: they are the bond system's own) and the last view really offered it. */
    public static boolean acceptPick(String id, Set<String> offered) {
        return offered != null && idProblem(id, true) == null && offered.contains(id);
    }

    /** Cuts a text to {@link #MAX_TEXT} characters. Never null. */
    public static String cut(String s) {
        if (s == null) return "";
        return s.length() <= MAX_TEXT ? s : s.substring(0, MAX_TEXT);
    }
}
