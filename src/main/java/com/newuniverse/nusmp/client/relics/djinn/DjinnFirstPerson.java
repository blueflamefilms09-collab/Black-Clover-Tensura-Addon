package com.newuniverse.nusmp.client.relics.djinn;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.relics.djinn.DjinnClientState.Snap;
import com.newuniverse.nusmp.client.relics.djinn.DjinnMeshes.Piece;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * The owner's own arms in first person. An empty hand is drawn by the game as an arm ({@link RenderArmEvent} fires inside that draw, with the pose already
 * at the arm); a hand that holds an item shows only the item, so {@link RenderHandEvent} redoes the arm's placement and draws the gauntlet and the vambrace
 * there (the Half Equip dresses the main-hand arm, the Maso both). The placement is copied from ItemInHandRenderer.renderPlayerArm. It is the one part of
 * the armour that was not tested in the game (see the report); set {@link #ENABLED} to false to switch it off.
 */
public final class DjinnFirstPerson {
    private DjinnFirstPerson() {}

    public static final boolean ENABLED = true;

    private static boolean dressed(Snap s, HumanoidArm arm, HumanoidArm main) {
        return s.djinn() != null && (s.phase() >= 3 || (s.phase() >= 2 && arm == main));
    }

    /** Game bus, client: an empty hand's arm is about to be drawn. */
    public static void onRenderArm(RenderArmEvent e) {
        if (!ENABLED) return;
        AbstractClientPlayer p = e.getPlayer();
        Snap s = DjinnClientState.of(p.getId());
        if (!dressed(s, e.getArm(), p.getMainArm())) return;
        draw(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), p, s, e.getArm());
    }

    /** Game bus, client: a hand is about to be drawn; if it holds an item, put the armoured arm where an empty hand's arm would be. */
    public static void onRenderHand(RenderHandEvent e) {
        if (!ENABLED || e.getItemStack().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer p = mc.player;
        if (p == null || p.isInvisible()) return;
        HumanoidArm arm = e.getHand() == InteractionHand.MAIN_HAND ? p.getMainArm() : p.getMainArm().getOpposite();
        Snap s = DjinnClientState.of(p.getId());
        if (!dressed(s, arm, p.getMainArm())) return;
        PoseStack pose = e.getPoseStack();
        pose.pushPose();
        float side = arm == HumanoidArm.LEFT ? -1f : 1f;
        float swing = e.getSwingProgress(), equip = e.getEquipProgress();
        float f1 = Mth.sqrt(swing);
        float f2 = -0.3f * Mth.sin(f1 * (float) Math.PI), f3 = 0.4f * Mth.sin(f1 * ((float) Math.PI * 2f)), f4 = -0.4f * Mth.sin(swing * (float) Math.PI);
        pose.translate(side * (f2 + 0.64000005f), f3 + -0.6f + equip * -0.6f, f4 + -0.71999997f);
        pose.mulPose(Axis.YP.rotationDegrees(side * 45f));
        float f5 = Mth.sin(swing * swing * (float) Math.PI), f6 = Mth.sin(f1 * (float) Math.PI);
        pose.mulPose(Axis.YP.rotationDegrees(side * f6 * 70f));
        pose.mulPose(Axis.ZP.rotationDegrees(side * f5 * -20f));
        pose.translate(side * -1f, 3.6f, 3.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(side * 120f));
        pose.mulPose(Axis.XP.rotationDegrees(200f));
        pose.mulPose(Axis.YP.rotationDegrees(side * -135f));
        pose.translate(side * 5.6f, 0f, 0f);
        draw(pose, e.getMultiBufferSource(), e.getPackedLight(), p, s, arm);
        pose.popPose();
    }

    /** The armour of one arm, in the arm's own frame (the pose is at the player model's arm pivot, as for the game's own hand draw). */
    private static void draw(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, Snap s, HumanoidArm arm) {
        Minecraft mc = Minecraft.getInstance();
        Djinn d = s.djinn();
        if (!(mc.getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer r)) return;
        PlayerModel<AbstractClientPlayer> m = r.getModel();
        boolean left = arm == HumanoidArm.LEFT;
        ModelPart part = left ? m.leftArm : m.rightArm;
        float ox = part.xRot, oy = part.yRot, oz = part.zRot;
        part.xRot = part.yRot = part.zRot = 0f;
        pose.pushPose();
        part.translateAndRotate(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        float glow = 0.9f + 0.1f * Mth.sin((mc.level == null ? 0 : mc.level.getGameTime()) * 0.12f);
        boolean dark = s.dark();                                  // the Dark Djinn's black and gold arm; the unique look's own pieces; the shared rig as the fallback
        DjinnDraw.piece(pose, buffers, d, left ? Piece.VAMBRACE_L : Piece.VAMBRACE_R, dark, light, 1f, glow);
        DjinnDraw.piece(pose, buffers, d, left ? Piece.GAUNTLET_L : Piece.GAUNTLET_R, dark, light, 1f, glow);
        pose.popPose();
        part.xRot = ox;
        part.yRot = oy;
        part.zRot = oz;
    }
}
