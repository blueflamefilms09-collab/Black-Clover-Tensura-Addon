package com.newuniverse.nusmp.client.mode;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.blackclover.ModeArmor;
import com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Look;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client events of the Valkyrie armour v2 (game bus, client only; registered by {@link ModeArmorClient#init}):
 * <ul>
 *   <li><b>First person</b> ({@link RenderArmEvent}, {@link RenderHandEvent}; the same two hooks and the same hand placement as {@code client.aura.FireArmFirstPerson} and the Djinn gauntlets, copied from
 *       {@code ItemInHandRenderer.renderPlayerArm}): the entity layer is not drawn for the local player in first person, so the sleeve, shoulder cap, cuff and sleeve veil of the arm being drawn are put on it here
 *       ({@link ModeArmorValkyrieV2#drawArm}). An empty hand's arm fires {@code RenderArmEvent} inside its draw (the pose already at the arm); a hand that holds an item shows only the item, so {@code RenderHandEvent}
 *       redoes the arm's placement. Only the local player, only while his own mode runs, only with {@code modeArmor.valkyrieLook} = V2. The ear discs of the preview are not built.</li>
 *   <li><b>The Mermaid look</b> ({@link RenderLivingEvent}): while a wearer's tail is drawn the vanilla legs and trouser layers are hidden from just before the body is drawn to just after it (their visibility is put back).</li>
 * </ul>
 * Render thread only; nothing is allocated per frame.
 */
final class ModeArmorValkyrieEvents {
    private ModeArmorValkyrieEvents() {}

    private static boolean done;

    static synchronized void init() {
        if (done) return;
        done = true;
        NeoForge.EVENT_BUS.addListener(ModeArmorValkyrieEvents::onRenderArm);
        NeoForge.EVENT_BUS.addListener(ModeArmorValkyrieEvents::onRenderHand);
        NeoForge.EVENT_BUS.addListener(ModeArmorValkyrieEvents::onLivingPre);
        NeoForge.EVENT_BUS.addListener(ModeArmorValkyrieEvents::onLivingPost);
    }

    // ================================================================ first person
    /** The fade of the local player's armour (grow in over 6 ticks, fade over the last 10), or 0 when he does not wear the V2 Valkyrie Dress. */
    private static float strength(AbstractClientPlayer p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || p.isInvisible() || ModeArmorValkyrieV2.look() != Look.V2) return 0f;
        long gt = mc.level.getGameTime();
        if (ModeArmorClient.mode(p.getId(), gt) != ModeArmor.Mode.VALKYRIE) return 0f;
        float now = gt + mc.getTimer().getGameTimeDeltaPartialTick(false);
        return Mth.clamp(ModeArmorClient.age(p.getId(), now) / 6f, 0.05f, 1f) * Mth.clamp(ModeArmorClient.left(p.getId(), now) / 10f, 0f, 1f);
    }

    /** Game bus, client: an empty hand's arm is about to be drawn. */
    static void onRenderArm(RenderArmEvent e) {
        AbstractClientPlayer p = e.getPlayer();
        float k = strength(p);
        if (k <= 0.02f) return;
        draw(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), p, e.getArm() == HumanoidArm.RIGHT, k);
    }

    /** Game bus, client: a hand is about to be drawn; if it holds an item, put the armoured arm where an empty hand's arm would be. */
    static void onRenderHand(RenderHandEvent e) {
        if (e.getItemStack().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer p = mc.player;
        if (p == null) return;
        float k = strength(p);
        if (k <= 0.02f) return;
        HumanoidArm arm = e.getHand() == InteractionHand.MAIN_HAND ? p.getMainArm() : p.getMainArm().getOpposite();
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
        draw(pose, e.getMultiBufferSource(), e.getPackedLight(), p, arm == HumanoidArm.RIGHT, k);
        pose.popPose();
    }

    /** The armour of one arm, in the arm's own frame (the pose is at the player model's arm pivot, as for the game's own hand draw). */
    private static void draw(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int light, AbstractClientPlayer p, boolean right, float k) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer r)) return;
        PlayerModel<AbstractClientPlayer> m = r.getModel();
        ModelPart part = right ? m.rightArm : m.leftArm;
        float ox = part.xRot, oy = part.yRot, oz = part.zRot;
        part.xRot = part.yRot = part.zRot = 0f;
        ModeArmorValkyrieV2.drawArm(pose, buffers, light, part, right, k);
        part.xRot = ox;
        part.yRot = oy;
        part.zRot = oz;
    }

    // ================================================================ the Mermaid look hides the vanilla legs while the tail is drawn
    private static PlayerModel<?> hidden;
    private static boolean wasLeftLeg, wasRightLeg, wasLeftPants, wasRightPants;

    /** Game bus, client: just before a living entity's model is drawn (inside its renderer, after the player model's own properties are set). */
    static void onLivingPre(RenderLivingEvent.Pre<?, ?> e) {
        if (ModeArmorValkyrieV2.look() != Look.V2 || ModeArmorValkyrieV2.form() != com.newuniverse.nusmp.book.ModeArmorValkyrieRules.Form.MERMAID) return;
        if (!(e.getEntity() instanceof AbstractClientPlayer p) || !(e.getRenderer().getModel() instanceof PlayerModel<?> pm)) return;
        if (!ModeArmorValkyrieV2.hidesLegs(p.getId())) return;
        hidden = pm;
        wasLeftLeg = pm.leftLeg.visible;
        wasRightLeg = pm.rightLeg.visible;
        wasLeftPants = pm.leftPants.visible;
        wasRightPants = pm.rightPants.visible;
        pm.leftLeg.visible = pm.rightLeg.visible = pm.leftPants.visible = pm.rightPants.visible = false;
    }

    /** Game bus, client: after the entity is drawn: the legs' visibility is put back. */
    static void onLivingPost(RenderLivingEvent.Post<?, ?> e) {
        PlayerModel<?> pm = hidden;
        if (pm == null) return;
        hidden = null;
        pm.leftLeg.visible = wasLeftLeg;
        pm.rightLeg.visible = wasRightLeg;
        pm.leftPants.visible = wasLeftPants;
        pm.rightPants.visible = wasRightPants;
    }
}
