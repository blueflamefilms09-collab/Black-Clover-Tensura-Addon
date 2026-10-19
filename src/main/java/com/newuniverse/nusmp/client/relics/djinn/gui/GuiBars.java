package com.newuniverse.nusmp.client.relics.djinn.gui;

/**
 * The bars (Magoi, assimilation, timer, lockout, cooldown), restyled per magic: the same fill and track for all, a moving surface and a head that
 * belong to the Djinn's magic (flame tongues, a lightning edge, waves, diagonal streaks, leaf notches, cracks, rhythm segments, a light sweep).
 */
final class GuiBars {
    private GuiBars() {}

    /**
     * @param frac 0..1 of the bar that is full
     * @param a    the fill's left colour, {@code b} its right colour (0xRRGGBB)
     * @param tall false for the thin lockout and cooldown bars (no surface effect)
     */
    static void draw(Ink k, GuiTheme th, int x, int y, int w, int h, float frac, int a, int b, float at, float en, boolean tall) {
        GuiPalette p = th.pal;
        int fw = GuiLayout.fillWidth(w, frac);
        if (!tall && fw <= 0) {                                                 // an idle thin bar is only a faint groove
            k.fill(x, y, x + w, y + h, GuiColor.argb(p.track(), 0.55f));
            return;
        }
        k.fill(x - 1, y - 1, x + w + 1, y + h + 1, GuiColor.argb(p.rim(), 0.30f));
        k.fill(x, y, x + w, y + h, GuiColor.argb(p.track(), 0.95f));
        if (fw <= 0) return;
        int steps = Math.min(fw, 12);
        for (int i = 0; i < steps; i++) {                                       // a horizontal gradient in twelve slabs
            int x1 = x + fw * i / steps, x2 = x + fw * (i + 1) / steps;
            int c = GuiColor.mix(a, b, (i + 0.5f) / steps);
            k.fill(x1, y, x2, y + h, GuiColor.argb(c, 1f));
            if (h >= 3) k.fill(x1, y, x2, y + 1, GuiColor.argb(GuiColor.mix(c, 0xFFFFFF, 0.3f), 1f));
        }
        if (!tall) return;
        int hi = GuiColor.argb(GuiColor.mix(b, 0xFFFFFF, 0.5f), 0.55f);
        int head = x + fw;
        switch (th.motif) {
            case EMBERS -> {
                for (int i = 0; i < fw; i += 4) {
                    int hh = 1 + (int) (2f * GuiAnim.noise(i * 0.4f + at * 0.4f, 5));
                    k.fill(x + i, y, x + i + 2, y + hh, hi);
                }
                k.dot(head, y - 1 - (int) (GuiAnim.noise(at * 0.5f, 6) * 2f), GuiColor.argb(p.glow(), 0.9f));
            }
            case ARCS -> {
                int pos = (int) (GuiAnim.frac(at * 0.02f) * fw);
                k.fill(x + pos, y, Math.min(x + fw, x + pos + 6), y + h, GuiColor.argb(0xFFFFFF, 0.45f));
                k.fill(head - 1, y - 1, head + 1, y + h + 1, GuiColor.argb(0xFFFFFF, 0.9f));
                int j = (int) (at / 2f);
                k.dot(head + 1, y - 1 + (int) (GuiAnim.rnd(j, 2) * (h + 2)), GuiColor.argb(p.glow(), 0.9f));
            }
            case RIPPLE -> {
                for (int i = 0; i < fw; i += 2) k.fill(x + i, y + 1 + Math.round(GuiAnim.sin(i * 0.5f + at * 0.3f)), x + i + 2, y + 2 + Math.round(GuiAnim.sin(i * 0.5f + at * 0.3f)), hi);
            }
            case STREAKS -> {
                int off = (int) (at * 0.7f);
                for (int i = 0; i < fw; i += 6) k.fill(x + (i + off) % fw, y, Math.min(x + fw, x + (i + off) % fw + 2), y + h, GuiColor.argb(0xFFFFFF, 0.22f));
            }
            case VINES -> {
                for (int i = 2; i < fw; i += 6) {
                    k.fill(x + i, y - 1, x + i + 2, y, GuiColor.argb(p.accent(), 0.9f));
                    k.fill(x + i + 3, y + h, x + i + 5, y + h + 1, GuiColor.argb(p.accent(), 0.9f));
                }
            }
            case CRACKS -> {
                for (int i = 5; i < fw; i += 9) {
                    int cx = x + i + (int) (GuiAnim.rnd(i, 3) * 3f);
                    k.fill(cx, y, cx + 1, y + h, GuiColor.argb(0x000000, 0.55f));
                    k.dot(cx + 1, y + (i % 2), GuiColor.argb(p.glow(), 0.5f));
                }
            }
            case RINGS -> {
                for (int i = 0; i < fw; i += 6) {
                    float f = GuiAnim.frac(at * 0.04f - i / 90f);
                    k.fill(x + i + 5, y, x + i + 6, y + h, GuiColor.argb(0x000000, 0.55f));
                    if (f < 0.2f) k.fill(x + i, y, x + i + 5, y + h, GuiColor.argb(0xFFFFFF, 0.3f * (1f - f * 5f)));
                }
            }
            case RAYS -> {
                int pos = (int) (GuiAnim.frac(at * 0.015f) * (fw + 12)) - 6;
                k.fill(Math.max(x, x + pos), y, Math.min(x + fw, x + pos + 8), y + h, GuiColor.argb(0xFFFFFF, 0.4f));
                k.star(head, y + h / 2, 2, GuiColor.argb(0xFFFFFF, 0.5f + 0.4f * GuiAnim.tri(at * 0.05f)));
            }
            default -> { }
        }
    }
}
