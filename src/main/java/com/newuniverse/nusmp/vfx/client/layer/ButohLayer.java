package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Butoh Magic (the dance of the Sea God). Look: the owner's still of the hooded dancer in the pale feather-scale cloak with the two
 * long jade-teal blades (a chain-link ornament down the middle, flat grey-blue tips) and ribbons of iridescent oil-slick water
 * (teal, violet, rose sheen) cutting across a dark cave, with small leaf-shaped ink flecks flying off the cuts. The caster colour
 * ({@code inst.color}, default rose 0xFF5A8A) only tints the halos and the flash; the bodies stay teal / jade / pale so it always reads as Butoh.
 *
 * <ul>
 * <li><b>BUTOH_FX1 (cast / projectile, Sea God Slash)</b>: from = hand, to = target, power = blade size (1 = 2.2 block blades), duration = flight
 * ticks (16). First quarter: a feather-star flash and a small turning wave disc gather at the hand. Then two crossed jade blades fly
 * from to to, trailing ghost copies of themselves and a tapering iridescent water ribbon, shedding ink flecks; the last quarter is a
 * small splash flash with a wave disc at the target.</li>
 * <li><b>BUTOH_FX2 (zone, the Dancer's Tide)</b>: from = centre on the ground, power = radius, duration = life (80). A turning seigaiha
 * wave sigil on the ground, a counter-turning chain-link ring on its rim, a standing wall of feather scales (fading upward), three
 * iridescent ribbons spiralling up, ink flecks drifting up and a wave pulse running out from the centre every 28 ticks.</li>
 * <li><b>BUTOH_FX3 (impact / signature)</b>: from = centre, to = optional direction hint (turns the slashes), power = scale, duration = life (28).
 * A feather-star flash, two crossed crescent slashes sweeping open on the screen plane, a splash of drops, an expanding wave disc,
 * jade blade shards and flecks flying out under gravity, and iridescent ribbons curling out in the afterglow.</li>
 * </ul>
 */
public class ButohLayer extends AbstractVfxLayer {
    private static final ResourceLocation BLADE = VfxTextures.byName("butoh_blade");
    private static final ResourceLocation SHEEN = VfxTextures.byName("butoh_sheen");
    private static final ResourceLocation RIPPLE = VfxTextures.byName("butoh_ripple");
    private static final ResourceLocation SCALES = VfxTextures.byName("butoh_scales");
    private static final ResourceLocation LINKS = VfxTextures.byName("butoh_links");
    private static final ResourceLocation FLECK = VfxTextures.byName("butoh_fleck");
    private static final ResourceLocation SPLASH = VfxTextures.byName("butoh_splash");
    private static final ResourceLocation SLASH = VfxTextures.byName("butoh_slash");
    private static final ResourceLocation FLASH = VfxTextures.byName("butoh_flash");

    private static final int TEAL = 0xFF48D8C8, JADE = 0xFF8CF0C8, PALE = 0xFFE6F8F4, VIOLET = 0xFFB088F4, WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BUTOH_FX1, VfxShape.BUTOH_FX2, VfxShape.BUTOH_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BUTOH_FX1 -> 16;
            case BUTOH_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFFF5A8A; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BUTOH_FX1 -> cast(inst, ctx, buf);
            case BUTOH_FX2 -> zone(inst, ctx, buf);
            case BUTOH_FX3 -> impact(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static int rgb(int argb) { return 0xFF000000 | (argb & 0xFFFFFF); }
    private static int a(int argb, float k) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(k, 0f, 1f)); }
    private static float saturate(float x) { return Mth.clamp(x, 0f, 1f); }

    /** Jade body colour, leaning a little toward the caster tint. */
    private static int body(VfxInstance inst, float k) { return VfxVertexBuffer.lerpColor(TEAL, rgb(inst.color), k); }

    /** A sprite lying on the screen plane, long axis along {@code dir}. Texture tip (image top) points along dir. */
    static void butohOriented(VfxVertexBuffer b, ResourceLocation tex, VfxBlend bl, Vector3f c, Vector3f dir, Vector3f toCam,
                              float len, float halfW, int col) {
        Vector3f d = new Vector3f(dir);
        if (d.lengthSquared() < 1e-8f) d.set(0, 1, 0);
        d.normalize();
        Vector3f s = new Vector3f(d).cross(toCam);
        if (s.lengthSquared() < 1e-8f) s.set(1, 0, 0);
        s.normalize().mul(halfW);
        d.mul(len * 0.5f);
        int k = bl.grade(col, 1f);
        b.quad(tex, bl, new Vector3f(c).sub(d).sub(s), new Vector3f(c).sub(d).add(s), new Vector3f(c).add(d).add(s), new Vector3f(c).add(d).sub(s),
                0, 0, 1, 1, k, k);
    }

    /** Standing band on a circle: bottom colour {@code cb}, top colour {@code ct}. */
    static void butohWall(VfxVertexBuffer b, ResourceLocation tex, VfxBlend bl, VfxPose pose, float radius, float height, int segs,
                          float repeats, float scroll, int cb, int ct) {
        Vector3f up = new Vector3f(pose.normal()).mul(height);
        int k0 = bl.grade(cb, 0.6f), k1 = bl.grade(ct, 0.6f);
        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            Vector3f m0 = pose.point(Mth.cos(a0) * radius, Mth.sin(a0) * radius), m1 = pose.point(Mth.cos(a1) * radius, Mth.sin(a1) * radius);
            b.quad(tex, bl, m0, m1, new Vector3f(m1).add(up), new Vector3f(m0).add(up),
                    scroll + repeats * i / segs, 0, scroll + repeats * (i + 1) / segs, 1, k0, k1);
        }
    }

    private static Vector3f toCam(Vector3f c) {
        Vector3f t = new Vector3f(c).negate();
        if (t.lengthSquared() < 1e-6f) return new Vector3f(0, 0, 1);
        return t.normalize();
    }

    // ------------------------------------------------------------------ FX1: Sea God Slash
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration);
        float t = saturate(age / dur), p = Math.max(0.3f, inst.power);
        Vector3f a = ctx.rel(inst.from(ctx)), z = ctx.rel(inst.to(ctx));
        Vector3f path = new Vector3f(z).sub(a);
        if (path.lengthSquared() < 1e-4f) path.set(ctx.camRight);
        Vector3f dir = new Vector3f(path).normalize();
        RandomSource r = inst.random();
        int jade = body(inst, 0.16f), halo = rgb(inst.color);

        // 1) gathering at the hand: wave disc turning in, feather star, rose halo
        float tc = saturate(age / (dur * 0.25f));
        float gather = (1f - saturate((t - 0.2f) / 0.15f));
        if (gather > 0.01f) {
            float k = VfxAnim.easeOutCubic(tc);
            buf.billboard(ctx, RIPPLE, VfxBlend.ADD, a, p * (1.9f - 0.9f * k), age * 0.25f, a(VfxVertexBuffer.lerpColor(jade, PALE, 0.4f), 0.55f * gather * (0.4f + 0.6f * k)));
            buf.billboard(ctx, FLASH, VfxBlend.ADD, a, p * (0.7f + 1.6f * k), age * 0.1f, a(PALE, gather * k));
            VfxBloom.glow(ctx, buf, a, p * 1.3f * (0.5f + 0.5f * k), halo, 0.6f * gather);
        }

        // 2) the blades in flight
        float tf = saturate((t - 0.18f) / 0.67f);
        float fly = (float) Math.pow(tf, 1.5);
        if (t > 0.18f && t < 0.97f) {
            float vis = saturate((t - 0.18f) / 0.06f) * (1f - saturate((t - 0.86f) / 0.11f));
            Vector3f tcam = toCam(new Vector3f(a).add(z).mul(0.5f));
            float len = 2.2f * p, hw = 0.3f * p;
            float spread = 0.3f * (1f - 0.55f * tf);

            // iridescent water ribbon dragged behind the blades
            Vector3f head = new Vector3f(path).mul(fly).add(a);
            float trail = Math.min(path.length() * fly, 4.2f * p);
            if (trail > 0.2f) {
                Vector3f tail = new Vector3f(dir).mul(-trail).add(head);
                buf.beam(ctx, SHEEN, VfxBlend.ALPHA, head, tail, 0.95f * p, 0.18f * p, 5, age * 0.04f, a(WHITE, 0.9f * vis), a(WHITE, 0f));
                Vector3f tail2 = new Vector3f(dir).mul(-trail * 0.7f).add(head);
                buf.beam(ctx, SHEEN, VfxBlend.ADD, head, tail2, 0.5f * p, 0.08f * p, 4, -age * 0.07f, a(VIOLET, 0.6f * vis), a(TEAL, 0f));
            }

            // ghosts of the blades (afterimage), oldest first
            for (int g = 3; g >= 1; g--) {
                float tg = saturate((t - g * 0.045f - 0.18f) / 0.67f);
                Vector3f pg = new Vector3f(path).mul((float) Math.pow(tg, 1.5f)).add(a);
                float al = vis * (0.34f - g * 0.08f);
                butohOriented(buf, BLADE, VfxBlend.ADD, pg, rot(dir, tcam, 0f), tcam, len * (1f - g * 0.08f), hw * 1.2f, a(jade, al));
            }

            // the crossed pair
            Vector3f d1 = rot(dir, tcam, spread), d2 = rot(dir, tcam, -spread);
            Vector3f side = new Vector3f(dir).cross(tcam).normalize();
            Vector3f c1 = new Vector3f(head).add(new Vector3f(side).mul(0.1f * p)), c2 = new Vector3f(head).sub(new Vector3f(side).mul(0.1f * p));
            butohOriented(buf, BLADE, VfxBlend.ALPHA, c1, d1, tcam, len, hw, a(VfxVertexBuffer.lerpColor(jade, PALE, 0.35f), vis));
            butohOriented(buf, BLADE, VfxBlend.ALPHA, c2, d2, tcam, len * 0.92f, hw * 0.92f, a(VfxVertexBuffer.lerpColor(jade, PALE, 0.15f), vis));
            // glow on the blades' edge and a rose halo around the head
            VfxBloom.glow(ctx, buf, head, 0.9f * p, halo, 0.45f * vis);

            // flecks shed along the path
            int n = ctx.seg(6, 3);
            for (int i = 0; i < n; i++) {
                float born = 0.2f + 0.55f * (i + r.nextFloat() * 0.6f) / n;
                float life = (t - born) / 0.22f;
                float sx = (r.nextFloat() - 0.5f), sy = (r.nextFloat() - 0.5f);
                if (life <= 0f || life >= 1f) continue;
                Vector3f origin = new Vector3f(path).mul((float) Math.pow(saturate((born - 0.18f) / 0.67f), 1.5f)).add(a);
                Vector3f drift = new Vector3f(side).mul(sx * 1.6f * p * life).add(new Vector3f(ctx.camUp).mul(sy * 1.2f * p * life - 0.6f * life * life));
                Vector3f pos = origin.add(drift);
                butohOriented(buf, FLECK, VfxBlend.ALPHA, pos, new Vector3f(drift).add(dir), tcam, 0.3f * p, 0.07f * p, a(JADE, (1f - life) * vis));
            }
        }

        // 3) landing: splash flash and wave disc at the target
        if (t > 0.82f) {
            float k = saturate((t - 0.82f) / 0.18f), e = VfxAnim.easeOutCubic(k);
            buf.billboard(ctx, FLASH, VfxBlend.ADD, z, p * (0.8f + 1.8f * e), 0.5f, a(PALE, 1f - k));
            buf.billboard(ctx, SPLASH, VfxBlend.ADD, z, p * (0.7f + 2.1f * e), r.nextFloat() * 6f, a(VfxVertexBuffer.lerpColor(jade, PALE, 0.5f), 0.9f * (1f - k)));
            buf.billboard(ctx, RIPPLE, VfxBlend.ADD, z, p * (0.6f + 1.8f * e), -age * 0.2f, a(jade, 0.5f * (1f - k)));
            VfxBloom.glow(ctx, buf, z, p * 1.6f, halo, 0.5f * (1f - k));
        }
    }

    /** dir turned about the camera axis by an angle (screen-plane rotation). */
    private static Vector3f rot(Vector3f dir, Vector3f axis, float ang) {
        return new Vector3f(dir).rotateAxis(ang, axis.x, axis.y, axis.z);
    }

    // ------------------------------------------------------------------ FX2: the Dancer's Tide
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration);
        float fade = Math.min(saturate(age / 8f), saturate((dur - age) / 12f));
        if (fade <= 0.01f) return;
        float R = Math.max(0.8f, inst.power);
        float open = VfxAnim.easeOutBack(saturate(age / 10f));
        Vector3f c = ctx.rel(inst.from(ctx));
        VfxPose ground = VfxPose.ground(c);
        RandomSource r = inst.random();
        int jade = body(inst, 0.14f), halo = rgb(inst.color);

        // ground: seigaiha wave sigil (solid, then a glow copy), counter-turning chain ring on the rim
        VfxPose sig = ground.lift(0.03f).spin(age * 0.012f);
        buf.plane(RIPPLE, VfxBlend.ALPHA, sig, R * open, a(VfxVertexBuffer.lerpColor(jade, PALE, 0.25f), 0.7f * fade));
        buf.plane(RIPPLE, VfxBlend.ADD, ground.lift(0.04f).spin(age * 0.012f + 0.1f), R * open * 1.01f, a(jade, 0.45f * fade * (0.7f + 0.3f * VfxAnim.pulse(age, 1.5f))));
        float bw = Math.max(0.2f, R * 0.07f);
        int segs = ctx.seg(20, 10);
        buf.ring(LINKS, VfxBlend.ALPHA, ground.lift(0.05f).spin(-age * 0.02f), R * open - bw, R * open, segs,
                Math.max(2, Math.round(Mth.TWO_PI * R / (4f * bw))), age * 0.01f, a(VfxVertexBuffer.lerpColor(PALE, jade, 0.25f), 0.95f * fade));

        // wave pulses running out
        for (int i = 0; i < 2; i++) {
            float k = ((age + i * 14f) % 28f) / 28f;
            float al = (1f - k) * (1f - k) * 0.5f * fade;
            buf.plane(RIPPLE, VfxBlend.ADD, ground.lift(0.06f).spin(i * 1.3f - age * 0.03f), R * (0.15f + 0.85f * VfxAnim.easeOutCubic(k)), a(VfxVertexBuffer.lerpColor(jade, halo, 0.3f), al));
        }

        // standing wall of feather scales, fading upward
        float wallH = (0.55f + 0.18f * Mth.sqrt(R)) * VfxAnim.easeOutCubic(saturate(age / 12f));
        butohWall(buf, SCALES, VfxBlend.ALPHA, ground.lift(0.02f).spin(age * 0.006f), R * 0.985f * open, wallH, segs,
                Math.max(2, Math.round(Mth.TWO_PI * R / (4f * wallH))), age * 0.004f, a(PALE, 0.85f * fade), a(jade, 0f));

        // iridescent ribbons spiralling up, three of them
        float H = 1.6f + 0.35f * R;
        for (int i = 0; i < 3; i++) {
            float th0 = Mth.TWO_PI * i / 3f + age * 0.035f;
            float rho = R * (0.55f + 0.2f * Mth.sin(age * 0.05f + i * 2f));
            for (int s = 0; s < 4; s++) {
                float s0 = s / 4f, s1 = (s + 1) / 4f;
                Vector3f p0 = helix(c, rho * (1f - 0.35f * s0), th0 + s0 * 2.3f, s0 * H);
                Vector3f p1 = helix(c, rho * (1f - 0.35f * s1), th0 + s1 * 2.3f, s1 * H);
                float w = (0.5f + 0.25f * R * 0.2f) * (1f - 0.55f * s0);
                float al = fade * (1f - s0) * 0.9f;
                buf.beam(ctx, SHEEN, VfxBlend.ALPHA, p0, p1, w, w * 0.8f, 1, age * 0.02f + i * 0.3f, a(WHITE, al), a(WHITE, al * 0.7f));
            }
        }

        // ink flecks drifting up
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = Mth.sqrt(r.nextFloat()) * R * 0.9f, ph = r.nextFloat(), sp = 0.012f + 0.01f * r.nextFloat();
            float k = (ph + age * sp) % 1f;
            Vector3f pos = new Vector3f(c).add(Mth.cos(ang + age * 0.01f) * rad, k * (H + 0.6f), Mth.sin(ang + age * 0.01f) * rad);
            buf.billboard(ctx, FLECK, VfxBlend.ALPHA, pos, 0.22f + 0.12f * r.nextFloat(), Mth.sin(age * 0.07f + i) * 0.5f, a(JADE, fade * Mth.sin(k * Mth.PI)));
        }

        // heart of the circle
        buf.billboard(ctx, FLASH, VfxBlend.ADD, new Vector3f(c).add(0, 0.25f, 0), 0.8f + 0.2f * R, age * 0.03f, a(PALE, 0.7f * fade));
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.3f, 0), 0.7f + 0.3f * R, halo, 0.35f * fade);
    }

    private static Vector3f helix(Vector3f c, float rho, float th, float y) {
        return new Vector3f(c).add(Mth.cos(th) * rho, y + 0.1f, Mth.sin(th) * rho);
    }

    // ------------------------------------------------------------------ FX3: Sea God's Descent (impact)
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration);
        float t = saturate(age / dur), p = Math.max(0.3f, inst.power);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        RandomSource r = inst.random();
        Vector3f tcam = toCam(c);
        int jade = body(inst, 0.16f), halo = rgb(inst.color);
        float e = VfxAnim.easeOutCubic(saturate(t * 3.2f)), eSlow = VfxAnim.easeOutCubic(t);

        // slash tilt follows the direction hint projected on the screen
        float tilt = 0.6f;
        if (hint.lengthSquared() > 0.04f) tilt = (float) Math.atan2(hint.dot(ctx.camUp), hint.dot(ctx.camRight));

        // wave disc, expanding on the ground and facing the camera
        float disc = 1f - t;
        buf.plane(RIPPLE, VfxBlend.ADD, VfxPose.ground(c).spin(age * 0.05f), p * (0.5f + 2.6f * eSlow), a(jade, 0.55f * disc * disc));
        buf.billboard(ctx, RIPPLE, VfxBlend.ADD, c, p * (0.6f + 3.2f * e), -age * 0.08f, a(VfxVertexBuffer.lerpColor(jade, PALE, 0.3f), 0.5f * disc * disc));

        // splash of drops, solid underlay and a glow copy
        buf.billboard(ctx, SPLASH, VfxBlend.ALPHA, c, p * (0.7f + 2.4f * e), 0.2f, a(jade, 0.5f * disc));
        buf.billboard(ctx, SPLASH, VfxBlend.ADD, c, p * (0.8f + 2.8f * e), 0.2f, a(PALE, 0.35f * saturate(1f - t * 2f)));

        // two crossed crescent slashes opening on the screen plane
        VfxPose face = VfxPose.facing(c, tcam);
        float open = VfxAnim.easeOutCubic(saturate(t / 0.35f)), life = 1f - saturate((t - 0.3f) / 0.7f);
        int segs = ctx.seg(8, 5);
        float rad = p * (0.8f + 1.1f * e), wd = p * 0.85f * (1f - 0.5f * t);
        buf.arc(SLASH, VfxBlend.ADD, face.spin(tilt), rad, wd, -1.2f + 0.5f * t, 2.5f * open, segs, a(VfxVertexBuffer.lerpColor(PALE, jade, 0.25f), life));
        buf.arc(SLASH, VfxBlend.ADD, face.spin(tilt + Mth.PI * 0.62f), rad * 1.05f, wd * 0.9f, -1.2f - 0.4f * t, 2.5f * open, segs, a(VfxVertexBuffer.lerpColor(VIOLET, jade, 0.5f), 0.85f * life));

        // jade blade shards and ink flecks flying out under gravity
        Vector3f bias = hint.lengthSquared() > 0.04f ? new Vector3f(hint).normalize() : new Vector3f();
        int nShard = ctx.seg(5, 3), nFleck = ctx.seg(10, 5);
        for (int i = 0; i < nShard + nFleck; i++) {
            boolean shard = i < nShard;
            Vector3f d = new Vector3f(r.nextFloat() * 2 - 1, r.nextFloat() * 1.2f - 0.2f, r.nextFloat() * 2 - 1);
            if (d.lengthSquared() < 1e-4f) d.set(0, 1, 0);
            d.normalize().add(new Vector3f(bias).mul(0.5f)).normalize();
            float sp = (shard ? 2.2f : 3.0f) * (0.6f + 0.6f * r.nextFloat()) * p;
            float k = saturate(t * (shard ? 1.25f : 1.0f));
            float out = VfxAnim.easeOutCubic(saturate(t * 1.6f)) * 0.9f + 0.1f * t;
            Vector3f pos = new Vector3f(d).mul(sp * out).add(c);
            pos.y -= 1.6f * t * t * p * (shard ? 0.6f : 1f);
            Vector3f vel = new Vector3f(d).mul(sp);
            float al = 1f - saturate((k - 0.55f) / 0.45f);
            if (shard) {
                butohOriented(buf, BLADE, VfxBlend.ALPHA, pos, vel, tcam, 0.75f * p, 0.1f * p, a(VfxVertexBuffer.lerpColor(jade, PALE, 0.3f), al));
            } else {
                butohOriented(buf, FLECK, VfxBlend.ALPHA, pos, vel, tcam, 0.34f * p, 0.08f * p, a(JADE, al));
            }
        }

        // flash on top, then lingering afterglow with curling iridescent ribbons
        float fl = 1f - saturate(t * 4f);
        buf.billboard(ctx, FLASH, VfxBlend.ADD, c, p * (1.2f + 3.4f * VfxAnim.easeOutCubic(saturate(t * 5f))), 0.4f + t, a(WHITE, fl));
        VfxBloom.glow(ctx, buf, c, p * (1.0f + 1.6f * e), halo, 0.7f * (1f - t) * (1f - t));
        for (int i = 0; i < 3; i++) {
            float ang = tilt + i * 2.1f + 0.5f * t;
            Vector3f out = new Vector3f(ctx.camRight).mul(Mth.cos(ang)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang)));
            Vector3f end = new Vector3f(out).mul(p * (0.8f + 2.2f * eSlow)).add(c);
            end.y += 0.5f * t * p;
            buf.beam(ctx, SHEEN, VfxBlend.ALPHA, c, end, 0.8f * p, 0.3f * p, 3, age * 0.05f + i, a(WHITE, 0.85f * (1f - t) * saturate(t * 6f + 0.3f)), a(WHITE, 0f));
        }
    }
}
