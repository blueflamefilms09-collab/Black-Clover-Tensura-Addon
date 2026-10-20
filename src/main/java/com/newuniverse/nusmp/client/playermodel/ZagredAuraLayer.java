package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.NURenderTypes;
import com.newuniverse.nusmp.client.NUShaders;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.remodel.KitZagredRenderer;
import com.newuniverse.nusmp.entity.ZagredBossEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the legacy Zagred has two things no other character has, and the old renderers kept them: the FRESNEL AURA (his body drawn again a little larger through the
 * {@code nusmp:rendertype_zagred_aura} shader, blood red to crimson, pulsing faster each phase; a dim additive pass stands in when the shader cannot load) and the TARGET RETICLE (a turning rune
 * ring at his target's feet, with four marks closing in while he casts a word). The aura is this layer, drawn with the posed player model itself; the reticle is {@link #reticle}, drawn at the
 * entity's feet by his {@link PlayerBodies.LegacyZagred} body through the kit's decoration hook, using the old renderer's own drawing.
 */
public class ZagredAuraLayer extends RenderLayer<ZagredBossEntity, PlayerModel<ZagredBossEntity>> {
    private final PlayerModelRenderer.Body<ZagredBossEntity> owner;

    public ZagredAuraLayer(PlayerModelRenderer.Body<ZagredBossEntity> owner) {
        super(owner);
        this.owner = owner;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, ZagredBossEntity e, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw,
                       float headPitch) {
        if (e.isInvisible() || owner.look() == null) return;
        float k = 0.55f + 0.15f * e.phase() + (e.clientState() == ZagredBossEntity.STATE_CASTING ? 0.3f : 0f);
        int a = (int) (255 * Math.min(1f, k));
        boolean shader = NUShaders.zagredAura() != null;
        int color = shader ? (a << 24) | 0xFFFFFF : 0xFF000000 | ((int) (a * 0.3f) << 16) | ((int) (a * 0.02f) << 8) | (int) (a * 0.04f);
        pose.pushPose();
        pose.scale(1.04f, 1.02f, 1.04f);
        getParentModel().renderToBuffer(pose, buffers.getBuffer(NURenderTypes.zagredAura(getTextureLocation(e))), PropDraw.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
        pose.popPose();
    }

    /** The target reticle: while he fights or telegraphs a word, a turning rune ring at his target's feet (the state comes from ZagredStatePayload). */
    public static void reticle(ZagredBossEntity e, float partial, PlayerBody.Decoration deco) {
        int state = e.clientState();
        if (state < ZagredBossEntity.STATE_COMBAT || e.clientTarget() < 0) return;
        Entity t = e.level().getEntity(e.clientTarget());
        if (t == null || !t.isAlive()) return;
        Vec3 off = t.getPosition(partial).subtract(e.getPosition(partial));
        KitZagredRenderer.reticle(deco.pose(), deco.buffers().getBuffer(com.newuniverse.nusmp.client.AdditiveTypes.of(PlayerAssets.rl("textures/particle/koto_ring.png"))), off, t.getBbWidth(), e.tickCount + partial,
                state == ZagredBossEntity.STATE_CASTING);
    }
}
