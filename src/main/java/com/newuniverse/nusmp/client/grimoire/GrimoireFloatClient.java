package com.newuniverse.nusmp.client.grimoire;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.core.magic.grimoire.GrimoireShelfLayout;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Draws summoned grimoires (see book.GrimoireSummon) floating in front of their owner's right hand: in first person in front of
 * your view to the right, in third person (and for other players) beside the body's right hand, facing out. The book bobs like
 * the shelf books, sways a little, pops in on summon and shrinks away on dismiss. Drawn with the in-hand display context so the
 * grimoire model shows its "in use" glow.
 */
public final class GrimoireFloatClient {
    private static final float SCALE = 0.55f;
    private static final int APPEAR_TICKS = 8, VANISH_TICKS = 6;

    private static final class Entry {
        ItemStack stack;
        long start;
        long vanishAt = -1;
        Entry(ItemStack stack, long start) { this.stack = stack; this.start = start; }
    }

    private static final Map<Integer, Entry> FLOATING = new HashMap<>();

    private GrimoireFloatClient() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(GrimoireFloatClient::onRender);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> FLOATING.clear());
    }

    /** Network handler (client thread). */
    public static void receive(int entityId, ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        if (stack.isEmpty()) {
            Entry e = FLOATING.get(entityId);
            if (e != null && e.vanishAt < 0) e.vanishAt = now;
            return;
        }
        Entry e = FLOATING.get(entityId);
        if (e != null && e.vanishAt < 0) e.stack = stack;          // swapped books: keep floating, change the look
        else FLOATING.put(entityId, new Entry(stack, now));
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { FLOATING.clear(); return; }
        long now = mc.level.getGameTime();
        Iterator<Map.Entry<Integer, Entry>> it = FLOATING.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            Entity ent = mc.level.getEntity(en.getKey());
            Entry e = en.getValue();
            if (ent == null || !ent.isAlive() || (e.vanishAt >= 0 && now - e.vanishAt > VANISH_TICKS)) it.remove();
        }
    }

    private static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || FLOATING.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float now = mc.level.getGameTime() + partial;
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        boolean firstPerson = mc.options.getCameraType().isFirstPerson();

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        PoseStack pose = new PoseStack();

        for (var en : FLOATING.entrySet()) {
            if (!(mc.level.getEntity(en.getKey()) instanceof Player owner) || owner.isInvisible()) continue;
            Entry e = en.getValue();
            float age = now - e.start;
            float grow = e.vanishAt >= 0 ? 1f - Mth.clamp((now - e.vanishAt) / VANISH_TICKS, 0, 1)
                    : easeOutBack(Mth.clamp(age / APPEAR_TICKS, 0, 1));
            if (grow <= 0.01f) continue;
            float bob = GrimoireShelfLayout.bob(0, age, 1f);
            float sway = Mth.sin(age * 0.05f) * 4f;

            pose.pushPose();
            if (firstPerson && owner == mc.getCameraEntity()) {
                // in front of your view, lower right: where your right hand would hold it
                Vector3f look = camera.getLookVector(), up = camera.getUpVector(), left = camera.getLeftVector();
                Vec3 at = cam.add(look.x() * 0.85, look.y() * 0.85, look.z() * 0.85)
                        .add(-left.x() * 0.42, -left.y() * 0.42, -left.z() * 0.42)
                        .add(up.x() * (bob - 0.2), up.y() * (bob - 0.2), up.z() * (bob - 0.2));
                pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
                pose.mulPose(Axis.YP.rotationDegrees(180f - camera.getYRot() - 18f + sway));   // turned towards you, a little inwards
                pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot() * 0.6f - 12f));
            } else {
                // beside the body's right hand, cover facing out
                double yaw = Math.toRadians(Mth.rotLerp(partial, owner.yBodyRotO, owner.yBodyRot));
                Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
                Vec3 base = owner.getPosition(partial).add(0, owner.isCrouching() ? 0.85 : 1.05, 0);
                Vec3 at = base.add(fwd.scale(0.55)).add(right.scale(0.45)).add(0, bob, 0);
                pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
                pose.mulPose(Axis.YP.rotationDegrees(-(float) Math.toDegrees(yaw) + 15f + sway));
                pose.mulPose(Axis.XP.rotationDegrees(-10f));
            }
            float s = SCALE * grow;
            pose.scale(s, s, s);
            mc.getItemRenderer().renderStatic(e.stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, en.getKey());
            pose.popPose();
        }
        buffers.endBatch();
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static float easeOutBack(float t) { float c = 1.70158f, u = t - 1; return 1 + (c + 1) * u * u * u + c * u * u; }
}
