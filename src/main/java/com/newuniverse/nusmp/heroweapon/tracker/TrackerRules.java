package com.newuniverse.nusmp.heroweapon.tracker;

import com.newuniverse.nusmp.heroweapon.HeroClass;
import com.newuniverse.nusmp.heroweapon.HeroKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Why: the Weapon Tracker answers one question for a player, "which of the 21 hero weapons are still free?", and the answer must be the same on the server (which builds it) and in the test (which pins it),
 * so the whole rule is pure Java with no Minecraft type. It takes a snapshot of the register (weapon item id to holder uuids), the settings and the viewer, and gives one {@link Row} per weapon in catalogue
 * order. A weapon is FREE (nobody holds it), YOURS (the viewer holds it), TAKEN (someone else holds it and the unique setting of its class is on, so the viewer cannot have it) or SHARED (someone else holds it
 * but the unique setting is off, so the viewer still can; the row carries the holder count). The class mapping is the one the bind gate uses: {@code holyUnique} for the four Legendary Weapons,
 * {@code vassalUnique} for the Seven Star and the Vassal Weapons. A holder's name is only given when {@code trackerShowNames} is on and the name is known; otherwise it is empty and the screen says "another
 * player". At most one holder is named (the first one that is online, else the first), so the work and the packet stay bounded however many players share a weapon.
 */
public final class TrackerRules {
    private TrackerRules() {}

    /** The status of one weapon from the viewer's side. */
    public enum Status { FREE, TAKEN, YOURS, SHARED }

    /** The chips of the screen: a class, a status, or everything. */
    public enum Filter { ALL, LEGENDARY, SEVEN_STAR, VASSAL, FREE, TAKEN, YOURS }

    /** Longest holder name sent (a Minecraft name has 16 characters; the cap guards a corrupt or modded value). */
    public static final int MAX_NAME = 24;

    /** The most rows a table can carry: one per weapon. */
    public static int maxRows() { return HeroKind.values().length; }

    /**
     * The settings the answer depends on. {@code maxBonds} is the server's bond limit (0 closes the door, 1 is one weapon per player).
     */
    public record Settings(boolean enabled, boolean holyUnique, boolean vassalUnique, boolean showNames, int maxBonds) {}

    /** The one asking: their uuid text and the weapon their own bond says they hold (null for none). */
    public record Viewer(String uuid, HeroKind bond) {}

    /**
     * One weapon. {@code holders} counts every holder in the register (the viewer included); {@code holder} is the one named holder or empty when nobody holds it, the names are hidden or the name is unknown;
     * {@code online} is whether that named holder is online (false with no holder); {@code unique} is whether a second holder is refused for this class.
     */
    public record Row(HeroKind kind, Status status, int holders, String holder, boolean online, boolean unique) {}

    /** The full answer: the settings that shaped it, the viewer's own weapon (null for none) and the rows. */
    public record Table(Settings settings, HeroKind own, List<Row> rows) {}

    /** Whether a second holder is refused for this weapon (the same mapping the bind gate uses). */
    public static boolean unique(HeroKind kind, Settings s) { return kind.isHoly() ? s.holyUnique() : s.vassalUnique(); }

    /** The class name the screen shows: Legendary, Seven Star or Vassal. */
    public static String classLabel(HeroClass c) {
        return switch (c) {
            case HOLY -> "Legendary";
            case SEVEN_STAR -> "Seven Star";
            case VASSAL -> "Vassal";
        };
    }

    /**
     * Builds the table. {@code holders} maps a weapon item id (the registry path, {@code legendary_shield}) to its holder uuid texts; a weapon with no entry is free. {@code nameOf} turns a uuid text into a name
     * ("" for unknown) and is called at most once per weapon, and never when names are hidden; {@code online} says whether a uuid text is online and is called for the holders of a weapon until one is found.
     */
    public static Table build(Map<String, ? extends List<String>> holders, Settings s, Viewer viewer, Function<String, String> nameOf, Predicate<String> online) {
        String me = viewer == null ? null : viewer.uuid();
        HeroKind own = viewer == null ? null : viewer.bond();
        List<Row> rows = new ArrayList<>(maxRows());
        for (HeroKind k : HeroKind.values()) {
            List<String> list = holders == null ? null : holders.get(k.itemId());
            int n = list == null ? 0 : list.size();
            boolean unique = unique(k, s);
            boolean mine = k == own || me != null && list != null && list.contains(me);
            if (mine && own == null) own = k;                                         // the register says so even if the bond has not synced yet
            if (n == 0 && !mine) {
                rows.add(new Row(k, Status.FREE, 0, "", false, unique));
                continue;
            }
            if (n == 0) n = 1;                                                        // the bond says so even if the register lost the claim
            Status status = mine ? Status.YOURS : unique ? Status.TAKEN : Status.SHARED;
            String named = pick(list, me, mine, online);
            boolean isOnline = named != null && online.test(named);
            String name = "";
            if (named != null && s.showNames() && !mine) name = clean(nameOf.apply(named));
            rows.add(new Row(k, status, n, name, isOnline, unique));
        }
        return new Table(s, own, List.copyOf(rows));
    }

    /** The holder to name: for the viewer's own row the viewer; else the first online holder, else the first. Null when there is none. */
    private static String pick(List<String> list, String me, boolean mine, Predicate<String> online) {
        if (mine) return me;
        if (list == null || list.isEmpty()) return null;
        for (String h : list) if (online.test(h)) return h;
        return list.get(0);
    }

    /** A name cut to {@link #MAX_NAME}, with control characters dropped; null becomes empty. */
    public static String clean(String name) {
        if (name == null) return "";
        StringBuilder b = new StringBuilder(Math.min(name.length(), MAX_NAME));
        for (int i = 0; i < name.length() && b.length() < MAX_NAME; i++) {
            char c = name.charAt(i);
            if (c >= 32 && c != 127) b.append(c);
        }
        return b.toString().strip();
    }

    // ================================================================ the screen's filters and scrolling
    /** True when the row passes the chip. TAKEN covers both TAKEN and SHARED (held by someone else); FREE is free only. */
    public static boolean matches(Row r, Filter f) {
        return switch (f) {
            case ALL -> true;
            case LEGENDARY -> r.kind().heroClass() == HeroClass.HOLY;
            case SEVEN_STAR -> r.kind().heroClass() == HeroClass.SEVEN_STAR;
            case VASSAL -> r.kind().heroClass() == HeroClass.VASSAL;
            case FREE -> r.status() == Status.FREE;
            case TAKEN -> r.status() == Status.TAKEN || r.status() == Status.SHARED;
            case YOURS -> r.status() == Status.YOURS;
        };
    }

    /** The rows that pass the chip, in order. */
    public static List<Row> filter(List<Row> rows, Filter f) {
        List<Row> out = new ArrayList<>(rows.size());
        for (Row r : rows) if (matches(r, f)) out.add(r);
        return out;
    }

    /** The next chip to the right ({@code step} 1) or left ({@code step} -1), wrapping round. */
    public static Filter step(Filter f, int step) {
        Filter[] all = Filter.values();
        return all[Math.floorMod(f.ordinal() + step, all.length)];
    }

    /** A scroll offset (in rows) kept inside {@code 0 .. total - visible}. */
    public static int clampScroll(int scroll, int total, int visible) {
        return Math.max(0, Math.min(scroll, Math.max(0, total - visible)));
    }

    /** How many weapons of the table are free, for the header line. */
    public static int countFree(List<Row> rows) {
        int n = 0;
        for (Row r : rows) if (r.status() == Status.FREE) n++;
        return n;
    }
}
