package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ArcaneSpellLayer.panel;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.45: Slash, Compass and Mercury Magic at the Time Magic standard. Textures from tools/gen_attr_vfx_textures.py.
 * <ul>
 *   <li>SLASH_BLADES: jagged green blades running out along both forearms of the followed caster, flickering with mana.</li>
 *   <li>SLASH_WAVE: a jagged crescent flung 'from' -> 'to' at speed, afterimages trailing, a split of light where it ends.</li>
 *   <li>SLASH_SCYTHE: Death Scythe. A giant reaping arc sweeps a full circle round 'from', a scythe blade at its head.</li>
 *   <li>COMPASS_ARRAY: brass compasses hover in an arc behind the caster with a golden aura: brass rim, green dial with its
 *       rose, glass sheen, and a gold needle swinging round to point at 'to', overshooting and settling.</li>
 *   <li>COMPASS_NEEDLE: a gold needle flying 'from' -> 'to' with a gilt trail and sparkle.</li>
 *   <li>COMPASS_LOCK: a compass rose sigil snapping shut on the target: it spins down, brackets close, a gold flash.</li>
 *   <li>MERCURY_DOME: a dome of liquid silver: a chrome sphere half sunk in the ground with bands of reflected sky flowing
 *       round it, rising out of the ground as it forms and wobbling like a liquid.</li>
 *   <li>MERCURY_SPEAR: a hyper-dense chrome spear 'from' -> 'to' with a liquid tail and droplets.</li>
 *   <li>MERCURY_EAGLE: a giant eagle of liquid silver flies 'from' -> 'to', wings beating, a chrome wake and falling drops.</li>
 * </ul>
 */
