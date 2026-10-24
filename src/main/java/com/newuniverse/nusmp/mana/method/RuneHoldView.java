package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;

/**
 * What a client knows of a rune that is being HELD (pure, no game classes, so the timeline is tested; the client holder {@code RuneClient} creates one per held rune and the layer
 * {@code RuneWritingLayer} reads it every frame). The server only says three things about a hold (it starts, it is released after N ticks, it is cancelled); everything that is drawn is
 * worked out here from the clock of the hold:
 * <ul>
 *   <li>the <b>hand ring</b> is written during the first {@code min} ticks, so the basic spell's rune is complete exactly when the minimum hold is reached;</li>
 *   <li>the <b>middle ring</b> is written from {@code min} to the middle of the charged range, the <b>outer ring</b> from {@code min} to {@code max} (complete exactly at the full hold), the
 *       <b>loose glyphs</b> in the last 40 % of the charged range;</li>
 *   <li>at the <b>release</b> the clock of the hold stops at the ticks held; the hand ring is always finished (the basic spell), the middle ring too when the minimum hold was reached,
 *       the outer ring and the loose glyphs only when the hold was full (a partly written ring stays as it was: the array the caster did not finish).</li>
 * </ul>
 * So what is on the screen at the release is exactly what the stage of {@link RuneCharge} pays for: one ring for a tap, two for a charged hold, everything for a full hold.
 */
public final class RuneHoldView {
    /** Ticks a ring takes to finish after the release (the completion of the tiers that were reached). */
    public static final double COMPLETE_TICKS = 5.0;
    /** The loose glyphs are written in this last share of the charged range. */
    private static final double FLOAT_SHARE = 0.4;
    /** How long a hold rune lives at most (a rune that never hears of its release or cast goes away by itself): the full hold plus this. */
    public static final int LIFE_AFTER_FULL = 100;
    /** Ticks a rune takes to fade at its end. */
    public static final int FADE = 4;

    public final int ownerId, minHold, maxHold;
    public final RuneSpec spec;
    /** The plane of the rune; the client holder replaces it every tick while the rune follows its caster. */
    public Frame frame;
    public boolean follow = true;
    /** Ticks since the rune began at which the page was released (-1 = still held) and the ticks held. */
    public float releaseAge = -1f;
    public int holdAtRelease;
    /** Ticks since the rune began at which the bolt leaves (-1 = unknown) and at which the rune is gone (-1 = not yet known). */
    public float launchAge = -1f, endAge = -1f;
    /**
     * 3.6.0, PAINTED runes (the Brush Rune and the Body Rune): a rune that nobody holds. When above 0 the rings are written one after the other over this many ticks from the start
     * (see {@link #written}) and stay written; the rune is then fixed or follows a body, and ends at {@link #endAge}. 0 = a held rune, as before.
     */
    public int paintTicks;

    public RuneHoldView(int ownerId, RuneSpec spec, int minHold, int maxHold, Frame frame) {
        this.ownerId = ownerId;
        this.spec = spec;
        this.minHold = RuneCharge.minHold(minHold);
        this.maxHold = RuneCharge.maxHold(minHold, maxHold);
        this.frame = frame;
    }

    public boolean released() { return releaseAge >= 0f; }

    public void release(float atAge, int held) {
        if (released()) return;
        releaseAge = Math.max(0f, atAge);
        holdAtRelease = Math.max(0, Math.min(RuneCharge.HOLD_CAP, held));
    }

    /** The duration to give the effect instance: the full hold plus the time a rune may linger. */
    public int lifetime() { return maxHold + LIFE_AFTER_FULL; }

    /** The stage the hold reached at the release (or so far). */
    public RuneCharge.Stage stageAt(double age) {
        return RuneCharge.stage(heldAt(age), minHold, maxHold);
    }

    /** The ticks held at {@code age}: the clock runs until the release, then stays. */
    public int heldAt(double age) {
        double h = released() ? holdAtRelease : Math.min(age, RuneCharge.HOLD_CAP);
        return (int) Math.max(0.0, Math.floor(h));
    }

