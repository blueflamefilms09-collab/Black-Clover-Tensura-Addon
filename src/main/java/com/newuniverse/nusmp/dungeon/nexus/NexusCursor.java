package com.newuniverse.nusmp.dungeon.nexus;

import java.util.List;

/**
 * Walks a plan's ops a few thousand blocks at a time, so a whole Nexus can be built over several ticks and picked up again after a
 * restart. The same walk as the Golden City's (dungeon/city/CityCursor), over {@link NexusOp}s. Pure: the world is reached through a
 * {@link Sink}. The position (op index and cell within the op) is all that has to be saved.
 */
public final class NexusCursor {
    /** Where the blocks go. Returns true if the world was changed (a write), false if the block was already as wanted (a read). */
    public interface Sink { boolean place(NexusOp op, int x, int y, int z); }

    /** What a write and a read cost against a tick's budget, so a tick does at most budget/WRITE writes. */
    public static final int WRITE = 4, READ = 1;
    /** How many ops of the current phase are done again when building resumes, in case the world saved less than the progress did. */
    public static final int REWIND = 8;

    private final ZonePlan plan;
    private final List<NexusOp> ops;
    private int op;
    private long cell;

    public NexusCursor(ZonePlan plan, int op, long cell) {
        this.plan = plan;
        this.ops = plan.ops;
        this.op = Math.max(0, Math.min(op, ops.size()));
        this.cell = this.op >= ops.size() ? 0 : Math.max(0, cell);
    }

    /** The cursor to continue from saved progress: a little earlier, inside the same phase, because replaying ops in order changes nothing. */
    public static NexusCursor resume(ZonePlan plan, int savedOp) {
        if (savedOp >= plan.ops.size()) return new NexusCursor(plan, plan.ops.size(), 0);
        int from = Math.max(plan.phaseStart(plan.phaseOf(Math.max(0, savedOp))), savedOp - REWIND);
        return new NexusCursor(plan, Math.max(0, from), 0);
    }

    public ZonePlan plan() { return plan; }

    public int op() { return op; }

    public long cell() { return cell; }

    public boolean done() { return op >= ops.size(); }

    /** Works until about `budget` units are spent. Returns the units spent. */
    public long run(Sink sink, long budget) {
        long spent = 0;
        while (op < ops.size() && spent < budget) {
            NexusOp o = ops.get(op);
            long volume = o.volume();
            int sx = o.sx(), sz = o.sz();
            while (cell < volume && spent < budget) {
                int dx = (int) (cell % sx);
                long rest = cell / sx;
                int dz = (int) (rest % sz);
                int dy = (int) (rest / sz);
                spent += sink.place(o, o.x1() + dx, o.y1() + dy, o.z1() + dz) ? WRITE : READ;
                cell++;
            }
            if (cell >= volume) {
                op++;
                cell = 0;
            }
        }
        return spent;
    }
}