public class SlashCompassMercuryLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation SBLADE = t("slash_blade"), CRESCENT = t("slash_crescent"), SARC = t("slash_arc"),
            RIM = t("compass_rim"), DIAL = t("compass_dial"), NEEDLE = t("compass_needle"), GLASS = t("compass_glass"),
            CHROME = t("merc_chrome"), SPHERE = t("merc_sphere"), SPEAR = t("merc_spear"), EAGLE = t("merc_eagle"),
            DROP = t("paint_drop"), RIPPLE = t("mirror_ripple");
    static final int GREEN = 0xFF48FF7A, BRASS = 0xFFC8A050, DIALC = 0xFF3A9A5E, GOLD = 0xFFFFC94A, SILVER = 0xFFE4E8F0;

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.SLASH_BLADES, VfxShape.SLASH_WAVE, VfxShape.SLASH_SCYTHE, VfxShape.COMPASS_ARRAY, VfxShape.COMPASS_NEEDLE,
                VfxShape.COMPASS_LOCK, VfxShape.MERCURY_DOME, VfxShape.MERCURY_SPEAR, VfxShape.MERCURY_EAGLE);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SLASH_BLADES -> 200;
            case COMPASS_ARRAY -> 60;
            case MERCURY_DOME -> 160;
            case MERCURY_EAGLE -> 26;
            case COMPASS_LOCK -> 30;
            default -> 14;
        };
    }

    @Override
    public int defaultColor(VfxShape s) {
        return switch (s) {
            case SLASH_BLADES, SLASH_WAVE, SLASH_SCYTHE -> GREEN;
            case COMPASS_ARRAY, COMPASS_NEEDLE, COMPASS_LOCK -> GOLD;
            default -> SILVER;
        };
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SLASH_BLADES -> blades(inst, ctx, buf);
            case SLASH_WAVE -> wave(inst, ctx, buf);
            case SLASH_SCYTHE -> scythe(inst, ctx, buf);
            case COMPASS_ARRAY -> array(inst, ctx, buf);
            case COMPASS_NEEDLE -> needle(inst, ctx, buf);
            case COMPASS_LOCK -> lock(inst, ctx, buf);
            case MERCURY_DOME -> dome(inst, ctx, buf);
            case MERCURY_SPEAR -> spear(inst, ctx, buf);
            case MERCURY_EAGLE -> eagle(inst, ctx, buf);
            default -> { }
        }
    }

    /** The followed entity's facing (body yaw), or the from->to direction when there is none (preview, nothing followed). */
    static Vector3f facing(VfxInstance inst, VfxRenderContext ctx) {
        float yawDeg = inst.followYaw(ctx);
        if (!Float.isNaN(yawDeg)) {
            float yaw = yawDeg * (Mth.PI / 180f);
            return new Vector3f(-Mth.sin(yaw), 0, Mth.cos(yaw));
        }
        Vector3f d = ctx.rel(inst.to(ctx)).sub(ctx.rel(inst.from(ctx)));
        d.y = 0;
        return d.lengthSquared() < 1e-4f ? new Vector3f(0, 0, 1) : d.normalize();
    }

    // ================================================================ Slash
    private void blades(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), L = inst.power;
        Vector3f g = ctx.rel(inst.from(ctx)), fwd = facing(inst, ctx), right = new Vector3f(fwd).cross(0, 1, 0).normalize();
        float grow = VfxAnim.easeOutBack(Mth.clamp(age / 5f, 0, 1)), fade = life(inst, age, 0, 8);
        for (int s = -1; s <= 1; s += 2) {
            Vector3f elbow = new Vector3f(g).add(new Vector3f(right).mul(0.38f * s)).add(0, 1.1f, 0).add(new Vector3f(fwd).mul(0.1f));
            Vector3f tip = new Vector3f(elbow).add(new Vector3f(fwd).mul(1.3f * L * grow)).add(new Vector3f(right).mul(0.15f * s)).add(0, 0.1f, 0);
            float flick = 0.85f + 0.15f * Mth.sin(age * 1.3f + s);
            DreamPaintLayer.strokePart(buf, ctx, SBLADE, VfxBlend.ALPHA, elbow, tip, 0, 1, 0.32f * L, VfxVertexBuffer.withAlpha(0xFF1E8A3E, 0.85f * fade));
            DreamPaintLayer.strokePart(buf, ctx, SBLADE, VfxBlend.ADD, elbow, tip, 0, 1, 0.36f * L, VfxVertexBuffer.withAlpha(GREEN, 0.7f * flick * fade));
            VfxBloom.glow(ctx, buf, tip, 0.5f * L, GREEN, 0.4f * flick * fade);
        }
    }

    private void wave(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), w = inst.power, t = age / inst.duration;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-4f) return;
        dir.normalize();
        Vector3f right = new Vector3f(dir).cross(0, 1, 0);
        if (right.lengthSquared() < 1e-4f) right.set(1, 0, 0);
        right.normalize();
        Vector3f up = new Vector3f(right).cross(dir).normalize();
        float travel = VfxAnim.easeOutCubic(Mth.clamp(t / 0.5f, 0, 1)), fade = life(inst, age, 0, 5);
        for (int k = 3; k >= 0; k--) {                                        // afterimages behind the head
            float f = Math.max(0, travel - k * 0.06f);
            Vector3f c = new Vector3f(a).lerp(b, f);
            float al = (k == 0 ? 1f : 0.35f / k) * fade;
            // the crescent stands across the flight path: its 'right' is the travel direction so the horns trail
            panel(buf, CRESCENT, VfxBlend.ADD, c, new Vector3f(dir).negate(), right, 0.9f * w, 1.3f * w, VfxVertexBuffer.withAlpha(GREEN, al));
            panel(buf, CRESCENT, VfxBlend.ADD, c, new Vector3f(dir).negate(), up, 0.7f * w, 1.0f * w, VfxVertexBuffer.withAlpha(GREEN, 0.5f * al));
        }
        Vector3f head = new Vector3f(a).lerp(b, travel);
        VfxBloom.glow(ctx, buf, head, 1.2f * w, GREEN, 0.6f * fade);
        if (travel >= 1) DreamPaintLayer.strokePart(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(b).sub(new Vector3f(right).mul(w)),
                new Vector3f(b).add(new Vector3f(right).mul(w)), 0, 1, 0.12f * w, VfxVertexBuffer.withAlpha(0xFFFFFFFF, fade));
    }

    private void scythe(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = inst.power, t = age / inst.duration;
        Vector3f c = ctx.rel(inst.from(ctx)), fwd = ctx.rel(inst.to(ctx)).sub(c);
        fwd.y = 0;
        float start = fwd.lengthSquared() > 1e-4f ? (float) Math.atan2(fwd.z, fwd.x) : 0;
        float sweep = Mth.TWO_PI * VfxAnim.easeOutCubic(Mth.clamp(t / 0.4f, 0, 1)), fade = life(inst, age, 0, 7);
        VfxPose ground = VfxPose.ground(new Vector3f(c));
        buf.arc(SARC, VfxBlend.ADD, ground, R, R * 0.35f, start, sweep, ctx.seg(24, 12), VfxVertexBuffer.withAlpha(GREEN, fade));
        buf.arc(SARC, VfxBlend.ADD, ground.lift(0.3f), R * 0.92f, R * 0.12f, start, sweep, ctx.seg(24, 12), VfxVertexBuffer.withAlpha(0xFFE0FFE8, 0.7f * fade));
        // the scythe blade at the head of the sweep
        float h = start + sweep;
        Vector3f headDir = new Vector3f(Mth.cos(h), 0, Mth.sin(h)), tangent = new Vector3f(-Mth.sin(h), 0, Mth.cos(h));
        Vector3f hc = new Vector3f(c).add(new Vector3f(headDir).mul(R * 0.85f));
        if (t < 0.5f) {
            DreamPaintLayer.strokePart(buf, ctx, SBLADE, VfxBlend.ADD, new Vector3f(hc).sub(new Vector3f(headDir).mul(R * 0.5f)),
                    new Vector3f(hc).add(new Vector3f(tangent).mul(R * 0.25f)), 0, 1, 0.8f * R * 0.25f, VfxVertexBuffer.withAlpha(GREEN, fade));
            VfxBloom.glow(ctx, buf, hc, R * 0.4f, GREEN, 0.7f * fade);
        }
        buf.plane(VfxTextures.GLOW, VfxBlend.ADD, ground.lift(0.02f), R * VfxAnim.easeOutCubic(Mth.clamp(t * 2, 0, 1)), VfxVertexBuffer.withAlpha(GREEN, 0.15f * fade));
    }

    // ================================================================ Compass
    /** One compass facing 'n' at c: brass rim, green dial with the rose, gold needle at angle 'needle' (in the dial plane), glass. */
    static void compass(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, Vector3f n, float size, float needle, float a) {
        Vector3f right = side(n), up = new Vector3f(right).cross(n).normalize();
        if (up.y < 0) { up.negate(); right.negate(); }
        Vector3f lift = new Vector3f(n).mul(0.01f);
        panel(buf, DIAL, VfxBlend.ALPHA, c, right, up, size * 0.8f, size * 0.8f, VfxVertexBuffer.withAlpha(DIALC, a));
        panel(buf, DIAL, VfxBlend.ADD, new Vector3f(c).add(lift), right, up, size * 0.8f, size * 0.8f, VfxVertexBuffer.withAlpha(GOLD, 0.25f * a));
        Vector3f nd = new Vector3f(right).mul(Mth.cos(needle)).add(new Vector3f(up).mul(Mth.sin(needle)));
        Vector3f nz = new Vector3f(c).add(new Vector3f(lift).mul(2));
        DreamPaintLayer.strokePart(buf, ctx, NEEDLE, VfxBlend.ADD, new Vector3f(nz).sub(new Vector3f(nd).mul(size * 0.6f)),
                new Vector3f(nz).add(new Vector3f(nd).mul(size * 0.6f)), 0, 1, size * 0.18f, VfxVertexBuffer.withAlpha(GOLD, a));
        panel(buf, RIM, VfxBlend.ALPHA, new Vector3f(c).add(new Vector3f(lift).mul(3)), right, up, size, size, VfxVertexBuffer.withAlpha(BRASS, a));
        panel(buf, GLASS, VfxBlend.ADD, new Vector3f(c).add(new Vector3f(lift).mul(4)), right, up, size, size, VfxVertexBuffer.withAlpha(0xFFFFFFFF, 0.6f * a));
    }

    private void array(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        int n = Mth.clamp(Math.round(inst.power), 1, 7);
        Vector3f g = ctx.rel(inst.from(ctx)), target = ctx.rel(inst.to(ctx)), fwd = facing(inst, ctx), side = new Vector3f(fwd).cross(0, 1, 0).normalize();
        float pop = VfxAnim.easeOutBack(Mth.clamp(age / 8f, 0, 1)), fade = life(inst, age, 0, 8);
        VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, 1.6f, 0).sub(new Vector3f(fwd).mul(0.8f)), 2.6f, GOLD, 0.35f * fade);
        for (int k = 0; k < n; k++) {
            float a = (n == 1 ? 0 : (k / (float) (n - 1) - 0.5f)) * 2.4f;
            Vector3f c = new Vector3f(g).add(0, 1.7f + 0.5f * Mth.cos(a) + 0.08f * Mth.sin(age * 0.1f + k), 0)
                    .sub(new Vector3f(fwd).mul(0.8f)).add(new Vector3f(side).mul(Mth.sin(a) * 1.8f));
            float size = (0.45f + 0.1f * (k % 2)) * pop;
            // the needle swings round to the target, overshoots and settles (damped)
            Vector3f n0 = new Vector3f(fwd);
            Vector3f right = side(n0), up = new Vector3f(right).cross(n0).normalize();
            if (up.y < 0) { up.negate(); right.negate(); }
            Vector3f toT = new Vector3f(target).sub(c);
            float want = (float) Math.atan2(toT.dot(up), toT.dot(right));
            float settle = Mth.clamp(age / 20f, 0, 1);
            float needleA = want + (1 - settle) * 3f * Mth.cos(age * 0.6f + k) + 0.05f * Mth.sin(age * 0.3f + k);
            compass(ctx, buf, c, n0, size, needleA, fade);
        }
    }

    private void needle(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), s = inst.power, t = age / inst.duration;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-4f) return;
        float travel = VfxAnim.easeInOutSine(Mth.clamp(t / 0.8f, 0, 1)), fade = life(inst, age, 0, 3);
        Vector3f head = new Vector3f(a).lerp(b, travel), tail = new Vector3f(a).lerp(b, Math.max(0, travel - 0.25f));
        DreamPaintLayer.strokePart(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, tail, head, 0, 1, 0.12f * s, VfxVertexBuffer.withAlpha(GOLD, 0.7f * fade));
        Vector3f nd = new Vector3f(dir).normalize().mul(0.5f * s);
        DreamPaintLayer.strokePart(buf, ctx, NEEDLE, VfxBlend.ADD, new Vector3f(head).sub(nd), new Vector3f(head).add(nd), 0, 1, 0.16f * s, VfxVertexBuffer.withAlpha(GOLD, fade));
        buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, head, 0.4f * s, age * 0.3f, VfxVertexBuffer.withAlpha(0xFFFFFFFF, fade));
    }

    private void lock(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), s = inst.power, t = age / inst.duration;
        Vector3f g = ctx.rel(inst.from(ctx)).add(0, 0.05f, 0);
        float snap = VfxAnim.easeOutBack(Mth.clamp(age / 8f, 0, 1)), fade = life(inst, age, 0, 8);
        float r = s * (1.6f - 0.6f * snap);
        VfxPose ground = VfxPose.ground(g).spin((1 - snap) * 3f + age * 0.02f);
        buf.plane(DIAL, VfxBlend.ADD, ground, r, VfxVertexBuffer.withAlpha(GOLD, 0.8f * fade));
        buf.plane(RIM, VfxBlend.ADD, ground.lift(0.01f), r * 1.15f, VfxVertexBuffer.withAlpha(GOLD, 0.6f * fade));
        float flash = Mth.clamp(1 - Math.abs(age - 8) / 3f, 0, 1);
        if (flash > 0) VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, 0.6f * s, 0), 1.6f * s, GOLD, flash * fade);
        for (int k = 0; k < 4; k++) {                                         // brackets closing in
            float a = k * Mth.HALF_PI + 0.785f, rr = r * (1.6f - 0.4f * snap);
            Vector3f q = new Vector3f(g).add(Mth.cos(a) * rr, 0.6f * s, Mth.sin(a) * rr);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.3f * s, a, VfxVertexBuffer.withAlpha(GOLD, fade));
        }
    }

    // ================================================================ Mercury
    private void dome(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = inst.power;
        Vector3f g = ctx.rel(inst.from(ctx));
        float rise = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1)), fade = life(inst, age, 0, 10);
        float wob = 1 + 0.03f * Mth.sin(age * 0.5f) + 0.02f * Mth.sin(age * 1.3f);
        Vector3f c = new Vector3f(g).add(0, -R * 0.15f + (1 - rise) * -R, 0);
        // the sphere (half under the ground): a chrome billboard, then flowing bands of reflected sky round it
        buf.billboard(ctx, SPHERE, VfxBlend.ALPHA, c, 2 * R * wob, 0, VfxVertexBuffer.withAlpha(SILVER, 0.72f * fade));
        buf.billboard(ctx, SPHERE, VfxBlend.ADD, c, 2 * R * wob, 0, VfxVertexBuffer.withAlpha(0xFFFFFFFF, 0.2f * fade));
        // the liquid flowing: the specular highlight slides round the skin, a second softer one against it
        buf.billboard(ctx, SPHERE, VfxBlend.ADD, c, 2 * R * wob, age * 0.03f, VfxVertexBuffer.withAlpha(0xFFFFFFFF, 0.3f * fade));
        buf.billboard(ctx, SPHERE, VfxBlend.ADD, c, 1.9f * R * wob, -age * 0.05f + 2f, VfxVertexBuffer.withAlpha(0xFFDDE6FF, 0.18f * fade));
        // ripples running over its skin
        float rp = (age * 0.04f) % 1f;
        buf.billboard(ctx, RIPPLE, VfxBlend.ADD, new Vector3f(c).add(0, R * 0.5f, 0), R * (0.6f + 1.2f * rp), age * 0.01f, VfxVertexBuffer.withAlpha(0xFFFFFFFF, 0.25f * (1 - rp) * fade));
    }

    private void spear(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), s = inst.power, t = age / inst.duration;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 0.01f) return;
        dir.div(len);
        float travel = VfxAnim.easeInCubic(Mth.clamp(t / 0.5f, 0, 1)) * 0.6f + VfxAnim.easeOutCubic(Mth.clamp(t / 0.5f, 0, 1)) * 0.4f;
        float fade = life(inst, age, 0, 5);
        Vector3f head = new Vector3f(a).add(new Vector3f(dir).mul(len * travel));
        Vector3f back = new Vector3f(head).sub(new Vector3f(dir).mul(1.6f * s));
        Vector3f tail = new Vector3f(a).lerp(head, Mth.clamp(travel - 0.3f, 0, 1));
        // the liquid tail: chrome flowing back from the spear, thinning
        DreamPaintLayer.strokePart(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, tail, back, 0, 1, 0.25f * s, VfxVertexBuffer.withAlpha(SILVER, 0.45f * fade));
        DreamPaintLayer.strokePart(buf, ctx, SPEAR, VfxBlend.ALPHA, back, head, 0, 1, 0.3f * s, VfxVertexBuffer.withAlpha(SILVER, fade));
        DreamPaintLayer.strokePart(buf, ctx, SPEAR, VfxBlend.ADD, back, head, 0, 1, 0.3f * s, VfxVertexBuffer.withAlpha(0xFFFFFFFF, 0.35f * fade));
        int n = ctx.seg(5, 3);
        for (int i = 0; i < n; i++) {
            float f = hash(inst.seed, i, 1), fall = Mth.clamp(t * 2 - f, 0, 1);
            Vector3f q = new Vector3f(a).lerp(head, f).add(0, -1.2f * fall * fall, 0);
            buf.billboard(ctx, DROP, VfxBlend.ALPHA, q, 0.1f * s, 0, VfxVertexBuffer.withAlpha(SILVER, fade * (1 - fall)));
        }
    }

    private void eagle(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), span = inst.power, t = age / inst.duration;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-4f) return;
        dir.normalize();
        Vector3f wing = new Vector3f(dir).cross(0, 1, 0);
        if (wing.lengthSquared() < 1e-4f) wing.set(1, 0, 0);
        wing.normalize();
        float travel = VfxAnim.easeInOutSine(Mth.clamp(t, 0, 1)), fade = life(inst, age, 3, 5);
        float flap = 0.85f + 0.15f * Mth.sin(age * 0.8f);
        Vector3f c = new Vector3f(a).lerp(b, travel).add(0, 0.6f * Mth.sin(t * Mth.PI), 0);
        // the eagle turns its wings toward the viewer round its flight axis, head forward ('up' of the texture = flight direction)
        Vector3f nose = new Vector3f(dir).mul(-1);
        Vector3f toCam = new Vector3f(a).lerp(b, travel).negate();
        Vector3f faceWing = new Vector3f(dir).cross(toCam);
        if (faceWing.lengthSquared() > 1e-4f) wing.set(faceWing.normalize());
        panel(buf, EAGLE, VfxBlend.ALPHA, c, new Vector3f(wing).mul(flap), nose, span * 0.5f, span * 0.5f, VfxVertexBuffer.withAlpha(SILVER, 0.95f * fade));
        panel(buf, EAGLE, VfxBlend.ADD, new Vector3f(c).add(0, 0.02f, 0), new Vector3f(wing).mul(flap), nose, span * 0.5f, span * 0.5f,
                VfxVertexBuffer.withAlpha(0xFFFFFFFF, 0.3f * fade));
        VfxBloom.glow(ctx, buf, c, span * 0.5f, SILVER, 0.25f * fade);
        // the chrome wake and falling drops
        Vector3f wake = new Vector3f(a).lerp(b, Math.max(0, travel - 0.25f));
        DreamPaintLayer.strokePart(buf, ctx, VfxTextures.GLOW, VfxBlend.ADD, wake, c, 0, 1, span * 0.3f, VfxVertexBuffer.withAlpha(SILVER, 0.35f * fade));
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float f = hash(inst.seed, i, 1) * travel, fall = Mth.clamp((travel - f) * 3, 0, 1);
            Vector3f q = new Vector3f(a).lerp(b, f).add(new Vector3f(wing).mul((hash(inst.seed, i, 2) - 0.5f) * span)).add(0, -2.5f * fall * fall, 0);
            buf.billboard(ctx, DROP, VfxBlend.ALPHA, q, 0.15f, 0, VfxVertexBuffer.withAlpha(SILVER, fade * (1 - fall)));
        }
    }
}
