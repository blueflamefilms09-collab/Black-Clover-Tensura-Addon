package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Hercules, Lord of Martial Prowess (Veldanava): the strongest fighter, drawn as A BLOW THAT WEIGHS A MOUNTAIN. Burnished bronze, bright gold, warm ivory / marble white, ember orange and a little deep crimson, with white-gold
 * flashes. Motifs: the clenched FIST and the LAUREL wreath (the owner-approved icon), shock rings that run over the ground, cracks that glow from inside, slabs and chips of rock thrown up, dust, speed lines, heavy bronze
 * chains, a web of cracks in the air like broken glass and overlapping bronze plates. The rhythm is slow-then-violent: a long gather, a short sharp blow, an impact frame that holds still for a moment (hit-stop), then
 * the shock. It shares nothing with the cold white and red judgement of {@code JudgmentLayer} (Sandalphon), the violet maze of {@link TrialLayer} (Israfil), the white and blue wings and feathers of {@link JibrilLayer} or the
 * indigo and gold runes of {@link SolomonLayer}, and never touches the Time Magic layer. Written from scratch for this mod (textures: {@code tools/gen_hercules_vfx_textures.py} ->
 * {@code textures/particle/hercules_*.png}, the metal and stone are lit in the texture, the ring, wave, streak, flash, flame and mote are light the layer tints). The colour of the packet is ignored.
 * <ul>
 *   <li>HERCULES_LEAP: Earth-Shattering Tremor take-off. Sent at the caster's feet: from = its feet, to = the point the leap is aimed at (up to 24 blocks ahead, the same as from for a straight jump), power = 1, 16 ticks. A
 *       crater press with cracks, a ring of dust and bronze shards blown out of the ground, a short upward shock column, speed lines that trail the rising body (leaning to {@code to}).</li>
 *   <li>HERCULES_TREMOR: Earth-Shattering Tremor landing. Sent at the ground point: from = that point, power = radius (14 in game, the picture runs from 4 to 24 blocks), 44 ticks. An impact flash, a crater press, three shock
 *       rings run out to the radius with a trailing wave and a bronze dust sheet, cracks that radiate and glow gold then cool to ember, slabs of rock lifted and falling back, a column of dust and light, embers; camera shake.</li>
 *   <li>HERCULES_INVICTUS: Invictus. Sent FOLLOWING the caster and re-sent every 80 ticks: from = its centre, power = size (~1.6), 100 ticks (fades in and out over 20 ticks so a re-send cross-fades: the two overlap for 20
 *       ticks and add up to one). A laurel wreath with a fist in it burns behind the body, a laurel crown turns over the head, tongues of bronze and gold flame stand along the body, a laurel ring on the floor, embers and leaves rise.</li>
 *   <li>HERCULES_GRIP: Titan's Grip. Sent at the caster: from = its centre, to = the centre of the pulled creature (1-40 blocks), power = 1, 24 ticks. A huge bronze hand opens round the target, closes into a fist on it (a flash,
 *       a web of cracks), heavy gold chains run from it to the caster and pull, speed lines run along the line.</li>
 *   <li>HERCULES_THROW: Devastating Throw. Sent at the thrown creature or the lifted block: from = where it is hurled from, to = the point it lands (up to 40 blocks, on the ground), power = 1, 30 ticks. A burst at the
 *       throw, a hurled streak with a cone of shock and torn-up dust along an arc, a heavy burst of rock, cracks and light where it lands; camera shake at the landing.</li>
 *   <li>HERCULES_ROAR: Battle Roar. Sent at the caster: from = its centre, power = radius (30 in game, 6 to 30 drawn), 36 ticks. A breath in, then three thick rings run out over the ground to the radius under a layered bronze
 *       dome, laurel leaves torn from the caster and blown outward, rays, dust at the front, the ground ringing; camera shake.</li>
 *   <li>HERCULES_STRIKE: World-Breaking Strike. Sent at the caster: from = its centre, to = the point of the punch, 4-8 blocks ahead, power = 1, 44 ticks. The body winds up into one giant fist of bronze and gold, the fist
 *       travels in two ticks and the picture holds still on the impact (a white-gold impact frame), then the air cracks like glass (a web in a disc facing the caster), shards of space fly out, shock rings and radial lines run
 *       out, the ground shakes; camera shake.</li>
 *   <li>HERCULES_SHOCKWAVE: God of Strength. Sent at the point a heavy blow lands (at the ground): from = that point, power = radius (3 in game), 12 ticks. A flat ring of dust and light with a few stones thrown up. Cheap.</li>
 *   <li>HERCULES_BODY: Adamantine Body turned a blow aside. Sent at the wearer: from = its centre, power = size (~1.5), 12 ticks. Bronze plates flash on the body and ring out, with sparks.</li>
 *   <li>HERCULES_MOMENTUM: Unstoppable Momentum grew a stage. Sent FOLLOWING the runner: from = its centre, power = stage (1, 2 or 3), 40 ticks. Speed lines and a trail of bronze afterimages behind the body (along its
 *       facing; the line from {@code from} to {@code to} when it is not following), a wind ring and dust at the feet; the higher the stage, the more lines and afterimages.</li>
 * </ul>
 * <b>Budgets (rule 11).</b> The engine hard-caps every effect at 100 quads (400 vertices) per activation; INVICTUS asks for 50 quads (200 vertices) because it is re-sent every 80 ticks for a 100-tick life and two
 * instances overlap for 20 ticks. Counts are quads at full detail ({@code ctx.seg} thins them by distance and by the particle setting; minimums in the code); a glow counts as 2 quads, a ring or plane of any radius is one
 * quad. Worst case quads per effect per frame (measured in the preview at every tick, see tools/vfx_preview/scenes.d/hercules.json and docs/vfx_remake/hercules_fx_calls.md):
 * LEAP 33, TREMOR 50, INVICTUS 33 (two overlap for 20 ticks: 65), GRIP 41, THROW 64, ROAR 46, STRIKE 54, SHOCKWAVE 15, BODY 20, MOMENTUM 36. Each is one activation; no collections or arrays are built per frame (only the small
 * short-lived pose vectors every layer makes), every loop count is a constant or a {@code ctx.seg} of at most 16, and every optional group checks {@code hasBudget} first so the buffer can never be pushed past its cap.
 * Decorative extras (embers, leaves, dust) fall away with the camera distance (the manager already drops an effect that is out of range and thins the rest by its distance level of detail). No effect keeps anything between
 * frames, so nothing has to be cleaned up on logout, death or dimension change; the server sends one packet per effect (VfxSpawn) and nothing here runs on the server.
 */
