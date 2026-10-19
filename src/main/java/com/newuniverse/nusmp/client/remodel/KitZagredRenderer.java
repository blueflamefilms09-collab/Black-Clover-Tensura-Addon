package com.newuniverse.nusmp.client.remodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.NURenderTypes;
import com.newuniverse.nusmp.client.NUShaders;
import com.newuniverse.nusmp.client.geo.GeoAnim;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoModelData;
import com.newuniverse.nusmp.client.geo.GeoModels;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.entity.ZagredBossEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the legacy Zagred (entity nusmp:zagred_legacy) has two things the generic mob renderer does not: the FRESNEL AURA, its model drawn again a little larger through the
 * {@code nusmp:rendertype_zagred_aura} core shader (blood red to crimson, pulsing faster each phase; if the shader cannot load, a dim additive pass stands in), and the TARGET RETICLE, a
 * turning rune ring projected at its target's feet (with four marks closing in while it casts a word). Both were the old renderer's ({@code client/ZagredRenderer}, kept); they are
 * reproduced here around the art kit's model so the boss loses nothing of its presence. The old renderer drew the trident as an item in the hand at the fourth phase; the kit model for that
 * phase (zagred_legacy_form2) carries it. The glow layer is the kit's own (the red eyes, horns, runes and wing veins), pulsing like the old one.
 */
public class KitZagredRenderer extends KitMobRenderer<ZagredBossEntity> {
    private static final ResourceLocation RING = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/particle/koto_ring.png");

    public KitZagredRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "zagred_legacy", Size.fit(), KitProbes.ZAGRED);
    }

    /** The aura: the body again, 4% larger, through the aura shader (or a dim additive stand-in). */
    @Override
    protected void decorate(ZagredBossEntity e, PoseStack pose, MultiBufferSource buffers, GeoSpec spec, String clip, float seconds, int light, float scale, float partial) {
        float k = 0.55f + 0.15f * e.phase() + (e.clientState() == ZagredBossEntity.STATE_CASTING ? 0.3f : 0f);
        int a = (int) (255 * Math.min(1f, k));
        boolean shader = NUShaders.zagredAura() != null;
        int color = shader ? (a << 24) | 0xFFFFFF : 0xFF000000 | ((int) (a * 0.3f) << 16) | ((int) (a * 0.02f) << 8) | (int) (a * 0.04f);
        GeoModelData model = GeoModels.model(spec.model());
        GeoAnim anims = GeoModels.animations(spec.animations());
        GeoAnim.Clip c = anims == null ? null : anims.clip(clip);
        VertexConsumer vc = buffers.getBuffer(NURenderTypes.zagredAura(spec.texture()));
        pose.pushPose();
        pose.scale(1.04f, 1.02f, 1.04f);
        GeoDraw.draw(pose, vc, model, c, seconds, GeoDraw.Space.PROP, PropDraw.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
        pose.popPose();
    }

    /** The target reticle: while it fights or telegraphs a word, a turning rune ring at its target's feet (state from ZagredStatePayload). */
    @Override
    protected void afterBody(ZagredBossEntity e, PoseStack pose, MultiBufferSource buffers, float partial) {
        int state = e.clientState();
        if (state < ZagredBossEntity.STATE_COMBAT || e.clientTarget() < 0) return;
        Entity t = e.level().getEntity(e.clientTarget());
        if (t == null || !t.isAlive()) return;
        Vec3 off = t.getPosition(partial).subtract(e.getPosition(partial));
        reticle(pose, buffers.getBuffer(RenderType.eyes(RING)), off, t.getBbWidth(), e.tickCount + partial, state == ZagredBossEntity.STATE_CASTING);
    }

    /** A turning rune ring at the target's feet; casting adds four marks closing in and a brighter, faster ring. */
    public static void reticle(PoseStack pose, VertexConsumer vc, Vec3 off, float width, float age, boolean casting) {
        pose.pushPose();
        pose.translate(off.x, off.y + 0.06, off.z);
        float r = Math.max(0.8f, width * 1.3f) * (casting ? 1.0f + 0.15f * Mth.sin(age * 0.6f) : 1f);
        pose.mulPose(Axis.YP.rotation(age * (casting ? 0.12f : 0.04f)));
        int k = casting ? 255 : 150;
        flat(pose.last(), vc, r, k, (int) (k * 0.05), (int) (k * 0.35));
        if (casting) {
            pose.mulPose(Axis.YP.rotation(-age * 0.2f));
            flat(pose.last(), vc, r * 0.55f, 220, 10, 30);
        }
        pose.popPose();
    }

    /** A horizontal textured square of half-size r (both faces), full-bright. */
    static void flat(PoseStack.Pose p, VertexConsumer vc, float r, int red, int green, int blue) {
        float[][] c = {{-r, -r, 0, 0}, {r, -r, 1, 0}, {r, r, 1, 1}, {-r, r, 0, 1}};
        for (int side = 0; side < 2; side++)
            for (int i = 0; i < 4; i++) {
                float[] k = c[side == 0 ? i : 3 - i];
                vc.addVertex(p, k[0], 0, k[1]).setColor(red, green, blue, 255).setUv(k[2], k[3]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(0xF000F0).setNormal(p, 0, side == 0 ? 1 : -1, 0);
            }
    }
}
