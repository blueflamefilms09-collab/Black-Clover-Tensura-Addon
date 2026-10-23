package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.spiritblades.SpiritBladesFxMath;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Why: Spirit of Zephyr and Spirit of Boreas in hand carry a few drifting green-white leaves (the wind spirit's light). This class holds the one extra pass: at most
 * {@link SpiritBladesFxMath#MAX_LEAVES} leaves, each two crossed planes drawn on both faces (4 quads), placed by pure maths ({@link SpiritBladesFxMath}), additive and full bright, in the leaf
 * texture (textures/entity/weapon/spirit_blades_leaf.png, made by tools/gen_spirit_blades_models.py). The weapon renderer calls it after the glow pass ({@link WeaponRenderer}); the mesh is already
 * placed, so a leaf is just a point in mesh space. Worst case per weapon per frame: {@code 6 x 4 = 24} quads (96 vertices), one extra buffer; skipped beyond {@link #FAR_BLOCKS} blocks from the
 * camera, and switched off with {@code -Dnusmp.spiritBlades.leaves=off}. Nothing is allocated per frame (scratch vector and array); render thread only.
 */
public final class SpiritBladesFx {
    private SpiritBladesFx() {}

    public static final ResourceLocation LEAF = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/weapon/spirit_blades_leaf.png");
    /** The leaves are skipped beyond this many blocks from the camera. */
    public static final double FAR_BLOCKS = 24.0;

    private static final boolean ON = !"off".equalsIgnoreCase(System.getProperty("nusmp.spiritBlades.leaves", "on"));
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final Vector3f SCRATCH = new Vector3f();
    private static final float[] LEAF_OUT = new float[SpiritBladesFxMath.FIELDS];

    /** Is this item (by registry path) one of the two blades, and are the leaves on? */
    public static boolean applies(String id) { return ON && ("spirit_of_zephyr".equals(id) || "spirit_of_boreas".equals(id)); }

    /** The leaves, after the main glow. {@code pose} is the pose the body was drawn with (mesh space). */
    public static void extras(PoseStack pose, MultiBufferSource buffers, WeaponRenderer.Mesh mesh, float time) {
        float len = mesh.ymax - mesh.ymin;
        if (len <= 0f) return;
        Matrix4f m = pose.last().pose();
        double dx = m.m30(), dy = m.m31(), dz = m.m32();
        if (dx * dx + dy * dy + dz * dz > FAR_BLOCKS * FAR_BLOCKS) return;
        VertexConsumer vc = buffers.getBuffer(SafeAdditive.additive(LEAF));
        for (int i = 0; i < SpiritBladesFxMath.MAX_LEAVES; i++) {
            SpiritBladesFxMath.leaf(i, time, LEAF_OUT);
            float a = LEAF_OUT[4];
            if (a < 0.03f) continue;
            float cx = LEAF_OUT[1] * len, cy = mesh.ymin + LEAF_OUT[0] * len, cz = LEAF_OUT[2] * len;
            float h = LEAF_OUT[3] * len, c = (float) Math.cos(LEAF_OUT[5]) * h, s = (float) Math.sin(LEAF_OUT[5]) * h;
            int al = Math.round(255f * a);
            // plane one: spun about the weapon's axis; plane two: across it (the leaf shows from every side)
            plane(vc, m, cx, cy, cz, c, s, h, al);
            plane(vc, m, cx, cy, cz, -s, c, h, al);
        }
    }

    /** One plane through (cx, cy, cz): half-width (ax, az) across the weapon's axis and half-height uy along it, both faces. */
    private static void plane(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float ax, float az, float uy, int al) {
        float x0 = cx - ax, z0 = cz - az, x1 = cx + ax, z1 = cz + az;
        float yt = cy + uy, yb = cy - uy;
        vert(vc, m, x0, yt, z0, 0f, 0f, al);
        vert(vc, m, x1, yt, z1, 1f, 0f, al);
        vert(vc, m, x1, yb, z1, 1f, 1f, al);
        vert(vc, m, x0, yb, z0, 0f, 1f, al);
        vert(vc, m, x0, yt, z0, 0f, 0f, al);
        vert(vc, m, x0, yb, z0, 0f, 1f, al);
        vert(vc, m, x1, yb, z1, 1f, 1f, al);
        vert(vc, m, x1, yt, z1, 1f, 0f, al);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int al) {
        m.transformPosition(x, y, z, SCRATCH);
        vc.addVertex(SCRATCH.x, SCRATCH.y, SCRATCH.z).setColor(255, 255, 255, al).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(0f, 1f, 0f);
    }
}
