package com.newuniverse.nusmp.client.relics.djinn;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.NURenderTypes;
import com.newuniverse.nusmp.client.NUShaders;
import com.newuniverse.nusmp.client.relics.djinn.DjinnClientState.Snap;
import com.newuniverse.nusmp.client.relics.djinn.DjinnMeshes.Piece;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Djinn's temporary armour, drawn on the player's own model and never put in an inventory or an armour slot (a crash or a relog cannot cost anyone
 * their real gear). It reads the synced state of the player ({@link DjinnClientState}), so everyone in sight sees it, and follows the model's poses
 * (crouching, swimming, swinging) because each piece is attached to the model part it was built for.
 * <ul>
 *   <li><b>Lesser Summon</b>: a small translucent bust of the Djinn above the right shoulder (a billboard).</li>
 *   <li><b>Half Equip</b>: the bearer's main-hand arm takes a gauntlet, a vambrace and a pauldron, a small crest sits on the brow, and the weapon in
 *       hand is sheathed in the Djinn's element (additive blades, flickering).</li>
 *   <li><b>Full Equip (Maso)</b>: the whole set: helm with the horns, ears and hair, chest and belt with tassets, both pauldrons, vambraces,
 *       gauntlets, greaves, the wings (two articulated segments that flap), a tail of chained segments, a halo; the body-art on the skin as a second
 *       additive pass with glowing eyes; and a fresnel aura through the mod's own Zagred shader, tinted in the Djinn's colour.</li>
 * </ul>
 * Pieces assemble over about twenty ticks (each flies in from its own side and fades up) and shatter out when the phase ends. The meshes are the
 * pieces of tools/relics/gen_djinn_art.py; drawing costs nothing but the vertex writes (no allocation per frame), and a player more than 48 blocks
 * away is skipped, one more than 24 away loses the small details.
 */
