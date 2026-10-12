package com.newuniverse.nusmp.dungeon.nexus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything a zone says about one of its Nexuses, planned: the ordered ops that build it, where it stands (footprint, floor, roof),
 * where its relics sit, where the treasury gates and the entrances are, and where its creatures are put. A pure, deterministic
 * description with no Minecraft imports: the same cell always gives the same plan. {@link NexusBuilder} applies the ops over several
 * ticks and hands the rest to the zone's {@link ZoneSite#populate}.
 *
 * <p>Conventions: {@code floorY} is the y of the topmost floor block (a player stands one above); a {@link Spot} for a block (relic,
 * gate) is that block's position, a spot for a creature or a person (spawn, entrance) is the block their feet are in.
 */
public final class ZonePlan {
    /** A position in the world. */
    public record Spot(int x, int y, int z) {}

    /** The roles the framework itself knows; a zone may use any other string for its own spawn groups. */
    public static final String ROLE_BOSS = "boss", ROLE_ELITE = "elite", ROLE_GUARD = "guard", ROLE_SWARM = "swarm", ROLE_MINION = "minion", ROLE_ARRIVAL = "arrival";

    /** The volume of a Nexus reaches this far below the floor and above the roof when asking who is "inside". */
    public static final int BELOW = 4, ABOVE = 2;

    public final NexusZone zone;
    public final int cellX, cellZ;
    public final Footprint footprint;
    public final int floorY, topY;
    public final List<NexusOp> ops;
    private final int[] phaseStart;
    /** Where the central relic's pedestal stands (null if the zone has none: its collapse comes from the boss). */
    public final Spot centralRelic;
    public final List<Spot> minorRelics, gates, entrances;
    private final Map<String, List<Spot>> spawns;
    /** The chunks the Nexus writes into, worked out on first use. */
    private List<int[]> allChunks;

