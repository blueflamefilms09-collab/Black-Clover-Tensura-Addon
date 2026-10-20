package com.newuniverse.nusmp.visual.genesis;

import java.util.List;

/**
 * The pose of one animated group at one moment: a rotation (3x3, row major), an offset, a uniform scale and an alpha, all about the group's pivot. One instance is reused for
 * every group of every frame ({@link #eval} fills it, nothing is allocated). Pure maths, no Minecraft classes, tested without the game.
 */
public final class VuPose {
    /** Row-major rotation matrix. */
    public final float[] r = new float[9];
    public float ox, oy, oz;
    public float scale = 1f;
    public float alpha = 1f;
    private final float[] tmp = new float[9];

    public VuPose reset() {
        r[0] = 1f; r[1] = 0f; r[2] = 0f;
        r[3] = 0f; r[4] = 1f; r[5] = 0f;
        r[6] = 0f; r[7] = 0f; r[8] = 1f;
        ox = oy = oz = 0f;
        scale = 1f;
        alpha = 1f;
        return this;
    }

    /** True when the pose is the identity (nothing to apply). */
    public boolean identity() {
        return scale == 1f && ox == 0f && oy == 0f && oz == 0f && r[0] == 1f && r[4] == 1f && r[8] == 1f && r[1] == 0f && r[2] == 0f && r[3] == 0f && r[5] == 0f && r[6] == 0f && r[7] == 0f;
    }

    /** Multiplies the rotation by a turn of {@code deg} degrees about the unit axis (ax, ay, az): r = turn * r. */
    public void rotate(float ax, float ay, float az, float deg) {
        double a = Math.toRadians(deg);
        float c = (float) Math.cos(a), s = (float) Math.sin(a), t = 1f - c;
        float m0 = t * ax * ax + c, m1 = t * ax * ay - s * az, m2 = t * ax * az + s * ay;
        float m3 = t * ax * ay + s * az, m4 = t * ay * ay + c, m5 = t * ay * az - s * ax;
        float m6 = t * ax * az - s * ay, m7 = t * ay * az + s * ax, m8 = t * az * az + c;
        for (int col = 0; col < 3; col++) {
            tmp[col] = m0 * r[col] + m1 * r[3 + col] + m2 * r[6 + col];
            tmp[3 + col] = m3 * r[col] + m4 * r[3 + col] + m5 * r[6 + col];
            tmp[6 + col] = m6 * r[col] + m7 * r[3 + col] + m8 * r[6 + col];
        }
        System.arraycopy(tmp, 0, r, 0, 9);
    }

    /**
     * Fills the pose of a group at {@code ticks} (game ticks with the partial tick), {@code drawAge} ticks after the weapon was drawn (a huge number once the draw is over).
     * spin: turns about the axis at dps degrees a second; sway: swings deg degrees each way; bob: moves amp cm along the axis; pulse: scales by 1 + amp; breathe: alpha between min
     * and max; flicker: alpha jumps between min and max `rate` times a second (smoothed, deterministic).
     */
    public VuPose eval(VuSpec.Group g, float ticks, float drawAge) {
        reset();
        for (int i = 0, n = g.anim.size(); i < n; i++) {
            VuSpec.Anim a = g.anim.get(i);
            switch (a.kind) {
                case "spin" -> rotate(a.ax, a.ay, a.az, a.dps * ticks / 20f + a.phase);
                case "sway" -> rotate(a.ax, a.ay, a.az, a.deg * VuRules.sin01(ticks / a.period + a.phase));
                case "bob" -> {
                    float d = a.amp * VuRules.sin01(ticks / a.period + a.phase);
                    ox += a.ax * d;
                    oy += a.ay * d;
                    oz += a.az * d;
                }
                case "pulse" -> scale *= 1f + a.amp * VuRules.sin01(ticks / a.period + a.phase);
                case "breathe" -> alpha *= a.min + (a.max - a.min) * (0.5f + 0.5f * VuRules.sin01(ticks / a.period + a.phase));
                case "flicker" -> alpha *= VuRules.flicker(ticks, a.rate, a.min, a.max, i * 7919 + g.name.hashCode());
                default -> { }
            }
        }
        if (g.draw != null) {
            float p = VuRules.drawProgress(drawAge - g.draw.delay(), g.draw.ticks());
            float e = VuRules.easeOutBack(p);
            scale *= g.draw.fromScale() + (1f - g.draw.fromScale()) * e;
            alpha *= p;
        }
        return this;
    }
}
