package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Shared real-geometry models for Bone, Sand, Slash, Legion and Recombination spell props. */
final class ModeledMagicProps {
    private static final ResourceLocation BONE = mcBlock("bone_block");
    private static final ResourceLocation SAND = mcBlock("sand");
    private static final ResourceLocation SLASH = mcBlock("lime_concrete");
    private static final ResourceLocation WOOD = mcBlock("oak_planks");
    private static final ResourceLocation STONE = mcBlock("polished_blackstone_bricks");
    private static final ResourceLocation GLOW = AuraRender.tex("particle/glow");

    private ModeledMagicProps() {}

    private static ResourceLocation mcBlock(String name) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/" + name + ".png");
    }

    private static VertexConsumer cutout(MultiBufferSource buffers, ResourceLocation tex) {
        return buffers.getBuffer(AuraRender.cutout(tex));
    }

    private static VertexConsumer glow(MultiBufferSource buffers) {
        return buffers.getBuffer(AuraRender.additive(GLOW));
    }

    static void bone(MagicPropEntity e, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = e.scale(), pulse = 0.78f + Mth.sin(age * 0.18f) * 0.12f;
        VertexConsumer solid = cutout(buffers, BONE);
        if (e.kind() == com.newuniverse.nusmp.prop.PropKind.BONE_1) {
            PropDraw.cylinder(pose, solid, 0, 0, -0.68f * s, 0, 0, 0.72f * s, 0.095f * s, 0.07f * s, 8, true, 0xFFF0E8D0, light);
            PropDraw.cylinder(pose, solid, 0, 0, 0.72f * s, 0, 0, 1.08f * s, 0.07f * s, 0, 8, true, 0xFFF8F2E5, light);
            for (int n = 0; n < 5; n++) {
                float z = (-0.48f + n * 0.24f) * s;
                PropDraw.sphere(pose, solid, 0, 0, z, 0.075f * s, 5, 8, 0xFFE9E0CC, light);
            }
        } else {
            float radius = 0.78f * s, height = 1.65f * s;
            for (int n = 0; n < 8; n++) {
                double a = n * Math.PI / 4;
                float x = (float) Math.cos(a) * radius, z = (float) Math.sin(a) * radius;
                float ex = x * 0.35f, ez = z * 0.35f;
                PropDraw.cylinder(pose, solid, x, 0, z, x * 0.88f, height * 0.55f, z * 0.88f, 0.07f * s, 0.055f * s, 7, false, 0xFFEAE2CE, light);
                PropDraw.cylinder(pose, solid, x * 0.88f, height * 0.55f, z * 0.88f, ex, height, ez, 0.055f * s, 0.025f * s, 7, false, 0xFFF7F1E2, light);
                PropDraw.sphere(pose, solid, x * 0.88f, height * 0.55f, z * 0.88f, 0.09f * s, 5, 8, 0xFFF8F2E5, light);
            }
        }
        VertexConsumer emissive = glow(buffers);
        if (e.kind() == com.newuniverse.nusmp.prop.PropKind.BONE_1) {
            for (int n = 0; n < 5; n++) {
                float z = (-0.48f + n * 0.24f) * s;
                PropDraw.torus(pose, emissive, 0, 0, z, 0, 0, 1, 0.11f * s, 0.018f * s, 12, 5, AuraRender.alpha(0xFFC9B9FF, pulse), PropDraw.FULL_BRIGHT);
            }
        } else {
            PropDraw.sphere(pose, emissive, 0, 0.84f * s, 0, 0.26f * s, 8, 12, AuraRender.alpha(0xFFC9B9FF, pulse), PropDraw.FULL_BRIGHT);
            PropDraw.torus(pose, emissive, 0, 0.08f * s, 0, 0, 1, 0, 0.76f * s, 0.035f * s, 24, 6, 0xFFC9B9FF, PropDraw.FULL_BRIGHT);
        }
    }

    static void sand(MagicPropEntity e, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = e.scale();
        VertexConsumer solid = cutout(buffers, SAND);
        if (e.kind() == com.newuniverse.nusmp.prop.PropKind.SAND_1) {
            for (int n = -1; n <= 1; n++) {
                float x = n * 0.24f * s;
                float z = (n == 0 ? 0.52f : 0.34f) * s;
                PropDraw.cylinder(pose, solid, x, -0.18f * s, z, x * 0.55f, 0.1f * s, z + 0.34f * s, 0.15f * s, 0.095f * s, 6, false, 0xFFE4C17E, light);
                PropDraw.cylinder(pose, solid, x * 0.55f, 0.1f * s, z + 0.34f * s, x * 0.2f, 0.22f * s, z + 0.66f * s, 0.095f * s, 0, 6, false, 0xFFFFE0A0, light);
            }
        } else {
            float radius = 1.05f * s;
            for (int n = 0; n < 12; n++) {
                float a = n * Mth.TWO_PI / 12f + age * 0.015f;
                float x = Mth.cos(a) * radius, z = Mth.sin(a) * radius;
                PropDraw.cylinder(pose, solid, x, 0, z, x * 0.8f, 0.52f * s, z * 0.8f, 0.12f * s, 0.025f * s, 6, false, 0xFFE7C785, light);
            }
        }
        PropDraw.torus(pose, glow(buffers), 0, e.kind() == com.newuniverse.nusmp.prop.PropKind.SAND_1 ? -0.05f * s : 0.18f * s, 0,
                0, 1, 0, e.kind() == com.newuniverse.nusmp.prop.PropKind.SAND_1 ? 0.72f * s : 1.05f * s,
                e.kind() == com.newuniverse.nusmp.prop.PropKind.SAND_1 ? 0.025f * s : 0.045f * s, 32, 6,
                e.kind() == com.newuniverse.nusmp.prop.PropKind.SAND_1 ? 0xAFFFF0B0 : 0xCCFFE4A1, PropDraw.FULL_BRIGHT);
    }

    static void slash(MagicPropEntity e, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = e.scale();
        VertexConsumer solid = cutout(buffers, SLASH);
        if (e.kind() == com.newuniverse.nusmp.prop.PropKind.SLASH_1) {
            for (int n = -1; n <= 1; n++) {
                float x = n * 0.22f * s;
                PropDraw.cylinder(pose, solid, x - 0.24f * s, -0.38f * s, 0.1f * s, x + 0.22f * s, 0.38f * s, 1.0f * s,
                        0.105f * s, 0, 5, false, 0xFFB9FFBF, light);
            }
        } else {
            float sweep = 0.25f * Mth.sin(age * 0.14f);
            PropDraw.cylinder(pose, solid, 0, -0.9f * s, 0, 0, 0.75f * s, 0, 0.045f * s, 0.03f * s, 7, false, 0xFF438D53, light);
            for (int n = 0; n < 10; n++) {
                float t0 = n / 10f, t1 = (n + 1) / 10f;
                float a0 = -1.0f + t0 * 2.25f + sweep, a1 = -1.0f + t1 * 2.25f + sweep;
                float r0 = 0.3f + t0 * 0.8f, r1 = 0.3f + t1 * 0.8f;
                PropDraw.cylinder(pose, solid, Mth.cos(a0) * r0 * s, 0.25f * s + Mth.sin(a0) * r0 * s, 0,
                        Mth.cos(a1) * r1 * s, 0.25f * s + Mth.sin(a1) * r1 * s, 0, 0.09f * s * (1 - t0), 0.09f * s * (1 - t1), 6, false, 0xFFD7FFDB, light);
            }
        }
        VertexConsumer emissive = glow(buffers);
        if (e.kind() == com.newuniverse.nusmp.prop.PropKind.SLASH_1) {
            for (int n = -1; n <= 1; n++) {
                float x = n * 0.22f * s;
                PropDraw.cylinder(pose, emissive, x - 0.24f * s, -0.38f * s, 0.1f * s, x + 0.22f * s, 0.38f * s, 1.0f * s,
                        0.045f * s, 0, 5, false, 0xFF63FF83, PropDraw.FULL_BRIGHT);
            }
        } else {
            PropDraw.torus(pose, emissive, 0, 0, 0, 0, 0, 1, 0.68f * s, 0.035f * s, 28, 6, 0xFF70FF8D, PropDraw.FULL_BRIGHT);
        }
    }

    static void legionPiece(MagicPropEntity e, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = e.scale();
        int piece = Math.floorMod(e.param(), 5);
        VertexConsumer solid = cutout(buffers, piece == 0 || piece == 2 ? STONE : WOOD);
        PropDraw.cylinder(pose, solid, 0, 0, 0, 0, 0.1f * s, 0, 0.27f * s, 0.21f * s, 12, true, 0xFFE8D6D8, light);
        float tall = (piece == 4 ? 0.92f : piece == 1 ? 0.78f : 0.64f) * s;
        if (piece == 0) {
            PropDraw.sphere(pose, solid, 0, tall * 0.72f, 0, 0.15f * s, 8, 12, 0xFFF7E7E8, light);
            PropDraw.cylinder(pose, solid, 0, 0.12f * s, 0, 0, tall * 0.65f, 0, 0.13f * s, 0.09f * s, 10, true, 0xFFE8D6D8, light);
        } else if (piece == 1) {
            PropDraw.cylinder(pose, solid, 0, 0.12f * s, 0, 0, tall * 0.8f, 0, 0.16f * s, 0.12f * s, 10, true, 0xFFE8D6D8, light);
            PropDraw.box(pose, solid, -0.22f * s, tall * 0.8f, -0.22f * s, 0.22f * s, tall, 0.22f * s, 0xFFF7E7E8, light);
        } else {
            PropDraw.cylinder(pose, solid, 0, 0.12f * s, 0, 0, tall * 0.75f, 0, 0.15f * s, 0.07f * s, 10, true, 0xFFE8D6D8, light);
            PropDraw.sphere(pose, solid, 0, tall * 0.82f, 0, (piece == 2 ? 0.21f : 0.18f) * s, 8, 12, 0xFFF7E7E8, light);
        }
        VertexConsumer emissive = glow(buffers);
        PropDraw.torus(pose, emissive, 0, 0.13f * s, 0, 0, 1, 0, 0.22f * s, 0.025f * s, 16, 5, 0xFFFF3658, PropDraw.FULL_BRIGHT);
        if (piece == 3 || piece == 4)
            PropDraw.torus(pose, emissive, 0, tall * 0.82f, 0, 0, 1, 0, 0.24f * s, 0.025f * s, 16, 5, 0xFFFF5270, PropDraw.FULL_BRIGHT);
    }

    static void legionBoard(MagicPropEntity e, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = e.scale(), tile = s / 4f, y = 0.015f * s;
        for (int x = 0; x < 8; x++) for (int z = 0; z < 8; z++) {
            float x0 = (x - 3.5f) * tile, z0 = (z - 3.5f) * tile;
            boolean dark = ((x + z) & 1) == 0;
            PropDraw.box(pose, cutout(buffers, dark ? STONE : WOOD), x0, y, z0, x0 + tile, y + 0.025f * s, z0 + tile,
                    ((x + z) & 1) == 0 ? 0xFF322D3A : 0xFFE5D7C8, light);
        }
        PropDraw.torus(pose, glow(buffers), 0, y, 0, 0, 1, 0, s * 1.02f, 0.025f * s, 48, 6, 0xFFFF3658, PropDraw.FULL_BRIGHT);
    }

    static void recombination(MagicPropEntity e, float age, PoseStack pose, MultiBufferSource buffers, int light) {
        float s = e.scale(), t = e.lifeFrac();
        VertexConsumer wood = cutout(buffers, WOOD);
        float rise = Mth.clamp(t * 3f, 0.15f, 1f);
        float half = 0.72f * s * rise;
        // A small magic-house construct: timber walls, roof frame, and a slowly turning plank halo.
        PropDraw.box(pose, wood, -half, 0, -half, half, 0.92f * s * rise, half, 0xFF9D6C43, light);
        PropDraw.box(pose, wood, -0.82f * s * rise, 0.88f * s * rise, -0.84f * s * rise,
                0.82f * s * rise, 1.02f * s * rise, 0.84f * s * rise, 0xFFC58B51, light);
        for (int n = 0; n < 4; n++) {
            float a = age * 0.025f + n * Mth.HALF_PI;
            float x = Mth.cos(a) * 1.05f * s, z = Mth.sin(a) * 1.05f * s;
            PropDraw.box(pose, wood, x - 0.09f * s, 0.35f * s, z - 0.09f * s,
                    x + 0.09f * s, 0.48f * s, z + 0.09f * s, 0xFFB27C48, light);
        }
        PropDraw.torus(pose, glow(buffers), 0, 0.5f * s, 0, 0, 1, 0, 1.22f * s, 0.035f * s, 32, 6,
                AuraRender.alpha(0xFFFFC678, rise), PropDraw.FULL_BRIGHT);
    }
}
