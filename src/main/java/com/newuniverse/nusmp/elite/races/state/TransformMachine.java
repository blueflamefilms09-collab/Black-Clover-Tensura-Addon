package com.newuniverse.nusmp.elite.races.state;

import com.newuniverse.nusmp.elite.races.state.EntryCondition.Reason;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the heart of the race transformations, as a pure state machine so every rule (who may enter, from where, how long, how long to rest, what ends it) is testable without
 * a server. One machine per player holds the current state, when it ends and the rest time of each state. It never touches attributes or particles: the runtime asks it
 * ({@link #check}, {@link #enter}, {@link #tick}, {@link #exit}) and applies the answer. The clock is the game clock passed in through the {@link TransformContext}, so a save
 * made at tick N and loaded after a restart continues exactly (the state is {@link #encode}d to one short string).
 *
 * <p>Rules: BASE is entered by leaving a state. A state is entered from BASE or from a state its spec lists in {@code from}; entering from another state ends that one with its
 * own rest. A state is refused during its rest. The entry is any one of the spec's entries (all conditions inside it must hold). A state ends when its duration is over, at
 * dawn when it is a night form, when its aura upkeep cannot be paid, or when the race no longer has it ({@code REMOVED}, with no rest).
 */
public final class TransformMachine {
    /** What a tick decided. */
    public enum Event { NONE, EXPIRED, DAWN, DRAINED, REMOVED }

    /** The answer of {@link #check}: {@code ok}, why not, and which entry let it through (so the caller can take its item). */
    public record Result(boolean ok, Reason reason, EntryCondition entry) {
        static Result yes(EntryCondition e) { return new Result(true, null, e); }

        static Result no(Reason r) { return new Result(false, r, null); }
    }

    private TransformState state = TransformState.BASE;
    private long endsAt;
    private long enteredAt;
    private final EnumMap<TransformState, Long> restUntil = new EnumMap<>(TransformState.class);

    public TransformState state() { return state; }

    public long enteredAt() { return enteredAt; }

    public long endsAt() { return endsAt; }

    public boolean active() { return state != TransformState.BASE; }

    /** Ticks of rest left for a state (0 when it may be entered). */
    public long restLeft(TransformState s, long now) {
        Long until = restUntil.get(s);
        return until == null ? 0L : Math.max(0L, until - now);
    }

    /** Ticks left of the current state (0 for BASE). */
    public long remaining(long now) { return active() ? Math.max(0L, endsAt - now) : 0L; }

    // ---------------------------------------------------------------- entry
    /** Can this state be entered now? Does not change anything. */
    public Result check(TransformSpec spec, TransformContext ctx) { return check(spec, ctx, false); }

    /** As {@link #check}; with {@code autoOnly} only the entries marked {@code auto} count (the rage awakening, the moon), never the plain "at will" ones. */
    public Result check(TransformSpec spec, TransformContext ctx, boolean autoOnly) {
        if (spec == null) return Result.no(Reason.NOT_AVAILABLE);
        if (ctx.tier() < spec.minTier()) return Result.no(Reason.TIER);
        if (state == spec.state()) return Result.no(Reason.ALREADY);
        if (state != TransformState.BASE && !spec.from().contains(state)) return Result.no(Reason.FROM);
        if (state == TransformState.BASE && !spec.from().contains(TransformState.BASE)) return Result.no(Reason.FROM);
        if (restLeft(spec.state(), ctx.now()) > 0) return Result.no(Reason.COOLDOWN);
        if (spec.auraDrainPerSecond() > 0 && ctx.auraFraction() <= 0.0) return Result.no(Reason.AURA);
        if (spec.entries().isEmpty()) return autoOnly ? Result.no(Reason.NOT_AVAILABLE) : Result.yes(EntryCondition.ALWAYS);
        Reason first = null;
        for (EntryCondition e : spec.entries()) {
            if (autoOnly && !e.auto()) continue;
            Reason r = e.check(ctx);
            if (r == null) return Result.yes(e);
            if (first == null) first = r;
        }
        return Result.no(first == null ? Reason.NOT_AVAILABLE : first);
    }

    /** Checks and enters. {@code leaving} is the spec of the state being left (for its rest), or null. */
    public Result enter(TransformSpec spec, TransformSpec leaving, TransformContext ctx) {
        Result r = check(spec, ctx);
        if (r.ok()) forceEnter(spec, leaving, ctx.now());
        return r;
    }

    /** Enters without any check (administrators, self-tests). */
    public void forceEnter(TransformSpec spec, TransformSpec leaving, long now) {
        if (active() && leaving != null) rest(leaving, now);
        state = spec.state();
        enteredAt = now;
        endsAt = spec.durationTicks() <= 0 ? Long.MAX_VALUE : now + spec.durationTicks();
    }

    // ---------------------------------------------------------------- tick and exit
    /** Advances the machine one step and reports what ended the state, if anything. */
    public Event tick(Map<TransformState, TransformSpec> specs, TransformContext ctx) {
        if (!active()) return Event.NONE;
        TransformSpec spec = specs.get(state);
        if (spec == null || ctx.tier() < spec.minTier()) {
            exit(null, ctx.now());
            return Event.REMOVED;
        }
        if (ctx.now() >= endsAt) {
            exit(spec, ctx.now());
            return Event.EXPIRED;
        }
        if (spec.endsAtDawn() && !ctx.night()) {
            exit(spec, ctx.now());
            return Event.DAWN;
        }
        if (spec.auraDrainPerSecond() > 0 && ctx.auraFraction() <= 0.0) {
            exit(spec, ctx.now());
            return Event.DRAINED;
        }
        return Event.NONE;
    }

    /** Leaves the current state for BASE. With a spec the state's rest starts; with null (the race lost the state) no rest does. */
    public void exit(TransformSpec spec, long now) {
        if (spec != null && active()) rest(spec, now);
        state = TransformState.BASE;
        enteredAt = 0L;
        endsAt = 0L;
    }

    private void rest(TransformSpec spec, long now) {
        if (spec.cooldownTicks() > 0) restUntil.put(spec.state(), now + spec.cooldownTicks());
    }

    /** Back to the start: BASE with no rest (a new race, a death). */
    public void reset() {
        state = TransformState.BASE;
        enteredAt = 0L;
        endsAt = 0L;
        restUntil.clear();
    }

    // ---------------------------------------------------------------- choosing
    /** The order the hotkey cycles through: the quiet boost, the awakening, the divine form, and last the Great Ape (which cannot be steered once raging). */
    public static final List<TransformState> CYCLE = List.of(TransformState.BOOSTED, TransformState.ASCENDED, TransformState.DIVINE, TransformState.GREAT_APE);

    /** The strongest state the player can enter now (by the cycle order, the Great Ape never picked automatically), or null. */
    public TransformState best(Map<TransformState, TransformSpec> specs, TransformContext ctx) {
        TransformState best = null;
        for (TransformState s : CYCLE) {
            if (s == TransformState.GREAT_APE) continue;
            if (check(specs.get(s), ctx).ok()) best = s;
        }
        return best;
    }

    /** The state that follows the current one in the cycle among those enterable now; null means "go back to BASE" (the cycle wrapped). */
    public TransformState cycleTarget(Map<TransformState, TransformSpec> specs, TransformContext ctx) {
        int from = active() ? CYCLE.indexOf(state) : -1;
        for (int i = from + 1; i < CYCLE.size(); i++) {
            TransformState s = CYCLE.get(i);
            if (check(specs.get(s), ctx).ok()) return s;
        }
        return null;
    }

    /** The first state that one of its {@code auto} entries lets in now, with the Great Ape first (the moon does not wait). */
    public TransformState autoTrigger(Map<TransformState, TransformSpec> specs, TransformContext ctx) {
        if (active()) return null;
        TransformState[] order = {TransformState.GREAT_APE, TransformState.DIVINE, TransformState.ASCENDED, TransformState.BOOSTED};
        for (TransformState s : order) {
            TransformSpec spec = specs.get(s);
            if (spec != null && check(spec, ctx, true).ok()) return s;
        }
        return null;
    }

    // ---------------------------------------------------------------- save
    /** One short string: {@code state;enteredAt;endsAt;state=restUntil,state=restUntil}. */
    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append(state.id).append(';').append(enteredAt).append(';').append(endsAt == Long.MAX_VALUE ? -1L : endsAt).append(';');
        boolean first = true;
        for (Map.Entry<TransformState, Long> e : restUntil.entrySet()) {
            if (!first) sb.append(',');
            sb.append(e.getKey().id).append('=').append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    /** Reads {@link #encode}; anything malformed gives a fresh machine (a corrupt save never throws). */
    public static TransformMachine decode(String s) {
        TransformMachine m = new TransformMachine();
        if (s == null || s.isEmpty()) return m;
        try {
            String[] parts = s.split(";", -1);
            if (parts.length < 3) return new TransformMachine();
            TransformState st = TransformState.byId(parts[0]);
            if (st == null) return new TransformMachine();
            m.state = st;
            m.enteredAt = Long.parseLong(parts[1]);
            long end = Long.parseLong(parts[2]);
            m.endsAt = end < 0 ? Long.MAX_VALUE : end;
            if (parts.length > 3 && !parts[3].isEmpty()) {
                for (String kv : parts[3].split(",")) {
                    int eq = kv.indexOf('=');
                    if (eq <= 0) continue;
                    TransformState k = TransformState.byId(kv.substring(0, eq));
                    if (k != null && k != TransformState.BASE) m.restUntil.put(k, Long.parseLong(kv.substring(eq + 1)));
                }
            }
            if (m.state == TransformState.BASE) { m.enteredAt = 0L; m.endsAt = 0L; }
            return m;
        } catch (NumberFormatException e) {
            return new TransformMachine();
        }
    }
}