public class HerculesLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation FIST = t("hercules_fist");
    public static final ResourceLocation HAND = t("hercules_hand");
    public static final ResourceLocation LAUREL = t("hercules_laurel");
    public static final ResourceLocation LEAF = t("hercules_leaf");
    public static final ResourceLocation RING = t("hercules_ring");
    public static final ResourceLocation WAVE = t("hercules_wave");
    public static final ResourceLocation CRACKS = t("hercules_cracks");
    public static final ResourceLocation WEB = t("hercules_web");
    public static final ResourceLocation DUST = t("hercules_dust");
    public static final ResourceLocation CHIPS = t("hercules_chips");
    public static final ResourceLocation STREAK = t("hercules_streak");
    public static final ResourceLocation CHAIN = t("hercules_chain");
    public static final ResourceLocation FLASH = t("hercules_flash");
    public static final ResourceLocation MOTE = t("hercules_mote");
    public static final ResourceLocation PLATES = t("hercules_plates");
    public static final ResourceLocation FLAME = t("hercules_flame");
    public static final ResourceLocation CRATER = t("hercules_crater");
    /** 3.20.0: the material atlas of the 3D models (bronze plates, marble, rock, laurel leaves; {@link HerculesModels}) and the ground fissure strip. */
    public static final ResourceLocation MODEL_ATLAS = t("hercules_model_atlas");
    public static final ResourceLocation FISSURE = t("hercules_model_fissure");

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GOLD = 0xFFFFD660;
    private static final int GOLD_L = 0xFFFFEEAA;
    private static final int IVORY = 0xFFFFF6DE;
    private static final int BRONZE = 0xFFD08A3C;
    private static final int EMBER = 0xFFFF7A2A;
    private static final int CRIMSON = 0xFFB4262C;
    private static final int DUST_TINT = 0xFFE6D2AE;

    /** Where the bright circle of the ring / the front of the wave / the reach of the cracks lies, as a fraction of the half size of its texture. */
    private static final float RING_R = 0.93f, WAVE_R = 0.95f, CRACK_R = 0.97f;
    /** The most the Tremor, Roar and the shock of a Shockwave draw out to (blocks): further is beyond the draw distance. */
    static final float TREMOR_MAX = 24f, ROAR_MAX = 30f;
    /** Ticks: the gather, the time the fist is in the air and the stop on the impact of the World-Breaking Strike. */
    static final int STRIKE_IMPACT = 18, STRIKE_HOLD = 3;
    /** Ticks the thrown creature takes to cross: 6 + 0.2 per block, 6..14, after the 2 ticks of the throw itself. */
    static final int THROW_START = 2;

    /** 3.20.0: the models are drawn within these distances (blocks) of the camera, further out the flat sprites of 3.19.0 stand in (they cost a quarter of the quads). */
    static final float MODEL_RANGE = 64f, CHUNK_RANGE = 44f, WREATH_RANGE = 36f;
    /** The tremor that is the aftershock (and any tremor this small) is the lighter, quieter variant: power (radius) at most this. */
    static final float AFTERSHOCK_MAX = 10f;
    /** Power of HERCULES_THROW that means "a chunk of ground is hurled" (the boulder is drawn); 1 = a creature is hurled (it flies itself). */
    static final float THROW_CHUNK_POWER = 2f;

    /** The poser / drawer of the 3D models (one per layer; render thread only). */
    private final HerculesMeshDraw mesh = new HerculesMeshDraw();

    /** Scratch corners of the quad being drawn (the layer draws on the render thread only, one effect at a time). */
    private final Vector3f q0 = new Vector3f(), q1 = new Vector3f(), q2 = new Vector3f(), q3 = new Vector3f();

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.HERCULES_LEAP, VfxShape.HERCULES_TREMOR, VfxShape.HERCULES_INVICTUS, VfxShape.HERCULES_GRIP, VfxShape.HERCULES_THROW, VfxShape.HERCULES_ROAR, VfxShape.HERCULES_STRIKE,
                VfxShape.HERCULES_SHOCKWAVE, VfxShape.HERCULES_BODY, VfxShape.HERCULES_MOMENTUM);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case HERCULES_LEAP -> 16;
            case HERCULES_TREMOR -> 44;
            case HERCULES_GRIP -> 24;
            case HERCULES_THROW -> 30;
            case HERCULES_ROAR -> 36;
            case HERCULES_STRIKE -> 44;
            case HERCULES_SHOCKWAVE -> 12;
            case HERCULES_BODY -> 12;
            case HERCULES_MOMENTUM -> 40;
            default -> 100;
        };
    }

    @Override public int defaultColor(VfxShape s) { return GOLD; }

    /** The long-running, re-sent Invictus is drawn with a cap of its own: 50 quads (two overlap for 20 ticks: 100 at most); the others use the usual 100 quads. */
    @Override
    public int vertexBudget(VfxShape s) {
        return switch (s) {
            case HERCULES_INVICTUS -> 200;
            default -> VfxVertexBuffer.MAX_VERTICES_PER_ACTIVATION;
        };
    }

    /** The landing tick of a throw over {@code dist} blocks (the flight starts after {@link #THROW_START} ticks). */
    static int throwLanding(double dist) {
        return THROW_START + Math.round(Mth.clamp(6f + (float) dist * 0.2f, 6f, 14f));
    }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.HERCULES_TREMOR) VfxShake.add(inst.payload.from(), Mth.clamp(inst.power / 14f * 1.7f, 0.8f, 2.6f), 18);
    }

    @Override
    public void onTick(VfxInstance inst) {
        // camera shake on the heavy beats (VfxShake falls off with the distance and keeps the strongest)
        switch (inst.shape) {
            case HERCULES_STRIKE -> { if (inst.age() == STRIKE_IMPACT) VfxShake.add(inst.payload.to(), 2.4f, 22); }
            case HERCULES_ROAR -> { if (inst.age() == 6) VfxShake.add(inst.payload.from(), 1.5f, 24); }
            case HERCULES_THROW -> { if (inst.age() == throwLanding(inst.payload.from().distanceTo(inst.payload.to()))) VfxShake.add(inst.payload.to(), 1.3f, 12); }
            default -> { }
        }
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case HERCULES_LEAP -> leap(inst, ctx, buf);
            case HERCULES_TREMOR -> tremor(inst, ctx, buf);
            case HERCULES_INVICTUS -> invictus(inst, ctx, buf);
            case HERCULES_GRIP -> grip(inst, ctx, buf);
            case HERCULES_THROW -> hurl(inst, ctx, buf);
            case HERCULES_ROAR -> roar(inst, ctx, buf);
            case HERCULES_STRIKE -> strike(inst, ctx, buf);
            case HERCULES_SHOCKWAVE -> shockwave(inst, ctx, buf);
            case HERCULES_BODY -> body(inst, ctx, buf);
            case HERCULES_MOMENTUM -> momentum(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ small maths
    /** 1 while running, ramping 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    /** A repeatable hash of (seed, i, k) in 0..1 (no allocation, same on every client). */
    static float h(long seed, int i, int k) {
        long z = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL + 0x2545F4914F6CDD1DL;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 40) / (float) (1 << 24);
    }

    private static float smooth(float e0, float e1, float x) {
        float u = Mth.clamp((x - e0) / (e1 - e0), 0, 1);
        return u * u * (3 - 2 * u);
    }

    private static float eo(float x) { return VfxAnim.easeOutCubic(Mth.clamp(x, 0, 1)); }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }

    private static int mix(int c0, int c1, float t) { return VfxVertexBuffer.lerpColor(c0, c1, Mth.clamp(t, 0f, 1f)); }

    private static float horizontal(Vector3f v) { return Mth.sqrt(v.x * v.x + v.z * v.z); }

    /** The age with the picture held still for {@code len} ticks from {@code at} (the hit-stop of a heavy blow). */
    private static float hold(float age, float at, float len) {
        if (age <= at) return age;
        if (age < at + len) return at;
        return age - len;
    }

    /** Cracks cool as they age: white-gold, gold, ember, crimson (u 0..1). */
    private static int heat(float u) {
        return u < 0.4f ? mix(GOLD_L, GOLD, u / 0.4f) : u < 0.75f ? mix(GOLD, EMBER, (u - 0.4f) / 0.35f) : mix(EMBER, CRIMSON, (u - 0.75f) / 0.25f);
    }

    // ------------------------------------------------------------------ primitives
    private void quad(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend,
                      float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                      float u0, float v0, float u1, float v1, int c0, int c1) {
        q0.set(ax, ay, az);
        q1.set(bx, by, bz);
        q2.set(cx, cy, cz);
        q3.set(dx, dy, dz);
        buf.quad(tex, blend, q0, q1, q2, q3, u0, v0, u1, v1, c0, c1);
    }

    /**
     * A ribbon from a to b that turns its face to the camera (the camera is at the origin: positions are camera-relative). Half widths hwA / hwB; the texture runs V = vA at a to V = vB at b (U across); cA / cB are the
     * vertex colours at the two ends (the alpha fades along the ribbon).
     */
    private void ribbon(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float ax, float ay, float az, float bx, float by, float bz,
                        float hwA, float hwB, float vA, float vB, int cA, int cB) {
        if (((cA >>> 24) | (cB >>> 24)) < 3) return;
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        float mx = (ax + bx) * 0.5f, my = (ay + by) * 0.5f, mz = (az + bz) * 0.5f;
        float ml = Mth.sqrt(mx * mx + my * my + mz * mz);
        float tx = 0, ty = 0, tz = 1;
        if (ml > 1e-4f) {
            tx = -mx / ml;
            ty = -my / ml;
            tz = -mz / ml;
        }
        float sx = dy * tz - dz * ty, sy = dz * tx - dx * tz, sz = dx * ty - dy * tx;
        float sl = Mth.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1e-5f) return;
        sx /= sl;
        sy /= sl;
        sz /= sl;
        quad(buf, tex, blend,
                ax - sx * hwA, ay - sy * hwA, az - sz * hwA, ax + sx * hwA, ay + sy * hwA, az + sz * hwA,
                bx + sx * hwB, by + sy * hwB, bz + sz * hwB, bx - sx * hwB, by - sy * hwB, bz - sz * hwB,
                0, vB, 1, vA, cA, cB);
    }

    /** A light streak from tail (a) to head (b): the STREAK texture runs faint to bright, drawn additively. */
    private void streak(VfxVertexBuffer buf, float ax, float ay, float az, float bx, float by, float bz, float hw, int argb) {
        ribbon(buf, STREAK, VfxBlend.ADD, ax, ay, az, bx, by, bz, hw * 0.35f, hw, 0f, 1f, a(argb, 0f), argb);
    }

    private void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float x, float y, float z, float size, float rot, int argb) {
        if (size <= 0.001f || (argb >>> 24) < 3) return;
        q0.set(x, y, z);
        buf.billboard(ctx, tex, blend, q0, size, rot, argb);
    }

    /** One cell {@code idx} of a {@code cols x rows} atlas as a camera-facing sprite (a dust puff, a chip of rock). */
    private void cell(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, int cols, int rows, int idx, float x, float y, float z, float size, float rot, int argb) {
        if (size <= 0.001f || (argb >>> 24) < 3) return;
        float c = Mth.cos(rot) * size * 0.5f, s = Mth.sin(rot) * size * 0.5f;
        Vector3f r = ctx.camRight, u = ctx.camUp;
        float rxx = r.x * c + u.x * s, rxy = r.y * c + u.y * s, rxz = r.z * c + u.z * s;
        float ryx = u.x * c - r.x * s, ryy = u.y * c - r.y * s, ryz = u.z * c - r.z * s;
        float u0 = (idx % cols) / (float) cols, v0 = (idx / cols) / (float) rows;
        quad(buf, tex, blend,
                x - rxx - ryx, y - rxy - ryy, z - rxz - ryz,
                x + rxx - ryx, y + rxy - ryy, z + rxz - ryz,
                x + rxx + ryx, y + rxy + ryy, z + rxz + ryz,
                x - rxx + ryx, y - rxy + ryy, z - rxz + ryz,
                u0, v0, u0 + 1f / cols, v0 + 1f / rows, blend.grade(argb, 1f), blend.grade(argb, 1f));
    }

    private void dust(VfxRenderContext ctx, VfxVertexBuffer buf, int idx, float x, float y, float z, float size, float rot, float alpha) {
        cell(ctx, buf, DUST, VfxBlend.ALPHA, 2, 2, idx & 3, x, y, z, size, rot, a(DUST_TINT, alpha));
    }

    private void slab(VfxRenderContext ctx, VfxVertexBuffer buf, int idx, float x, float y, float z, float size, float rot, float alpha) {
        cell(ctx, buf, CHIPS, VfxBlend.ALPHA, 4, 2, idx & 3, x, y, z, size, rot, a(WHITE, alpha));
    }

    private void shard(VfxRenderContext ctx, VfxVertexBuffer buf, int idx, float x, float y, float z, float size, float rot, int argb) {
        cell(ctx, buf, CHIPS, VfxBlend.ADD, 4, 2, 4 + (idx & 3), x, y, z, size, rot, argb);
    }

    private void flash(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, float rot, int argb) { sprite(ctx, buf, FLASH, VfxBlend.ADD, x, y, z, size, rot, argb); }

    private void mote(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, int argb) { sprite(ctx, buf, MOTE, VfxBlend.ADD, x, y, z, size, 0f, argb); }

    private void glow(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, int argb, float intensity) {
        if (intensity <= 0.01f) return;
        q0.set(x, y, z);
        VfxBloom.glow(ctx, buf, q0, size, argb, intensity);
    }

    private static VfxPose ground(float x, float y, float z) { return VfxPose.ground(new Vector3f(x, y, z)); }

    /** A pose at (x, y, z) whose plane faces along (dx, dy, dz): a ring or a web standing across a direction. */
    private static VfxPose across(float x, float y, float z, float dx, float dy, float dz) { return VfxPose.facing(new Vector3f(x, y, z), new Vector3f(dx, dy, dz)); }

    /** A pose at (x, y, z) that lies nearly flat but is tipped towards the camera by k (0 = flat): a crown over a head reads as a ring, not as an edge-on line. */
    private static VfxPose tilted(float x, float y, float z, float k) {
        float d = Mth.sqrt(x * x + z * z);
        float nx = 0f, nz = 0f;
        if (d > 1e-3f) {
            nx = -x / d * k;
            nz = -z / d * k;
        }
        return VfxPose.facing(new Vector3f(x, y, z), new Vector3f(nx, 1f, nz));
    }

    /** The shock ring whose bright band lies at {@code radius}. */
    private static void ringAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(RING, VfxBlend.ADD, pose, radius / RING_R, argb);
    }

    /** The shock wave whose front lies at {@code radius}, drawn additively (light) or alpha-blended (a sheet of dust). */
    private static void waveAt(VfxVertexBuffer buf, VfxBlend blend, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(WAVE, blend, pose, radius / WAVE_R, argb);
    }

    /** Cracks that reach {@code radius}: the dark, alpha-blended scar and the glow of its core (2 quads). */
    private static void cracksAt(VfxVertexBuffer buf, VfxPose pose, float radius, float scar, int glowArgb) {
        if (radius <= 0.05f) return;
        if (scar > 0.01f) buf.plane(CRACKS, VfxBlend.ALPHA, pose, radius / CRACK_R, a(WHITE, scar));
        if ((glowArgb >>> 24) >= 3) buf.plane(CRACKS, VfxBlend.ADD, pose.lift(0.012f), radius / CRACK_R, glowArgb);
    }

    /** The screen-plane angle that turns an upright icon so its top points along (dx, dy, dz) (a fist that punches that way). */
    private static float iconRot(VfxRenderContext ctx, float dx, float dy, float dz) {
        float px = dx * ctx.camRight.x + dy * ctx.camRight.y + dz * ctx.camRight.z;
        float py = dx * ctx.camUp.x + dy * ctx.camUp.y + dz * ctx.camUp.z;
        if (px * px + py * py < 0.04f) return 0f;
        return (float) Math.atan2(-px, py);
    }

    // ------------------------------------------------------------------ 3.20.0: the 3D models and the ground fissures
    /** True when the 3D models are drawn here: the setting is on, the particle setting is not minimal and the thing is within {@code range} blocks of the camera (else the 3.19.0 sprites stand in). */
    private static boolean modelsOn(VfxRenderContext ctx, float x, float y, float z, float range) {
        return HerculesModels.enabled() && ctx.detail >= 0.5f && x * x + y * y + z * z < range * range;
    }

    /** A chunk of rock (the 3D rock of the ground, at most 4 quads) tumbling about an axis; returns the quads drawn (0 = nothing, the caller may draw the old sprite). */
    private int chunk(VfxVertexBuffer buf, float x, float y, float z, float size, float spin, long seed, int i, float alpha, float heat) {
        if (alpha <= 0.03f || size <= 0.02f) return 0;
        mesh.posed().tumble(h(seed, i, 40) - 0.5f, 1f, h(seed, i, 41) - 0.5f, spin, size, size * 0.9f, size);
        return mesh.draw(buf, MODEL_ATLAS, HerculesModels.CHUNK, x, y, z, a(WHITE, alpha), heat);
    }

    /**
     * A ground fissure: a jagged strip of two quads lying on the ground from radius {@code r0} to {@code r1} out of (cx, gy, cz) along the angle {@code an}, {@code w} wide at the start and narrowing to a point, a dark
     * broken lip round a glowing core ({@code argb} tints it: white-gold when it opens, ember and crimson as it cools). Two quads each, no textures but the strip, nothing allocated.
     */
    private void fissure(VfxVertexBuffer buf, float cx, float gy, float cz, float an, float r0, float r1, float w, float jag, int argb) {
        if ((argb >>> 24) < 4 || r1 - r0 < 0.3f || !buf.hasBudget(8)) return;
        float ca = Mth.cos(an), sa = Mth.sin(an), rm = (r0 + r1) * 0.5f;
        float x0 = cx + ca * r0, z0 = cz + sa * r0;
        float x1 = cx + ca * rm - sa * jag, z1 = cz + sa * rm + ca * jag;
        float x2 = cx + ca * r1 + sa * jag * 0.6f, z2 = cz + sa * r1 - ca * jag * 0.6f;
        float d0x = x1 - x0, d0z = z1 - z0, l0 = Mth.sqrt(d0x * d0x + d0z * d0z) + 1e-4f;
        float d1x = x2 - x1, d1z = z2 - z1, l1 = Mth.sqrt(d1x * d1x + d1z * d1z) + 1e-4f;
        float p0x = -d0z / l0, p0z = d0x / l0, p1x = -d1z / l1, p1z = d1x / l1;
        float pmx = p0x + p1x, pmz = p0z + p1z, pml = Mth.sqrt(pmx * pmx + pmz * pmz) + 1e-4f;
        pmx /= pml;
        pmz /= pml;
        float w1 = w * 0.85f, w2 = w * 0.12f;
        quad(buf, FISSURE, VfxBlend.ALPHA, x0 - p0x * w, gy, z0 - p0z * w, x0 + p0x * w, gy, z0 + p0z * w, x1 + pmx * w1, gy, z1 + pmz * w1, x1 - pmx * w1, gy, z1 - pmz * w1, 0f, 0.45f, 1f, 1f, argb, argb);
        quad(buf, FISSURE, VfxBlend.ALPHA, x1 - pmx * w1, gy, z1 - pmz * w1, x1 + pmx * w1, gy, z1 + pmz * w1, x2 + p1x * w2, gy, z2 + p1z * w2, x2 - p1x * w2, gy, z2 - p1z * w2, 0f, 0f, 1f, 0.45f, argb, argb);
    }

    // ================================================================== HERCULES_LEAP
    private void leap(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 0, 3);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        Vector3f f = ctx.rel(inst.from(ctx));
        Vector3f e = ctx.rel(inst.to(ctx));
        float near = 1f - smooth(40f, 90f, horizontal(f));
        float gy = f.y + 0.04f;
        float dx = e.x - f.x, dz = e.z - f.z;
        float dl = Mth.sqrt(dx * dx + dz * dz);
        float lean = Mth.clamp(dl / 24f, 0f, 1f) * 0.8f;
        float nx = dl > 0.5f ? dx / dl : 0f, nz = dl > 0.5f ? dz / dl : 0f;
        VfxPose floor = ground(f.x, gy, f.z);

        // 1. the crater press and the cracks that the take-off leaves, the shock rings that run out of it
        buf.plane(CRATER, VfxBlend.ALPHA, floor.spin(h(seed, 0, 1) * Mth.TWO_PI), 2.1f, a(WHITE, 0.95f * smooth(0f, 1.5f, age) * (1f - smooth(10f, 16f, age))));
        cracksAt(buf, floor.lift(0.004f).spin(h(seed, 0, 2) * Mth.TWO_PI), 3.0f * eo(age / 5f), 0.9f * (1f - smooth(9f, 16f, age)), a(heat(age / 20f), 0.8f * (1f - smooth(8f, 16f, age))));
        float r1 = eo(age / 9f), r2 = eo((age - 2f) / 8f);
        ringAt(buf, floor.lift(0.02f), 0.6f + 3.4f * r1, a(GOLD, (1f - r1) * 0.95f * fade));
        ringAt(buf, floor.lift(0.025f), 0.4f + 2.3f * r2, a(IVORY, (1f - r2) * 0.8f * fade));
        waveAt(buf, VfxBlend.ADD, floor.lift(0.03f), 4.0f * r1, a(BRONZE, (1f - r1) * 0.7f * fade));

        // 2. the column: a short upward shock that stands over the feet, with a flash and a glow at the take-off
        float col = 1f - smooth(3f, 12f, age);
        if (col > 0.01f) {
            float hgt = 5.2f * eo(age / 4f);
            float lx = nx * lean * hgt * 0.5f, lz = nz * lean * hgt * 0.5f;
            ribbon(buf, STREAK, VfxBlend.ADD, f.x + lx, gy + hgt, f.z + lz, f.x, gy, f.z, 0.2f, 0.7f * col, 0f, 1f, a(GOLD, 0f), a(GOLD, 0.95f * col));
            ribbon(buf, STREAK, VfxBlend.ADD, f.x + lx, gy + hgt * 0.9f, f.z + lz, f.x, gy, f.z, 0.1f, 0.34f * col, 0f, 1f, a(IVORY, 0f), a(WHITE, col));
        }
        float fl = 1f - smooth(0f, 5f, age);
        if (fl > 0.01f) {
            flash(ctx, buf, f.x, gy + 0.5f, f.z, 4.6f * (0.5f + 0.5f * fl), 0.3f, a(IVORY, fl));
            glow(ctx, buf, f.x, gy + 0.4f, f.z, 2.2f * fl, EMBER, 0.9f * fl);
        }

        // 3. the dust that is blown out of the ground in a ring, and the bronze shards that go up with it
        int nd = ctx.seg(8, 4);
        for (int i = 0; i < nd && buf.hasBudget(4); i++) {
            float p = Mth.clamp(age / 15f - h(seed, i, 3) * 0.1f, 0f, 1f);
            float an = Mth.TWO_PI * (i + h(seed, i, 4) * 0.5f) / nd;
            float rr = 0.4f + (1.6f + 1.4f * h(seed, i, 5)) * eo(p);
            dust(ctx, buf, i, f.x + Mth.cos(an) * rr, gy + 0.2f + 0.8f * p * (0.6f + 0.6f * h(seed, i, 6)), f.z + Mth.sin(an) * rr, 0.9f + 1.5f * p, h(seed, i, 7) * 6f + age * 0.05f, 0.85f * (1f - p * p) * near * smooth(0f, 1f, age));
        }
        int nc = ctx.seg(6, 3);
        for (int i = 0; i < nc && buf.hasBudget(4); i++) {
            float tt = age - 0.5f;
            if (tt <= 0f) break;
            float an = Mth.TWO_PI * (i + h(seed, i, 8)) / nc;
            float sp = 0.10f + 0.12f * h(seed, i, 9), vy = 0.34f + 0.2f * h(seed, i, 10);
            float y = vy * tt - 0.02f * tt * tt;
            if (y < -0.05f) continue;
            slab(ctx, buf, i, f.x + Mth.cos(an) * (0.4f + sp * tt), gy + 0.15f + y, f.z + Mth.sin(an) * (0.4f + sp * tt), 0.35f + 0.35f * h(seed, i, 11), tt * (0.3f + h(seed, i, 12) * 0.4f) * (i % 2 == 0 ? 1 : -1), 1f - smooth(11f, 16f, age));
        }

        // 4. the speed lines that trail the rising body: streaks that shoot up round the axis, leaning to the aim point
        int ns = ctx.seg(8, 4);
        for (int i = 0; i < ns && buf.hasBudget(4); i++) {
            float p = (age - 1f - h(seed, i, 13) * 3f) / 9f;
            if (p <= 0f || p >= 1f) continue;
            float an = Mth.TWO_PI * i / ns + h(seed, i, 14);
            float rr = 0.25f + 0.55f * h(seed, i, 15);
            float head = 0.8f + 5.5f * eo(p), tail = Math.max(0f, head - (1.4f + 1.6f * h(seed, i, 16)));
            float ox = Mth.cos(an) * rr, oz = Mth.sin(an) * rr;
            float al = Mth.sin(p * Mth.PI) * fade;
            streak(buf, f.x + ox + nx * lean * tail * 0.5f, gy + tail, f.z + oz + nz * lean * tail * 0.5f, f.x + ox + nx * lean * head * 0.5f, gy + head, f.z + oz + nz * lean * head * 0.5f, 0.07f + 0.04f * h(seed, i, 17), a(i % 3 == 0 ? IVORY : GOLD, 0.95f * al));
        }
    }

    // ================================================================== HERCULES_TREMOR
    private void tremor(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 0, 8);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        float Rv = Mth.clamp(inst.power, 4f, TREMOR_MAX);
        Vector3f g = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(50f, 100f, horizontal(g));
        float gy = g.y + 0.04f;
        VfxPose floor = ground(g.x, gy, g.z);
        float t = Math.max(0f, age - 2f);                                     // the blow stops for two ticks on the ground, then everything runs
        float rot = h(seed, 0, 1) * Mth.TWO_PI;
        float k = Mth.clamp(Rv / 14f, 0.5f, 1.8f);

        // 1. the impact frame: a white-gold flash, then the crater press that stays
        float fl = 1f - smooth(0f, 5f, age);
        if (fl > 0.01f) {
            flash(ctx, buf, g.x, gy + 0.8f, g.z, (6f + 4f * k) * (0.6f + 0.4f * fl), 0.2f, a(WHITE, fl));
            glow(ctx, buf, g.x, gy + 0.6f, g.z, 3.2f * k * fl, GOLD, fl);
        }
        buf.plane(CRATER, VfxBlend.ALPHA, floor.spin(rot), Math.min(Rv * 0.36f, 5.2f), a(WHITE, 0.95f * smooth(0f, 1.5f, age) * (1f - smooth(30f, 44f, age))));

        // 2. the three shock rings with the wave behind the first and a bronze sheet of dust on the ground
        float p0 = eo(t / 20f), p1 = eo((t - 3f) / 20f), p2 = eo((t - 6f) / 20f);
        ringAt(buf, floor.lift(0.03f), Rv * p0, a(GOLD, (1f - p0 * p0) * 0.95f * fade));
        ringAt(buf, floor.lift(0.035f), Rv * p1, a(IVORY, (1f - p1 * p1) * 0.85f * fade));
        ringAt(buf, floor.lift(0.04f), Rv * p2, a(BRONZE, (1f - p2 * p2) * 0.8f * fade));
        waveAt(buf, VfxBlend.ADD, floor.lift(0.02f), Rv * p0 * 0.98f, a(GOLD, (1f - p0) * 0.7f * fade));
        waveAt(buf, VfxBlend.ALPHA, floor.lift(0.01f), Rv * p0 * 0.96f, a(DUST_TINT, (1f - p0) * 0.55f * fade));

        // 3. the cracks: two layers that run out fast, glow gold and cool down to ember and crimson
        float cr = eo(t / 9f);
        float cool = Mth.clamp(age / 44f, 0f, 1f);
        float glowA = (0.55f + 0.45f * smooth(0f, 4f, age)) * (1f - smooth(26f, 44f, age));
        float scar = 0.94f * (1f - smooth(30f, 44f, age));
        cracksAt(buf, floor.lift(0.005f).spin(rot), Rv * 0.94f * cr, scar, a(heat(cool), glowA));
        cracksAt(buf, floor.lift(0.008f).spin(rot + 1.9f), Rv * 0.6f * cr, scar * 0.8f, a(heat(Math.min(1f, cool + 0.12f)), glowA * 0.9f));

        // 4. the column of light and the column of dust over the crater
        float colA = 1f - smooth(2f, 12f, age);
        if (colA > 0.01f) {
            float hgt = (7f + 5f * k) * eo(age / 5f);
            ribbon(buf, STREAK, VfxBlend.ADD, g.x, gy + hgt, g.z, g.x, gy, g.z, 0.3f, 1.5f * k * colA, 0f, 1f, a(GOLD, 0f), a(GOLD, 0.95f * colA));
            ribbon(buf, STREAK, VfxBlend.ADD, g.x, gy + hgt * 0.85f, g.z, g.x, gy, g.z, 0.15f, 0.7f * k * colA, 0f, 1f, a(IVORY, 0f), a(WHITE, colA));
        }
        for (int j = 0; j < 6 && buf.hasBudget(4); j++) {
            float p = Mth.clamp((t - j * 0.5f) / 16f, 0f, 1f);
            if (p <= 0f) continue;
            dust(ctx, buf, j, g.x + (h(seed, j, 2) - 0.5f) * 1.2f * k, gy + (0.5f + j * 0.95f * k) * eo(p) + 0.3f, g.z + (h(seed, j, 3) - 0.5f) * 1.2f * k, (2.2f + 0.7f * j) * k * (0.5f + 0.5f * p), h(seed, j, 4) * 6f + age * 0.02f, 0.85f * (1f - smooth(16f, 40f, age)) * (0.5f + 0.5f * (1f - j / 6f)));
        }

        // 5. slabs of rock lifted out of the glowing ground, thrown up and falling back
        int ns = ctx.seg(10, 5);
        for (int i = 0; i < ns && buf.hasBudget(4); i++) {
            float t0 = 1.5f + h(seed, i, 5) * 5f;
            float tt = age - t0;
            if (tt <= 0f) continue;
            float an = Mth.TWO_PI * (i + h(seed, i, 6)) / ns;
            float rr = 1.4f + (Rv * 0.55f - 1.4f) * h(seed, i, 7);
            float vy = 0.45f + 0.4f * h(seed, i, 8);
            float y = vy * tt - 0.025f * tt * tt;
            if (y < 0f) continue;                                             // it has landed again
            float drift = 0.06f * tt * (h(seed, i, 9) - 0.5f);
            slab(ctx, buf, i, g.x + Mth.cos(an) * (rr + drift * 4f), gy + 0.3f + y, g.z + Mth.sin(an) * (rr + drift * 4f), (0.7f + 0.9f * h(seed, i, 10)) * (0.7f + 0.3f * k), tt * 0.12f * (i % 2 == 0 ? 1f : -1f) + h(seed, i, 11) * 6f, 1f - smooth(30f, 44f, age));
        }

        // 6. the dust that billows at the front of the first ring, and the embers that rise out of the cracks
        if (near > 0.01f) {
            int nr = ctx.seg(10, 4);
            for (int i = 0; i < nr && buf.hasBudget(4); i++) {
                float p = Mth.clamp(t / 22f, 0f, 1f);
                if (p <= 0f || p >= 1f) break;
                float an = Mth.TWO_PI * (i + h(seed, i, 12) * 0.6f) / nr;
                float rr = Rv * eo(t / 20f) * (0.86f + 0.1f * h(seed, i, 13));
                dust(ctx, buf, i + 1, g.x + Mth.cos(an) * rr, gy + 0.35f + 0.9f * p, g.z + Mth.sin(an) * rr, 1.2f + Rv * 0.13f * (0.6f + 0.6f * h(seed, i, 14)), h(seed, i, 15) * 6f + age * 0.03f, 0.8f * (1f - p * p) * near);
            }
            int ne = ctx.seg(12, 4);
            for (int i = 0; i < ne && buf.hasBudget(4); i++) {
                float u = Mth.frac((age - 3f) / 30f + h(seed, i, 16));
                if (age < 3f) break;
                float an = h(seed, i, 17) * Mth.TWO_PI, rr = 1f + (Rv * 0.7f - 1f) * h(seed, i, 18);
                mote(ctx, buf, g.x + Mth.cos(an) * rr + Mth.sin(u * 7f + i) * 0.2f, gy + 0.3f + u * (2.5f + 2f * h(seed, i, 19)), g.z + Mth.sin(an) * rr, 0.3f, a(mix(GOLD, EMBER, u), Mth.sin(u * Mth.PI) * 0.95f * near * fade));
            }
        }
    }

    // ================================================================== HERCULES_INVICTUS
    private void invictus(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 20, 20);                                  // 20 ticks each way: a re-send every 80 ticks cross-fades with the one that is ending
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        float hs = Mth.clamp(inst.power / 1.6f, 0.6f, 2.2f);
        Vector3f c = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(40f, 90f, horizontal(c));
        float gy = c.y - 0.9f * hs;
        float pulse = 0.9f + 0.1f * Mth.sin(age * 0.17f);

        // 1. the icon behind the body: a laurel wreath with a fist in it, a little behind the caster as seen from the camera
        float cl = Mth.sqrt(c.x * c.x + c.y * c.y + c.z * c.z) + 1e-3f;
        float bx = c.x + c.x / cl * 0.9f * hs, by = c.y + 0.1f * hs + c.y / cl * 0.9f * hs, bz = c.z + c.z / cl * 0.9f * hs;
        float wz = 3.5f * hs * (0.97f + 0.03f * Mth.sin(age * 0.1f));
        sprite(ctx, buf, LAUREL, VfxBlend.ADD, bx, by, bz, wz, 0f, a(WHITE, 0.62f * fade * pulse));
        sprite(ctx, buf, FIST, VfxBlend.ADD, bx, by + 0.12f * hs, bz, wz * 0.56f, 0f, a(WHITE, 0.8f * fade * pulse));
        glow(ctx, buf, bx, by, bz, 2.4f * hs, EMBER, 0.35f * fade);

        // 2. the crown: a laurel wreath turns over the head, a ring of gold under it, a glow over it
        float hy = c.y + 1.12f * hs;
        buf.plane(LAUREL, VfxBlend.ADD, tilted(c.x, hy, c.z, 0.85f).spin(age * 0.03f), 0.68f * hs, a(WHITE, 0.95f * fade));
        buf.plane(RING, VfxBlend.ADD, tilted(c.x, hy - 0.04f * hs, c.z, 0.85f).spin(-age * 0.05f), 0.66f * hs / RING_R, a(GOLD, 0.75f * fade * pulse));
        glow(ctx, buf, c.x, hy + 0.1f * hs, c.z, 1.1f * hs, GOLD, 0.55f * fade);
        flash(ctx, buf, c.x, hy + 0.15f * hs, c.z, 0.9f * hs * (0.8f + 0.2f * Mth.sin(age * 0.23f)), age * 0.04f, a(IVORY, 0.8f * fade));

        // 3. the aura: tongues of bronze and gold flame stand along the body and lick upward; white-gold cores in the middle of the nearer ones
        int nf = ctx.seg(4, 2);
        for (int i = 0; i < nf && buf.hasBudget(8); i++) {
            float an = Mth.TWO_PI * i / nf + age * 0.012f + h(seed, i, 1);
            float x = c.x + Mth.cos(an) * 0.45f * hs, z = c.z + Mth.sin(an) * 0.45f * hs;
            float top = gy + (1.7f + 0.9f * h(seed, i, 2) + 0.2f * Mth.sin(age * 0.31f + i * 2.1f)) * hs;
            float sw = Mth.sin(age * 0.19f + i * 1.7f) * 0.14f * hs;
            ribbon(buf, FLAME, VfxBlend.ADD, x, gy + 0.05f, z, x + sw, top, z + sw * 0.7f, 0.42f * hs, 0.34f * hs, 1f, 0f, a(BRONZE, 0.55f * fade), a(EMBER, 0.3f * fade));
            if ((i & 1) == 0) ribbon(buf, FLAME, VfxBlend.ADD, x, gy + 0.1f, z, x + sw * 0.8f, gy + (top - gy) * 0.78f, z + sw * 0.5f, 0.26f * hs, 0.2f * hs, 1f, 0f, a(GOLD_L, 0.5f * fade), a(GOLD, 0.25f * fade));
        }

        // 4. the floor: a ring and a laurel ring turning under the feet, a glow
        VfxPose floor = ground(c.x, gy + 0.04f, c.z);
        ringAt(buf, floor, 1.25f * hs * (0.95f + 0.05f * Mth.sin(age * 0.2f)), a(GOLD, 0.8f * fade));
        buf.plane(LAUREL, VfxBlend.ADD, floor.lift(0.01f).spin(age * 0.025f), 1.5f * hs, a(WHITE, 0.6f * fade));
        VfxBloom.planeGlow(buf, floor, 1.5f * hs, EMBER, 0.55f * fade);

        // 5. embers and golden leaves rise round the body
        if (near > 0.01f) {
            int ne = ctx.seg(10, 4);
            for (int i = 0; i < ne && buf.hasBudget(4); i++) {
                float u = Mth.frac(age / 36f + h(seed, i, 3));
                float an = h(seed, i, 4) * Mth.TWO_PI + u * 1.6f;
                float rr = (0.35f + 0.55f * h(seed, i, 5)) * hs;
                mote(ctx, buf, c.x + Mth.cos(an) * rr, gy + 0.1f + u * 2.8f * hs, c.z + Mth.sin(an) * rr, 0.2f * hs * (0.6f + 0.8f * h(seed, i, 6)), a(mix(GOLD, EMBER, u), Mth.sin(u * Mth.PI) * 0.95f * fade * near));
            }
            int nl = ctx.seg(4, 2);
            for (int i = 0; i < nl && buf.hasBudget(4); i++) {
                float u = Mth.frac(age / 60f + h(seed, i, 7));
                float an = h(seed, i, 8) * Mth.TWO_PI + u * 3.2f;
                float rr = (0.75f + 0.4f * h(seed, i, 9)) * hs;
                sprite(ctx, buf, LEAF, VfxBlend.ADD, c.x + Mth.cos(an) * rr, gy + 0.3f + u * 2.4f * hs, c.z + Mth.sin(an) * rr, 0.34f * hs, an + u * 5f, a(WHITE, Mth.sin(u * Mth.PI) * 0.85f * fade * near));
            }
        }

        // 6. every time it is (re)cast: the fist flares on the chest for a moment
        float fp = smooth(0f, 4f, age) * (1f - smooth(5f, 20f, age));
        if (fp > 0.01f) {
            sprite(ctx, buf, FIST, VfxBlend.ADD, c.x - c.x / cl * 0.5f, c.y + 0.1f * hs, c.z - c.z / cl * 0.5f, 1.4f * hs * (0.75f + 0.25f * fp), 0f, a(WHITE, 0.8f * fp));
        }
    }

    // ================================================================== HERCULES_GRIP
    private void grip(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 2, 4);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f t = ctx.rel(inst.to(ctx));
        float dx = c.x - t.x, dy = c.y - t.y, dz = c.z - t.z;                    // from the target to the caster
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        float ux = len > 0.01f ? dx / len : 0f, uy = len > 0.01f ? dy / len : 0f, uz = len > 0.01f ? dz / len : 1f;
        float near = 1f - smooth(40f, 90f, horizontal(t));
        float rot = iconRot(ctx, -ux, -uy, -uz);                                  // the fingers point from the caster to the target
        float close = smooth(8f, 13f, age);                                       // 0 open hand ... 1 fist
        float openIn = VfxAnim.easeOutBack(Mth.clamp(age / 7f, 0, 1));
        float sq = Mth.sin(Math.max(0f, age - 13f) * 0.9f) * 0.06f * smooth(13f, 15f, age);
        float handSize = 4.4f * openIn * (1f - 0.12f * close);
        float fistSize = (4.0f - 0.8f * close) + sq * 2f;

        // 1. the hand opens round the target and closes into a fist on it: a 3D bronze Titan's hand (3.20.0), or the flat sprite of 3.19.0 (setting off, far away, thin particles)
        float ha = (1f - close) * fade, fa = close * fade;
        float flashK = smooth(10.5f, 12f, age) * (1f - smooth(12f, 17f, age));
        boolean drawnHand = false;
        if (modelsOn(ctx, t.x, t.y, t.z, MODEL_RANGE)) drawnHand = titanHand(buf, t, -ux, -uy, -uz, close, openIn, sq, fade, flashK) > 0;
        if (!drawnHand) {
            sprite(ctx, buf, HAND, VfxBlend.ALPHA, t.x, t.y + 0.1f, t.z, handSize, rot, a(WHITE, 0.92f * ha));
            sprite(ctx, buf, HAND, VfxBlend.ADD, t.x, t.y + 0.1f, t.z, handSize * 1.04f, rot, a(GOLD, 0.45f * ha));
            sprite(ctx, buf, FIST, VfxBlend.ALPHA, t.x, t.y + 0.1f, t.z, fistSize * smooth(8f, 10f, age), rot, a(WHITE, 0.95f * fa));
            sprite(ctx, buf, FIST, VfxBlend.ADD, t.x, t.y + 0.1f, t.z, fistSize * 1.04f * smooth(8f, 10f, age), rot, a(GOLD, 0.5f * fa));
        }
        glow(ctx, buf, t.x, t.y + 0.1f, t.z, 2.6f + 1.4f * close, EMBER, 0.7f * fade);

        // 2. the closing: a flash, a ring and a web of cracks round the fist
        float fl = smooth(10.5f, 12f, age) * (1f - smooth(12f, 17f, age));
        if (fl > 0.01f) {
            float fk = drawnHand ? 0.55f : 1f;                                                      // with the 3D fist the flash is softer, so the fist stays readable
            flash(ctx, buf, t.x, t.y + 0.1f, t.z, 7.5f * fk * (0.5f + 0.5f * fl), 0.2f, a(WHITE, fl * fk));
            VfxPose rp = across(t.x, t.y + 0.1f, t.z, -t.x, -t.y, -t.z);
            ringAt(buf, rp, 1.2f + 3.2f * eo((age - 11.5f) / 6f), a(GOLD, fl * 0.95f));
        }
        float web = smooth(12f, 13.5f, age) * (1f - smooth(19f, 24f, age));
        if (web > 0.01f) {
            VfxPose wp = across(t.x, t.y + 0.1f, t.z, -t.x, -t.y, -t.z).spin(h(seed, 0, 1) * Mth.TWO_PI);
            float wk = drawnHand ? 0.6f : 1f;
            buf.plane(WEB, VfxBlend.ALPHA, wp, 2.0f + 0.4f * eo((age - 12f) / 4f), a(WHITE, 0.6f * wk * web));
            buf.plane(WEB, VfxBlend.ADD, wp.lift(0.01f), 2.0f + 0.4f * eo((age - 12f) / 4f), a(GOLD, 0.5f * wk * web));
        }
        float gy = t.y - 0.9f;
        float cl = eo((age - 3f) / 10f);
        if (age > 3f && cl < 1f) ringAt(buf, ground(t.x, gy + 0.05f, t.z), 3.6f - 2.6f * cl, a(GOLD, 0.8f * fade * (1f - 0.3f * cl)));
        VfxBloom.planeGlow(buf, ground(t.x, gy + 0.04f, t.z), 1.7f, EMBER, 0.5f * fade * smooth(3f, 10f, age));

        // 3. the chains: three heavy chains of bronze run from the hand to the caster, shoot out as it closes and then pull
        if (len > 0.8f) {
            float reach = eo((age - 4f) / 9f);
            float taut = smooth(12f, 16f, age);
            float sx = ctx.camRight.x, sy = ctx.camRight.y, sz = ctx.camRight.z;
            for (int k = 0; k < 3 && reach > 0.02f; k++) {
                float off = (k - 1) * 0.5f;
                float sag = (1f - taut) * (0.5f + 0.5f * h(seed, k, 2)) * Math.min(2.4f, len * 0.12f);
                for (int sgm = 0; sgm < 4 && buf.hasBudget(4); sgm++) {
                    float u0 = reach * sgm / 4f, u1 = reach * (sgm + 1) / 4f;
                    float x0 = t.x + (c.x - t.x) * u0 + sx * off * (1f - u0) * 0.8f, y0 = t.y + 0.1f + (c.y - t.y) * u0 + (k - 1) * 0.25f * (1f - u0) - sag * 4f * u0 * (1f - u0);
                    float z0 = t.z + (c.z - t.z) * u0 + sz * off * (1f - u0) * 0.8f;
                    float x1 = t.x + (c.x - t.x) * u1 + sx * off * (1f - u1) * 0.8f, y1 = t.y + 0.1f + (c.y - t.y) * u1 + (k - 1) * 0.25f * (1f - u1) - sag * 4f * u1 * (1f - u1);
                    float z1 = t.z + (c.z - t.z) * u1 + sz * off * (1f - u1) * 0.8f;
                    float segLen = Mth.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
                    float v0 = u0 * len * 0.48f - age * 0.09f * taut, v1 = v0 + segLen * 0.48f;
                    ribbon(buf, CHAIN, VfxBlend.ALPHA, x0, y0, z0, x1, y1, z1, 0.24f, 0.24f, v0, v1, a(WHITE, 0.95f * fade), a(WHITE, 0.95f * fade));
                }
            }
            // a glow runs along the middle chain, and streaks of pull run along the line towards the caster
            float gx0 = t.x, gy0 = t.y + 0.1f, gz0 = t.z;
            ribbon(buf, STREAK, VfxBlend.ADD, gx0 + (c.x - gx0) * reach, gy0 + (c.y - gy0) * reach, gz0 + (c.z - gz0) * reach, gx0, gy0, gz0, 0.4f, 0.55f, 0f, 1f, a(GOLD, 0f), a(GOLD, 0.35f * fade * smooth(12f, 16f, age)));
            if (age > 5f) {
                int nl = ctx.seg(8, 4);
                float seg = Math.min(3.5f, len * 0.3f);
                for (int i = 0; i < nl && buf.hasBudget(4); i++) {
                    float s = Mth.frac(age / 7f + h(seed, i, 3));
                    float head = s * len, tail = Math.max(0f, head - seg);
                    float an = h(seed, i, 4) * Mth.TWO_PI, rr = 0.3f + 0.9f * h(seed, i, 5);
                    float ox = ctx.camRight.x * Mth.cos(an) * rr + ctx.camUp.x * Mth.sin(an) * rr, oy = ctx.camRight.y * Mth.cos(an) * rr + ctx.camUp.y * Mth.sin(an) * rr, oz = ctx.camRight.z * Mth.cos(an) * rr + ctx.camUp.z * Mth.sin(an) * rr;
                    float al = Mth.sin(s * Mth.PI) * fade * smooth(5f, 8f, age);
                    streak(buf, t.x + ux * tail + ox, t.y + uy * tail + oy, t.z + uz * tail + oz, t.x + ux * head + ox, t.y + uy * head + oy, t.z + uz * head + oz, 0.07f, a(IVORY, 0.9f * al));
                }
            }
            // where the chains reach the caster: a flash and a ring
            float ar = smooth(11f, 14f, age) * (1f - smooth(14f, 20f, age));
            if (ar > 0.01f) {
                flash(ctx, buf, c.x, c.y + 0.1f, c.z, 2.0f * ar + 0.4f, 0f, a(GOLD_L, ar));
                ringAt(buf, across(c.x, c.y + 0.1f, c.z, -c.x, -c.y, -c.z), 0.5f + 1.1f * eo((age - 11f) / 6f), a(GOLD, ar * 0.9f));
            }
        }

        // 4. embers fall from the grip
        if (near > 0.01f) {
            int nm = ctx.seg(6, 3);
            for (int i = 0; i < nm && buf.hasBudget(4); i++) {
                float u = Mth.frac(age / 18f + h(seed, i, 6));
                if (age < 8f) break;
                float an = h(seed, i, 7) * Mth.TWO_PI, rr = 0.8f + 1.4f * h(seed, i, 8);
                mote(ctx, buf, t.x + ctx.camRight.x * Mth.cos(an) * rr, t.y + 1.2f - u * 2.4f + Mth.sin(an) * 0.4f, t.z + ctx.camRight.z * Mth.cos(an) * rr, 0.26f, a(mix(GOLD, EMBER, u), Mth.sin(u * Mth.PI) * 0.9f * fade * near));
            }
        }
    }

    /**
     * 3.20.0, Titan's Grip: the huge bronze hand ({@link HerculesModels#HAND}, at most 40 quads). Its fingers point along the line caster -> target (dir), its back faces up and a little toward the camera, it grows out of
     * the caster's side in 7 ticks with the fingers fanned, then {@code close} (0..1) curls them into a fist whose centre lies on the target. Returns the quads drawn (0 = the caller draws the flat sprite).
     */
    private int titanHand(VfxVertexBuffer buf, Vector3f t, float dirx, float diry, float dirz, float close, float openIn, float sq, float fade, float flashK) {
        float S = 1.5f * Math.max(0.05f, openIn) * (1f - 0.1f * close) * (1f + sq);
        HerculesModels.Posed p = mesh.posed();
        p.clearPose();
        HerculesModels.handPose(close, 1f + 0.6f * (1f - Mth.clamp(openIn, 0f, 1f)), p.bend(), p.spread());
        float tl = Mth.sqrt(t.x * t.x + t.y * t.y + t.z * t.z) + 1e-3f;
        p.look(dirx, diry, dirz, -t.x / tl * 0.8f, 0.9f - t.y / tl * 0.8f, -t.z / tl * 0.8f, 0f, S);
        float fy = HerculesModels.HAND_FIST_Y, fz = HerculesModels.HAND_FIST_Z;
        float lift = 0.55f * (1f - close);                                           // the open hand holds a little above the target, the fist closes onto it
        float reach = 1.4f * (1f - Mth.clamp(openIn, 0f, 1f));                        // and comes in from the caster's side
        float ox = t.x - p.wx(0f, fy - lift, fz) - dirx * reach * S;
        float oy = t.y + 0.1f - p.wy(0f, fy - lift, fz) - diry * reach * S;
        float oz = t.z - p.wz(0f, fy - lift, fz) - dirz * reach * S;
        return mesh.draw(buf, MODEL_ATLAS, HerculesModels.HAND, ox, oy, oz, a(WHITE, 0.97f * fade), 0.85f * flashK);
    }

    // ================================================================== HERCULES_THROW
    private void hurl(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 0, 6);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        Vector3f f = ctx.rel(inst.from(ctx));
        Vector3f e = ctx.rel(inst.to(ctx));
        float dx = e.x - f.x, dy = e.y - f.y, dz = e.z - f.z;
        float dist = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        float flight = throwLanding(dist) - THROW_START;
        int land = THROW_START + (int) flight;
        float near = 1f - smooth(50f, 100f, horizontal(e));
        float nearF = 1f - smooth(50f, 100f, horizontal(f));
        float arcH = Math.min(5f, dist * 0.12f);
        float u = Mth.clamp((age - THROW_START) / flight, 0f, 1f);
        float tx = dx / Math.max(dist, 0.01f), ty = dy / Math.max(dist, 0.01f), tz = dz / Math.max(dist, 0.01f);

        // 1. the throw: a burst at the origin
        if (age < 11f && nearF > 0.01f) {
            float gy = f.y - 0.85f;
            float fl = 1f - smooth(0f, 4f, age);
            if (fl > 0.01f) {
                flash(ctx, buf, f.x, f.y, f.z, 4.4f * (0.5f + 0.5f * fl), 0.5f, a(WHITE, fl));
                glow(ctx, buf, f.x, f.y, f.z, 2f * fl, GOLD, fl);
            }
            float p = eo(age / 9f);
            ringAt(buf, ground(f.x, gy + 0.05f, f.z), 0.7f + 2.6f * p, a(GOLD, (1f - p) * 0.9f * fade));
            cracksAt(buf, ground(f.x, gy + 0.03f, f.z).spin(h(seed, 0, 1) * Mth.TWO_PI), 2.2f * eo(age / 4f), 0.85f * (1f - smooth(5f, 11f, age)), a(heat(age / 14f), 0.7f * (1f - smooth(4f, 11f, age))));
            for (int i = 0; i < 4 && buf.hasBudget(4); i++) {
                float an = Mth.TWO_PI * (i + h(seed, i, 2)) / 4f;
                dust(ctx, buf, i, f.x + Mth.cos(an) * (0.4f + 1.8f * p), gy + 0.3f + 0.6f * p, f.z + Mth.sin(an) * (0.4f + 1.8f * p), 1f + 1.2f * p, h(seed, i, 3) * 6f, 0.8f * (1f - p * p));
            }
        }

        // 2. the flight: a streak along an arc, a cone of shock behind it, dust torn up along the way, speed lines
        float uTail = Math.max(0f, u - Math.min(0.9f, 8f / Math.max(dist, 1f)));
        if (age >= THROW_START && age < land + 3) {
            float al = (1f - smooth(land, land + 3f, age)) * fade;
            float hx = f.x + dx * u, hy = f.y + dy * u + arcH * 4f * u * (1f - u), hz = f.z + dz * u;
            float px = hx, py = hy, pz = hz;
            for (int s = 1; s <= 3 && buf.hasBudget(8); s++) {
                float us = uTail + (u - uTail) * (3 - s) / 3f;
                float x = f.x + dx * us, y = f.y + dy * us + arcH * 4f * us * (1f - us), z = f.z + dz * us;
                float w = 0.2f + 0.55f * (1f - s / 3f);
                streak(buf, x, y, z, px, py, pz, w * 1.4f, a(EMBER, 0.85f * al));
                streak(buf, x, y, z, px, py, pz, w * 0.7f, a(IVORY, al));
                px = x;
                py = y;
                pz = z;
            }
            // the tail is drawn from head back, so the loop above went tail-ward: nothing more to do for the line itself
            glow(ctx, buf, hx, hy, hz, 1.8f, GOLD, 0.95f * al);
            // the cone of shock: rings across the flight direction, behind the head, wider and fainter the further back; two lines of the cone seen from the side
            float tdx = dx + arcH * 4f * (1f - 2f * u) * 0f, tdy = dy + arcH * 4f * (1f - 2f * u), tdz = dz;
            for (int k = 0; k < 3 && buf.hasBudget(4); k++) {
                float back = 1.2f + 1.5f * k;
                float rx = hx - tx * back, ry = hy - ty * back, rz = hz - tz * back;
                ringAt(buf, across(rx, ry, rz, tdx, tdy, tdz), 0.7f + 0.55f * k, a(k == 0 ? GOLD : BRONZE, (0.75f - 0.2f * k) * al));
            }
            float sx = ctx.camUp.x, sy = ctx.camUp.y, sz = ctx.camUp.z;
            for (int sd = -1; sd <= 1 && buf.hasBudget(4); sd += 2) {
                streak(buf, hx - tx * 5.5f + sx * sd * 1.9f, hy - ty * 5.5f + sy * sd * 1.9f, hz - tz * 5.5f + sz * sd * 1.9f, hx, hy, hz, 0.06f, a(IVORY, 0.7f * al));
            }
            int nd = ctx.seg(8, 4);
            for (int i = 0; i < nd && buf.hasBudget(4); i++) {
                float back = (i + h(seed, i, 4)) / nd;                            // 0 = at the head, 1 = at the tail
                float us = Mth.lerp(back, u, uTail);
                if (us <= 0.001f) continue;
                float x = f.x + dx * us, y = f.y + dy * us + arcH * 4f * us * (1f - us), z = f.z + dz * us;
                float an = h(seed, i, 5) * Mth.TWO_PI, rr = 0.3f + 0.7f * back;
                dust(ctx, buf, i, x + Mth.cos(an) * rr * 0.6f, y - 0.2f + 0.3f * back + Mth.sin(an) * rr * 0.5f, z + Mth.sin(an) * rr * 0.6f, 0.9f + 0.9f * back, h(seed, i, 6) * 6f + age * 0.1f, 0.7f * (1f - back * 0.6f) * al * near);
            }
            int nl = ctx.seg(6, 3);
            for (int i = 0; i < nl && buf.hasBudget(4); i++) {
                float back = 0.25f + 0.75f * h(seed, i, 7);
                float ox = ctx.camRight.x * (h(seed, i, 8) - 0.5f) * 2.2f + sx * (h(seed, i, 9) - 0.5f) * 1.6f, oy = ctx.camRight.y * (h(seed, i, 8) - 0.5f) * 2.2f + sy * (h(seed, i, 9) - 0.5f) * 1.6f, oz = ctx.camRight.z * (h(seed, i, 8) - 0.5f) * 2.2f + sz * (h(seed, i, 9) - 0.5f) * 1.6f;
                float us = Mth.lerp(back * 0.5f, u, uTail), us2 = Mth.lerp(back * 0.5f + 0.5f, u, uTail);
                streak(buf, f.x + dx * us2 + ox, f.y + dy * us2 + arcH * 4f * us2 * (1f - us2) + oy, f.z + dz * us2 + oz, f.x + dx * us + ox, f.y + dy * us + arcH * 4f * us * (1f - us) + oy, f.z + dz * us + oz, 0.05f, a(GOLD_L, 0.75f * al));
            }
        }

        // 3. the landing: an impact frame, rings, cracks, rock, dust and embers
        float tl = age - land;
        if (tl >= 0f) {
            float gy = e.y + 0.04f;
            VfxPose floor = ground(e.x, gy, e.z);
            float fl = 1f - smooth(0f, 6f, tl);
            if (fl > 0.01f) {
                flash(ctx, buf, e.x, gy + 0.7f, e.z, 9.5f * (0.55f + 0.45f * fl), 0.1f, a(WHITE, fl));
                glow(ctx, buf, e.x, gy + 0.6f, e.z, 3.4f * fl, GOLD, fl);
            }
            buf.plane(CRATER, VfxBlend.ALPHA, floor.spin(h(seed, 1, 1) * Mth.TWO_PI), 3.4f, a(WHITE, 0.95f * smooth(0f, 1.5f, tl) * (1f - smooth(18f, 30f, age))));
            float p0 = eo(tl / 10f), p1 = eo((tl - 2f) / 10f);
            ringAt(buf, floor.lift(0.03f), 0.9f + 7.0f * p0, a(GOLD, (1f - p0 * p0) * 0.95f * fade));
            ringAt(buf, floor.lift(0.035f), 0.6f + 5.0f * p1, a(IVORY, (1f - p1 * p1) * 0.8f * fade));
            waveAt(buf, VfxBlend.ALPHA, floor.lift(0.01f), (0.9f + 7.0f * p0) * 0.96f, a(DUST_TINT, (1f - p0) * 0.5f * fade));
            cracksAt(buf, floor.lift(0.005f).spin(h(seed, 1, 2) * Mth.TWO_PI), 5.5f * eo(tl / 5f), 0.92f * (1f - smooth(16f, 30f, age)), a(heat(tl / 26f), 0.85f * (1f - smooth(10f, 28f, age))));
            for (int j = 0; j < 4 && buf.hasBudget(4); j++) {
                float p = Mth.clamp((tl - j * 0.5f) / 12f, 0f, 1f);
                if (p <= 0f) continue;
                dust(ctx, buf, j, e.x + (h(seed, j, 3) - 0.5f) * 1.2f, gy + 0.4f + (0.5f + j * 0.9f) * eo(p), e.z + (h(seed, j, 4) - 0.5f) * 1.2f, 2f + 0.6f * j, h(seed, j, 5) * 6f, 0.85f * (1f - smooth(8f, 28f, age)));
            }
            int ns = ctx.seg(10, 5);
            for (int i = 0; i < ns && buf.hasBudget(4); i++) {
                float tt = tl - h(seed, i, 6) * 2f;
                if (tt <= 0f) continue;
                float an = Mth.TWO_PI * (i + h(seed, i, 7)) / ns;
                float rr = 0.7f + 3.2f * h(seed, i, 8);
                float vy = 0.4f + 0.35f * h(seed, i, 9);
                float y = vy * tt - 0.03f * tt * tt;
                if (y < 0f) continue;
                slab(ctx, buf, i, e.x + Mth.cos(an) * (rr + 0.12f * tt), gy + 0.3f + y, e.z + Mth.sin(an) * (rr + 0.12f * tt), 0.5f + 0.9f * h(seed, i, 10), tt * 0.14f * (i % 2 == 0 ? 1f : -1f) + h(seed, i, 11) * 6f, 1f - smooth(20f, 30f, age));
            }
            if (near > 0.01f) {
                int nd = ctx.seg(8, 4);
                for (int i = 0; i < nd && buf.hasBudget(4); i++) {
                    float p = Mth.clamp(tl / 14f, 0f, 1f);
                    if (p >= 1f) break;
                    float an = Mth.TWO_PI * (i + h(seed, i, 12) * 0.5f) / nd;
                    float rr = (0.9f + 7.0f * eo(tl / 10f)) * 0.9f;
                    dust(ctx, buf, i + 1, e.x + Mth.cos(an) * rr, gy + 0.35f + 0.8f * p, e.z + Mth.sin(an) * rr, 1.6f + 0.8f * h(seed, i, 13), h(seed, i, 14) * 6f, 0.8f * (1f - p * p) * near);
                }
                int ne = ctx.seg(6, 3);
                for (int i = 0; i < ne && buf.hasBudget(4); i++) {
                    float uu = Mth.frac(tl / 18f + h(seed, i, 15));
                    float an = h(seed, i, 16) * Mth.TWO_PI, rr = 0.6f + 2.6f * h(seed, i, 17);
                    mote(ctx, buf, e.x + Mth.cos(an) * rr, gy + 0.3f + uu * 3f, e.z + Mth.sin(an) * rr, 0.28f, a(mix(GOLD, EMBER, uu), Mth.sin(uu * Mth.PI) * 0.9f * near * (1f - smooth(18f, 30f, age))));
                }
            }
        }
    }

    // ================================================================== HERCULES_ROAR
    private void roar(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 0, 8);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        float Rv = Mth.clamp(inst.power, 6f, ROAR_MAX);
        Vector3f c = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(50f, 100f, horizontal(c));
        float gy = c.y - 0.86f;
        VfxPose floor = ground(c.x, gy + 0.05f, c.z);
        float t = Math.max(0f, age - 6f);                                      // 6 ticks to breathe in, then the roar

        // 1. the breath: dust and light are drawn in towards the chest, a ring closes on the feet
        if (age < 8f) {
            float in = smooth(0f, 6f, age) * (1f - smooth(6f, 8f, age));
            int ni = ctx.seg(8, 4);
            for (int i = 0; i < ni && buf.hasBudget(4); i++) {
                float s = Mth.frac(age / 5f + h(seed, i, 1));
                float an = Mth.TWO_PI * (i + h(seed, i, 2) * 0.5f) / ni;
                float ry = 0.3f + 1.5f * h(seed, i, 3);
                float r0 = 4.2f * (1f - s * 0.85f), r1 = Math.max(1f, r0 - 1.4f);
                streak(buf, c.x + Mth.cos(an) * r0, gy + ry, c.z + Mth.sin(an) * r0, c.x + Mth.cos(an) * r1, gy + ry, c.z + Mth.sin(an) * r1, 0.07f, a(GOLD_L, 0.85f * in * Mth.sin(s * Mth.PI)));
            }
            ringAt(buf, floor, 3.6f - 2.4f * smooth(0f, 6f, age), a(GOLD, 0.8f * in));
            glow(ctx, buf, c.x, c.y + 0.3f, c.z, 1.2f + 1.4f * smooth(0f, 7f, age), EMBER, 0.8f * smooth(0f, 6f, age) * (1f - smooth(8f, 12f, age)));
        }
        if (age < 6f) return;

        // 2. the roar: an impact flash at the chest, three thick rings over the ground under a layered bronze dome
        float fl = 1f - smooth(0f, 5f, t);
        if (fl > 0.01f) {
            flash(ctx, buf, c.x, c.y + 0.3f, c.z, 8f * (0.55f + 0.45f * fl), 0f, a(WHITE, fl));
            glow(ctx, buf, c.x, c.y + 0.3f, c.z, 3.2f * fl, GOLD, fl);
        }
        float p0 = eo(t / 22f), p1 = eo((t - 4f) / 22f), p2 = eo((t - 8f) / 22f);
        ringAt(buf, floor.lift(0.03f), Rv * p0, a(GOLD, (1f - p0 * p0) * 0.95f * fade));
        ringAt(buf, floor.lift(0.035f), Rv * p1, a(IVORY, (1f - p1 * p1) * 0.85f * fade));
        ringAt(buf, floor.lift(0.04f), Rv * p2, a(BRONZE, (1f - p2 * p2) * 0.8f * fade));
        float pd = eo(t / 15f);
        for (int k = 0; k < 4; k++) {
            float ph = 0.25f * k;                                              // 0, 0.25, 0.5, 0.75 of the way up the dome
            float rk = Rv * pd * Mth.cos(ph * 1.45f), yk = Rv * pd * 0.36f * Mth.sin(ph * 1.45f);
            waveAt(buf, VfxBlend.ADD, ground(c.x, gy + 0.1f + yk, c.z), rk, a(mix(BRONZE, GOLD, 1f - ph), (1f - pd) * (0.85f - 0.14f * k) * fade));
        }
        waveAt(buf, VfxBlend.ALPHA, floor.lift(0.01f), Rv * p0 * 0.96f, a(DUST_TINT, (1f - p0) * 0.5f * fade));
        // the ground rings: two faint waves that go out and back
        for (int k = 0; k < 2; k++) {
            float q = Mth.frac((t - 3f) / 12f + k * 0.5f);
            if (t > 3f) waveAt(buf, VfxBlend.ADD, floor.lift(0.02f), Rv * (0.12f + 0.4f * q), a(GOLD, Mth.sin(q * Mth.PI) * 0.4f * fade));
        }
        cracksAt(buf, floor.lift(0.005f).spin(h(seed, 0, 1) * Mth.TWO_PI), Math.min(Rv * 0.25f, 6f) * eo(t / 8f), 0.85f * (1f - smooth(22f, 30f, age)), a(heat(Mth.clamp(age / 36f, 0f, 1f)), 0.7f * (1f - smooth(18f, 30f, age))));

        // 3. rays that fly out of the chest, laurel leaves torn from the caster and blown outward, dust at the front of the first ring
        int nr = ctx.seg(10, 5);
        for (int i = 0; i < nr && buf.hasBudget(4); i++) {
            float p = (t - h(seed, i, 4) * 3f) / 14f;
            if (p <= 0f || p >= 1f) continue;
            float an = Mth.TWO_PI * (i + h(seed, i, 5) * 0.6f) / nr;
            float ry = 0.3f + 1.4f * h(seed, i, 6);
            float head = 2.5f + (Rv * 0.45f) * eo(p), tail = Math.max(1f, head - (3f + 3f * h(seed, i, 7)));
            streak(buf, c.x + Mth.cos(an) * tail, gy + ry, c.z + Mth.sin(an) * tail, c.x + Mth.cos(an) * head, gy + ry, c.z + Mth.sin(an) * head, 0.09f + 0.05f * h(seed, i, 8), a(i % 3 == 0 ? IVORY : GOLD, 0.9f * (1f - p * p)));
        }
        if (near > 0.01f) {
            int nl = ctx.seg(12, 6);
            for (int i = 0; i < nl && buf.hasBudget(4); i++) {
                float p = (t - h(seed, i, 9) * 4f) / 26f;
                if (p <= 0f || p >= 1f) continue;
                float an = Mth.TWO_PI * (i + h(seed, i, 10) * 0.7f) / nl + p * 1.4f;
                float dd = 1.2f + Rv * 0.55f * eo(p) * (0.6f + 0.4f * h(seed, i, 11));
                sprite(ctx, buf, LEAF, VfxBlend.ADD, c.x + Mth.cos(an) * dd, gy + 1.1f + 2.4f * p * h(seed, i, 12) + 0.8f * Mth.sin(p * 9f + i), c.z + Mth.sin(an) * dd, 0.55f + 0.5f * h(seed, i, 13), p * 14f * (i % 2 == 0 ? 1f : -1f) + h(seed, i, 14) * 6f, a(WHITE, Mth.sin(p * Mth.PI) * 0.95f * near));
            }
            int nd = ctx.seg(10, 4);
            for (int i = 0; i < nd && buf.hasBudget(4); i++) {
                float p = Mth.clamp(t / 24f, 0f, 1f);
                if (p >= 1f) break;
                float an = Mth.TWO_PI * (i + h(seed, i, 15) * 0.6f) / nd;
                float rr = Rv * p0 * (0.88f + 0.08f * h(seed, i, 16));
                dust(ctx, buf, i, c.x + Mth.cos(an) * rr, gy + 0.4f + 0.9f * p, c.z + Mth.sin(an) * rr, 1.3f + Rv * 0.1f * (0.6f + 0.6f * h(seed, i, 17)), h(seed, i, 18) * 6f + age * 0.03f, 0.75f * (1f - p * p) * near);
            }
        }
    }

    // ================================================================== HERCULES_STRIKE
    private void strike(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 2, 8);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f e = ctx.rel(inst.to(ctx));
        float dx = e.x - c.x, dy = e.y - c.y, dz = e.z - c.z;
        float dl = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = dl > 0.01f ? dx / dl : 0f, ny = dl > 0.01f ? dy / dl : 0f, nz = dl > 0.01f ? dz / dl : 1f;
        float near = 1f - smooth(50f, 100f, horizontal(e));
        float gy = c.y - 0.86f;
        float rot = iconRot(ctx, nx, ny, nz);
        float te = hold(age, STRIKE_IMPACT, STRIKE_HOLD);                         // the picture stops on the impact
        float since = Math.max(0f, te - STRIKE_IMPACT);                           // ticks since the blow, with the stop taken out
        boolean hit = age >= STRIKE_IMPACT;

        // the fist: it forms behind the shoulder, trembles, then goes to the point of the punch in two ticks and stays there
        float grow = 0.55f + 3.1f * eo(age / 14f);
        float trav = VfxAnim.easeInCubic(Mth.clamp((age - (STRIKE_IMPACT - 3f)) / 3f, 0f, 1f));
        float wx = c.x - nx * 1.0f + (h(seed, (int) age, 1) - 0.5f) * 0.08f * smooth(0f, 14f, age) * (1f - trav);
        float wy = c.y + 0.3f + (h(seed, (int) age, 2) - 0.5f) * 0.08f * smooth(0f, 14f, age) * (1f - trav);
        float wz = c.z - nz * 1.0f + (h(seed, (int) age, 3) - 0.5f) * 0.08f * smooth(0f, 14f, age) * (1f - trav);
        float fx = Mth.lerp(trav, wx, e.x), fy = Mth.lerp(trav, wy, e.y), fz = Mth.lerp(trav, wz, e.z);
        float fs = grow * (1f + 0.2f * trav) * (hit ? 1.12f : 1f);
        float fa = smooth(0f, 5f, age) * (1f - smooth(STRIKE_IMPACT + STRIKE_HOLD + 6f, 34f, age));
        boolean drawnFist = false;
        if (fa > 0.01f) {
            // the clenched fist: a 3D bronze fist (3.20.0) that trembles behind the shoulder and holds on the impact, or the flat sprite of 3.19.0
            if (modelsOn(ctx, fx, fy, fz, MODEL_RANGE)) drawnFist = punchFist(buf, fx, fy, fz, nx, ny, nz, fs, age, trav, hit ? smooth(STRIKE_IMPACT + STRIKE_HOLD, STRIKE_IMPACT + STRIKE_HOLD + 6f, te) : 0f, a(WHITE, 0.97f * fa), seed) > 0;
            if (!drawnFist) {
                sprite(ctx, buf, FIST, VfxBlend.ALPHA, fx, fy, fz, fs, rot, a(WHITE, 0.97f * fa));
                sprite(ctx, buf, FIST, VfxBlend.ADD, fx, fy, fz, fs * 1.05f, rot, a(mix(GOLD, WHITE, trav), (0.4f + 0.4f * trav) * fa));
            }
            glow(ctx, buf, fx, fy, fz, 1.6f + fs * 0.6f, EMBER, (0.35f + 0.5f * smooth(0f, 14f, age)) * fa * (drawnFist ? 0.2f : 1f));
        }

        // 1. the gather: light and leaves are drawn into the fist, the ground ring closes under the feet, embers rise
        if (age < STRIKE_IMPACT) {
            float gat = smooth(0f, 3f, age) * (1f - smooth(STRIKE_IMPACT - 4f, STRIKE_IMPACT - 1f, age));
            int ng = ctx.seg(8, 4);
            for (int i = 0; i < ng && buf.hasBudget(4); i++) {
                float s = Mth.frac(age / 9f + h(seed, i, 4));
                float an = Mth.TWO_PI * (i + h(seed, i, 5) * 0.6f) / ng;
                float r0 = 5.5f * (1f - s * 0.88f), r1 = Math.max(0.7f, r0 - 1.7f);
                float yy = wy - 0.3f + (h(seed, i, 6) - 0.4f) * 2.2f;
                streak(buf, wx + Mth.cos(an) * r0, yy + (wy - yy) * s * 0.5f, wz + Mth.sin(an) * r0, wx + Mth.cos(an) * r1, yy + (wy - yy) * (s + 0.2f) * 0.5f, wz + Mth.sin(an) * r1, 0.08f, a(i % 2 == 0 ? GOLD : IVORY, 0.9f * gat * Mth.sin(s * Mth.PI)));
            }
            int nl = ctx.seg(6, 3);
            for (int i = 0; i < nl && buf.hasBudget(4); i++) {
                float s = Mth.frac(age / 14f + h(seed, i, 7));
                float an = h(seed, i, 8) * Mth.TWO_PI + s * 5f;
                float rr = 2.6f * (1f - s * 0.85f);
                sprite(ctx, buf, LEAF, VfxBlend.ADD, wx + Mth.cos(an) * rr, wy + Mth.sin(an) * rr * 0.6f + (h(seed, i, 9) - 0.5f), wz + Mth.sin(an) * rr * 0.9f, 0.45f, an * 2f, a(WHITE, Mth.sin(s * Mth.PI) * 0.9f * gat * near));
            }
            float ct = smooth(0f, STRIKE_IMPACT - 3f, age);
            ringAt(buf, ground(c.x, gy + 0.05f, c.z), 4.4f - 2.7f * ct, a(GOLD, 0.6f * gat));
            VfxBloom.planeGlow(buf, ground(c.x, gy + 0.04f, c.z), 2.0f, EMBER, 0.5f * gat);
            int ne = ctx.seg(6, 3);
            for (int i = 0; i < ne && buf.hasBudget(4); i++) {
                float u = Mth.frac(age / 16f + h(seed, i, 10));
                float an = h(seed, i, 11) * Mth.TWO_PI, rr = 0.5f + 1.3f * h(seed, i, 12);
                mote(ctx, buf, c.x + Mth.cos(an) * rr, gy + 0.2f + u * 2.4f, c.z + Mth.sin(an) * rr, 0.24f, a(mix(GOLD, EMBER, u), Mth.sin(u * Mth.PI) * 0.9f * near * gat));
            }
        }

        // 2. the blow: the fist smears across the gap in two ticks
        float sm = smooth(STRIKE_IMPACT - 3.2f, STRIKE_IMPACT - 2f, age) * (1f - smooth(STRIKE_IMPACT, STRIKE_IMPACT + 3f, age));
        if (sm > 0.01f) {
            streak(buf, wx, wy, wz, fx, fy, fz, fs * 0.42f, a(EMBER, 0.85f * sm));
            streak(buf, wx, wy, wz, fx, fy, fz, fs * 0.2f, a(WHITE, sm));
            int nsl = ctx.seg(4, 2);
            for (int i = 0; i < nsl && buf.hasBudget(4); i++) {
                float an = Mth.TWO_PI * i / nsl + 0.6f, rr = fs * (0.35f + 0.25f * h(seed, i, 13));
                float ox = ctx.camRight.x * Mth.cos(an) * rr + ctx.camUp.x * Mth.sin(an) * rr, oy = ctx.camRight.y * Mth.cos(an) * rr + ctx.camUp.y * Mth.sin(an) * rr, oz = ctx.camRight.z * Mth.cos(an) * rr + ctx.camUp.z * Mth.sin(an) * rr;
                streak(buf, wx + ox, wy + oy, wz + oz, fx + ox, fy + oy, fz + oz, 0.07f, a(GOLD_L, 0.9f * sm));
            }
        }

        // 3. the impact frame: white-gold, held still for the stop
        if (hit) {
            float fl = 1f - smooth(STRIKE_IMPACT + STRIKE_HOLD, STRIKE_IMPACT + STRIKE_HOLD + 6f, age);
            if (fl > 0.01f) {
                float fk = drawnFist ? 0.62f : 1f;                                              // with the 3D fist the flash lets it show through
                flash(ctx, buf, e.x, e.y, e.z, 11f * (0.55f + 0.45f * fl), 0.1f, a(WHITE, 0.8f * fk * fl));
                flash(ctx, buf, e.x, e.y, e.z, 6.5f * (0.5f + 0.5f * fl), 0.78f, a(GOLD_L, 0.7f * fk * fl));
                glow(ctx, buf, e.x, e.y, e.z, 4.5f * fl, GOLD, 0.8f * fl);
            }
            // 4. the air cracks like glass: a web in a disc facing the caster, two layers, lit from within
            VfxPose wp = across(e.x, e.y, e.z, -nx, -ny, -nz);
            float wr = 1.4f + 4.6f * eo(since / 5f);
            float wa = smooth(0f, 1.2f, since) * (1f - smooth(20f, 34f, since));
            float rw = h(seed, 0, 14) * Mth.TWO_PI;
            if (wa > 0.01f && buf.hasBudget(16)) {
                buf.plane(WEB, VfxBlend.ALPHA, wp.spin(rw), wr, a(WHITE, 0.95f * wa));
                buf.plane(WEB, VfxBlend.ADD, wp.lift(0.01f).spin(rw), wr, a(mix(WHITE, GOLD, smooth(0f, 12f, since)), 0.9f * wa));
                buf.plane(WEB, VfxBlend.ALPHA, wp.lift(0.02f).spin(rw + 2.3f), wr * 0.66f, a(WHITE, 0.8f * wa));
                buf.plane(WEB, VfxBlend.ADD, wp.lift(0.03f).spin(rw + 2.3f), wr * 0.66f, a(GOLD, 0.7f * wa));
            }
            // shards of space fly out of the web
            int nsh = ctx.seg(10, 5);
            for (int i = 0; i < nsh && buf.hasBudget(4); i++) {
                float tt = since - 1f - h(seed, i, 15) * 2f;
                if (tt <= 0f) continue;
                float p = Mth.clamp(tt / 22f, 0f, 1f);
                float an = Mth.TWO_PI * (i + h(seed, i, 16)) / nsh;
                float sp = 0.22f + 0.4f * h(seed, i, 17);
                float rr = 1.2f + sp * tt * (1f - 0.3f * p);
                float px = e.x + (wp.right().x * Mth.cos(an) + wp.up().x * Mth.sin(an)) * rr - nx * tt * 0.12f, py = e.y + (wp.right().y * Mth.cos(an) + wp.up().y * Mth.sin(an)) * rr - ny * tt * 0.12f - 0.012f * tt * tt, pz = e.z + (wp.right().z * Mth.cos(an) + wp.up().z * Mth.sin(an)) * rr - nz * tt * 0.12f;
                shard(ctx, buf, i, px, py, pz, 0.55f + 0.7f * h(seed, i, 18), tt * 0.2f * (i % 2 == 0 ? 1f : -1f) + h(seed, i, 19) * 6f, a(mix(WHITE, GOLD, p), (1f - p * p) * 0.95f));
            }
            // 5. the shock: two rings across the direction of the blow, a wave, radial lines
            float r0 = eo(since / 16f), r1 = eo((since - 3f) / 16f);
            ringAt(buf, across(e.x, e.y, e.z, nx, ny, nz), 1.0f + 11f * r0, a(GOLD, (1f - r0 * r0) * 0.95f * fade));
            ringAt(buf, across(e.x, e.y, e.z, nx, ny, nz).lift(0.01f), 0.7f + 8f * r1, a(IVORY, (1f - r1 * r1) * 0.8f * fade));
            waveAt(buf, VfxBlend.ADD, across(e.x, e.y, e.z, nx, ny, nz).lift(0.02f), (1.0f + 11f * r0) * 0.97f, a(BRONZE, (1f - r0) * 0.6f * fade));
            int nrl = ctx.seg(12, 6);
            for (int i = 0; i < nrl && buf.hasBudget(4); i++) {
                float p = (since - h(seed, i, 20) * 2f) / 12f;
                if (p <= 0f || p >= 1f) continue;
                float an = Mth.TWO_PI * (i + h(seed, i, 21) * 0.6f) / nrl;
                float head = 1.5f + (3.5f + 6f * h(seed, i, 22)) * eo(p), tail = Math.max(0.8f, head - 2.2f - 2f * h(seed, i, 23));
                float cx = Mth.cos(an), sy = Mth.sin(an);
                streak(buf, e.x + (wp.right().x * cx + wp.up().x * sy) * tail, e.y + (wp.right().y * cx + wp.up().y * sy) * tail, e.z + (wp.right().z * cx + wp.up().z * sy) * tail,
                        e.x + (wp.right().x * cx + wp.up().x * sy) * head, e.y + (wp.right().y * cx + wp.up().y * sy) * head, e.z + (wp.right().z * cx + wp.up().z * sy) * head, 0.08f + 0.05f * h(seed, i, 24), a(i % 2 == 0 ? WHITE : GOLD, 0.95f * (1f - p * p)));
            }
            // 6. the ground under the caster shakes: a ring of dust rolls out, cracks open
            VfxPose floor = ground(c.x, gy + 0.05f, c.z);
            float gr = eo(since / 18f);
            if (since > 0.5f) {
                ringAt(buf, floor, 1.5f + 9f * gr, a(GOLD, (1f - gr * gr) * 0.7f * fade));
                cracksAt(buf, floor.lift(0.004f).spin(h(seed, 1, 25) * Mth.TWO_PI), 3.8f * eo(since / 6f), 0.85f * (1f - smooth(10f, 26f, since)), a(heat(since / 26f), 0.75f * (1f - smooth(6f, 24f, since))));
            }
            if (near > 0.01f) {
                int nd = ctx.seg(8, 4);
                for (int i = 0; i < nd && buf.hasBudget(4); i++) {
                    float p = Mth.clamp(since / 20f, 0f, 1f);
                    if (p >= 1f || since <= 0.5f) break;
                    float an = Mth.TWO_PI * (i + h(seed, i, 26) * 0.6f) / nd;
                    float rr = (1.5f + 9f * gr) * 0.9f;
                    dust(ctx, buf, i, c.x + Mth.cos(an) * rr, gy + 0.35f + 0.8f * p, c.z + Mth.sin(an) * rr, 1.3f + 0.7f * h(seed, i, 27), h(seed, i, 28) * 6f + age * 0.03f, 0.7f * (1f - p * p) * near);
                }
                int ne = ctx.seg(6, 3);
                for (int i = 0; i < ne && buf.hasBudget(4); i++) {
                    float u = Mth.frac(since / 26f + h(seed, i, 29));
                    float an = h(seed, i, 30) * Mth.TWO_PI, rr = 0.5f + 2.4f * h(seed, i, 31);
                    mote(ctx, buf, e.x + Mth.cos(an) * rr * 0.6f, e.y + 1.5f - u * 3.5f, e.z + Mth.sin(an) * rr * 0.6f, 0.26f, a(mix(WHITE, EMBER, u), Mth.sin(u * Mth.PI) * 0.9f * near * (1f - smooth(22f, 34f, since))));
                }
            }
        }
    }

    /**
     * 3.20.0, World-Breaking Strike: the clenched 3D fist ({@link HerculesModels#FIST}, at most 30 quads) with its centre at (fx, fy, fz), knuckles toward (nx, ny, nz), back of the hand up. {@code fs} is the sprite size of 3.19.0
     * (the fist is about 0.46 of it long per unit); it shakes with a roll while it gathers, glows hotter as it crosses ({@code trav}) and cools after the impact frame ({@code cool}). Returns the quads drawn (0 = draw the sprite).
     */
    private int punchFist(VfxVertexBuffer buf, float fx, float fy, float fz, float nx, float ny, float nz, float fs, float age, float trav, float cool, int tint, long seed) {
        float S = Math.max(0.3f, fs * 0.46f);
        float shake = smooth(2f, 14f, age) * (1f - trav);
        float roll = (h(seed, (int) age, 5) - 0.5f) * 0.16f * shake;
        HerculesModels.Posed p = mesh.posed();
        p.clearPose();
        p.look(nx, ny, nz, 0f, 1f, 0f, roll, S);
        float heat = Math.max(0.15f + 0.3f * smooth(0f, 14f, age) * (1f - trav), 0.95f * trav) * (1f - 0.7f * cool);
        return mesh.draw(buf, MODEL_ATLAS, HerculesModels.FIST, fx - nx * 0.2f * S, fy - ny * 0.2f * S, fz - nz * 0.2f * S, tint, heat);
    }

    // ================================================================== HERCULES_SHOCKWAVE
    private void shockwave(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 0, 4);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        float Rv = Mth.clamp(inst.power, 1.5f, 8f);
        Vector3f g = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(40f, 80f, horizontal(g));
        float gy = g.y + 0.05f;
        VfxPose floor = ground(g.x, gy, g.z);
        float p = eo(age / 8f);

        ringAt(buf, floor.lift(0.02f), Rv * p, a(GOLD, (1f - p * p) * 0.95f * fade));
        waveAt(buf, VfxBlend.ADD, floor, Rv * p * 0.95f, a(BRONZE, (1f - p) * 0.6f * fade));
        float fl = 1f - smooth(0f, 4f, age);
        if (fl > 0.01f) {
            flash(ctx, buf, g.x, gy + 0.3f, g.z, Rv * 1.5f * (0.5f + 0.5f * fl), 0.2f, a(WHITE, fl));
            glow(ctx, buf, g.x, gy + 0.3f, g.z, Rv * 0.6f * fl, GOLD, 0.9f * fl);
        }
        cracksAt(buf, floor.lift(0.004f).spin(h(seed, 0, 1) * Mth.TWO_PI), Rv * 0.85f * eo(age / 3f), 0.85f * (1f - smooth(5f, 12f, age)), a(heat(age / 12f), 0.7f * (1f - smooth(4f, 12f, age))));
        if (near > 0.01f) {
            int nd = ctx.seg(4, 2);
            for (int i = 0; i < nd && buf.hasBudget(4); i++) {
                float an = Mth.TWO_PI * (i + h(seed, i, 2) * 0.6f) / nd;
                float rr = Rv * p * 0.85f;
                dust(ctx, buf, i, g.x + Mth.cos(an) * rr, gy + 0.2f + 0.5f * p, g.z + Mth.sin(an) * rr, 0.6f + Rv * 0.16f, h(seed, i, 3) * 6f, 0.75f * (1f - p * p) * near);
            }
            int ns = ctx.seg(4, 2);
            for (int i = 0; i < ns && buf.hasBudget(4); i++) {
                float tt = age - 0.3f;
                if (tt <= 0f) break;
                float an = Mth.TWO_PI * (i + h(seed, i, 4)) / ns;
                float y = (0.2f + 0.2f * h(seed, i, 5)) * tt - 0.018f * tt * tt;
                if (y < 0f) continue;
                slab(ctx, buf, i, g.x + Mth.cos(an) * (0.3f + 0.1f * tt * Rv * 0.3f), gy + 0.1f + y, g.z + Mth.sin(an) * (0.3f + 0.1f * tt * Rv * 0.3f), 0.3f + 0.25f * h(seed, i, 6), tt * 0.4f * (i % 2 == 0 ? 1f : -1f), 1f - smooth(8f, 12f, age));
            }
        }
    }

    // ================================================================== HERCULES_BODY
    private void body(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 0, 3);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        float S = Mth.clamp(inst.power, 0.6f, 4f);
        float hs = Mth.clamp(S / 1.5f, 0.6f, 2.2f);
        Vector3f c = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(40f, 90f, horizontal(c));
        float gy = c.y - 0.9f * hs;
        float hit = smooth(0f, 1.2f, age) * (1f - smooth(2.5f, 9f, age));      // the flash of the plates
        float ring = eo(age / 8f);

        // 1. the plates: bronze plates over the whole body for an instant, a second set that rings out wider and fades first
        float hw = 0.64f * hs, top = c.y + 1.12f * hs, bot = c.y - 1.05f * hs;
        ribbon(buf, PLATES, VfxBlend.ADD, c.x, bot, c.z, c.x, top, c.z, hw, hw, 1f, 0f, a(WHITE, 0.95f * hit * fade), a(WHITE, 0.95f * hit * fade));
        float gh = smooth(0.5f, 1.5f, age) * (1f - smooth(1.5f, 7f, age));
        ribbon(buf, PLATES, VfxBlend.ADD, c.x, c.y - 1.05f * hs * (1.15f + 0.3f * ring), c.z, c.x, c.y + 1.12f * hs * (1.1f + 0.3f * ring), c.z, hw * (1.2f + 0.5f * ring), hw * (1.2f + 0.5f * ring), 1f, 0f, a(GOLD, 0.6f * gh * fade), a(GOLD, 0.6f * gh * fade));
        glow(ctx, buf, c.x, c.y, c.z, 1.7f * hs, EMBER, 0.55f * hit * fade);
        float fl = smooth(0f, 1f, age) * (1f - smooth(1f, 5f, age));
        if (fl > 0.01f) flash(ctx, buf, c.x, c.y + 0.1f * hs, c.z, 2.8f * hs * (0.5f + 0.5f * fl), age * 0.1f, a(WHITE, fl));

        // 2. the ring that rings out of the plates, facing the camera, and the ring on the floor
        VfxPose rp = across(c.x, c.y, c.z, -c.x, -c.y, -c.z);
        ringAt(buf, rp, (0.5f + 2.2f * S * 0.5f) * ring, a(GOLD, (1f - ring) * 0.9f * fade));
        ringAt(buf, rp.lift(0.01f), (0.4f + 1.5f * S * 0.5f) * eo((age - 1.5f) / 7f), a(IVORY, (1f - eo((age - 1.5f) / 7f)) * 0.7f * fade));
        ringAt(buf, ground(c.x, gy + 0.05f, c.z), (0.6f + 1.3f * S * 0.5f) * ring, a(BRONZE, (1f - ring) * 0.7f * fade));

        // 3. sparks fly off the plates: short streaks out of the chest, and a few motes
        int ns = ctx.seg(8, 4);
        for (int i = 0; i < ns && buf.hasBudget(4); i++) {
            float p = (age - 0.5f - h(seed, i, 1) * 1.5f) / 7f;
            if (p <= 0f || p >= 1f) continue;
            float an = Mth.TWO_PI * (i + h(seed, i, 2) * 0.7f) / ns;
            float head = (0.55f + (0.9f + 0.9f * h(seed, i, 3)) * eo(p)) * hs, tail = Math.max(0.2f * hs, head - (0.5f + 0.5f * h(seed, i, 4)) * hs * (1f - p));
            float cx = Mth.cos(an), sy = Mth.sin(an);
            streak(buf, c.x + (ctx.camRight.x * cx + ctx.camUp.x * sy) * tail, c.y + (ctx.camRight.y * cx + ctx.camUp.y * sy) * tail, c.z + (ctx.camRight.z * cx + ctx.camUp.z * sy) * tail,
                    c.x + (ctx.camRight.x * cx + ctx.camUp.x * sy) * head, c.y + (ctx.camRight.y * cx + ctx.camUp.y * sy) * head, c.z + (ctx.camRight.z * cx + ctx.camUp.z * sy) * head, 0.05f * hs + 0.02f, a(i % 2 == 0 ? GOLD_L : EMBER, 0.95f * (1f - p * p)));
        }
        if (near > 0.01f) {
            int nm = ctx.seg(4, 2);
            for (int i = 0; i < nm && buf.hasBudget(4); i++) {
                float p = Mth.clamp((age - 1f) / 10f, 0f, 1f);
                float an = Mth.TWO_PI * (i + h(seed, i, 5)) / nm;
                float rr = (0.5f + 0.9f * h(seed, i, 6)) * hs * (0.4f + 0.6f * p);
                mote(ctx, buf, c.x + ctx.camRight.x * Mth.cos(an) * rr, c.y + Mth.sin(an) * rr * 0.9f - 0.5f * p, c.z + ctx.camRight.z * Mth.cos(an) * rr, 0.22f * hs, a(BRONZE, (1f - p) * 0.9f * near));
            }
        }
    }

    // ================================================================== HERCULES_MOMENTUM
    private void momentum(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 4, 10);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        int stage = Mth.clamp(Math.round(inst.power), 1, 3);
        Vector3f c = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(40f, 90f, horizontal(c));
        float gy = c.y - 0.9f;
        // the way the runner goes: its body facing when it is followed, else from -> to, else away from the camera
        float yaw = inst.followYaw(ctx);
        float dx, dz;
        if (!Float.isNaN(yaw)) {
            float r = yaw * Mth.DEG_TO_RAD;
            dx = -Mth.sin(r);
            dz = Mth.cos(r);
        } else {
            Vector3f e = ctx.rel(inst.to(ctx));
            float ex = e.x - c.x, ez = e.z - c.z, el = Mth.sqrt(ex * ex + ez * ez);
            if (el > 0.5f) {
                dx = ex / el;
                dz = ez / el;
            } else {
                float hd = horizontal(c);
                dx = hd > 1e-3f ? c.x / hd : 0f;
                dz = hd > 1e-3f ? c.z / hd : 1f;
            }
        }
        float sx = -dz, sz = dx;                                                   // sideways

        // 1. the afterimages: bronze bodies that trail behind and fade
        int ng = stage + 1;
        float fr = Mth.frac(age / 7f);
        for (int k = 0; k < ng && buf.hasBudget(4); k++) {
            float d = 0.85f * (k + fr);
            float al = (1f - (k + fr) / ng) * 0.55f * fade * smooth(0f, 0.3f, k + fr);
            ribbon(buf, PLATES, VfxBlend.ADD, c.x - dx * d, c.y - 1.0f, c.z - dz * d, c.x - dx * d, c.y + 1.05f, c.z - dz * d, 0.44f, 0.44f, 1f, 0f, a(k % 2 == 0 ? GOLD : BRONZE, al), a(k % 2 == 0 ? GOLD : BRONZE, al));
        }

        // 2. the speed lines: streaks that rush backward past the body, more of them and longer at a higher stage
        int nl = Math.min(ctx.seg(16, 4), 3 + 4 * stage);
        for (int i = 0; i < nl && buf.hasBudget(4); i++) {
            float s = Mth.frac(age / (5f - stage * 0.6f) + h(seed, i, 1));
            float an = h(seed, i, 2) * Mth.TWO_PI, rr = 0.35f + 0.75f * h(seed, i, 3);
            float side = Mth.cos(an) * rr, up = (0.1f + 1.9f * h(seed, i, 4)) - 1f + Mth.sin(an) * 0.2f;
            float len = 1.2f + 0.9f * stage + 1.2f * h(seed, i, 5);
            float head = 0.4f + 5.5f * s - 1.2f, tail = head + len;                  // behind the body: along -dir
            float al = Mth.sin(s * Mth.PI) * fade;
            streak(buf, c.x - dx * tail + sx * side, c.y + up, c.z - dz * tail + sz * side, c.x - dx * head + sx * side, c.y + up, c.z - dz * head + sz * side, 0.05f + 0.015f * stage, a(i % 3 == 0 ? IVORY : (stage == 3 && i % 2 == 0 ? EMBER : GOLD), 0.9f * al));
        }

        // 3. the wind ring and the dust at the feet
        VfxPose floor = ground(c.x, gy + 0.05f, c.z);
        ringAt(buf, floor.spin(age * 0.18f), 1.0f + 0.25f * stage + 0.12f * Mth.sin(age * 0.5f), a(GOLD, 0.6f * fade));
        waveAt(buf, VfxBlend.ADD, floor.lift(0.01f), 1.8f + 0.5f * stage, a(BRONZE, 0.4f * fade));
        glow(ctx, buf, c.x, c.y, c.z, 1.6f, stage == 3 ? EMBER : GOLD, 0.35f * fade);
        if (near > 0.01f) {
            int nd = stage * 2;
            for (int k = 0; k < nd && buf.hasBudget(4); k++) {
                float p = Mth.frac(age / 8f + k / (float) nd);
                float d = 0.4f + 3.2f * p;
                dust(ctx, buf, k, c.x - dx * d + sx * (h(seed, k, 6) - 0.5f) * 0.8f, gy + 0.2f + 0.5f * p, c.z - dz * d + sz * (h(seed, k, 6) - 0.5f) * 0.8f, 0.6f + 0.9f * p, h(seed, k, 7) * 6f, 0.7f * (1f - p) * fade * near);
            }
            int ne = stage * 2;
            for (int i = 0; i < ne && buf.hasBudget(4); i++) {
                float u = Mth.frac(age / 20f + h(seed, i, 8));
                float d = 0.3f + 2.4f * u;
                mote(ctx, buf, c.x - dx * d + sx * (h(seed, i, 9) - 0.5f) * 1.4f, gy + 0.3f + 1.6f * h(seed, i, 10) + 0.3f * u, c.z - dz * d + sz * (h(seed, i, 9) - 0.5f) * 1.4f, 0.22f, a(mix(GOLD, EMBER, u), Mth.sin(u * Mth.PI) * 0.9f * fade * near));
            }
        }
        // 4. at the highest stage the runner burns: a tongue of flame trails off the back
        if (stage == 3 && buf.hasBudget(4)) {
            ribbon(buf, FLAME, VfxBlend.ADD, c.x - dx * 0.3f, c.y + 0.1f, c.z - dz * 0.3f, c.x - dx * 1.6f, c.y + 1.3f, c.z - dz * 1.6f, 0.4f, 0.14f, 1f, 0f, a(EMBER, 0.7f * fade), a(CRIMSON, 0.3f * fade));
        }
    }
}
