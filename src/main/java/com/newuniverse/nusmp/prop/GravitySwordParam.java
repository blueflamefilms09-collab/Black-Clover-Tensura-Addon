package com.newuniverse.nusmp.prop;

/**
 * 1.93.0 Gravity Magic: the param of the compressed rock sword prop (PropKind.GRAVITY_2), pure so the server behaviour, the spell and the painter share one
 * definition (and the preview can compile it): bits 0 to 7 = the stage (0 forming, 1 hovering, 2 launched), the rest = the pitch in degrees plus 90.
 */
public final class GravitySwordParam {
    private GravitySwordParam() {}

    public static final int FORMING = 0, HOVER = 1, LAUNCHED = 2;

    public static int of(int stage, float pitchDeg) { return (stage & 255) | ((Math.round(Math.max(-90f, Math.min(90f, pitchDeg))) + 90) << 8); }
    public static int stageOf(int param) { return param & 255; }
    public static float pitchOf(int param) { return (param >> 8) - 90; }
}
