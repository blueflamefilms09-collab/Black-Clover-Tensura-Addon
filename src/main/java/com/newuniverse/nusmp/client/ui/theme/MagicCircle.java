package com.newuniverse.nusmp.client.ui.theme;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

/**
 * The anime magic circle: a stack of prebuilt white-on-transparent layers (tools/gen_ui_theme_textures.py) drawn rotated about one centre at
 * different speeds and directions, tinted by one colour, under a pulsing additive glow:
 * <pre>
 *   glow                (additive bloom, breathes)
 *   outer     0.30 deg/tick   double ring, 72 ticks, a band of 24 runes
 *   sweep     two broken arcs, one outside the ring and one just inside it, turning in opposite directions, the fast layer
 *   star      -0.55           hexagram with nodes
 *   poly      +0.85           octagon, eight-pointed star, dashed ring
 *   inner     -1.30           arc dashes and diamonds, the centre
 * </pre>
 * Cheap by construction: at most eight textured quads, one reused rotation quaternion, no allocation of its own. The angle of every layer
 * is a function of the clock only ({@link UiMath#angle}), so it is smooth at any frame rate and honours the partial tick when the caller
 * passes {@code ticks + partial}. The texture tier (256 / 128 / 64 px) is chosen from the drawn size.
 * <p>
 * Only for the anime look: callers check {@link UiTheme#anime()}.
 */
public final class MagicCircle {
    private MagicCircle() {}

    /** Layer speeds in degrees per tick (negative turns the other way) and the glow strength. Preallocated; never create one per frame. */
    public static final class Style {
        final float outer, star, poly, inner, sweep, glow;

        public Style(float outer, float star, float poly, float inner, float sweep, float glow) {
            this.outer = outer; this.star = star; this.poly = poly; this.inner = inner; this.sweep = sweep; this.glow = glow;
        }
    }

    public static final Style STANDARD = new Style(0.30f, -0.55f, 0.85f, -1.30f, 2.2f, 0.45f);
    /** For backgrounds behind text: slower and dimmer glow. */
    public static final Style CALM = new Style(0.12f, -0.22f, 0.34f, -0.52f, 0.9f, 0.25f);
    /** For a moment of casting: everything turns fast. */
    public static final Style FAST = new Style(0.9f, -1.6f, 2.3f, -3.4f, 5.5f, 0.7f);

    private static final String DIR = "textures/gui/theme/";
    private static final ResourceLocation[] OUTER = tiers("circle_outer"), STAR = tiers("circle_star"), POLY = tiers("circle_poly"),
            INNER = tiers("circle_inner"), ARCS = tiers("circle_arcs");
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("nusmp", DIR + "circle_glow.png");
    private static final Quaternionf Q = new Quaternionf();

    private static ResourceLocation[] tiers(String base) {
        return new ResourceLocation[]{ResourceLocation.fromNamespaceAndPath("nusmp", DIR + base + ".png"),
                ResourceLocation.fromNamespaceAndPath("nusmp", DIR + base + "_128.png"),
                ResourceLocation.fromNamespaceAndPath("nusmp", DIR + base + "_64.png")};
    }

    /** 0 for the 256 px tier, 1 for 128, 2 for 64, by the diameter that layer is drawn at. */
    static int tier(float diameter) { return diameter >= 170f ? 0 : diameter >= 84f ? 1 : 2; }

    /** Standard circle at (cx, cy), 'radius' in GUI pixels (the outer ring), tinted 'rgb', at 'alpha' (0..1). */
    public static void draw(GuiGraphics g, float cx, float cy, float radius, float time, int rgb, float alpha) {
        draw(g, cx, cy, radius, time, rgb, alpha, STANDARD, 1f);
    }

    /**
     * @param build 0..1: how far the circle has built up (layers scale up and fade in one after another); 1 = complete. Values above 1
     *              (an overshoot) simply enlarge it slightly.
     */
    public static void draw(GuiGraphics g, float cx, float cy, float radius, float time, int rgb, float alpha, Style st, float build) {
        if (alpha <= 0.01f || radius < 6f || build <= 0f) return;
        float r = ((rgb >> 16) & 0xFF) / 255f, gr = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float hr = r + (1f - r) * 0.55f, hg = gr + (1f - gr) * 0.55f, hb = b + (1f - b) * 0.55f;      // a lighter tint for the inner layers
        float pulse = UiMath.pulse(time, 90f, 0f);

        // the additive glow first, under the lines
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        float gl = alpha * st.glow * (0.65f + 0.35f * pulse) * Math.min(1f, build);
        layer(g, GLOW, cx, cy, radius * 1.32f * Math.min(build, 1.1f), 0f, r, gr, b, gl);
        RenderSystem.defaultBlendFunc();

        float b0 = stage(build, 0f), b1 = stage(build, 0.12f), b2 = stage(build, 0.24f), b3 = stage(build, 0.36f), b4 = stage(build, 0.48f);
        float k = build > 1f ? build : 1f;
        float d = radius * 2f;
        layer(g, OUTER[tier(d)], cx, cy, radius * k * grow(b0), UiMath.angle(time, st.outer, 0f), r, gr, b, alpha * b0);
        layer(g, ARCS[tier(d * 1.12f)], cx, cy, radius * 1.12f * k * grow(b4), UiMath.angle(time, st.sweep, 40f), hr, hg, hb, alpha * 0.8f * b4);
        layer(g, ARCS[tier(d * 0.66f)], cx, cy, radius * 0.66f * k * grow(b4), UiMath.angle(time, -st.sweep * 0.7f, 200f), hr, hg, hb, alpha * 0.7f * b4);
        layer(g, STAR[tier(d * 0.64f)], cx, cy, radius * 0.64f * k * grow(b1), UiMath.angle(time, st.star, 15f), r, gr, b, alpha * b1);
        layer(g, POLY[tier(d * 0.42f)], cx, cy, radius * 0.42f * k * grow(b2), UiMath.angle(time, st.poly, 0f), hr, hg, hb, alpha * 0.9f * b2);
        layer(g, INNER[tier(d * 0.22f)], cx, cy, radius * 0.22f * k * grow(b3), UiMath.angle(time, st.inner, 0f), hr, hg, hb, alpha * (0.75f + 0.25f * pulse) * b3);
        g.setColor(1f, 1f, 1f, 1f);
    }

    /** The staggered progress of one layer: starts at 'from' of the build and takes a third of it. */
    private static float stage(float build, float from) { return UiMath.clamp01((Math.min(build, 1f) - from) / 0.34f); }

    /** Layers grow from 70% to full size as they appear. */
    private static float grow(float stage) { return 0.7f + 0.3f * UiMath.easeOutCubic(stage); }

    private static void layer(GuiGraphics g, ResourceLocation tex, float cx, float cy, float half, float deg, float r, float gr, float b, float a) {
        if (a <= 0.004f || half < 2f) return;
        int hs = Math.max(1, Math.round(half));
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(cx, cy, 0f);
        Q.identity().rotateZ((float) Math.toRadians(deg));
        ps.mulPose(Q);
        g.setColor(r, gr, b, Math.min(1f, a));
        g.blit(tex, -hs, -hs, hs * 2, hs * 2, 0f, 0f, 256, 256, 256, 256);
        ps.popPose();
    }
}
