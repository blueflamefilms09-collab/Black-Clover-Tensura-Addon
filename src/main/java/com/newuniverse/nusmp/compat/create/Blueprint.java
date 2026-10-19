package com.newuniverse.nusmp.compat.create;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the drawing of one trap, in the frame of its lane and with logical parts only (no world, no registry), so the same drawing can be proved safe by a plain test, placed
 * by the Minecraft layer and re-derived after a restart from nothing but (family, length, headroom, trigger, seed). The frame: x across the lane (-1 .. 1 is the lane, the
 * walls start at +-2), y up (0 is the floor row, 1 is the first cell a player stands in), z along the lane (0 .. length-1; the two end cells are the margins that carry
 * the levers, the plates and tripwires stand just inside them). A trap has parts (blocks placed once), power cells (a redstone block the runtime swaps for an inert block
 * to start and stop the machinery or to light a nixie tube), rams (cells the runtime fills for the moment a press closes), bands (the slabs of the lane that hurt, with
 * their timing) and optionally a conveyor.
 */
public record Blueprint(TrapFamily family, TriggerKind trigger, int length, int headroom, List<Placed> parts, List<Band> bands, List<PowerCell> power,
                        List<int[]> rams, Conveyor conveyor, long seed) {

    /** One block: its cell, the part, a facing and a rotation axis (both optional, as {@link Dir6}) and a variant word for the few parts that have one (belt: start / middle / end). */
    public record Placed(int dx, int dy, int dz, Part part, Dir6 facing, Dir6 axis, String variant) {
        public String key() { return dx + "," + dy + "," + dz; }
    }

    /** What a {@link PowerCell} is for. */
    public enum Role {
        /** Next to a clutch: powered = the machinery is cut (idle, disarmed); the runtime unpowers it for the length of a run. */
        CLUTCH,
        /** Next to a nixie tube: powered = lit; the runtime powers it while the band of the same index is winding up or striking. */
        LIGHT
    }

    public record PowerCell(int dx, int dy, int dz, Role role, int index) {
        public String key() { return dx + "," + dy + "," + dz; }
    }

    /**
     * A slab of the lane that hurts: the box x {@code xMin..xMax}, y {@code 1..yMax}, z {@code zMin..zMax} (lane frame, inclusive), cycling every {@code period} ticks after
     * {@code offset} ticks from the start of a run: {@code telegraph} ticks of warning, then {@code active} ticks that hurt, then quiet. {@code damageFactor} scales the
     * damage; {@code lightIndex} is the {@link PowerCell} group lit while this band winds up (or -1).
     */
    public record Band(int xMin, int xMax, int yMax, int zMin, int zMax, int offset, int period, int telegraph, int active, HazardKind kind, double damageFactor, int lightIndex) {
        public boolean contains(int x, int y, int z) { return x >= xMin && x <= xMax && y >= 1 && y <= yMax && z >= zMin && z <= zMax; }
    }

    /** A belt in the centre column of the lane (x = 0), floor row, from {@code zMin} to {@code zMax}, carrying whoever stands on it forward at {@code strength} blocks per tick. */
    public record Conveyor(int zMin, int zMax, double strength) {
        public boolean contains(int x, int z) { return x == 0 && z >= zMin && z <= zMax; }
    }

    public Blueprint {
        parts = List.copyOf(parts);
        bands = List.copyOf(bands);
        power = List.copyOf(power);
        rams = List.copyOf(rams);
    }

    public List<Placed> of(Part part) {
        List<Placed> out = new ArrayList<>();
        for (Placed p : parts) if (p.part() == part) out.add(p);
        return out;
    }

    /** The cells of the levers that disarm the trap. */
    public List<Placed> levers() { return of(Part.LEVER); }

    /** The cells whose state starts the trap: pressure plates, or the tripwire strings. Empty for the proximity trigger. */
    public List<Placed> triggerCells() {
        List<Placed> out = new ArrayList<>();
        for (Placed p : parts) if (p.part() == Part.PLATE || p.part() == Part.STRING) out.add(p);
        return out;
    }

    /** Is this cell inside the lane volume (the cells a player walks through)? */
    public boolean inLane(int x, int y, int z) { return x >= -1 && x <= 1 && y >= 1 && y <= headroom && z >= 0 && z <= length - 1; }

    /** The ticks one run lasts (the machinery runs, the bands cycle). */
    public int runTicks() { return TrapRules.RUN_TICKS; }

    /** The ticks after a run before the trap can be triggered again. */
    public int cooldownTicks() { return TrapRules.COOLDOWN_TICKS; }
}
