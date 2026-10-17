package com.newuniverse.nusmp.labyrinth.mega;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Why: a {@code StructureProcessor} sees one block at a time and has no idea which Region it is building (its arguments are the block, its
 * position and the placement settings). The sector generator places pieces synchronously on the server thread, so it publishes "I am
 * building THIS sector, and these are its markers" in a ThreadLocal for the duration of each piece, and the theme processor and the
 * mod-compat processor read it. Outside the generator (a datapack Jigsaw, {@code /place}, an old Spire) the context is null and both
 * processors do nothing, which is exactly what keeps them harmless everywhere else. A ThreadLocal rather than a static field because world
 * generation of the Overworld runs on other threads at the same time and must never see the sector's theme.
 */
public final class RegionContext {
    private RegionContext() {}

    /** The sector being built and its markers by CELL position (the marker cell, not the block it describes). */
    public record Active(Sector sector, Map<BlockPos, Marker> markers) {}

    private static final ThreadLocal<Active> CURRENT = new ThreadLocal<>();

    /** The context of the piece being placed on this thread, or null. */
    public static Active current() { return CURRENT.get(); }

    /** Runs {@code work} with the context set, restoring the previous one afterwards (nests). */
    public static void run(Sector sector, Map<BlockPos, Marker> markers, Runnable work) {
        Active before = CURRENT.get();
        CURRENT.set(new Active(sector, markers));
        try { work.run(); } finally { if (before == null) CURRENT.remove(); else CURRENT.set(before); }
    }

    public static <T> T call(Sector sector, Map<BlockPos, Marker> markers, Supplier<T> work) {
        Active before = CURRENT.get();
        CURRENT.set(new Active(sector, markers));
        try { return work.get(); } finally { if (before == null) CURRENT.remove(); else CURRENT.set(before); }
    }
}
