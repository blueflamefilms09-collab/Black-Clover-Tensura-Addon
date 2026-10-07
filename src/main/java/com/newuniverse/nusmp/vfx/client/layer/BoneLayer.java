package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Bone Magic, drawn after the owner's still: dozens of huge warm-white skeletal arms radiating from the caster like a sunburst, every
 * bone long and segmented with ring-shaped joints and ending in a long curved claw, cel-shaded warm white with a light grey outline,
 * the grimoire in the middle glowing pale violet-white. The arms are single textured ribbons ({@code bone_arm} / {@code bone_limb}),
 * the glow is additive violet-white, so the bone stays solid and the magic stays luminous.
 * <ul>
 *   <li>BONE_FX1 (cast / projectile, Bone Spear): at 'from' six short skeletal claws unfold in a ring and a violet flare gathers, then
 *       a long bone lance (cone of bone with a vertebra bead streak behind it) flies to 'to', three violet afterimages lagging behind,
 *       splinters orbiting the lance and chalk dust shed along the path; at 'to' a flash, a shock ring of bone teeth and five claws
 *       snapping out. power = size scale, duration = flight ticks.</li>
 *   <li>BONE_FX2 (zone / field, Bone Cage): from = ground centre, power = RADIUS. A dark ink underlay, a rotating bone sigil
 *       (ribs, vertebrae, claw-arms) and a counter-rotating vertebra ring on the ground; twelve huge two-jointed skeletal arms rise out of
 *       the centre like spider legs and plant their claws at the rim (a cage); bone spikes burst up along the rim, chalk motes
 *       and splinters rise, a violet pulse ring runs outwards. Fades in over 8 ticks and out over the last 12.</li>
 *   <li>BONE_FX3 (impact / signature, Bone Burst): a flash, then twenty skeletal arms burst out of 'from' in all directions (the
 *       sunburst of the still: part in a camera-facing fan, part all around), overshoot and retract, curved claws and bone splinters
 *       fly out, two shock rings, chalk dust and a ghost skull glowing behind the arms, a violet afterglow. 'to - from' (if long)
 *       biases the arms into a cone along it. power = scale, duration = life ticks.</li>
 * </ul>
 * colour: inst.color is mixed (35 %) into the bone white; the violet glow is the magic's own and always present.
 */
