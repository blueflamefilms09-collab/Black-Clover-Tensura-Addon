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
 * Demon Beast Magic, drawn after the owner's still: a ragged, grainy, stippled crimson beast-shaped aura with two horn spikes,
 * and white double rings of glowing rune glyphs held in front of the outstretched hand. Textures: {@code demon_beast_*}
 * (aura, aura_b, puff, rune_ring, rune_band, claws, fang, eye, paw, streak, ember), written by {@code tools/gen_demon_beast_textures.py}.
 * <ul>
 *   <li>DEMON_BEAST_FX1 (cast / projectile, Chimera's Roar / spectral beast lunge): the double rune ring opens at {@code from} facing
 *       {@code to} and two embers-streams gather into it (first quarter); then a horned beast spirit (dark stippled aura with a slit
 *       eye and a white-hot core) tears along {@code from -> to} behind a ragged lick of flame, smoke puffs and two claw slashes
 *       left in its wake; a small ring gate flashes where it passes the half way mark and a claw-cross flash closes the flight at
 *       {@code to}. power scales the beast and the ring, colour tints the aura / glow.</li>
 *   <li>DEMON_BEAST_FX2 (zone / field, Beast Territory): a double rune circle turns on the ground out to radius = power, its two rings
 *       counter-rotating; horned beast silhouettes stand in a ring on the rim with slit eyes blinking, black fangs rise from the
 *       ground, paw prints are pressed into the earth, a larger beast looms over the centre and a pulse wave runs out to the rim
 *       every second; embers drift up. Fades in over 8 ticks, out over the last 12.</li>
 *   <li>DEMON_BEAST_FX3 (impact / roar / signature): a flash, the beast aura blooming out of {@code from} (two layers turning against
 *       each other), a rune ring opening facing the camera, a ground shockwave, three claw slashes at random angles, fangs and embers
 *       flying outward (biased along {@code to - from}), a pair of eyes that flare in the cloud and a crimson afterglow.</li>
 * </ul>
 */
public class DemonBeastLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName("demon_beast_" + n); }

    public static final ResourceLocation AURA = t("aura"), AURA_B = t("aura_b"), PUFF = t("puff"), RUNE_RING = t("rune_ring"),
            RUNE_BAND = t("rune_band"), CLAWS = t("claws"), FANG = t("fang"), EYE = t("eye"), PAW = t("paw"), STREAK = t("streak"), EMBER = t("ember");

    private static final int DARK = 0xFF8A1020, RED = 0xFFE0283C, HOT = 0xFFFFB090, WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.DEMON_BEAST_FX1, VfxShape.DEMON_BEAST_FX2, VfxShape.DEMON_BEAST_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) { case DEMON_BEAST_FX1 -> 16; case DEMON_BEAST_FX2 -> 80; default -> 28; };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFB02A6A; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case DEMON_BEAST_FX1 -> cast(inst, ctx, buf);
            case DEMON_BEAST_FX2 -> zone(inst, ctx, buf);
            case DEMON_BEAST_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ palette helpers
    private static int a(int c, float k) { return VfxVertexBuffer.withAlpha(c, Mth.clamp(k, 0f, 1f)); }
    private static int aura(VfxInstance i) { return VfxVertexBuffer.lerpColor(DARK, i.color, 0.15f); }
    private static int glow(VfxInstance i) { return VfxVertexBuffer.lerpColor(RED, i.color, 0.45f); }
    private static int rune(VfxInstance i) { return VfxVertexBuffer.whiten(VfxVertexBuffer.lerpColor(WHITE, i.color, 0.25f), 0.55f); }
    private static float life(VfxInstance inst, float age, float in, float out) {
        float x = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1), y = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(x, y);
    }
    private static float lerpf(float a, float b, float t) { return a + (b - a) * t; }

    /** A billboard stood on a point with its base there (fangs, horns); rotation is the screen angle of its tip. */
    private static void pointing(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f at, Vector3f dir, float size, int col) {
        float dx = dir.dot(ctx.camRight), dy = dir.dot(ctx.camUp);
        if (dx * dx + dy * dy < 1e-6f) { dy = 1; dx = 0; }
        buf.billboard(ctx, tex, blend, at, size, (float) Math.atan2(-dx, dy), col);
    }

    private static void ember(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f p, float size, float k, int col) {
        if (k > 0.02f && buf.hasBudget(4)) buf.billboard(ctx, EMBER, VfxBlend.ADD, p, size, 0, a(col, k));
    }

    // ------------------------------------------------------------------ FX1 cast / projectile
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = Mth.clamp(age / inst.duration, 0, 1), pw = Mth.clamp(inst.power, 0.5f, 3.2f);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        if (dir.lengthSquared() < 1e-4f) dir.set(0, 0, 1);
        float len = dir.length();
        dir.normalize();
        RandomSource r = inst.random();
        int au = aura(inst), gl = glow(inst), ru = rune(inst);

        float charge = Mth.clamp(p / 0.25f, 0, 1), fly = Mth.clamp((p - 0.2f) / 0.65f, 0, 1), hit = Mth.clamp((p - 0.82f) / 0.18f, 0, 1);
        float ease = VfxAnim.easeInOutSine(fly) * 0.35f + fly * 0.65f;

        // 1. the rune gate at the hand: opens fast, holds, then shrinks away as the beast leaves
        float gate = (p < 0.25f ? VfxAnim.easeOutBack(charge) : 1f - Mth.clamp((p - 0.4f) / 0.4f, 0, 1)) * 0.95f * pw;
        if (gate > 0.02f) {
            VfxPose pose = VfxPose.facing(new Vector3f(from).fma(0.25f * pw, dir), dir);
            float fa = Mth.clamp(gate / pw * 1.2f, 0, 1);
            VfxBloom.planeGlow(buf, pose.lift(-0.03f), gate * 0.9f, gl, 0.45f * fa);
            buf.plane(RUNE_RING, VfxBlend.ADD, pose.spin(age * 0.07f), gate, a(ru, fa));
            buf.plane(RUNE_RING, VfxBlend.ADD, pose.spin(-age * 0.11f).lift(0.04f), gate * 0.62f, a(VfxVertexBuffer.lerpColor(ru, gl, 0.4f), fa));
        }
        // 2. gathering embers: sucked into the gate during the charge
        if (p < 0.4f) {
            for (int i = 0; i < 6; i++) {
                float ang = r.nextFloat() * Mth.TWO_PI, rad = (0.8f + r.nextFloat() * 0.9f) * pw, ph = r.nextFloat();
                float q = (charge * 0.9f + ph) % 1f, k = 1f - q;
                Vector3f side = new Vector3f(dir).cross(0, 1, 0);
                if (side.lengthSquared() < 1e-4f) side.set(1, 0, 0);
                side.normalize();
                Vector3f up = new Vector3f(side).cross(dir).normalize();
                Vector3f at = new Vector3f(from).fma(Mth.cos(ang) * rad * k, side).fma(Mth.sin(ang) * rad * k, up).fma(0.2f * pw, dir);
                ember(buf, ctx, at, 0.22f * pw, Mth.sin(q * Mth.PI) * Mth.clamp((0.4f - p) * 6f, 0, 1), HOT);
            }
        }
        if (fly <= 0f) {
            VfxBloom.glow(ctx, buf, new Vector3f(from).fma(0.25f * pw, dir), 0.8f * pw * charge, gl, 0.6f * charge);
            return;
        }

        // 3. the beast: flies from -> to, a dark horned stippled aura around a hot core, slit eye on it
        float travel = len * ease;
        Vector3f head = new Vector3f(from).fma(travel, dir);
        float fade = 1f - Mth.clamp((p - 0.88f) / 0.12f, 0, 1);
        float lift = Mth.sin(age * 0.9f) * 0.05f * pw;
        head.y += lift;
        float tailLen = Math.min(travel, (2.6f + 1.2f * pw) * 1.0f);
        Vector3f tail = new Vector3f(head).fma(-tailLen, dir);
        // ragged lick: wide dark underlay, narrow hot core; textures scroll along the length
        buf.beam(ctx, STREAK, VfxBlend.ALPHA, head, tail, 0.9f * pw, 1.5f * pw, 5, -age * 0.12f, a(au, 0.85f * fade), a(au, 0.0f));
        buf.beam(ctx, STREAK, VfxBlend.ADD, head, new Vector3f(head).fma(-tailLen * 0.8f, dir), 0.5f * pw, 0.9f * pw, 5, -age * 0.2f, a(gl, 0.9f * fade), a(gl, 0f));
        // smoke and ember wake
        for (int i = 0; i < 4; i++) {
            float f = (i + 0.5f) / 4f, wob = Mth.sin(age * 0.5f + i * 2.1f) * 0.18f * pw;
            Vector3f at = new Vector3f(head).fma(-tailLen * f, dir).add(0, wob + 0.15f * pw * f, 0);
            buf.billboard(ctx, PUFF, VfxBlend.ALPHA, at, (0.9f + f * 0.9f) * pw, age * 0.05f * (i % 2 == 0 ? 1 : -1) + i, a(au, 0.7f * (1 - f) * fade));
        }
        for (int i = 0; i < 6; i++) {
            float f = (r.nextFloat() * 0.9f + 0.05f), ph = r.nextFloat() * Mth.TWO_PI;
            Vector3f at = new Vector3f(head).fma(-tailLen * f * 1.1f, dir).add(Mth.sin(ph + age * 0.4f) * 0.35f * pw, Mth.cos(ph * 1.3f + age * 0.3f) * 0.3f * pw + f * 0.3f, 0);
            ember(buf, ctx, at, 0.2f * pw, (1 - f) * fade, HOT);
        }
        // two claw slashes left hanging in the air behind the beast
        for (int i = 0; i < 2; i++) {
            float back = (0.18f + 0.22f * i) * Math.min(travel, 3f);
            Vector3f at = new Vector3f(head).fma(-back, dir).add(0, 0.1f * pw, 0);
            float k = fade * (1 - 0.35f * i) * Mth.clamp(1 - back / 3.2f, 0.2f, 1);
            buf.billboard(ctx, CLAWS, VfxBlend.ADD, at, 1.5f * pw, 0.5f - 0.5f * i + (float) Mth.atan2(dir.dot(ctx.camUp), dir.dot(ctx.camRight)) * 0.0f, a(i == 0 ? HOT : gl, 0.8f * k));
        }
        // the beast body: two stipple layers turning in opposite senses, glow, eye
        buf.billboard(ctx, AURA, VfxBlend.ALPHA, head, 2.6f * pw, Mth.sin(age * 0.3f) * 0.08f, a(au, 0.95f * fade));
        VfxBloom.glow(ctx, buf, head, 1.1f * pw, gl, 0.65f * fade);
        buf.billboard(ctx, AURA_B, VfxBlend.ADD, new Vector3f(head).fma(0.05f * pw, dir), 1.35f * pw, -age * 0.04f, a(gl, 0.35f * fade));
        buf.billboard(ctx, EYE, VfxBlend.ADD, new Vector3f(head).add(0, 0.28f * pw, 0).fma(0.1f, dir), 0.55f * pw, 0, a(HOT, fade));
        VfxBloom.glow(ctx, buf, head, 0.5f * pw, WHITE, 0.7f * fade);
        // 4. a small gate flashes where the beast passes the middle of the flight
        float mid = Mth.clamp(1f - Math.abs(fly - 0.5f) * 5f, 0, 1);
        if (mid > 0.02f) {
            VfxPose pose = VfxPose.facing(new Vector3f(from).fma(len * 0.5f, dir), dir);
            buf.plane(RUNE_RING, VfxBlend.ADD, pose.spin(age * 0.15f), 0.8f * pw * (0.7f + 0.3f * mid), a(ru, mid * 0.8f));
        }
        // 5. arrival at 'to': flash, claw cross, shockwave
        if (hit > 0f) {
            float h = VfxAnim.easeOutCubic(hit), k = 1f - hit * 0.7f;
            VfxBloom.glow(ctx, buf, to, (1.0f + 1.6f * h) * pw, gl, 0.9f * k);
            buf.billboard(ctx, CLAWS, VfxBlend.ADD, to, 1.8f * pw * (0.6f + 0.5f * h), 0.8f, a(HOT, k));
            buf.billboard(ctx, CLAWS, VfxBlend.ADD, to, 1.4f * pw * (0.6f + 0.5f * h), -2.2f, a(WHITE, k * 0.8f));
            buf.billboard(ctx, EMBER, VfxBlend.ADD, to, 1.4f * pw * h, 0.4f, a(WHITE, k));
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.facing(to, dir), 0.5f * pw * h, 0.85f * pw * h, ctx.seg(10, 6), 1, 0, a(gl, 0.6f * k));
        }
    }

    // ------------------------------------------------------------------ FX2 zone / field
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), fade = life(inst, age, 8, 12), R = Math.max(1.5f, inst.power);
        if (fade <= 0.01f) return;
        float open = VfxAnim.easeOutCubic(Mth.clamp(age / 14f, 0, 1));
        Vector3f c = ctx.rel(inst.from(ctx));
        RandomSource r = inst.random();
        int au = aura(inst), gl = glow(inst), ru = rune(inst);
        VfxPose floor = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        float beastK = Mth.clamp(R / 6f, 0.5f, 1.5f);

        // 1. dark scorch glow under everything, then the two counter-rotating rune rings
        VfxBloom.planeGlow(buf, floor, R * 1.02f * open, DARK, 0.5f * fade);
        buf.plane(RUNE_RING, VfxBlend.ADD, floor.lift(0.02f).spin(age * 0.018f), R * open, a(ru, fade));
        buf.plane(RUNE_RING, VfxBlend.ADD, floor.lift(0.04f).spin(-age * 0.03f), R * 0.64f * open, a(VfxVertexBuffer.lerpColor(ru, gl, 0.35f), 0.9f * fade));
        // pulse wave out to the rim once a second
        float pu = (age % 20f) / 20f;
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, floor.lift(0.06f), R * pu * 0.92f, R * (pu * 0.92f + 0.08f), ctx.seg(12, 8), 1, 0, a(gl, 0.65f * (1 - pu) * fade));

        // 2. paw prints pressed round the rim, appearing one after another
        int paws = 6;
        for (int i = 0; i < paws; i++) {
            float ang = Mth.TWO_PI * (i + 0.5f) / paws + 0.4f, rad = R * (0.45f + 0.1f * (i % 2));
            float show = Mth.clamp((age - i * 3f) / 5f, 0, 1);
            if (show <= 0) continue;
            float s = R * 0.12f + 0.2f, rot = ang + Mth.HALF_PI;
            Vector3f ctr = floor.lift(0.035f).point(Mth.cos(ang) * rad, Mth.sin(ang) * rad);
            float cs = Mth.cos(rot) * s, sn = Mth.sin(rot) * s;
            Vector3f ex = floor.right(), ey = floor.up();
            Vector3f rx = new Vector3f(ex).mul(cs).fma(sn, ey), ry = new Vector3f(ey).mul(cs).fma(-sn, ex);
            int col = a(RED, 0.85f * show * fade);
            buf.quad(PAW, VfxBlend.ADD, new Vector3f(ctr).sub(rx).sub(ry), new Vector3f(ctr).add(rx).sub(ry), new Vector3f(ctr).add(rx).add(ry), new Vector3f(ctr).sub(rx).add(ry), 0, 0, 1, 1, col, col);
        }

        // 3. fangs rising from the ground at the rim, and horned beast silhouettes standing behind them
        int fangs = 8;
        for (int i = 0; i < fangs; i++) {
            float ang = Mth.TWO_PI * i / fangs + 0.2f + r.nextFloat() * 0.2f, rad = R * (0.9f + r.nextFloat() * 0.08f);
            float grow = VfxAnim.easeOutBack(Mth.clamp((age - 4 - i * 1.5f) / 8f, 0, 1));
            if (grow <= 0) continue;
            float s = (0.9f + 0.6f * r.nextFloat()) * beastK * grow;
            Vector3f at = new Vector3f(c).add(Mth.cos(ang) * rad, s * 0.5f, Mth.sin(ang) * rad);
            buf.billboard(ctx, FANG, VfxBlend.ALPHA, at, s, (Mth.cos(ang * 2) * 0.25f), a(VfxVertexBuffer.lerpColor(au, WHITE, 0.25f), 0.95f * fade));
        }
        int beasts = 5;
        for (int i = 0; i < beasts; i++) {
            float ang = Mth.TWO_PI * (i + 0.3f) / beasts + age * 0.004f, rad = R * 0.86f;
            float rise = VfxAnim.easeOutCubic(Mth.clamp((age - 6 - i * 2f) / 14f, 0, 1));
            float s = 2.6f * beastK * rise * (0.9f + 0.2f * Mth.sin(age * 0.15f + i));
            if (s <= 0.05f) continue;
            Vector3f at = new Vector3f(c).add(Mth.cos(ang) * rad, s * 0.5f, Mth.sin(ang) * rad);
            buf.billboard(ctx, i % 2 == 0 ? AURA : AURA_B, VfxBlend.ALPHA, at, s, (i - 2) * 0.03f, a(au, 0.8f * fade));
            // slit eyes blink open now and then
            float blink = Mth.clamp(Mth.sin(age * 0.11f + i * 1.9f) * 3f, 0, 1);
            if (blink > 0.05f && i != 3) buf.billboard(ctx, EYE, VfxBlend.ADD, new Vector3f(at).add(0, s * 0.16f, 0), s * 0.2f, 0, a(HOT, blink * fade));
        }

        // 4. a great beast looming over the centre: dark stipple, hot glow, a pair of eyes
        float loom = VfxAnim.easeOutCubic(Mth.clamp((age - 8f) / 20f, 0, 1)) * (1f + 0.04f * Mth.sin(age * 0.2f));
        float bs = R * 0.95f * loom + 0.1f;
        Vector3f top = new Vector3f(c).add(0, bs * 0.5f, 0);
        buf.billboard(ctx, AURA, VfxBlend.ALPHA, top, bs, 0, a(au, 0.5f * fade));
        buf.billboard(ctx, AURA_B, VfxBlend.ADD, top, bs * 0.8f, 0, a(gl, 0.18f * fade));
        float lid = Mth.clamp(loom * 1.4f - 0.4f, 0, 1) * (0.6f + 0.4f * Mth.sin(age * 0.25f));
        buf.billboard(ctx, EYE, VfxBlend.ADD, new Vector3f(top).add(ctx.camRight.x * bs * 0.1f, bs * 0.12f, ctx.camRight.z * bs * 0.1f), bs * 0.16f, 0, a(HOT, lid * fade));
        buf.billboard(ctx, EYE, VfxBlend.ADD, new Vector3f(top).add(-ctx.camRight.x * bs * 0.1f, bs * 0.12f, -ctx.camRight.z * bs * 0.1f), bs * 0.16f, 0, a(HOT, lid * fade));

        // 5. embers drifting up from the whole disc
        int n = ctx.seg(12, 5);
        for (int i = 0; i < n; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = Mth.sqrt(r.nextFloat()) * R * 0.95f, sp = 0.012f + 0.01f * r.nextFloat(), ph = r.nextFloat();
            float q = (age * sp + ph) % 1f;
            Vector3f at = new Vector3f(c).add(Mth.cos(ang) * rad + Mth.sin(age * 0.1f + i) * 0.15f, 0.2f + q * (2.2f + 0.3f * R), Mth.sin(ang) * rad);
            ember(buf, ctx, at, 0.22f + 0.1f * beastK, Mth.sin(q * Mth.PI) * fade, i % 3 == 0 ? WHITE : HOT);
        }
    }

    // ------------------------------------------------------------------ FX3 impact / roar
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = Mth.clamp(age / inst.duration, 0, 1), pw = Mth.clamp(inst.power, 0.5f, 3.5f);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        boolean has = hint.lengthSquared() > 1e-3f;
        if (has) hint.normalize();
        RandomSource r = inst.random();
        int au = aura(inst), gl = glow(inst), ru = rune(inst);
        float out = 1f - Mth.clamp((p - 0.55f) / 0.45f, 0, 1);
        float burst = VfxAnim.explosionBurst(Mth.clamp(p * 1.8f, 0, 1));
        float flash = Mth.clamp(1f - age / 5f, 0, 1);

        // 1. flash and the lingering afterglow
        VfxBloom.glow(ctx, buf, c, (2.4f * burst + 0.5f) * pw, gl, 0.4f * out + 0.5f * flash);
        if (flash > 0) buf.billboard(ctx, EMBER, VfxBlend.ADD, c, 3.2f * pw * (1.2f - flash * 0.4f), age * 0.2f, a(WHITE, flash));
        // 2. beast cloud: two stipple layers turning against each other, rising a little
        Vector3f up = new Vector3f(c).add(0, 0.5f * pw * burst, 0);
        float bs = 3.0f * pw * burst;
        buf.billboard(ctx, AURA, VfxBlend.ALPHA, up, bs, age * 0.01f, a(au, 0.9f * out));
        buf.billboard(ctx, AURA_B, VfxBlend.ALPHA, up, bs * 0.8f, -age * 0.02f + 1.2f, a(VfxVertexBuffer.lerpColor(au, gl, 0.3f), 0.55f * out));
        buf.billboard(ctx, AURA_B, VfxBlend.ADD, up, bs * 0.6f, age * 0.03f, a(gl, 0.35f * out));
        for (int i = 0; i < 3; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, d = (0.6f + r.nextFloat() * 0.8f) * pw * burst;
            buf.billboard(ctx, PUFF, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * d, 0.3f * pw + r.nextFloat() * 0.8f * pw * burst, Mth.sin(ang) * d), (1.1f + r.nextFloat()) * pw * burst, ang, a(au, 0.6f * out));
        }
        // 3. rune ring facing the camera, two rings
        float ro = VfxAnim.easeOutCubic(Mth.clamp(p * 2.2f, 0, 1));
        float rk = Mth.clamp(out * 1.4f, 0, 1);
        buf.billboard(ctx, RUNE_RING, VfxBlend.ADD, up, 4.2f * pw * ro, age * 0.06f, a(ru, rk));
        buf.billboard(ctx, RUNE_RING, VfxBlend.ADD, up, 2.7f * pw * ro, -age * 0.09f, a(VfxVertexBuffer.lerpColor(ru, gl, 0.4f), 0.85f * rk));
        // 4. ground shockwave
        float sw = VfxAnim.easeOutCubic(Mth.clamp(p * 1.6f, 0, 1));
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0)), 3.2f * pw * sw, 3.2f * pw * sw + 0.35f * pw * (1 - sw * 0.5f), ctx.seg(14, 8), 1, 0, a(gl, 0.8f * out));
        // 5. claw slashes tearing out at random angles
        for (int i = 0; i < 3; i++) {
            float show = Mth.clamp((age - i * 1.5f) / 3f, 0, 1) * Mth.clamp(out * 1.5f, 0, 1);
            if (show <= 0) continue;
            float ang = r.nextFloat() * Mth.TWO_PI;
            Vector3f at = new Vector3f(c).add(Mth.cos(ang) * 0.7f * pw, 0.4f * pw + Mth.sin(ang) * 0.5f * pw, Mth.sin(ang) * 0.7f * pw).add(0, 0.5f * pw, 0);
            buf.billboard(ctx, CLAWS, VfxBlend.ADD, at, (1.9f + 0.4f * i) * pw * (0.7f + 0.3f * show), ang, a(i == 0 ? HOT : gl, 0.7f * show));
        }
        // 6. fangs and embers flying outward
        int fangs = ctx.seg(8, 4);
        for (int i = 0; i < fangs; i++) {
            Vector3f d = randDir(r);
            if (has) d.add(new Vector3f(hint).mul(0.8f)).normalize();
            float sp = (1.5f + r.nextFloat() * 2.2f) * pw, tt = VfxAnim.easeOutCubic(Mth.clamp(p * 1.3f, 0, 1));
            Vector3f at = new Vector3f(c).add(0, 0.5f * pw, 0).fma(sp * tt, d);
            at.y -= 1.5f * p * p * pw;
            pointing(buf, ctx, FANG, VfxBlend.ALPHA, at, d, (0.5f + r.nextFloat() * 0.4f) * pw, a(VfxVertexBuffer.lerpColor(au, WHITE, 0.3f), out));
        }
        int em = ctx.seg(12, 5);
        for (int i = 0; i < em; i++) {
            Vector3f d = randDir(r);
            float sp = (2.5f + r.nextFloat() * 3.5f) * pw, tt = VfxAnim.easeOutCubic(Mth.clamp(p * 1.1f, 0, 1));
            Vector3f at = new Vector3f(c).add(0, 0.5f * pw, 0).fma(sp * tt, d);
            at.y += 0.8f * p * pw;
            ember(buf, ctx, at, (0.25f + 0.2f * r.nextFloat()) * pw, out * (1f - p * 0.5f), i % 3 == 0 ? WHITE : HOT);
        }
        // 7. a pair of slit eyes flaring in the cloud
        float eye = Mth.clamp((age - 2f) / 3f, 0, 1) * Mth.clamp(1f - (p - 0.35f) / 0.4f, 0, 1);
        if (eye > 0.02f) {
            float es = 1.1f * pw * burst;
            Vector3f ey = new Vector3f(up).add(0, 0.35f * pw, 0);
            buf.billboard(ctx, EYE, VfxBlend.ADD, new Vector3f(ey).fma(0.45f * pw, ctx.camRight), es * 0.55f, 0.12f, a(HOT, eye));
            buf.billboard(ctx, EYE, VfxBlend.ADD, new Vector3f(ey).fma(-0.45f * pw, ctx.camRight), es * 0.55f, -0.12f, a(HOT, eye));
        }
    }

    private static Vector3f randDir(RandomSource r) {
        float u = r.nextFloat() * 2 - 1, phi = r.nextFloat() * Mth.TWO_PI, s = Mth.sqrt(1 - u * u);
        return new Vector3f(s * Mth.cos(phi), Math.abs(u) * 0.6f + 0.15f, s * Mth.sin(phi)).normalize();
    }
}
