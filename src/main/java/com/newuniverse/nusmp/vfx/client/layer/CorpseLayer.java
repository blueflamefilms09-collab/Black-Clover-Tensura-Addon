package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ZogratisFx.*;

/**
 * Why: Rades's Soul Corpse spells need an effect of their own, but it must still read as the same magic as the generic Soul Corpse
 * bolt, field and burst: the INFERNAL group of WikiStyles (a dark seal, a skull sigil, red embers) in the Soul Corpse
 * colour. So this layer uses the same two sprites (wiki_grp_infernal, wiki_soul_corpse) and the two palette constants of
 * WikiMagicLayer, and adds only what is specific to a
 * raising: a soul pulled up out of the ground, and a sigil of stitched loops round the caster. One effect spends well under 300
 * vertices.
 * <ul>
 *   <li>CORPSE_RAISE: at 'from' on the ground: the dark seal opens, cracks run out of it, pale soul ribbons are torn upward in a
 *       spiral, bone shards thrust through the seal and the skull sigil flares at the top; power = size (1 is a person).</li>
 *   <li>CORPSE_PACT: round the followed caster: a turning seal under the feet, three stitched loops (rings with a thread of sparks)
 *       turning about the body, souls drifting up; power = size. Seed bit 0 = the short flash form used when the pact pays out (the
 *       loops snap shut and flare instead of turning slowly).</li>
 * </ul>
 */
public class CorpseLayer extends AbstractVfxLayer {
    private static final int EMBER = 0xFFFF5232, SKULL = 0xFFE86A58, DARK = 0xFF14060A;
    private static final ResourceLocation SEAL = VfxTextures.byName("wiki_grp_infernal"), SKULL_TEX = VfxTextures.byName("wiki_soul_corpse");

