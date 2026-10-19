package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.newuniverse.nusmp.labyrinth.mega.mechanics.Seeds;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Why: every puzzle needs the same plumbing, and getting it wrong in one of 29 copies would be a soft lock: the seeded board (the same seed always builds the same board, a reset puts the
 * board back exactly), the effects list, the lock-out after too many mistakes (a short cool-down, then the puzzle starts over: the party is never stuck behind a puzzle that refuses input
 * forever), the move budget of free-form puzzles (a puzzle with no wrong move still costs something if it is mashed: it jams, charges a small penalty and re-deals), and the guarantee that
 * {@link #suggest} never returns an action earlier than the rule's own clock. A subclass fills in {@link #makeLayout} (what to build), {@link #init} (the board, from {@link #rng}),
 * {@link #handle} (what an action does), {@link #face} and {@link #next} (the next correct action). Everything is deterministic from {@code Params}. Pure.
 */
public abstract class PuzzleBase implements PuzzleRule {
    /** How long the puzzle ignores input after it started over because of too many mistakes. */
    public static final int COOL_TICKS = 40;

    protected final String id;
    protected final Params p;
    protected SplittableRandom rng;
    /** Bumped by a reshuffling rule each time its board is re-dealt, so the new deal differs; 0 for a rule that deals one board for ever. */
    protected int attempt;
    protected boolean solved;
    protected int fails;
    protected long now;
    protected long lockedUntil;
    protected long lastActAt;
    /** Free-form clicks since the last deal and the budget after which the mechanism jams (0 = no budget). */
    protected int moves;
    protected int jamBudget;
    /** A rule that wants a different board after a restart sets this. */
    protected boolean reshuffleOnRestart;
    private Layout layout;
    private final List<Fx> out = new ArrayList<>();

    protected PuzzleBase(String id, Params p) {
        this.id = id;
        this.p = p;
    }

    /** Builds the board. The catalogue calls this once, right after the constructor. */
    public final PuzzleBase build() {
        layout = makeLayout();
        deal();
        return this;
    }

    private void deal() {
        rng = new SplittableRandom(Seeds.mix(Seeds.mix(p.seed(), id.hashCode()), attempt * 7919L + 13));
        moves = 0;
        jamBudget = 0;
        init();
    }

    // ---------------------------------------------------------------- what a subclass provides
    protected abstract Layout makeLayout();

    /** Deals the board from {@link #rng} and sets {@link #jamBudget}. Called at build, at every restart and at every reset. */
    protected abstract void init();

    /** What the action does; add effects with {@link #say}, {@link #tone}, {@link #fail} and call {@link #win} when beaten. Called only when the puzzle is open for input. */
    protected abstract void handle(Act a);

    protected abstract int face(int cell);

    /** The next correct action from the current state, or null when the rule is waiting for time to pass (or is solved). */
    protected abstract Act next(long tick);

    /** Does this action use up the move budget? By default every click does; a rule whose clicks include free "look" gestures overrides it. */
    protected boolean countsAsMove(Act a) { return a.kind() == Act.Kind.CLICK; }

    /** The first effects of the puzzle (a pattern is shown, a melody plays). */
    protected void onBegin(long tick) { }

    /** Time passed. */
    protected void onTick(long tick) { }

    /** How many mistakes make the puzzle start over. */
    protected int failLimit() { return 4; }

    // ---------------------------------------------------------------- the interface
    @Override public final String id() { return id; }

    @Override public final Layout layout() { return layout; }

    @Override public final Params params() { return p; }

    @Override public final boolean solved() { return solved; }

    @Override public final int fails() { return fails; }

    @Override public int height(int cell) {
        Layout.Group g = layout.groupOf(cell);
        return g == null ? 1 : g.height();
    }

    @Override public int face(int cell, int layer) { return face(cell); }

    @Override
    public final List<Fx> begin(long tick) {
        now = Math.max(now, tick);
        out.clear();
        onBegin(tick);
        return take();
    }

    @Override
    public final List<Fx> act(Act a) {
        now = Math.max(now, a.tick());
        out.clear();
        if (solved) return List.of();
        if (a.kind() != Act.Kind.CHOOSE && (a.cell() < 0 || a.cell() >= layout.total())) return List.of();
        if (now < lockedUntil) {
            out.add(Fx.say(a.actor(), "cooling", 0));
            return take();
        }
        lastActAt = now;
        handle(a);
        if (!solved && jamBudget > 0 && countsAsMove(a) && ++moves > jamBudget) {
            out.add(Fx.say(-1, "jam", 0));
            fail(a.actor(), 1, "slow", null);
            if (!out.isEmpty() && !hasReset()) restart("restarted");
        }
        return take();
    }

    @Override
    public final List<Fx> tick(long tick) {
        if (tick < now) tick = now;
        now = tick;
        out.clear();
        if (!solved) onTick(tick);
        return take();
    }

    @Override
    public final Act suggest(long tick) {
        if (solved) return null;
        long t = Math.max(Math.max(tick, now), lockedUntil);
        Act a = next(t);
        if (a == null) return null;
        return a.tick() < t ? a.at(t) : a;
    }

    @Override
    public void reset(long tick) {
        now = Math.max(now, tick);
        solved = false;
        lockedUntil = 0;
        if (reshuffleOnRestart) attempt++;
        deal();
        out.clear();
        onBegin(tick);
        out.clear();
    }

    // ---------------------------------------------------------------- helpers for the subclasses
    private List<Fx> take() { return out.isEmpty() ? List.of() : new ArrayList<>(out); }

    private boolean hasReset() {
        for (Fx f : out) if (f.kind() == Fx.Kind.RESET) return true;
        return false;
    }

    protected final void say(int actor, String key, int n, String... more) { out.add(Fx.say(actor, key, n, more)); }

    protected final void tone(int cell, int note, String instrument) { out.add(Fx.tone(cell, note, instrument)); }

    protected final void spark(int cell, boolean good) { out.add(Fx.spark(cell, good)); }

    protected final void emit(Fx f) { out.add(f); }

    protected final void win() {
        if (solved) return;
        solved = true;
        out.add(Fx.solved());
    }

    /**
     * A mistake: a penalty of the given severity (kind: hurt, dark, drain, slow, hunger, ambush) and a line of text, and after {@link #failLimit} mistakes the puzzle starts over behind a short
     * cool-down. The world scales the penalty and never lets it kill; nothing here closes a door.
     */
    protected final void fail(int actor, int severity, String kind, String sayKey, String... more) {
        fails++;
        out.add(Fx.penalty(actor, severity, kind));
        if (sayKey != null) out.add(Fx.say(actor, sayKey, 0, more));
        int lim = failLimit();
        if (lim > 0 && fails % lim == 0) {
            lockedUntil = now + COOL_TICKS;
            restart("tooMany");
        }
    }

    /** Deals the board again (a reshuffling rule gets a new deal) and tells the world to redraw. */
    protected final void restart(String sayKey) {
        if (reshuffleOnRestart) attempt++;
        deal();
        if (sayKey != null) out.add(Fx.say(-1, sayKey, 0));
        out.add(Fx.reset());
    }

    /** The next tick at which {@code (t / step + off) mod m} equals {@code target}, never before {@code from}. */
    protected static long nextTickAt(long from, long step, long off, long m, long target) {
        long phase = Math.floorDiv(from, step);
        for (long k = 0; k <= m + 1; k++) {
            long idx = phase + k;
            if (Math.floorMod(idx + off, m) == target) {
                long t = idx * step;
                return Math.max(t, from);
            }
        }
        return from;
    }

    protected static void shuffle(int[] a, SplittableRandom r) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = r.nextInt(i + 1);
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }

    protected static int[] iota(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i;
        return a;
    }
}
