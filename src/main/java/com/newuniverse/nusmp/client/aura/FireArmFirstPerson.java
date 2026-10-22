package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.book.FuegoleonArmRules;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
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
 * Spirit Dive remake: Fuegoleon's flaming right arm in FIRST person. An empty hand is drawn by the game as an arm ({@link RenderArmEvent} fires inside that draw, with the pose already at the arm, before
 * the arm's own cubes); a hand that holds an item shows only the item, so {@link RenderHandEvent} puts the arm where an empty hand's arm would be and draws the flaming arm there (the same placement as
 * the Djinn gauntlet, client.relics.djinn.DjinnFirstPerson: copied from ItemInHandRenderer.renderPlayerArm). Only the RIGHT arm, only while the server says the arm is on ({@code Aura.FUEGOLEON_ARM}
 * running on the local player) and the viewer's setting allows it; nothing else about the hand changes (the item, the swing, the other arm). The molten model and its flames are the same as in third
 * person ({@link FuegoleonArmAura}, {@link FuegoleonArmFx}); the flames use the hand's own camera basis ({@link FuegoleonArmFx#cameraFirstPerson}). Not seen in a running game by its author (see the
 * doc); {@link #ENABLED} switches it off. No allocation per frame.
 */
public final class FireArmFirstPerson {
    private FireArmFirstPerson() {}

    public static final boolean ENABLED = true;

    private static final GeoSpec SPEC = GeoSpec.of("spiritdive", "arm_flame_arm_r");
    private static final Quaternionf Q = new Quaternionf();                                       // scratch: the render thread is the only user
    private static final float DEG = (float) (Math.PI / 180.0);

    /** The state of the local player's arm this frame: strength 0 = not drawn. */
    private static float strengthOf(AbstractClientPlayer p) {
        var running = PlayerAuraClient.running(p.getId());
        long[] w = running == null ? null : running.get(Aura.FUEGOLEON_ARM);
        if (w == null || p.level().getGameTime() >= w[1] || !FuegoleonArmRules.valid((int) w[2]) || p.isInvisible()) return 0f;
        FuegoleonArmRules.View view = SpiritDiveLook.armView();
        if (view == FuegoleonArmRules.View.OFF) return 0f;
        boolean casting = DiveCast.casting(p.getId()), diving = PlayerAuraClient.has(p.getId(), Aura.SPIRIT_DIVE);
        float grow = Mth.clamp((p.level().getGameTime() - w[0]) / 8f, 0f, 1f);
        return FuegoleonArmRules.strength(view, casting, diving, (int) w[2] == FuegoleonArmRules.STYLE_LINKED) * grow;
    }

    /** Game bus, client: an empty hand's arm is about to be drawn. */
    public static void onRenderArm(RenderArmEvent e) {
        if (!ENABLED || e.getArm() != HumanoidArm.RIGHT) return;
        AbstractClientPlayer p = e.getPlayer();
        float k = strengthOf(p);
        if (k <= 0.01f) return;
        draw(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), p, k);
    }

    /** Game bus, client: a hand is about to be drawn; if it holds an item, put the flaming arm where an empty hand's arm would be. */
    public static void onRenderHand(RenderHandEvent e) {
        if (!ENABLED || e.getItemStack().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer p = mc.player;
        if (p == null) return;
        HumanoidArm arm = e.getHand() == InteractionHand.MAIN_HAND ? p.getMainArm() : p.getMainArm().getOpposite();
        if (arm != HumanoidArm.RIGHT) return;
        float k = strengthOf(p);
        if (k <= 0.01f) return;
        PoseStack pose = e.getPoseStack();
        pose.pushPose();
        float side = 1f;
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
        draw(pose, e.getMultiBufferSource(), e.getPackedLight(), p, k);
        pose.popPose();
    }

    /** The flaming arm in the arm's own frame (the pose is at the player model's arm pivot, as for the game's own hand draw). */
    private static void draw(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, float k) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer r)) return;
        PlayerModel<AbstractClientPlayer> m = r.getModel();
        ModelPart part = m.rightArm;
        PartPose rest = part.getInitialPose();
        float ox = part.xRot, oy = part.yRot, oz = part.zRot;
        part.xRot = part.yRot = part.zRot = 0f;
        float age = (mc.level == null ? 0f : mc.level.getGameTime()) + mc.getTimer().getGameTimeDeltaPartialTick(false);
        boolean casting = DiveCast.casting(p.getId()), diving = PlayerAuraClient.has(p.getId(), Aura.SPIRIT_DIVE);
        float flare = FuegoleonArmRules.flare(casting, diving);
        pose.pushPose();                                                                          // the molten model (authored in whole-body coordinates: undo the arm's rest position)
        part.translateAndRotate(pose);
        pose.translate(-rest.x / 16f, -rest.y / 16f, -rest.z / 16f);
        int glow = SealDraw.argb(Mth.clamp(k * (1.0f + 0.5f * flare), 0f, 1f), 0xFF8A30);
        GeoDraw.paint(pose, buffers, SPEC, "idle", age / 20f, GeoDraw.Space.PLAYER, GeoDraw.Layer.CUTOUT, SealDraw.lift(light, 10), 0xFFFFFFFF, glow);
        pose.popPose();
        pose.pushPose();                                                                          // the flames, in model pixels
        part.translateAndRotate(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        FuegoleonArmFx.draw(pose, buffers, age, k, flare, true, true);
        pose.popPose();
        part.xRot = ox;
        part.yRot = oy;
        part.zRot = oz;
    }
}
