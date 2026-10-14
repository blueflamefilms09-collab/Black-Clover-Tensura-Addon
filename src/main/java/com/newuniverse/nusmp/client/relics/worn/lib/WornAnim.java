package com.newuniverse.nusmp.client.relics.worn.lib;

/**
 * The motion of an animation group of a worn model: pure arithmetic from the wearer's clock, so it is tested and the draw loop allocates nothing (one reusable
 * {@link Xf} is filled per group). The transform is "move to the pivot, translate by (tx, ty, tz), turn by (rx, ry, rz) radians, scale by s, move back".
 * Numbers per kind (a, b, c as written in the model file):
 * <ul>
 *   <li>BOB: up and down by {@code a} pixels at {@code b} radians a tick, phase {@code c}.</li>
 *   <li>SPIN, ORBIT: turn about the vertical axis through the pivot, {@code a} degrees a tick (an orbit's pivot is the centre of the circle it moves on).</li>
 *   <li>SWING: tilt about the sideways axis: {@code a} degrees at full stride (the hem trails behind a runner) plus {@code b} degrees of slow idle breathing.</li>
 *   <li>SWAY: rock about the front axis, {@code a} degrees at {@code b} radians a tick, phase {@code c}.</li>
 *   <li>PULSE: grow and shrink by the fraction {@code a} at {@code b} radians a tick, phase {@code c}.</li>
 *   <li>WAVE: no transform; the draw loop bends each vertex sideways ({@link #waveOffset}).</li>
 * </ul>
 */
public final class WornAnim {
    private WornAnim() {}

    /** A reusable transform. */
    public static final class Xf {
        public float tx, ty, tz, rx, ry, rz, s = 1f;

        void reset() { tx = ty = tz = rx = ry = rz = 0f; s = 1f; }
    }

    private static final float DEG = (float) (Math.PI / 180.0);

    /** Fills {@code out} with the transform of a group at the wearer's clock {@code age} (ticks, with the partial tick) and stride {@code limb} (0 standing .. 1 running). */
    public static void eval(WornMesh.Group g, float age, float limb, boolean crouching, Xf out) {
        out.reset();
        if (g == null) return;
        switch (g.kind()) {
            case BOB -> out.ty = g.a() * (float) Math.sin(age * g.b() + g.c());
            case SPIN, ORBIT -> out.ry = age * g.a() * DEG;
            case SWING -> out.rx = (g.a() * Math.min(1f, Math.max(0f, limb)) + g.b() * (float) Math.sin(age * 0.09f) + (crouching ? 4f : 0f)) * DEG;
            case SWAY -> out.rz = g.a() * (float) Math.sin(age * g.b() + g.c()) * DEG;
            case PULSE -> out.s = 1f + g.a() * (float) Math.sin(age * g.b() + g.c());
            default -> { }
        }
    }

    /** How far a WAVE group bends the vertex at height {@code y} (layer pixels) sideways at the clock {@code age}: nothing at the pivot, full 12 pixels below it. */
    public static float waveOffset(WornMesh.Group g, float y, float age) {
        float w = Math.min(1f, Math.abs(y - g.py()) / 12f);
        return g.a() * w * (float) Math.sin(age * g.b() + y * g.c());
    }
}
