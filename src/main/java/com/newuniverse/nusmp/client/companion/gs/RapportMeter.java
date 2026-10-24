package com.newuniverse.nusmp.client.companion.gs;

import com.newuniverse.nusmp.companion.CompanionRules;

/**
 * The rapport meter of the Great Spirit screen: three leaves that light at rapport 3, 10 and 25 (acquaintance, friend, close; the numbers are {@link CompanionRules}' own, so they cannot drift), a bar that
 * fills toward the next leaf, and the animation when rapport rises (the bar glides to its new value, the glow decays, a newly lit leaf pops). All maths, no Minecraft type, unit tested; the painter only
 * asks for numbers.
 */
public final class RapportMeter {
    private RapportMeter() {}

    /** The rapport at which leaf 0, 1 and 2 light. */
    public static final int[] LEAF_AT = {CompanionRules.ACQUAINTANCE_AT, CompanionRules.FRIEND_AT, CompanionRules.CLOSE_AT};
    public static final int LEAVES = 3;
    /** Ticks the bar takes to glide to a new value, the glow takes to fade, and a newly lit leaf takes to pop. */
    public static final float GLIDE = 24f, GLOW = 48f, POP = 16f;

    /** The tier (0 stranger .. 3 close) of a rapport number. */
    public static int tier(int rapport) { return CompanionRules.tier(rapport); }

    /** How many leaves are lit. */
    public static int lit(int rapport) { return tier(rapport); }

    /** Is leaf i (0..2) lit at this rapport? */
    public static boolean isLit(int rapport, int leaf) { return leaf >= 0 && leaf < LEAVES && rapport >= LEAF_AT[leaf]; }

    /** How far the bar is toward leaf i: 1 when lit, else the share of the way from the previous threshold. */
    public static float leafProgress(float rapport, int leaf) {
        if (leaf < 0 || leaf >= LEAVES) return 0f;
        float lo = leaf == 0 ? 0f : LEAF_AT[leaf - 1], hi = LEAF_AT[leaf];
        return clamp01((rapport - lo) / (hi - lo));
    }

    /** The whole bar, 0..1: each leaf owns a third, so the bar is full at the 'close' tier. */
    public static float bar(float rapport) {
        float sum = 0f;
        for (int i = 0; i < LEAVES; i++) sum += leafProgress(rapport, i);
        return sum / LEAVES;
    }

    /** Rapport still to go to the next leaf (0 at close). */
    public static int toNext(int rapport) {
        for (int i = 0; i < LEAVES; i++) if (rapport < LEAF_AT[i]) return LEAF_AT[i] - rapport;
        return 0;
    }

    /** The rapport value to draw 'age' ticks after it went from 'from' to 'to' (an ease-out glide; falls are instant). */
    public static float shown(int from, int to, float age) {
        if (to <= from || from < 0) return to;
        float t = clamp01(age / GLIDE), u = 1f - t;
        return from + (to - from) * (1f - u * u * u);
    }

    /** 0..1 glow of the bar and the leaves after a rise ('age' ticks ago); 0 when there was none or it has faded. */
    public static float glow(float age) {
        if (age < 0f || age >= GLOW) return 0f;
        float u = 1f - age / GLOW;
        return u * u;
    }

    /** Did leaf i light up because of the change from 'from' to 'to'? */
    public static boolean newlyLit(int from, int to, int leaf) {
        return leaf >= 0 && leaf < LEAVES && from >= 0 && from < LEAF_AT[leaf] && to >= LEAF_AT[leaf];
    }

    /** The scale (1 = normal) of a leaf 'age' ticks after it lit: a quick pop that settles. */
    public static float pop(float age) {
        if (age < 0f || age >= POP) return 1f;
        return 1f + 0.45f * (float) Math.sin(Math.PI * age / POP);
    }

    private static float clamp01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }

    // ---------------------------------------------------------------- the tracker of one open screen
    /** Remembers the last rapport the screen was shown so a rise can be animated (one per open screen). */
    public static final class Track {
        private int from = -1, to = -1;
        private float riseAt = -1000f;

        /** Feeds the rapport of a new view. Returns the change (0 for the first view or no change). */
        public int update(int rapport, float now) {
            if (rapport < 0) return 0;
            int delta = to < 0 ? 0 : rapport - to;
            if (to < 0) {
                from = rapport;
            } else if (delta > 0) {
                from = shownRounded(now);
                riseAt = now;
            } else if (delta < 0) {
                from = rapport;
                riseAt = -1000f;
            }
            to = rapport;
            return delta;
        }

        private int shownRounded(float now) { return Math.max(0, Math.round(shown(now))); }

        /** The rapport value to draw now. */
        public float shown(float now) { return to < 0 ? 0f : RapportMeter.shown(from, to, now - riseAt); }

        public float glow(float now) { return RapportMeter.glow(now - riseAt); }

        /** The leaf that lit with the latest rise and how long ago, or -1. */
        public int litLeaf(float now) {
            if (now - riseAt >= POP) return -1;
            for (int i = 0; i < LEAVES; i++) if (newlyLit(from, to, i)) return i;
            return -1;
        }

        public float sinceRise(float now) { return now - riseAt; }

        public int rapport() { return to; }
    }
}
