package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.ui.theme.MagicCircle;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Why: the drawing pieces of the Multiverse Hub (docs/ui_hub.md), all stage-space geometry made of {@code GuiGraphics.fill} and the mod's own {@link MagicCircle} textures (no new texture, nothing copied from any
 * other mod): dotted rings, spokes, the octagon nodes with their flare and lock, the floating motes, the click ripple and the open sweep. Stateless statics so the screen owns the state; nothing here allocates per frame
 * (the unit circles are tables built once, rotation is two multiplications per dot). Budget: a dotted ring is at most {@link #RING_A_N} fills, a node about 25 fills, a spoke about 6, the motes
 * {@link HubMotes#COUNT}, a ripple {@link #RING_B_N}; the textured quads are the {@link MagicCircle}s only (at most 8 each), see docs/ui_hub.md for the totals.
 */
final class HubFx {
    private HubFx() {}

    static final int RING_A_N = 72, RING_B_N = 48;
    private static final float[] AX = new float[RING_A_N], AY = new float[RING_A_N], BX = new float[RING_B_N], BY = new float[RING_B_N];

    static {
        for (int i = 0; i < RING_A_N; i++) {
            AX[i] = (float) Math.cos(i * 2 * Math.PI / RING_A_N);
            AY[i] = (float) Math.sin(i * 2 * Math.PI / RING_A_N);
        }
        for (int i = 0; i < RING_B_N; i++) {
            BX[i] = (float) Math.cos(i * 2 * Math.PI / RING_B_N);
            BY[i] = (float) Math.sin(i * 2 * Math.PI / RING_B_N);
        }
    }

    // ---------------------------------------------------------------- rings and spokes
    /** The dotted ring of 72 dots (every sixth one bigger), turned by {@code rot} radians; {@code reveal} 0..1 draws only the first part of it (the intro's expansion). */
    static void ringA(GuiGraphics g, float cx, float cy, float r, float rot, int rgb, float alpha, float reveal) { ring(g, AX, AY, RING_A_N, cx, cy, r, rot, rgb, alpha, reveal, 6); }

    /** The same with 48 dots (the inner ring and the click ripple). */
    static void ringB(GuiGraphics g, float cx, float cy, float r, float rot, int rgb, float alpha, float reveal) { ring(g, BX, BY, RING_B_N, cx, cy, r, rot, rgb, alpha, reveal, 4); }

    private static void ring(GuiGraphics g, float[] ux, float[] uy, int n, float cx, float cy, float r, float rot, int rgb, float alpha, float reveal, int bigEvery) {
        if (alpha <= 0.01f || reveal <= 0f || r < 3f) return;
        float cr = (float) Math.cos(rot), sr = (float) Math.sin(rot);
        int shown = Math.min(n, (int) Math.ceil(n * Math.min(1f, reveal)));
        int soft = UiMath.withAlpha(rgb, 0.5f * alpha), strong = UiMath.withAlpha(rgb, Math.min(1f, 0.95f * alpha));
        for (int i = 0; i < shown; i++) {
            float x = cx + r * (ux[i] * cr - uy[i] * sr), y = cy + r * (ux[i] * sr + uy[i] * cr);
            boolean big = i % bigEvery == 0;
            int s = big ? 3 : 2, px = Math.round(x - s / 2f), py = Math.round(y - s / 2f);
            g.fill(px, py, px + s, py + s, big ? strong : soft);
        }
    }

    /** A dotted line from (x0, y0) to (x1, y1), a dot every 5 units, the first {@code reveal} of it (0..1). */
    static void spoke(GuiGraphics g, float x0, float y0, float x1, float y1, int rgb, float alpha, float reveal) {
        if (alpha <= 0.01f || reveal <= 0f) return;
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy);
        int n = Math.max(1, (int) (len / 5f)), shown = Math.min(n, (int) Math.ceil(n * Math.min(1f, reveal)));
        int c = UiMath.withAlpha(rgb, 0.6f * alpha);
        for (int i = 1; i <= shown; i++) {
            float t = i / (float) (n + 1);
            int px = Math.round(x0 + dx * t) - 1, py = Math.round(y0 + dy * t) - 1;
            g.fill(px, py, px + 2, py + 2, c);
        }
    }

    // ---------------------------------------------------------------- circles
    /** The big quiet circle behind the ring (one {@link MagicCircle}, at most 8 textured quads). */
    static void backdrop(GuiGraphics g, float cx, float cy, float radius, float now, float alpha, float build) {
        MagicCircle.draw(g, cx, cy, radius, now, UiTheme.CYAN, 0.2f * alpha, MagicCircle.CALM, build);
    }

    /** The core circle (one {@link MagicCircle}). */
    static void core(GuiGraphics g, float cx, float cy, float radius, float now, int rgb, float alpha, float build) {
        MagicCircle.draw(g, cx, cy, radius, now, rgb, 0.85f * alpha, MagicCircle.STANDARD, build);
    }

    /** The flare round a hovered node (one {@link MagicCircle}); drawn only while the hover is up. */
    static void flare(GuiGraphics g, float cx, float cy, float radius, float now, int rgb, float hover, float alpha) {
        if (hover > 0.05f) MagicCircle.draw(g, cx, cy, radius, now, rgb, 0.9f * hover * alpha, MagicCircle.FAST, 1f);
    }

    // ---------------------------------------------------------------- nodes
    /** An octagon filled with {@code argb}, from three strips that never overlap (so a translucent colour stays even). */
    static void octagon(GuiGraphics g, int cx, int cy, int r, int argb) {
        int c = Math.max(2, r / 3), x0 = cx - r, y0 = cy - r, x1 = cx + r, y1 = cy + r;
        g.fill(x0 + c, y0, x1 - c, y0 + c, argb);
        g.fill(x0, y0 + c, x1, y1 - c, argb);
        g.fill(x0 + c, y1 - c, x1 - c, y1, argb);
    }

    /** The thin edge of that octagon. */
    static void octagonEdge(GuiGraphics g, int cx, int cy, int r, int argb) {
        int c = Math.max(2, r / 3), x0 = cx - r, y0 = cy - r, x1 = cx + r, y1 = cy + r;
        g.fill(x0 + c, y0, x1 - c, y0 + 1, argb);
        g.fill(x0 + c, y1 - 1, x1 - c, y1, argb);
        g.fill(x0, y0 + c, x0 + 1, y1 - c, argb);
        g.fill(x1 - 1, y0 + c, x1, y1 - c, argb);
        for (int k = 0; k < c; k += 2) {                                               // the four slanted corners, a step every two units
            g.fill(x0 + c - k - 2, y0 + k, x0 + c - k, y0 + k + 2, argb);
            g.fill(x1 - c + k, y0 + k, x1 - c + k + 2, y0 + k + 2, argb);
            g.fill(x0 + c - k - 2, y1 - k - 2, x0 + c - k, y1 - k, argb);
            g.fill(x1 - c + k, y1 - k - 2, x1 - c + k + 2, y1 - k, argb);
        }
    }

    /**
     * One node: an octagon in the tab's colour with its two-letter mark, brighter with the {@code lit} intro progress and the {@code hover} lerp, a bright edge when it is the open tab, a lock over the mark when
     * its feature is locked. {@code alpha} fades the whole node. About 25 fills; the flare is drawn separately ({@link #flare}).
     */
    static void node(GuiGraphics g, Font font, int cx, int cy, int r, HubNodes.Tab tab, float lit, float hover, boolean active, boolean locked, float alpha) {
        float vis = UiMath.clamp01(lit);
        if (vis <= 0.02f || alpha <= 0.02f) {
            octagonEdge(g, cx, cy, r, UiMath.withAlpha(tab.rgb, 0.18f * alpha));          // not lit yet: only a faint outline waits on the ring
            return;
        }
        float dim = locked ? 0.55f : 1f;
        int body = UiMath.withAlpha(UiMath.mix(0xFF0A1024, 0xFF000000 | tab.rgb, 0.12f + 0.22f * hover + (active ? 0.16f : 0f)), (0.55f + 0.35f * vis) * alpha);
        octagon(g, cx, cy, r, body);
        octagonEdge(g, cx, cy, r, UiMath.withAlpha(tab.rgb, Math.min(1f, (0.4f + 0.45f * vis + 0.25f * hover + (active ? 0.3f : 0f)) * dim * alpha)));
        if (active) octagonEdge(g, cx, cy, r + 2, UiMath.withAlpha(0xFFFFFF, 0.35f * alpha));
        float ta = vis * alpha * dim;
        if (!locked && ta > 0.08f) g.drawCenteredString(font, tab.glyph, cx, cy - 4, UiMath.withAlpha(UiMath.mix(0xFF000000 | tab.rgb, 0xFFFFFFFF, 0.6f), ta));
        if (locked) lock(g, cx, cy, UiMath.withAlpha(tab.rgb, 0.85f * vis * alpha));
    }

    /** A small padlock centred on (cx, cy): a shackle and a body, 5 fills. */
    static void lock(GuiGraphics g, int cx, int cy, int argb) {
        g.fill(cx - 2, cy - 6, cx + 3, cy - 5, argb);
        g.fill(cx - 3, cy - 5, cx - 2, cy - 1, argb);
        g.fill(cx + 3, cy - 5, cx + 4, cy - 1, argb);
        g.fill(cx - 4, cy - 1, cx + 5, cy + 5, argb);
        g.fill(cx, cy + 1, cx + 1, cy + 3, 0xCC0A1024);
    }

    // ---------------------------------------------------------------- ambient
    /** The motes: {@link HubMotes#COUNT} fills, each twinkling. */
    static void motes(GuiGraphics g, HubMotes m, float now, int rgb, float alpha) {
        if (alpha <= 0.01f) return;
        for (int i = 0; i < HubMotes.COUNT; i++) {
            int a = Math.round(255f * Math.min(1f, (0.12f + 0.5f * m.twinkle(i, now)) * alpha)), px = Math.round(m.x[i]), py = Math.round(m.y[i]), s = m.size[i];
            g.fill(px, py, px + s, py + s, (a << 24) | (rgb & 0xFFFFFF));
        }
    }

    /** The open sweep: a soft band falling down the stage, {@code p} 0..1 (see {@link HubTimeline#sweep}). */
    static void sweep(GuiGraphics g, float p, int rgb) {
        if (p < 0f) return;
        int y = Math.round(p * HubLayout.STAGE_H);
        g.fillGradient(0, y - 14, HubLayout.STAGE_W, y, UiMath.withAlpha(rgb, 0f), UiMath.withAlpha(rgb, 0.16f * (1f - p)));
        g.fill(0, y, HubLayout.STAGE_W, y + 1, UiMath.withAlpha(rgb, 0.3f * (1f - p)));
    }

    /** The click ripple: one dotted ring growing out of the click and fading, {@code p} 0..1 (see {@link HubTimeline#ripple}). */
    static void ripple(GuiGraphics g, float x, float y, float p, int rgb) {
        if (p < 0f) return;
        ringB(g, x, y, 3f + UiMath.easeOutCubic(p) * 44f, 0f, rgb, 1f - p, 1f);
    }
}