    /**
     * How much of each group is written at {@code age} (0..1): {@code out[0]} the hand ring, {@code out[1]} the middle ring, {@code out[2]} the outer ring, {@code out[3]} the loose glyphs.
     * Writes four numbers; allocation free.
     */
    public void written(double age, double[] out) {
        if (paintTicks > 0) {                                                               // a painted rune: all four groups over the paint time, hand ring first
            double t = clamp(age / paintTicks);
            out[0] = clamp(t / 0.4);
            out[1] = clamp((t - 0.15) / 0.45);
            out[2] = clamp((t - 0.3) / 0.6);
            out[3] = clamp((t - 0.7) / 0.3);
            return;
        }
        double hold = released() ? holdAtRelease : Math.min(age, RuneCharge.HOLD_CAP);
        double span = maxHold - minHold, since = released() ? Math.max(0.0, age - releaseAge) : 0.0;
        double w0 = clamp(hold / minHold);
        double w1 = clamp((hold - minHold) / (0.5 * span));
        double w2 = clamp((hold - minHold) / span);
        double w3 = clamp((hold - minHold - (1.0 - FLOAT_SHARE) * span) / (FLOAT_SHARE * span));
        if (released()) {
            double done = clamp(since / COMPLETE_TICKS);
            w0 = w0 + (1.0 - w0) * done;                                                    // the basic spell is always a finished hand ring
            if (hold >= minHold) w1 = w1 + (1.0 - w1) * done;                               // two tiers: the middle ring is finished too
            // the outer ring and the loose glyphs are as far as the hold got them (finished only by a full hold)
        }
        out[0] = w0;
        out[1] = w1;
        out[2] = w2;
        out[3] = w3;
    }

    /** The layout progress and the ticks per unit of progress of the four groups at {@code age}: the numbers {@link RuneGlyphLayout#fillStaged} takes. */
    public void progress(double age, double[] prog, double[] scale, double[] scratch) {
        written(age, scratch);
        for (int g = 0; g < RuneGlyphLayout.RING_GROUPS; g++) {
            prog[g] = RuneGlyphLayout.progressOf(g, scratch[g]);
            scale[g] = ticksPerProgress(g);
        }
    }

    /** How many ticks one unit of layout progress of a group lasts, so that the pens of a ring move at the speed that finishes the ring in the ticks the hold allots it. */
    public double ticksPerProgress(int group) {
        double span = maxHold - minHold;
        double ringTicks = switch (group) {
            case 0 -> minHold;
            case 1 -> 0.5 * span;
            case 2 -> span;
            default -> FLOAT_SHARE * span;
        };
        double window = RuneGlyphLayout.progressOf(group, 1.0) - RuneGlyphLayout.progressOf(group, 0.0);
        return Math.max(1.0, ringTicks / Math.max(0.05, window));
    }

    /** The flare of the outer ring after the bolt left (0..1), 0 before. */
    public double flare(double age) {
        if (launchAge < 0f || age <= launchAge) return 0.0;
        return Math.min(1.0, (age - launchAge) / RuneRules.FLARE_TICKS);
    }

    /** The brightness of the whole rune at {@code age}: it fades over its last {@value #FADE} ticks, whether the end is known ({@link #endAge}) or it is the end of its lifetime. */
    public float fade(double age) {
        double end = endAge >= 0f ? endAge : lifetime();
        return (float) Math.max(0.0, Math.min(1.0, (end - age) / FADE));
    }

    /** The rings that exist for the stage a hold of {@code held} ticks reaches (for the tests and the previews): the tier of {@link RuneCharge}. */
    public int tierFor(int held) { return RuneCharge.tier(RuneCharge.stage(held, minHold, maxHold)); }

    private static double clamp(double v) { return Double.isNaN(v) ? 0.0 : Math.max(0.0, Math.min(1.0, v)); }

    // ---------------------------------------------------------------- the registry (client thread only)
    /** The views by key. The key travels in the effect's seed (bits 8..15) so the layer can find the view of its instance; 0 = no view (the plain, fixed rune of the old cast). */
    private static final RuneHoldView[] BY_KEY = new RuneHoldView[256];
    private static int nextKey = 1;

    /** Stores a view and returns its key (1..255, reused round-robin: at most six runes are alive at once). */
    public static int register(RuneHoldView v) {
        int key = nextKey;
        nextKey = nextKey >= 255 ? 1 : nextKey + 1;
        BY_KEY[key] = v;
        return key;
    }

    /** The view of a key, or null. */
    public static RuneHoldView lookup(int key) { return key > 0 && key < BY_KEY.length ? BY_KEY[key] : null; }

    /** The key a seed carries, 0 when it carries none. */
    public static int keyOf(long seed) { return (int) ((seed >>> 8) & 0xFF); }

    /** A seed for a rune: the packed spec (bits 0..5) and the view's key (bits 8..15). */
    public static long seedOf(int packedSpec, int key) { return (packedSpec & 63) | ((long) (key & 0xFF) << 8); }

    public static void forgetAll() { java.util.Arrays.fill(BY_KEY, null); }
}
