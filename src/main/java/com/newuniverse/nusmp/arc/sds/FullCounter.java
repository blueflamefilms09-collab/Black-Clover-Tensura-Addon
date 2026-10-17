package com.newuniverse.nusmp.arc.sds;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: Full Counter is the one rule of Meliodas's fight that answers what strong players do most, which is throw magic at a boss. In the series it
 * throws magic back and does nothing against steel; a boss that simply reflected every spell would be unbeatable for a caster and a one-shot for the
 * caster whose own spell comes back, so the rule has to be exact and fair. This pure state machine holds the rule so a test can prove it without a
 * game: the stance has a duration and a cooldown; only magic is answered (never a blade, an arrow or an anti-magic blow); it only answers what he
 * faces unless his Lostvayne images stand guard (then every side); one answer takes at most {@link #VICTIM_CAP} of the caster's maximum health, one
 * stance at most {@link #VICTIM_BUDGET} of it in all, and a spell that hits many times is answered once per {@link #REPEAT_GAP} ticks, so a reflection
 * is never a kill; and the stance has a limit: once it has swallowed more than its break threshold it shatters (the counter to a pile of magic is
 * more magic than it can hold, or none at all).
 *
 * <p>The class also keeps the tally of recent spell hits ({@link #notePressure}) that makes him raise the stance when the party starts to cast.
 * Nothing here knows what an entity or a damage source is: the boss decides "magic" and "frontal" and passes the numbers in.
 */
public final class FullCounter {
    /** PASS: the hit lands as usual. REFLECT: the hit is absorbed (the boss takes nothing) and {@code reflected} goes back. SHATTER: the stance broke on this hit and the hit lands. */
    public enum Verdict { PASS, REFLECT, SHATTER }

    /** What happened to one hit. {@code reflected} is the damage to send back at the caster (health points, already capped); 0 unless REFLECT. */
    public record Result(Verdict verdict, float reflected) {
        static final Result PASS = new Result(Verdict.PASS, 0f);
    }

    /** The caster's own damage comes back as it was (the stance does not double it: the cap, not a multiplier, keeps it fair). */
    public static final double REFLECT_RATIO = 1.0;
    /** One answer takes at most this share of the caster's maximum health. */
    public static final double VICTIM_CAP = 0.30;
    /** One stance takes at most this share of one caster's maximum health in all. */
    public static final double VICTIM_BUDGET = 0.55;
    /** A feeble spell still stings: an answer is never less than this share of the caster's maximum health. */
    public static final double FLOOR_SHARE = 0.03;
    /** Ticks between two answers to the same caster (a spell that hits every tick is answered once per gap). */
    public static final int REPEAT_GAP = 6;
    /** The spell hits within this many ticks count as pressure, and this many of them make him raise the stance. */
    public static final int PRESSURE_WINDOW = 80, PRESSURE_HITS = 3;

    private long upUntil = Long.MIN_VALUE, readyAt;
    private int cooldown;
    private boolean omni;
    private float absorbed, breakAt = 1f;
    private final Map<Integer, Long> lastAnswer = new HashMap<>();
    private final Map<Integer, Float> answered = new HashMap<>();
    private final long[] recent = new long[8];
    private int recentCount;

    /** Is the stance up at this tick? */
    public boolean up(long now) { return now < upUntil; }

    /** May a stance be raised now (not up, and the cooldown of the last one is over)? */
    public boolean ready(long now) { return !up(now) && now >= readyAt; }

    /** Is the stance answering every side (his images guard him)? */
    public boolean omni(long now) { return omni && up(now); }

    /** The magic it has swallowed in this stance, in health points. */
    public float absorbed() { return absorbed; }

    /** How full the stance is, 0 (empty) to 1 (it breaks on the next hit). */
    public float load() { return Math.max(0f, Math.min(1f, absorbed / breakAt)); }

    /**
     * Raises the stance for {@code duration} ticks. {@code breakAt} is the magic (health points) it can swallow before it shatters; {@code omni}
     * answers every side; {@code cooldown} runs after the stance ends. {@code force} ignores the cooldown (the images and the Demon King's Wrath
     * raise it by themselves). False (nothing changes) when it is already up, the cooldown is not over and {@code force} is off, or the duration is not positive.
     */
    public boolean raise(long now, int duration, int cooldown, float breakAt, boolean omni, boolean force) {
        if (duration <= 0 || up(now) || (!force && now < readyAt)) return false;
        this.upUntil = now + duration;
        this.cooldown = Math.max(0, cooldown);
        this.readyAt = this.upUntil + this.cooldown;
        this.omni = omni;
        this.absorbed = 0f;
        this.breakAt = Math.max(1f, breakAt);
        lastAnswer.clear();
        answered.clear();
        return true;
    }

    /** Ends the stance now (it shattered, the fight ended, a stagger cut it): the cooldown starts from here. */
    public void drop(long now) {
        if (up(now)) readyAt = now + cooldown;
        upUntil = Long.MIN_VALUE;
        omni = false;
    }

    /** Forgets everything (a new fight). */
    public void reset() {
        upUntil = Long.MIN_VALUE;
        readyAt = 0;
        omni = false;
        absorbed = 0f;
        lastAnswer.clear();
        answered.clear();
        recentCount = 0;
    }

    /**
     * Judges one hit.
     *
     * @param magic         is it a spell (the boss decides: vanilla magic or a Tensura non-physical type, and not an anti-magic blow)
     * @param frontal       does the boss face the caster (within the cone the stance covers)
     * @param amount        the damage the hit would do, in health points
     * @param attackerId    the caster's entity id, or -1 when there is none (the hit is absorbed and nothing is sent back)
     * @param attackerMax   the caster's maximum health
     */
    public Result onHit(long now, boolean magic, boolean frontal, float amount, int attackerId, float attackerMax) {
        if (!up(now) || !magic || amount <= 0f) return Result.PASS;
        if (!frontal && !omni) return Result.PASS;
        absorbed += amount;
        if (absorbed > breakAt) {
            drop(now);
            return new Result(Verdict.SHATTER, 0f);
        }
        if (attackerId < 0 || !(attackerMax > 0f)) return new Result(Verdict.REFLECT, 0f);
        Long last = lastAnswer.get(attackerId);
        if (last != null && now - last < REPEAT_GAP) return new Result(Verdict.REFLECT, 0f);
        float budgetLeft = (float) (attackerMax * VICTIM_BUDGET) - answered.getOrDefault(attackerId, 0f);
        float wanted = Math.max((float) (amount * REFLECT_RATIO), (float) (attackerMax * FLOOR_SHARE));
        float back = Math.max(0f, Math.min(Math.min(wanted, (float) (attackerMax * VICTIM_CAP)), budgetLeft));
        lastAnswer.put(attackerId, now);
        answered.merge(attackerId, back, Float::sum);
        return new Result(Verdict.REFLECT, back);
    }

    // ---------------------------------------------------------------- spell pressure
    /** Notes a spell hit at this tick (kept: the last eight). */
    public void notePressure(long now) {
        if (recentCount < recent.length) recent[recentCount++] = now;
        else {
            System.arraycopy(recent, 1, recent, 0, recent.length - 1);
            recent[recent.length - 1] = now;
        }
    }

    /** How many spell hits landed within {@link #PRESSURE_WINDOW} ticks of now. */
    public int pressure(long now) {
        int n = 0;
        for (int i = 0; i < recentCount; i++) if (now - recent[i] <= PRESSURE_WINDOW && recent[i] <= now) n++;
        return n;
    }

    /** True when the party is casting at him and the stance is ready: time to raise it. */
    public boolean pressed(long now) { return ready(now) && pressure(now) >= PRESSURE_HITS; }
}
