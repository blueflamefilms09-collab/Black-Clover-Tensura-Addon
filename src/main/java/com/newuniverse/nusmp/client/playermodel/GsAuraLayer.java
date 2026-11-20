package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.goku.GokuBoss;
import com.newuniverse.nusmp.arc.goku.GokuLadder;
import com.newuniverse.nusmp.client.NURenderTypes;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/**
 * Why: Goku's form aura on the redesigned body (docs/boss_redesign_goku_sinbad.md): the posed model drawn once more, a little larger, through the mod's fresnel aura render type (the same one Zagred's
 * aura and the Djinn Full Equip use, with its additive fallback when the shader cannot load), in the colour of the ladder step ({@link GokuLadder#rgb}). Base has none; the strength grows with the step,
 * the lightning forms flicker faster. One extra draw of the 4-part model (about 6 cubes of 6 quads), skipped beyond 48 blocks and when invisible; nothing allocated per frame.
 */
public class GsAuraLayer extends RenderLayer<ArcBoss, PlayerModel<ArcBoss>> {
    private static final double FAR2 = 48.0 * 48.0;
    private final PlayerModelRenderer.Body<ArcBoss> owner;

    public GsAuraLayer(PlayerModelRenderer.Body<ArcBoss> owner) {
        super(owner);
        this.owner = owner;
    }

    /** The aura strength 0..1 of a step: none for Base, then rising (pure, tested). */
    public static float strength(int step) { return step <= 0 ? 0f : Math.min(1f, 0.28f + 0.08f * step); }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, ArcBoss e, float limbSwing, float limbSwingAmount, float partial, float age, float netHeadYaw, float headPitch) {
        if (!(e instanceof GokuBoss g) || e.isInvisible() || owner.look() == null) return;
        int step = g.formIdx();
        float k = strength(step);
        if (k <= 0f) return;
        double d2 = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(e.getX(), e.getY(), e.getZ());
        if (d2 > FAR2) return;
        float flick = GokuLadder.electric(step) ? 0.75f + 0.25f * Mth.sin(age * 1.7f) : 0.85f + 0.15f * Mth.sin(age * 0.25f);
        int rgb = GokuLadder.rgb(step) & 0xFFFFFF;
        int a = (int) (255 * Math.min(1f, k * flick));
        pose.pushPose();
        pose.scale(1.06f, 1.03f, 1.06f);
        getParentModel().renderToBuffer(pose, buffers.getBuffer(NURenderTypes.zagredAura(getTextureLocation(e))), PropDraw.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, (a << 24) | rgb);
        pose.popPose();
    }
}
