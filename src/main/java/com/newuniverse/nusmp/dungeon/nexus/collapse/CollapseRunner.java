package com.newuniverse.nusmp.dungeon.nexus.collapse;

import java.util.BitSet;

/**
 * One collapse, one tick at a time, with no world in it: given the Dungeon's game time it decides what the collapse does this tick
 * (which stage it is in, which countdown mark and guard wave are due, which columns come down under the tick's budget, which corridor
 * block is shaken loose, when the wipe starts and ends) and tells the outside through {@link Events}. The world is a
 * {@link ColumnSweep.Sink}. All its progress lives in the {@link CollapseState} it was given, so a runner made again from a saved state
 * carries on where the first one stopped; the only things lost in a restart are the falling-block count (the ledger, a minute at most)
 * and the corridor drips that were about to land.
 */
public final class CollapseRunner {
    /** What the collapse tells the outside; every call comes from {@link #tick} on the caller's thread. */
    public interface Events {
        /** The stage changed (also called once for the stage the runner starts in). */
        default void stage(CollapsePlan.Stage to) {}

        /** Countdown mark `index` ({@link CollapsePlan#MARKS}) was reached with `ticksLeft` to go. */
        default void line(int index, long ticksLeft) {}

        /** A crack opens in the stone at this column (sound and dust only). */
        default void crack(int x, int z) {}

        /** This column is about to give (dust only): a warning, or a column the front is nearing. */
        default void dust(int x, int z) {}

        /** Guard wave `index` ({@link GuardPlan#WAVES}) is due. */
        default void wave(int index) {}

        /** The wipe is under way: whoever is inside the footprint must be thrown out. `first` is true on the one call that starts it; later calls (twice a second) catch anyone who walked back in. */
        default void expel(boolean first) {}
    }

    /** How far ahead of the front, in columns, the warning dust is shown. */
    private static final int LEAD = 120;
    private static final int PENDING = 8;
    /** Passed-over columns looked at, and taken if they have loaded since, in one tick. */
    private static final int RETRY_LOOKS = 12, RETRY_TAKES = 3;

    private final CollapseState s;
    private final CollapseSweep order;
    private final ColumnSweep sweep;
    private final FallLedger ledger = new FallLedger();
    private final ColumnSweep.Pass pass = new ColumnSweep.Pass();
    /** Columns of the order the rolling fall passed over because their chunk was not loaded; not saved (the wipe takes whatever a restart forgot). */
    private final BitSet skipped = new BitSet();
    private int retryAt;
    private final long[] dripAt = new long[PENDING];
    private final int[] dripColumn = new int[PENDING];
    private int pending;
    private CollapsePlan.Stage announced;
    private boolean ended;
    private int lastRemoved, lastEntities, lastUnits;
    private boolean lastBlocked;

    public CollapseRunner(CollapseState s, CollapseSweep order, ColumnSweep sweep) {
        this.s = s;
        this.order = order;
        this.sweep = sweep;
    }

    public CollapseState state() { return s; }

    public CollapseSweep order() { return order; }

    /** Has the wipe finished (everything that stood is gone)? */
    public boolean ended() { return ended; }

    /** How many columns the rolling fall passed over (chunk not loaded) and has not taken since. */
    public int skipped() { return skipped.cardinality(); }

    /** What the last tick removed, how many of those were started as falling blocks, and the units it spent. */
    public int lastRemoved() { return lastRemoved; }

    public int lastEntities() { return lastEntities; }

    public int lastUnits() { return lastUnits; }

    /** Did the last tick stop at a column whose chunk is not loaded? */
    public boolean blocked() { return lastBlocked; }

    /** The swept ground that is down, in percent. */
    public int percent() { return order.open() == 0 ? 100 : (int) Math.min(100L, 100L * s.cursor / order.open()); }

    /** The column the front is at (the next to fall), or the last one if it is through; null if there are none. */
    public int[] front() {
        if (order.size() == 0) return null;
        int i = Math.min(Math.max(0, s.cursor), Math.max(0, order.open() - 1));
        return new int[]{order.x(i), order.z(i)};
    }

    /** The most one column can cost the sweep: the soft overshoot of a tick's budget. */
    public int sweepColumnCost() { return sweep.maxColumnCost(); }

    /** Where the blocks fell in this collapse alive now, as the ledger counts them. */
    public int fallingAlive(long now) { return ledger.alive(now); }

    /** One tick at game time `now`. Returns the stage it was in. */
    public CollapsePlan.Stage tick(long now, ColumnSweep.Sink world, Events ev) {
        long elapsed = s.elapsed(now);
        lastRemoved = 0;
        lastEntities = 0;
        lastUnits = 0;
        lastBlocked = false;
        CollapsePlan.Stage stage = CollapsePlan.stageAt(elapsed);
        if (stage != announced) {
            announced = stage;
            ev.stage(stage);
        }
        int mark = CollapsePlan.latestDue(s.nextMsg, elapsed);
        if (mark >= 0) {
            s.nextMsg = mark + 1;
            ev.line(mark, CollapsePlan.remaining(elapsed));
        }
        if (stage == CollapsePlan.Stage.WIPE) {
            wipe(elapsed, world, ev);
            return stage;
        }
        int wave = GuardPlan.dueWave(s.nextWave, elapsed);
        if (wave >= 0) {
            s.nextWave = wave + 1;
            ev.wave(wave);
        }
        cracks(elapsed, now, world, ev);
        if (stage != CollapsePlan.Stage.CRACKS) front(elapsed, now, world, ev);
        if (stage == CollapsePlan.Stage.FINAL) corridors(elapsed, now, world, ev);
        return stage;
    }

