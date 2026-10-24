package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.perf.PerfMath;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * The plane of the Mana Method rune array (pure: JOML and plain numbers, no Minecraft class, so it runs on a server and in a unit test). The rune is a flat figure in a plane, and the plane
 * always faces the caster:
 * <ol>
 *   <li>{@link Params}: radius (blocks), complexity (1..{@value #MAX_COMPLEXITY}: how many floating glyphs and how big the figure is) and the colour and sparks; {@link #paramsFor} gives the game's
 *       look of a spell (bigger with more modifiers, one more step for True Magic).</li>
 *   <li>{@link #faceQuaternion}: a quaternion that turns the plane so its normal points back at the caster (minus the look vector) with no roll, so the symbols stay upright.</li>
 *   <li>{@link #frame}: the plane's two axes and its normal in the world; the figure's points are then {@code centre + right * x + up * y} (a few multiplications, no quaternion per point).</li>
 * </ol>
 * What is drawn in that plane is {@link RuneGlyphLayout}: runes written stroke by stroke ({@link RuneAlphabet}), rings of them, circle lines, lightning, the open grimoire. (The older dot-ring figure
 * and its particle budget lived here and were replaced by the writing: see docs/magic/mana_method.md.)
 */
public final class RuneGeneratorUtil {
    private RuneGeneratorUtil() {}

    public static final int MIN_COMPLEXITY = 1, MAX_COMPLEXITY = 5;
    /** At most this many runes are drawn at once, one open grimoire each. */
    public static final int MAX_RUNES_DRAWN = 6;
    /** A rune farther than this from the camera is not drawn at all; within {@link #LOD_NEAR} it is at full detail. The open grimoire is drawn within {@link #BOOK_DISTANCE} only. */
    public static final double CULL_DISTANCE = 64.0, LOD_NEAR = 24.0, BOOK_DISTANCE = 32.0;

    /** What a rune looks like: radius in blocks, complexity (the floating glyphs; the figure's size), the particle of the sparks and of the glyph sparks (catalog ids), the colour. */
    public record Params(double radius, int complexity, String ringParticle, String glyphParticle, int tint) {
        public Params {
            radius = Double.isNaN(radius) ? 1.0 : Math.max(0.3, Math.min(4.0, radius));
            complexity = Math.max(MIN_COMPLEXITY, Math.min(MAX_COMPLEXITY, complexity));
            ringParticle = ringParticle == null ? RuneElement.RING_PARTICLE : ringParticle;
            glyphParticle = glyphParticle == null ? RuneElement.FIRE.glyph : glyphParticle;
        }
    }

    /** The general entry: a radius, a complexity and a particle for the sparks. */
    public static Params of(double radius, int complexity, String glyphParticle, int tint) {
        return new Params(radius, complexity, RuneElement.RING_PARTICLE, glyphParticle, tint);
    }

    /** The look of a spell's rune: bigger with more modifiers, one more step for True Magic. */
    public static Params paramsFor(RuneSpec s) {
        int mods = RuneMods.count(s.mods());
        int complexity = 2 + mods + (s.trueMagic() ? 1 : 0);
        double radius = 1.15 + 0.2 * mods + (s.trueMagic() ? 0.25 : 0.0);
        return of(radius, complexity, s.element().glyph, s.element().tint);
    }

    // ---------------------------------------------------------------- facing the caster
    /**
     * The rotation that takes the plane (normal +z, up +y) to a plane whose normal is {@code -look} (it faces the caster who looks along {@code look}), turned by yaw about the
     * world's vertical axis and then pitch about the plane's own x axis: no roll, so the plane's +x stays horizontal and the symbols stay upright. A zero or NaN look gives the
     * identity facing (+z).
     */
    public static Quaterniond faceQuaternion(double lx, double ly, double lz) {
        double len = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (!(len > 1e-9)) return new Quaterniond();
        double nx = -lx / len, ny = -ly / len, nz = -lz / len;                      // the plane's normal: back toward the caster
        double pitch = -Math.asin(Math.max(-1.0, Math.min(1.0, ny)));
        double yaw = Math.atan2(nx, nz);
        return new Quaterniond().rotateY(yaw).rotateX(pitch);
    }

    /** The plane in the world: its centre, its x axis, its y axis and its normal (unit vectors). */
    public record Frame(double cx, double cy, double cz, double ux, double uy, double uz, double vx, double vy, double vz, double nx, double ny, double nz) {}

    /** The world frame of a plane rotated by {@code q} and centred at (cx, cy, cz). */
    public static Frame frame(Quaterniond q, double cx, double cy, double cz) {
        Vector3d u = q.transform(new Vector3d(1, 0, 0)), v = q.transform(new Vector3d(0, 1, 0)), n = q.transform(new Vector3d(0, 0, 1));
        return new Frame(cx, cy, cz, u.x, u.y, u.z, v.x, v.y, v.z, n.x, n.y, n.z);
    }

    /** The frame of a rune at (cx, cy, cz) that faces a caster looking along (lx, ly, lz). */
    public static Frame frameFacing(double cx, double cy, double cz, double lx, double ly, double lz) {
        return frame(faceQuaternion(lx, ly, lz), cx, cy, cz);
    }

    /** The detail of a rune at this squared distance from the camera: 0 beyond {@link #CULL_DISTANCE}, 1 within {@link #LOD_NEAR} (stepped, see {@link PerfMath#lodDetailSq}). */
    public static float distanceDetailSq(double distSq, boolean own) {
        return own ? 1f : PerfMath.lodDetailSq(distSq, LOD_NEAR, CULL_DISTANCE);
    }
}
