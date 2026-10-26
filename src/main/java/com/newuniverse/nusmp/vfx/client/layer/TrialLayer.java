package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Israfil, Lord of Trials (Veldanava): the angel of trials, space and labyrinths. Deep indigo and midnight teal, bright cyan-white edges, small brass accents; maze lines, coordinate ticks and crosshairs,
 * folded-space glass shards, a trumpet and its sound rings, ghostly afterimages. Written from scratch for this mod (textures: {@code tools/gen_trial_vfx_textures.py} -> {@code textures/particle/trial_*.png});
 * it shares nothing with any other mod, looks nothing like the gold, steel and clock of {@link JudgmentLayer}, and does not touch the Time Magic layer. The colour of the packet is ignored: the palette is the layer's own.
 * <ul>
 *   <li>TRIAL_LABYRINTH: Dimensional Labyrinth. Sent following the trapped creature: from = its centre (the floor is drawn 0.9 below it), power = cage radius (~4.5), 160 ticks. A cage of tall translucent
 *       maze panes stands in a ring (10 at full detail), slides slowly and re-shuffles every 20 ticks (position, height, which maze it shows, a white flash); a maze disc on the floor, a coordinate seal, a ring at the
 *       top, floating corner shards and motes. Fades in over 8 ticks, out over 10.</li>
 *   <li>TRIAL_GRAVITY: Weight of Sins. Sent following the target: from = its feet, power = size (~1.5), 40 ticks. A dark disc presses into the ground while a coordinate plate comes down on it (it speeds up),
 *       3-4 pillars of heavy downward light streaks and 3 chains of tally marks sink, the ground cracks, and at 70 % of the time a thud ring, flash and dust end it.</li>
 *   <li>TRIAL_COLLAPSE: Spatial Collapse. Sent at the target point: from = the ground point, to = from + (0,1,0), power = implosion radius (~6.5), 120 ticks. Phase 1 (18 ticks): 12 space shards and 3 rings are pulled
 *       to the centre; phase 2: a pinch flash, a shock ring on the ground and one in the air (a camera shake); phase 3, to the end: the Nihility Collapse field, a dark void disc with a violet-black sphere and an inverted-colour
 *       rim with a cyan edge, angular ground cracks that creep outwards, orbiting shards, motes falling in, a slow pulse. It thins out over the last 24 ticks.</li>
 *   <li>TRIAL_SHIFT: Coordinate Shift. from = where the caster was, to = where they arrive (1-48 blocks), power = 1, 14 ticks. A coordinate ring (flat and upright) flips shut at the start and unfolds at the end, a dashed
 *       folded line runs between them, swapped humanoid afterimages light up in turn, a ring and flares mark the arrival. The segments of the line and the ghosts shrink with the length and the distance.</li>
 *   <li>TRIAL_TRUMPET: The Grand Trumpet. Sent at the caster: from = its centre, power = radius (100 in game), 50 ticks. A golden horn appears above the caster and bursts into a cone of light rays and a shaft; 5 sound rings
 *       reach the radius in ~30 ticks on the ground (a ring far from the camera fades) and 3 round rings expand as the dome; glass shards of broken barriers fly out; a flash and a camera shake (VfxShake falls off by itself, to nothing at 32 blocks).</li>
 *   <li>TRIAL_RIFT: Rift Walk, held. Sent following the player (re-sent while held): from = its centre, power = 1, 100 ticks. Shimmering vertical rift lines round the body, glitch slices of the silhouette, a trail of afterimage
 *       columns behind the body (behind the facing of the followed entity), pulsing rings and a grid at the feet, motes drifting through.</li>
 * </ul>
 * <b>Budgets (rule 11).</b> Every effect draws through the 400-vertex (100-quad) activation cap of the vertex buffer and is far below it; counts are at full detail, {@code ctx.seg} thins them by distance and the particle
 * setting (minimums in brackets); decorative extras (motes, floating shards) also drop out with the camera distance. Worst case quads per effect per frame (measured in the preview, see tools/vfx_preview/scenes.d/trial.json):
 * LABYRINTH 43, GRAVITY 31, COLLAPSE 35, SHIFT 35 (30 for a short shift), TRUMPET 62, RIFT 27 (39 ages per scene scanned; the cap is 100). No effect allocates more than a few small vectors and poses per frame (the same as the other layers); nothing is kept between frames, so nothing has to be cleaned
 * up on logout, death or dimension change; the server only sends one packet per effect (VfxSpawn) and nothing here runs on the server. A ring or plane of any radius is one quad.
 * <p><b>3.21.0: 3D models</b> ({@link IsrafilModels}, drawn by {@link IsrafilMeshDraw}, switch {@code visual.israfilModels}, doc {@code docs/vfx_remake/israfil_models.md}). Near the camera (and with the setting on and the
 * particle setting not minimal) the flat parts are replaced: the Grand Trumpet is a real horn that swings up, blares and cracks into 3D glass shards (the rays leave its bell), the Weight of Sins slams an engraved slab on
 * three chains with a turning rune ring, the labyrinth also raises up to five 3D maze walls and pillars that slide at every re-shuffle, the collapse pulls 3D crystal shards into a faceted dark core, the coordinate shift
 * has two 3D gates. The 3.20.0 draw calls are all still here as the fallback (setting off, far away, no room in the budget). Worst case quads per effect with the models (preview, every tick or two, big radii too):
 * LABYRINTH 74, GRAVITY 60, COLLAPSE 50, SHIFT 54, TRUMPET 98, RIFT 27. A model asks {@code hasBudget} for all its visible faces plus the room the rest of the effect needs and is skipped whole otherwise.
 */
