package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.prop.PropDraw;
import com.newuniverse.nusmp.client.prop.TreeDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1.93.0 nature-magic rebuild: the loose sprites each temporary armour adds after its pieces (all additive or translucent, drawn last, camera-facing through {@link TreeDraw}): motes or leaves
 * that rise (or fall) round the body, a turning sigil on the ground, a halo behind the back. Layer space as in SealAura: y points DOWN, the feet are at y = 1.5, the back is +z.
 * Everything fades with the shatter of the set ({@link EquipAnimation}) and costs at most a few dozen quads.
 */
public final class TreeAuraFx {
    private TreeAuraFx() {}

    private static final int FB = PropDraw.FULL_BRIGHT;

    private static float frac(float x) { return x - (float) Math.floor(x); }

    /** The fade the sprites follow: the last piece of the set (delay 6, so they leave with the middle of it). */
    public static float k(AuraContext c) { return EquipAnimation.alpha(EquipAnimation.assembled(c, 6f)); }

    /**
     * n sprites that rise (fall when rise is negative) in a column of the given radius round the body, each on its own phase, fading in and out; colours alternate a and b.
     *
     * @param size half size of one sprite in blocks
     * @param rise blocks per 100 ticks (negative: falling)
     */
    public static void sprites(AuraContext c, ResourceLocation tex, boolean additive, int a, int b, int n, float radius, float rise, float size, float spin) {
        float k = k(c);
        if (k <= 0.01f) return;
        PoseStack pose = c.pose();
        TreeDraw.camera(pose);
        VertexConsumer vc = c.buffers().getBuffer(additive ? AuraRender.additive(tex) : AuraRender.translucent(tex));
        float age = c.age();
        boolean fall = rise < 0f;
        for (int i = 0; i < n; i++) {
            float ph = frac(age * Math.abs(rise) * 0.0105f + i * 0.618f + 0.17f * (i % 3));
            float ang = age * 0.035f * (i % 2 == 0 ? 1f : -1f) + i * 2.399f;
            float r = radius * (0.7f + 0.3f * Mth.sin(i * 1.7f + age * 0.03f));
            float y = fall ? -0.7f + ph * 2.1f : 1.4f - ph * 2.1f;
            float fade = Mth.sin(ph * Mth.PI);
            TreeDraw.glow(pose, vc, Mth.cos(ang) * r, y, Mth.sin(ang) * r * 0.85f, size * (0.6f + 0.6f * fade), age * spin + i * 1.3f,
                    TreeDraw.argb(fade * 0.9f * k, i % 2 == 0 ? a : b), FB);
        }
    }

    /** A sigil lying on the ground at the feet, turning, with a second counter-turning copy at 60 percent size. */
    public static void sigil(AuraContext c, ResourceLocation tex, int rgb, float radius, float alpha) {
        float k = k(c);
        if (k <= 0.01f) return;
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(tex));
        float grow = 0.55f + 0.45f * Mth.clamp(c.t() / 10f, 0f, 1f);
        pose.pushPose();
        pose.translate(0f, 1.47f, 0f);
        TreeDraw.flat(pose, vc, radius * grow, c.age() * 1.1f, TreeDraw.argb(alpha * k, rgb), FB);
        TreeDraw.flat(pose, vc, radius * 0.6f * grow, -c.age() * 1.9f, TreeDraw.argb(alpha * 0.8f * k, TreeDraw.mix(rgb, 0xFFFFFF, 0.4f)), FB);
        pose.popPose();
    }

    /** An upright halo behind the back (both sides drawn), turned slowly. */
    public static void halo(AuraContext c, ResourceLocation tex, int rgb, float half, float cy, float alpha) {
        float k = k(c);
        if (k <= 0.01f) return;
        PoseStack pose = c.pose();
        VertexConsumer vc = c.buffers().getBuffer(AuraRender.additive(tex));
        float rot = c.age() * 0.03f, cs = Mth.cos(rot) * half, sn = Mth.sin(rot) * half;
        int col = TreeDraw.argb(alpha * k, rgb);
        TreeDraw.plane(pose, vc, 0f, cy, 0.46f, cs, sn, 0f, -sn, cs, 0f, false, col, FB);
        TreeDraw.plane(pose, vc, 0f, cy, 0.46f, cs, sn, 0f, -sn, cs, 0f, true, col, FB);
    }

    /** The sprites of Tree Magic: sap motes rising in the heartwood colours, leaves falling green, a ring of heartwood on the ground. */
    public static void sap(AuraContext c, int sapRgb, int leafRgb) {
        sprites(c, AuraRender.tex("particle/tree_fx_mote"), true, sapRgb, TreeDraw.mix(sapRgb, 0xFFFFFF, 0.5f), 9, 0.62f, 55f, 0.07f, 0.1f);
        sprites(c, AuraRender.tex("particle/tree_fx_leaf"), false, leafRgb, TreeDraw.mix(leafRgb, 0x2E6B1E, 0.5f), 7, 0.7f, -30f, 0.09f, 0.06f);
        sigil(c, AuraRender.tex("particle/tree_fx_ring"), sapRgb, 0.95f, 0.8f);
    }
}
