package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.client.upgrade.UpgradeConfig;
import com.newuniverse.nusmp.client.upgrade.UpgradeSpec;
import com.newuniverse.nusmp.client.upgrade.UpgradeSpecs;
import com.newuniverse.nusmp.relics.upgrade.UpgradeItems;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.AbstractVfxLayer;
import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxInstance;
import com.newuniverse.nusmp.vfx.client.VfxPose;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxTextures;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Why: the signature effect of an act of an upgraded weapon (docs/visual_upgrade/god_melee_a.md section 6). The server sends {@link VfxShape#UPGRADE_SIGNATURE} with the signature id of
 * (item, act) in the upper 32 bits of the seed ({@code UpgradeSignatures}); this layer finds the recipe in the item's spec ({@link UpgradeSpecs#signature}) and draws its components, each
 * on its own clock (delay, dur), out of the effect system's primitives: expanding rings, a turning glyph disc, a column of light, a burst of streaks, a spiral, a beam to the target, a
 * rain of streaks, forked bolts, orbiting marks, a flash. Which components, in which colours and textures, is the weapon's own (its recipe), so no two weapons answer an act alike.
 * <p>
 * The geometry is a few dozen quads per recipe (the activation budget of 400 vertices applies and the quality detail scales the segment counts); everything is computed from the packet and the
 * age, nothing is kept between frames but a texture-name cache. {@code from} follows the caster (his feet), {@code to} is the point he looked at.
 */
public class UpgradeSignatureLayer extends AbstractVfxLayer {
    private static final Map<String, ResourceLocation> TEX = new HashMap<>();

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.UPGRADE_SIGNATURE); }

    @Override public int defaultDuration(VfxShape s) { return 52; }

    @Override public int defaultColor(VfxShape s) { return 0xFFFFFFFF; }

    private static ResourceLocation tex(String name) {
        ResourceLocation t = TEX.get(name);
        if (t == null) {
            t = VfxTextures.byName(name);
            TEX.put(name, t);
        }
        return t;
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (!UpgradeConfig.signatures()) return;
        UpgradeSpec.Act act = UpgradeSpecs.signature(UpgradeItems.signatureOf(inst.seed));
        if (act == null) return;
        float age = inst.ageTicks(ctx.partialTick);
        Vec3 from = inst.from(ctx), to = inst.to(ctx);
        for (UpgradeSpec.Comp c : act.recipe()) {
            float t = (age - c.delay()) / Math.max(1, c.dur());
            if (t < 0f || t > 1f) continue;
            Vec3 base = "to".equals(c.at()) ? to : from;
            draw(c, t, age, base, from, to, inst, ctx, buf);
        }
    }

    // ---------------------------------------------------------------- components
    private static float ease(float t) { return 1f - (1f - t) * (1f - t); }

    /** Fade in over the first tenth, out over the last third. */
    private static float env(float t) { return Math.min(1f, t * 10f) * Math.min(1f, (1f - t) * 3f); }

    private static float hash(float a) {
        float s = Mth.sin(a * 12.9898f) * 43758.547f;
        return s - (float) Math.floor(s);
    }

    private static void draw(UpgradeSpec.Comp c, float t, float age, Vec3 base, Vec3 from, Vec3 to, VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        ResourceLocation tex = tex(c.tex());
        int rgb = c.rgb() & 0xFFFFFF;
        float fade = env(t);
        int col = VfxVertexBuffer.withAlpha(0xFF000000 | rgb, fade);
        float seed = (inst.seed & 0xFFFF) / 65536f;
        switch (c.t()) {
            case "ring" -> {
                float r = Mth.lerp(ease(t), c.r0(), c.r1());
                float width = Math.max(0.12f, r * 0.09f);
                VfxPose pose = VfxPose.ground(ctx.rel(base.add(0, 0.07, 0)));
                buf.ring(tex, VfxBlend.ADD, pose, Math.max(0f, r - width), r + width, ctx.seg(28, 10), 1f, age * 0.02f, col);
            }
            case "glyph" -> {
                VfxPose pose = VfxPose.ground(ctx.rel(base.add(0, 0.06, 0))).spin(c.spin() * age);
                float grow = Math.min(1f, t * 6f);
                buf.plane(tex, VfxBlend.ADD, pose, c.r() * (0.4f + 0.6f * grow), col);
                buf.plane(tex, VfxBlend.ADD, VfxPose.ground(ctx.rel(base.add(0, 0.09, 0))).spin(-c.spin() * age * 1.4f + 0.7f), c.r() * 0.62f * (0.4f + 0.6f * grow), VfxVertexBuffer.withAlpha(col, 0.7f));
            }
            case "column" -> {
                Vector3f a = ctx.rel(base), b = ctx.rel(base.add(0, c.h() * ease(Math.min(1f, t * 3f)), 0));
                float w = Math.max(0.3f, c.r() * 2f) * (1f - 0.4f * t);
                buf.beam(ctx, tex("glow"), VfxBlend.ADD, a, b, w, w * 0.5f, 1, 0f, col, VfxVertexBuffer.withAlpha(col, 0.3f));
                buf.billboard(ctx, tex("glow"), VfxBlend.ADD, a, w * 3.2f, 0f, VfxVertexBuffer.withAlpha(col, 0.8f));
            }
            case "burst" -> {
                int n = Math.max(3, c.n());
                float len = c.len() * ease(t);
                Vec3 o = base.add(0, 0.9, 0);
                for (int i = 0; i < n; i++) {
                    float a = 6.2831853f * i / n + seed * 6.28f;
                    float el = (hash(i + seed * 17f) - 0.4f) * 1.2f;
                    Vec3 d = new Vec3(Mth.cos(a) * Mth.cos(el), Mth.sin(el), Mth.sin(a) * Mth.cos(el));
                    Vector3f p0 = ctx.rel(o.add(d.scale(len * 0.3f))), p1 = ctx.rel(o.add(d.scale(len)));
                    buf.beam(ctx, tex, VfxBlend.ADD, p0, p1, 0.22f, 0.04f, 1, 0f, col, VfxVertexBuffer.withAlpha(col, 0.2f));
                }
            }
            case "spiral" -> {
                int n = ctx.seg(Math.max(6, c.n()), 6);
                for (int i = 0; i < n; i++) {
                    float s = (float) i / n;
                    float a = 6.2831853f * c.turns() * s + age * 0.25f;
                    float rr = c.r() * (0.6f + 0.4f * s);
                    float y = c.h() * s * ease(Math.min(1f, t * 2.5f));
                    buf.billboard(ctx, tex("glow"), VfxBlend.ADD, ctx.rel(base.add(Mth.cos(a) * rr, 0.15 + y, Mth.sin(a) * rr)), 0.34f * (1f - 0.5f * s), 0f, VfxVertexBuffer.withAlpha(col, 1f - 0.5f * s));
                }
            }
            case "beam" -> {
                Vec3 a = c.from().equals("to") ? to : from.add(0, 1.0, 0);
                Vec3 b = c.to().equals("caster") ? from.add(0, 1.0, 0) : to;
                float w = Math.max(0.2f, c.w());
                buf.beam(ctx, tex, VfxBlend.ADD, ctx.rel(a), ctx.rel(b), w, w * 0.4f, ctx.seg(6, 2), age * 0.15f, col, VfxVertexBuffer.withAlpha(col, 0.4f));
            }
            case "rain" -> {
                int n = ctx.seg(Math.max(4, c.n()), 4);
                for (int i = 0; i < n; i++) {
                    float hx = (hash(i * 1.7f + seed * 31f) - 0.5f) * 2f * c.r(), hz = (hash(i * 3.1f + 5f + seed * 7f) - 0.5f) * 2f * c.r();
                    float fall = (t * 2.2f + hash(i * 0.9f)) % 1f;
                    float y = c.h() * (1f - fall);
                    Vec3 top = base.add(hx, y + c.len(), hz), bot = base.add(hx, y, hz);
                    buf.beam(ctx, tex, VfxBlend.ADD, ctx.rel(top), ctx.rel(bot), 0.12f, 0.06f, 1, 0f, VfxVertexBuffer.withAlpha(col, 0.2f), col);
                }
            }
            case "arcs" -> {
                int n = ctx.seg(Math.max(2, c.n()), 2);
                float step = (float) Math.floor(age * 0.5f);
                for (int i = 0; i < n; i++) {
                    float a = 6.2831853f * (i + hash(i + step * 0.37f)) / n;
                    float rr = c.r() * (0.3f + 0.7f * hash(i * 2.3f + step));
                    Vec3 top = base.add(Mth.cos(a) * rr * 0.3, c.h(), Mth.sin(a) * rr * 0.3), bot = base.add(Mth.cos(a) * rr, 0.0, Mth.sin(a) * rr);
                    Vec3 prev = top;
                    for (int sgm = 1; sgm <= 4; sgm++) {
                        float k = sgm / 4f;
                        Vec3 pnt = top.lerp(bot, k).add((hash(i * 5f + sgm + step) - 0.5f) * 1.1f * (1f - k * 0.5f), 0, (hash(i * 7f + sgm * 1.3f + step) - 0.5f) * 1.1f * (1f - k * 0.5f));
                        buf.beam(ctx, tex("spark"), VfxBlend.ADD, ctx.rel(prev), ctx.rel(pnt), 0.32f, 0.2f, 1, 0f, col, col);
                        prev = pnt;
                    }
                }
            }
            case "orbit" -> {
                int n = Math.max(2, c.n());
                for (int i = 0; i < n; i++) {
                    float a = 6.2831853f * i / n + c.spin() * age;
                    Vec3 p = base.add(Mth.cos(a) * c.r(), c.y(), Mth.sin(a) * c.r());
                    buf.billboard(ctx, tex, VfxBlend.ADD, ctx.rel(p), Math.max(0.2f, c.size()), a, col);
                }
            }
            case "flash" -> {
                float k = 1f - t;
                buf.billboard(ctx, tex("glow"), VfxBlend.ADD, ctx.rel(base.add(0, 1.0, 0)), c.size() * (0.5f + ease(t)), 0f, VfxVertexBuffer.withAlpha(0xFF000000 | rgb, k * k));
            }
            default -> { }
        }
    }
}
