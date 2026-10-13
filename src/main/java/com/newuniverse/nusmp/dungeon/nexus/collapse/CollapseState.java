package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import com.newuniverse.nusmp.dungeon.nexus.NexusZone;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Everything one collapse is, as plain data (saved by {@link CollapseSave}): where it is centred, what ground it covers, when it began,
 * the exits and the Root Gates at them, how far each of its jobs has got (the sweep's cursor, the wipe's cursor, the next countdown
 * message, the next guard wave) and who stands inside. No Minecraft types, so the whole run can be played in a test. Mutate it only
 * from the server thread; the save is marked dirty by its owner.
 */
public final class CollapseState {
    /** A collapse's number, stable for its life (the horde and falling blocks carry it). */
    public long id;
    /** Where the event said the trouble is (a pedestal, a boss, a player). */
    public int centreX, centreY, centreZ;
    public NexusZone zone = NexusZone.GOLDEN_CITY;
    /** True when a standing Nexus of core cell (cellX, cellZ) collapses; false for a pocket of the Dungeon (a collapse outside any Nexus: an admin's). */
    public boolean nexus;
    public int cellX, cellZ;
    /** The Dungeon's game time at the trigger. */
    public long startTick;
    /** The ground that collapses, and the y of the floor (kept) and of the highest block that falls. */
    public Footprint fp = Footprint.oval(0, 0, 1, 1);
    public int floorY, topY;
    /** The ways out: {x, y, z} of the spot where a player's feet go. */
    public final List<int[]> exits = new ArrayList<>();
    /** The Root Gate at each exit (same index): {x, y, z, mine} with mine = 1 if the collapse placed it, or an empty array while there is none. */
    public final List<int[]> gates = new ArrayList<>();
    public int cursor, wipeCursor, nextMsg, nextWave, spawned;
    /** The opening (broadcast, gates, the air taken) has been done. */
    public boolean started;
    /** The wipe has thrown out whoever was inside. */
    public boolean expelled;
    /** The ids of the players who were inside at the last look. */
    public final Set<UUID> inside = new LinkedHashSet<>();

    /** The number of the Nexus's core cell as the Nexus save keys it, or 0 for a pocket. */
    public long cellKey() { return nexus ? ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL) : 0L; }

    public long elapsed(long now) { return Math.max(0L, now - startTick); }

    /** Blocks round the footprint that are swept too (its edge counts as inside). */
    public static final int MARGIN = 1;

    /** Where the sweep's order is measured from: the exits themselves, flat. */
    public int[] exitXs() { return axis(0); }

    public int[] exitZs() { return axis(2); }

    private int[] axis(int k) {
        int[] out = new int[exits.size()];
        for (int i = 0; i < out.length; i++) out[i] = exits.get(i)[k];
        return out;
    }

    /** The sweep order of this collapse (the same every time it is asked: nothing it reads changes after the start). */
    public CollapseSweep order() { return CollapseSweep.build(fp, MARGIN, exitXs(), exitZs(), CollapsePlan.CORRIDOR_RADIUS); }

    /** The blocks swept in each column. */
    public ColumnSweep sweep(CollapseSweep order) { return new ColumnSweep(order, floorY + 1, topY + 2); }

    /** A copy of every field, so a test can "restart" from a saved moment. */
    public CollapseState copy() {
        CollapseState c = new CollapseState();
        c.id = id;
        c.centreX = centreX;
        c.centreY = centreY;
        c.centreZ = centreZ;
        c.zone = zone;
        c.nexus = nexus;
        c.cellX = cellX;
        c.cellZ = cellZ;
        c.startTick = startTick;
        c.fp = fp;
        c.floorY = floorY;
        c.topY = topY;
        for (int[] e : exits) c.exits.add(e.clone());
        for (int[] g : gates) c.gates.add(g.clone());
        c.cursor = cursor;
        c.wipeCursor = wipeCursor;
        c.nextMsg = nextMsg;
        c.nextWave = nextWave;
        c.spawned = spawned;
        c.started = started;
        c.expelled = expelled;
        c.inside.addAll(inside);
        return c;
    }
}
