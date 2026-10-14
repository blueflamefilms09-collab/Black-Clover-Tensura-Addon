package com.newuniverse.nusmp.client.relics.worn;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;

/**
 * The Artifact Mimic at the waist: a GeoLite model ({@code geo/entity/relic_mimic_belt.geo.json}, its animations and textures, written by
 * tools/relics/waveboss_mimic_geo.py) of leather straps round the hips with a small mimic chest on the front, its lid for a jaw, teeth, a tongue and two
 * amber eyes. The model is authored in the player's own frame (the same frame as the Avatar and the other body-sized GeoLite models: file y 24 is
 * the neck line, the hips are at y 12, the front is -z), so it is drawn from {@code model.body}'s pose ({@code ModelPart#translateAndRotate}: the body's
 * pivot, its lean and the crouch offset) and follows the torso through a sneak, a swim and a bow draw.
 * <p>
 * <b>The clock.</b> The idle clip loops on the wearer's age (a slow tongue wobble, the lid breathing). The bite and the yawn are one-shot clips played
 * from a start stamp: the server sends a tiny cue packet ({@code WaveBossNet.Cue}) when Bite or Hoard fires, the client keeps (kind, game time)
 * per player in {@link WornCues}, and this class plays the clip for {@code now - start + partialTick} ticks, then drops back to idle. A packet instead
 * of replicated persistent data because it needs no registered data attachment and costs nothing while nothing happens; late joiners just see the idle
 * belt. The clip lengths are the constants in {@link WornMath} (the generator writes the same numbers into the animation file).
 * <p>
 * Seen from the player's own eyes nothing is drawn.
 */
public final class ArtifactMimicRenderer implements WornRenderer {
    static final GeoSpec SPEC = GeoSpec.of("relic", "mimic_belt");

    @Override
    public void render(WornCtx c) {
        if (c.firstPerson()) return;
        var player = c.player();
        long now = player.level().getGameTime();
        WornCues.Cue cue = WornCues.get(player.getId(), WaveBossKinds.WHICH_MIMIC, now);
        float t = cue == null ? -1f : WornMath.since(now, cue.start(), c.partialTick());
        String clip = WornMath.mimicClip(cue == null ? 0 : cue.kind(), t);
        float seconds = WornMath.mimicSeconds(clip, t, c.ageInTicks());
        boolean idle = WornMath.IDLE.equals(clip);
        float pulse = idle ? 0.65f + 0.25f * (float) Math.sin(c.ageInTicks() * 0.12) : 1.0f;      // the eyes and the tongue glow harder when it bites
        int glow = ((int) (pulse * 255f) << 24) | 0xFFFFFF;

        PoseStack pose = c.pose();
        pose.pushPose();
        c.model().body.translateAndRotate(pose);
        GeoDraw.paint(pose, c.buffers(), SPEC, clip, seconds, GeoDraw.Space.PLAYER, GeoDraw.Layer.CUTOUT, c.light(), 0xFFFFFFFF, glow);
        pose.popPose();
    }
}
