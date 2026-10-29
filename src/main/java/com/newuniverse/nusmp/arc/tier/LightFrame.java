package com.newuniverse.nusmp.arc.tier;

/**
 * Why: the anime-style cinematic of the Death Note gag (black bars, speed lines, a close-up of the red eyes, panel bursts, the pen rising, the green pool of light over the page, the
 * screen shake) is drawn by the client only, but WHAT is drawn at each tick of the scene is plain arithmetic, so it lives here, pure, where a unit test can prove the bounds the server rules ask
 * for: at most {@link #MAX_QUADS} rectangles a frame, always inside the screen (plus the shake), never any allocation (one reusable object holds the arrays; {@link #plan} refills it).
 * The client class only reads {@code x0..argb[0..n)} and fills them with {@code GuiGraphics.fill}, draws the text the plan names, and shakes the view by {@code shakeX / shakeY}.
 *
 * <p>The beats follow {@link LightScene}: the chip (the eyes, the dark slash, the dramatic chip, the panel burst), the raised pen (the vignette, the rising hand and pen), the writing (the
 * green pool, the page, the second panel burst, the name being written, a bar that fills until the last tick). A bystander (not the target) gets only the bars and the captions.
 */
public final class LightFrame {
    /** The most rectangles any frame holds. */
    public static final int MAX_QUADS = 24;
    /** Text the plan can name: 0 = none. */
    public static final int CAPTION_NONE = 0, CAPTION_CHIP = 1, CAPTION_WRITING = 2;
    public static final int CARD_NONE = 0, CARD_CHIP = 1, CARD_PEN = 2, CARD_NOTE = 3;

    public final int[] x0 = new int[MAX_QUADS], y0 = new int[MAX_QUADS], x1 = new int[MAX_QUADS], y1 = new int[MAX_QUADS], argb = new int[MAX_QUADS];
    public int n;
    /** The shake in pixels (and, scaled, in degrees of camera roll); 0 for a bystander. */
    public float shakeX, shakeY;
    public int caption, card;
    /** 0..1: how far the title card has grown (the client scales the text by it). */
    public float cardGrow;
    /** 0..1: how much of the name on the page is written (0 until the pen touches). */
    public float write;
    /** True from the moment the page is on screen (the client draws the name on it). */
    public boolean page;
    /** The page's rectangle (valid when {@link #page}). */
    public int pageX0, pageY0, pageX1, pageY1;

    private void clear() {
        n = 0;
        shakeX = shakeY = 0f;
        caption = CAPTION_NONE;
        card = CARD_NONE;
        cardGrow = 0f;
        write = 0f;
        page = false;
    }

    private void quad(int a, int b, int c, int d, int color) {
        if (n >= MAX_QUADS || ((color >>> 24) == 0)) return;
        x0[n] = Math.min(a, c);
        y0[n] = Math.min(b, d);
        x1[n] = Math.max(a, c);
        y1[n] = Math.max(b, d);
        argb[n] = color;
        n++;
    }

