package com.newuniverse.nusmp.weaponpage;

import java.util.List;

/**
 * Why: the Weapon page of the Codex (docs/ui_weapon_page.md) asks the server for exactly one thing at a time, and everything the server must refuse is decided here, pure (no game types), so it is unit-tested
 * ({@code WeaponPageRulesTest}): which weapon ids and actions exist, which arguments are well formed, the two cooldowns, the page checksum that lets the server skip a sync the client already holds, and the
 * limits that bound every list in the packets. The client never decides anything: it names a weapon, an action and an argument; the server checks them here, then again against the game state (ownership, the
 * weapon's own rules and cooldowns) before it changes anything. Entries of {@link #WEAPONS} are only ever appended.
 */
public final class WeaponPageRules {
    private WeaponPageRules() {}

    /** The weapons the page knows, in page order: the hero / vassal weapon of the bond, the four Lord weapons (the order of {@code LordRules.Sin}), Greed. */
    public static final List<String> WEAPONS = List.of("hero", "sloth", "envy", "vanity", "holy", "greed");

    /** What a click asks for. OPEN has no weapon and carries the checksum of the client's copy. */
    public static final int ACT_OPEN = 0, ACT_FORM = 1, ACT_LEARN = 2, ACT_FEED = 3, ACT_HALF = 4, ACTIONS = 5;

    /** What an option shows: shut (no click), ready (a click does it), current (the form in use / the stance that is on). */
    public static final int ST_LOCKED = 0, ST_READY = 1, ST_CURRENT = 2;

    /** Bounds of every list and string of the two packets. */
    public static final int MAX_ENTRIES = 6, MAX_BARS = 4, MAX_OPTIONS = 24, TEXT = 96, ID = 48, ITEM_ID = 96, ARG = 96;

    /** Forms of a Lord weapon (the item ids of one sin) and of Greed (the eight forms of the Black Sword). */
    public static final int LORD_FORMS = 3, GREED_FORMS = 8;

    /** Game ticks that must pass between two page requests of one player: a click, and an open (which may build a whole page). */
    public static final int GAP_ACT_TICKS = 10, GAP_OPEN_TICKS = 20;

    public enum Verdict { OK, BAD_WEAPON, BAD_ACTION, BAD_ARG }

    public static boolean isWeapon(String w) { return w != null && WEAPONS.contains(w); }

    public static boolean isLord(String w) { return isWeapon(w) && !w.equals("hero") && !w.equals("greed"); }

    /** The number of numbered forms of a weapon (0 for the hero weapon, whose forms are named by data). */
    public static int forms(String w) { return isLord(w) ? LORD_FORMS : "greed".equals(w) ? GREED_FORMS : 0; }

    /** The request, checked as the server receives it. The weapon, the action and the argument must fit together; nothing here looks at the game. */
    public static Verdict check(String weapon, int action, String arg) {
        if (action < 0 || action >= ACTIONS) return Verdict.BAD_ACTION;
        if (arg == null || arg.length() > ARG) return Verdict.BAD_ARG;
        if (action == ACT_OPEN) {
            if (weapon == null || !weapon.isEmpty()) return Verdict.BAD_WEAPON;
            return arg.isEmpty() || isInt(arg) ? Verdict.OK : Verdict.BAD_ARG;
        }
        if (!isWeapon(weapon)) return Verdict.BAD_WEAPON;
        boolean hero = weapon.equals("hero"), greed = weapon.equals("greed");
        switch (action) {
            case ACT_FORM:
                if (hero) return isFormId(arg) ? Verdict.OK : Verdict.BAD_ARG;
                return isIndex(arg, forms(weapon)) ? Verdict.OK : Verdict.BAD_ARG;
            case ACT_LEARN:
                if (!hero) return Verdict.BAD_ACTION;
                return isFormId(arg) ? Verdict.OK : Verdict.BAD_ARG;
            case ACT_FEED:
                if (!hero) return Verdict.BAD_ACTION;
                return isItemId(arg) ? Verdict.OK : Verdict.BAD_ARG;
            default:                                                                    // ACT_HALF
                if (!greed) return Verdict.BAD_ACTION;
                return arg.isEmpty() ? Verdict.OK : Verdict.BAD_ARG;
        }
    }

    /** The minimum gap in ticks after a request of this action. */
    public static int gap(int action) { return action == ACT_OPEN ? GAP_OPEN_TICKS : GAP_ACT_TICKS; }

    /** True when {@code gap} ticks have passed since {@code last} ({@code last} below 0 = never); a world clock set back counts as ready, never as a lock for ever. */
    public static boolean ready(long now, long last, int gap) { return last < 0 || now < last || now - last >= gap; }

    /** Progress to the next Black Sword tier in percent (0..99, 100 at the top tier) from the stored EP, with the sword's own thresholds supplied by the caller (lowest first, one per tier). */
    public static int tierPct(double ep, double[] thresholds) {
        int t = 0;
        for (int i = 1; i < thresholds.length; i++) if (ep >= thresholds[i]) t = i;
        if (t >= thresholds.length - 1) return 100;
        double lo = thresholds[t], hi = thresholds[t + 1];
        if (hi <= lo || ep <= lo) return 0;
        return (int) Math.max(0, Math.min(99, Math.round((ep - lo) * 100.0 / (hi - lo))));
    }

    // ---------------------------------------------------------------- the checksum (FNV-1a over what the page shows)
    public static final int SEED = 0x811C9DC5;

    public static int mix(int h, int v) {
        h ^= v & 0xFF;
        h *= 16777619;
        h ^= (v >>> 8) & 0xFF;
        h *= 16777619;
        h ^= (v >>> 16) & 0xFF;
        h *= 16777619;
        h ^= (v >>> 24) & 0xFF;
        return h * 16777619;
    }

    public static int mix(int h, String s) {
        String t = s == null ? "" : s;
        h = mix(h, t.length());
        for (int i = 0; i < t.length(); i++) h = mix(h, (int) t.charAt(i));
        return h;
    }

    // ---------------------------------------------------------------- argument shapes
    /** A hero form id: lower case letters, digits and underscores, 1 to {@link #ID}. */
    public static boolean isFormId(String s) {
        if (s == null || s.isEmpty() || s.length() > ID) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_')) return false;
        }
        return true;
    }

    /** An item id {@code namespace:path}: both parts non-empty, only the characters a resource location allows. */
    public static boolean isItemId(String s) {
        if (s == null || s.isEmpty() || s.length() > ITEM_ID) return false;
        int colon = s.indexOf(':');
        if (colon <= 0 || colon == s.length() - 1 || s.indexOf(':', colon + 1) >= 0) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.' || c == ':' || (c == '/' && i > colon);
            if (!ok) return false;
        }
        return true;
    }

    /** A decimal number of one or two digits that is below {@code limit}. */
    public static boolean isIndex(String s, int limit) {
        if (s == null || s.isEmpty() || s.length() > 2) return false;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) < '0' || s.charAt(i) > '9') return false;
        return Integer.parseInt(s) < limit;
    }

    /** An optionally negative int written in decimal (the checksum the client holds). */
    public static boolean isInt(String s) {
        if (s == null || s.isEmpty() || s.length() > 11) return false;
        int i = s.charAt(0) == '-' ? 1 : 0;
        if (i == s.length()) return false;
        for (; i < s.length(); i++) if (s.charAt(i) < '0' || s.charAt(i) > '9') return false;
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException tooBig) {
            return false;
        }
    }
}