    /** Lifts a dark colour toward white until additive blending shows it (the same idea as WikiStyles.lift; Soul Corpse is drawn dark). */
    private static int lift(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        float luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
        if (luma >= 0.42f) return rgb & 0xFFFFFF;
        float t = Math.min(0.6f, (0.42f - luma) * 2.4f);
        return (Math.round(r + (255 - r) * t) << 16) | (Math.round(g + (255 - g) * t) << 8) | Math.round(b + (255 - b) * t);
    }

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.CORPSE_RAISE, VfxShape.CORPSE_PACT); }

    @Override public int defaultDuration(VfxShape s) { return s == VfxShape.CORPSE_PACT ? 100 : 36; }

    @Override public int defaultColor(VfxShape s) { return 0xFF6C7899; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        try {
            if (inst.shape == VfxShape.CORPSE_RAISE) raise(inst, ctx, buf);
            else if (inst.shape == VfxShape.CORPSE_PACT) pact(inst, ctx, buf);
        } catch (RuntimeException e) {
            // an effect must never take the frame down; the next frame draws again
        }
    }

    private static int dark(int col, float alpha) { return a(VfxVertexBuffer.lerpColor(col | 0xFF000000, DARK, 0.75f), alpha); }

    private void raise(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), S = Math.max(0.6f, inst.power);
        int col = lift(inst.color) | 0xFF000000;
        ResourceLocation bulk = SEAL, skull = SKULL_TEX;
        VfxBlend bulkBlend = VfxBlend.ALPHA;
        Vector3f c = ctx.rel(inst.from(ctx));
        float fade = ElementFx.life(inst, age, 3, Math.min(12, inst.duration / 3f));
        float open = outCubic(ramp(t, 0f, 0.22f));
        float R = S * 1.5f * open;
        // the seal and the cracks it opens
        buf.plane(bulk, bulkBlend, VfxPose.ground(new Vector3f(c).add(0, 0.03f, 0)).spin(age * 0.05f), R, dark(col, 0.9f * fade));
        ground(buf, GROUND_CRACK, VfxBlend.ADD, c, 0.05f, R * 1.15f, 0.4f, a(EMBER, 0.55f * fade * (1f - ramp(t, 0.6f, 1f))));
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0)), R * 0.92f, R, ctx.seg(20, 10), 1f, 0f, a(EMBER, 0.6f * fade));
        // bone shards thrusting up through the seal
        int shards = ctx.seg(6, 3);
        float thrust = outCubic(ramp(t, 0.1f, 0.45f)) * (1f - ease(ramp(t, 0.75f, 1f)));
        for (int k = 0; k < shards; k++) {
            float ang = (k + hash(inst.seed, k, 1) * 0.5f) / shards * Mth.TWO_PI, rad = R * (0.35f + 0.5f * hash(inst.seed, k, 2));
            Vector3f base = new Vector3f(c).add(Mth.cos(ang) * rad, 0.02f, Mth.sin(ang) * rad);
            Vector3f tip = new Vector3f(base).add(Mth.cos(ang) * 0.12f * S, (0.6f + 0.7f * hash(inst.seed, k, 3)) * S * thrust, Mth.sin(ang) * 0.12f * S);
            spear(buf, VfxTextures.SHARD, VfxBlend.ADD, base, tip, 0.22f * S, a(0xFFE9E4D8, 0.85f * fade));
        }
        // the soul torn upward: pale ribbons in a spiral
        int ribbons = ctx.seg(8, 4);
        for (int k = 0; k < ribbons; k++) {
            float ph = ramp(t * 1.25f - hash(inst.seed, k, 4) * 0.25f, 0f, 1f);
            if (ph <= 0f) continue;
            float ang = hash(inst.seed, k, 5) * Mth.TWO_PI + ph * 4.5f, rad = S * 0.55f * (1f - ph * 0.6f);
            float h = ph * 2.6f * S;
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rad, 0.2f + h, Mth.sin(ang) * rad);
            Vector3f v = new Vector3f(-Mth.sin(ang) * 0.5f, 1f, Mth.cos(ang) * 0.5f);
            along(buf, ctx, WISP, VfxBlend.ADD, p, v, (1.1f + 0.7f * ph) * S, a(k % 3 == 0 ? WHITE : hot(col, 0.4f), 0.85f * fade * Mth.sin(Mth.PI * ph)));
        }
        // the skull sigil flaring above, a pale core and a few embers
        float flare = Mth.sin(Mth.PI * ramp(t, 0.25f, 0.85f));
        if (flare > 0f) {
            Vector3f top = new Vector3f(c).add(0, 1.7f * S, 0);
            buf.billboard(ctx, skull, VfxBlend.ADD, top, S * 1.3f * flare, 0f, a(SKULL, 0.7f * flare * fade));
            glow(buf, ctx, top, S * 2.2f * flare, a(hot(col, 0.5f), 0.35f * flare * fade));
        }
        int embers = ctx.seg(8, 3);
        for (int k = 0; k < embers; k++) {
            float ph = (age * 0.04f + hash(inst.seed, k, 6)) % 1f, ang = hash(inst.seed, k, 7) * Mth.TWO_PI;
            float rad = R * hash(inst.seed, k, 8);
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rad, ph * 2.2f * S, Mth.sin(ang) * rad);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, p, 0.2f * S, age * 0.15f + k, a(EMBER, 0.9f * fade * Mth.sin(Mth.PI * ph)));
        }
    }

    private void pact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = Mth.clamp(age / inst.duration, 0f, 1f), S = Math.max(0.6f, inst.power);
        boolean flash = telegraph(inst);
        int col = lift(inst.color) | 0xFF000000;
        ResourceLocation bulk = SEAL, skull = SKULL_TEX;
        VfxBlend bulkBlend = VfxBlend.ALPHA;
        Vector3f c = ctx.rel(inst.from(ctx));
        float fade = ElementFx.life(inst, age, 6, Math.min(16, inst.duration / 3f));
        float open = outCubic(ramp(age, 0f, flash ? 3f : 10f));
        float R = S * 1.4f * open;
        buf.plane(bulk, bulkBlend, VfxPose.ground(new Vector3f(c).add(0, 0.03f, 0)).spin(-age * 0.03f), R, dark(col, 0.85f * fade));
        ground(buf, RUNE_RING, VfxBlend.ADD, c, 0.05f, R * 0.95f, age * 0.04f, a(hot(col, 0.3f), 0.7f * fade));
        ground(buf, RUNE_RING, VfxBlend.ADD, c, 0.06f, R * 0.6f, -age * 0.07f, a(EMBER, 0.55f * fade));
        buf.plane(skull, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.07f, 0)).spin(age * 0.02f), R * 0.3f, a(SKULL, 0.5f * fade));
        // three stitched loops turning about the body: a ring each, and a thread of sparks running round it
        float snap = flash ? 1f - ease(ramp(t, 0f, 0.5f)) : 1f;
        for (int k = 0; k < 3; k++) {
            float tilt = 0.5f + k * 0.55f, spin = age * (flash ? 0.25f : 0.05f) * (k % 2 == 0 ? 1 : -1) + k * 2.1f;
            Vector3f normal = new Vector3f(Mth.sin(tilt) * Mth.cos(spin), Mth.cos(tilt), Mth.sin(tilt) * Mth.sin(spin));
            float rr = S * (1.1f + 0.15f * k) * (flash ? 0.4f + 0.6f * snap : 1f);
            VfxPose loop = VfxPose.facing(new Vector3f(c).add(0, 0.95f * S, 0), normal).spin(age * 0.08f);
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, loop, rr * 0.95f, rr, ctx.seg(14, 8), 1f, 0f, a(k == 0 ? hot(col, 0.5f) : EMBER, 0.6f * fade));
            int stitches = ctx.seg(8, 4);
            for (int s = 0; s < stitches; s++) {
                float an = s * Mth.TWO_PI / stitches + age * 0.1f;
                Vector3f p = loop.point(Mth.cos(an) * rr, Mth.sin(an) * rr);
                buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, p, 0.16f * S, an, a(WHITE, 0.8f * fade));
            }
        }
        // souls drifting up from the seal
        int souls = ctx.seg(6, 3);
        for (int k = 0; k < souls; k++) {
            float ph = (age * 0.02f + hash(inst.seed, k, 1)) % 1f, ang = hash(inst.seed, k, 2) * Mth.TWO_PI + age * 0.03f, rad = R * (0.4f + 0.5f * hash(inst.seed, k, 3));
            Vector3f p = new Vector3f(c).add(Mth.cos(ang) * rad, 0.2f + ph * 2.6f * S, Mth.sin(ang) * rad);
            along(buf, ctx, WISP, VfxBlend.ADD, p, new Vector3f(0, 1, 0), 1.0f * S, a(hot(col, 0.4f), 0.7f * fade * Mth.sin(Mth.PI * ph)));
        }
        if (flash) glow(buf, ctx, new Vector3f(c).add(0, 1f * S, 0), S * 3.2f * (1f - ramp(t, 0f, 0.6f)), a(hot(col, 0.7f), 0.7f * fade));
    }
}
