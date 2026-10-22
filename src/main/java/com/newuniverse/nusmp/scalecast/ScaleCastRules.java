package com.newuniverse.nusmp.scalecast;

import java.util.Locale;

/**
 * Why: Scale Magic (Damnatio Kira) is now carried out through ONE floating balance scale beside the caster that weighs the foe, and a passive suppression aura that presses a foe's blows and
 * spells down by what the Scale measured (docs/magic/scale_casting.md). Every decision that does not need the game lives here so it can be tested without it: how a creature's weight is built
 * from its existence points (EP) and its magicule load, the log scale the weights travel in, how far the beam tilts, how much the aura takes off, the hard caps, the floors, the throttles, the
 * prop word the server and the painter share and the spring of the beam. Pure: no Minecraft class, no clock, no static state.
 * <p>
 * Prop word of the Scale (the int {@code param} of a {@code MagicPropEntity}, kind SCALE_BALANCE):
 * <pre>
 *   bits  0-7   caster weight q   (log scale: weight = 10^(q / 20), so 0..255 covers 1 .. 10^12.75)
 *   bits  8-15  foe weight q      (same scale; meaningless without bit 27)
 *   bits 16-21  suppression percent of the weighed foe (0..63, the cap keeps it at 35 or less)
 *   bits 22-25  aura radius - 1   (blocks, 1..16)
 *   bit  26     the suppression aura is on
 *   bit  27     a foe is being weighed
 *   bits 28-30  cast pulse        (counts up by one every Scale page cast, wraps at 8: the weighing animation starts when it changes)
 *   bit  31     fading            (the Scale is about to go: the client runs its own fade)
 * </pre>
 */
public final class ScaleCastRules {
    private ScaleCastRules() {}

    // ================================================================ caps and throttles (rule 11)
    /** Most foes one caster suppresses at once. */
    public static final int MAX_FOES = 8;
    /** Ticks between two scans of the aura (and two choices of the weighed foe). */
    public static final int SCAN_EVERY = 10;
    /** The most Scales on the whole server (a thirty-third caster has no Scale and no aura). */
    public static final int GLOBAL_SCALES = 32;
    /** The most aura scans the server runs in one tick, whatever the number of casters. */
    public static final int SCANS_PER_TICK = 6;
    /** Ticks the Scale stays after the last cast, hold or summon (12 s), and the ticks of its fade (the client fades by its own clock). */
    public static final int LINGER = 240, FADE = 14;
    /** A suppression entry that has not been renewed by a scan lapses after this many ticks (a scan every 10 renews it). */
    public static final int ENTRY_LIFE = 24;
    /** How long a weighed foe is kept while it stays in reach, so the beam does not flicker between two near foes (ticks). */
    public static final int FOE_STICKY = 40;
    /** Aura radius in blocks: default, and the hard range a config may choose from. */
    public static final double RADIUS = 12.0, RADIUS_MIN = 4.0, RADIUS_MAX = 16.0;

    // ================================================================ suppression numbers
    /**
     * The most of a foe's outgoing damage the aura takes (35 percent): a config may lower it, never raise it above this. {@link #MAGIC_CAP} is the same for a casting foe's magic output
     * (the damage multiplier of its spells); it has its own, lower cap.
     */
    public static final double DAMAGE_CAP = 0.35, MAGIC_CAP = 0.30;
    /** A boss feels half (bosses are the one thing no aura should be able to trivialise). */
    public static final double BOSS_FACTOR = 0.5;
    /** The magic reduction is this share of the damage reduction (then capped by {@link #MAGIC_CAP}). */
    public static final double MAGIC_SHARE = 0.85;
    /** Reductions under two percent are not applied (a dead zone, so a foe at the edge of the curve does not flicker). */
    public static final double DEAD_ZONE = 0.02;
    /** A blow of this many hit points or fewer is never reduced, and a bigger one is never taken below it: the aura cannot erase a hit. */
    public static final float DAMAGE_FLOOR = 1.0f;
    /**
     * The curve: the aura is at full strength when the foe is {@link #FULL_AT} decades (powers of ten of weight) lighter than the caster, and gone when it is {@link #NONE_AT} decades heavier;
     * between them it follows a smooth step. At equal weight it is already about 16 percent of the cap.
     */
    public static final double FULL_AT = -1.5, NONE_AT = 0.5;

