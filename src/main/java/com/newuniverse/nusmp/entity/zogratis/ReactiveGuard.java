package com.newuniverse.nusmp.entity.zogratis;

import java.util.Set;

/**
 * Why: Leon Cromwell (docs/boss_reference_leon.md, 1.2) guards while a foe is close: 70% of the hits are blocked in a 12-tick block action, 30% are dodged with 8 invulnerable ticks, and a
 * blocked hit is answered with a counter swing. Our sword bosses only ever stood and took it. This is that behaviour as one pure state machine (no game classes, unit tested, one instance per
 * boss), opted into by id ({@link #OPT_IN}) and tuned by two SERVER keys.
 *
 * <p><b>It only ever cuts a hit, never adds one, and it runs ahead of the framework's lines.</b> {@code ZogratisBoss.hurt} asks {@link #judge} after the boss's own filters and BEFORE
 * {@code PowerCurve.clampDamage} and the per-second budget, so the power-step clamp, the DPS budget and the no-regeneration rules all still see (and cap) whatever gets through; the guard
 * heals nothing and raises nothing.
 *
 * <p><b>It cannot make a boss unkillable.</b> A guard action is only started when its cooldown is over, and a cooldown is never shorter than twice its action window
 * ({@link Params} clamps that), so at most {@code blockWindow/blockCooldown} (default 12/40 = 30%) of the time is a block, which still lets {@code blockKeep} (25%) through, and at most
 * {@code dodgeFrames/dodgeCooldown} (8/60 = 13.3%) is a dodge window. A foe's sustained damage therefore keeps at least 1 - 0.30 x 0.75 - 0.133 = 64% of its value, always above zero,
 * before the budget (which only limits from above, so the budget's floor on the kill time is unchanged). Dodges are also capped per second ({@link Params#maxDodgesPerSecond}, enforced
 * as a minimum spacing of 20/cap ticks). A boss that is exposed (after an ultimate), winding up a move, transforming or refilling does not guard at all.
 */
public final class ReactiveGuard {
    /** The characters that guard (Escanor has his own counter and is left out). */
    public static final Set<String> OPT_IN = Set.of("meliodas", "ban", "heath_grice", "vetto", "mars", "udaeus");

    /** Leon's numbers. */
    public static final double BLOCK_CHANCE = 0.70, DODGE_CHANCE = 0.30, MAX_CHANCE = 0.90;
    public static final int DODGE_FRAMES = 8, BLOCK_WINDOW = 12, BLOCK_COOLDOWN = 40, DODGE_COOLDOWN = 60, MAX_DODGES_PER_SECOND = 1;
    public static final double STANCE_ENTER = 5.0, STANCE_EXIT = 6.0;
    /** A blocked hit lets this share of its damage through (a block is not a wall). */
    public static final float BLOCK_KEEP = 0.25f;

    /** What happened to one hit. */
    public enum Verdict { NONE, BLOCK, DODGE }

    /** The tuning; every field is clamped into a safe range at construction. */
    public record Params(double blockChance, double dodgeChance, int dodgeFrames, int blockWindow, int blockCooldown, int dodgeCooldown, int maxDodgesPerSecond, float blockKeep) {
        public Params {
            blockChance = clamp(blockChance, 0, MAX_CHANCE);
            dodgeChance = clamp(dodgeChance, 0, MAX_CHANCE);
            dodgeFrames = Math.max(1, Math.min(dodgeFrames, 20));
            blockWindow = Math.max(1, Math.min(blockWindow, 30));
            maxDodgesPerSecond = Math.max(1, Math.min(maxDodgesPerSecond, 4));
            blockCooldown = Math.max(blockCooldown, blockWindow * 2);                         // an action is never more than half of its own cycle
            dodgeCooldown = Math.max(dodgeCooldown, Math.max(dodgeFrames * 2, (20 + maxDodgesPerSecond - 1) / maxDodgesPerSecond));
            blockKeep = (float) clamp(blockKeep, 0.05, 1.0);                                // a block always lets something through
        }

        /** Leon's numbers with the two chances of the server keys. */
        public static Params of(double blockChance, double dodgeChance) {
            return new Params(blockChance, dodgeChance, DODGE_FRAMES, BLOCK_WINDOW, BLOCK_COOLDOWN, DODGE_COOLDOWN, MAX_DODGES_PER_SECOND, BLOCK_KEEP);
        }

        /** The most of the time a guard action can cover, as a share (the proof of the class comment). */
        public double blockShare() { return blockWindow / (double) blockCooldown; }
        public double dodgeShare() { return dodgeFrames / (double) dodgeCooldown; }

        /** The least of a foe's sustained damage that gets through a guard that always rolls its best (always above 0). */
        public double leastThrough() { return 1.0 - blockShare() * (1.0 - blockKeep) - dodgeShare(); }
    }

    private static double clamp(double v, double lo, double hi) { return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v)); }

    private final Params p;
    private boolean stance;
    private long blockReady, dodgeReady, blockUntil = -1, dodgeUntil = -1, counterAt = -1;
    private int counterTarget = -1;

    public ReactiveGuard(Params p) { this.p = p; }

    public Params params() { return p; }

    /** Guard stance with hysteresis: enters inside {@link #STANCE_ENTER}, leaves beyond {@link #STANCE_EXIT}. */
    public boolean stance(double distance) {
        if (stance ? distance > STANCE_EXIT : distance <= STANCE_ENTER) stance = !stance;
        return stance;
    }

    /**
     * The fate of one hit. 'canGuard' is false while the boss is exposed, winding up, transforming or refilling (and for hits that are not a foe's blow); 'dodgeRoll' and 'blockRoll' are
     * uniform in [0,1) (two independent rolls, as Leon rolls each). An action already running (a dodge's invulnerable ticks, a block's window) takes the hit without a roll; a new
     * action needs its cooldown over, and both need the foe inside the guard stance.
     */
    public Verdict judge(long t, double distance, boolean canGuard, double dodgeRoll, double blockRoll, int attackerId) {
        boolean near = stance(distance);
        if (!canGuard || !near) return Verdict.NONE;
        if (t < dodgeUntil) return Verdict.DODGE;
        if (t < blockUntil) return Verdict.BLOCK;
        if (t >= dodgeReady && dodgeRoll < p.dodgeChance()) {
            dodgeUntil = t + p.dodgeFrames();
            dodgeReady = t + p.dodgeCooldown();
            return Verdict.DODGE;
        }
        if (t >= blockReady && blockRoll < p.blockChance()) {
            blockUntil = t + p.blockWindow();
            blockReady = t + p.blockCooldown();
            counterAt = blockUntil;
            counterTarget = attackerId;
            return Verdict.BLOCK;
        }
        return Verdict.NONE;
    }

    /** The damage left after a verdict: nothing for a dodge, {@link Params#blockKeep} of it for a block. */
    public float apply(Verdict v, float amount) {
        return v == Verdict.DODGE ? 0f : v == Verdict.BLOCK ? amount * p.blockKeep() : amount;
    }

    /** The entity id to answer, once, when the block action of the last block is over; -1 when none is due. Clears the answer. */
    public int takeCounter(long t) {
        if (counterAt < 0 || t < counterAt) return -1;
        int id = counterTarget;
        counterAt = -1;
        counterTarget = -1;
        return id;
    }

    /** Forgets a pending counter and the running actions (the boss was interrupted or the fight reset). Cooldowns stay. */
    public void reset() {
        counterAt = -1;
        counterTarget = -1;
        blockUntil = dodgeUntil = -1;
        stance = false;
    }
}
