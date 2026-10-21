package com.newuniverse.nusmp.perf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Why: the owner's report on 1.109.0 was that the upgraded weapons and relics throw "so many" particles that the game lags. Every emitter (the ambient layers, the swing trails, the act
 * bursts, the tiny Djinn) used its own table rate and only the shared per-tick client allowance stood between a crowd of holders and the particle engine. This is the hard budget they all
 * share now, as pure maths plus a little state (no game types, injected tick time, so the numbers and the buckets are unit-tested):
 * <ul>
 *   <li><b>per item</b>: a token bucket per (holder, item) refilled at {@link #perItemPerSecond} particles per second;</li>
 *   <li><b>per player</b>: a second bucket per holder over all the items he holds or wears ({@link #perPlayerPerSecond});</li>
 *   <li><b>global</b>: the number of upgrade particles alive at once over every holder ({@link #liveCap}: tied to the effect quality and to the client allowance of the performance pass);</li>
 *   <li><b>scale</b>: the option {@code particles.upgradeDensity} (0 to 1, default 0.5), the effect quality ({@link VfxQuality#detail}), the frame rate ({@link #fpsFactor}: nothing below
 *       {@link #MIN_FPS}), the distance of somebody else's item (none beyond {@link #OTHERS_MAX_DISTANCE} blocks) and the view direction ({@link #viewFactor}: nothing behind the camera).</li>
 * </ul>
 * One instance belongs to the client thread. Holders are keyed by entity id (the client has no other key for a remote player; it is never a static "current player").
 */
public final class UpgradeBudget {
    /** An upgrade particle lives about this many ticks (the live count of the global cap covers this window). */
    public static final int LIFE_TICKS = 30;
    /** Somebody else's item spawns nothing beyond this distance (blocks). */
    public static final double OTHERS_MAX_DISTANCE = 16.0;
    /** The default of {@code particles.upgradeDensity}. */
    public static final float DEFAULT_DENSITY = 0.5f;
    /** Below this frame rate the upgrade effects spawn nothing; they are back at full rate from {@link #FULL_FPS}. */
    public static final int MIN_FPS = 12, FULL_FPS = 45;

    private static final int PRUNE_EVERY = 200, STALE_AFTER = 100;

    // ------------------------------------------------------------------------------------------------- pure maths
    /** The particles per second one held item may spawn (all its layers together, after the density scale). */
    public static int perItemPerSecond(VfxQuality q) {
        return switch (q) {
            case LOW -> 4;
            case MEDIUM -> 10;
            case HIGH -> 16;
        };
    }

    /** The particles per second one player may spawn over everything he holds and wears. */
    public static int perPlayerPerSecond(VfxQuality q) {
        return switch (q) {
            case LOW -> 8;
            case MEDIUM -> 18;
            case HIGH -> 28;
        };
    }

    /**
     * The most upgrade particles alive at once, over every holder: 60 / 140 / 240 by quality, scaled by the client allowance per tick of the performance pass (400 is the default; a smaller
     * allowance lowers it down to a quarter, a larger one raises it up to one and a half; 0 or less = unlimited allowance, the base cap stays).
     */
    public static int liveCap(VfxQuality q, int clientParticlesPerTick) {
        int base = switch (q) {
            case LOW -> 60;
            case MEDIUM -> 140;
            case HIGH -> 240;
        };
        if (clientParticlesPerTick <= 0) return base;
        float s = clientParticlesPerTick / (float) PerfSettings.DEFAULT_CLIENT_PARTICLES_PER_TICK;
        s = s < 0.25f ? 0.25f : Math.min(s, 1.5f);
        return Math.max(1, Math.round(base * s));
    }

    /** The option clamped to 0..1 (NaN gives the default). */
    public static float density(float option) {
        if (Float.isNaN(option)) return DEFAULT_DENSITY;
        return option < 0f ? 0f : Math.min(option, 1f);
    }

    /** 1 at {@link #FULL_FPS} or more (or when the frame rate is not measured yet: 0 or less), 0 below {@link #MIN_FPS}, linear between. */
    public static float fpsFactor(int fps) {
        if (fps <= 0 || fps >= FULL_FPS) return 1f;
        if (fps < MIN_FPS) return 0f;
        return (fps - MIN_FPS) / (float) (FULL_FPS - MIN_FPS);
    }

    /** Own item: 1. Somebody else's: 1 up to 8 blocks, falling linearly to 0 at {@link #OTHERS_MAX_DISTANCE}. */
    public static float distanceFactor(double dist, boolean own) {
        if (own) return 1f;
        if (dist >= OTHERS_MAX_DISTANCE) return 0f;
        if (dist <= 8.0) return 1f;
        return (float) ((OTHERS_MAX_DISTANCE - dist) / (OTHERS_MAX_DISTANCE - 8.0));
    }

    /**
     * The view-direction cull: {@code dot} is the cosine of the angle between the camera's look direction and the direction to the holder. Own item and anybody within 3 blocks: 1; otherwise 1
     * for dot 0.3 and more (inside the view), 0 for -0.1 or less (behind the camera), linear between.
     */
    public static float viewFactor(double dot, double distSq, boolean own) {
        if (own || distSq <= 9.0) return 1f;
        if (dot >= 0.3) return 1f;
        if (dot <= -0.1) return 0f;
        return (float) ((dot + 0.1) / 0.4);
    }

    /** The product of all the scales of one holder for one tick: density x quality x fps x distance x view (0 = spawn nothing for this holder). */
    public static float scale(float densityOption, VfxQuality q, int fps, double dist, double dot, boolean own) {
        float s = density(densityOption) * q.detail * fpsFactor(fps) * distanceFactor(dist, own) * viewFactor(dot, dist * dist, own);
        return s < 0f ? 0f : s;
    }

    /** The most particles one signature burst (an act, a tiny Djinn's poof) may spawn: 30 / 20 / 8 by quality. */
    public static int burstCap(VfxQuality q) {
        return switch (q) {
            case LOW -> 8;
            case MEDIUM -> 20;
            case HIGH -> 30;
        };
    }

    /** The particles a burst of {@code wanted} spawns: scaled by the density, the quality and the frame rate, at least one when any is left, never above {@link #burstCap}. */
    public static int burstCount(int wanted, float densityOption, VfxQuality q, int fps) {
        if (wanted <= 0) return 0;
        float s = density(densityOption) * q.detail * fpsFactor(fps);
        if (s <= 0f) return 0;
        return Math.min(burstCap(q), Math.max(1, Math.round(wanted * s)));
    }

    /** {@code want} particles as a whole number: the integer part, plus one with the probability of the fraction ({@code r} in [0,1)). */
    public static int stochastic(float want, float r) {
        if (!(want > 0f)) return 0;
        int n = (int) want;
        return r < want - n ? n + 1 : n;
    }

    // ------------------------------------------------------------------------------------------------- the buckets
    private final Map<Integer, double[]> items = new HashMap<>();                 // {tokens, last tick}
    private final Map<Integer, double[]> players = new HashMap<>();
    private final long[] ringTick = new long[LIFE_TICKS];
    private final int[] ringCount = new int[LIFE_TICKS];
    private long lastPrune = Long.MIN_VALUE;
    private long asked, granted;

    /** The key of one holder's one item (the item key is any stable int: the item's hash, a slot number, a layer id). */
    public static int key(int holderId, int itemKey) {
        return holderId * 131 + (itemKey & 0x7F);
    }

    private static double[] bucket(Map<Integer, double[]> map, int key, long tick, double perTick, double burst) {
        double[] b = map.get(key);
        if (b == null) {
            b = new double[]{burst * 0.5, tick};
            map.put(key, b);
            return b;
        }
        long dt = tick - (long) b[1];
        if (dt > 0) {
            b[0] = Math.min(burst, b[0] + dt * perTick);
            b[1] = tick;
        } else if (dt < 0) {
            b[1] = tick;                                                            // the clock went back (a new world): keep the tokens, follow the clock
        }
        return b;
    }

    private static double burstOf(int perSecond) {
        return Math.max(2.0, perSecond * 0.5);
    }

    /** How many upgrade particles are alive (spawned within the last {@link #LIFE_TICKS} ticks). */
    public int live(long tick) {
        int n = 0;
        for (int i = 0; i < LIFE_TICKS; i++) if (tick - ringTick[i] < LIFE_TICKS && tick - ringTick[i] >= 0) n += ringCount[i];
        return n;
    }

    /**
     * How many of {@code wanted} particles holder {@code holderId} may spawn for item {@code itemKey} in tick {@code tick}: the least of the item bucket, the player bucket and the room left
     * under the global live cap. What is granted is spent from all three.
     */
    public int take(int holderId, int itemKey, long tick, int wanted, VfxQuality q, int clientParticlesPerTick) {
        if (wanted <= 0) return 0;
        asked += wanted;
        prune(tick);
        int ip = perItemPerSecond(q), pp = perPlayerPerSecond(q);
        double[] ib = bucket(items, key(holderId, itemKey), tick, ip / 20.0, burstOf(ip));
        double[] pb = bucket(players, holderId, tick, pp / 20.0, burstOf(pp));
        int free = Math.max(0, liveCap(q, clientParticlesPerTick) - live(tick));
        int n = Math.min(wanted, Math.min((int) ib[0], Math.min((int) pb[0], free)));
        if (n <= 0) return 0;
        ib[0] -= n;
        pb[0] -= n;
        int idx = (int) Math.floorMod(tick, (long) LIFE_TICKS);
        if (ringTick[idx] != tick) {
            ringTick[idx] = tick;
            ringCount[idx] = 0;
        }
        ringCount[idx] += n;
        granted += n;
        return n;
    }

    /** How many of {@code wanted} burst particles fit under the global live cap in this tick (bursts are rare and one-shot, so they skip the per-item buckets); what is granted counts as live. */
    public int takeBurst(long tick, int wanted, VfxQuality q, int clientParticlesPerTick) {
        if (wanted <= 0) return 0;
        asked += wanted;
        int n = Math.min(wanted, Math.max(0, liveCap(q, clientParticlesPerTick) - live(tick)));
        if (n <= 0) return 0;
        int idx = (int) Math.floorMod(tick, (long) LIFE_TICKS);
        if (ringTick[idx] != tick) {
            ringTick[idx] = tick;
            ringCount[idx] = 0;
        }
        ringCount[idx] += n;
        granted += n;
        return n;
    }

    private void prune(long tick) {
        if (lastPrune != Long.MIN_VALUE && tick - lastPrune < PRUNE_EVERY && tick >= lastPrune) return;
        lastPrune = tick;
        stale(items, tick);
        stale(players, tick);
    }

    private static void stale(Map<Integer, double[]> map, long tick) {
        for (Iterator<double[]> it = map.values().iterator(); it.hasNext(); ) {
            double[] b = it.next();
            if (tick - (long) b[1] > STALE_AFTER || tick < (long) b[1]) it.remove();
        }
    }

    /** Forgets every bucket and the live count (leaving a world). */
    public void clear() {
        items.clear();
        players.clear();
        java.util.Arrays.fill(ringTick, 0L);
        java.util.Arrays.fill(ringCount, 0);
        lastPrune = Long.MIN_VALUE;
    }

    public long asked() { return asked; }

    public long granted() { return granted; }

    public void resetStats() {
        asked = 0;
        granted = 0;
    }

    public int trackedItems() { return items.size(); }

    public int trackedPlayers() { return players.size(); }

    /** One line for {@code /nusmp perf report}. */
    public String line(long tick, VfxQuality q, int clientParticlesPerTick) {
        return "upgrade particles: live " + live(tick) + "/" + liveCap(q, clientParticlesPerTick) + ", granted " + granted + " of " + asked + " asked, " + items.size() + " items / " + players.size() + " holders tracked";
    }
}