public class BoneLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation ARM = t("bone_arm");
    public static final ResourceLocation LIMB = t("bone_limb");
    public static final ResourceLocation SPIKE = t("bone_spike");
    public static final ResourceLocation CLAW = t("bone_claw");
    public static final ResourceLocation SHARD = t("bone_shard");
    public static final ResourceLocation SKULL = t("bone_skull");
    public static final ResourceLocation RING = t("bone_ring");
    public static final ResourceLocation SIGIL = t("bone_sigil");
    public static final ResourceLocation DUST = t("bone_dust");
    public static final ResourceLocation FLARE = t("bone_flare");
    public static final ResourceLocation SHOCK = t("bone_shock");
    public static final ResourceLocation TRAIL = t("bone_trail");

    private static final int BONE = 0xFFF4F1E6;
    private static final int GREY = 0xFF9A968A;
    private static final int VIOLET = 0xFFC8B8FF;
    private static final int INK = 0xFF2A2634;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BONE_FX1, VfxShape.BONE_FX2, VfxShape.BONE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BONE_FX1 -> 16;
            case BONE_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFF0EAD0; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BONE_FX1 -> cast(inst, ctx, buf);
            case BONE_FX2 -> zone(inst, ctx, buf);
            default -> burst(inst, ctx, buf);
        }
    }

    // ------------------------------------------------------------------------------------------------ small helpers

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static Vector3f mad(Vector3f o, Vector3f d, float s) { return new Vector3f(d).mul(s).add(o); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }

    private static int boneColor(VfxInstance inst) { return VfxVertexBuffer.lerpColor(BONE, inst.color | 0xFF000000, 0.35f); }

    /** One skeletal arm as a camera-facing ribbon (base -> claw); 'w' is the ribbon width, the shaft is about a third of it. */
    private static void bone(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f base, Vector3f tip, float w, int col) {
        if (!buf.hasBudget(4)) return;
        buf.beam(ctx, tex, bl, base, tip, w, w * 0.92f, 1, 0f, col, col);
    }

    /** Vertical quad standing on 'base' that always turns its face to the camera around the vertical axis. */
    private static void upright(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f base, float w, float h, int col) {
        if (!buf.hasBudget(4)) return;
        Vector3f r = new Vector3f(-base.z, 0f, base.x);
        if (r.lengthSquared() < 1e-6f) r.set(1f, 0f, 0f);
        r.normalize().mul(w * 0.5f);
        Vector3f up = new Vector3f(0f, h, 0f);
        int c = bl.grade(col, 1f);
        buf.quad(tex, bl, new Vector3f(base).sub(r), new Vector3f(base).add(r), new Vector3f(base).add(r).add(up), new Vector3f(base).sub(r).add(up),
                0f, 0f, 1f, 1f, c, c);
    }

    private static void glow(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f p, float size, float rot, int col) {
        if (!buf.hasBudget(4)) return;
        buf.billboard(ctx, tex, bl, p, size, rot, col);
    }

    /** A direction in the camera plane (angle) tilted by 'z' towards the view axis. */
    private static Vector3f fan(VfxRenderContext ctx, float ang, float z) {
        Vector3f fwd = new Vector3f(ctx.camRight).cross(ctx.camUp);
        Vector3f d = new Vector3f(ctx.camRight).mul(Mth.cos(ang)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang))).add(fwd.mul(z));
        return d.normalize();
    }

    // ------------------------------------------------------------------------------------------------ FX1 : Bone Spear

    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float P = Math.max(0.5f, inst.power);
        int bone = boneColor(inst);
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(f);
        float dist = dir.length();
        if (dist < 0.05f) dir.set(ctx.camRight); else dir.div(dist);

        // 1. charge at 'from' (first quarter): violet gathering ring, flare, six claws unfolding
        float cp = sub(p, 0f, 0.25f), cfade = 1f - sub(p, 0.22f, 0.5f);
        if (cfade > 0f) {
            glow(ctx, buf, SHOCK, VfxBlend.ADD, f, P * (2.2f - 1.5f * VfxAnim.easeOutCubic(cp)), age * 0.15f, a(VIOLET, 0.8f * cfade * Math.min(1f, cp * 4f)));
            glow(ctx, buf, FLARE, VfxBlend.ADD, f, P * (0.4f + 1.1f * VfxAnim.easeOutCubic(cp)), age * 0.3f, a(VfxVertexBuffer.whiten(VIOLET, 0.5f), cfade));
            for (int i = 0; i < 6; i++) {
                float ang = Mth.TWO_PI * i / 6f + age * 0.08f;
                float g = VfxAnim.easeOutBack(cl(cp * 1.4f)) * 0.95f * P;
                Vector3f d = fan(ctx, ang, 0.15f);
                bone(ctx, buf, ARM, VfxBlend.ALPHA, mad(f, d, 0.12f * P), mad(f, d, 0.12f * P + g), 0.34f * P, a(bone, cfade));
            }
        }

        // 2. the lance and its afterimages
        float fp = sub(p, 0.12f, 0.82f);
        float travel = fp * 0.7f + fp * fp * (3f - 2f * fp) * 0.3f;
        Vector3f head = mad(f, dir, dist * travel);
        float lanceA = 1f - sub(p, 0.84f, 1f) * 0.8f;
        if (fp > 0f) {
            float len = 2.3f * P;
            float tailLen = Math.min(dist * travel, 5.2f * P);
            // streak of vertebra beads and chalk behind the lance
            if (tailLen > 0.2f)
                buf.beam(ctx, TRAIL, VfxBlend.ADD, mad(head, dir, -tailLen), head, 0.5f * P, 0.9f * P, 3, 0f, a(VIOLET, 0.0f + 0.8f * lanceA), a(VfxVertexBuffer.whiten(VIOLET, 0.6f), lanceA));
            for (int k = 3; k >= 1; k--) {
                float lag = Math.min(dist * travel, 0.95f * P * k);
                Vector3f hh = mad(head, dir, -lag);
                bone(ctx, buf, SPIKE, VfxBlend.ADD, mad(hh, dir, -len), hh, 0.85f * P * (1f - 0.12f * k), a(VIOLET, 0.34f / k * lanceA));
            }
            glow(ctx, buf, FLARE, VfxBlend.ADD, head, 1.3f * P, age * 0.5f, a(VIOLET, 0.55f * lanceA));
            bone(ctx, buf, SPIKE, VfxBlend.ALPHA, mad(head, dir, -len), mad(head, dir, 0.25f * P), 0.88f * P, a(bone, lanceA));
            glow(ctx, buf, FLARE, VfxBlend.ADD, mad(head, dir, 0.2f * P), 0.8f * P, -age * 0.4f, a(0xFFFFFFFF, 0.9f * lanceA));
            // splinters orbiting the lance
            Vector3f side = new Vector3f(dir).cross(ctx.camUp);
            if (side.lengthSquared() < 1e-4f) side.set(ctx.camRight);
            side.normalize();
            Vector3f up2 = new Vector3f(side).cross(dir).normalize();
            for (int i = 0; i < 3; i++) {
                float ang = age * 0.55f + Mth.TWO_PI * i / 3f, rr = 0.55f * P;
                Vector3f o = mad(mad(head, dir, -0.9f * P - i * 0.35f * P), side, Mth.cos(ang) * rr);
                o = mad(o, up2, Mth.sin(ang) * rr);
                glow(ctx, buf, SHARD, VfxBlend.ALPHA, o, 0.38f * P, ang * 1.7f, a(bone, lanceA));
            }
            // chalk dust shed along the flight path
            for (int i = 0; i < 6; i++) {
                float s = hash(inst.seed, i, 1) * Math.max(0.01f, travel);
                float life = cl((travel - s) * 3.2f + (p - 0.12f) * 0.0f);
                if (life <= 0f || life >= 1f) continue;
                Vector3f o = mad(f, dir, dist * s);
                o = mad(o, side, (hash(inst.seed, i, 2) - 0.5f) * 0.9f * P * (0.4f + life));
                o = mad(o, up2, (hash(inst.seed, i, 3) - 0.5f) * 0.9f * P * (0.4f + life) + life * 0.35f * P);
                glow(ctx, buf, DUST, VfxBlend.ALPHA, o, P * (0.4f + 0.7f * life), hash(inst.seed, i, 4) * 6f, a(GREY, 0.55f * (1f - life)));
            }
        }

        // 3. impact at 'to'
        float ip = sub(p, 0.78f, 1f);
        if (ip > 0f) {
            float e = VfxAnim.easeOutCubic(ip), fl = 1f - ip;
            glow(ctx, buf, FLARE, VfxBlend.ADD, to, P * (0.6f + 2.4f * e), age * 0.3f, a(0xFFFFFFFF, fl));
            glow(ctx, buf, SHOCK, VfxBlend.ADD, to, P * (0.5f + 2.6f * e), 0f, a(VIOLET, fl * 0.9f));
            for (int i = 0; i < 5; i++) {
                Vector3f d = fan(ctx, Mth.TWO_PI * i / 5f + hash(inst.seed, i, 5), 0.2f);
                float g = VfxAnim.easeOutBack(cl(ip * 2.2f)) * 1.15f * P;
                bone(ctx, buf, ARM, VfxBlend.ALPHA, mad(to, d, 0.1f * P), mad(to, d, 0.1f * P + g), 0.3f * P, a(bone, fl));
            }
            for (int i = 0; i < 4; i++) {
                Vector3f d = fan(ctx, Mth.TWO_PI * (i + 0.5f) / 4f, 0.3f);
                glow(ctx, buf, SHARD, VfxBlend.ALPHA, mad(to, d, (0.4f + 1.4f * e) * P), 0.3f * P, i + ip * 6f, a(bone, fl));
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ FX2 : Bone Cage

    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float R = Math.max(1.2f, inst.power);
        float fade = VfxAnim.fadeInOut(p, 8f / inst.duration, 12f / inst.duration);
        if (fade <= 0f) return;
        int bone = boneColor(inst);
        Vector3f c = ctx.rel(inst.from(ctx));
        VfxPose g = VfxPose.ground(c).lift(0.04f);
        float open = VfxAnim.easeOutCubic(cl(age / 12f));

        // ground: ink underlay, sigil (violet light), counter-rotating vertebra ring (bone)
        buf.plane(SIGIL, VfxBlend.ALPHA, g.spin(age * 0.012f), R * 1.04f * open, a(INK, 0.55f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, g.lift(0.01f).spin(age * 0.02f), R * open, a(VIOLET, 0.85f * fade));
        buf.plane(RING, VfxBlend.ALPHA, g.lift(0.02f).spin(-age * 0.03f), R * 1.02f * open, a(bone, 0.95f * fade));
        // pulse rings running out
        for (int k = 0; k < 2; k++) {
            float ph = ((age / 26f) + k * 0.5f) % 1f;
            buf.plane(SHOCK, VfxBlend.ADD, g.lift(0.03f), R * (0.25f + 0.85f * VfxAnim.easeOutCubic(ph)), a(VIOLET, 0.55f * (1f - ph) * fade * open));
        }

        // twelve spider-leg arms out of the centre, claws planted at the rim (two jointed ribbons each)
        int n = 12;
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + 0.12f * Mth.sin(age * 0.02f);
            boolean tall = (i & 1) == 0;
            float grow = VfxAnim.easeOutBack(cl((age - i * 0.9f) / 15f));
            if (grow <= 0f) continue;
            float reach = (tall ? 0.96f : 0.74f) * R, kneeR = (tall ? 0.42f : 0.34f) * R;
            float kneeH = (tall ? 0.62f : 0.42f) * R * (1f + 0.04f * Mth.sin(age * 0.15f + i));
            Vector3f d = new Vector3f(Mth.cos(ang), 0f, Mth.sin(ang));
            Vector3f root = mad(c, new Vector3f(0, 1, 0), 0.5f + 0.35f * (tall ? 0f : 1f));
            Vector3f knee = mad(mad(root, d, kneeR * grow), new Vector3f(0, 1, 0), (kneeH - 0.4f) * grow);
            Vector3f tip = mad(mad(c, d, reach * grow), new Vector3f(0, 1, 0), 0.05f);
            float w1 = knee.distance(root) * 0.40f, w2 = knee.distance(tip) * 0.38f;
            int col = a(bone, fade);
            bone(ctx, buf, LIMB, VfxBlend.ALPHA, root, knee, Math.max(0.2f, w1), col);
            bone(ctx, buf, ARM, VfxBlend.ALPHA, knee, tip, Math.max(0.2f, w2), col);
        }

        // spikes bursting up along the rim
        int ns = 14;
        for (int i = 0; i < ns; i++) {
            float ang = Mth.TWO_PI * (i + 0.5f * hash(inst.seed, i, 1)) / ns + 0.1f;
            float sg = VfxAnim.easeOutBack(cl((age - 4f - i * 0.6f) / 10f));
            if (sg <= 0f) continue;
            float h = (0.2f * R + 0.5f) * (0.7f + 0.6f * hash(inst.seed, i, 2)) * sg;
            Vector3f base = mad(c, new Vector3f(Mth.cos(ang), 0f, Mth.sin(ang)), R * (0.9f + 0.1f * hash(inst.seed, i, 3)));
            upright(buf, SPIKE, VfxBlend.ALPHA, base, h * 0.55f, h, a(bone, fade));
        }

        // rising chalk motes and splinters
        for (int i = 0; i < 9; i++) {
            float ph = (age / (34f + 10f * hash(inst.seed, i, 4)) + hash(inst.seed, i, 5)) % 1f;
            float ang = hash(inst.seed, i, 6) * Mth.TWO_PI, rr = R * (0.15f + 0.8f * hash(inst.seed, i, 7));
            Vector3f o = new Vector3f(c.x + Mth.cos(ang) * rr, c.y + 0.1f + ph * R * 0.8f, c.z + Mth.sin(ang) * rr);
            float al = Mth.sin(ph * Mth.PI) * fade;
            glow(ctx, buf, DUST, VfxBlend.ADD, o, 0.5f + 0.4f * hash(inst.seed, i, 8), age * 0.02f + i, a(VIOLET, 0.5f * al));
            if (i < 3) glow(ctx, buf, SHARD, VfxBlend.ALPHA, o, 0.3f, age * 0.07f + i, a(bone, 0.9f * al));
        }
        // the grimoire glow in the middle
        float pu = VfxAnim.pulse(age, 1.6f);
        glow(ctx, buf, FLARE, VfxBlend.ADD, mad(c, new Vector3f(0, 1, 0), 1.0f), (1.5f + 0.5f * pu) * Math.min(2f, 0.6f + R * 0.12f), age * 0.1f, a(VIOLET, 0.8f * fade));
    }

    // ------------------------------------------------------------------------------------------------ FX3 : Bone Burst

    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float P = Math.max(0.5f, inst.power);
        int bone = boneColor(inst);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        boolean aimed = hint.lengthSquared() > 1.0f;
        if (aimed) hint.normalize();
        float fade = 1f - sub(p, 0.72f, 1f);
        float retract = 1f - VfxAnim.easeInCubic(sub(p, 0.66f, 0.98f));

        // back: ghost skull and violet halo
        glow(ctx, buf, SKULL, VfxBlend.ADD, c, 2.4f * P * (0.8f + 0.3f * VfxAnim.easeOutCubic(p)), 0f, a(VIOLET, 0.45f * Mth.sin(Mth.PI * sub(p, 0.05f, 0.9f))));
        glow(ctx, buf, DUST, VfxBlend.ADD, c, 4.0f * P * (0.5f + VfxAnim.easeOutCubic(p)), age * 0.03f, a(VIOLET, 0.5f * (1f - p)));

        // the sunburst of arms: 12 around in the world, 8 in the camera fan
        int n = 20;
        float spin = age * 0.012f;
        for (int i = 0; i < n; i++) {
            Vector3f d;
            if (i < 12) {
                float yy = 1f - 2f * (i + 0.5f) / 12f, rr = (float) Math.sqrt(Math.max(0f, 1f - yy * yy)), th = i * 2.399963f + spin;
                d = new Vector3f(rr * Mth.cos(th), yy, rr * Mth.sin(th));
            } else {
                d = fan(ctx, Mth.TWO_PI * (i - 12 + 0.5f * hash(inst.seed, i, 1)) / 8f + spin, 0.1f * (hash(inst.seed, i, 2) - 0.5f));
            }
            if (aimed) d.mul(0.45f).add(new Vector3f(hint).mul(0.9f)).normalize();
            float delay = hash(inst.seed, i, 3) * 0.12f;
            float gr = VfxAnim.easeOutBack(cl((p - delay) / 0.22f)) * retract;
            if (gr <= 0.01f) continue;
            float len = (1.7f + 1.5f * hash(inst.seed, i, 4)) * P * gr;
            Vector3f base = mad(c, d, 0.18f * P);
            Vector3f tip = mad(c, d, 0.18f * P + len);
            bone(ctx, buf, ARM, VfxBlend.ALPHA, base, tip, len * 0.42f, a(bone, fade));
        }

        // curved claws and splinters flung out
        for (int i = 0; i < 8; i++) {
            Vector3f d = new Vector3f(hash(inst.seed, i, 5) - 0.5f, hash(inst.seed, i, 6) - 0.5f, hash(inst.seed, i, 7) - 0.5f);
            if (d.lengthSquared() < 1e-4f) d.set(0, 1, 0);
            d.normalize();
            if (aimed) d.mul(0.5f).add(new Vector3f(hint)).normalize();
            float e = VfxAnim.easeOutCubic(sub(p, 0.04f, 0.9f));
            Vector3f o = mad(c, d, (0.8f + 2.8f * hash(inst.seed, i, 8)) * P * e);
            o.y -= 0.9f * P * e * e * (i & 1);
            boolean isClaw = i < 4;
            glow(ctx, buf, isClaw ? CLAW : SHARD, VfxBlend.ALPHA, o, (isClaw ? 0.65f : 0.4f) * P, i * 1.3f + age * 0.18f * (isClaw ? 1 : 2), a(bone, fade));
        }
        for (int i = 0; i < 4; i++) {
            Vector3f d = fan(ctx, Mth.TWO_PI * (i + hash(inst.seed, i, 9)) / 4f, 0.2f);
            float e = VfxAnim.easeOutCubic(sub(p, 0.06f, 0.9f));
            glow(ctx, buf, SHARD, VfxBlend.ALPHA, mad(c, d, (0.6f + 1.6f * hash(inst.seed, i, 10)) * P * e), 0.34f * P, age * 0.3f + i, a(bone, fade));
        }

        // chalk dust puffs
        for (int i = 0; i < 5; i++) {
            Vector3f d = fan(ctx, Mth.TWO_PI * i / 5f + 0.5f, 0.3f);
            float e = VfxAnim.easeOutCubic(sub(p, 0.03f, 0.95f));
            Vector3f o = mad(c, d, (0.5f + 1.3f * hash(inst.seed, i, 11)) * P * e);
            o.y += 0.5f * P * e;
            glow(ctx, buf, DUST, VfxBlend.ALPHA, o, P * (0.9f + 1.4f * e), i * 2f, a(GREY, 0.5f * (1f - e) * fade));
        }

        // front: flash, shock rings, afterglow
        float fl = (1f - sub(p, 0f, 0.3f));
        glow(ctx, buf, FLARE, VfxBlend.ADD, c, P * (0.8f + 2.0f * VfxAnim.easeOutCubic(sub(p, 0f, 0.2f))), age * 0.2f, a(0xFFFFFFFF, 0.7f * fl * fl));
        float e1 = VfxAnim.easeOutCubic(sub(p, 0f, 0.7f)), e2 = VfxAnim.easeOutCubic(sub(p, 0.08f, 0.85f));
        glow(ctx, buf, SHOCK, VfxBlend.ADD, c, P * (0.8f + 8.0f * e1), age * 0.02f, a(VIOLET, 0.6f * (1f - e1)));
        glow(ctx, buf, SHOCK, VfxBlend.ALPHA, c, P * (0.6f + 5.4f * e2), -age * 0.03f, a(bone, 0.7f * (1f - e2)));
        float after = sub(p, 0.3f, 0.6f) * (1f - sub(p, 0.75f, 1f));
        glow(ctx, buf, FLARE, VfxBlend.ADD, c, P * (1.4f + 0.4f * VfxAnim.pulse(age, 2f)), age * 0.1f, a(VIOLET, 0.6f * after));
    }
}
