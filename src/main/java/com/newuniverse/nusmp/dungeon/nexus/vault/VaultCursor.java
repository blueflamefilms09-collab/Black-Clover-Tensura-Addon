package com.newuniverse.nusmp.dungeon.nexus.vault;

import java.util.List;

/**
 * Walks a vault's ops a few hundred blocks at a time, so a room can be built over a few ticks and picked up again after a restart. The same
 * walk as the Golden City's cursor, over {@link VaultOp}s. Pure: the world is reached through a {@link Sink}. The position (op index and
 * cell within the op) is all that has to be saved. Every op is idempotent, so a resume may start a few ops early.
 */
public final class VaultCursor {
    /** Where the blocks go. Returns true if the world was changed (a write), false if the block was already as wanted (a read). */
    public interface Sink { boolean place(VaultOp op, int x, int y, int z); }

    /** What a write and a read cost against a tick's budget. */
    public static final int WRITE = 4, READ = 1;
    /** How many ops are done again when building resumes, in case the world saved less than the progress did. */
    public static final int REWIND = 3;

    private final List<VaultOp> ops;
    private int op;
    private long cell;

    public VaultCursor(VaultLayout layout, int op, long cell) {
        this.ops = layout.ops;
        this.op = Math.max(0, Math.min(op, ops.size()));
        this.cell = this.op >= ops.size() ? 0 : Math.max(0, cell);
    }

    /** The cursor to continue from saved progress: a few ops earlier, from the start of the op. */
    public static VaultCursor resume(VaultLayout layout, int savedOp) {
        if (savedOp >= layout.ops.size()) return new VaultCursor(layout, layout.ops.size(), 0);
        return new VaultCursor(layout, Math.max(0, savedOp - REWIND), 0);
    }

    public int op() { return op; }

    public long cell() { return cell; }

    public boolean done() { return op >= ops.size(); }

    /** Works until about `budget` units are spent. Returns the units spent. */
    public long run(Sink sink, long budget) {
        long spent = 0;
        while (op < ops.size() && spent < budget) {
            VaultOp o = ops.get(op);
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