    // ================================================================ weight
    /**
     * The weight of a creature: its existence points are the body of it, the share of its magicule pool that is loaded is how much of that body it brings to bear. A creature that has spent
     * its pool weighs half of what it does at full. {@code ep} is Tensura's max EP; when it reads 0 (a creature without existence data) ten times the max health stands in for it.
     * {@code magicule} and {@code maxMagicule} are the current and the maximum pool (a creature without a pool counts as full).
     */
    public static double weight(double ep, double maxHealth, double magicule, double maxMagicule) {
        double body = ep > 0 && Double.isFinite(ep) ? ep : Math.max(1.0, finite(maxHealth) * 10.0);
        double fill = maxMagicule > 0 && Double.isFinite(maxMagicule) ? clamp01(finite(magicule) / maxMagicule) : 1.0;
        return Math.max(1.0, body * (0.5 + 0.5 * fill));
    }

    /** The log-scale step of the prop word: 20 steps per power of ten. */
    public static final int Q_PER_DECADE = 20;

    /** Weight to its 0..255 log code. */
    public static int quantize(double weight) {
        double w = Math.max(1.0, finite(weight));
        return (int) Math.max(0, Math.min(255, Math.round(Math.log10(w) * Q_PER_DECADE)));
    }

    /** The weight a log code stands for. */
    public static double dequantize(int q) { return Math.pow(10.0, Math.max(0, Math.min(255, q)) / (double) Q_PER_DECADE); }

    /** Difference of two log codes in decades (foe minus caster): positive when the foe is heavier. */
    public static double decades(int casterQ, int foeQ) { return (foeQ - casterQ) / (double) Q_PER_DECADE; }

    // ================================================================ the beam
    /** The beam never tilts more than this many degrees. */
    public static final float MAX_TILT = 24f;

    /**
     * How far the beam tilts toward the heavier side, degrees: positive = the foe's pan is down. A smooth S curve of the difference in decades (about 12 degrees at one decade, 22 at three),
     * 0 at equal weight.
     */
    public static float tilt(double decadesFoeMinusCaster) {
        return (float) (MAX_TILT * Math.tanh(finite(decadesFoeMinusCaster) / 1.4));
    }

    /** The tilt for two log codes; a Scale with no foe weighed rests level. */
    public static float tilt(int casterQ, int foeQ, boolean hasFoe) { return hasFoe ? tilt(decades(casterQ, foeQ)) : 0f; }

    /** The spring of the beam: stiffness and damping per tick (settles in about a second with one small overshoot). */
    public static final float SPRING_K = 0.30f, SPRING_D = 0.24f;

    /** One spring step in place: {@code st[0]} the angle, {@code st[1]} the angular speed (degrees, degrees per tick), toward {@code goal} over {@code dt} ticks (0..2.5). No allocation. */
    public static void spring(float[] st, float goal, float dt) {
        float d = Math.max(0f, Math.min(2.5f, dt));
        st[1] += (SPRING_K * (goal - st[0]) - SPRING_D * st[1]) * d;
        st[0] += st[1] * d;
    }

    /** The weighing swing after a cast, degrees, {@code age} ticks since the pulse: a damped swing that rings down in about 30 ticks; {@code sign} alternates it between casts. */
    public static float weighSwing(float age, float sign) {
        if (age < 0f || age > 36f) return 0f;
        return (float) (sign * 17.0 * Math.exp(-age * 0.14) * Math.sin(age * 0.52));
    }

    /** How much the pans bob during the weighing (blocks of model space), 0 when it is over. */
    public static float panBob(float age) {
        if (age < 0f || age > 36f) return 0f;
        return (float) (0.06 * Math.exp(-age * 0.16) * Math.sin(age * 0.9));
    }

    /** The glow of a pan's load, 0.15..1: the heavier side shines more. */
    public static float load(int q, int otherQ) {
        int hi = Math.max(1, Math.max(q, otherQ));
        return Math.max(0.15f, Math.min(1f, q / (float) hi));
    }

    // ================================================================ the aura
    /** The smooth step 0..1 of the curve described at {@link #FULL_AT}. */
    public static double strength(double decadesFoeMinusCaster) {
        double d = finite(decadesFoeMinusCaster);
        double t = clamp01((NONE_AT - d) / (NONE_AT - FULL_AT));
        return t * t * (3.0 - 2.0 * t);
    }

