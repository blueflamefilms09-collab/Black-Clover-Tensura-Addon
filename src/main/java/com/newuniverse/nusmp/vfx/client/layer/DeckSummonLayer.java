package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.client.relics.deck.DeckSummonOptions;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.AbstractVfxLayer;
import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxBloom;
import com.newuniverse.nusmp.vfx.client.VfxInstance;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxTextures;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import com.newuniverse.nusmp.vfx.deck.DeckSummonGeometry;
import com.newuniverse.nusmp.vfx.deck.DeckSummonTimeline;
import com.newuniverse.nusmp.vfx.deck.DeckSummonTimeline.Phase;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * The card summon of the Deck of Many Things (1.110.0), drawn in real 3D from the shapes of the effect buffer: the card (a thin box with a back, a face and a gold rim) slides out of the deck
 * slot of the worn duel disk, short radial light streaks flash, the card is slapped on a card zone with a shockwave ring, it flips face up, a column of light in the colour of the card's
 * alignment rises, the card dissolves into sparks (the mod's own particles, through the client particle budget). Client only. The server sends the drawn card, its zone and its alignment
 * in the payload seed ({@link DeckSummonGeometry#pack}); the clock is {@link DeckSummonTimeline}. No flat picture of a card is ever drawn: the face is a cell of an atlas generated from the
 * card's alignment and motif (tools/deck_summon/gen_deck_summon.py) on a box with thickness. Switched off by {@code deck.summonEffect}.
 */
public class DeckSummonLayer extends AbstractVfxLayer {
    private static final ResourceLocation ATLAS = VfxTextures.DECK_SUMMON;
    private static final float CARD_W = 0.30f, CARD_H = 0.46f, CARD_T = 0.016f;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.DECK_SUMMON); }
    @Override public int defaultDuration(VfxShape s) { return DeckSummonTimeline.TOTAL; }
    @Override public int defaultColor(VfxShape s) { return 0xFFFFD870; }
    @Override public int vertexBudget(VfxShape s) { return 240; }

    /** u0, v0, u1, v1 of cell {@code i} of the 8 x 4 atlas, inset by a quarter texel. */
    private static float[] cell(int i) {
        float cu = 1f / 8f, cv = 1f / 4f, e = 0.25f / 512f;
        float u = (i % 8) * cu, v = (i / 8) * cv;
        return new float[]{u + e, v + e, u + cu - e, v + cv - e};
    }

    private static float bodyYaw(VfxInstance inst, VfxRenderContext ctx) {
        int id = inst.payload.followEntity();
        Entity e = id >= 0 && ctx.level != null ? ctx.level.getEntity(id) : null;
        if (e instanceof LivingEntity le) return Mth.rotLerp(ctx.partialTick, le.yBodyRotO, le.yBodyRot);
        return e == null ? 0f : e.getYRot();
    }

    /** World offset (from the feet) of the card at time t: the slot, then the hover point, then the zone. */
    static double[] cardOffset(float t, int zone, float yaw) {
        float s = DeckSummonTimeline.travel(t);
        float[] a, b;
        float k;
        if (s <= 1f) {
            a = DeckSummonGeometry.slot();
            b = DeckSummonGeometry.hover(zone);
            k = s;
        } else {
            a = DeckSummonGeometry.hover(zone);
            b = DeckSummonGeometry.zone(zone);
            k = Math.min(1f, s - 1f);
        }
        float[] p = {Mth.lerp(k, a[0], b[0]), Mth.lerp(k, a[1], b[1]), Mth.lerp(k, a[2], b[2])};
        return DeckSummonGeometry.world(p, yaw);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (!DeckSummonOptions.enabled()) return;
        float t = inst.ageTicks(ctx.partialTick);
        if (DeckSummonTimeline.finished(t)) return;
        long seed = inst.seed;
        int card = Math.floorMod(DeckSummonGeometry.cardOf(seed), 22), zone = DeckSummonGeometry.zoneOf(seed), mood = DeckSummonGeometry.moodOf(seed);
        float yaw = bodyYaw(inst, ctx);
        Vec3 feet = inst.from(ctx);
        double yr = Math.toRadians(yaw);
        Vector3f left = new Vector3f((float) Math.cos(yr), 0f, (float) Math.sin(yr)), fwd = new Vector3f((float) -Math.sin(yr), 0f, (float) Math.cos(yr)), up = new Vector3f(0f, 1f, 0f);
        double[] off = cardOffset(t, zone, yaw);
        Vector3f c = ctx.rel(feet.add(off[0], off[1], off[2]));
        int glow = 0xFF000000 | DeckSummonGeometry.alignmentColour(mood), rim = 0xFF000000 | DeckSummonGeometry.alignmentRim(mood);
        float alpha = DeckSummonTimeline.cardAlpha(t);

        // ---- the card: a thin box, the back outward at first, the face outward after the flip
        float sc = DeckSummonTimeline.cardScale(t);
        float hw = CARD_W * 0.5f * sc, hh = CARD_H * 0.5f * sc, ht = CARD_T * sc;
        double th = Math.toRadians(DeckSummonTimeline.flipDegrees(t));
        float cs = (float) Math.cos(th), sn = (float) Math.sin(th);
        Vector3f a = new Vector3f(fwd).mul(cs).add(new Vector3f(left).mul(sn));          // the width axis, turned about the long axis
        Vector3f n = new Vector3f(left).mul(cs).sub(new Vector3f(fwd).mul(sn));          // the normal: the back outward at 0, the face outward at 180
        if (alpha > 0.01f) {
            int white = VfxVertexBuffer.withAlpha(0xFFFFFFFF, alpha);
            slab(buf, c, a, up, n, hw, hh, ht, cell(22), cell(card), cell(23), white);
            VfxBloom.glow(ctx, buf, c, 0.42f * sc, rim, 0.35f * alpha * (0.4f + 0.6f * DeckSummonTimeline.progress(Phase.FLIP, t)));
        }

        // ---- the flash: short radial light streaks round the card (additive, tinted by the rim colour)
        float fl = DeckSummonTimeline.flash(t);
        if (fl > 0.01f) {
            float[] st = cell(24);
            int streaks = ctx.seg(10, 6);
            float r0 = 0.08f, r1 = 0.30f + 0.26f * fl;
            int col = VfxVertexBuffer.withAlpha(rim, fl);
            for (int i = 0; i < streaks; i++) {
                float ang = Mth.TWO_PI * i / streaks + 0.3f * i;
                float cx = Mth.cos(ang), cy = Mth.sin(ang);
                Vector3f dir = new Vector3f(ctx.camRight).mul(cx).add(new Vector3f(ctx.camUp).mul(cy));
                Vector3f side = new Vector3f(ctx.camRight).mul(-cy).add(new Vector3f(ctx.camUp).mul(cx)).mul(0.018f);
                Vector3f root = new Vector3f(c).add(new Vector3f(dir).mul(r0));
                Vector3f tip = new Vector3f(c).add(new Vector3f(dir).mul(r1 * (0.7f + 0.15f * (i % 3))));
                buf.quad(ATLAS, VfxBlend.ADD, new Vector3f(tip).sub(side), new Vector3f(tip).add(side), new Vector3f(root).add(new Vector3f(side).mul(2.4f)), new Vector3f(root).sub(new Vector3f(side).mul(2.4f)),
                        st[0], st[1], st[2], st[3], col, col);
            }
            VfxBloom.glow(ctx, buf, c, 0.55f, rim, 0.8f * fl);
        }

        // ---- the shockwave ring on the zone (in the plane of the card)
        float sw = DeckSummonTimeline.shockwaveRadius(t), swa = DeckSummonTimeline.shockwaveAlpha(t);
        if (swa > 0.01f && sw > 0.01f) {
            float[] rc = cell(25);
            int col = VfxVertexBuffer.withAlpha(glow, swa);
            Vector3f f = new Vector3f(fwd).mul(sw), u = new Vector3f(up).mul(sw);
            buf.quad(ATLAS, VfxBlend.ADD, new Vector3f(c).sub(f).sub(u), new Vector3f(c).add(f).sub(u), new Vector3f(c).add(f).add(u), new Vector3f(c).sub(f).add(u), rc[0], rc[3], rc[2], rc[1], col, col);
        }

        // ---- the hologram column of light rising from the card (two crossed quads, scan lines in the texture)
        float ch = DeckSummonTimeline.columnHeight(t), ca = DeckSummonTimeline.columnAlpha(t);
        if (ca > 0.01f && ch > 0.01f) {
            float[] cc = cell(26);
            int col = VfxVertexBuffer.withAlpha(glow, ca * 0.9f);
            Vector3f top = new Vector3f(up).mul(ch);
            for (Vector3f axis : new Vector3f[]{fwd, left}) {
                Vector3f s = new Vector3f(axis).mul(0.20f);
                buf.quad(ATLAS, VfxBlend.ADD, new Vector3f(c).sub(s), new Vector3f(c).add(s), new Vector3f(c).add(s).add(top), new Vector3f(c).sub(s).add(top), cc[0], cc[1], cc[2], cc[3], col, col);
            }
            VfxBloom.glow(ctx, buf, c, 0.40f, glow, 0.5f * ca);
        }
    }

    /** A thin box: the back at +n, the face at -n, four rim strips between them. */
    private static void slab(VfxVertexBuffer buf, Vector3f c, Vector3f a, Vector3f up, Vector3f n, float hw, float hh, float ht, float[] back, float[] face, float[] rim, int col) {
        Vector3f ax = new Vector3f(a).mul(hw), uy = new Vector3f(up).mul(hh), nz = new Vector3f(n).mul(ht);
        Vector3f cb = new Vector3f(c).add(nz), cf = new Vector3f(c).sub(nz);
        buf.quad(ATLAS, VfxBlend.ALPHA, new Vector3f(cb).sub(ax).sub(uy), new Vector3f(cb).add(ax).sub(uy), new Vector3f(cb).add(ax).add(uy), new Vector3f(cb).sub(ax).add(uy), back[0], back[3], back[2], back[1], col, col);
        buf.quad(ATLAS, VfxBlend.ALPHA, new Vector3f(cf).add(ax).sub(uy), new Vector3f(cf).sub(ax).sub(uy), new Vector3f(cf).sub(ax).add(uy), new Vector3f(cf).add(ax).add(uy), face[0], face[3], face[2], face[1], col, col);
        float u0 = (rim[0] + rim[2]) * 0.5f, v0 = (rim[1] + rim[3]) * 0.5f;
        Vector3f[] e0 = {new Vector3f(c).add(ax).sub(uy), new Vector3f(c).add(ax).add(uy), new Vector3f(c).sub(ax).add(uy), new Vector3f(c).sub(ax).sub(uy)};
        for (int i = 0; i < 4; i++) {
            Vector3f q0 = e0[i], q1 = e0[(i + 1) % 4];
            buf.quad(ATLAS, VfxBlend.ALPHA, new Vector3f(q0).add(nz), new Vector3f(q1).add(nz), new Vector3f(q1).sub(nz), new Vector3f(q0).sub(nz), u0, v0, u0 + 0.0005f, v0 + 0.0005f, col, col);
        }
    }

    @Override
    public void onTick(VfxInstance inst) {
        if (!DeckSummonOptions.enabled()) return;
        int n = DeckSummonTimeline.sparks(inst.age());
        if (n <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = inst.payload.followEntity() >= 0 ? mc.level.getEntity(inst.payload.followEntity()) : null;
        if (e == null) return;
        long seed = inst.seed;
        float yaw = e instanceof LivingEntity le ? le.yBodyRot : e.getYRot();
        double[] off = DeckSummonGeometry.world(DeckSummonGeometry.zone(DeckSummonGeometry.zoneOf(seed)), yaw);
        FxTone tone = FxTone.of(DeckSummonGeometry.moodOf(seed) == 2);
        Fx.burst(mc.level, FxThemes.RELIC_WAVE_30, e.position().add(off[0], off[1], off[2]), tone, n, 0.10);       // the mod's own particles; the client particle budget decides how many appear
    }
}
