package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.book.SpiritDiveMergeRules;
import com.newuniverse.nusmp.book.SpiritDivePieces;
import com.newuniverse.nusmp.book.SpiritDivePieces.Element;
import com.newuniverse.nusmp.perf.PerfProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4fStack;

/**
 * Spirit Dive MERGE 2, first person: the game side (the level render stage event) of {@link SpiritDiveMergeFirstPerson}, which holds the drawing. Only the local player, only while his own Spirit Dive runs and the
 * camera is first person, only during the 22 tick entry / exit (clock 0..18.6), only with the client options {@code spiritDive.merge} and {@code spiritDive.mergeFirstPerson}. Drawn in the level stage
 * {@code AFTER_PARTICLES} with the frame set up as the Paladin overlay does it. Cost: one ghost (at most 900 vertices, slim character tier) and 4 vertices of flash, once per frame; no allocation per frame beyond
 * the pose stack of one frame, no packet, no server state. Timed in {@code /multiverse perf} as {@code c.spiritdive.mergefp}.
 */
public final class DiveMergeFirstPerson {
    private DiveMergeFirstPerson() {}

    private static final PerfProbe.Section SECTION = PerfProbe.section("c.spiritdive.mergefp");

    /** Game bus, client: the level render stage. */
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || !SpiritDiveMerge2.firstPerson() || !SpiritDiveMerge.enabled()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || !mc.options.getCameraType().isFirstPerson()) return;
        var running = PlayerAuraClient.running(p.getId());
        long[] w = running == null ? null : running.get(Aura.SPIRIT_DIVE);
        long gt = mc.level.getGameTime();
        if (w == null || gt >= w[1] || !SpiritDivePieces.valid((int) w[2])) return;
        float partial = e.getPartialTick().getGameTimeDeltaPartialTick(false);
        float t = gt + partial - w[0], left = w[1] - gt - partial;
        float m = SpiritDiveMergeRules.clock(t, left);
        if (!SpiritDiveMergeRules.flowing(m) && SpiritDiveMergeRules.flash(m) <= 0.01f) return;
        int style = (int) w[2];
        Element el = SpiritDivePieces.element(style);
        boolean character = SpiritDiveMergeRules.character(el, SpiritDivePieces.grade(style)) && SpiritDiveLook.characterEffects();
        long t0 = PerfProbe.begin();
        try {
            float fov = mc.options.fov().get().floatValue(), aspect = mc.getWindow().getWidth() / (float) Math.max(1, mc.getWindow().getHeight());
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            Matrix4fStack modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix();
            modelView.set(e.getModelViewMatrix());
            RenderSystem.applyModelViewMatrix();
            PoseStack pose = new PoseStack();
            pose.mulPose(e.getCamera().rotation());                                                       // the pose is the view's space now: x right, y up, -z ahead
            SpiritDiveMergeFirstPerson.draw(pose, buffers, el, character, SpiritDiveMergeRules.side(p.getUUID().hashCode()), m, t, fov, aspect);
            buffers.endBatch();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        } finally {
            PerfProbe.end(SECTION, t0);
        }
    }

}