public class TrialLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation RING = t("trial_ring");
    public static final ResourceLocation SOUND = t("trial_sound");
    public static final ResourceLocation MAZE = t("trial_maze");
    public static final ResourceLocation PANE = t("trial_pane");
    public static final ResourceLocation GRID = t("trial_grid");
    public static final ResourceLocation SHARD = t("trial_shard");
    public static final ResourceLocation FLARE = t("trial_flare");
    public static final ResourceLocation MOTE = t("trial_mote");
    public static final ResourceLocation BEAM = t("trial_beam");
    public static final ResourceLocation STREAK = t("trial_streak");
    public static final ResourceLocation CHAIN = t("trial_chain");
    public static final ResourceLocation HORN = t("trial_horn");
    public static final ResourceLocation CRACKS = t("trial_cracks");
    public static final ResourceLocation VOID = t("trial_void");
    public static final ResourceLocation INVERT = t("trial_invert");
    public static final ResourceLocation GHOST = t("trial_ghost");
    public static final ResourceLocation RIFT = t("trial_rift");
    public static final ResourceLocation DASH = t("trial_dash");
    /** 3.21.0: the material atlas of the Israfil 3D models (tools/gen_israfil_model_textures.py). */
    public static final ResourceLocation MODEL_ATLAS = t("israfil_model_atlas");

    private static final int WHITE = 0xFFFFFFFF;
    private static final int ICE = 0xFFD8FBFF;
    private static final int CYAN = 0xFF70E8F7;
    private static final int TEAL = 0xFF1EA8BE;
    private static final int INDIGO = 0xFF4A4CCD;
    private static final int DEEP = 0xFF1A1760;
    private static final int VIOLET = 0xFF9868EE;
    private static final int BRASS = 0xFFD6A03E;
    private static final int BRASS_HI = 0xFFFFE294;
    private static final int VOID_C = 0xFF0B0720;
    private static final int CRACK_C = 0xFF04030C;

    /** Scratch corners of the quad being drawn (the layer draws on the render thread only, one effect at a time). */
    private final Vector3f q0 = new Vector3f(), q1 = new Vector3f(), q2 = new Vector3f(), q3 = new Vector3f();

    /** 3.21.0: the models are drawn within these distances (blocks) of the camera, further out the flat sprites and planes of 3.20.0 stand in. */
    static final float HORN_RANGE = 80f, PLATE_RANGE = 52f, MAZE_RANGE = 60f, CORE_RANGE = 64f, SHARD_RANGE = 44f, GATE_RANGE = 44f;
    /** Quads of the 100-quad activation budget kept free after the 3D horn while the light bursts (the rays, the rings, the glass shards): the horn is only drawn when it and these fit. */
    static final int HORN_KEEP = 44;
    /** The poser / drawer of the 3D models (one per layer; render thread only). */
    private final IsrafilMeshDraw mesh = new IsrafilMeshDraw();
    /** The camera-relative mouth of the bell of the 3D horn, set by {@link #hornModel} for the rays that leave it. */
    private float hmx, hmy, hmz;
    /** The maze pieces of the labyrinth for the frame being drawn (position, height, angle, distance, kind); at most 5, filled and drawn in the same call. */
    private final float[] wx = new float[8], wz = new float[8], wh = new float[8], wa = new float[8], wd = new float[8];
    private final int[] wk = new int[8];

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.TRIAL_LABYRINTH, VfxShape.TRIAL_GRAVITY, VfxShape.TRIAL_COLLAPSE, VfxShape.TRIAL_SHIFT, VfxShape.TRIAL_TRUMPET, VfxShape.TRIAL_RIFT);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case TRIAL_LABYRINTH -> 160;
            case TRIAL_GRAVITY -> 40;
            case TRIAL_COLLAPSE -> 120;
            case TRIAL_SHIFT -> 14;
            case TRIAL_TRUMPET -> 50;
            default -> 100;
        };
    }

    @Override public int defaultColor(VfxShape s) { return CYAN; }

    /** The tick the implosion of the collapse turns into the pinch. */
    static final float PINCH = 18f;
    /** The share of the gravity effect after which the weight lands (the thud). */
    static final float THUD = 0.7f;

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.TRIAL_TRUMPET) VfxShake.add(inst.payload.from(), 2.2f, 18);
    }

    @Override
    public void onTick(VfxInstance inst) {
        if (inst.shape == VfxShape.TRIAL_COLLAPSE && inst.age() == (int) PINCH) VfxShake.add(inst.payload.from(), 2.0f, 14);
        if (inst.shape == VfxShape.TRIAL_GRAVITY && inst.age() == Math.round(inst.duration * THUD)) VfxShake.add(inst.payload.from(), 0.9f, 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case TRIAL_LABYRINTH -> labyrinth(inst, ctx, buf);
            case TRIAL_GRAVITY -> gravity(inst, ctx, buf);
            case TRIAL_COLLAPSE -> collapse(inst, ctx, buf);
            case TRIAL_SHIFT -> shift(inst, ctx, buf);
            case TRIAL_TRUMPET -> trumpet(inst, ctx, buf);
            case TRIAL_RIFT -> rift(inst, ctx, buf);
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

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }

    /** The colour of a NEGATIVE (inverting) quad: the grey level is the strength, the alpha is ignored by that blend. */
    private static int negative(float g) {
        float k = Mth.clamp(g, 0f, 1f);
        return 0xFF000000 | ((int) (k * 205f) << 16) | ((int) (k * 160f) << 8) | (int) (k * 255f);
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
     * A ribbon from a to b that turns its face to the camera (the camera is at the origin: positions are camera-relative). Half widths hwA / hwB; the texture runs V = vA at a to V = vB at b
     * (U across); cA / cB are the vertex colours at the two ends (the alpha fades along the ribbon).
     */
    private void ribbon(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float ax, float ay, float az, float bx, float by, float bz,
                        float hwA, float hwB, float vA, float vB, int cA, int cB) {
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

    /** An upright sprite of half size hw x hh round (cx, cy, cz), spanned by the unit vectors r (right) and u (up); the picture's top is at +u. */
    private void sprite(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, float cx, float cy, float cz, float rx, float ry, float rz,
                        float ux, float uy, float uz, float hw, float hh, int argb) {
        quad(buf, tex, blend,
                cx - rx * hw - ux * hh, cy - ry * hw - uy * hh, cz - rz * hw - uz * hh,
                cx + rx * hw - ux * hh, cy + ry * hw - uy * hh, cz + rz * hw - uz * hh,
                cx + rx * hw + ux * hh, cy + ry * hw + uy * hh, cz + rz * hw + uz * hh,
                cx - rx * hw + ux * hh, cy - ry * hw + uy * hh, cz - rz * hw + uz * hh,
                0, 0, 1, 1, argb, argb);
    }

    private void flare(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, float rot, int argb) {
        if (size <= 0.001f || (argb >>> 24) < 3) return;
        q0.set(x, y, z);
        buf.billboard(ctx, FLARE, VfxBlend.ADD, q0, size, rot, argb);
    }

    private void mote(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, int argb) {
        if (size <= 0.001f || (argb >>> 24) < 3) return;
        q0.set(x, y, z);
        buf.billboard(ctx, MOTE, VfxBlend.ADD, q0, size, 0f, argb);
    }

    private void shard(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, float rot, int argb) {
        if (size <= 0.001f || (argb >>> 24) < 3) return;
        q0.set(x, y, z);
        buf.billboard(ctx, SHARD, VfxBlend.ADD, q0, size, rot, argb);
    }

    private void glow(VfxRenderContext ctx, VfxVertexBuffer buf, float x, float y, float z, float size, int argb, float intensity) {
        if (intensity <= 0.01f) return;
        q0.set(x, y, z);
        VfxBloom.glow(ctx, buf, q0, size, argb, intensity);
    }

    /** The ring texture whose bright circle lies at {@code radius}. */
    private static void ringAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(RING, VfxBlend.ADD, pose, radius / 0.965f, argb);
    }

    /** The sound ring (a bright front with echoes inside it) whose front lies at {@code radius}. */
    private static void soundAt(VfxVertexBuffer buf, VfxPose pose, float radius, int argb) {
        if (radius <= 0.02f || (argb >>> 24) < 3) return;
        buf.plane(SOUND, VfxBlend.ADD, pose, radius / 0.955f, argb);
    }

    /** A pose with its right axis scaled by k (a ring seen while it flips over: k = cos of the flip angle). */
    private static VfxPose flipped(VfxPose p, float k) {
        return new VfxPose(p.origin(), new Vector3f(p.right()).mul(k), p.up(), p.normal());
    }

    /** A pose standing at (x, y, z) that faces the camera (the camera is at the origin). */
    private static VfxPose faceCamera(float x, float y, float z) {
        return VfxPose.facing(new Vector3f(x, y, z), new Vector3f(-x, -y, -z));
    }

    // ------------------------------------------------------------------ 3.21.0: the 3D models
    /** True when the 3D models are drawn here: the setting is on, the particle setting is not minimal and the thing is within {@code range} blocks of the camera (else the 3.20.0 sprites stand in). */
    private static boolean modelsOn(VfxRenderContext ctx, float x, float y, float z, float range) {
        return IsrafilModels.enabled() && ctx.detail >= 0.5f && x * x + y * y + z * z < range * range;
    }

    /**
     * The Grand Trumpet ({@link IsrafilModels#HORN}, at most 68 quads (about 45 from below)): it swings up from the caster's side to stand over the head (bell up, leaning a little toward the camera), blares (it shakes and glows brighter),
     * and at tick 13 cracks and breaks into 3D glass shards. Always sets the mouth point (hmx, hmy, hmz) the rays leave from. Returns false when the model could not be drawn (the caller draws the flat horn).
     */
    private boolean hornModel(VfxVertexBuffer buf, VfxRenderContext ctx, float age, long seed, float gx, float gy, float gz, float rx, float rz, float tcx, float tcz) {
        float w = VfxAnim.easeOutBack(Mth.clamp(age / 6f, 0f, 1f));
        float phi = 1.45f * (1f - w);
        float sp = Mth.sin(phi);
        float dx = rx * sp + tcx * 0.2f * w, dy = Mth.cos(phi), dz = rz * sp + tcz * 0.2f * w;
        float dl = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= dl;
        dy /= dl;
        dz /= dl;
        float blare = smooth(4.5f, 7f, age) * (1f - smooth(13.4f, 14.2f, age));
        float crack = smooth(9.5f, 13.4f, age) * (1f - smooth(13.4f, 14.2f, age));
        float side = 1.0f * (1f - w);
        float cx = gx + rx * side + crack * 0.09f * Mth.sin(age * 5.1f);
        float cy = gy + 1.0f + 3.0f * w + crack * 0.07f * Mth.sin(age * 6.3f + 1f);
        float cz = gz + rz * side + crack * 0.09f * Mth.sin(age * 4.7f + 2f);
        float s = 1.2f * (0.35f + 0.65f * Mth.clamp(age / 4f, 0f, 1f)) * (1f + 0.03f * blare * Mth.sin(age * 2.7f));
        hmx = cx + dx * IsrafilModels.HORN_MOUTH_Z * s;
        hmy = cy + dy * IsrafilModels.HORN_MOUTH_Z * s;
        hmz = cz + dz * IsrafilModels.HORN_MOUTH_Z * s;
        float alpha = Math.min(Mth.clamp(age / 3f, 0f, 1f), 1f - smooth(13.2f, 15.4f, age));
        int drawn = 0;
        if (alpha > 0.02f) {
            IsrafilModels.Posed p = mesh.posed();
            p.clearPose();
            p.look(dx, dy, dz, -cx, -cy, -cz, 0.05f * crack * Mth.sin(age * 3.7f), s);
            drawn = mesh.draw(buf, MODEL_ATLAS, IsrafilModels.HORN, cx, cy, cz, a(WHITE, 0.97f * alpha), 0.08f + 0.22f * blare + 0.4f * crack, age < 13.4f ? HORN_KEEP : 30);   // the rays and rings that follow keep their room: an extreme view (from above) that would not leave it gets the flat horn
            if (drawn > 0) glow(ctx, buf, hmx, hmy, hmz, 2.6f * smooth(4f, 8f, age), CYAN, 0.45f * alpha);
        }
        if (age >= 13.2f) hornShards(buf, ctx, age, seed, cx, cy, cz, dx, dy, dz, s);
        return drawn > 0 || age > 15.4f;
    }

    /** The horn cracks: up to six 3D glass shards ({@link IsrafilModels#SHARD}, at most 3 quads each) fly out from along its length, tumbling, and fall. */
    private void hornShards(VfxVertexBuffer buf, VfxRenderContext ctx, float age, long seed, float cx, float cy, float cz, float dx, float dy, float dz, float s) {
        int n = ctx.seg(4, 3);
        IsrafilModels.Posed p = mesh.posed();
        for (int i = 0; i < n; i++) {
            float born = 13.2f + 1.6f * h(seed, i, 61);
            float q = (age - born) / (16f + 8f * h(seed, i, 62));
            if (q <= 0f || q >= 1f) continue;
            float t = (-2.0f + 4.3f * h(seed, i, 63)) * s;
            float ph = h(seed, i, 64) * Mth.TWO_PI, el = (h(seed, i, 65) - 0.25f) * 1.1f;
            float dd = (3.5f + 5f * h(seed, i, 66)) * (1f - (float) Math.pow(1f - q, 2.0));
            float x = cx + dx * t + Mth.cos(ph) * Mth.cos(el) * dd, y = cy + dy * t + Mth.sin(el) * dd - 6f * q * q, z = cz + dz * t + Mth.sin(ph) * Mth.cos(el) * dd;
            float sc = (0.7f + 0.6f * h(seed, i, 67)) * (1f - 0.3f * q);
            p.clearPose();
            p.tumble(h(seed, i, 68) - 0.5f, 1f, h(seed, i, 69) - 0.5f, age * (0.15f + 0.4f * h(seed, i, 70)) + i * 1.3f, sc, sc, sc);
            mesh.draw(buf, MODEL_ATLAS, IsrafilModels.SHARD, x, y, z, a(WHITE, Math.min(q * 8f, 1f) * (1f - smooth(0.6f, 1f, q)) * 0.95f), 0.45f * (1f - q));
        }
    }

    /**
     * The Weight Plate ({@link IsrafilModels#PLATE}, at most 36 quads): an engraved hexagonal slab on three chains under a turning rune ring, wobbling as it falls, slamming down on the target's feet (a small rebound and
     * a flash at the thud). Returns the quads drawn (0 = the caller draws the flat coordinate plate).
     */
    private int weightPlate(VfxVertexBuffer buf, VfxRenderContext ctx, float age, float press, float tt, float fade, float x, float y, float z, float S) {
        float k = S * (1.95f - 0.2f * press) * 0.95f;
        float lift = tt > 0f ? 0.2f * S * Mth.sin(Mth.clamp(tt * 5f, 0f, 1f) * Mth.PI) * (1f - tt) : 0f;
        IsrafilModels.Posed p = mesh.posed();
        p.clearPose();
        p.spread()[IsrafilModels.P_RING] = age * 0.13f;
        p.upright(age * 0.045f, (1f - press) * 0.16f, Mth.cos(age * 0.31f), Mth.sin(age * 0.31f), k);
        float heat = 0.18f + 0.3f * press + (tt > 0f ? 0.7f * (1f - Mth.clamp(tt * 2.2f, 0f, 1f)) : 0f);
        int q = mesh.draw(buf, MODEL_ATLAS, IsrafilModels.PLATE, x, y + lift, z, a(WHITE, 0.97f * fade), heat, 12);
        if (q > 0) {
            float ay = y + lift + IsrafilModels.PLATE_APEX * k;
            ringAt(buf, VfxPose.ground(new Vector3f(x, ay, z)).spin(age * 0.13f), k * 0.5f, a(CYAN, 0.5f * fade));
            glow(ctx, buf, x, ay, z, k * 0.9f, CYAN, 0.45f * fade * (0.6f + 0.4f * press));
        }
        return q;
    }

    /**
     * The maze walls and pillars of the Dimensional Labyrinth: up to five {@link IsrafilModels#WALL_A} / {@link IsrafilModels#WALL_B} / {@link IsrafilModels#PILLAR} (at most 8 / 8 / 10 quads) rise round the trapped
     * creature (their maze faces toward it) and slide to new places and heights at every re-shuffle, drawn far to near. The panes stay as the rest of the cage.
     */
    private void mazePieces(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f c, float fy, float R, long seed, float age, int epoch, int e0, float mix, float rise, float flash, float fade) {
        int n = ctx.seg(5, 3);
        float spin = age * 0.004f;
        float dip = epoch == 0 ? 0f : 0.22f * Mth.sin(mix * Mth.PI);
        for (int k = 0; k < n; k++) {
            int i = Math.round(k * 10f / n) % 10;
            float dA = Mth.lerp(mix, (h(seed, i, e0 * 8 + 5) - 0.5f) * 0.5f, (h(seed, i, epoch * 8 + 5) - 0.5f) * 0.5f);
            float dR = Mth.lerp(mix, (h(seed, i, e0 * 8 + 6) - 0.5f) * 1.1f, (h(seed, i, epoch * 8 + 6) - 0.5f) * 1.1f);
            float hgt = Mth.lerp(mix, 3.0f + 1.7f * h(seed, i, e0 * 8 + 3), 3.0f + 1.7f * h(seed, i, epoch * 8 + 3)) * rise * (1f - dip);
            float an = Mth.TWO_PI * i / 10f + spin + dA + Mth.sin(age * 0.035f + i * 1.9f) * 0.05f;
            float rr = R * 0.93f - 0.75f + dR;
            float hv = h(seed, i, (mix > 0.4f ? epoch : e0) * 8 + 7);
            wx[k] = c.x + Mth.cos(an) * rr;
            wz[k] = c.z + Mth.sin(an) * rr;
            wh[k] = hgt;
            wa[k] = an;
            wk[k] = hv < 0.35f ? 2 : hv < 0.68f ? 0 : 1;
            wd[k] = wx[k] * wx[k] + wz[k] * wz[k] + c.y * c.y;
        }
        int done = 0;
        IsrafilModels.Posed p = mesh.posed();
        for (int pass = 0; pass < n; pass++) {
            int best = -1;
            for (int k = 0; k < n; k++) if ((done & (1 << k)) == 0 && (best < 0 || wd[k] > wd[best])) best = k;
            done |= 1 << best;
            if (wh[best] < 0.05f) continue;
            p.clearPose();
            float zx = -Mth.cos(wa[best]), zz = -Mth.sin(wa[best]);
            IsrafilModels.Mesh m;
            if (wk[best] == 2) {
                p.standing(zx, zz, 0.95f, wh[best], 0.95f);
                m = IsrafilModels.PILLAR;
            } else {
                p.standing(zx, zz, 1.9f, wh[best], 1.9f);
                m = wk[best] == 0 ? IsrafilModels.WALL_A : IsrafilModels.WALL_B;
            }
            mesh.draw(buf, MODEL_ATLAS, m, wx[best], fy + 0.02f, wz[best], a(WHITE, 0.97f * fade), 0.08f + 0.7f * flash, 14);
        }
    }

    /** The dark faceted core of the collapse ({@link IsrafilModels#CORE}, at most 12 quads), turning, radius {@code rc}; returns the quads drawn (0 = the caller draws the flat void sphere). */
    private int coreModel(VfxVertexBuffer buf, float age, float x, float y, float z, float rc, float alpha, float heat) {
        if (rc < 0.05f) return 0;
        IsrafilModels.Posed p = mesh.posed();
        p.clearPose();
        p.tumble(0.3f, 1f, 0.2f, age * 0.07f, rc, rc, rc);
        return mesh.draw(buf, MODEL_ATLAS, IsrafilModels.CORE, x, y, z, a(WHITE, 0.98f * alpha), heat, 6);
    }

    /** A reality shard ({@link IsrafilModels#SHARD}, at most 3 quads) with its tip along (dx, dy, dz), rolled about that axis; returns the quads drawn (0 = the caller draws the flat shard). */
    private int shardModel(VfxVertexBuffer buf, float x, float y, float z, float dx, float dy, float dz, float size, float roll, float alpha, float heat) {
        if (alpha <= 0.03f || size <= 0.02f) return 0;
        mesh.posed().clearPose();
        mesh.posed().look(dx, dy, dz, 0f, 1f, 0f, roll, size);
        return mesh.draw(buf, MODEL_ATLAS, IsrafilModels.SHARD, x, y, z, a(WHITE, alpha), heat, 4);
    }

    /** A Coordinate Gate ({@link IsrafilModels#GATE}, at most 14 quads) standing at (x, y, z), its face along the horizontal direction (zx, zz) turned about the vertical by the flip angle {@code phi}; returns the quads drawn. */
    private int gateModel(VfxVertexBuffer buf, float x, float y, float z, float zx, float zz, float phi, float scale, float alpha, float heat) {
        if (alpha <= 0.03f) return 0;
        float c = Mth.cos(phi), sn = Mth.sin(phi);
        mesh.posed().clearPose();
        mesh.posed().standing(zx * c + zz * sn, -zx * sn + zz * c, scale, scale, scale);
        return mesh.draw(buf, MODEL_ATLAS, IsrafilModels.GATE, x, y, z, a(WHITE, alpha), heat, 6);
    }

    // ================================================================== TRIAL_LABYRINTH
    private void labyrinth(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 8, 10);
        if (fade <= 0.01f) return;
        float R = Mth.clamp(inst.power, 1.5f, 12f);
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        float fy = c.y - 0.9f;
        float dist = Mth.sqrt(c.x * c.x + c.z * c.z);
        float near = 1f - smooth(40f, 90f, dist);                          // the decorative extras are left out with distance
        float rise = VfxAnim.easeOutCubic(Mth.clamp(age / 8f, 0, 1));
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.11f);
        // the labyrinth shifts every 20 ticks: epoch = which shuffle we are in, mix = how far the panes have slid to the new one, flash = the white flash of the shuffle
        int epoch = (int) Math.floor(age / 20f);
        float into = age - epoch * 20f;
        float mix = epoch == 0 ? 1f : smooth(0f, 6f, into);
        float flash = epoch == 0 ? 0f : 1f - Mth.clamp(into / 5f, 0, 1);
        int e0 = epoch - 1;

        // the floor: a dark disc under a maze disc, a coordinate seal at the middle, the rim, a ring at the top of the cage
        VfxPose floor = VfxPose.ground(new Vector3f(c.x, fy + 0.04f, c.z));
        buf.plane(VOID, VfxBlend.ALPHA, floor, R * 1.08f, a(VOID_C, 0.55f * fade));
        buf.plane(MAZE, VfxBlend.ADD, floor.lift(0.01f).spin(age * 0.006f), R / 0.94f, a(CYAN, 0.62f * fade * pulse + 0.25f * flash * fade));
        buf.plane(MAZE, VfxBlend.ADD, floor.lift(0.02f).spin(-age * 0.011f + 1.1f), R * 0.52f / 0.94f, a(TEAL, 0.55f * fade));
        buf.plane(GRID, VfxBlend.ADD, floor.lift(0.03f).spin(age * 0.02f), 1.25f * (0.8f + 0.2f * rise), a(BRASS_HI, 0.5f * fade * pulse));
        ringAt(buf, floor.lift(0.03f), R * 1.0f, a(ICE, 0.65f * fade));
        float tp = (age % 20f) / 20f;
        ringAt(buf, floor.lift(0.04f), R * (0.55f + 0.55f * VfxAnim.easeOutCubic(tp)), a(CYAN, (1f - tp) * 0.28f * fade));
        VfxPose top = VfxPose.ground(new Vector3f(c.x, fy + 4.2f * rise, c.z));
        ringAt(buf, top, R * 0.97f, a(CYAN, 0.35f * fade));

        // the panes: a ring of tall maze panes (a dark copy under a bright one), sliding slowly; each shuffle moves, resizes and re-shows them
        int n = ctx.seg(10, 6);
        float arcHalf = Mth.PI / n * 0.9f;
        float spin = age * 0.004f;
        for (int i = 0; i < n; i++) {
            float dA = Mth.lerp(mix, (h(seed, i, e0 * 8 + 1) - 0.5f) * 0.24f, (h(seed, i, epoch * 8 + 1) - 0.5f) * 0.24f);
            float dR = Mth.lerp(mix, (h(seed, i, e0 * 8 + 2) - 0.5f) * 1.0f, (h(seed, i, epoch * 8 + 2) - 0.5f) * 1.0f);
            float hgt = Mth.lerp(mix, 3.3f + 1.7f * h(seed, i, e0 * 8 + 3), 3.3f + 1.7f * h(seed, i, epoch * 8 + 3)) * rise;
            int variant = (int) (h(seed, i, (mix > 0.4f ? epoch : e0) * 8 + 4) * 4f);
            float an = Mth.TWO_PI * i / n + spin + dA + Mth.sin(age * 0.035f + i * 1.9f) * 0.05f;
            float r = R * 0.93f + dR;
            float ax = c.x + Mth.cos(an - arcHalf) * r, az = c.z + Mth.sin(an - arcHalf) * r;
            float bx = c.x + Mth.cos(an + arcHalf) * r, bz = c.z + Mth.sin(an + arcHalf) * r;
            float u0 = (variant & 1) == 0 ? 0f : 1f, v0 = (variant & 2) == 0 ? 0f : 1f;
            float al = (0.62f + 0.1f * Mth.sin(age * 0.2f + i * 2.3f) + 0.55f * flash) * fade;
            quad(buf, PANE, VfxBlend.ALPHA, ax, fy, az, bx, fy, bz, bx, fy + hgt, bz, ax, fy + hgt, az, u0, v0, 1f - u0, 1f - v0, a(VOID_C, 0.5f * fade), a(VOID_C, 0.2f * fade));
            quad(buf, PANE, VfxBlend.ADD, ax, fy, az, bx, fy, bz, bx, fy + hgt, bz, ax, fy + hgt, az, u0, v0, 1f - u0, 1f - v0, a(WHITE, al), a(WHITE, al * 0.55f));
        }

        // 3.21.0: real 3D maze walls and pillars rise inside the panes and slide to new places at every shuffle (a few pieces; the panes stay as the rest of the cage)
        if (modelsOn(ctx, c.x, c.y, c.z, MAZE_RANGE)) mazePieces(buf, ctx, c, fy, R, seed, age, epoch, e0, mix, rise, flash, fade);

        // floating corner shards that bob at the top of the cage, and motes rising inside it
        int ns = near > 0.01f ? ctx.seg(8, 4) : 0;
        for (int i = 0; i < ns; i++) {
            float an = Mth.TWO_PI * i / ns + spin * 2f + h(seed, i, 71) * 0.4f;
            float r = R * (0.84f + 0.2f * h(seed, i, 72));
            float y = fy + (2.2f + 2.4f * h(seed, i, 73)) * rise + 0.12f * Mth.sin(age * 0.08f + i * 1.7f);
            shard(ctx, buf, c.x + Mth.cos(an) * r, y, c.z + Mth.sin(an) * r, 0.5f + 0.35f * h(seed, i, 74), age * 0.05f * (h(seed, i, 75) - 0.5f) * 4f + i, a(WHITE, 0.8f * fade * near));
        }
        int nm = near > 0.01f ? ctx.seg(8, 3) : 0;
        for (int i = 0; i < nm; i++) {
            float u = Mth.frac(age / 70f + h(seed, i, 81));
            float an = h(seed, i, 82) * Mth.TWO_PI, rr = (0.15f + 0.7f * h(seed, i, 83)) * R;
            mote(ctx, buf, c.x + Mth.cos(an + u) * rr, fy + 0.2f + u * 4f, c.z + Mth.sin(an + u) * rr, 0.24f, a(i % 3 == 0 ? BRASS_HI : CYAN, Mth.sin(u * Mth.PI) * 0.8f * fade * near));
        }
    }

    // ================================================================== TRIAL_GRAVITY
    private void gravity(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float D = inst.duration;
        float S = Mth.clamp(inst.power, 0.5f, 4f);
        float fade = Math.min(Mth.clamp(age / 4f, 0, 1), Mth.clamp((D - age) / 8f, 0, 1));
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        float tThud = D * THUD;
        float p = Mth.clamp(age / tThud, 0, 1);                               // the build-up, 0 -> 1 at the thud
        float press = VfxAnim.easeInCubic(p);                                 // the weight speeds up as it falls
        float tt = Mth.clamp((age - tThud) / Math.max(1f, D - tThud), 0, 1);  // after the thud, 0 -> 1
        Vector3f g = ctx.rel(inst.from(ctx));
        float dist = Mth.sqrt(g.x * g.x + g.z * g.z);
        float near = 1f - smooth(40f, 90f, dist);
        VfxPose floor = VfxPose.ground(new Vector3f(g.x, g.y + 0.05f, g.z));

        // the disc that is pressed into the ground, with its dark rim
        float discR = S * (2.3f - 0.55f * press);
        buf.plane(VOID, VfxBlend.ALPHA, floor, discR, a(VOID_C, (0.35f + 0.5f * press) * fade));
        ringAt(buf, floor.lift(0.01f), discR * 0.98f, a(INDIGO, 0.55f * fade));
        VfxBloom.planeGlow(buf, floor, S * 1.5f, DEEP, (0.4f + 0.6f * press) * fade);

        // the plate: a coordinate disc comes down on it, a soft column of light between them
        float plateY = g.y + 0.14f + 3.6f * S * (1f - press);
        VfxPose plate = VfxPose.ground(new Vector3f(g.x, plateY, g.z));
        float plateR = S * (1.95f - 0.2f * press);
        int plate3d = modelsOn(ctx, g.x, g.y, g.z, PLATE_RANGE) ? weightPlate(buf, ctx, age, press, tt, fade, g.x, plateY, g.z, S) : 0;      // 3.21.0: the engraved 3D slab on its chains
        if (plate3d == 0) {
            buf.plane(GRID, VfxBlend.ADD, plate.spin(age * 0.045f), plateR / 0.985f, a(CYAN, 0.8f * fade));
            ringAt(buf, plate, plateR * 0.96f, a(ICE, 0.7f * fade));
        }
        ribbon(buf, BEAM, VfxBlend.ADD, g.x, plateY, g.z, g.x, g.y + 0.1f, g.z, S * 1.1f, S * 1.3f, -age * 0.03f, -age * 0.03f + 1.2f, a(TEAL, 0.22f * fade * (1f - press * 0.5f)), a(INDIGO, 0.10f * fade));

        // 3-4 pillars of heavy light flowing down onto the disc (the pattern runs downwards, faster as the weight falls)
        int np = ctx.seg(4, 3);
        float flow = -(age * 0.045f + 0.08f * age * age / D);
        for (int i = 0; i < np; i++) {
            float an = Mth.TWO_PI * i / np + 0.5f + (h(seed, i, 1) - 0.5f) * 0.3f;
            float rr = S * (0.95f + 0.25f * h(seed, i, 2));
            float px = g.x + Mth.cos(an) * rr, pz = g.z + Mth.sin(an) * rr;
            float topY = g.y + (4.8f + 1.6f * h(seed, i, 3)) * S;
            float sc = flow * (0.8f + 0.5f * h(seed, i, 4));
            float len = topY - g.y;
            ribbon(buf, BEAM, VfxBlend.ADD, px, topY, pz, px, g.y + 0.05f, pz, 0.34f * S, 0.30f * S, 0f, 1f, a(INDIGO, 0f), a(TEAL, 0.30f * fade));
            ribbon(buf, STREAK, VfxBlend.ADD, px, topY, pz, px, g.y + 0.05f, pz, 0.22f * S, 0.20f * S, sc, sc + len / 3.6f, a(CYAN, 0f), a(ICE, 0.95f * fade * (0.55f + 0.45f * press)));
        }

        // chains of tally marks that sink into the ground
        int nc = ctx.seg(3, 2);
        for (int i = 0; i < nc; i++) {
            float an = Mth.TWO_PI * (i + 0.5f) / nc + 1.1f;
            float rr = S * 0.62f;
            float cx = g.x + Mth.cos(an) * rr, cz = g.z + Mth.sin(an) * rr;
            float topY = g.y + 5.8f * S;
            float len = topY - g.y;
            float sc = -(age * 0.035f + 0.06f * age * age / D);
            float hw = 0.30f * S;
            ribbon(buf, CHAIN, VfxBlend.ADD, cx, topY, cz, cx, g.y + 0.02f, cz, hw, hw, sc, sc + len / (hw * 8f), a(WHITE, 0f), a(WHITE, 0.9f * fade));
        }

        // the ground cracks under the weight: they spread as it comes down and surge at the thud
        float crack = S * (0.9f + 1.6f * VfxAnim.easeOutCubic(Mth.clamp((age - 0.35f * tThud) / (0.65f * tThud), 0, 1)) + 0.7f * VfxAnim.easeOutCubic(Mth.clamp((age - tThud) / 4f, 0, 1)));
        if (age > 0.3f * tThud) {
            float ca = Mth.clamp((age - 0.3f * tThud) / 6f, 0, 1) * fade;
            buf.plane(CRACKS, VfxBlend.ALPHA, floor.lift(0.02f).spin(h(seed, 9, 0) * Mth.TWO_PI), crack, a(CRACK_C, 0.88f * ca));
            buf.plane(CRACKS, VfxBlend.ALPHA, floor.lift(0.025f).spin(h(seed, 10, 0) * Mth.TWO_PI + 2f), crack * 0.62f, a(CRACK_C, 0.7f * ca));
            buf.plane(CRACKS, VfxBlend.ADD, floor.lift(0.03f).spin(h(seed, 9, 0) * Mth.TWO_PI), crack, a(CYAN, (tt > 0f ? 0.6f * (1f - tt) : 0.16f + 0.3f * press) * fade));
        }

        // falling dust motes before the thud, dust pushed outwards after it
        int nm = near > 0.01f ? ctx.seg(6, 3) : 0;
        for (int i = 0; i < nm; i++) {
            float an = h(seed, i, 21) * Mth.TWO_PI;
            if (tt <= 0f) {
                float u = Mth.frac(age / 16f + h(seed, i, 22));
                float rr = S * (0.5f + 1.6f * h(seed, i, 23));
                mote(ctx, buf, g.x + Mth.cos(an) * rr, g.y + (1f - u) * 4.2f * S, g.z + Mth.sin(an) * rr, 0.2f * S + 0.08f, a(CYAN, Mth.sin(u * Mth.PI) * 0.8f * fade * near));
            } else {
                float rr = S * (0.9f + 3.4f * VfxAnim.easeOutCubic(tt)) * (0.7f + 0.5f * h(seed, i, 24));
                mote(ctx, buf, g.x + Mth.cos(an) * rr, g.y + 0.12f + 0.25f * Mth.sin(tt * Mth.PI), g.z + Mth.sin(an) * rr, 0.3f * S, a(ICE, (1f - tt) * 0.8f * near));
            }
        }

        // the thud: a flash, a sound ring and a ring of light flying out of the point where the weight lands
        if (tt > 0f) {
            float f = 1f - Mth.clamp(tt * 2.2f, 0, 1);
            glow(ctx, buf, g.x, g.y + 0.3f, g.z, 2.2f * S, CYAN, f);
            flare(ctx, buf, g.x, g.y + 0.35f, g.z, 3.4f * S * (0.5f + f), 0f, a(ICE, f));
            soundAt(buf, floor.lift(0.05f), S * (0.8f + 3.8f * VfxAnim.easeOutCubic(tt)), a(CYAN, (float) Math.pow(1f - tt, 1.15) * 0.95f));
            ringAt(buf, floor.lift(0.06f), S * (0.4f + 2.6f * VfxAnim.easeOutCubic(tt)), a(ICE, (1f - tt) * 0.75f));
        }
    }

    // ================================================================== TRIAL_COLLAPSE
    private void collapse(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float D = inst.duration;
        float R = Mth.clamp(inst.power, 1.5f, 14f);
        float out = Mth.clamp((D - age) / 24f, 0, 1);
        if (out <= 0.01f) return;
        long seed = inst.seed;
        Vector3f g = ctx.rel(inst.from(ctx));
        float dist = Mth.sqrt(g.x * g.x + g.z * g.z);
        float near = 1f - smooth(45f, 100f, dist);
        VfxPose floor = VfxPose.ground(new Vector3f(g.x, g.y + 0.06f, g.z));
        float cy = g.y + 0.9f + 0.18f * R;                                    // the heart of the collapse, a little above the ground
        float Rs = Mth.clamp(R / 6.5f, 0.8f, 1.3f);                            // 3.21.0: the size of the 3D shards and the core follows the radius
        float d = age - PINCH;                                                // < 0 while the space is pulled in

        // ---- phase 1: shards and rings are pulled in
        if (d < 1f) {
            float u = Mth.clamp(age / PINCH, 0, 1);
            int m = ctx.seg(12, 6);
            int nSh = modelsOn(ctx, g.x, g.y, g.z, SHARD_RANGE) ? Math.min(8, m) : 0;                  // 3.21.0: the first eight shards are 3D crystals
            if (d < 0f && modelsOn(ctx, g.x, g.y, g.z, CORE_RANGE)) coreModel(buf, age, g.x, cy, g.z, Rs * (0.10f + 0.62f * u * u), Math.min(1f, age / 3f) * out, 0.2f + 0.5f * u);   // the dark core grows at the centre
            for (int i = 0; i < m; i++) {
                float delay = h(seed, i, 0) * 4f;
                float q = Mth.clamp((age - delay) / (PINCH - delay), 0, 1);
                float qb = Mth.clamp(q - 0.12f, 0, 1);
                float an = Mth.TWO_PI * i / m + h(seed, i, 1) * 0.5f;
                float zy = (h(seed, i, 2) - 0.15f) * R * 0.85f;
                float rho0 = R * (1.0f + 0.25f * h(seed, i, 3));
                float k = 1f - VfxAnim.easeInCubic(q), k2 = 1f - VfxAnim.easeInCubic(qb);
                float sw = 2.2f * q, sw2 = 2.2f * qb;                          // they swirl as they fall in
                float x1 = g.x + Mth.cos(an + sw) * rho0 * k, z1 = g.z + Mth.sin(an + sw) * rho0 * k, y1 = cy + zy * k - 0.4f * R * (1f - k) * 0.3f;
                float x2 = g.x + Mth.cos(an + sw2) * rho0 * k2, z2 = g.z + Mth.sin(an + sw2) * rho0 * k2, y2 = cy + zy * k2;
                float al = Math.min(q * 6f, 1f) * (1f - smooth(0.93f, 1f, q)) * out;
                if (al <= 0.01f) continue;
                ribbon(buf, BEAM, VfxBlend.ADD, x2, y2, z2, x1, y1, z1, 0.05f, 0.16f, 0f, 1f, a(TEAL, 0f), a(CYAN, 0.7f * al));
                if (i >= nSh || shardModel(buf, x1, y1, z1, x1 - x2, y1 - y2, z1 - z2, 0.62f * (1f - 0.55f * q) * Rs, age * 0.3f + i * 1.3f, 0.95f * al, 0.3f + 0.4f * q) == 0)
                    shard(ctx, buf, x1, y1, z1, 0.95f * (1f - 0.55f * q), age * 0.3f + i * 1.3f, a(WHITE, 0.95f * al));
            }
            for (int k = 0; k < 3; k++) {
                float q = Mth.clamp(age / PINCH - 0.10f * k, 0, 1);
                float r = R * (1.15f - 1.08f * VfxAnim.easeInCubic(q));
                ringAt(buf, floor.lift(0.02f * k), r, a(k == 1 ? BRASS_HI : CYAN, (0.25f + 0.65f * q) * (1f - 0.2f * k) * out * (1f - smooth(0.95f, 1f, q))));
            }
            float gr = R * (1.1f - 0.95f * VfxAnim.easeInCubic(u));
            buf.plane(GRID, VfxBlend.ADD, floor.lift(0.03f).spin(age * 0.18f), gr / 0.985f, a(INDIGO, 0.75f * u * out));
            glow(ctx, buf, g.x, cy, g.z, 0.5f + 2.5f * u * u, CYAN, (0.3f + 0.7f * u) * out);
        }

        // ---- phase 2: the pinch, a bright flash, a shock ring on the ground and one in the air
        if (d >= 0f && d < 26f) {
            float flash = 1f - Mth.clamp(d / 9f, 0, 1);
            if (flash > 0f) {
                flare(ctx, buf, g.x, cy, g.z, 11f * (0.3f + 0.7f * flash) * Mth.clamp(R / 6.5f, 0.6f, 1.6f), age * 0.04f, a(WHITE, flash));
                flare(ctx, buf, g.x, cy, g.z, 6f * Mth.clamp(R / 6.5f, 0.6f, 1.6f), 0.785f - age * 0.03f, a(CYAN, 0.9f * flash));
                glow(ctx, buf, g.x, cy, g.z, 7f * (0.4f + 0.6f * flash), WHITE, 1.1f * flash);
            }
            float sw = d / 24f;
            if (sw < 1f) {
                soundAt(buf, floor.lift(0.04f), R * 1.25f * VfxAnim.easeOutCubic(sw), a(CYAN, (float) Math.pow(1f - sw, 1.2) * 0.95f));
                ringAt(buf, floor.lift(0.05f), R * 0.9f * VfxAnim.easeOutCubic(Mth.clamp(sw * 1.3f, 0, 1)), a(ICE, (1f - sw) * 0.8f));
                float sa = d / 18f;
                if (sa < 1f) buf.plane(SOUND, VfxBlend.ADD, faceCamera(g.x, cy, g.z).spin(0.4f), R * 1.15f * VfxAnim.easeOutCubic(sa) / 0.955f, a(VIOLET, (1f - sa) * 0.8f));
            }
        }

        // ---- phase 3: the Nihility Collapse field
        float open = VfxAnim.easeOutCubic(Mth.clamp(d / 10f, 0, 1));
        if (open <= 0f) return;
        float field = open * out;
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.12f);
        float rs = R * 0.36f * open * (0.96f + 0.04f * Mth.sin(age * 0.2f));        // the radius of the void sphere
        buf.plane(VOID, VfxBlend.ALPHA, floor.spin(age * 0.012f), R * open * 1.02f, a(VOID_C, 0.82f * field));
        buf.plane(VOID, VfxBlend.ALPHA, floor.lift(0.01f).spin(-age * 0.02f + 1.3f), R * open * 0.62f, a(DEEP, 0.55f * field));
        float cr = R * (0.35f + 0.65f * VfxAnim.easeOutCubic(Mth.clamp(d / 70f, 0, 1)));
        buf.plane(CRACKS, VfxBlend.ALPHA, floor.lift(0.02f).spin(h(seed, 5, 5) * Mth.TWO_PI), cr, a(CRACK_C, 0.9f * field));
        buf.plane(CRACKS, VfxBlend.ADD, floor.lift(0.03f).spin(h(seed, 5, 5) * Mth.TWO_PI), cr, a(CYAN, 0.28f * field * pulse));
        buf.plane(INVERT, VfxBlend.NEGATIVE, floor.lift(0.05f), R * open / 0.90f, negative(0.75f * field));
        ringAt(buf, floor.lift(0.06f), R * open, a(CYAN, 0.9f * field * pulse));
        buf.plane(GRID, VfxBlend.ADD, floor.lift(0.04f).spin(age * 0.008f), R * 0.9f * open / 0.985f, a(INDIGO, 0.38f * field));
        float tp = (age % 24f) / 24f;
        soundAt(buf, floor.lift(0.07f), R * (0.15f + 0.85f * tp) * open, a(VIOLET, (1f - tp) * 0.4f * field));

        // the void sphere: a dark core with a slow spiral, an inverted rim, a bright cyan edge, a glow
        if (rs > 0.1f) {
            q0.set(g.x, cy, g.z);
            float rcore = d < 1.5f ? Mth.lerp(smooth(0f, 1.5f, d), Rs * 0.72f, 0.15f) : Math.min(rs * 0.97f, Mth.lerp(smooth(1.5f, 7f, d), 0.15f, rs * 0.97f));
            int core3d = modelsOn(ctx, g.x, g.y, g.z, CORE_RANGE) ? coreModel(buf, age, g.x, cy, g.z, rcore, field, 0.1f + 0.2f * pulse) : 0;      // 3.21.0: the dark faceted core
            if (core3d == 0) {
                buf.billboard(ctx, VOID, VfxBlend.ALPHA, q0, rs * 2f / 0.97f, age * 0.05f, a(VOID_C, 0.97f * field));
                buf.billboard(ctx, VOID, VfxBlend.ALPHA, q0, rs * 1.3f / 0.97f, -age * 0.08f, a(0xFF2A1060, 0.55f * field));
            }
            buf.billboard(ctx, INVERT, VfxBlend.NEGATIVE, q0, rs * 2.1f / 0.90f, 0f, negative(0.85f * field));
            buf.billboard(ctx, RING, VfxBlend.ADD, q0, rs * 2f * 1.02f / 0.965f, 0f, a(CYAN, 0.95f * field * pulse));
            glow(ctx, buf, g.x, cy, g.z, rs * 1.7f, TEAL, 0.5f * field);
        }
        // shards orbit it, and motes fall into it
        int no = near > 0.01f ? ctx.seg(6, 3) : 0;
        int nOr = modelsOn(ctx, g.x, g.y, g.z, SHARD_RANGE) ? Math.min(4, no) : 0;                       // 3.21.0: four of the orbiting shards are 3D crystals
        for (int i = 0; i < no; i++) {
            float an = age * (0.05f + 0.025f * h(seed, i, 31)) + Mth.TWO_PI * i / no;
            float orb = R * (0.42f + 0.32f * h(seed, i, 32));
            float tilt = (h(seed, i, 33) - 0.5f) * 1.2f;
            float ox = g.x + Mth.cos(an) * orb, oy = cy + Mth.sin(an) * orb * tilt, oz = g.z + Mth.sin(an) * orb * Mth.cos(tilt);
            if (i >= nOr || shardModel(buf, ox, oy, oz, -Mth.sin(an), Mth.cos(an) * tilt, Mth.cos(an) * Mth.cos(tilt), (0.55f + 0.3f * h(seed, i, 34)) * Rs, age * 0.1f * (i + 1) * 0.3f, 0.9f * field * near, 0.25f) == 0)
                shard(ctx, buf, ox, oy, oz, 0.7f + 0.4f * h(seed, i, 34), age * 0.1f * (i + 1) * 0.3f, a(WHITE, 0.85f * field * near));
        }
        int nm = near > 0.01f ? ctx.seg(8, 3) : 0;
        for (int i = 0; i < nm; i++) {
            float u = Mth.frac(age / 46f + h(seed, i, 41));
            float an = h(seed, i, 42) * Mth.TWO_PI + u * 1.5f, rr = R * (0.95f - 0.8f * u * u);
            mote(ctx, buf, g.x + Mth.cos(an) * rr, g.y + 0.15f + (cy - g.y) * u * u, g.z + Mth.sin(an) * rr, 0.26f, a(i % 2 == 0 ? CYAN : VIOLET, Mth.sin(u * Mth.PI) * 0.9f * field * near));
        }
    }

    // ================================================================== TRIAL_SHIFT
    private void shift(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float D = inst.duration;
        float u = Mth.clamp(age / D, 0, 1);
        float S = Mth.clamp(inst.power, 0.5f, 3f);
        long seed = inst.seed;
        Vector3f A = ctx.rel(inst.from(ctx)), B = ctx.rel(inst.to(ctx));
        float dx = B.x - A.x, dy = B.y - A.y, dz = B.z - A.z;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        float hl = Mth.sqrt(dx * dx + dz * dz);
        float ix = 0, iz = 1;                                                // the horizontal direction of the shift
        if (hl > 0.05f) {
            ix = dx / hl;
            iz = dz / hl;
        }
        float near = 1f - smooth(40f, 90f, Mth.sqrt(A.x * A.x + A.z * A.z));
        float endFade = 1f - smooth(0.7f, 1f, u);
        float R = 1.45f * S;
        boolean gatesA = modelsOn(ctx, A.x, A.y, A.z, GATE_RANGE), gatesB = modelsOn(ctx, B.x, B.y, B.z, GATE_RANGE);        // 3.21.0: two 3D gate frames, the rune ring shrinks to sit inside them

        // ---- where the caster was: the ring flips shut (its width goes to nothing and past it), a ghost stays behind
        float phiA = Mth.PI * Mth.clamp(age / (D * 0.6f), 0, 1);
        float alA = (1f - smooth(0.4f, 1f, phiA / Mth.PI)) * Mth.clamp(age / 1.5f + 0.4f, 0, 1);
        VfxPose floorA = VfxPose.ground(new Vector3f(A.x, A.y + 0.06f, A.z));
        buf.plane(GRID, VfxBlend.ADD, flipped(floorA.spin(age * 0.2f), Mth.cos(phiA)), R * 1.15f / 0.985f, a(CYAN, 0.85f * alA));
        VfxPose hoopA = VfxPose.facing(new Vector3f(A.x, A.y + 1.0f, A.z), new Vector3f(ix, 0, iz));
        int gateA = gatesA ? gateModel(buf, A.x, A.y, A.z, ix, iz, phiA, S * 0.95f, 0.95f * alA, 0.25f + 0.5f * (1f - Mth.clamp(age / 4f, 0, 1))) : 0;
        buf.plane(GRID, VfxBlend.ADD, flipped(hoopA.spin(-age * 0.25f), Mth.cos(phiA * 1.0f)), (gateA > 0 ? R * 0.62f : R) / 0.985f, a(ICE, 0.7f * alA));
        glow(ctx, buf, A.x, A.y + 1.0f, A.z, 2.4f * S, TEAL, alA * 0.7f);

        // ---- where they arrive: the ring unfolds, a ring runs out, flares
        float uB = Mth.clamp((age - 0.2f * D) / (0.5f * D), 0, 1);
        if (uB > 0f) {
            float phiB = Mth.PI * (1f - VfxAnim.easeOutCubic(uB));
            float alB = Mth.clamp(uB * 3f, 0, 1) * endFade;
            VfxPose floorB = VfxPose.ground(new Vector3f(B.x, B.y + 0.06f, B.z));
            buf.plane(GRID, VfxBlend.ADD, flipped(floorB.spin(-age * 0.2f), Mth.cos(phiB)), R * 1.15f / 0.985f, a(CYAN, 0.85f * alB));
            VfxPose hoopB = VfxPose.facing(new Vector3f(B.x, B.y + 1.0f, B.z), new Vector3f(ix, 0, iz));
            int gateB = gatesB ? gateModel(buf, B.x, B.y, B.z, ix, iz, phiB, S * 0.95f, 0.95f * alB, 0.25f + 0.5f * (1f - Mth.clamp((age - 0.2f * D) / 4f, 0, 1))) : 0;
            buf.plane(GRID, VfxBlend.ADD, flipped(hoopB.spin(age * 0.25f), Mth.cos(phiB)), (gateB > 0 ? R * 0.62f : R) / 0.985f, a(ICE, 0.7f * alB));
            float tr = Mth.clamp((age - 0.25f * D) / (0.7f * D), 0, 1);
            if (tr < 1f) soundAt(buf, floorB.lift(0.03f), 0.4f + 2.4f * S * VfxAnim.easeOutCubic(tr), a(CYAN, (1f - tr) * 0.9f));
            float fl = 1f - Mth.clamp((age - 0.25f * D) / (0.35f * D), 0, 1);
            glow(ctx, buf, B.x, B.y + 1.0f, B.z, 2.6f * S, CYAN, alB * 0.7f);
            flare(ctx, buf, B.x, B.y + 1.0f, B.z, 3.8f * S * (0.4f + 0.6f * fl), age * 0.3f, a(WHITE, fl * alB));
        }
        flare(ctx, buf, A.x, A.y + 1.0f, A.z, 3.0f * S * (1f - u), -age * 0.2f, a(ICE, (1f - Mth.clamp(age / (D * 0.5f), 0, 1))));

        if (len < 0.4f) return;

        // ---- the dashed, folded line at chest height: it is drawn from the start to the end, then fades
        float head = VfxAnim.easeOutCubic(Mth.clamp(age / (D * 0.4f), 0, 1));
        float lineA = (1f - smooth(0.55f, 1f, u));
        int segs = ctx.seg(Mth.clamp(Math.round(len / 5f), 3, 8), 3);
        float ux = -iz, uz = ix;                                              // sideways
        float amp = Math.min(0.5f, 0.06f * len) * S;
        float px = A.x, py = A.y + 1.0f, pz = A.z;
        for (int k = 1; k <= segs; k++) {
            float f = (float) k / segs;
            if ((float) (k - 1) / segs >= head) break;
            float fe = Math.min(f, head);
            float side = (k % 2 == 0 ? 1f : -1f) * amp * (k == segs ? 0f : 1f) * Mth.sin(f * Mth.PI) * 1.4f;
            float lift = ((k % 3) - 1f) * amp * 0.5f * (k == segs ? 0f : 1f);
            float qx = A.x + dx * fe + ux * side, qy = A.y + 1.0f + dy * fe + lift, qz = A.z + dz * fe + uz * side;
            float vA = (float) (k - 1) / segs * len / 4f, vB = fe * len / 4f;
            ribbon(buf, DASH, VfxBlend.ADD, px, py, pz, qx, qy, qz, 0.20f * S, 0.20f * S, vA - age * 0.25f, vB - age * 0.25f, a(CYAN, 0.95f * lineA), a(ICE, lineA));
            px = qx;
            py = qy;
            pz = qz;
        }
        ribbon(buf, BEAM, VfxBlend.ADD, A.x, A.y + 1.0f, A.z, A.x + dx * head, A.y + 1.0f + dy * head, A.z + dz * head, 0.34f * S, 0.34f * S, 0f, 1f, a(INDIGO, 0.22f * lineA), a(TEAL, 0.22f * lineA));

        // ---- swapped afterimages: the one left behind, one that arrives, and some that flash on between them
        float hh = 1.0f * S, hw = hh * 0.29f;
        float vx = -A.x, vz = -A.z, vl = Mth.sqrt(vx * vx + vz * vz);
        float rx = 1f, rz = 0f;
        if (vl > 1e-3f) {
            rx = vz / vl;
            rz = -vx / vl;
        }
        float leftA = (1f - smooth(0.05f, 0.65f, u)) * 0.85f;
        sprite(buf, GHOST, VfxBlend.ADD, A.x + ix * 0.25f * u, A.y + hh, A.z + iz * 0.25f * u, rx, 0, rz, 0, 1, 0, hw, hh, a(WHITE, leftA));
        float arriveA = smooth(0.15f, 0.55f, u) * (1f - smooth(0.6f, 1f, u)) * 0.9f;
        sprite(buf, GHOST, VfxBlend.ADD, B.x, B.y + hh, B.z, rx, 0, rz, 0, 1, 0, hw, hh, a(WHITE, arriveA));
        int mid = near > 0.01f && len > 4f ? ctx.seg(3, 1) : 0;
        for (int k = 0; k < mid; k++) {
            float f = (k + 1f) / (mid + 1f);
            float tk = Mth.clamp((age - f * 0.45f * D) / (0.35f * D), 0, 1);
            float al = Mth.sin(tk * Mth.PI) * 0.6f;
            if (al <= 0.02f) continue;
            float jx = (h(seed, k, (int) Math.floor(age)) - 0.5f) * 0.5f;
            sprite(buf, GHOST, VfxBlend.ADD, A.x + dx * f + ux * jx, A.y + dy * f + hh, A.z + dz * f + uz * jx, rx, 0, rz, 0, 1, 0, hw, hh, a(CYAN, al));
        }

        // ---- motes along the line and shards thrown off both ends
        int nm = near > 0.01f ? ctx.seg(6, 2) : 0;
        for (int i = 0; i < nm; i++) {
            float f = Mth.frac(h(seed, i, 3) + u * 0.6f);
            float off = (h(seed, i, 4) - 0.5f) * 1.0f;
            mote(ctx, buf, A.x + dx * f + ux * off, A.y + dy * f + 0.7f + h(seed, i, 5) * 0.8f, A.z + dz * f + uz * off, 0.22f, a(i % 2 == 0 ? ICE : VIOLET, Mth.sin(f * Mth.PI) * 0.9f * lineA));
        }
        int ns = near > 0.01f ? ctx.seg(4, 0) : 0;
        for (int i = 0; i < ns; i++) {
            Vector3f e = (i & 1) == 0 ? A : B;
            float tk = Mth.clamp((age - ((i & 1) == 0 ? 0f : 0.25f * D)) / (0.6f * D), 0, 1);
            if (tk <= 0f || tk >= 1f) continue;
            float an = h(seed, i, 8) * Mth.TWO_PI;
            float rr = (0.5f + 1.6f * h(seed, i, 9)) * VfxAnim.easeOutCubic(tk) * S;
            shard(ctx, buf, e.x + Mth.cos(an) * rr, e.y + 1.0f + (h(seed, i, 10) - 0.3f) * 1.2f * tk, e.z + Mth.sin(an) * rr, 0.42f, age * 0.4f + i, a(WHITE, (1f - tk) * 0.9f));
        }
    }

    // ================================================================== TRIAL_TRUMPET
    private void trumpet(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float D = inst.duration;
        float fade = Mth.clamp((D - age) / 12f, 0, 1);
        if (fade <= 0.01f) return;
        float R = Mth.clamp(inst.power, 8f, 140f);
        long seed = inst.seed;
        Vector3f g = ctx.rel(inst.from(ctx));
        float dist = Mth.sqrt(g.x * g.x + g.z * g.z);
        float near = 1f - smooth(60f, 130f, dist);
        VfxPose floor = VfxPose.ground(new Vector3f(g.x, g.y - 0.88f, g.z));

        // ---- the flash at the caster, a glow on the ground
        float f0 = 1f - Mth.clamp(age / 10f, 0, 1);
        if (f0 > 0f) {
            flare(ctx, buf, g.x, g.y, g.z, 9f * (0.4f + 0.6f * f0), age * 0.03f, a(WHITE, f0));
            glow(ctx, buf, g.x, g.y, g.z, 6f * (0.4f + 0.6f * f0), CYAN, f0);
        }
        VfxBloom.planeGlow(buf, floor, Math.min(R * 0.08f, 8f) + 3f, TEAL, 0.9f * (1f - Mth.clamp(age / 24f, 0, 1)));

        // ---- the horn: it opens above the caster, bell up, then the light bursts from its mouth
        float vx = -g.x, vz = -g.z, vl = Mth.sqrt(vx * vx + vz * vz);
        float rx = 1f, rz = 0f;
        if (vl > 1e-3f) {
            rx = vz / vl;
            rz = -vx / vl;
        }
        float hornIn = VfxAnim.easeOutBack(Mth.clamp(age / 5f, 0, 1));
        float hornA = Math.min(Mth.clamp(age / 3f, 0, 1), 1f - smooth(14f, 24f, age));
        float hy = g.y + 3.0f;
        float hs = 2.3f * hornIn;
        // 3.21.0: the real 3D Grand Trumpet swings up over the caster, blares and cracks into 3D glass shards; the rays leave its bell. Out of range, with the setting off or without room, the flat horn of 3.20.0.
        boolean horn3d = modelsOn(ctx, g.x, g.y, g.z, HORN_RANGE) && hornModel(buf, ctx, age, seed, g.x, g.y, g.z, rx, rz, vl > 1e-3f ? vx / vl : 0f, vl > 1e-3f ? vz / vl : 0f);
        if (!horn3d && hornA > 0.01f) {
            sprite(buf, HORN, VfxBlend.ADD, g.x, hy, g.z, 0, 1, 0, rx, 0, rz, hs, hs * 0.5f, a(WHITE, hornA));
            glow(ctx, buf, g.x, hy, g.z, 2.6f * hornIn, BRASS_HI, 0.5f * hornA);
        }
        float mouthY = horn3d ? hmy : hy + hs * 0.6f;                         // the rim of the bell is at 80 % of the picture
        float mx = horn3d ? hmx : g.x, mz = horn3d ? hmz : g.z;               // where the light leaves
        float burst = VfxAnim.easeOutCubic(Mth.clamp((age - 4f) / 7f, 0, 1));
        if (burst > 0f) {
            float fm = 1f - Mth.clamp((age - 5f) / 12f, 0, 1);
            flare(ctx, buf, mx, mouthY, mz, 7f * (0.4f + 0.6f * fm), -age * 0.05f, a(ICE, fm));
            // the cone of rays upwards and outwards, and the shaft in the middle
            int K = ctx.seg(12, 6);
            float rf = 1f - smooth(10f, 34f, age);
            for (int i = 0; i < K; i++) {
                float ph = Mth.TWO_PI * (i + h(seed, i, 1) * 0.8f) / K;
                float th = (0.10f + 0.95f * (float) Math.pow(h(seed, i, 2), 0.8)) * 1.0f;
                float len = (14f + 34f * h(seed, i, 3)) * burst;
                float sc = -age * (0.05f + 0.04f * h(seed, i, 4));
                float ex = mx + Mth.sin(th) * Mth.cos(ph) * len, ey = mouthY + Mth.cos(th) * len, ez = mz + Mth.sin(th) * Mth.sin(ph) * len;
                ribbon(buf, BEAM, VfxBlend.ADD, mx, mouthY, mz, ex, ey, ez, 0.16f + 0.1f * h(seed, i, 5), 0.5f + 0.5f * h(seed, i, 6), sc, sc + len / 7f, a(ICE, 0.75f * rf * fade), a(i % 3 == 0 ? BRASS_HI : CYAN, 0f));
            }
            // the horn of light itself: a wide soft cone that opens from the mouth
            ribbon(buf, BEAM, VfxBlend.ADD, mx, mouthY, mz, mx, mouthY + 26f * burst, mz, 0.5f, 9f * burst + 0.5f, 0f, 1f, a(BRASS_HI, 0.42f * rf * fade), a(TEAL, 0.05f * rf * fade));
            float sh = 60f * burst;
            ribbon(buf, BEAM, VfxBlend.ADD, mx, mouthY, mz, mx, mouthY + sh, mz, 1.3f, 0.35f, 0f, 3f, a(CYAN, 0.55f * rf * fade), a(INDIGO, 0f));
            ribbon(buf, BEAM, VfxBlend.ADD, mx, mouthY, mz, mx, mouthY + sh, mz, 0.38f, 0.12f, -age * 0.08f, -age * 0.08f + 3f, a(ICE, 0.9f * rf * fade), a(CYAN, 0f));
            glow(ctx, buf, mx, mouthY, mz, 4.4f * burst, CYAN, 0.8f * rf * fade);
        }

        // ---- the sound rings: on the ground and as the dome; a ring far from the camera fades (it is only a thin line there)
        for (int k = 0; k < 5; k++) {
            float tt = Mth.clamp((age - 2f - k * 3f) / 30f, 0, 1);
            if (tt <= 0f || tt >= 1f) continue;
            float r = R * VfxAnim.easeOutCubic(tt);
            float away = Math.abs(dist - r);
            float vis = 1f - smooth(30f, 130f, away);
            float al = (float) Math.pow(1f - tt, 0.9) * (1f - 0.1f * k) * (0.15f + 0.85f * vis) * fade;
            soundAt(buf, floor.lift(0.02f * k), r, a(k == 2 ? BRASS_HI : (k % 2 == 0 ? ICE : CYAN), 0.95f * al));
        }
        float domeR = Math.min(R, 70f);
        for (int k = 0; k < 3; k++) {
            float tt = Mth.clamp((age - 1f - k * 4f) / 24f, 0, 1);
            if (tt <= 0f || tt >= 1f) continue;
            float r = domeR * VfxAnim.easeOutCubic(tt);
            soundAt(buf, faceCamera(g.x, g.y, g.z).spin(k * 0.7f), r, a(k == 0 ? WHITE : CYAN, (float) Math.pow(1f - tt, 1.2) * 0.8f * fade));
        }

        // ---- glass shards of the broken barriers fly out, turning
        int ns = ctx.seg(24, 10);
        if (horn3d && age < 26f) ns = ns / 3;                            // 3.21.0: the 3D horn and its shards take their share of the 100 quads
        for (int i = 0; i < ns; i++) {
            float born = 2f + h(seed, i, 11) * 10f;
            float q = (age - born) / (26f + 16f * h(seed, i, 12));
            if (q <= 0f || q >= 1f) continue;
            float ph = h(seed, i, 13) * Mth.TWO_PI, el = (h(seed, i, 14) - 0.2f) * 0.9f;
            float dd = (3f + 5f * h(seed, i, 15) + (10f + 28f * h(seed, i, 16)) * (1f - (float) Math.pow(1f - q, 2.2))) * (near * 0.5f + 0.5f);
            float sx = g.x + Mth.cos(ph) * Mth.cos(el) * dd, sz = g.z + Mth.sin(ph) * Mth.cos(el) * dd;
            float sy = g.y + 0.4f + Mth.sin(el) * dd * 0.8f - 5f * q * q;
            float sz2 = (0.6f + 0.9f * h(seed, i, 17)) * (1f - 0.3f * q);
            shard(ctx, buf, sx, sy, sz, sz2, age * (h(seed, i, 18) - 0.5f) * 0.9f + h(seed, i, 19) * 6f, a(WHITE, Math.min(q * 9f, 1f) * (1f - smooth(0.65f, 1f, q)) * fade));
        }
        int nm = near > 0.01f ? ctx.seg(8, 3) : 0;
        if (horn3d && age < 26f) nm = 0;
        for (int i = 0; i < nm; i++) {
            float u = Mth.frac(age / 40f + h(seed, i, 21));
            float an = h(seed, i, 22) * Mth.TWO_PI, rr = (0.8f + 5f * h(seed, i, 23)) * (0.5f + u);
            mote(ctx, buf, g.x + Mth.cos(an) * rr, g.y - 0.7f + u * 9f, g.z + Mth.sin(an) * rr, 0.3f, a(i % 3 == 0 ? BRASS_HI : ICE, Mth.sin(u * Mth.PI) * 0.8f * fade * near));
        }
    }

    // ================================================================== TRIAL_RIFT
    private void rift(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 6, 12);
        if (fade <= 0.01f) return;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        float near = 1f - smooth(30f, 70f, Mth.sqrt(c.x * c.x + c.z * c.z));
        int frame = (int) Math.floor(age / 2f);
        float shimmer = 0.78f + 0.22f * Mth.sin(age * 0.7f);
        float feet = c.y - 0.9f;
        // the way the body faces (the trail is behind it); a followed living entity gives its body yaw, otherwise the trail points back along -z
        float yaw = inst.followYaw(ctx);
        if (Float.isNaN(yaw)) yaw = 0f;
        float fx = -Mth.sin(yaw * Mth.DEG_TO_RAD), fz = Mth.cos(yaw * Mth.DEG_TO_RAD);
        float sx = -fz, sz = fx;

        // ---- the feet: two rings that breathe out, a slow grid, a soft glow
        VfxPose floor = VfxPose.ground(new Vector3f(c.x, feet + 0.05f, c.z));
        for (int k = 0; k < 2; k++) {
            float tt = ((age + 12f * k) % 24f) / 24f;
            ringAt(buf, floor.lift(0.01f * k), 0.45f + 1.35f * VfxAnim.easeOutCubic(tt), a(k == 0 ? CYAN : VIOLET, (1f - tt) * 0.65f * fade));
        }
        buf.plane(GRID, VfxBlend.ADD, floor.lift(0.02f).spin(age * 0.05f), 0.95f / 0.985f, a(VIOLET, 0.4f * fade * shimmer));
        VfxBloom.planeGlow(buf, floor, 0.7f, TEAL, 0.6f * fade);

        // ---- afterimages behind the body: soft humanoid columns, each fainter and further back, swaying
        int ng = ctx.seg(4, 2);
        for (int k = 1; k <= ng; k++) {
            float back = 0.85f * k + 0.12f * Mth.sin(age * 0.2f + k);
            float sway = Mth.sin(age * 0.11f + k * 1.7f) * (0.12f + 0.12f * k);
            float al = (0.52f - 0.11f * k) * fade * (0.7f + 0.3f * Mth.sin(age * 0.5f + k * 2f));
            float vx = -c.x, vz = -c.z, vl = Mth.sqrt(vx * vx + vz * vz);
            float rx = 1f, rz = 0f;
            if (vl > 1e-3f) {
                rx = vz / vl;
                rz = -vx / vl;
            }
            sprite(buf, GHOST, VfxBlend.ADD, c.x - fx * back + sx * sway, c.y + 0.02f, c.z - fz * back + sz * sway, rx, 0, rz, 0, 1, 0, 0.30f + 0.04f * k, 1.0f, a(k % 2 == 0 ? VIOLET : WHITE, al));
        }

        // ---- rift lines round the body: vertical tears that shimmer and jump a little on whole frames
        int nl = ctx.seg(6, 3);
        for (int i = 0; i < nl; i++) {
            float an = Mth.TWO_PI * i / nl + age * 0.02f;
            float rr = 0.52f + 0.1f * Mth.sin(age * 0.13f + i * 1.3f);
            float jit = (h(seed, i, frame) - 0.5f) * 0.14f;
            float x = c.x + Mth.cos(an) * rr + jit, z = c.z + Mth.sin(an) * rr;
            float flick = 0.45f + 0.55f * h(seed, i, (int) Math.floor(age / 3f) + 40);
            float y0 = feet - 0.05f + 0.15f * h(seed, i, 6), y1 = c.y + 1.0f + 0.2f * h(seed, i, 7);
            ribbon(buf, RIFT, VfxBlend.ADD, x, y0, z, x, y1, z, 0.30f, 0.30f, 1f, 0f, a(WHITE, 0.85f * flick * fade * shimmer), a(WHITE, 0.85f * flick * fade * shimmer));
        }

        // ---- glitch slices: three bands of the silhouette that slide sideways for a moment
        for (int k = 0; k < 3; k++) {
            float on = h(seed, k, frame + 90);
            if (on < 0.45f) continue;
            float vTop = 0.12f + 0.25f * k + 0.1f * h(seed, k, 2), vBot = vTop + 0.07f;
            float yT = c.y + 1.0f - vTop * 2f, yB = c.y + 1.0f - vBot * 2f;
            float off = (h(seed, k, frame + 120) - 0.5f) * 0.7f;
            float vx = -c.x, vz = -c.z, vl = Mth.sqrt(vx * vx + vz * vz);
            float rx = 1f, rz = 0f;
            if (vl > 1e-3f) {
                rx = vz / vl;
                rz = -vx / vl;
            }
            float hw = 0.30f;
            float cx = c.x + rx * off, cz = c.z + rz * off;
            quad(buf, GHOST, VfxBlend.ADD, cx - rx * hw, yB, cz - rz * hw, cx + rx * hw, yB, cz + rz * hw, cx + rx * hw, yT, cz + rz * hw, cx - rx * hw, yT, cz - rz * hw,
                    0f, vTop, 1f, vBot, a(ICE, 0.45f * fade), a(CYAN, 0.45f * fade));
        }

        // ---- motes drifting through the body
        int nm = near > 0.01f ? ctx.seg(8, 3) : 0;
        for (int i = 0; i < nm; i++) {
            float u = Mth.frac(age / 48f + h(seed, i, 51));
            float an = h(seed, i, 52) * Mth.TWO_PI + u * 2f, rr = 0.15f + 0.5f * h(seed, i, 53);
            mote(ctx, buf, c.x + Mth.cos(an) * rr, feet + u * 2.1f, c.z + Mth.sin(an) * rr, 0.2f, a(i % 2 == 0 ? CYAN : VIOLET, Mth.sin(u * Mth.PI) * 0.85f * fade * near));
        }
        glow(ctx, buf, c.x, c.y, c.z, 1.6f, INDIGO, 0.45f * fade * shimmer);
    }
}
