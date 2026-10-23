package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.book.SpiritDiveDjinnPieces;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.book.SpiritDivePieces.Stage;
import com.newuniverse.nusmp.book.SpiritDiveTier;
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
 * Spirit Dive, Djinn-equip style: the owner's own arms in FIRST person, as the Djinn Full Equip does them ({@code client.relics.djinn.DjinnFirstPerson}): an empty hand is drawn by the game as an arm
 * ({@link RenderArmEvent} fires inside that draw, with the pose already at the arm); a hand that holds an item shows only the item, so {@link RenderHandEvent} redoes the arm's placement and draws the
 * gauntlet and the vambrace there (copied from ItemInHandRenderer.renderPlayerArm, the placement the Djinn's gauntlet and Fuegoleon's flaming arm use). Only while this client's own Spirit Dive runs
 * ({@code Aura.SPIRIT_DIVE} on the local player), only with the DJINN look, only for the arms the dive dresses (the half dive of Wind the left one; the pieces are the same models as in third person:
 * {@code sdj_<element>_<s|c>_arm_gauntlet_<r|l>} and {@code vambrace_<r|l>}). No right-arm plate is drawn for Fuegoleon's dive or whenever his flaming arm ({@code Aura.FUEGOLEON_ARM}) is running on
 * the local player: that arm is {@link FireArmFirstPerson}'s. A piece still forging in (the first ticks) is drawn translucent at the Djinn's own fade. Not seen in a running game by its author;
 * {@link #ENABLED} switches it off. No allocation per frame (statics only).
 */
public final class DiveFirstPerson {
    private DiveFirstPerson() {}

    public static final boolean ENABLED = true;

    private static final Quaternionf Q = new Quaternionf();                                       // scratch: the render thread is the only user
    private static final float DEG = (float) (Math.PI / 180.0);

    /** What this frame draws for one arm: bit 0 gauntlet, bit 1 vambrace; 0 = nothing. Written by {@link #state}. */
    private static int pieces;
    private static Element element = Element.WIND;
    private static boolean character;
    private static float gAlpha, vAlpha;
    private static int glowRgb;

    /** Works out what the local player's {@code arm} wears right now (sets the statics above); false = draw nothing. */
    private static boolean state(AbstractClientPlayer p, HumanoidArm arm) {
        if (SpiritDiveLook.look() != SpiritDiveDjinnPieces.Look.DJINN || p.isInvisible()) return false;
        var running = PlayerAuraClient.running(p.getId());
        long[] w = running == null ? null : running.get(Aura.SPIRIT_DIVE);
        long gt = p.level().getGameTime();
        if (w == null || gt >= w[1] || !SpiritDivePieces.valid((int) w[2])) return false;
        int style = (int) w[2];
        element = SpiritDivePieces.element(style);
        Stage st = SpiritDivePieces.stage(style);
        var grade = SpiritDivePieces.grade(style);
        boolean right = arm == HumanoidArm.RIGHT;
        if (right && element == Element.FIRE && PlayerAuraClient.has(p.getId(), Aura.FUEGOLEON_ARM)) return false;      // the flaming arm is drawn by FireArmFirstPerson
        character = SpiritDiveDjinnPieces.character(element, grade) && SpiritDiveLook.characterEffects();
        String g = right ? "gauntlet_r" : "gauntlet_l", v = right ? "vambrace_r" : "vambrace_l";
        var names = SpiritDiveDjinnPieces.names(element, st, grade, character);
        boolean hasG = names.contains(g), hasV = names.contains(v);
        if (!hasG && !hasV) return false;
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float t = gt + partial - w[0], left = w[1] - gt - partial;
        gAlpha = hasG ? alpha(st, grade, character, g, t, left) : 0f;
        vAlpha = hasV ? alpha(st, grade, character, v, t, left) : 0f;
        pieces = (gAlpha > 0.01f ? 1 : 0) | (vAlpha > 0.01f ? 2 : 0);
        glowRgb = SpiritDiveDjinnPieces.glowRgb(element, SpiritDiveTier.effective(element, grade));
        return pieces != 0;
    }

    private static float alpha(Stage st, SpiritDiveTier.Grade grade, boolean ch, String name, float t, float left) {
        var def = SpiritDiveDjinnPieces.def(name, ch);
        if (def == null) return 0f;
        boolean already = SpiritDiveDjinnPieces.alreadyOn(element, st, grade, ch, name);
        return EquipAnimation.alpha(EquipAnimation.assembled(already ? 1e4f : t, left, def.delay()));
    }

    /** Game bus, client: an empty hand's arm is about to be drawn. */
    public static void onRenderArm(RenderArmEvent e) {
        if (!ENABLED) return;
        AbstractClientPlayer p = e.getPlayer();
        if (p != Minecraft.getInstance().player || !state(p, e.getArm())) return;
        draw(e.getPoseStack(), e.getMultiBufferSource(), e.getPackedLight(), p, e.getArm());
    }

    /** Game bus, client: a hand is about to be drawn; if it holds an item, put the armoured arm where an empty hand's arm would be. */
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
        draw(pose, e.getMultiBufferSource(), e.getPackedLight(), p, arm);
        pose.popPose();
    }

    /** The armour of one arm, in the arm's own frame (the pose is at the player model's arm pivot, as for the game's own hand draw). */
    private static void draw(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer p, HumanoidArm arm) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer r)) return;
        PlayerModel<AbstractClientPlayer> m = r.getModel();
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart part = right ? m.rightArm : m.leftArm;
        PartPose rest = part.getInitialPose();
        float ox = part.xRot, oy = part.yRot, oz = part.zRot;
        part.xRot = part.yRot = part.zRot = 0f;
        float age = (mc.level == null ? 0f : mc.level.getGameTime()) + mc.getTimer().getGameTimeDeltaPartialTick(false);
        float pulse = 0.85f + 0.15f * Mth.sin(age * 0.12f);
        pose.pushPose();
        part.translateAndRotate(pose);
        pose.translate(-rest.x / 16f, -rest.y / 16f, -rest.z / 16f);                              // the models are authored in whole-body coordinates: undo the arm's rest position
        if ((pieces & 2) != 0) one(pose, buffers, light, right ? "vambrace_r" : "vambrace_l", vAlpha, pulse);
        if ((pieces & 1) != 0) one(pose, buffers, light, right ? "gauntlet_r" : "gauntlet_l", gAlpha, pulse);
        pose.popPose();
        part.xRot = ox;
        part.yRot = oy;
        part.zRot = oz;
    }

    private static void one(PoseStack pose, MultiBufferSource buffers, int light, String name, float alpha, float pulse) {
        var def = SpiritDiveDjinnPieces.def(name, character);
        GeoSpec spec = SpiritDiveDjinn.specOf(element, character, def);
        boolean solid = alpha >= 0.98f;
        int white = ((int) (Mth.clamp(alpha, 0f, 1f) * 255f + 0.5f) << 24) | 0xFFFFFF;
        int glow = ((int) (Mth.clamp(alpha * pulse * def.glow(), 0f, 1f) * 255f + 0.5f) << 24) | (glowRgb & 0xFFFFFF);
        GeoDraw.paint(pose, buffers, spec, null, 0f, GeoDraw.Space.PLAYER, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, SealDraw.lift(light, 4), white, glow);
    }
}
