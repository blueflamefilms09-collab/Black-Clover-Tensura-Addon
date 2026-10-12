package com.newuniverse.nusmp.dungeon.nexus;

/**
 * Runs the stages of a Nexus build one after the other under one budget: stage 0 is the plan's own ops, stages 1 and up are the extra
 * work a zone asks for (the Golden City hands its whole structure to the old city builder this way). The position to save is
 * (stage, op, cell); on resume only the stage that was running is opened again, at its saved op and cell. Pure: stages are
 * {@link Stage}s, the world is their business.
 */
public final class StageSequence {
    /** One stage of work. {@link #run} spends about `budget` units and returns what it spent; a stage that is done spends nothing more. */
    public interface Stage {
        long run(long budget);

        boolean done();

        /** The position inside this stage (what a restart is given back); a stage with no sense of position may return 0. */
        int op();

        long cell();
    }

    /** Opens stage `index` at a saved position ((0, 0) when it starts fresh). */
    public interface Opener { Stage open(int index, int savedOp, long savedCell); }

    private final int count;
    private final Opener opener;
    private int stage;
    private Stage current;

    /** count: how many stages in all; (stage, op, cell): where to start (stage >= count means everything is done). */
    public StageSequence(int count, Opener opener, int stage, int op, long cell) {
        this.count = Math.max(0, count);
        this.opener = opener;
        this.stage = Math.max(0, stage);
        if (this.stage < this.count) current = opener.open(this.stage, Math.max(0, op), Math.max(0, cell));
        else this.stage = this.count;
    }

    public boolean done() { return stage >= count; }

    /** The stage that is running (the count once everything is done). */
    public int stage() { return stage; }

    public int op() { return done() ? 0 : current.op(); }

    public long cell() { return done() ? 0 : current.cell(); }

    /** Works until about `budget` units are spent or everything is done; returns the units spent. A stage that is waiting (spends nothing, not done) ends the slice. */
    public long run(long budget) {
        long spent = 0;
        while (!done() && spent < budget) {
            if (!current.done()) {
                long used = current.run(budget - spent);
                spent += used;
                if (!current.done() && used <= 0) break;
            }
            if (current.done()) {
                stage++;
                current = stage < count ? opener.open(stage, 0, 0L) : null;
            }
        }
        return spent;
    }
}
