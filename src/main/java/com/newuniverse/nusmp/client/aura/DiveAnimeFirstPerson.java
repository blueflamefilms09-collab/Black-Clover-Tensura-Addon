package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.book.SpiritDiveAnimeRules;
import com.newuniverse.nusmp.book.SpiritDiveAnimeRules.Pal;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier;
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
import org.joml.Quaternionf;

/**
 * Spirit Dive, ANIME look, FIRST person: the owner's own arms glow lightly (the hair, the halo, the eyes and the plumes are not drawn: the camera is inside the head). Only the local player, only
 * while his own Spirit Dive runs with the ANIME look, only the arms the dive dresses (the half dive of Wind: the left one), and never the right arm of Fuegoleon's dive or while his flame arm runs
 * ({@code Aura.FUEGOLEON_ARM}): that arm is {@link FireArmFirstPerson}'s. An empty hand is drawn by the game as an arm ({@link RenderArmEvent} fires inside that draw, the pose already at the arm); a hand that
 * holds an item shows only the item, so {@link RenderHandEvent} redoes the arm's placement (copied from {@code ItemInHandRenderer.renderPlayerArm}, as {@link DiveFirstPerson} does) and draws the arm there.
 * The glow is half the strength of the third person shell ({@link SpiritDiveAnimeRules#FIRST_PERSON_ALPHA}), two passes over one arm: at most 2 x 24 vertices. No allocation per frame. Not seen in a
 * running game by its author.
 */
public final class DiveAnimeFirstPerson {
    private DiveAnimeFirstPerson() {}

    public static final boolean ENABLED = true;

    private static final Quaternionf Q = new Quaternionf();                                       // scratch: the render thread is the only user
    private static final float DEG = (float) (Math.PI / 180.0);
    private static Element element = Element.WIND;
    private static float alpha;
    private static int tint, rim;

    /** Works out what the local player's {@code arm} wears right now (sets the statics above); false = draw nothing. */
    private static boolean state(AbstractClientPlayer p, HumanoidArm arm) {
        if (!SpiritDiveLook.anime() || p.isInvisible()) return false;
        var running = PlayerAuraClient.running(p.getId());
        long[] w = running == null ? null : running.get(Aura.SPIRIT_DIVE);
        long gt = p.level().getGameTime();
        if (w == null || gt >= w[1] || !SpiritDivePieces.valid((int) w[2])) return false;
        int style = (int) w[2];
        element = SpiritDivePieces.element(style);
        Stage st = SpiritDivePieces.stage(style);
        boolean right = arm == HumanoidArm.RIGHT;
        if (right && element == Element.FIRE && PlayerAuraClient.has(p.getId(), Aura.FUEGOLEON_ARM)) return false;          // the flaming arm is FireArmFirstPerson's
        int mask = SpiritDivePieces.shellParts(element, st);
        if ((mask & (right ? SpiritDivePieces.SHELL_ARM_R : SpiritDivePieces.SHELL_ARM_L)) == 0) return false;
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float t = gt + partial - w[0], left = w[1] - gt - partial;
        float prog = st == Stage.UPGRADE && !right ? SpiritDiveAnimeRules.progress(1e4f, left) : SpiritDiveAnimeRules.progress(t, left);
        alpha = SpiritDiveAnimeRules.FIRST_PERSON_ALPHA * SpiritDiveAnimeRules.partAlpha(prog, 1);
        if (alpha <= 0.01f) return false;
        Pal pal = SpiritDiveAnimeRules.pal(element, SpiritDivePieces.grade(style), false);
        tint = pal.tint();
        rim = pal.rim();
        return true;
    }

    /** Game bus, client: an empty hand's arm is about to be drawn. */
    public static void onRenderArm(RenderArmEvent e) {
        if (!ENABLED) return;
        AbstractClientPlayer p = e.getPlayer();
        if (p != Minecraft.getInstance().player || !state(p, e.getArm())) return;
        draw(e.getPoseStack(), e.getMultiBufferSource(), p, e.getArm());
    }

    /** Game bus, client: a hand is about to be drawn; if it holds an item, put the glowing arm where an empty hand's arm would be. */
    public static void onRenderHand(RenderHandEvent e) {
        if (!ENABLED || e.getItemStack().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer p = mc.player;
        if (p == null) return;
        HumanoidArm arm = e.getHand() == InteractionHand.MAIN_HAND ? p.getMainArm() : p.getMainArm().getOpposite();
        if (!state(p, arm)) return;
        PoseStack pose = e.getPoseStack();
        pose.pushPose();
        float side = arm == HumanoidArm.LEFT ? -1f : 1f;
        float swing = e.getSwingProgress(), equip = e.getEquipProgress();
        float f1 = Mth.sqrt(swing);
        float f2 = -0.3f * Mth.sin(f1 * (float) Math.PI), f3 = 0.4f * Mth.sin(f1 * ((float) Math.PI * 2f)), f4 = -0.4f * Mth.sin(swing * (float) Math.PI);
        pose.translate(side * (f2 + 0.64000005f), f3 + -0.6f + equip * -0.6f, f4 + -0.71999997f);
        pose.mulPose(Q.rotationY(side * 45f * DEG));
        float f5 = Mth.sin(swing * swing * (float) Math.PI), f6 = Mth.sin(f1 * (float) Math.PI);
        pose.mulPose(Q.rotationY(side * f6 * 70f * DEG));
        pose.mulPose(Q.rotationZ(side * f5 * -20f * DEG));
        pose.translate(side * -1f, 3.6f, 3.5f);
        pose.mulPose(Q.rotationZ(side * 120f * DEG));
        pose.mulPose(Q.rotationX(200f * DEG));
        pose.mulPose(Q.rotationY(side * -135f * DEG));
        pose.translate(side * 5.6f, 0f, 0f);
        draw(pose, e.getMultiBufferSource(), p, arm);
        pose.popPose();
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, AbstractClientPlayer p, HumanoidArm arm) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer r)) return;
        PlayerModel<AbstractClientPlayer> m = r.getModel();
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart part = right ? m.rightArm : m.leftArm;
        float ox = part.xRot, oy = part.yRot, oz = part.zRot;
        part.xRot = part.yRot = part.zRot = 0f;
        float age = (mc.level == null ? 0f : mc.level.getGameTime()) + mc.getTimer().getGameTimeDeltaPartialTick(false);
        SpiritDiveAnimeShell.arm(pose, buffers, part, right, WaterAuraRig.slim(m), element, tint, rim, alpha * (0.85f + 0.15f * Mth.sin(age * 0.12f)), age);
        part.xRot = ox;
        part.yRot = oy;
        part.zRot = oz;
    }
}
