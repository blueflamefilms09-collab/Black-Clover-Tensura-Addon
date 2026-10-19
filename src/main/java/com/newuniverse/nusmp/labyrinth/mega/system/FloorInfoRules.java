package com.newuniverse.nusmp.labyrinth.mega.system;

import com.newuniverse.nusmp.labyrinth.mega.enc.DepthDropRules;
import com.newuniverse.nusmp.labyrinth.mega.enc.DepthRules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Why: when the System tells a player about the floor they have just reached, a handful of DECISIONS sit between "the game fired a floor event" and "a window
 * appears": which words describe the move (UP or DOWN, arrived or going on or going back), how strong the monsters of this depth are, whether the report may be
 * shown again yet (the owner's rule is no spam: once per arrival, a cooldown for a staircase walked up and down, a short gap for the key), and what changed in the
 * set of mechanics so that a change in the middle of a floor can be announced once. All of it is plain arithmetic and logic with no Minecraft type, so every rule
 * is a line in {@code FloorInfoRulesTest} and the game classes ({@link FloorInfo}) only gather the facts and carry the answer out. The direction is read from the
 * SIGNED floor number: floors grow towards +1, +2 ... on the Holy road (UP) and towards -1, -2 ... on the Abyssal road (DOWN), so a move to a higher number is
 * UP on any road, and a player who has no previous floor (a login, a respawn, a fresh arrival from the Hub) is told the direction of the road they stand on.
 */
public final class FloorInfoRules {
    private FloorInfoRules() {}

    /** Why a report is being shown. The code travels over the wire, so it is never renumbered. */
    public enum Reason {
        /** A first look: a login, a respawn, an arrival from the Hub, a party member joining. */
        ARRIVE(0),
        /** The player went on to a floor farther from the Hub than the one they left. */
        FORWARD(1),
        /** The player went back towards the Hub. */
        BACK(2),
        /** The player asked (the key, the command). */
        REQUEST(3),
        /** The mechanics of the floor changed while the player stood on it. */
        UPDATE(4);

        private final int code;

        Reason(int code) { this.code = code; }

        public int code() { return code; }

        public static Reason fromCode(int code) {
            for (Reason r : values()) if (r.code == code) return r;
            return ARRIVE;
        }
    }

    /** The reason a floor event has: no previous floor is an arrival, a deeper floor is going on, a shallower one is going back. */
    public static Reason reasonFor(int previous, int floor) {
        if (previous == 0 || floor == 0 || Integer.signum(previous) != Integer.signum(floor)) return Reason.ARRIVE;
        return Math.abs(floor) > Math.abs(previous) ? Reason.FORWARD : Reason.BACK;
    }

    /**
     * Is the move UP? A higher signed number is up on any road (+3 to +4, and -4 to -3). Without a previous floor the road decides: the Holy road climbs, the Abyssal one
     * descends, and the signed floor itself says which.
     */
    public static boolean up(int previous, int floor) {
        if (previous == 0 || floor == 0 || Integer.signum(previous) != Integer.signum(floor)) return floor > 0;
        return floor > previous;
    }

    // ================================================================ the depth
    /** What a depth does to the monsters, for the report's last line. {@code xpTimes100} is the experience multiplier times 100 (109 = x1.09). */
    public record Depth(int depth, int healthPct, int damagePct, int mind, int xpTimes100) {
        public boolean flat() { return healthPct <= 0 && damagePct <= 0 && mind <= 0 && xpTimes100 <= 100; }
    }

    /** The strength, the mind level and the experience multiplier of a signed floor, from the rules of the Dungeon Master's depth layer (the same functions the monsters use). */
    public static Depth depth(int floor, int[] mindFloors, DepthDropRules.Params drops, double strengthScale) {
        int depth = DepthRules.depthOf(floor);
        DepthRules.Strength s = DepthRules.strength(depth).times(strengthScale);
        int[] floors = mindFloors == null ? DepthRules.parseThresholds(DepthRules.DEFAULT_MIND_FLOORS) : mindFloors;
        int xp = (int) Math.round(DepthDropRules.xpMultiplier(depth, drops == null ? DepthDropRules.DEFAULTS : drops) * 100.0);
        return new Depth(depth, (int) Math.round(s.health() * 100.0), (int) Math.round(s.damage() * 100.0), DepthRules.mind(depth, floors), xp);
    }

    /** A multiplier times 100 as text without a pointless zero: 109 is "1.09", 120 is "1.2", 100 is "1". */
    public static String times(int hundredths) {
        String s = String.format(Locale.ROOT, "%.2f", hundredths / 100.0);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    /** A theme's danger multiplier as text: 0.9, 1.5, 1. */
    public static String dangerText(double danger) { return times((int) Math.round(danger * 100.0)); }

    // ================================================================ the words of a theme
    /** The line used for a theme that has no built-in description: what its data says (the catastrophe its Crucible favours), else a plain sentence. */
    public static String genericThemeDesc(String favouredCatastrophe) {
        if (favouredCatastrophe == null || favouredCatastrophe.isEmpty()) return FloorInfoText.englishOrNull(FloorInfoText.THEME_GENERIC_PLAIN);
        return String.format(Locale.ROOT, FloorInfoText.englishOrNull(FloorInfoText.THEME_GENERIC).replace("%1$s", "%s"), favouredCatastrophe);
    }

    /** The catastrophe with the highest weight in a theme's table, or "" when none has any weight. Ties go to the alphabetically first. */
    public static String favouredCatastrophe(java.util.Map<String, Integer> weights) {
        String best = "";
        int bestW = 0;
        if (weights == null) return best;
        for (String k : new TreeSet<>(weights.keySet())) {
            int w = weights.getOrDefault(k, 0);
            if (w > bestW) {
                best = k;
                bestW = w;
            }
        }
        return best;
    }

    /** A readable name from an id when nothing better exists: {@code feywild_grove} becomes {@code Feywild Grove}. */
    public static String humanize(String id) {
        if (id == null || id.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String part : id.split("[_.]")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    /** The alignment code a card carries: {@code holy}, {@code abyssal} or {@code any}, with {@code _only} added for a theme that is drawn on its own road only. */
    public static String alignCode(String route, boolean exclusive) {
        String r = route == null ? "any" : route.toLowerCase(Locale.ROOT);
        if (!r.equals("holy") && !r.equals("abyssal")) return "any";
        return exclusive ? r + "_only" : r;
    }

    // ================================================================ no spam
    /** How often a report may appear. Ticks. */
    public record Gap(int arriveCooldown, int updateCooldown, int requestGap) {
        public static final Gap DEFAULT = new Gap(600, 200, 10);
    }

    /**
     * One player's memory of what they were shown and when. {@link #offer} answers whether a report for this reason may be shown now, and if so remembers it:
     * an arrival is always shown for a floor the player was not just on, and for the SAME floor only after the cooldown (walking a staircase up and down does not
     * repeat the report); a request is shown unless the previous request was a moment ago; a mid-floor update is shown only after its cooldown AND a moment after the
     * last report, so the arrival report and the update for the mechanics that woke at the same time are not two windows.
     */
    public static final class Throttle {
        private int shownFloor;
        private long shownAt = Long.MIN_VALUE / 2, updatedAt = Long.MIN_VALUE / 2, requestedAt = Long.MIN_VALUE / 2;

        public boolean offer(Reason reason, int floor, long now, Gap gap) {
            switch (reason) {
                case REQUEST -> {
                    if (now - requestedAt < gap.requestGap()) return false;
                    requestedAt = now;
                    shownAt = now;
                    shownFloor = floor;
                    return true;
                }
                case UPDATE -> {
                    if (now - updatedAt < gap.updateCooldown() || now - shownAt < Math.min(40, gap.updateCooldown())) return false;
                    updatedAt = now;
                    return true;
                }
                default -> {
                    if (floor == shownFloor && now - shownAt < gap.arriveCooldown()) return false;
                    shownFloor = floor;
                    shownAt = now;
                    updatedAt = now;
                    return true;
                }
            }
        }

        public int shownFloor() { return shownFloor; }

        public long shownAt() { return shownAt; }
    }

    // ================================================================ what changed
    /** The difference between two sets of "id|state" entries (what a mid-floor watcher compares). */
    public record Diff(List<String> added, List<String> removed) {
        public boolean any() { return !added.isEmpty() || !removed.isEmpty(); }
    }

    /** What is new in {@code after} and what is gone from {@code before}, each sorted so the answer does not depend on the order of a hash set. */
    public static Diff diff(Set<String> before, Set<String> after) {
        List<String> added = new ArrayList<>(), removed = new ArrayList<>();
        if (after != null) for (String s : new TreeSet<>(after)) if (before == null || !before.contains(s)) added.add(s);
        if (before != null) for (String s : new TreeSet<>(before)) if (after == null || !after.contains(s)) removed.add(s);
        return new Diff(Collections.unmodifiableList(added), Collections.unmodifiableList(removed));
    }

    /** The id half of an "id|state" entry. */
    public static String entryId(String entry) {
        int i = entry == null ? -1 : entry.indexOf('|');
        return i < 0 ? String.valueOf(entry) : entry.substring(0, i);
    }

    /** The state half of an "id|state" entry ("" when there is none). */
    public static String entryState(String entry) {
        int i = entry == null ? -1 : entry.indexOf('|');
        return i < 0 ? "" : entry.substring(i + 1);
    }

    // ================================================================ the wire limits
    /** A text cut to {@code max} characters (the wire format has bounded strings, and a datapack text can be any length). */
    public static String clip(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return max <= 3 ? s.substring(0, Math.max(0, max)) : s.substring(0, max - 3) + "...";
    }
}
