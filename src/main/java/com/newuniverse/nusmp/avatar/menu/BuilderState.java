package com.newuniverse.nusmp.avatar.menu;

import com.newuniverse.nusmp.avatar.AvatarPersonality;

import java.util.Locale;

/**
 * Why: the creation screen has four pages with rules about when you may leave one (you cannot choose a race before you have rolled; you cannot create
 * before the race, the primary personality and a valid name are in place). Keeping those rules in a pure state machine, away from widgets, means the
 * screen only draws and forwards clicks, and the rules are unit-tested. The server re-validates everything anyway: this only decides which buttons
 * are lit.
 */
public final class BuilderState {
    public static final int PAGE_STATS = 0, PAGE_RACE = 1, PAGE_LOOK = 2, PAGE_CONFIRM = 3, PAGES = 4;
    public static final int NAME_MIN = 2, NAME_MAX = 24;

    private int page = PAGE_STATS;
    private boolean rolled, busy;
    private String race = "";
    private boolean raceAvailable;
    private AvatarPersonality primary, secondary;
    private String name = "";

    public int page() { return page; }
    public boolean rolled() { return rolled; }
    public boolean busy() { return busy; }
    public String race() { return race; }
    public AvatarPersonality primary() { return primary; }
    public AvatarPersonality secondary() { return secondary; }
    public String name() { return name; }

    public void onRolled() { rolled = true; }
    public void setBusy(boolean b) { busy = b; }
    public void setName(String n) { name = n == null ? "" : n; }

    public void chooseRace(String id, boolean available) { race = id == null ? "" : id; raceAvailable = available && !race.isEmpty(); }

    public void choosePrimary(AvatarPersonality p) {
        primary = p;
        if (secondary == p) secondary = null;     // the same trope twice is not a hybrid
    }

    /** The secondary personality is optional; it can never equal the primary. */
    public void chooseSecondary(AvatarPersonality p) { secondary = p == primary ? null : p; }

    /** May the player go forward from the current page? */
    public boolean canNext() {
        if (busy) return false;
        return switch (page) {
            case PAGE_STATS -> rolled;
            case PAGE_RACE -> raceAvailable && primary != null;
            case PAGE_LOOK -> true;
            default -> false;
        };
    }

    public boolean next() {
        if (!canNext()) return false;
        page++;
        return true;
    }

    public boolean back() {
        if (busy || page == PAGE_STATS) return false;
        page--;
        return true;
    }

    /** Jump to a page by its tab: allowed to any page already earned (never past a page whose conditions are not met). */
    public boolean goTo(int target) {
        if (busy || target < 0 || target >= PAGES) return false;
        while (page < target) if (!next()) return false;
        while (page > target) back();
        return true;
    }

    /** The optional name: empty is fine; otherwise 2..24 of letters, digits, space, apostrophe, hyphen (the server applies the same rule). */
    public static boolean nameOk(String n) {
        if (n == null || n.isEmpty()) return true;
        String t = n.strip();
        if (t.length() < NAME_MIN || t.length() > NAME_MAX) return false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == ' ' || c == '\'' || c == '-')) return false;
        }
        return true;
    }

    public boolean canCreate() {
        return !busy && rolled && raceAvailable && primary != null && nameOk(name);
    }

    /** "Primary + secondary" line key parts for the hybrid preview, lower case; empty when there is no secondary. */
    public String hybridId() {
        if (primary == null || secondary == null) return "";
        return (primary.name() + "_" + secondary.name()).toLowerCase(Locale.ROOT);
    }
}
