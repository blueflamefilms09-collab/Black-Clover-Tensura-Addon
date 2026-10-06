package com.newuniverse.nusmp.vfx.fx;

/** Named easing curves. Every entrance, exit and motion uses one of these. */
public enum Ease {
    LINEAR, OUT_QUAD, IN_QUAD, OUT_CUBIC, IN_CUBIC, IN_OUT_SINE, OUT_BACK, OUT_EXPO;

    public double at(double t) {
        t = Math.max(0, Math.min(1, t));
        return switch (this) {
            case LINEAR -> t;
            case OUT_QUAD -> 1 - (1 - t) * (1 - t);
            case IN_QUAD -> t * t;
            case OUT_CUBIC -> 1 - Math.pow(1 - t, 3);
            case IN_CUBIC -> t * t * t;
            case IN_OUT_SINE -> -(Math.cos(Math.PI * t) - 1) / 2;
            case OUT_BACK -> { double c = 1.70158, u = t - 1; yield 1 + (c + 1) * u * u * u + c * u * u; }
            case OUT_EXPO -> t >= 1 ? 1 : 1 - Math.pow(2, -10 * t);
        };
    }
}
