package com.newuniverse.nusmp.client.heroweapon.bow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.heroweapon.weapon.bow.BowArrowForms;
import com.newuniverse.nusmp.heroweapon.weapon.bow.HeroArrow;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the Legendary Bow's arrows must look like vanilla arrows ("regular Minecraft arrows, just reskinned") with a small glow trail in the colour of the bow
 * form. This renderer IS the vanilla arrow renderer (same model, same draw), with the texture chosen by the form number the arrow carries in its synced data
 * and one extra thing after it: a soft additive ribbon (two crossed quads, eight vertices) along the way the arrow just came, fading to nothing, in the form's
 * colour. Cost per arrow per frame: the vanilla arrow plus 8 vertices; the ribbon is skipped for an arrow that is farther than 48 blocks from the camera or
 * that is not moving (a stuck arrow has no trail). Nothing is allocated per frame (the texture locations are built once, the maths are floats). Client only.
 */
public class HeroArrowRenderer extends ArrowRenderer<HeroArrow> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[BowArrowForms.count()];
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("anime_multiverse", BowArrowForms.GLOW_PATH);
    private static final double TRAIL_DISTANCE_SQ = 48.0 * 48.0;
    private static final int FULL_BRIGHT = 0xF000F0;

    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = ResourceLocation.fromNamespaceAndPath("anime_multiverse", BowArrowForms.texturePath(BowArrowForms.byIndex(i)));
        }
    }

    public HeroArrowRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(HeroArrow arrow) {
        int i = arrow.lookIndex();
        return TEXTURES[i < 0 || i >= TEXTURES.length ? 0 : i];
    }

    @Override
    public void render(HeroArrow arrow, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(arrow, yaw, partial, pose, buffers, light);
        if (this.entityRenderDispatcher.distanceToSqr(arrow) > TRAIL_DISTANCE_SQ) return;
        Vec3 v = arrow.getDeltaMovement();
        double speed = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        if (speed < 0.3) return;
        float nx = (float) (v.x / speed), ny = (float) (v.y / speed), nz = (float) (v.z / speed);
        float len = (float) Math.min(2.6, speed * 1.2);
        // a perpendicular pair: p1 = n x up (or x when flying straight up or down), p2 = n x p1
        float p1x = -nz, p1y = 0f, p1z = nx;
        float pl = (float) Math.sqrt(p1x * p1x + p1z * p1z);
        if (pl < 1.0E-3f) {
            p1x = 1f;
            p1z = 0f;
        } else {
            p1x /= pl;
            p1z /= pl;
        }
        float p2x = ny * p1z - nz * p1y, p2y = nz * p1x - nx * p1z, p2z = nx * p1y - ny * p1x;
        int rgb = BowArrowForms.byIndex(arrow.lookIndex()).rgb();
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        float w = 0.11f, tail = 0.25f;
        float ex = -nx * len, ey = -ny * len, ez = -nz * len;
        VertexConsumer vc = buffers.getBuffer(SafeAdditive.additive(GLOW));
        PoseStack.Pose last = pose.last();
        quad(vc, last, 0f, 0f, 0f, ex, ey, ez, p1x * w, p1y * w, p1z * w, tail, r, g, b);
        quad(vc, last, 0f, 0f, 0f, ex, ey, ez, p2x * w, p2y * w, p2z * w, tail, r, g, b);
    }

    /** One ribbon quad from the arrow (full width, bright) to {@code e} (a fifth of the width, clear). */
    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float ax, float ay, float az, float ex, float ey, float ez, float sx, float sy, float sz,
                             float tail, int r, int g, int b) {
        vertex(vc, pose, ax + sx, ay + sy, az + sz, 0f, 0f, r, g, b, 190);
        vertex(vc, pose, ax - sx, ay - sy, az - sz, 1f, 0f, r, g, b, 190);
        vertex(vc, pose, ex - sx * tail, ey - sy * tail, ez - sz * tail, 1f, 1f, r, g, b, 0);
        vertex(vc, pose, ex + sx * tail, ey + sy * tail, ez + sz * tail, 0f, 1f, r, g, b, 0);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        vc.addVertex(pose, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0f, 1f, 0f);
    }
}
