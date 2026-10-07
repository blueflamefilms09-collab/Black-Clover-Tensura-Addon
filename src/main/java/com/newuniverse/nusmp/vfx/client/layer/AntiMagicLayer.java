package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * ANTI_MAGIC_SLASH (0.49 rework of the 0.20 crescent; every demon sword uses it). A sweep of anti-magic travelling 'from' ->
 * 'to': a wide black crescent with a solid black core and a bright rim in the sword's colour, two fading afterimages
 * behind it, torn black flakes and coloured sparks flung off its edge, a thin shock streak along its path, and a crack of light
 * where it ends.
 */
public class AntiMagicLayer extends AbstractVfxLayer {
    static final ResourceLocation SLASH = VfxTextures.ANTI_MAGIC_SLASH, SMOKE = VfxTextures.byName("koto_smoke"), STREAK = VfxTextures.byName("light_streak"),
            CRACK = VfxTextures.byName("koto_crack");

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ANTI_MAGIC_SLASH); }
    @Override public int defaultDuration(VfxShape s) { return 18; }
    @Override public int defaultColor(VfxShape s) { return 0xFF2A0A30; }

    @Override
    public void onSpawn(VfxInstance inst) { if (inst.power >= 1.5f) VfxShake.add(inst.payload.from(), 1.0f, 8); }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), P = inst.power;
        Vec3 a = inst.from(ctx), b = inst.to(ctx), d = b.subtract(a);
        if (d.lengthSqr() < 0.01) return;
        Vector3f dir = new Vector3f((float) d.x, (float) d.y, (float) d.z).normalize();
        Vector3f n = Math.abs(dir.y) > 0.95f ? new Vector3f(1, 0, 0) : new Vector3f(dir).cross(0, 1, 0).normalize();
        int rim = VfxVertexBuffer.whiten(brighten(inst.color), 0.2f), hot = VfxVertexBuffer.whiten(brighten(inst.color), 0.7f);
        float alpha = VfxAnim.fadeInOut(t, 0.05f, 0.35f), r = 1.6f * P;
        int seg = ctx.seg(10, 6), segTrail = ctx.seg(6, 4);
        // afterimages first (oldest, faintest), then the slash itself
        for (int k = 2; k >= 0; k--) {
            float tk = Math.max(0, t - k * 0.06f);
            Vector3f mid = ctx.rel(a.lerp(b, VfxAnim.slashHead(tk)));
            VfxPose blade = VfxPose.facing(mid, n);
            float ang = (float) Math.atan2(dir.dot(blade.up()), dir.dot(blade.right()));
            float ak = alpha * (k == 0 ? 1f : 0.35f / k);
            buf.arc(SLASH, VfxBlend.ALPHA, blade, r * 1.02f, 2.0f * P, ang - 1.25f, 2.5f, k == 0 ? seg : segTrail, VfxVertexBuffer.withAlpha(0xFF050207, 0.85f * ak));
            if (k == 0) buf.arc(SLASH, VfxBlend.ALPHA, blade, r, 1.4f * P, ang - 1.2f, 2.4f, seg, VfxVertexBuffer.withAlpha(0xFF000000, ak));
            buf.arc(SLASH, VfxBlend.ADD, blade, r * 1.08f, 0.5f * P, ang - 1.2f, 2.4f, k == 0 ? seg : segTrail, VfxVertexBuffer.withAlpha(rim, ak));
            if (k == 0) buf.arc(SLASH, VfxBlend.ADD, blade, r * 1.1f, 0.16f * P, ang - 1.1f, 2.2f, seg, VfxVertexBuffer.withAlpha(hot, ak));
        }
        Vector3f head = ctx.rel(a.lerp(b, VfxAnim.slashHead(t)));
        // the shock streak along its path
        Vector3f start = ctx.rel(a);
        streak(buf, ctx, STREAK, VfxBlend.ADD, start, head, 0.12f * P, VfxVertexBuffer.withAlpha(rim, 0.6f * alpha));
        // torn black flakes and sparks flung off the edge
        int flakes = ctx.seg(10, 5);
        for (int k = 0; k < flakes; k++) {
            float born = hash(inst.seed, k, 1) * 0.5f;
            if (t < born) continue;
            float u = Math.min(1, (t - born) / 0.5f);
            Vector3f off = new Vector3f(hash(inst.seed, k, 2) - 0.5f, (hash(inst.seed, k, 3) - 0.5f) * 0.6f, hash(inst.seed, k, 4) - 0.5f).normalize().mul(r * (0.6f + 1.4f * VfxAnim.easeOutCubic(u)));
            Vector3f p = ctx.rel(a.lerp(b, VfxAnim.slashHead(born))).add(off).add(0, -0.6f * u * u, 0);
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, p, (0.5f + 0.4f * u) * P, age * 0.2f + k, VfxVertexBuffer.withAlpha(0xFF050207, 0.75f * (1 - u)));
            if (k % 2 == 0) buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, p, 0.22f * P, k, VfxVertexBuffer.withAlpha(hot, 1 - u));
        }
        // where it ends: a crack of light
        if (t > 0.55f) {
            float c = (t - 0.55f) / 0.45f;
            Vector3f end = ctx.rel(b);
            buf.billboard(ctx, CRACK, VfxBlend.ADD, end, (1.0f + 0.8f * c) * P, hash(inst.seed, 9, 9) * Mth.TWO_PI, VfxVertexBuffer.withAlpha(rim, 1 - c));
        }
        VfxBloom.glow(ctx, buf, head, 1.0f * P, rim, alpha * 0.6f);
    }

    /** The sword's colour pushed up to a visible rim brightness (demon swords pass a near-black violet). */
    static int brighten(int argb) {
        int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255, m = Math.max(1, Math.max(r, Math.max(g, b)));
        float k = Math.max(1f, 230f / m);
        return 0xFF000000 | Math.min(255, (int) (r * k)) << 16 | Math.min(255, (int) (g * k)) << 8 | Math.min(255, (int) (b * k));
    }
}
