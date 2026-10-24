package com.newuniverse.nusmp.client.companion.gs;

/**
 * The companion's portrait, as pure data: a bust built from her own player-model skin (the head's front and hat layer, the top of the torso and the two arms) drawn big, with the five expressions laid
 * over the face as a handful of coloured rectangles, a blink, a breath and a little shake when she is angry. The face of a standard skin is 8 by 8 texels with the eyes on rows 4 and 5, the cheeks on row 6
 * and the mouth on row 7, so every rectangle below is given in HALF texels of the face ({@code x2, y2, w2, h2}) and a colour; the colour {@link #TONE} means "her own skin tone" (sampled from the skin
 * when the screen opens), which is how an open eye is covered for a blink or a smile. Static arrays only: a frame allocates nothing here. Unit tested.
 */
public final class PortraitPlan {
    private PortraitPlan() {}

    /** The colour code for "the skin tone of this face" (a real colour always has a non-zero alpha). */
    public static final int TONE = 0;
    private static final int DARK = 0xFF3A2418, MOUTH = 0xFF8A3030, WHITE = 0xFFFFFFFF;

    private static final int[][] NEUTRAL = {};
    private static final int[][] HAPPY = {
            {2, 8, 4, 4, TONE}, {10, 8, 4, 4, TONE},                                               // the eyes shut into arches
            {2, 10, 1, 2, DARK}, {3, 9, 2, 1, DARK}, {5, 10, 1, 2, DARK},
            {10, 10, 1, 2, DARK}, {11, 9, 2, 1, DARK}, {13, 10, 1, 2, DARK},
            {4, 14, 8, 2, TONE}, {5, 14, 6, 1, MOUTH}, {6, 15, 4, 1, MOUTH},                        // an open smile
            {2, 12, 4, 2, 0x55FF6080}, {10, 12, 4, 2, 0x55FF6080}};                                 // a little more blush
    private static final int[][] SHY = {
            {2, 8, 4, 2, TONE}, {10, 8, 4, 2, TONE},                                               // half closed lids
            {2, 10, 4, 1, DARK}, {10, 10, 4, 1, DARK},
            {0, 12, 6, 2, 0x99FF5878}, {10, 12, 6, 2, 0x99FF5878},                                  // a deep blush
            {4, 14, 8, 2, TONE}, {6, 14, 1, 1, MOUTH}, {7, 15, 1, 1, MOUTH}, {8, 14, 1, 1, MOUTH}, {9, 15, 1, 1, MOUTH},
            {14, 6, 1, 2, 0xCC9AD8FF}};                                                             // a sweat drop
    private static final int[][] ANGRY = {
            {2, 6, 2, 1, DARK}, {4, 7, 2, 1, DARK}, {10, 7, 2, 1, DARK}, {12, 6, 2, 1, DARK},       // brows slanting in
            {2, 8, 4, 1, DARK}, {10, 8, 4, 1, DARK},
            {2, 12, 4, 2, 0x55FF3030}, {10, 12, 4, 2, 0x55FF3030},
            {4, 14, 8, 2, TONE}, {6, 14, 4, 1, MOUTH}, {5, 15, 1, 1, MOUTH}, {10, 15, 1, 1, MOUTH}, // a frown
            {12, 2, 1, 3, 0xFFE03030}, {11, 3, 3, 1, 0xFFE03030}};                                  // the vein mark
    private static final int[][] HUNGRY = {
            {3, 10, 1, 1, WHITE}, {11, 10, 1, 1, WHITE},                                            // shining eyes
            {4, 14, 8, 2, TONE}, {6, 14, 4, 2, 0xFF7A2A2A}, {7, 15, 2, 1, 0xFFE06070},               // an open mouth and a tongue
            {10, 15, 1, 3, 0xCCBDE8FF}};                                                            // a drop of drool
    private static final int[][] BLINK = {{2, 8, 4, 4, TONE}, {10, 8, 4, 4, TONE}, {2, 11, 4, 1, DARK}, {10, 11, 4, 1, DARK}};

