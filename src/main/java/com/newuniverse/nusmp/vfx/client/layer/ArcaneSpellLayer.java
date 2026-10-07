package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.34: Thread, Seal, Lightning, Transmutation, Poison and Spatial magic, drawn after the wiki's spell art to the Time Magic
 * standard (layered textures, glows, motion over the effect's life, everything inside the 400-vertex budget).
 * <ul>
 *   <li>THREAD_WEB, Arachne's Web: threads shoot out from the centre, then the web spreads over the ground and quivers.</li>
 *   <li>THREAD_STRINGS, Dancing Doll: puppet threads from the caster's fingers to the target's head and limbs, a control cross
 *       above the target, glints running down the threads.</li>
 *   <li>SEAL_CHAINS: three rune chains wrap the target and tighten, a seal circle under it.</li>
 *   <li>SEAL_TRINITY: three seal circles rise round the target, converge, and crystallise into a seal crystal.</li>
 *   <li>LIGHTNING_FIEND, Thunder Fiend: arcs jump over the caster's body, a crackling ring at the feet (power > 1.5: black
 *       lightning).</li>
 *   <li>LIGHTNING_GOD, God of Lightning Rising Salim: a pillar of lightning crashes down, bolts branch round it, the ground
 *       shock ring spreads.</li>
 *   <li>ALCHEMY_CIRCLE, Magic Convert: a transmutation circle turns, motes are drawn in and recombined into light.</li>
 *   <li>POISON_CURTAIN, Violett Schirm: a curtain of purple poison hangs along the line, dripping, mist at its foot.</li>
 *   <li>POISON_BREATH, Basilisk's Breath: billows of toxic mist roll out in a widening cone.</li>
 *   <li>SPACE_PORTAL, Fallen Angel Gate: an upright dimensional gate tears open, its rim spinning, shards of space orbiting.</li>
 * </ul>
 */
public class ArcaneSpellLayer extends AbstractVfxLayer {
    static ResourceLocation t(String n) { return VfxTextures.byName(n); }
    static final ResourceLocation WEB = t("arc_web"), CHAIN = t("arc_chain"), RUNES = t("arc_rune_band"), BOLT = t("arc_bolt"),
            ALCHEMY = t("arc_alchemy"), SMOKE = t("arc_smoke"), CURTAIN = t("arc_curtain"), PORTAL = t("arc_portal");

