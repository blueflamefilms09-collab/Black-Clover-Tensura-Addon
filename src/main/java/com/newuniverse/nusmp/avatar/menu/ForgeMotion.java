package com.newuniverse.nusmp.avatar.menu;

/**
 * Why: the Soul Forge screen is meant to feel like a character creator: the module with the page's controls does not pop in, it slides out to the left and the
 * next one slides in with an eased curve, and numbers glide instead of jumping. All of that motion is arithmetic over "ticks", so it is kept here, free of any
 * game class, and unit tested: a slide never shows two pages at once, never ends half way, and can be reversed in the middle of its flight without a jump. The
 * screen only reads {@code offset()} / {@code value()} once per frame and advances the clocks once per client tick (or by the frame time, which is also accepted).
 */
public final class ForgeMotion {
    private ForgeMotion() {}

    public static float clamp01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Fast start, slow end: 0 -> 0, 1 -> 1, never above 1. */
    public static float easeOutCubic(float t) {
        float u = 1f - clamp01(t);
        return 1f - u * u * u;
    }

    /** Slow start and end: 0 -> 0, 0.5 -> 0.5, 1 -> 1. */
    public static float easeInOutCubic(float t) {
        float x = clamp01(t);
        return x < 0.5f ? 4f * x * x * x : 1f - (float) Math.pow(-2f * x + 2f, 3) / 2f;
    }

    /** Overshoots a little before it settles (a pop): 0 -> 0, 1 -> 1. */
    public static float easeOutBack(float t) {
        float x = clamp01(t), c1 = 1.70158f, c3 = c1 + 1f;
        float u = x - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    /**
     * One number that glides to a target along an eased curve over a fixed number of ticks. Starting a new glide while one is flying continues from where the
     * number is NOW, so reversing never jumps.
     */
    public static final class Glide {
        private float from, to, t = 1f, dur = 1f;

        public Glide(float start) { from = start; to = start; }

        /** Jumps to a value at once (no glide). */
        public void set(float v) { from = v; to = v; t = 1f; }

        /** Starts gliding from the current value to {@code target} over {@code ticks} (at least one). */
        public void to(float target, float ticks) {
            from = value();
            to = target;
            dur = Math.max(1f, ticks);
            t = 0f;
        }

        /** Advances by {@code dt} ticks (fractions allowed; never goes back). */
        public void tick(float dt) {
            if (t < 1f && dt > 0f) t = Math.min(1f, t + dt / dur);
        }

        public float value() { return t >= 1f ? to : lerp(from, to, easeOutCubic(t)); }

        public float target() { return to; }

        public boolean done() { return t >= 1f; }
    }

    /**
     * The eased hover glow of many controls at once: each key (a button, an icon in a grid) has its own 0..1 value that rises while the pointer is on it and
     * falls when it leaves, so highlights fade instead of snapping. Keys that were not asked about for a while are forgotten, so a long grid never leaks.
     */
    public static final class Hovers {
        private final java.util.Map<Object, float[]> v = new java.util.HashMap<>();
        private int frame;

        /** One step for {@code key}: moves its value toward 1 (hovered) or 0 by {@code rate} per tick over {@code dt} ticks, and returns it. */
        public float step(Object key, boolean hovered, float dt, float rate) {
            float[] cell = v.computeIfAbsent(key, k -> new float[]{0f, 0f});
            float target = hovered ? 1f : 0f;
            float k = 1f - (float) Math.pow(1.0 - clamp01(rate), Math.max(0f, dt));
            cell[0] += (target - cell[0]) * k;
            if (Math.abs(target - cell[0]) < 0.004f) cell[0] = target;
            cell[1] = frame;
            return cell[0];
        }

        public float get(Object key) {
            float[] cell = v.get(key);
            return cell == null ? 0f : cell[0];
        }

        /** Call once per frame after drawing: forgets the keys nobody asked about in the last 60 frames. */
        public void endFrame() {
            frame++;
            if (frame % 60 == 0) v.values().removeIf(c -> frame - c[1] > 60 && c[0] <= 0.0f);
        }

        public int size() { return v.size(); }

        public void clear() { v.clear(); }
    }

    /**
     * The module's slide. {@code shown()} is the page whose controls are drawn; {@code offset()} is how far the module is pushed out of its place, as a
     * fraction of its own width plus margin (0 = in place, -1 = fully off to the left). Asking for another page makes the shown one slide out first and only
     * then does the shown page change and the new one slide in, so the screen never has two pages' controls at once, and clicks are only accepted when
     * {@code settled()}.
     */
    public static final class ModuleSlide {
        public static final float OUT_TICKS = 7f, IN_TICKS = 11f;

        private int shown, wanted;
        private boolean leaving;
        private final Glide off = new Glide(0f);

        public ModuleSlide(int page) { shown = page; wanted = page; }

        public int shown() { return shown; }

        public int wanted() { return wanted; }

        /** The left offset of the module, -1..0. */
        public float offset() { return off.value(); }

        public boolean settled() { return !leaving && shown == wanted && off.done(); }

        public void request(int page) {
            wanted = page;
            if (wanted == shown) {
                if (leaving) { leaving = false; off.to(0f, IN_TICKS); }          // changed our mind mid-exit: come back
                return;
            }
            if (!leaving) { leaving = true; off.to(-1f, OUT_TICKS); }
        }

        public void tick(float dt) {
            off.tick(dt);
            if (leaving && off.done()) {
                shown = wanted;
                leaving = false;
                off.set(-1f);
                off.to(0f, IN_TICKS);
            }
        }
    }
}
