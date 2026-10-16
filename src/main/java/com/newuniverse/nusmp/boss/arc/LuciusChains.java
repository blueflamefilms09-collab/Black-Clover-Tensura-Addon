package com.newuniverse.nusmp.boss.arc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Why: Lucius's old move chooser picked one spell at a time, so his combos were an accident of cooldowns. The owner wants the fight to
 * be lethal spell combinations that read like the manga: Checkmate (gravity pins you, space opens behind you, time closes round you),
 * the Soul Touch that follows it, and different chains for each act. A chain is therefore <i>data</i>: an ordered list of steps, each
 * with the ticks of telegraph the players get before it happens and the pause before the next one. This pure class holds the
 * chains, the rule that keeps them fair (a step that can hurt is announced at least {@value #MIN_HURT_TELEGRAPH} ticks ahead), the
 * weighted pick per act, and {@link Runner}, the state machine that walks a chain tick by tick and can be saved and resumed; the
 * entity glue only has to say what each step kind does in the world.
 */
public final class LuciusChains {
    private LuciusChains() {}

    /** A step that can hurt is shown at least this many ticks (0.6 s) before it lands. */
    public static final int MIN_HURT_TELEGRAPH = 12;

    /** What a step does. Entries are only appended (the ordinal is not saved, the name is). */
    public enum Kind {
        /** Heavy downward velocity, Slowness and a crush at a marked spot under the target. */
        GRAVITY_PIN,
        /** A spatial blink to the target's blind spot, 2.5 blocks behind its look vector. */
        SPATIAL_BLINK,
        /** A bounded stasis bubble round the target (Time Magic). */
        CHRONO_STASIS,
        /** The melee Soul Touch: damage, Soul Corruption, and the target's allies turned. */
        SOUL_TOUCH,
        /** A slow projectile that holds whatever it hits for a moment. */
        CHRONO_BOLT,
        /** A volley of Grimoire Tower spells. */
        TOWER_VOLLEY,
        /** A gravity zone that crushes everything inside a ring. */
        CRUSH_ZONE,
        /** Phase 3: the clones (re)appear. */
        CLONE_WAVE,
        /** A teleport away from whoever came too close. */
        RETREAT,
        // ---- 1.45.0, the god tier (appended; the name is what is saved)
        /** Stage 1: the Soul Touch from range, a soul lance at the target (Soul Corruption level one, a share of the touch's damage). */
        SOUL_RAY,
        /** A spatial pulse that de-attunes every relic of whoever stands in it for a few seconds. */
        RELIC_EMP,
        /** A binding on one player that severs their Djinn and spirits for a few seconds. */
        SEVERANCE,
        /** A pulse from the starvation field that drains a share of the magicule of everyone in range. */
        STARVATION_PULSE,
        /** Stage 2: Vanica's blood tide, as a chain step. */
        BLOOD_TIDE,
        /** A step beside the target and an unblockable physical strike (the Checkmate counter and the answer to a clamped hit). */
        COUNTER_STRIKE
    }

    /**
     * One step. 'telegraph' is the ticks between the warning and the effect, 'gap' the ticks after the effect before the next
     * telegraph begins, 'hurts' says the effect can damage a player (then the telegraph must be {@link #MIN_HURT_TELEGRAPH} or more).
     */
    public record Step(Kind kind, int telegraph, int gap, boolean hurts) {}

    /** A chain: usable in the acts of 'phaseMask' (bit n = act n), on its own cooldown, drawn with 'weight'. */
    public record Chain(String id, int phaseMask, int cooldown, int weight, List<Step> steps) {
        public boolean usable(int phase) { return phase >= 1 && phase <= 3 && (phaseMask & (1 << phase)) != 0; }

        /** Ticks from the first warning to the end of the last gap. */
        public int length() {
            int n = 0;
            for (Step s : steps) n += s.telegraph() + s.gap();
            return n;
        }
    }

    private static int phases(int... ps) {
        int m = 0;
        for (int p : ps) m |= 1 << p;
        return m;
    }

    private static Step s(Kind k, int telegraph, int gap, boolean hurts) { return new Step(k, telegraph, gap, hurts); }

    public static final String CHECKMATE = "checkmate", CHECKMATE_SOUL = "checkmate_soul", SOUL_TOUCH_FOLLOWUP = "soul_touch_followup",
            STASIS_VOLLEY = "stasis_volley", TOWER_SPAM = "tower_spam", RETREAT = "retreat", CRUSH_PRESSURE = "crush_pressure",
            CRUSH_BLINK_TOUCH = "crush_blink_touch", CLONE_WAVE = "clone_wave";
    /** 1.45.0, the god tier's chains. */
    public static final String SOUL_RAY_CHAIN = "soul_ray", STASIS_SNARE = "stasis_snare", BLOOD_TIDE_CHAIN = "blood_tide_pressure",
            STARVATION_PULSE_CHAIN = "starvation_pulse", RELIC_EMP_CHAIN = "relic_emp", SEVERANCE_CHAIN = "severance_bind",
            CHECKMATE_COUNTER = "checkmate_counter", HIT_COUNTER = "hit_counter";

    private static final List<Chain> ALL = List.of(
            // Checkmate: gravity pins you, space opens behind you, time closes round you
            new Chain(CHECKMATE, phases(2, 3), 360, 5, List.of(
                    s(Kind.GRAVITY_PIN, 20, 6, true), s(Kind.SPATIAL_BLINK, 12, 4, false), s(Kind.CHRONO_STASIS, 16, 8, false))),
            // the same, then the Soul Touch once the stasis has run out: the lethal one of act 2
            new Chain(CHECKMATE_SOUL, phases(2), 760, 3, List.of(
                    s(Kind.GRAVITY_PIN, 20, 6, true), s(Kind.SPATIAL_BLINK, 12, 4, false), s(Kind.CHRONO_STASIS, 16, 64, false),
                    s(Kind.SOUL_TOUCH, 14, 10, true))),
            // somebody is caught in close quarters: step to their back and touch their soul
            new Chain(SOUL_TOUCH_FOLLOWUP, phases(2, 3), 240, 4, List.of(s(Kind.SPATIAL_BLINK, 12, 4, false), s(Kind.SOUL_TOUCH, 14, 10, true))),
            // act 1: the slow time bolts that hold what they touch, and the tower spam
            new Chain(STASIS_VOLLEY, phases(1, 3), 300, 5, List.of(
                    s(Kind.CHRONO_BOLT, 18, 14, true), s(Kind.CHRONO_BOLT, 18, 14, true), s(Kind.CHRONO_BOLT, 18, 10, true))),
            new Chain(TOWER_SPAM, phases(1, 3), 240, 6, List.of(
                    s(Kind.TOWER_VOLLEY, 20, 10, true), s(Kind.TOWER_VOLLEY, 20, 10, true), s(Kind.TOWER_VOLLEY, 20, 10, true))),
            // act 1: when somebody comes within six blocks he is simply elsewhere (the glue starts this one by itself)
            new Chain(RETREAT, phases(1), 120, 0, List.of(s(Kind.RETREAT, 8, 0, false))),
            // act 2: gravity zones, and the blink that follows them
            new Chain(CRUSH_PRESSURE, phases(2, 3), 320, 4, List.of(s(Kind.CRUSH_ZONE, 24, 10, true), s(Kind.CRUSH_ZONE, 24, 10, true))),
            new Chain(CRUSH_BLINK_TOUCH, phases(2), 420, 5, List.of(
                    s(Kind.CRUSH_ZONE, 24, 6, true), s(Kind.SPATIAL_BLINK, 12, 4, false), s(Kind.SOUL_TOUCH, 14, 10, true))),
            // act 3: the clones appear (the clones cast on their own after that)
            new Chain(CLONE_WAVE, phases(3), 400, 3, List.of(s(Kind.CLONE_WAVE, 30, 20, false))),
            // ---- 1.45.0, the god tier (never drawn when it is off). Stage 1, Time and Soul: a stasis snare and a soul lance from range.
            new Chain(STASIS_SNARE, phases(1), 360, 3, List.of(s(Kind.CHRONO_STASIS, 16, 24, false))),
            new Chain(SOUL_RAY_CHAIN, phases(1), 420, 3, List.of(s(Kind.SOUL_RAY, 18, 20, true))),
            // stage 2, Gravity and Blood: the blood tide, the starvation pulse, the severance binding
            new Chain(BLOOD_TIDE_CHAIN, phases(2, 3), 380, 4, List.of(s(Kind.BLOOD_TIDE, 22, 16, true))),
            new Chain(STARVATION_PULSE_CHAIN, phases(2, 3), 700, 2, List.of(s(Kind.STARVATION_PULSE, 20, 30, false))),
            new Chain(SEVERANCE_CHAIN, phases(2, 3), 800, 2, List.of(s(Kind.SEVERANCE, 28, 20, false))),
            // stage 3, Space: the relic pulse
            new Chain(RELIC_EMP_CHAIN, phases(3), 900, 3, List.of(s(Kind.RELIC_EMP, 24, 20, false))),
            // answers (weight 0: started by the glue, never drawn): the Checkmate counter and the reply to a clamped hit
            new Chain(CHECKMATE_COUNTER, phases(1, 2, 3), 160, 0, List.of(s(Kind.SPATIAL_BLINK, 12, 2, false), s(Kind.COUNTER_STRIKE, 14, 10, true))),
            new Chain(HIT_COUNTER, phases(1, 2, 3), 0, 0, List.of(s(Kind.COUNTER_STRIKE, 14, 10, true))));

    /** The chains that exist only in the god tier: {@link #pick} leaves them out unless it is told the god tier is on. */
    private static final java.util.Set<String> GOD_ONLY = java.util.Set.of(STASIS_SNARE, SOUL_RAY_CHAIN, BLOOD_TIDE_CHAIN, STARVATION_PULSE_CHAIN,
            SEVERANCE_CHAIN, RELIC_EMP_CHAIN, CHECKMATE_COUNTER, HIT_COUNTER);

    public static boolean godOnly(String id) { return GOD_ONLY.contains(id); }

    public static List<Chain> all() { return ALL; }

    public static Chain byId(String id) {
        for (Chain c : ALL) if (c.id().equals(id)) return c;
        return null;
    }

    /** Is the chain fair: not empty, every step has a positive telegraph, and every step that hurts gives at least the minimum. */
    public static boolean valid(Chain c) {
        if (c == null || c.steps().isEmpty()) return false;
        for (Step st : c.steps()) {
            if (st.telegraph() < 1 || st.gap() < 0) return false;
            if (st.hurts() && st.telegraph() < MIN_HURT_TELEGRAPH) return false;
        }
        return true;
    }

    /**
     * The next chain for this act: the heaviest-weighted draw among the chains that are usable in the act and off cooldown. 'readyAt'
     * maps a chain id to the tick it may start again. A target within 8 blocks makes the Soul Touch chains three times as likely
     * (the "caught in close quarters" rule). The retreat (weight 0) is never drawn here.
     */
    public static Optional<Chain> pick(int phase, long now, Map<String, Long> readyAt, Random rng, double targetDistance) {
        return pick(phase, now, readyAt, rng, targetDistance, false);
    }

    /** Like {@link #pick(int, long, Map, Random, double)}; with 'god' the god-tier chains (1.45.0) are drawn too, without it they never are. */
    public static Optional<Chain> pick(int phase, long now, Map<String, Long> readyAt, Random rng, double targetDistance, boolean god) {
        List<Chain> pool = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int total = 0;
        for (Chain c : ALL) {
            if (!c.usable(phase) || c.weight() <= 0 || readyAt.getOrDefault(c.id(), 0L) > now || (!god && GOD_ONLY.contains(c.id()))) continue;
            int w = c.weight();
            boolean touch = c.steps().stream().anyMatch(st -> st.kind() == Kind.SOUL_TOUCH);
            if (touch && targetDistance <= 8.0) w *= 3;
            pool.add(c);
            weights.add(w);
            total += w;
        }
        if (pool.isEmpty()) return Optional.empty();
        int roll = rng.nextInt(total);
        for (int i = 0; i < pool.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) return Optional.of(pool.get(i));
        }
        return Optional.of(pool.get(pool.size() - 1));
    }

    // ================================================================ the runner
    /** What {@link Runner#tick} reports. */
    public enum EventType { NONE, BEGIN, FIRE, DONE }

    public record Event(EventType type, int step, Kind kind) {
        static final Event NONE = new Event(EventType.NONE, -1, null);
    }

    private enum Stage { IDLE, TELEGRAPH, GAP }

    /** What a save writes for a running chain (an idle runner writes nothing). */
    public record Saved(String chainId, int step, int left) {}

    /**
     * Walks one chain at a time, one {@link #tick} per game tick. The order of events for a chain of n steps is BEGIN 0, FIRE 0,
     * BEGIN 1, FIRE 1 ... FIRE n-1, DONE; FIRE comes exactly 'telegraph' ticks after its BEGIN and the next BEGIN 'gap' ticks after the
     * FIRE. It also keeps the cooldowns of the chains it has started.
     */
    public static final class Runner {
        private Chain chain;
        private int step, left;
        private Stage stage = Stage.IDLE;
        private boolean pendingBegin;
        private final Map<String, Long> readyAt = new HashMap<>();

        public boolean active() { return stage != Stage.IDLE; }

        public String chainId() { return chain == null ? null : chain.id(); }

        public int stepIndex() { return step; }

        public Step currentStep() { return chain == null || step >= chain.steps().size() ? null : chain.steps().get(step); }

        /** Cooldowns by chain id (the live map: the picker reads it, the glue saves it). */
        public Map<String, Long> readyAt() { return readyAt; }

        /** Starts a chain now. False if one is already running or the chain is not valid. Sets the chain's cooldown. */
        public boolean start(Chain c, long now) {
            if (active() || !valid(c)) return false;
            chain = c;
            step = 0;
            readyAt.put(c.id(), now + c.cooldown());
            enterTelegraph();
            return true;
        }

        private void enterTelegraph() {
            stage = Stage.TELEGRAPH;
            left = chain.steps().get(step).telegraph();
            pendingBegin = true;
        }

        /** Stops the running chain (its target died, the act changed, Julius surfaced). The cooldown stays. */
        public void abort() {
            stage = Stage.IDLE;
            chain = null;
            pendingBegin = false;
            step = 0;
            left = 0;
        }

        public Event tick() {
            if (stage == Stage.IDLE) return Event.NONE;
            if (pendingBegin) {
                pendingBegin = false;
                return new Event(EventType.BEGIN, step, chain.steps().get(step).kind());
            }
            if (--left > 0) return Event.NONE;
            Step st = chain.steps().get(step);
            if (stage == Stage.TELEGRAPH) {
                stage = Stage.GAP;
                left = Math.max(1, st.gap());
                return new Event(EventType.FIRE, step, st.kind());
            }
            // the gap is over
            if (step + 1 >= chain.steps().size()) {
                abort();
                return new Event(EventType.DONE, -1, null);
            }
            step++;
            enterTelegraph();
            pendingBegin = false;
            return new Event(EventType.BEGIN, step, chain.steps().get(step).kind());
        }

        /** The running chain's position, or null when idle. */
        public Saved save() { return stage == Stage.IDLE ? null : new Saved(chain.id(), step, left); }

        /**
         * Resumes a saved chain at its step. The warning of that step starts again from the top (a reload never shortens what the
         * players get to see), so a chain that was in a gap simply begins its next step's telegraph afresh.
         */
        public boolean restore(Saved s, long now) {
            abort();
            if (s == null) return false;
            Chain c = byId(s.chainId());
            if (c == null || s.step() < 0 || s.step() >= c.steps().size()) return false;
            chain = c;
            step = s.step();
            enterTelegraph();
            return true;
        }
    }
}
