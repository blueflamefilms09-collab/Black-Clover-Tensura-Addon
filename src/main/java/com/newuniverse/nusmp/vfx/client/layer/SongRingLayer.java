package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ZogratisFx.*;

/**
 * Why: each pulse of a song reaches a known radius, and the players need to see that edge to stand inside it. This draws a sound ring:
 * two thin rings on the ground that run out from the singer to the pulse radius (the second a beat behind), a soft flash under the
 * singer, and sparks standing up on the leading edge. Shared textures only; the number of vertices is bounded by the segment count.
 * <p>
 * SONG_RING: 'from' = the centre on the ground, power = the final radius in blocks.
 */
public class SongRingLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SONG_RING); }

    @Override public int defaultDuration(VfxShape s) { return 24; }

    @Override public int defaultColor(VfxShape s) { return 0xFFFFB4E8; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1f, inst.power);
        float t = Mth.clamp(age / inst.duration, 0f, 1f);
        int col = inst.color | 0xFF000000;
        Vector3f c = ctx.rel(inst.from(ctx)).add(0, 0.06f, 0);
        int segs = ctx.seg(40, 16);
        for (int ring = 0; ring < 2; ring++) {
            float tt = ramp(t, ring * 0.18f, 1f);
            if (tt <= 0f) continue;
            float r = R * outCubic(tt);
            float fade = (1f - tt) * (ring == 0 ? 1f : 0.6f);
            float w = 0.18f + 0.1f * (1f - tt);
            buf.ring(RUNE_RING, VfxBlend.ADD, VfxPose.ground(c), Math.max(0.01f, r - w), r + w, segs, 6f, age * 0.03f * (ring == 0 ? 1 : -1), a(ring == 0 ? hot(col, 0.4f) : col, fade));
        }
        float flash = 1f - ramp(t, 0f, 0.35f);
        if (flash > 0f) ground(buf, HALO, VfxBlend.ADD, c, 0.02f, 1.6f + 0.8f * R * 0.1f, age * 0.05f, a(col, 0.55f * flash));
        float edge = R * outCubic(ramp(t, 0f, 1f));
        int n = ctx.seg(10, 4);
        for (int k = 0; k < n; k++) {
            float ang = hash(inst.seed, k, 1) * Mth.TWO_PI;
            float rise = (1f - t) * hash(inst.seed, k, 2) * 0.9f + 0.1f;
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * edge, rise * (0.4f + t), Mth.sin(ang) * edge);
            buf.billboard(ctx, SPARK, VfxBlend.ADD, p, 0.22f, age * 0.2f, a(k % 3 == 0 ? WHITE : col, 0.85f * (1f - t)));
        }
    }
}