    @Override
    public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.THREAD_WEB, VfxShape.THREAD_STRINGS, VfxShape.SEAL_CHAINS, VfxShape.SEAL_TRINITY, VfxShape.LIGHTNING_FIEND,
                VfxShape.LIGHTNING_GOD, VfxShape.ALCHEMY_CIRCLE, VfxShape.POISON_CURTAIN, VfxShape.POISON_BREATH, VfxShape.SPACE_PORTAL);
    }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case THREAD_WEB -> 120;
            case THREAD_STRINGS -> 60;
            case SEAL_CHAINS -> 80;
            case SEAL_TRINITY -> 40;
            case LIGHTNING_FIEND -> 400;
            case LIGHTNING_GOD -> 26;
            case ALCHEMY_CIRCLE -> 30;
            case POISON_CURTAIN -> 160;
            case POISON_BREATH -> 30;
            case SPACE_PORTAL -> 400;
            default -> 30;
        };
    }

    @Override
    public int defaultColor(VfxShape s) {
        return switch (s) {
            case THREAD_WEB, THREAD_STRINGS -> 0xFFFF5070;
            case SEAL_CHAINS, SEAL_TRINITY -> 0xFFFFC870;
            case LIGHTNING_FIEND, LIGHTNING_GOD -> 0xFF8CE8FF;
            case ALCHEMY_CIRCLE -> 0xFF7AF0D8;
            case POISON_CURTAIN, POISON_BREATH -> 0xFFB050E0;
            case SPACE_PORTAL -> 0xFFB088FF;
            default -> 0xFFFFFFFF;
        };
    }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.LIGHTNING_GOD) VfxShake.add(inst.payload.from(), 1.3f, 14);
        else if (inst.shape == VfxShape.SEAL_TRINITY) VfxShake.add(inst.payload.from(), 0.5f, 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case THREAD_WEB -> web(inst, ctx, buf);
            case THREAD_STRINGS -> strings(inst, ctx, buf);
            case SEAL_CHAINS -> chains(inst, ctx, buf);
            case SEAL_TRINITY -> trinity(inst, ctx, buf);
            case LIGHTNING_FIEND -> fiend(inst, ctx, buf);
            case LIGHTNING_GOD -> god(inst, ctx, buf);
            case ALCHEMY_CIRCLE -> alchemy(inst, ctx, buf);
            case POISON_CURTAIN -> curtain(inst, ctx, buf);
            case POISON_BREATH -> breath(inst, ctx, buf);
            case SPACE_PORTAL -> portal(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ shared
    /** An oriented quad centred at c spanning +-hw along right and +-hh along up (texture upright along up). */
    static void panel(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, Vector3f right, Vector3f up, float hw, float hh, int argb) {
        Vector3f r = new Vector3f(right).mul(hw), u = new Vector3f(up).mul(hh);
        buf.quad(tex, blend, new Vector3f(c).sub(r).sub(u), new Vector3f(c).add(r).sub(u), new Vector3f(c).add(r).add(u), new Vector3f(c).sub(r).add(u),
                0, 0, 1, 1, argb, argb);
    }

    /** An upright camera-facing figure standing on {@code foot} (silhouettes, hands): the texture's top at the top. */
    static void stand(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f foot, float w, float h, int argb) {
        Vector3f right = new Vector3f(ctx.camRight.x, 0, ctx.camRight.z);
        if (right.lengthSquared() < 1e-4f) right.set(1, 0, 0);
        right.normalize();
        panel(buf, tex, blend, new Vector3f(foot).add(0, h / 2, 0), right, new Vector3f(0, 1, 0), w / 2, h / 2, argb);
    }

    /** A jagged bolt from a to b (camera-facing strip of the bolt texture, U along it). */
    static void bolt(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f a, Vector3f b, float width, int argb) {
        streak(buf, ctx, BOLT, VfxBlend.ADD, a, b, width, argb);
    }

    // ------------------------------------------------------------------ Arachne's Web
    private void web(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), r = inst.power;
        float spread = VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1));
        float fade = life(inst, age, 0, 14);
        Vector3f c = ctx.rel(inst.from(ctx)).add(0, 0.06f, 0);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.55f);
        // threads shooting out from the centre first
        int n = ctx.seg(12, 8);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n, len = r * Math.min(1, age / 6f);
            Vector3f tip = new Vector3f(c).add(Mth.cos(a) * len, 0.05f, Mth.sin(a) * len);
            streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, c, tip, 0.08f, VfxVertexBuffer.withAlpha(light, fade * (age < 12 ? 1f : 0.4f)));
        }
        // the web itself, quivering
        float quiver = 1 + 0.02f * Mth.sin(age * 0.6f);
        buf.plane(WEB, VfxBlend.ALPHA, VfxPose.ground(c).spin(0.1f * Mth.sin(age * 0.05f)), r * spread * quiver, VfxVertexBuffer.withAlpha(light, 0.95f * fade));
        buf.plane(WEB, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.02f, 0)), r * spread * quiver, VfxVertexBuffer.withAlpha(col, 0.5f * fade));
        // dew-like glints running round the rings
        int g = ctx.seg(10, 5);
        for (int i = 0; i < g; i++) {
            float ring = (0.3f + 0.65f * hash(inst.seed, i, 1)) * r * spread, a = hash(inst.seed, i, 2) * Mth.TWO_PI + age * 0.03f;
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(a) * ring, 0.08f, Mth.sin(a) * ring), 0.22f, age * 0.2f,
                    VfxVertexBuffer.withAlpha(WHITE, fade * (0.5f + 0.5f * Mth.sin(age * 0.4f + i))));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.2f, 0), 0.6f, col, 0.6f * fade);
    }

    // ------------------------------------------------------------------ Dancing Doll
    private void strings(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        float reach = VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1));
        float fade = life(inst, age, 0, 8);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f);
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-4f) return;
        Vector3f sd = side(new Vector3f(dir).normalize());
        // the control cross floating above the target
        Vector3f cross = new Vector3f(b).add(0, 2.2f * p, 0);
        float sway = Mth.sin(age * 0.15f) * 0.25f;
        Vector3f bar1a = new Vector3f(cross).add(new Vector3f(sd).mul(-0.7f * p)).add(0, sway * 0.3f, 0), bar1b = new Vector3f(cross).add(new Vector3f(sd).mul(0.7f * p)).sub(0, sway * 0.3f, 0);
        streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, bar1a, bar1b, 0.14f * p, VfxVertexBuffer.withAlpha(light, fade));
        streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, new Vector3f(cross).add(0, 0.5f * p, 0), new Vector3f(cross).sub(0, 0.5f * p, 0), 0.14f * p, VfxVertexBuffer.withAlpha(light, fade));
        // strings: caster's fingers -> cross, cross -> head, hands, feet of the target
        Vector3f[] limbs = {new Vector3f(b).add(0, 0.9f * p, 0), new Vector3f(b).add(new Vector3f(sd).mul(0.5f * p)).add(0, 0.2f, 0),
                new Vector3f(b).add(new Vector3f(sd).mul(-0.5f * p)).add(0, 0.2f, 0), new Vector3f(b).add(new Vector3f(sd).mul(0.25f * p)).add(0, -0.85f * p, 0),
                new Vector3f(b).add(new Vector3f(sd).mul(-0.25f * p)).add(0, -0.85f * p, 0)};
        Vector3f[] anchors = {cross, bar1b, bar1a, bar1b, bar1a};
        for (int k = 0; k < limbs.length; k++) {
            Vector3f end = new Vector3f(anchors[k]).lerp(limbs[k], reach);
            streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, anchors[k], end, 0.05f * p, VfxVertexBuffer.withAlpha(col, 0.9f * fade));
        }
        for (int k = 0; k < 3; k++) {
            Vector3f finger = new Vector3f(a).add(new Vector3f(sd).mul((k - 1) * 0.12f));
            Vector3f end = new Vector3f(finger).lerp(cross, reach);
            Vector3f mid = new Vector3f(finger).lerp(end, 0.5f).add(0, 0.6f + 0.15f * Mth.sin(age * 0.2f + k), 0);   // a little slack
            streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, finger, mid, 0.05f, VfxVertexBuffer.withAlpha(col, 0.9f * fade));
            streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, mid, end, 0.05f, VfxVertexBuffer.withAlpha(col, 0.9f * fade));
            // a glint running down each thread
            float gl = ((age * 0.06f) + k * 0.33f) % 1f;
            Vector3f q = gl < 0.5f ? new Vector3f(finger).lerp(mid, gl * 2) : new Vector3f(mid).lerp(end, gl * 2 - 1);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.2f, age * 0.3f, VfxVertexBuffer.withAlpha(WHITE, fade));
        }
        VfxBloom.glow(ctx, buf, cross, 0.5f * p, col, 0.6f * fade);
        VfxBloom.glow(ctx, buf, a, 0.3f, col, 0.6f * fade);
    }

    // ------------------------------------------------------------------ Seal chains
    private void chains(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float close = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        float fade = life(inst, age, 0, 10);
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f);
        // the seal circle under the target
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, -0.9f * p + 0.05f, 0)).spin(age * 0.02f);
        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, ground, 1.6f * p, VfxVertexBuffer.withAlpha(col, 0.8f * fade));
        buf.ring(RUNES, VfxBlend.ADD, ground, 1.6f * p, 1.95f * p, ctx.seg(16, 10), 4, age * 0.01f, VfxVertexBuffer.withAlpha(light, 0.8f * fade));
        // three chains wrapping round at different tilts, closing in
        for (int k = 0; k < 3; k++) {
            float tilt = 0.5f + 0.45f * k, yaw = k * 2.1f + age * 0.01f;
            Vector3f axis = new Vector3f(Mth.sin(tilt) * Mth.cos(yaw), Mth.cos(tilt), Mth.sin(tilt) * Mth.sin(yaw));
            float r = (2.6f - 1.85f * close) * p;
            hoop(buf, CHAIN, VfxBlend.ALPHA, VfxPose.facing(new Vector3f(c).add(0, (k - 1) * 0.3f * p, 0), axis), r, 0.12f * p, ctx.seg(14, 10), 4,
                    age * 0.02f * (k % 2 == 0 ? 1 : -1), VfxVertexBuffer.withAlpha(light, fade));
        }
        VfxBloom.glow(ctx, buf, c, 0.9f * p, col, 0.45f * fade * (1 - 0.5f * Mth.clamp(age / 20f, 0, 1)));        ArcaneSpellSealFx.chainsExtra(inst, ctx, buf, c, age, p, fade, close, col, light);
    }

    // ------------------------------------------------------------------ Trinity Seal
    private void trinity(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float t = Mth.clamp(age / (inst.duration * 0.6f), 0, 1), conv = VfxAnim.easeInOutSine(t);
        float burst = Mth.clamp((age - inst.duration * 0.6f) / (inst.duration * 0.4f), 0, 1);
        float fade = life(inst, age, 0, 8);
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f);
        for (int k = 0; k < 3; k++) {
            float a = Mth.TWO_PI * k / 3 + age * 0.04f, rad = 2.4f * p * (1 - conv);
            Vector3f at = new Vector3f(c).add(Mth.cos(a) * rad, 0.6f * p * Mth.sin(conv * Mth.PI), Mth.sin(a) * rad);
            Vector3f face = new Vector3f(c).sub(at);
            if (face.lengthSquared() < 1e-4f) face.set(0, 1, 0);
            VfxPose pose = VfxPose.facing(at, face).spin(age * 0.08f);
            buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, pose, 0.9f * p * (1 - 0.4f * conv), VfxVertexBuffer.withAlpha(col, fade * (1 - burst)));
            buf.ring(RUNES, VfxBlend.ADD, pose, 0.9f * p, 1.1f * p, ctx.seg(12, 8), 2, -age * 0.02f, VfxVertexBuffer.withAlpha(light, fade * (1 - burst)));
        }
        // the crystal the seals leave behind
        if (burst > 0) {
            float s = (0.6f + 0.5f * VfxAnim.easeOutBack(burst)) * p;
            buf.billboard(ctx, VfxTextures.SHARD, VfxBlend.ADD, c, 1.8f * s, age * 0.05f, VfxVertexBuffer.withAlpha(light, fade));
            buf.billboard(ctx, VfxTextures.SHARD, VfxBlend.ADD, c, 1.3f * s, -age * 0.07f + 0.8f, VfxVertexBuffer.withAlpha(WHITE, fade));
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, -0.8f * p, 0)), 3 * p * burst, 3.4f * p * burst, ctx.seg(16, 10), 1, 0,
                    VfxVertexBuffer.withAlpha(col, (1 - burst) * fade));
        }
        VfxBloom.glow(ctx, buf, c, (0.6f + conv) * p, col, fade);        ArcaneSpellSealFx.trinityExtra(inst, ctx, buf, c, age, p, fade, conv, burst, col, light);
    }

    // ------------------------------------------------------------------ Thunder Fiend
    private void fiend(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float fade = life(inst, age, 0, 10);
        Vector3f g = ctx.rel(inst.from(ctx));
        boolean black = p > 1.5f;
        int col = black ? 0xFF2A0A30 : inst.color, light = black ? 0xFFB0102C : VfxVertexBuffer.whiten(inst.color, 0.6f);
        float s = Math.min(p, 1.5f);
        // arcs jumping over the body: re-rolled every 2 ticks
        int frame = (int) (age / 2), n = ctx.seg(7, 4);
        for (int i = 0; i < n; i++) {
            float a0 = hash(inst.seed + frame, i, 1) * Mth.TWO_PI, a1 = a0 + (hash(inst.seed + frame, i, 2) - 0.5f) * 2.4f;
            float y0 = 0.1f + 1.8f * hash(inst.seed + frame, i, 3), y1 = Mth.clamp(y0 + (hash(inst.seed + frame, i, 4) - 0.5f) * 1.6f, 0, 2);
            Vector3f q0 = new Vector3f(g).add(Mth.cos(a0) * 0.45f * s, y0, Mth.sin(a0) * 0.45f * s);
            Vector3f q1 = new Vector3f(g).add(Mth.cos(a1) * 0.55f * s, y1, Mth.sin(a1) * 0.55f * s);
            bolt(buf, ctx, q0, q1, 0.35f * s, VfxVertexBuffer.withAlpha(black ? col : light, fade));
            if (black) bolt(buf, ctx, q0, q1, 0.15f * s, VfxVertexBuffer.withAlpha(light, fade));
        }
        // the crackling ring at the feet and sparks
        VfxPose foot = VfxPose.ground(new Vector3f(g).add(0, 0.06f, 0)).spin(age * 0.3f);
        buf.ring(BOLT, VfxBlend.ADD, foot, 0.7f * s, 1.05f * s, ctx.seg(12, 8), 3, age * 0.2f, VfxVertexBuffer.withAlpha(light, 0.8f * fade));
        int k = ctx.seg(8, 4);
        for (int i = 0; i < k; i++) {
            float ph = hash(inst.seed, i, 5) * 8, lt = ((age + ph) % 8f) / 8f, a = hash(inst.seed, i, 6) * Mth.TWO_PI;
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, new Vector3f(g).add(Mth.cos(a) * (0.5f + lt) * s, 0.2f + 1.6f * lt, Mth.sin(a) * (0.5f + lt) * s),
                    0.2f, age, VfxVertexBuffer.withAlpha(light, fade * (1 - lt)));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, 1, 0), 1.0f * s, black ? light : inst.color, 0.35f * fade * (0.7f + 0.3f * Mth.sin(age * 1.3f)));
    }

    // ------------------------------------------------------------------ God of Lightning Rising Salim
    private void god(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), r = inst.power;
        float strike = Mth.clamp(age / 4f, 0, 1), after = Mth.clamp((age - 4) / (inst.duration - 4f), 0, 1);
        float fade = 1 - VfxAnim.easeInCubic(after);
        Vector3f g = ctx.rel(inst.from(ctx));
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.6f);
        float h = 22f;
        Vector3f top = new Vector3f(g).add(0, h, 0), bottom = new Vector3f(top).lerp(g, strike);
        // the pillar: segmented so it stays jagged, wide glow behind it
        int segs = 6;
        Vector3f prev = top;
        for (int k = 1; k <= segs; k++) {
            float f = (float) k / segs;
            Vector3f q = new Vector3f(top).lerp(bottom, f).add((hash(inst.seed + (int) (age / 2), k, 1) - 0.5f) * 1.2f * r * (k < segs ? 1 : 0), 0,
                    (hash(inst.seed + (int) (age / 2), k, 2) - 0.5f) * 1.2f * r * (k < segs ? 1 : 0));
            bolt(buf, ctx, prev, q, 1.4f * r, VfxVertexBuffer.withAlpha(light, fade));
            bolt(buf, ctx, prev, q, 0.5f * r, VfxVertexBuffer.withAlpha(WHITE, fade));
            prev = q;
        }
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, top, bottom, 3.5f * r, 3.5f * r, 2, 0, VfxVertexBuffer.withAlpha(col, 0.45f * fade), VfxVertexBuffer.withAlpha(col, 0.45f * fade));
        // branches crawling out over the ground
        if (strike >= 1) {
            int n = ctx.seg(8, 5);
            for (int i = 0; i < n; i++) {
                float a = Mth.TWO_PI * i / n + hash(inst.seed, i, 3), len = (1.5f + 2.5f * hash(inst.seed, i, 4)) * r * Math.min(1, after * 3 + 0.3f);
                Vector3f mid = new Vector3f(g).add(Mth.cos(a + 0.3f) * len * 0.5f, 0.15f, Mth.sin(a + 0.3f) * len * 0.5f);
                Vector3f end = new Vector3f(g).add(Mth.cos(a) * len, 0.1f, Mth.sin(a) * len);
                bolt(buf, ctx, new Vector3f(g).add(0, 0.2f, 0), mid, 0.4f * r, VfxVertexBuffer.withAlpha(light, fade));
                bolt(buf, ctx, mid, end, 0.3f * r, VfxVertexBuffer.withAlpha(light, fade));
            }
            float sr = (1 + 6 * VfxAnim.easeOutCubic(after)) * r;
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(new Vector3f(g).add(0, 0.08f, 0)), sr * 0.85f, sr, ctx.seg(16, 10), 1, 0,
                    VfxVertexBuffer.withAlpha(light, fade));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, 0.6f, 0), 2.2f * r, col, fade);
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(g).add(0, 1, 0), 7 * r * (1 - after), 0, VfxVertexBuffer.withAlpha(WHITE, (1 - after) * (1 - after)));
    }

    // ------------------------------------------------------------------ Magic Convert
    private void alchemy(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float open = VfxAnim.circleOpen(Mth.clamp(age / inst.duration, 0, 1));
        float fade = life(inst, age, 0, 10);
        Vector3f c = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f face = new Vector3f(to).sub(c);
        if (face.lengthSquared() < 1e-4f) face.set(0, 1, 0);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f);
        VfxPose pose = VfxPose.facing(c, face);
        buf.plane(ALCHEMY, VfxBlend.ADD, pose.spin(age * 0.06f), 1.6f * p * open, VfxVertexBuffer.withAlpha(col, fade));
        buf.plane(ALCHEMY, VfxBlend.ADD, pose.spin(-age * 0.09f).lift(0.05f), 0.9f * p * open, VfxVertexBuffer.withAlpha(light, 0.8f * fade));
        // motes drawn in from all round, recombined into light at the centre
        int n = ctx.seg(14, 7);
        for (int i = 0; i < n; i++) {
            float ph = hash(inst.seed, i, 1) * 14f, lt = ((age + ph) % 14f) / 14f;
            float a = hash(inst.seed, i, 2) * Mth.TWO_PI + lt * 2;
            float rad = 2.6f * p * (1 - VfxAnim.easeInCubic(lt));
            Vector3f q = pose.point(Mth.cos(a) * rad, Mth.sin(a) * rad).add(new Vector3f(pose.normal()).mul(0.4f * (1 - lt)));
            buf.billboard(ctx, VfxTextures.MANA_MOTE, VfxBlend.ADD, q, 0.22f * p, 0, VfxVertexBuffer.withAlpha(i % 3 == 0 ? WHITE : light, fade * Mth.sin(lt * Mth.PI)));
        }
        VfxBloom.glow(ctx, buf, c, 0.8f * p * open, col, fade * (0.6f + 0.4f * Mth.sin(age * 0.5f)));
    }

    // ------------------------------------------------------------------ Violett Schirm
    private void curtain(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), h = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f along = new Vector3f(b).sub(a);
        float len = along.length();
        if (len < 0.01f) return;
        along.div(len);
        float drop = VfxAnim.easeOutCubic(Mth.clamp(age / 12f, 0, 1));
        float fade = life(inst, age, 0, 16);
        int col = inst.color, dark = ElementFx.mulRgb(col, 0.6f);
        // panels of liquid poison hanging down, swaying out of step
        int n = Math.max(2, Math.min(8, Math.round(len / 1.2f)));
        float pw = len / n;
        for (int k = 0; k < n; k++) {
            Vector3f mid = new Vector3f(a).lerp(b, (k + 0.5f) / n);
            float sway = Mth.sin(age * 0.08f + k * 1.3f) * 0.15f;
            Vector3f top = new Vector3f(mid).add(0, h, 0), bottom = new Vector3f(top).add(0, -h * drop, 0).add(new Vector3f(along).cross(0, 1, 0).mul(sway));
            Vector3f c = new Vector3f(top).lerp(bottom, 0.5f);
            Vector3f up = new Vector3f(top).sub(bottom);
            float hh = up.length() / 2;
            if (hh < 0.02f) continue;
            panel(buf, CURTAIN, VfxBlend.ALPHA, c, along, up.normalize(), pw * 0.55f, hh, VfxVertexBuffer.withAlpha(k % 2 == 0 ? col : dark, 0.85f * fade));
        }
        // the rail of mist along the top and the toxic haze at its foot
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {
            float f = (i + 0.5f) / m, lt = ((age * 0.02f) + hash(inst.seed, i, 1)) % 1f;
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, new Vector3f(a).lerp(b, f).add(0, 0.3f + lt * 0.8f, 0), (1.2f + lt) * Math.min(h, 3f) * 0.5f, i,
                    VfxVertexBuffer.withAlpha(dark, 0.6f * fade * (1 - lt)));
        }
        // drips falling off the hem
        int d = ctx.seg(8, 4);
        for (int i = 0; i < d; i++) {
            float f = hash(inst.seed, i, 2), lt = ((age + hash(inst.seed, i, 3) * 10) % 10f) / 10f;
            Vector3f q = new Vector3f(a).lerp(b, f).add(0, h * (1 - drop) - lt * lt * 0.8f, 0);
            buf.billboard(ctx, WATER_DROP, VfxBlend.ALPHA, q, 0.22f, 0, VfxVertexBuffer.withAlpha(col, fade * (1 - lt)));
        }
    }

    // ------------------------------------------------------------------ Basilisk's Breath
    private void breath(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 0.01f) return;
        dir.div(len);
        Vector3f sd = side(dir), up = new Vector3f(sd).cross(dir).normalize();
        float fade = life(inst, age, 0, 10);
        int col = inst.color, dark = ElementFx.mulRgb(col, 0.55f), light = VfxVertexBuffer.whiten(col, 0.35f);
        // billows rolling out along the cone, widening and thinning
        int n = ctx.seg(22, 10);
        for (int i = 0; i < n; i++) {
            float speed = 0.045f + 0.02f * hash(inst.seed, i, 1);
            float lt = (hash(inst.seed, i, 2) + age * speed) % 1f;
            if (age * speed < hash(inst.seed, i, 2) * 0.3f) continue;          // the first wave leaves the mouth in order
            float spread = 0.2f + 0.45f * lt;
            float ang = hash(inst.seed, i, 3) * Mth.TWO_PI, rad = spread * len * 0.45f * hash(inst.seed, i, 4);
            Vector3f q = new Vector3f(dir).mul(len * lt).add(a).add(new Vector3f(sd).mul(Mth.cos(ang) * rad)).add(new Vector3f(up).mul(Mth.sin(ang) * rad));
            float size = (0.7f + 2.6f * lt) * p;
            buf.billboard(ctx, SMOKE, VfxBlend.ALPHA, q, size, age * 0.02f + i, VfxVertexBuffer.withAlpha(i % 3 == 0 ? light : i % 3 == 1 ? col : dark,
                    fade * Mth.clamp((1 - lt) * 2.2f, 0, 1) * 0.85f));
        }
        // venom glints in the mist and a glow at the mouth
        int g = ctx.seg(8, 4);
        for (int i = 0; i < g; i++) {
            float lt = (hash(inst.seed, i, 5) + age * 0.06f) % 1f;
            Vector3f q = new Vector3f(dir).mul(len * lt).add(a).add(new Vector3f(sd).mul((hash(inst.seed, i, 6) - 0.5f) * len * 0.4f * lt));
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.2f * p, age * 0.2f, VfxVertexBuffer.withAlpha(light, fade * (1 - lt)));
        }
        VfxBloom.glow(ctx, buf, a, 0.6f * p, col, 0.7f * fade);
    }

    // ------------------------------------------------------------------ Fallen Angel Gate
    private void portal(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), r = inst.power;
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        float fade = life(inst, age, 0, 12);
        Vector3f c = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f face = new Vector3f(to.x - c.x, 0, to.z - c.z);
        if (face.lengthSquared() < 1e-4f) face.set(new Vector3f(ctx.camRight).cross(0, 1, 0));
        face.normalize();
        Vector3f right = new Vector3f(face).cross(0, 1, 0).normalize(), up = new Vector3f(0, 1, 0);
        Vector3f centre = new Vector3f(c).add(0, 1.3f * r, 0);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.55f);
        // the gate: an upright oval of swirling space, twice as tall as wide, both faces
        float hw = 0.9f * r * open, hh = 1.35f * r * open;
        VfxPose pose = new VfxPose(centre, right, up, face);
        float spin = age * 0.04f;
        Vector3f rr = new Vector3f(right).mul(Mth.cos(spin)).add(new Vector3f(up).mul(Mth.sin(spin) * 0.15f));
        panel(buf, PORTAL, VfxBlend.ALPHA, centre, rr, up, hw, hh, VfxVertexBuffer.withAlpha(ElementFx.mulRgb(col, 0.45f), 0.92f * fade));
        panel(buf, PORTAL, VfxBlend.ADD, new Vector3f(centre).add(new Vector3f(face).mul(0.02f)), right, up, hw, hh, VfxVertexBuffer.withAlpha(light, 0.8f * fade));
        // the rim: a ring of light hugging the oval, spinning
        int seg = ctx.seg(16, 10);
        for (int i = 0; i < seg; i++) {
            float a0 = Mth.TWO_PI * i / seg + spin * 3, a1 = Mth.TWO_PI * (i + 1) / seg + spin * 3;
            Vector3f p0 = pose.point(Mth.cos(a0) * hw * 0.98f, Mth.sin(a0) * hh * 0.98f), p1 = pose.point(Mth.cos(a1) * hw * 0.98f, Mth.sin(a1) * hh * 0.98f);
            streak(buf, ctx, WIND_STREAK, VfxBlend.ADD, p0, p1, 0.22f * r, VfxVertexBuffer.withAlpha(light, fade));
        }
        // shards of space orbiting it and drifting in
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {
            float a = hash(inst.seed, i, 1) * Mth.TWO_PI + age * 0.05f, d = (1.2f + 0.5f * Mth.sin(age * 0.1f + i)) ;
            Vector3f q = pose.point(Mth.cos(a) * hw * d, Mth.sin(a) * hh * d).add(new Vector3f(face).mul((hash(inst.seed, i, 2) - 0.5f) * 0.6f));
            buf.billboard(ctx, VfxTextures.SHARD, VfxBlend.ADD, q, 0.35f * r, age * 0.1f + i, VfxVertexBuffer.withAlpha(light, 0.8f * fade));
        }
        VfxBloom.glow(ctx, buf, centre, 1.0f * r * open, col, 0.6f * fade);
    }
}