    // ---------------------------------------------------------------- cracks
    private void cracks(long elapsed, long now, ColumnSweep.Sink world, Events ev) {
        if (order.open() == 0 || !CollapsePlan.crackDue(elapsed)) return;
        int i = CollapsePlan.pick(s.id, elapsed, order.open());
        ev.crack(order.x(i), order.z(i));
        if (CollapsePlan.crackDrips(s.id, elapsed) && ledger.room(now, CollapsePlan.MAX_ALIVE) > 0) drip(i, now, world);
    }

    private void drip(int column, long now, ColumnSweep.Sink world) {
        boolean went = sweep.dripTop(world, column, pass);
        lastBlocked |= pass.blocked;
        lastUnits += pass.units;
        if (!went) return;
        lastRemoved += pass.removed;
        lastEntities += pass.entities;
        ledger.add(now, pass.entities);
    }

    // ---------------------------------------------------------------- the rolling fall
    private void front(long elapsed, long now, ColumnSweep.Sink world, Events ev) {
        int limit = Math.min(order.open(), CollapsePlan.frontColumns(elapsed, order.open()));
        if (s.cursor < limit) {
            int entities = Math.min(CollapsePlan.entityBudget(elapsed), ledger.room(now, CollapsePlan.MAX_ALIVE));
            s.cursor = sweep.run(world, s.cursor, limit, CollapsePlan.sweepUnits(elapsed), entities, false, skipped, pass);
            lastBlocked |= pass.blocked;
            lastUnits += pass.units;
            lastRemoved += pass.removed;
            lastEntities += pass.entities;
            ledger.add(now, pass.entities);
        }
        if (!skipped.isEmpty()) retry(now, world);
        if (elapsed % 10 == 0 && s.cursor < order.open()) {
            int i = s.cursor + CollapsePlan.pick(s.id + 1, elapsed, LEAD);
            if (i < order.open()) ev.dust(order.x(i), order.z(i));
        }
    }

    /** A few of the columns that were passed over, if their chunk has loaded since (the others wait; the wipe takes what is left). */
    private void retry(long now, ColumnSweep.Sink world) {
        int takes = 0, at = retryAt;
        for (int looks = 0; looks < RETRY_LOOKS && takes < RETRY_TAKES; looks++) {
            int i = skipped.nextSetBit(at);
            if (i < 0) i = skipped.nextSetBit(0);
            if (i < 0) break;
            at = i + 1;
            if (!world.loaded(order.x(i), order.z(i))) continue;
            int entities = Math.min(2, ledger.room(now, CollapsePlan.MAX_ALIVE));
            if (sweep.one(world, i, entities, pass)) {
                skipped.clear(i);
                takes++;
                lastUnits += pass.units;
                lastRemoved += pass.removed;
                lastEntities += pass.entities;
                ledger.add(now, pass.entities);
            }
        }
        retryAt = at;
    }

    // ---------------------------------------------------------------- the corridors, in the last half minute
    private void corridors(long elapsed, long now, ColumnSweep.Sink world, Events ev) {
        int spared = order.size() - order.open();
        if (spared <= 0) return;
        for (int k = 0; k < pending; ) {                                                 // the drips whose second of warning is up
            if (dripAt[k] > now) {
                k++;
                continue;
            }
            if (ledger.room(now, CollapsePlan.MAX_ALIVE) > 0) drip(dripColumn[k], now, world);
            pending--;
            dripAt[k] = dripAt[pending];
            dripColumn[k] = dripColumn[pending];
        }
        if (CollapsePlan.dripDue(elapsed) && pending < PENDING) {
            int i = order.open() + CollapsePlan.pick(s.id + 2, elapsed, spared);
            dripAt[pending] = now + CollapsePlan.DRIP_LEAD;
            dripColumn[pending] = i;
            pending++;
            ev.dust(order.x(i), order.z(i));
        }
    }

    // ---------------------------------------------------------------- the final wipe
    private void wipe(long elapsed, ColumnSweep.Sink world, Events ev) {
        pending = 0;
        if (!s.expelled) {
            s.expelled = true;
            ev.expel(true);
        } else if (elapsed % 10 == 0) {
            ev.expel(false);
        }
        if (s.wipeCursor < order.size()) {
            s.wipeCursor = sweep.run(world, s.wipeCursor, order.size(), CollapsePlan.WIPE_UNITS, 0, true, null, pass);
            lastBlocked |= pass.blocked;
            lastUnits += pass.units;
            lastRemoved += pass.removed;
        }
        if (s.wipeCursor >= order.size()) ended = true;
    }
}