    private ZonePlan(Builder b) {
        this.zone = b.zone;
        this.cellX = b.cellX;
        this.cellZ = b.cellZ;
        this.footprint = b.footprint;
        this.floorY = b.floorY;
        this.topY = b.topY;
        this.ops = Collections.unmodifiableList(new ArrayList<>(b.ops.list()));
        int[] starts = new int[b.ops.phaseStarts().size() + 1];
        for (int i = 0; i < b.ops.phaseStarts().size(); i++) starts[i + 1] = b.ops.phaseStarts().get(i);
        this.phaseStart = starts;
        this.centralRelic = b.centralRelic;
        this.minorRelics = List.copyOf(b.minorRelics);
        this.gates = List.copyOf(b.gates);
        this.entrances = List.copyOf(b.entrances);
        Map<String, List<Spot>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<Spot>> e : b.spawns.entrySet()) copy.put(e.getKey(), List.copyOf(e.getValue()));
        this.spawns = Collections.unmodifiableMap(copy);
    }

    // ---------------------------------------------------------------- queries
    public int centreX() { return footprint.centreX(); }

    public int centreZ() { return footprint.centreZ(); }

    /** The floor block in the middle of the Nexus (what a {@code ZoneSite} is handed as the centre). */
    public Spot centre() { return new Spot(footprint.centreX(), floorY, footprint.centreZ()); }

    public int phaseCount() { return phaseStart.length; }

    public int phaseStart(int phase) { return phaseStart[Math.max(0, Math.min(phase, phaseStart.length - 1))]; }

    /** Which phase an op index belongs to. */
    public int phaseOf(int opIndex) {
        for (int ph = phaseStart.length - 1; ph > 0; ph--) if (opIndex >= phaseStart[ph]) return ph;
        return 0;
    }

    /** Is this block inside the volume of the Nexus (its footprint grown by `margin`, from a little below the floor to a little above the roof)? */
    public boolean contains(int x, int y, int z, int margin) {
        return y >= floorY - BELOW && y <= topY + ABOVE && footprint.contains(x, z, margin);
    }

    /**
     * The chunks (x, z) the Nexus touches: all of them must be loaded before building. The footprint's own, then any further chunk an op
     * writes into (a detail that strays past the footprint), so the builder never has to load a chunk to place a block.
     */
    public List<int[]> chunks() {
        List<int[]> out = allChunks;
        if (out == null) {
            out = new ArrayList<>(footprint.chunks());
            Set<Long> seen = new HashSet<>();
            for (int[] c : out) seen.add(chunkKey(c[0], c[1]));
            for (NexusOp o : ops) {
                for (int cx = Math.floorDiv(o.x1(), 16); cx <= Math.floorDiv(o.x2(), 16); cx++) {
                    for (int cz = Math.floorDiv(o.z1(), 16); cz <= Math.floorDiv(o.z2(), 16); cz++) {
                        if (seen.add(chunkKey(cx, cz))) out.add(new int[]{cx, cz});
                    }
                }
            }
            allChunks = out = Collections.unmodifiableList(out);
        }
        return out;
    }

    private static long chunkKey(int cx, int cz) { return ((long) cx << 32) | (cz & 0xFFFFFFFFL); }

    /** Does any op write into a chunk the footprint does not touch? (A sign the footprint was drawn too small; the builder then waits for those chunks too.) */
    public boolean opsStrayFromFootprint() { return chunks().size() > footprint.chunks().size(); }

    /**
     * Does the footprint (and the two-block edge that still counts as inside) stay within the 128-block core cell of the plan? Every
     * lookup by position ({@code NexusZones.containing}, {@code lifeAllowed}) goes through the cell of the position, so a Nexus must
     * not reach into the next cell.
     */
    public boolean insideCell() {
        int x0 = cellX * NexusZone.CELL, z0 = cellZ * NexusZone.CELL;
        return footprint.minX() - 2 >= x0 && footprint.maxX() + 2 < x0 + NexusZone.CELL && footprint.minZ() - 2 >= z0 && footprint.maxZ() + 2 < z0 + NexusZone.CELL;
    }

    /** The spawn spots of a role (empty if the zone has none). */
    public List<Spot> spawns(String role) { return spawns.getOrDefault(role, List.of()); }

    public Map<String, List<Spot>> allSpawns() { return spawns; }

    /** Where to set down someone who was caught inside the new stonework: the first arrival spot, else the first entrance, else the middle of the floor. */
    public Spot rescuePoint() {
        List<Spot> arrival = spawns(ROLE_ARRIVAL);
        if (!arrival.isEmpty()) return arrival.get(0);
        if (!entrances.isEmpty()) return entrances.get(0);
        return new Spot(footprint.centreX(), floorY + 1, footprint.centreZ());
    }

    // ---------------------------------------------------------------- building a plan
    /** Starts a plan for the Nexus of this zone in the core cell (cellX, cellZ), centred on column (centreX, centreZ). */
    public static Builder builder(NexusZone zone, int cellX, int cellZ, int centreX, int centreZ) { return new Builder(zone, cellX, cellZ, centreX, centreZ); }

    public static final class Builder {
        private final NexusZone zone;
        private final int cellX, cellZ, centreX, centreZ;
        private final NexusOps ops;
        private Footprint footprint;
        private int floorY = Integer.MIN_VALUE, topY = Integer.MIN_VALUE;
        private Spot centralRelic;
        private final List<Spot> minorRelics = new ArrayList<>(), gates = new ArrayList<>(), entrances = new ArrayList<>();
        private final Map<String, List<Spot>> spawns = new LinkedHashMap<>();

        private Builder(NexusZone zone, int cellX, int cellZ, int centreX, int centreZ) {
            if (zone == null) throw new IllegalArgumentException("a plan needs a zone");
            this.zone = zone;
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.centreX = centreX;
            this.centreZ = centreZ;
            this.ops = new NexusOps(centreX, centreZ);
        }

        /** The op collector: its local x and z are offsets from the centre, y is absolute. */
        public NexusOps ops() { return ops; }

        /** The y of the topmost floor block. */
        public Builder floorY(int y) {
            this.floorY = y;
            return this;
        }

        /** The y of the highest block of the Nexus (its roof or cap). */
        public Builder topY(int y) {
            this.topY = y;
            return this;
        }

        public Builder footprint(Footprint.Shape shape, int halfX, int halfZ) {
            this.footprint = new Footprint(shape, centreX, centreZ, halfX, halfZ);
            return this;
        }

        public Builder centralRelic(int x, int y, int z) {
            this.centralRelic = new Spot(x, y, z);
            return this;
        }

        public Builder minorRelic(int x, int y, int z) {
            minorRelics.add(new Spot(x, y, z));
            return this;
        }

        public Builder gate(int x, int y, int z) {
            gates.add(new Spot(x, y, z));
            return this;
        }

        public Builder entrance(int x, int y, int z) {
            entrances.add(new Spot(x, y, z));
            return this;
        }

        public Builder spawn(String role, int x, int y, int z) {
            spawns.computeIfAbsent(role, r -> new ArrayList<>()).add(new Spot(x, y, z));
            return this;
        }

        public ZonePlan build() {
            if (footprint == null) throw new IllegalStateException("a plan needs a footprint");
            if (floorY == Integer.MIN_VALUE || topY == Integer.MIN_VALUE) throw new IllegalStateException("a plan needs a floorY and a topY");
            if (topY <= floorY) throw new IllegalStateException("the roof (" + topY + ") must be above the floor (" + floorY + ")");
            return new ZonePlan(this);
        }
    }
}
