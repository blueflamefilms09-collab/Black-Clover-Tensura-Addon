package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;

import java.util.UUID;

/**
 * A {@link CollapseState} as two plain arrays, an int[] and a long[], and back: what {@link CollapseSave} writes into the world's data.
 * Keeping the layout here, with no Minecraft types, lets the round trip be tested, and lets a damaged record (cut short, a count that is
 * nonsense, an unknown version) be refused instead of crashing the load. The first int is the layout's version; never reorder, only append
 * a new version.
 */
public final class CollapseCodec {
    private CollapseCodec() {}

    public static final int VERSION = 1;
    /** The most exits (and Root Gates) a record may claim: more is a damaged record. */
    public static final int LIMIT = 64;

    private static final int FIXED = 21;
    private static final int NEXUS = 1, STARTED = 2, EXPELLED = 4;

    /** The int part: version, zone, flags, cell, centre, footprint, heights, the five counters, then the exits and the gates. */
    public static int[] ints(CollapseState s) {
        int gateInts = 1;
        for (int[] g : s.gates) gateInts += 1 + (g.length >= 4 ? 4 : 0);
        int[] a = new int[FIXED + 3 * s.exits.size() + gateInts];
        int i = 0;
        a[i++] = VERSION;
        a[i++] = s.zone.ordinal();
        a[i++] = (s.nexus ? NEXUS : 0) | (s.started ? STARTED : 0) | (s.expelled ? EXPELLED : 0);
        a[i++] = s.cellX;
        a[i++] = s.cellZ;
        a[i++] = s.centreX;
        a[i++] = s.centreY;
        a[i++] = s.centreZ;
        a[i++] = s.fp.shape().ordinal();
        a[i++] = s.fp.centreX();
        a[i++] = s.fp.centreZ();
        a[i++] = s.fp.halfX();
        a[i++] = s.fp.halfZ();
        a[i++] = s.floorY;
        a[i++] = s.topY;
        a[i++] = s.cursor;
        a[i++] = s.wipeCursor;
        a[i++] = s.nextMsg;
        a[i++] = s.nextWave;
        a[i++] = s.spawned;
        a[i++] = s.exits.size();
        for (int[] e : s.exits) {
            a[i++] = e[0];
            a[i++] = e[1];
            a[i++] = e[2];
        }
        a[i++] = s.gates.size();
        for (int[] g : s.gates) {
            if (g.length >= 4) {
                a[i++] = 4;
                a[i++] = g[0];
                a[i++] = g[1];
                a[i++] = g[2];
                a[i++] = g[3];
            } else {
                a[i++] = 0;
            }
        }
        return a;
    }

    /** The long part: the collapse's number, the Dungeon's game time at the trigger, then the players who were inside as pairs of UUID halves. */
    public static long[] longs(CollapseState s) {
        long[] a = new long[2 + 2 * s.inside.size()];
        a[0] = s.id;
        a[1] = s.startTick;
        int i = 2;
        for (UUID u : s.inside) {
            a[i++] = u.getMostSignificantBits();
            a[i++] = u.getLeastSignificantBits();
        }
        return a;
    }

    /** The state the arrays describe, or null if they are damaged or of a layout this code does not know. */
    public static CollapseState decode(int[] a, long[] l) {
        if (a == null || l == null || a.length < FIXED + 1 || l.length < 2 || a[0] != VERSION) return null;
        try {
            CollapseState s = new CollapseState();
            int i = 1;
            NexusZone[] zones = NexusZone.values();
            s.zone = a[i] >= 0 && a[i] < zones.length ? zones[a[i]] : NexusZone.GOLDEN_CITY;
            i++;
            int flags = a[i++];
            s.nexus = (flags & NEXUS) != 0;
            s.started = (flags & STARTED) != 0;
            s.expelled = (flags & EXPELLED) != 0;
            s.cellX = a[i++];
            s.cellZ = a[i++];
            s.centreX = a[i++];
            s.centreY = a[i++];
            s.centreZ = a[i++];
            int shape = a[i++];
            Footprint.Shape[] shapes = Footprint.Shape.values();
            if (shape < 0 || shape >= shapes.length) return null;
            int fx = a[i++], fz = a[i++], hx = a[i++], hz = a[i++];
            if (hx < 1 || hz < 1) return null;
            s.fp = new Footprint(shapes[shape], fx, fz, hx, hz);
            s.floorY = a[i++];
            s.topY = a[i++];
            if (s.topY <= s.floorY) return null;
            s.cursor = Math.max(0, a[i++]);
            s.wipeCursor = Math.max(0, a[i++]);
            s.nextMsg = Math.max(0, a[i++]);
            s.nextWave = Math.max(0, a[i++]);
            s.spawned = Math.max(0, a[i++]);
            int exits = a[i++];
            if (exits < 0 || exits > LIMIT || a.length < i + 3 * exits + 1) return null;
            for (int k = 0; k < exits; k++) {
                s.exits.add(new int[]{a[i], a[i + 1], a[i + 2]});
                i += 3;
            }
            int gates = a[i++];
            if (gates < 0 || gates > LIMIT) return null;
            for (int k = 0; k < gates; k++) {
                if (i >= a.length) return null;
                int len = a[i++];
                if (len == 0) {
                    s.gates.add(new int[0]);
                } else if (len == 4 && i + 4 <= a.length) {
                    s.gates.add(new int[]{a[i], a[i + 1], a[i + 2], a[i + 3]});
                    i += 4;
                } else {
                    return null;
                }
            }
            while (s.gates.size() < s.exits.size()) s.gates.add(new int[0]);          // one gate slot for every exit, however the record was cut
            while (s.gates.size() > s.exits.size()) s.gates.remove(s.gates.size() - 1);
            s.id = l[0];
            s.startTick = l[1];
            for (int k = 2; k + 1 < l.length; k += 2) s.inside.add(new UUID(l[k], l[k + 1]));
            return s;
        } catch (RuntimeException broken) {
            return null;
        }
    }
}
