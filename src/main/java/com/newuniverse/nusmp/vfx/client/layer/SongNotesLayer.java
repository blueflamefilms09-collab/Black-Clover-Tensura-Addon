package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ZogratisFx.*;

/**
 * Why: a song has to be SEEN as well as heard, or a quiet pulse looks like nothing happened. This draws a handful of musical notes
 * (a glowing head, a stem and a flag) that leave the singer and drift up in a slow spiral, fading as they rise, in the colour of the
 * current song mode. It uses the shared glow, halo and spark textures only (no new texture file) and a few dozen vertices at most.
 * <p>
 * SONG_NOTES: 'from' = the followed body's position, 'to' = the point the notes rise from (followed too), power = size (1 = a normal
 * pulse, 1.6 = the Crescendo burst, which also sends more notes).
 */
public class SongNotesLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SONG_NOTES); }

    @Override public int defaultDuration(VfxShape s) { return 40; }

    @Override public int defaultColor(VfxShape s) { return 0xFFFFB4E8; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), S = Math.max(0.5f, inst.power);
        int col = inst.color | 0xFF000000;
        Vector3f o = ctx.rel(inst.to(ctx));
        int n = ctx.seg(Math.round(5 * S), 3);
        for (int k = 0; k < n; k++) {
            float start = hash(inst.seed, k, 1) * 0.35f;
            float ph = (age / inst.duration - start) / (1f - start);
            if (ph <= 0f || ph >= 1f) continue;
            float ang = hash(inst.seed, k, 2) * Mth.TWO_PI + ph * 2.4f;
            float r = 0.5f + 0.9f * ph + 0.3f * hash(inst.seed, k, 3);
            Vector3f head = new Vector3f(o).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * r))
                    .add(new Vector3f(ctx.camUp).mul(ph * 1.8f * S + Mth.sin(ang) * r * 0.25f));
            float fade = Mth.sin(Mth.PI * ph);
            float size = 0.16f * S;
            Vector3f top = new Vector3f(head).add(new Vector3f(ctx.camUp).mul(size * 3.2f)).add(new Vector3f(ctx.camRight).mul(size * 0.7f));
            glow(buf, ctx, head, size * 3.0f, a(col, 0.45f * fade));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, head, size * 1.6f, 0f, a(hot(col, 0.6f), 0.95f * fade));
            buf.beam(ctx, GLOW, VfxBlend.ADD, new Vector3f(head).add(new Vector3f(ctx.camRight).mul(size * 0.7f)), top,
                    size * 0.35f, size * 0.25f, 1, 0f, a(hot(col, 0.4f), 0.9f * fade), a(col, 0.5f * fade));
            if (k % 2 == 0) {   // an eighth-note flag
                Vector3f flag = new Vector3f(top).add(new Vector3f(ctx.camRight).mul(size * 1.2f)).sub(new Vector3f(ctx.camUp).mul(size * 1.1f));
                buf.beam(ctx, GLOW, VfxBlend.ADD, top, flag, size * 0.3f, size * 0.15f, 1, 0f, a(col, 0.8f * fade), a(col, 0.3f * fade));
            }
        }
    }
}
