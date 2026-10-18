package com.newuniverse.nusmp.client.mega.system;

import com.newuniverse.nusmp.labyrinth.mega.system.PointMode;
import com.newuniverse.nusmp.labyrinth.mega.system.Stat;
import com.newuniverse.nusmp.labyrinth.mega.system.StatTable;
import com.newuniverse.nusmp.labyrinth.mega.system.SystemPointsNet;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the stat screen and the badge on the window need the facts of the stat points that the old window picture does not carry (the free pool, the per-stat
 * cap, whether taking points back is allowed, the point mode, and what ONE point of each stat does, which on a dedicated server only the server knows). The server
 * sends them as {@link SystemPointsNet.PointsSync} whenever they change; this class is the client's copy, plain static fields written on the client thread by the
 * payload handler and read by the screen and the overlay on the same thread (the pattern of {@link ClientSystem}). The stat POINTS themselves are not here: they
 * arrive in the window picture and stay in {@link ClientSystem}. Before the server has spoken everything holds a harmless default (an empty pool, the shipped cap, no
 * effect lines). Imports no Minecraft class, so it loads anywhere and the unit tests can fill it.
 */
public final class ClientPoints {
    private ClientPoints() {}

    /** One line of "what a point does", already decoded: the attribute id, the way it combines, the amount per point and its ceiling (0 = none). */
    public record Effect(String attribute, StatTable.Op op, double perPoint, double cap) {
        /** The total this effect gives for this many points (the table's own rule: per point, cut to the ceiling, the sign kept). */
        public double amount(int points) { return new StatTable.Effect(attribute, op, perPoint, cap).amount(points); }
    }

    private static int free;
    private static int cap = 30;
    private static boolean allowRespec = true;
    private static PointMode mode = PointMode.CHOOSE;
    @SuppressWarnings("unchecked")
    private static final List<Effect>[] EFFECTS = new List[Stat.COUNT];
    /** Bumped on every sync (a screen rebuilds its text when it changes). */
    private static int version;

    static {
        clear();
    }

    /** The server's facts arrived. An effect with an unknown stat index or operation is dropped (a newer server, a damaged packet). */
    public static void apply(SystemPointsNet.PointsSync p) {
        free = Math.max(0, p.free());
        cap = Math.max(0, p.cap());
        allowRespec = p.allowRespec();
        mode = PointMode.ofCode(p.mode());
        List<Effect>[] next = lists();
        StatTable.Op[] ops = StatTable.Op.values();
        for (SystemPointsNet.Effect e : p.effects()) {
            if (e.stat() < 0 || e.stat() >= Stat.COUNT || e.op() < 0 || e.op() >= ops.length) continue;
            next[e.stat()].add(new Effect(e.attribute(), ops[e.op()], e.perPoint(), e.cap()));
        }
        System.arraycopy(next, 0, EFFECTS, 0, Stat.COUNT);
        version++;
    }

    /** Forgets everything (the player left the world). */
    public static void clear() {
        free = 0;
        cap = 30;
        allowRespec = true;
        mode = PointMode.CHOOSE;
        System.arraycopy(lists(), 0, EFFECTS, 0, Stat.COUNT);
        version++;
    }

    @SuppressWarnings("unchecked")
    private static List<Effect>[] lists() {
        List<Effect>[] l = new List[Stat.COUNT];
        for (int i = 0; i < l.length; i++) l[i] = new ArrayList<>(2);
        return l;
    }

    /** The unspent points. */
    public static int free() { return free; }

    /** The most points one stat can hold. */
    public static int cap() { return cap; }

    /** May the player take placed points back? */
    public static boolean allowRespec() { return allowRespec; }

    /** Where quest stat rewards go. */
    public static PointMode mode() { return mode; }

    /** What one point of this stat does (an empty list when the server sent nothing). */
    public static List<Effect> effects(Stat s) { return EFFECTS[s.ordinal()]; }

    public static int version() { return version; }

    /** How many points a click on a button asks for: the wanted count, cut to the pool and to the room under the cap (the server cuts again). */
    public static int clampRequest(Stat s, int wanted, int have) {
        int room = Math.max(0, cap - have);
        return Math.max(0, Math.min(wanted, Math.min(free, room)));
    }
}
