package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.relics.ExSingularity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Why: the sword of the EX-Singularity grade (Shishou, above Genesis) must look like nothing else in the mod. A Genesis weapon draws its glow twice (an additive pass and an emissive copy, the
 * glow's quads twice). The EX-Singularity sword draws its glow ONCE, but (1) the glow walks through every hue of the spectrum, slowly (one turn in ten seconds, a gentle pulse on top), on a
 * tint-neutral copy of the glow skin so every hue shows; (2) a VEIL: the blade's own quads (about 450, the upper part of the mesh) once more, additively, in the spectrum laid along the blade
 * with a bright band that runs up it every three seconds, so the silver shimmers like oil on water; (3) a few prismatic shards (four-pointed stars) orbit the blade. This class holds the extra
 * passes ({@link #extras}) and the hooks ({@link #glowTex}, {@link #glowRgb}); the hand ({@link WeaponRenderer}) and the world body ({@link ShishouMeshDraw}) call it, so the sword in the hand,
 * the flying sword and the one in the ground cannot differ. Package private mesh access, which is why it sits next to {@link WeaponRenderer}. Render thread only.
 * <p>
 * Worst case per sword per frame (docs/relics/ex_singularity.md): the veil's quads (the list is built once per mesh) plus {@link ExSingularity#MAX_SHARDS} shards of four quads (two crossed planes,
 * both faces): about 450 + 32 quads, FEWER than the glow quads a Genesis weapon draws twice (971 x 2), two extra buffers (the blade skin, the star texture). Nothing is allocated per frame (scratch
 * vector, ints, floats; the veil table is built once when the mesh is first seen or reloaded); the passes are skipped beyond {@link #FAR_BLOCKS} blocks from the camera, like every upgraded
 * weapon's additive passes.
 */
public final class ExSingularityFx {
    private ExSingularityFx() {}

    /** The four-pointed star every shard shows (textures/entity/weapon/ex_singularity_star.png, made by tools/gen_ex_singularity_assets.py). */
    public static final ResourceLocation STAR = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/weapon/ex_singularity_star.png");

    /** The sword's glow skin made tint-neutral (tools/gen_ex_singularity_assets.py): the cyan map of Shishou's mesh could only ever make a dim cyan-ish red or green, this one takes every hue. */
    public static final ResourceLocation PRISM_GLOW = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/weapon/shishou_prism_glow.png");

    /** The extra passes are skipped beyond this many blocks from the camera. */
    public static final double FAR_BLOCKS = 24.0;

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final Vector3f SCRATCH = new Vector3f();
    private static final float[] SHARD = new float[4];

    /** The veil's table: which quads of the body layer are the blade, and how high on the sword each one sits. Built once per mesh object (a resource reload makes a new one). */
    private static WeaponRenderer.Mesh veilFor;
    private static int[] veilStart = new int[0];
    private static float[] veilFrac = new float[0];
    private static int veilCount;

    /** Does the item with this registry path wear the EX-Singularity look? */
    public static boolean applies(String id) { return ExSingularity.isAkashicPath(id); }

    /** The glow texture of a draw: the neutral prism map for the EX-Singularity sword, the mesh's own for everything else. */
    public static ResourceLocation glowTex(String id, WeaponRenderer.Mesh mesh) { return applies(id) ? PRISM_GLOW : mesh.glowTex; }

    /** The colour of the main glow pass at this time (replaces the mesh's own glow colour). */
    public static int glowRgb(float time) { return ExSingularity.glowRgb(time); }

    /** The extra passes, after the main glow: the blade veil and the orbiting shards. {@code pose} is the pose the body was drawn with. */
    public static void extras(PoseStack pose, MultiBufferSource buffers, WeaponRenderer.Mesh mesh, float time) {
        Matrix4f at = pose.last().pose();
        double dx = at.m30(), dy = at.m31(), dz = at.m32();
        if (dx * dx + dy * dy + dz * dz > FAR_BLOCKS * FAR_BLOCKS) return;
        if (veilFor != mesh) buildVeil(mesh);
        if (veilCount > 0 && mesh.tex != null) veil(pose.last(), buffers.getBuffer(SafeAdditive.additive(mesh.tex)), mesh, time);
        shards(pose.last(), buffers.getBuffer(SafeAdditive.additive(STAR)), mesh, time);
    }

    /** Picks the blade's quads out of the body layer once: every quad whose four vertices lie in the upper part of the sword ({@link ExSingularity#VEIL_FROM}). */
    private static void buildVeil(WeaponRenderer.Mesh mesh) {
        List<float[]> body = mesh.layers[WeaponRenderer.BODY];
        float len = mesh.ymax - mesh.ymin;
        int quads = body.size() / 4;
        int[] start = new int[quads];
        float[] frac = new float[quads];
        int n = 0;
        if (len > 0f) {
            for (int q = 0; q < quads; q++) {
                float lo = Float.MAX_VALUE, sum = 0f;
                for (int k = 0; k < 4; k++) {
                    float y = body.get(q * 4 + k)[1];
                    lo = Math.min(lo, y);
                    sum += y;
                }
                float f = (sum / 4f - mesh.ymin) / len;
                if ((lo - mesh.ymin) / len >= ExSingularity.VEIL_FROM) {
                    start[n] = q * 4;
                    frac[n] = Math.min(1f, f);
                    n++;
                }
            }
        }
        veilStart = start;
        veilFrac = frac;
        veilCount = n;
        veilFor = mesh;
    }

    /** The veil: the blade's quads once more with the blade's own skin, additive, one colour per quad from the spectrum along the blade. */
    private static void veil(PoseStack.Pose p, VertexConsumer vc, WeaponRenderer.Mesh mesh, float time) {
        Matrix4f m = p.pose();
        List<float[]> body = mesh.layers[WeaponRenderer.BODY];
        for (int q = 0; q < veilCount; q++) {
            int rgb = ExSingularity.veilRgb(veilFrac[q], time);
            int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
            int s = veilStart[q];
            for (int k = 0; k < 4; k++) {
                float[] v = body.get(s + k);
                m.transformPosition(v[0], v[1], v[2], SCRATCH);
                vc.addVertex(SCRATCH.x, SCRATCH.y, SCRATCH.z).setColor(r, g, b, 255).setUv(v[3] / 128f, v[4] / 128f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
            }
        }
    }

    /** The orbiting shards: two crossed planes each, both faces, in the star texture; colour per shard from the spectrum, brightness from its own twinkle. */
    private static void shards(PoseStack.Pose p, VertexConsumer vc, WeaponRenderer.Mesh mesh, float time) {
        float len = mesh.ymax - mesh.ymin;
        if (len <= 0f) return;
        Matrix4f m = p.pose();
        int n = ExSingularity.MAX_SHARDS;
        for (int i = 0; i < n; i++) {
            ExSingularity.shard(i, n, time, 0.13f, SHARD);
            float cx = SHARD[0] * len, cy = mesh.ymin + SHARD[1] * len, cz = SHARD[2] * len;
            float tw = SHARD[3];
            float hh = len * (0.030f + 0.030f * tw), hw = hh * 0.8f;
            int rgb = ExSingularity.shardRgb(i, n, time);
            int a = 120 + Math.round(135f * tw);
            int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
            plane(vc, m, cx, cy, cz, hw, 0f, hh, r, g, b, a);
            plane(vc, m, cx, cy, cz, 0f, hw, hh, r, g, b, a);
        }
    }

    /** One plane through (cx, cy, cz), half-width (ax, az) across and hh up, both faces (the additive type culls back faces). */
    private static void plane(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float ax, float az, float hh, int r, int g, int b, int a) {
        vert(vc, m, cx - ax, cy + hh, cz - az, 0f, 0f, r, g, b, a);
        vert(vc, m, cx + ax, cy + hh, cz + az, 1f, 0f, r, g, b, a);
        vert(vc, m, cx + ax, cy - hh, cz + az, 1f, 1f, r, g, b, a);
        vert(vc, m, cx - ax, cy - hh, cz - az, 0f, 1f, r, g, b, a);
        vert(vc, m, cx - ax, cy + hh, cz - az, 0f, 0f, r, g, b, a);
        vert(vc, m, cx - ax, cy - hh, cz - az, 0f, 1f, r, g, b, a);
        vert(vc, m, cx + ax, cy - hh, cz + az, 1f, 1f, r, g, b, a);
        vert(vc, m, cx + ax, cy + hh, cz + az, 1f, 0f, r, g, b, a);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        m.transformPosition(x, y, z, SCRATCH);
        vc.addVertex(SCRATCH.x, SCRATCH.y, SCRATCH.z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
    }
}