    /** The rectangles {x2, y2, w2, h2, argb} of an expression (never null, do not modify). */
    public static int[][] rects(Mood m) {
        if (m == null) return NEUTRAL;
        return switch (m) {
            case HAPPY -> HAPPY;
            case SHY -> SHY;
            case ANGRY -> ANGRY;
            case HUNGRY -> HUNGRY;
            default -> NEUTRAL;
        };
    }

    /** The rectangles of a closed eye (drawn over the expression while she blinks). */
    public static int[][] blinkRects() { return BLINK; }

    /** The most rectangles any expression plus a blink draws (for the cost statement and the tests). */
    public static int maxRects() {
        int n = BLINK.length;
        int most = 0;
        for (Mood m : Mood.values()) most = Math.max(most, rects(m).length);
        return n + most;
    }

    /** Is she mid-blink at this time (ticks)? A blink of 5 ticks about every 5 seconds, a quick second one now and then. Happy faces already have their eyes closed. */
    public static boolean blinking(float time, Mood m) {
        if (m == Mood.HAPPY) return false;
        int t = (int) Math.floor(time) % 103;
        if (t < 0) t += 103;
        return t < 5 || (t >= 12 && t < 15);
    }

    /** The breath: 0 or 1 pixel the head rises (and the shoulders stretch to meet it), one slow cycle every 70 ticks. */
    public static int breath(float time) { return Math.sin(time * 0.09f) > 0.15f ? 1 : 0; }

    /** A shiver of the head when she is angry: -1, 0 or +1 pixel for the first 24 ticks of the mood. */
    public static int shake(Mood m, float moodAge) {
        if (m != Mood.ANGRY || moodAge < 0f || moodAge >= 24f) return 0;
        return ((int) (moodAge / 2f) % 2 == 0) ? 1 : -1;
    }

    /** The biggest scale (pixels per texel, at most 'max') at which the bust of 16 by 12 texels (head 8 high plus 4 of shoulders, arms either side) fits the inner box. */
    public static int scaleFor(int innerW, int innerH, int max) {
        int s = Math.min(innerW / 15, innerH / 13);
        return Math.max(1, Math.min(max, s));
    }

    /** Pixels of a half-texel start/size at a scale. */
    public static int px(int half, int scale) { return half * scale / 2; }

    /** A size in pixels that is at least one pixel. */
    public static int pxSize(int half, int scale) { return Math.max(1, half * scale / 2); }

    // ---------------------------------------------------------------- the mood's sparkles around the head
    /** How many sparkles the mood draws (at most {@link #MAX_AURA}). */
    public static final int MAX_AURA = 4;

    public static int auraCount(Mood m) {
        return switch (m) {
            case HAPPY -> 4;
            case SHY -> 3;
            case ANGRY -> 2;
            case HUNGRY -> 2;
            default -> 0;
        };
    }

    /** The colour (RGB) of a mood's sparkles. */
    public static int auraColor(Mood m) {
        return switch (m) {
            case HAPPY -> 0xFFE88A;
            case SHY -> 0xFF8EB4;
            case ANGRY -> 0xFF5A4A;
            case HUNGRY -> 0xA8E0FF;
            default -> 0xFFFFFF;
        };
    }

    /**
     * Sparkle i of a mood inside a box of w by h: fills out = {x, y, alpha 0..255}. They drift up and fade in a loop of 60 ticks, each starting at its own phase. Returns false when i is not a sparkle of
     * this mood.
     */
    public static boolean aura(Mood m, int i, float time, int w, int h, int[] out) {
        if (i < 0 || i >= auraCount(m)) return false;
        float phase = ((time + i * 17f) % 60f) / 60f;
        float side = (i % 2 == 0) ? 0.18f : 0.82f;
        float jitter = m == Mood.ANGRY ? (float) Math.sin(time * 0.9f + i) * 2f : 0f;
        out[0] = (int) (w * (side + (i / 2) * (i % 2 == 0 ? 0.09f : -0.09f)) + jitter);
        out[1] = (int) (h * (0.55f - phase * 0.35f - (i / 2) * 0.08f));
        out[2] = (int) (255f * Math.sin(Math.PI * phase));
        return true;
    }
}