    /** The cap a config value is held to: never above {@link #DAMAGE_CAP}, never below 0. */
    public static double clampCap(double configured) { return Math.max(0.0, Math.min(DAMAGE_CAP, finite(configured))); }

    /** The aura radius a config value is held to. */
    public static double clampRadius(double configured) { return Math.max(RADIUS_MIN, Math.min(RADIUS_MAX, finite(configured))); }

    /**
     * The share of a foe's outgoing damage the aura takes (0..cap): {@code cap * strength(difference)}, halved for a boss, 0 inside the dead zone. {@code cap} is the damage cap (already
     * clamped by {@link #clampCap}).
     */
    public static double reduction(double decadesFoeMinusCaster, boolean boss, double cap) {
        double r = clampCap(cap) * strength(decadesFoeMinusCaster) * (boss ? BOSS_FACTOR : 1.0);
        return r < DEAD_ZONE ? 0.0 : Math.min(r, clampCap(cap));
    }

    /** The share of a casting foe's spell power the aura takes: {@link #MAGIC_SHARE} of the damage reduction, never above {@link #MAGIC_CAP}. */
    public static double magicReduction(double damageReduction) { return magicReduction(damageReduction, MAGIC_CAP); }

    /** The same with a lower cap chosen by the server owner ({@code cap} is held to 0..{@link #MAGIC_CAP}). */
    public static double magicReduction(double damageReduction, double cap) {
        double r = Math.max(0.0, Math.min(DAMAGE_CAP, finite(damageReduction))) * MAGIC_SHARE;
        return r < DEAD_ZONE ? 0.0 : Math.min(r, Math.max(0.0, Math.min(MAGIC_CAP, finite(cap))));
    }

    /** The multiplier a magic reduction makes (1 = nothing taken). */
    public static double magicMultiplier(double magicReduction) { return 1.0 - Math.max(0.0, Math.min(MAGIC_CAP, finite(magicReduction))); }

    /**
     * The damage after the aura: never more than the blow was (the aura only takes), never below {@link #DAMAGE_FLOOR} for a blow that was bigger than that, and a blow of the floor or less
     * is left alone. A reduction outside 0..{@link #DAMAGE_CAP} is clamped, so no call can take more than the cap. Not a percent of anybody's health: a share of the blow.
     */
    public static float apply(float amount, double reduction) {
        if (!(amount > DAMAGE_FLOOR) || !Float.isFinite(amount)) return amount;
        double r = Math.max(0.0, Math.min(DAMAGE_CAP, finite(reduction)));
        return (float) Math.max(DAMAGE_FLOOR, amount * (1.0 - r));
    }

    /** Two reductions from different casters on the same foe: the stronger one holds (they never add up). */
    public static double stronger(double a, double b) { return Math.max(a, b); }

    // ================================================================ the prop word
    public static int pack(int casterQ, int foeQ, boolean hasFoe, int percent, double radius, boolean aura, int pulse, boolean fading) {
        int rad = (int) Math.max(0, Math.min(15, Math.round(clampRadius(radius)) - 1));
        int pct = Math.max(0, Math.min(63, percent));
        return (clampQ(casterQ)) | (clampQ(foeQ) << 8) | (pct << 16) | (rad << 22) | (aura ? 1 << 26 : 0) | (hasFoe ? 1 << 27 : 0) | ((pulse & 7) << 28) | (fading ? 1 << 31 : 0);
    }

    private static int clampQ(int q) { return Math.max(0, Math.min(255, q)); }

    public static int casterQ(int word) { return word & 0xFF; }
    public static int foeQ(int word) { return (word >>> 8) & 0xFF; }
    public static int percent(int word) { return (word >>> 16) & 63; }
    public static int radius(int word) { return ((word >>> 22) & 15) + 1; }
    public static boolean aura(int word) { return (word & (1 << 26)) != 0; }
    public static boolean hasFoe(int word) { return (word & (1 << 27)) != 0; }
    public static int pulse(int word) { return (word >>> 28) & 7; }
    public static boolean fading(int word) { return (word & (1 << 31)) != 0; }

    /** The bits that change only when the weights are measured again (the weights and the percent): the server sends them at most {@link #WEIGH_RESEND} ticks apart. */
    public static final int WEIGHT_MASK = 0x3FFFFF;
    public static final int WEIGH_RESEND = 10;