    private static int alpha(int a, int rgb) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }

    private static float ease(float t) { t = Math.max(0f, Math.min(1f, t)); return t * t * (3f - 2f * t); }

    /** A cheap deterministic value in 0..1 from two ints (no Random object per frame). */
    static float noise(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535f;
    }

    /**
     * Fills this plan for one frame.
     *
     * @param age    ticks since the scene began (may be fractional through the partial tick; clamped to the scene)
     * @param w      screen width in GUI pixels
     * @param h      screen height in GUI pixels
     * @param target true for the player being written at (the full cinematic), false for a bystander (bars and captions)
     * @param fade   1 while the scene runs, falling to 0 after it was cancelled or ended (the bars slide away)
     */
    public LightFrame plan(float age, int w, int h, boolean target, float fade) {
        clear();
        if (w <= 0 || h <= 0 || fade <= 0f) return this;
        int t = (int) Math.max(0, Math.min(LightScene.CLIENT_HARD_LIMIT, age));
        LightScene.Phase ph = LightScene.phaseAt(t);
        boolean writing = ph == LightScene.Phase.WRITE || ph == LightScene.Phase.DONE;
        // the letterbox bars: grow through the chip, thicker for the pen, thickest while writing
        float bar = ph == LightScene.Phase.CHIP ? 0.10f + 0.02f * ease(t / 10f) : ph == LightScene.Phase.RAISE ? 0.12f + 0.04f * ease((t - LightScene.CHIP_TICKS) / 20f) : 0.17f;
        int bh = Math.round(h * bar * ease(t / 8f) * fade);
        quad(0, 0, w, bh, alpha(255, 0x000000));
        quad(0, h - bh, w, h, alpha(255, 0x000000));
        // captions and title cards (named here, drawn by the client)
        if (t >= 6 && t < 62) caption = CAPTION_CHIP;
        else if (writing) caption = CAPTION_WRITING;
        if (!target) return this;                                                  // a bystander: bars and captions only
        if (t >= 30 && t < 56) { card = CARD_CHIP; cardGrow = ease((t - 30) / 8f); }
        else if (t >= 82 && t < 106) { card = CARD_PEN; cardGrow = ease((t - 82) / 8f); }
        else if (t >= 112 && t < 136) { card = CARD_NOTE; cardGrow = ease((t - 112) / 8f); }

        int mid = h / 2;
        // part 1: the chip. A flash on the first beat, the red eyes close up under a hard dark slash, speed lines, then the panel burst
        if (t < 10) quad(0, 0, w, h, alpha(Math.round(200 * (1f - t / 10f) * fade), 0xFFFFFF));
        if (t >= 8 && t < 30) {
            float k = ease((t - 8) / 4f) * (1f - ease((t - 24) / 6f));
            quad(0, mid - h / 7, w, mid + h / 7, alpha(Math.round(215 * k * fade), 0x000000));
            quad(w * 3 / 10, mid - 3, w * 9 / 20, mid + 3, alpha(Math.round(255 * k * fade), 0xE01818));
            quad(w * 11 / 20, mid - 3, w * 7 / 10, mid + 3, alpha(Math.round(255 * k * fade), 0xE01818));
        }
        if (t >= 10 && t < 62) speedLines(t, w, h, bh, 6, 0xFFFFFF, 110);
        if (t >= 52 && t < 68) panelBurst(t - 52, w, h, bh, 0x101010, 0xC41E24);
        // part 2: the pen. The screen darkens at its edges, a hand and a pen rise from the bottom
        if (ph == LightScene.Phase.RAISE) {
            float k = ease((t - LightScene.CHIP_TICKS) / 12f) * fade;
            int a = Math.round(150 * k);
            quad(0, bh, w / 8, h - bh, alpha(a, 0x000000));
            quad(w - w / 8, bh, w, h - bh, alpha(a, 0x000000));
            float rise = ease((t - LightScene.CHIP_TICKS) / (float) LightScene.RAISE_TICKS);
            int hx = w / 2 - w / 24, hy = h - bh - Math.round(rise * h * 0.30f);
            quad(hx, hy, hx + w / 12, h - bh, alpha(Math.round(230 * fade), 0xE8CFC0));
            quad(hx + w / 24 - 1, hy - h / 14, hx + w / 24 + 2, hy, alpha(Math.round(255 * fade), 0x0A0A0A));
            speedLines(t, w, h, bh, 4, 0xFFC0C0, 80);
        }
        // part 3: the writing. The green pool, the page, a burst when the pen lands, a bar that fills until the last tick
        if (writing) {
            float in = ease((t - LightScene.CHIP_TICKS - LightScene.RAISE_TICKS) / 10f) * fade;
            quad(w / 5, h * 11 / 20, w * 4 / 5, h * 17 / 20, alpha(Math.round(95 * in), 0x38FF70));
            pageX0 = w * 3 / 10;
            pageY0 = h * 3 / 5;
            pageX1 = w * 7 / 10;
            pageY1 = h * 4 / 5;
            quad(pageX0, pageY0, pageX1, pageY1, alpha(Math.round(235 * in), 0xF2F2EA));
            page = in > 0.05f;
            write = LightScene.writeProgress(t);
            int bx = pageX0 + Math.round((pageX1 - pageX0) * write);
            quad(pageX0, pageY1 + 2, bx, pageY1 + 5, alpha(Math.round(255 * in), 0xE01818));
            if (t >= 112 && t < 124) panelBurst(t - 112, w, h, bh, 0x0A2A10, 0x38FF70);
            if (t >= 120) speedLines(t, w, h, bh, 6, 0xC8FFD8, 90);
            if (t >= LightScene.TOTAL_TICKS - 5) quad(0, 0, w, h, alpha(Math.round(220 * ease((t - (LightScene.TOTAL_TICKS - 5)) / 5f) * fade), 0xFFFFFF));
        }
        // the shake: none at first, a tremor during the chip's peak, growing with the pen, hard on the last ticks
        float amp = t < 30 ? 0f : t < 56 ? 2f : t < 70 ? 0.5f : ph == LightScene.Phase.RAISE ? 1f + (t - 70) / 20f : 3f + 4f * ease((t - (LightScene.TOTAL_TICKS - 12)) / 12f);
        shakeX = (noise(t, 1) - 0.5f) * 2f * amp * fade;
        shakeY = (noise(t, 2) - 0.5f) * 2f * amp * fade;
        return this;
    }

    /** Thin lines that rush in from the screen's edges, a few at a time, refreshed every 2 ticks (a hand-drawn look): {@code count} rectangles at most. */
    private void speedLines(int t, int w, int h, int bar, int count, int rgb, int a) {
        int step = t / 2;
        int usable = h - 2 * bar;
        if (usable <= 8) return;
        for (int i = 0; i < count; i++) {
            float yy = noise(step, i * 7 + 3);
            int y = bar + 2 + Math.round(yy * (usable - 6));
            int len = Math.round(w * (0.12f + 0.20f * noise(step, i * 13 + 5)));
            if ((i & 1) == 0) quad(0, y, len, y + 2, alpha(a, rgb));
            else quad(w - len, y, w, y + 2, alpha(a, rgb));
        }
    }

    /** Four boxes at once (the manga panel burst): they slide in from the corners over 6 ticks and stay; four rectangles. */
    private void panelBurst(int k, int w, int h, int bar, int fill, int edge) {
        float e = ease(k / 6f);
        int gap = Math.max(3, w / 80);
        int top = bar + gap, bottom = h - bar - gap;
        int cx = w / 2, cy = h / 2;
        int pw = Math.round(w * 0.30f * e), ph = Math.round((bottom - top) * 0.30f * e);
        if (pw <= 0 || ph <= 0) return;
        quad(cx - gap - pw, cy - gap - ph, cx - gap, cy - gap, alpha(200, fill));
        quad(cx + gap, cy - gap - ph, cx + gap + pw, cy - gap, alpha(200, edge));
        quad(cx - gap - pw, cy + gap, cx - gap, cy + gap + ph, alpha(200, edge));
        quad(cx + gap, cy + gap, cx + gap + pw, cy + gap + ph, alpha(200, fill));
    }

    /** How many characters of a name of {@code length} are written at this share of the writing (all of them at 1). */
    public static int charsWritten(float write, int length) {
        if (length <= 0) return 0;
        return Math.max(0, Math.min(length, (int) Math.ceil(Math.max(0f, Math.min(1f, write)) * length)));
    }
}
