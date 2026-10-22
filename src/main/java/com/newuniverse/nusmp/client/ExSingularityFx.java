package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.relics.ExSingularity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Why: the sword of the EX-Singularity grade (Shishou, above Genesis) must look like nothing else in the mod. A Genesis weapon draws its glow once in its own colour plus one emissive copy; the
 * EX-Singularity sword draws the SAME two glow passes (same quads, same cost) but (1) the main glow walks through every hue of the spectrum, slowly (one turn in ten seconds, a gentle pulse on top),
 * (2) the second pass is a slightly fatter halo in the OPPOSITE hue, so the blade shows a prism edge, and (3) a few prismatic shards (four-pointed stars) orbit the blade. This class holds only the
 * extra passes ({@link #extras}) and the colour hook ({@link #glowRgb}); the hand ({@link WeaponRenderer}) and the world body ({@link ShishouMeshDraw}) call it, so the sword in the hand, the flying
 * sword and the one in the ground cannot differ. Package private mesh access, which is why it sits next to {@link WeaponRenderer}. Render thread only.
 * <p>
 * Worst case per sword per frame (docs/relics/ex_singularity.md): the glow quads once more (the halo; a Genesis weapon also draws its glow twice), plus {@link ExSingularity#MAX_SHARDS} shards
 * of four quads (two crossed planes, both faces): 32 quads, 128 vertices, one extra buffer (the star texture). Nothing is allocated per frame (scratch vector, ints, floats); the passes are skipped
 * beyond {@link #FAR_BLOCKS} blocks from the camera, like every upgraded weapon's additive passes.
 */
public final class ExSingularityFx {
    private ExSingularityFx() {}

    /** The four-pointed star every shard shows (textures/entity/weapon/ex_singularity_star.png, made by tools/gen_ex_singularity_assets.py). */
    public static final ResourceLocation STAR = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/weapon/ex_singularity_star.png");

    /** The extra passes are skipped beyond this many blocks from the camera. */
    public static final double FAR_BLOCKS = 24.0;

    private static final int FULL_BRIGHT = 0xF000F0;
    private static final Vector3f SCRATCH = new Vector3f();
    private static final float[] SHARD = new float[4];

    /** Does the item with this registry path wear the EX-Singularity look? */
    public static boolean applies(String id) { return ExSingularity.isAkashicPath(id); }

    /** The colour of the main glow pass at this time (replaces the mesh's own glow colour). */
    public static int glowRgb(float time) { return ExSingularity.glowRgb(time); }

    /**
     * The extra passes, after the main glow: the opposite-hue halo (the glow quads again, a touch fatter) and the orbiting shards. {@code pose} is the pose the body was drawn with.
     */
    public static void extras(PoseStack pose, MultiBufferSource buffers, WeaponRenderer.Mesh mesh, float time) {
        Matrix4f at = pose.last().pose();
        double dx = at.m30(), dy = at.m31(), dz = at.m32();
        if (dx * dx + dy * dy + dz * dz > FAR_BLOCKS * FAR_BLOCKS) return;
        if (mesh.glowTex != null && !mesh.layers[WeaponRenderer.GLOW].isEmpty()) {
            pose.pushPose();
            pose.scale(1.06f, 1f, 1.06f);
            int a = ExSingularity.haloAlpha(time);
            WeaponRenderer.emit(pose.last(), buffers.getBuffer(SafeAdditive.emissive(mesh.glowTex)), mesh.layers[WeaponRenderer.GLOW], a << 24 | ExSingularity.haloRgb(time), FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        shards(pose.last(), buffers.getBuffer(SafeAdditive.additive(STAR)), mesh, time);
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