    /**
     * Whether the server should write the new word now: never when nothing changed; at once when anything but the measured numbers changed (the aura, the foe flag, the pulse, the fade, the
     * radius); for a change of the numbers alone at most once every {@link #WEIGH_RESEND} ticks (a data packet to every watcher each time).
     */
    public static boolean shouldSetWord(int current, int next, long now, long lastSet) {
        if (current == next) return false;
        if (((current ^ next) & ~WEIGHT_MASK) != 0) return true;
        return now - lastSet >= WEIGH_RESEND;
    }

    // ================================================================ where it floats
    /**
     * Where the Scale floats: to the caster's LEFT (the off hand side; the Compass is on the right), up and a little in front, and how far it is turned from the caster's facing toward the
     * caster, degrees (its front looks at the caster's face from the shoulder: the caster reads their own pan on the left and the foe's on the right).
     */
    public static final double SIDE = 1.15, UP = 1.45, FRONT = 0.20, IN_YAW = 80.0;
    /** Radius of the whole Scale (beam half length) in blocks: scale of the prop. */
    public static final float MAIN_SCALE = 0.55f;

    /** The Scale's centre for a caster at (x, y, z) facing {@code yawDeg} (Minecraft yaw: 0 faces +z): {x, y, z}. Allocation of one small array per call; the server asks twice a tick at most. */
    public static double[] position(double x, double y, double z, float yawDeg) {
        double a = Math.toRadians(yawDeg), fx = -Math.sin(a), fz = Math.cos(a), lx = Math.cos(a), lz = Math.sin(a);
        return new double[]{x + lx * SIDE + fx * FRONT, y + UP, z + lz * SIDE + fz * FRONT};
    }

    /** x of {@link #position} without allocating (the client asks per frame whether a Scale is the local player's own). */
    public static double positionX(double x, float yawDeg) { double a = Math.toRadians(yawDeg); return x + Math.cos(a) * SIDE - Math.sin(a) * FRONT; }

    /** z of {@link #position} without allocating. */
    public static double positionZ(double z, float yawDeg) { double a = Math.toRadians(yawDeg); return z + Math.sin(a) * SIDE + Math.cos(a) * FRONT; }

    /** y of {@link #position}. */
    public static double positionY(double y) { return y + UP; }

    /** The yaw the Scale faces (Minecraft yaw: positive turns toward the caster's left, which is where the caster is seen from the Scale). */
    public static float yaw(float ownerYawDeg) { return ownerYawDeg + (float) IN_YAW; }

    /** Moves {@code cur} toward {@code target} by the fraction k along the short way round, degrees. */
    public static float approachDegrees(float cur, float target, float k) {
        float d = ((target - cur) % 360f + 540f) % 360f - 180f;
        return cur + d * Math.max(0f, Math.min(1f, k));
    }

    // ================================================================ client animation
    /** Pop in with a little overshoot over {@code in} ticks. */
    public static float popIn(float ageTicks, float in) {
        float t = Math.max(0f, Math.min(1f, ageTicks / Math.max(1f, in)));
        float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    /** The ticks of the pop in and of the client's fade out. */
    public static final int FADE_IN = 10;

    /** Opacity 0..1: in over {@link #FADE_IN} ticks, out over {@link #FADE} ticks counted from {@code ticksSinceFade} (negative = not fading). */
    public static float opacity(float ageTicks, float ticksSinceFade) {
        float in = Math.max(0f, Math.min(1f, ageTicks / FADE_IN));
        float out = ticksSinceFade < 0f ? 1f : Math.max(0f, 1f - ticksSinceFade / FADE);
        return in * out;
    }

    // ================================================================ text (HUD readout)
    /** A weight in the short form the readout shows: 940, 12.4K, 3.1M, 2.5B, 7T, 1.2e15. */
    public static String format(double weight) {
        double w = Math.max(0.0, finite(weight));
        if (w < 1000.0) return String.format(Locale.ROOT, "%d", Math.round(w));
        String[] units = {"K", "M", "B", "T"};
        double v = w;
        for (String u : units) {
            v /= 1000.0;
            if (v < 999.5) return v < 10.0 ? String.format(Locale.ROOT, "%.1f%s", v, u) : String.format(Locale.ROOT, "%d%s", Math.round(v), u);
        }
        return String.format(Locale.ROOT, "%.1e", w);
    }

    // ================================================================ small helpers
    private static double finite(double v) { return Double.isFinite(v) ? v : 0.0; }
    private static double clamp01(double v) { return v < 0.0 ? 0.0 : Math.min(1.0, v); }
}
