package com.newuniverse.nusmp.client.zogratis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Lucius's open grimoire (assets zogratis_grimoire_prop.*), floating in front of his chest with its pages turned toward whoever he faces.
 * Lucius and his clones both carry it, so a clone looks like the real thing. It plays the clip of the body it floats before: it stirs
 * while he casts, blazes with an ultimate, and goes out with the transformation and the death.
 */
final class LuciusBook {
    private LuciusBook() {}

    private static final GeoSpec BOOK = ZogratisRenderer.spec("grimoire_prop");
    private static final ZogratisRenderer.GeoClips BOOK_CLIPS = new ZogratisRenderer.GeoClips(BOOK);

    /**
     * Draws the book in the pose of the body (feet at the origin, front +z). 'phase' is the 0..1 of the transformation, 'fade' 1 down to 0 for
     * a dying body, 'glow' the glow layer's ARGB.
     */
    static void draw(PoseStack pose, MultiBufferSource buffers, ZogratisPose clock, ZogratisRenderer.GeoClips bodyClips, float phase, int light,
                     float fade, int glow, boolean divine) {
        String name = ZogratisPose.IDLE;
        float secs = Float.isNaN(clock.lastAge()) ? 0f : clock.lastAge() / 20f;
        switch (clock.clip) {
            case ZogratisPose.CAST -> { name = ZogratisPose.CAST; secs = clock.seconds; }
            case ZogratisPose.CAST_BIG -> { name = ZogratisPose.CAST_BIG; secs = fraction(clock, bodyClips) * BOOK_CLIPS.length(name); }
            case ZogratisPose.TRANSFORM -> { name = ZogratisPose.TRANSFORM; secs = phase * BOOK_CLIPS.length(name); }
            case ZogratisPose.DEATH -> { name = ZogratisPose.DEATH; secs = fraction(clock, bodyClips) * BOOK_CLIPS.length(name); }
            default -> { }
        }
        float s = divine ? 0.65f : 0.45f;           // a book 1.4 blocks wide at scale 1: checked beside the models with the preview harness
        pose.pushPose();
        pose.translate(0f, divine ? 0.95f : 0.5f, divine ? 0.9f : 0.85f);   // from the feet, in front (the model's front is +z in the world)
        pose.scale(s, s, s);
        ZogratisRenderer.paint(pose, buffers, BOOK, name, secs, light, 0xFFFFFF, fade, glow);
        pose.popPose();
    }

    /** How far along its clip the body is, 0..1: held at the end for a one-shot clip, round and round for a looping one (Lucius's cast_big). */
    private static float fraction(ZogratisPose clock, ZogratisRenderer.GeoClips clips) {
        float len = clips.length(clock.clip);
        if (len <= 0f) return 0f;
        float t = clock.seconds / len;
        return clips.loops(clock.clip) ? t - (float) Math.floor(t) : Math.max(0f, Math.min(1f, t));
    }
}
