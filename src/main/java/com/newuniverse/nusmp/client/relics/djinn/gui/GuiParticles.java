package com.newuniverse.nusmp.client.relics.djinn.gui;

import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.H;
import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.MED_CX;
import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.MED_CY;
import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.W;

/**
 * The drifting particles of the panel, one kind per magic: embers, lightning sparks and bolts, bubbles, wind streaks, petals, stone chips and dust,
 * music notes, twinkling stars. Every particle is a pure function of (index, clock): its life is a repeating fraction, its place a formula of
 * that fraction and a hash of its index, so nothing is stored and nothing is allocated.
 */
final class GuiParticles {
    private GuiParticles() {}

    /** How many particles a motif shows at an energy (never fewer than 4, never more than 2x the motif's count). */
    static int count(GuiMotif m, float energy) {
        return Math.max(4, Math.min(m.particles * 2, Math.round(m.particles * (0.45f + 0.55f * energy))));
    }

    static void paint(Ink k, GuiTheme th, float at, float en) {
        GuiPalette p = th.pal;
        int n = count(th.motif, en);
        for (int i = 0; i < n; i++) {
            float period = 34f + GuiAnim.rnd(i, 1) * 52f;                       // ticks of one life
            float u = GuiAnim.frac(at / period + GuiAnim.rnd(i, 2));
            float fade = Math.min(1f, u * 6f) * (1f - u);
            float rx = GuiAnim.rnd(i, 3), ry = GuiAnim.rnd(i, 4);
            int px = 3 + (int) (rx * (W - 6)), size = GuiAnim.rnd(i, 5) < 0.4f ? 2 : 1;
            switch (th.motif) {
                case EMBERS -> {
                    int x = px + Math.round(GuiAnim.sin(u * GuiAnim.TAU * 2f + i) * 3f), y = H - 3 - (int) (u * (H - 8));
                    k.box(x, y, size, size, GuiColor.argb(GuiColor.mix(p.glow(), p.accent(), u), fade * 0.95f));
                }
                case ARCS -> {
                    if (i % 3 == 0) {                                           // a short bolt that flashes up now and then
                        float f = GuiAnim.frac(at / (period * 0.5f) + GuiAnim.rnd(i, 2));
                        if (f < 0.1f) {
                            int x = px, y = 4 + (int) (ry * (H - 22));
                            for (int s = 0; s < 6; s++) {
                                int nx = x + (int) (GuiAnim.rnd(i * 17 + s + (int) (at / 2f), 8) * 7f) - 3, ny = y + 3;
                                k.line(x, y, nx, ny, GuiColor.argb(s % 2 == 0 ? 0xFFFFFF : p.glow(), 0.85f * (1f - f * 8f)));
                                x = nx;
                                y = ny;
                            }
                        }
                    } else {                                                    // sparks that jitter round the medallion
                        float a = GuiAnim.rnd(i, 9) * GuiAnim.TAU + at * 0.03f * (i % 2 == 0 ? 1 : -1);
                        float r = 24f + GuiAnim.rnd(i + (int) (at / 4f), 10) * 8f;
                        if (GuiAnim.rnd(i + (int) (at / 4f), 11) < 0.6f)
                            k.box(Math.round(MED_CX + GuiAnim.cos(a) * r), Math.round(MED_CY + GuiAnim.sin(a) * r), 1, 1, GuiColor.argb(p.glow(), 0.9f));
                    }
                }
                case RIPPLE -> {
                    int x = px + Math.round(GuiAnim.sin(u * GuiAnim.TAU * 1.5f + i) * 2f), y = H - 3 - (int) (u * (H - 8));
                    k.box(x, y, 2, 2, GuiColor.argb(p.accent(), fade * 0.55f));
                    k.dot(x, y, GuiColor.argb(0xFFFFFF, fade * 0.8f));
                    if (size == 2) k.frame(x - 1, y - 1, 4, 4, GuiColor.argb(p.glow(), fade * 0.4f));
                }
                case STREAKS -> {
                    int len = 5 + (int) (GuiAnim.rnd(i, 6) * 12f), x = (int) (u * (W + len * 2)) - len, y = 3 + (int) (ry * (H - 6));
                    if (i % 4 == 0) {                                           // a leaf, tilted
                        k.box(x, y, 2, 1, GuiColor.argb(p.accent(), 0.9f));
                        k.dot(x + 2, y + 1, GuiColor.argb(p.accent(), 0.8f));
                    } else {
                        k.fill(x, y, x + len, y + 1, GuiColor.argb(p.glow(), 0.18f + fade * 0.35f));
                        k.fill(x + len / 2, y, x + len, y + 1, GuiColor.argb(0xFFFFFF, 0.25f * fade));
                    }
                }
                case VINES -> {
                    int x = px + Math.round(GuiAnim.sin(u * GuiAnim.TAU + i) * 5f), y = 2 + (int) (u * (H - 6));
                    int c = GuiColor.argb(i % 2 == 0 ? GuiColor.mix(p.accent(), 0xFF9AC8, 0.6f) : p.accent(), fade * 0.95f);
                    k.box(x, y, 2, 1, c);
                    k.dot(x + 1, y + 1, c);
                }
                case CRACKS -> {
                    if (i % 3 == 0) {                                           // a chip falls
                        int x = px, y = 2 + (int) (u * u * (H - 6));
                        k.box(x, y, 2, 2, GuiColor.argb(GuiColor.mix(p.rim(), 0x606060, 0.6f), fade * 0.95f));
                    } else {                                                    // dust drifts up
                        int x = px + Math.round(GuiAnim.sin(u * GuiAnim.TAU + i) * 2f), y = H - 4 - (int) (u * (H - 10));
                        k.dot(x, y, GuiColor.argb(0xB8A890, fade * 0.45f));
                    }
                }
                case RINGS -> {
                    int x = 8 + (int) (rx * (W - 16)) + Math.round(GuiAnim.sin(u * GuiAnim.TAU + i) * 3f), y = H - 6 - (int) (u * (H - 14));
                    int c = GuiColor.argb(i % 2 == 0 ? p.glow() : p.accent(), fade * 0.95f);
                    k.box(x, y + 3, 2, 2, c);
                    k.fill(x + 1, y, x + 2, y + 4, c);
                    if (i % 3 != 0) k.dot(x + 2, y, c);
                }
                case RAYS -> {
                    float tw = GuiAnim.tri(at / period + GuiAnim.rnd(i, 2));
                    int x = px, y = 3 + (int) (ry * (H - 6));
                    k.star(x, y, tw > 0.66f ? 2 : 1, GuiColor.argb(tw > 0.66f ? 0xFFFFFF : p.glow(), tw * 0.9f));
                }
                default -> { }
            }
        }
    }
}