public class DjinnArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public DjinnArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) { super(parent); }

    private static final double FAR2 = 48.0 * 48.0, DETAIL2 = 24.0 * 24.0;
    private static final net.minecraft.resources.ResourceLocation SILHOUETTE = DjinnMeshes.rl("textures/entity/djinn/djinn_silhouette.png");

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount, float partial,
                       float age, float yawHead, float pitch) {
        if (p.isInvisible() || p.isSpectator()) return;
        Minecraft mc = Minecraft.getInstance();
        Snap s = DjinnClientState.of(p.getId());
        Djinn d = s.djinn();
        if (d == null || mc.level == null || mc.gameRenderer.getMainCamera() == null) return;
        boolean showsAnything = s.phase() > 0 || s.prev() > 0 || s.marked();
        if (!showsAnything) return;
        double dist2 = mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(p.getX(), p.getY(), p.getZ());
        if (dist2 > FAR2) return;
        boolean detail = dist2 < DETAIL2;
        long gt = mc.level.getGameTime();
        float now = gt + partial;
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        HumanoidArm main = p.getMainArm();
        float glow = 0.85f + 0.15f * Mth.sin(now * 0.12f) + 0.6f * s.pulse(gt, partial) + (s.phase() >= 4 ? 0.35f : 0f);

        if (s.phase() == 1 || (s.phase() == 0 && s.prev() == 1 && now - s.since() < 8)) bust(pose, buffers, p, d, s, gt, now, partial);

        // the pieces of the Half Equip set belong to the main-hand arm; the rest wait for the Maso
        boolean mainRight = main == HumanoidArm.RIGHT;
        Piece pauM = mainRight ? Piece.PAULDRON_R : Piece.PAULDRON_L, pauO = mainRight ? Piece.PAULDRON_L : Piece.PAULDRON_R;
        Piece vamM = mainRight ? Piece.VAMBRACE_R : Piece.VAMBRACE_L, vamO = mainRight ? Piece.VAMBRACE_L : Piece.VAMBRACE_R;
        Piece gauM = mainRight ? Piece.GAUNTLET_R : Piece.GAUNTLET_L, gauO = mainRight ? Piece.GAUNTLET_L : Piece.GAUNTLET_R;
        ModelPart armM = mainRight ? m.rightArm : m.leftArm, armO = mainRight ? m.leftArm : m.rightArm;

        // ---- head
        if (detail) part(pose, buffers, m.head, d, Piece.CREST, s, 2, 0, 0f, -8f, -8f, light, now, glow);
        part(pose, buffers, m.head, d, Piece.HELM, s, 3, 0, 0f, -16f, 0f, light, now, glow);
        part(pose, buffers, m.head, d, Piece.HORN_L, s, 3, 3, 6f, -12f, 0f, light, now, glow);
        part(pose, buffers, m.head, d, Piece.HORN_R, s, 3, 3, -6f, -12f, 0f, light, now, glow);
        if (detail) part(pose, buffers, m.head, d, Piece.EARS, s, 3, 4, 0f, -5f, 0f, light, now, glow);
        // ---- body
        part(pose, buffers, m.body, d, Piece.CHEST, s, 3, 2, 0f, 0f, -14f, light, now, glow);
        part(pose, buffers, m.body, d, Piece.BELT, s, 3, 5, 0f, 10f, -8f, light, now, glow);
        // ---- arms: main hand from the Half Equip, the other from the Maso
        part(pose, buffers, armM, d, pauM, s, 2, 1, mainRight ? -14f : 14f, -4f, 0f, light, now, glow);
        part(pose, buffers, armM, d, vamM, s, 2, 2, mainRight ? -12f : 12f, 2f, -6f, light, now, glow);
        part(pose, buffers, armM, d, gauM, s, 2, 3, mainRight ? -10f : 10f, 8f, -6f, light, now, glow);
        part(pose, buffers, armO, d, pauO, s, 3, 3, mainRight ? 14f : -14f, -4f, 0f, light, now, glow);
        part(pose, buffers, armO, d, vamO, s, 3, 4, mainRight ? 12f : -12f, 2f, -6f, light, now, glow);
        part(pose, buffers, armO, d, gauO, s, 3, 5, mainRight ? 10f : -10f, 8f, -6f, light, now, glow);
        // ---- legs
        part(pose, buffers, m.rightLeg, d, Piece.GREAVE_R, s, 3, 6, -3f, 14f, -6f, light, now, glow);
        part(pose, buffers, m.leftLeg, d, Piece.GREAVE_L, s, 3, 7, 3f, 14f, -6f, light, now, glow);
        // ---- the back: wings, tail, halo
        float a = assembled(s, 3, 8, now);
        if (a != 0f) {
            wings(pose, buffers, p, m, d, a, light, now, glow);
            if (detail) tail(pose, buffers, p, m, d, a, light, now, glow);
            halo(pose, buffers, m, d, a, light, now, glow);
        }
        // ---- the weapon in hand
        if (detail && !p.getMainHandItem().isEmpty()) sheath(pose, buffers, p, m, d, s, main, now);
        // ---- the body-art and the aura over the whole model
        float body = Math.max(a, 0f);
        if (body > 0.02f || s.marked()) {
            float k = s.phase() >= 3 ? body : (s.marked() ? 0.35f : 0f);
            float v = Math.min(1f, k * (0.75f + 0.25f * Mth.sin(now * 0.2f) + 0.5f * s.pulse(gt, partial)));
            if (v > 0.02f) {
                int rgb = scale(d.glow(), v);
                m.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(DjinnMeshes.rune(d))), DjinnDraw.FULL, OverlayTexture.NO_OVERLAY, 0xFF000000 | rgb);
            }
            if (s.phase() >= 3 && body > 0.05f && NUShaders.zagredAura() != null) {
                int aa = (int) (255 * Math.min(1f, body * (0.6f + 0.4f * s.pulse(gt, partial) + 0.25f * (s.phase() >= 4 ? 1f : 0f))));
                pose.pushPose();
                pose.scale(1.05f, 1.025f, 1.05f);
                m.renderToBuffer(pose, buffers.getBuffer(NURenderTypes.zagredAura(SILHOUETTE)), DjinnDraw.FULL,
                        OverlayTexture.NO_OVERLAY, (aa << 24) | (tintSafe(d.glow())));
                pose.popPose();
            }
        }
    }

    /** The glow colour as the shader's custom tint (it treats white as "use the default colours", so a pure white is nudged). */
    private static int tintSafe(int rgb) { return (rgb & 0xFFFFFF) == 0xFFFFFF ? 0xFFFFFE : rgb & 0xFFFFFF; }

    private static int scale(int rgb, float k) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * k)), g = Math.min(255, (int) (((rgb >> 8) & 255) * k)), b = Math.min(255, (int) ((rgb & 255) * k));
        return (r << 16) | (g << 8) | b;
    }

    // ================================================================ assembling
    /**
     * How far a piece is assembled: 1 fully on, between 0 and 1 forging in, negative shattering out ({@code -progress}), 0 not there. {@code minPhase}
     * is the phase it belongs to, {@code delay} staggers the pieces of one set.
     */
    static float assembled(Snap s, int minPhase, int delay, float now) {
        int cur = Math.min(s.phase(), 3);
        if (cur >= minPhase) {
            if (s.prev() >= minPhase) return 1f;
            return Mth.clamp((now - s.since() - delay) / 8f, 0f, 1f);
        }
        if (s.prev() >= minPhase) {
            float p = (now - s.since() - delay * 0.5f) / 12f;
            return p >= 1f ? 0f : -Mth.clamp(p, 0.001f, 1f);
        }
        return 0f;
    }

    /** One piece on one model part, forged in from the direction (dx, dy, dz) in model pixels. */
    private void part(PoseStack pose, MultiBufferSource buffers, ModelPart anchor, Djinn d, Piece piece, Snap s, int minPhase, int delay, float dx, float dy, float dz,
                      int light, float now, float glow) {
        float a = assembled(s, minPhase, delay, now);
        if (a == 0f) return;
        pose.pushPose();
        anchor.translateAndRotate(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        float alpha = 1f, sc = 1f;
        if (a < 1f) {
            float off;
            if (a > 0) { off = (1f - a) * (1f - a); alpha = a; sc = 0.85f + 0.15f * a; }
            else { float p = -a; off = p * p * 1.4f; alpha = 1f - p; sc = 1f + 0.25f * p; }
            pose.translate(dx * off, dy * off, dz * off);
            pose.scale(sc, sc, sc);
        }
        DjinnDraw.piece(pose, buffers, d, piece, light, alpha, glow * Math.max(0.0f, alpha));
        pose.popPose();
    }

    // ================================================================ the back
    private void wings(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer p, PlayerModel<AbstractClientPlayer> m, Djinn d, float a, int light, float now, float glow) {
        if (d.look().wings() == Djinn.Look.Wings.NONE) return;
        boolean air = !p.onGround() && !p.isInWater();
        boolean fold = p.isFallFlying() || p.isSwimming();
        float beat = Mth.sin(now * (air ? 0.34f : 0.12f)) * (air ? 16f : 7f);
        float lift = fold ? 8f : 36f + beat, sweep = fold ? 62f : 20f - beat * 0.35f, bend = fold ? 120f : 62f - beat * 0.6f;
        float alpha = a > 0 ? a : 1f + a, out = a > 0 ? (1f - a) * (1f - a) : a * a * 1.4f;
        for (int side = 1; side >= -1; side -= 2) {
            pose.pushPose();
            m.body.translateAndRotate(pose);
            pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
            pose.translate(side * 1.8f, 3.0f, 2.6f + out * 10f);
            pose.mulPose(Axis.YP.rotationDegrees(-side * sweep));
            pose.mulPose(Axis.ZP.rotationDegrees(-side * lift));
            DjinnDraw.piece(pose, buffers, d, side > 0 ? Piece.WING_IN_L : Piece.WING_IN_R, light, alpha, glow);
            pose.translate(side * 11f, 0f, 0f);
            pose.mulPose(Axis.ZP.rotationDegrees(side * bend));
            DjinnDraw.piece(pose, buffers, d, side > 0 ? Piece.WING_OUT_L : Piece.WING_OUT_R, light, alpha, glow);
            pose.popPose();
        }
    }

    private void tail(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer p, PlayerModel<AbstractClientPlayer> m, Djinn d, float a, int light, float now, float glow) {
        if (d.look().tail() == Djinn.Look.Tail.NONE) return;
        float alpha = a > 0 ? a : 1f + a;
        float run = Mth.clamp(p.walkAnimation.speed(), 0f, 1f);
        pose.pushPose();
        m.body.translateAndRotate(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        pose.translate(0f, 10.5f, 2.2f);
        pose.mulPose(Axis.XP.rotationDegrees(-18f + (p.isCrouching() ? 25f : 0f)));
        int n = 5;
        for (int i = 0; i < n; i++) {
            float wave = Mth.sin(now * 0.14f - i * 0.8f) * (9f + 14f * run);
            pose.mulPose(Axis.YP.rotationDegrees(wave));
            pose.mulPose(Axis.XP.rotationDegrees(4f + Mth.cos(now * 0.1f - i * 0.6f) * 3f));
            float sc = 1f - i * 0.1f;
            pose.pushPose();
            pose.scale(sc, sc, sc);
            DjinnDraw.piece(pose, buffers, d, Piece.TAIL_SEG, light, alpha, glow);
            pose.popPose();
            pose.translate(0f, 0f, 6f * sc);
        }
        pose.scale(0.55f, 0.55f, 0.55f);
        DjinnDraw.piece(pose, buffers, d, Piece.TAIL_TIP, light, alpha, glow);
        pose.popPose();
    }

    private void halo(PoseStack pose, MultiBufferSource buffers, PlayerModel<AbstractClientPlayer> m, Djinn d, float a, int light, float now, float glow) {
        if (d.look().halo() == Djinn.Look.Halo.NONE) return;
        float alpha = a > 0 ? a : 1f + a;
        pose.pushPose();
        m.head.translateAndRotate(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        pose.translate(0f, -12.5f - 0.6f * Mth.sin(now * 0.1f) - (a > 0 ? (1f - a) * 16f : -a * 14f), 0f);
        pose.mulPose(Axis.YP.rotationDegrees(now * 2.2f));
        pose.mulPose(Axis.XP.rotationDegrees(180f));
        DjinnDraw.piece(pose, buffers, d, Piece.HALO, light, alpha, glow);
        pose.popPose();
    }

    // ================================================================ the weapon in hand
    /** The held weapon's elemental sheath: additive blades along the item's blade, drawn in the item's own space (the display transform of a hand-held tool). */
    private void sheath(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer p, PlayerModel<AbstractClientPlayer> m, Djinn d, Snap s, HumanoidArm main, float now) {
        float a = assembled(s, 2, 6, now);
        if (a <= 0f) return;
        boolean left = main == HumanoidArm.LEFT;
        pose.pushPose();
        m.translateToHand(main, pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.translate((left ? -1 : 1) / 16.0F, 0.125F, -0.625F);
        // the display transform of item/handheld (third person), then the model's centre: the blade lies along the sprite's diagonal
        pose.translate(0f, 4f / 16f, 0.5f / 16f);
        pose.mulPose(Axis.YP.rotationDegrees(-90f));
        pose.mulPose(Axis.ZP.rotationDegrees(55f));
        pose.scale(0.85f, 0.85f, 0.85f);
        pose.translate(-0.5f, -0.5f, -0.5f);
        pose.translate(0.12f, 0.12f, 0.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(-45f));
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        float flick = 0.7f + 0.3f * Mth.sin(now * 0.7f) + 0.15f * Mth.sin(now * 1.9f);
        DjinnDraw.glowOnly(pose, buffers, d, Piece.SHEATH, d.glow(), Math.min(1f, a) * flick);
        pose.popPose();
    }

    // ================================================================ the Lesser Summon
    private static final Vector3f T = new Vector3f();

    private void bust(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer p, Djinn d, Snap s, long gt, float now, float partial) {
        Minecraft mc = Minecraft.getInstance();
        float in = Mth.clamp((now - s.since()) / 8f, 0f, 1f);
        float out = s.phase() == 1 ? Mth.clamp(s.leftNow(gt) / 10f, 0f, 1f) : Mth.clamp(1f - (now - s.since()) / 8f, 0f, 1f);
        float alpha = 0.85f * in * out;
        if (alpha <= 0.02f) return;
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        pose.translate(-0.42f, -0.28f + 0.04f * Mth.sin(now * 0.12f), 0.0f);
        Matrix4f pm = pose.last().pose();
        pm.getTranslation(T);
        pm.identity().translate(T);                                 // keep the place, drop the body's turning: the bust always faces the camera
        pose.last().normal().identity();
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        float w = 0.3f * (0.8f + 0.2f * in), h = w;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(DjinnMeshes.bust(d)));
        PoseStack.Pose l = pose.last();
        int a = (int) (255 * alpha);
        vc.addVertex(l, -w, -h, 0f).setColor(255, 255, 255, a).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(DjinnDraw.FULL).setNormal(l, 0f, 0f, 1f);
        vc.addVertex(l, w, -h, 0f).setColor(255, 255, 255, a).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(DjinnDraw.FULL).setNormal(l, 0f, 0f, 1f);
        vc.addVertex(l, w, h, 0f).setColor(255, 255, 255, a).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(DjinnDraw.FULL).setNormal(l, 0f, 0f, 1f);
        vc.addVertex(l, -w, h, 0f).setColor(255, 255, 255, a).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(DjinnDraw.FULL).setNormal(l, 0f, 0f, 1f);
        pose.popPose();
    }
}
