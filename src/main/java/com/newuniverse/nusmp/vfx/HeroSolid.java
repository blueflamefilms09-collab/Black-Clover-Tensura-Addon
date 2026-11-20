package com.newuniverse.nusmp.vfx;

/**
 * Why (docs/heroweapon/flat_parts.md): the arrows of the Legendary Bow and the tips of the spears and the harpoon were drawn flat: a hairline nocked on the bow, a paper-thin leaf for a spear head, and a flat glow
 * streak for the arrow or the spear tip that flies. They are solid now: the in-hand meshes were rebuilt (tools/heroweapon/solidify_flat_parts.py), and the flying arrow / spear tip (shapes {@code HERO_ARROW},
 * {@code HERO_SPEAR}, and the thrust of the hero weapon signature) is a small shaded 3D model ({@code SolidModels.arrow / spearTip}) on a glow trail.
 *
 * <p>This is the pure half (no Minecraft class, so it runs in the unit tests and on a dedicated server): the switch {@code visual.heroSolidParts} (CLIENT config in {@code nusmp-flatprops-client.toml}, default ON;
 * off restores the old flat look: the old meshes in {@code weapon_meshes/flat_parts_old} and the plain glow streak), the cost caps, and the aim maths. The dedicated server never reads the switch (it draws nothing)
 * and no packet is added: the server sends the same single effect packet per arrow as before, with a different shape id.
 *
 * <p>Worst case per client: {@link #MAX_PER_FRAME} solid arrows / tips a frame, each at most {@link #MAX_QUADS_EACH} quads; one further than 40 blocks is not built; the rest of a crowded frame keeps the glow trail.
 */
public final class HeroSolid {
    private HeroSolid() {}

    /** At most this many solid arrows or spear tips are built in one frame of one client (the rest draw their glow trail only). */
    public static final int MAX_PER_FRAME = 16;
    /** Every solid model stays under this many quads (4 vertices each) once the faces turned away from the camera are dropped. */
    public static final int MAX_QUADS_EACH = 48;
    /** The vertex budget of one HERO_ARROW / HERO_SPEAR effect: the model (at most 4 x MAX_QUADS_EACH) + the trail and sparks, inside the 400 of an activation. */
    public static final int VERTEX_BUDGET = 320;

    private static volatile boolean solid = true;

    /** True unless the player turned {@code visual.heroSolidParts} off: the solid arrows and tips are drawn. Default true. */
    public static boolean solid() { return solid; }

    /** Written by the client config only. */
    public static void setSolid(boolean on) { solid = on; }

    private static int lastFrame;
    private static int used;

    /**
     * One slot of the per-frame budget. {@code frame} is any object that is new each frame (the render context); the count starts again when it changes. Render thread only.
     * False when the frame already built {@link #MAX_PER_FRAME} models.
     */
    public static boolean takeSlot(Object frame) {
        int id = System.identityHashCode(frame);
        if (id != lastFrame) {
            lastFrame = id;
            used = 0;
        }
        if (used >= MAX_PER_FRAME) return false;
        used++;
        return true;
    }

    /**
     * The yaw and tilt that point the local +y axis of a {@code SolidParts} model along (dx, dy, dz): the model's basis is Ry(yaw) * Rz(tilt), which sends (0, 1, 0) to
     * (-sin(tilt) cos(yaw), cos(tilt), sin(tilt) sin(yaw)). Returns {yaw, tilt} in radians; a vertical direction gives yaw 0.
     */
    public static float[] aim(double dx, double dy, double dz) {
        double h = Math.sqrt(dx * dx + dz * dz);
        float tilt = (float) Math.atan2(h, dy);
        float yaw = h < 1.0E-9 ? 0f : (float) Math.atan2(dz, -dx);
        return new float[]{yaw, tilt};
    }

    /** The direction the model's +y axis points along for a yaw and tilt (the inverse of {@link #aim}); for the tests. */
    public static double[] axis(float yaw, float tilt) {
        return new double[]{-Math.sin(tilt) * Math.cos(yaw), Math.cos(tilt), Math.sin(tilt) * Math.sin(yaw)};
    }

    /** How far along its path a flying arrow / tip is after {@code age} ticks of an effect that lasts {@code duration}: it lands after 70 % of the time and stays put (fading) after that. */
    public static float flight(float age, float duration) {
        float f = age / Math.max(1f, duration * 0.7f);
        return f < 0f ? 0f : Math.min(f, 1f);
    }
}
