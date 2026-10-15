package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ZogratisFx.*;

/**
 * Why: in act 3 the real Lucius shows a gem on his chest while his core is exposed, and that gem has to tell the players two things at
 * a glance: <i>this</i> Lucius is the real one (the copies never have it) and <i>now</i> is the moment to hit it. It is not a model: the
 * gem part ({@code LuciusCoreGem}) has no renderer, so the boss sends this effect, following his body, for exactly as long as the window
 * is open, and it is drawn here as a pulsing four-pointed star with a hot core, a turning ring and motes rising off it, in his gold
 * (or the violet of his enraged light: the colour is the payload's). It reuses the Zogratis layers' textures and helpers, so no new
 * texture file is needed, and it spends a few dozen vertices at most.
 * <p>
 * LUCIUS_CORE_GEM: 'from' = the followed body's position, 'to' = the point on his chest (followed too), power = size (0.5 is the size of
 * the hit box). The gem sits 0.42 blocks in front of that point, in the direction the body faces.
 */
public class LuciusCoreGemLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.LUCIUS_CORE_GEM); }

    @Override public int defaultDuration(VfxShape s) { return 160; }

    @Override public int defaultColor(VfxShape s) { return 0xFFFFD24A; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), S = Math.max(0.3f, inst.power);
        int col = inst.color | 0xFF000000;
        float fade = ElementFx.life(inst, age, 6, 10);
        float pulse = 0.78f + 0.22f * Mth.sin(age * 0.45f);
        float yaw = inst.followYaw(ctx);
        if (Float.isNaN(yaw)) yaw = 0f;
        float rad = (float) Math.toRadians(yaw);
        Vector3f g = new Vector3f(ctx.rel(inst.to(ctx))).add(-Mth.sin(rad) * 0.42f, 0f, Mth.cos(rad) * 0.42f);
        glow(buf, ctx, g, S * 3.2f * pulse, a(hot(col, 0.5f), 0.5f * fade));
        buf.billboard(ctx, HALO, VfxBlend.ADD, g, S * 2.6f * (1.15f - 0.15f * pulse), -age * 0.05f, a(col, 0.6f * fade));
        buf.billboard(ctx, SPARK, VfxBlend.ADD, g, S * 2.0f * pulse, age * 0.02f, a(WHITE, 0.95f * fade));
        buf.billboard(ctx, SPARK, VfxBlend.ADD, g, S * 1.5f * pulse, age * 0.02f + Mth.PI / 4f, a(col, 0.9f * fade));
        glow(buf, ctx, g, S * 1.1f, a(WHITE, fade));
        int n = ctx.seg(6, 3);
        for (int k = 0; k < n; k++) {
            float ph = (age * 0.04f + hash(inst.seed, k, 1)) % 1f, ang = hash(inst.seed, k, 2) * Mth.TWO_PI + age * 0.05f;
            float r = S * 0.7f * (1f - ph);
            Vector3f p = new Vector3f(g).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * r)).add(new Vector3f(ctx.camUp).mul(ph * 1.2f + Mth.sin(ang) * r * 0.4f));
            buf.billboard(ctx, SPARK, VfxBlend.ADD, p, 0.22f, age * 0.2f, a(k % 2 == 0 ? col : WHITE, 0.85f * fade * (1f - ph)));
        }
    }
}
