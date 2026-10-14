package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.BossFx3.*;

/**
 * 1.35.0: the VFX of the Dungeon's Juggernaut (the immune system's champion). Ash and antibodies: grey ash that boils out of the floor
 * and an immune-system pulse of white-red rings that close on the target. Shared sprites only (dark_slash_ash, glow, rings).
 * <ul>
 *   <li>JUG_ASH_PULSE (its stomp, its birth crack, its enrage): an expanding ring of ash at 'from' with flakes thrown up and a low
 *       ash cloud; power = radius, duration = life.</li>
 *   <li>JUG_ANTIBODY_RING (its charge and rally wind-up): white-red rings close in on 'from' (the spot it has marked), with small
 *       antibody crosses pulsing round them. power = radius.</li>
 * </ul>
 */
public class JuggernautBossLayer extends AbstractVfxLayer {
    static final int ASH = 0xB8B2AC, ANTI = 0xFF4B4B, WHITE = 0xFFFFFF, SOOT = 0x2A2624;
    private static final ResourceLocation ASH_TEX = VfxTextures.byName("dark_slash_ash");

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.JUG_ASH_PULSE, VfxShape.JUG_ANTIBODY_RING); }

    @Override public int defaultDuration(VfxShape s) { return s == VfxShape.JUG_ASH_PULSE ? 26 : 22; }

    @Override public int defaultColor(VfxShape s) { return 0xFF000000 | (s == VfxShape.JUG_ASH_PULSE ? ASH : ANTI); }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (inst.shape == VfxShape.JUG_ASH_PULSE) ash(inst, ctx, buf); else antibody(inst, ctx, buf);
    }

    private void ash(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float prog = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), R = Math.max(1.5f, inst.power), fade = life(age, inst.duration, 1, 8);
        Vector3f c = ctx.rel(inst.from(ctx));
        float front = R * ease(Math.min(1f, prog * 1.6f));
        band(buf, ASH_TEX, VfxBlend.ALPHA, c, Math.max(0.1f, front - 0.9f), front, ctx.seg(20, 8), 3f, 0f, prog, 0.05f, rgbA(ASH, 0.9f * fade));
        band(buf, VfxTextures.GLOW, VfxBlend.ADD, c, Math.max(0.1f, front - 0.25f), front, ctx.seg(16, 8), 1f, 0f, 0f, 0.07f, rgbA(WHITE, 0.45f * (1f - prog) * fade));
        int n = ctx.seg(14, 6);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + h(inst.seed, i), d = front * (0.5f + 0.5f * h(inst.seed, i + 30));
            float up = (0.4f + 1.6f * h(inst.seed, i + 60)) * Mth.sin(Math.min(1f, prog * 1.4f) * Mth.PI);
            buf.billboard(ctx, ASH_TEX, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * d, 0.2f + up, Mth.sin(ang) * d), 0.5f + 0.4f * h(inst.seed, i + 90), age * 0.1f + i, rgbA(i % 3 == 0 ? SOOT : ASH, 0.75f * fade));
        }
        buf.billboard(ctx, KotodamaLayer.SMOKE, VfxBlend.ALPHA, new Vector3f(c).add(0f, 0.6f, 0f), R * 0.7f * (0.5f + prog), age * 0.02f, rgbA(SOOT, 0.4f * fade));
    }

    private void antibody(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float prog = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), R = Math.max(1.5f, inst.power), fade = life(age, inst.duration, 2, 5);
        Vector3f c = ctx.rel(inst.from(ctx));
        int rgb = inst.color & 0xFFFFFF;
        for (int k = 0; k < 3; k++) {                                          // three pulses close in one after the other
            float p = ((prog * 1.5f) - k * 0.25f);
            if (p <= 0f || p >= 1f) continue;
            float rr = R * (1f - ease(p)) + 0.3f;
            band(buf, KotodamaLayer.RING, VfxBlend.ADD, c, rr * 0.9f, rr, ctx.seg(18, 8), 3f, 0f, age * 0.05f, 0.05f + 0.01f * k, rgbA(k % 2 == 0 ? rgb : WHITE, 0.8f * Mth.sin(p * Mth.PI) * fade));
        }
        buf.plane(VfxTextures.DEVIL_CIRCLE, VfxBlend.ADD, VfxPose.ground(c).spin(-age * 0.04f).lift(0.03f), R * 0.5f, rgbA(rgb, 0.4f * prog * fade));
        int n = 6;
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + age * 0.08f, d = R * (1f - 0.7f * ease(prog));
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * d, 0.4f, Mth.sin(ang) * d), 0.45f, ang, rgbA(WHITE, 0.9f * fade));
        }
        float flash = win(prog, 0.85f, 0.95f) * (1f - win(prog, 0.95f, 1f));
        if (flash > 0f) buf.plane(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(c).lift(0.08f), R * 0.7f, rgbA(WHITE, 0.7f * flash));
    }
}
