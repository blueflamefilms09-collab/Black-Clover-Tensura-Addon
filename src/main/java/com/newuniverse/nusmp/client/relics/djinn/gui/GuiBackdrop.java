package com.newuniverse.nusmp.client.relics.djinn.gui;

import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.H;
import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.MED_CX;
import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.MED_CY;
import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.W;

/**
 * The themed panel: {@link #back} paints the panel's body and the animated background of the Djinn's magic (heat haze, flashes, waves, streaks, stone
 * and cracks, rings, rays); {@link #frame} paints the border over everything with its corner ornaments, the magic's edge decoration (flame tongues,
 * lightning arcs, a rippling edge, chevrons, vines, broken stone, nodes, stars) and, in a Full Equip, the light that runs round the border.
 * {@code at} is the animation clock already scaled by the Djinn's and the magic's tempo, {@code en} the energy of the phase.
 */
final class GuiBackdrop {
    private GuiBackdrop() {}

    static void back(Ink k, GuiTheme th, GuiState st, float at, float en) {
        GuiPalette p = th.pal;
        for (int i = 0; i < 8; i++) {
            int y0 = H * i / 8, y1 = H * (i + 1) / 8;
            k.fill(0, y0, W, y1, GuiColor.argb(GuiColor.mix(p.mid(), p.deep(), i / 7f), 0.90f));
        }
        float ac = 0.55f + 0.45f * en;
        switch (th.motif) {
            case EMBERS -> {
                for (int i = 0; i < 5; i++) {
                    float a = (0.22f - 0.04f * i) * (0.8f + 0.2f * GuiAnim.sin(at * 0.15f + i)) * ac;
                    k.fill(1, H - 4 - i * 3, W - 1, H - 1 - i * 3, GuiColor.argb(p.accent(), a));
                }
            }
            case ARCS -> {
                int slot = (int) (at / 9f);
                float flash = GuiAnim.rnd(slot, 3) < 0.2f ? 1f - GuiAnim.frac(at / 9f) : 0f;
                if (flash > 0.02f) k.fill(1, 1, W - 1, H - 1, GuiColor.argb(p.accent(), 0.17f * flash * ac));
                for (int x = 78; x < W - 4; x += 14) k.fill(x, 3, x + 1, H - 3, GuiColor.argb(p.accent(), 0.06f));
            }
            case RIPPLE -> {
                for (int w = 0; w < 2; w++)
                    for (int x = 2; x < W - 2; x += 3) {
                        int y = H - 15 - w * 10 + Math.round(2f * GuiAnim.sin(x * 0.12f + at * 0.12f * (1f + w * 0.4f) + w * 2f));
                        k.fill(x, y, x + 3, y + 1, GuiColor.argb(p.accent(), (0.20f - 0.06f * w) * ac));
                    }
            }
            case STREAKS -> {
                for (int s = 0; s < 7; s++) {
                    int y = 4 + (int) (GuiAnim.rnd(s, 1) * (H - 8)), len = 10 + (int) (GuiAnim.rnd(s, 2) * 26);
                    float sp = 0.6f + GuiAnim.rnd(s, 4) * 1.4f;
                    int x = (int) ((at * sp * 1.6f + GuiAnim.rnd(s, 3) * (W + len)) % (W + len)) - len;
                    k.fill(x, y, x + len, y + 1, GuiColor.argb(p.accent(), 0.13f * ac));
                    k.fill(x + len * 6 / 10, y, x + len, y + 1, GuiColor.argb(p.glow(), 0.24f * ac));
                }
            }
            case VINES -> {
                for (int i = 0; i < 4; i++) k.fill(1, H - 4 - i * 4, W - 1, H - 1 - i * 4, GuiColor.argb(p.accent(), 0.10f - 0.02f * i));
            }
            case CRACKS -> {
                for (int i = 0; i < 46; i++) {
                    int x = (int) (GuiAnim.rnd(i, 21) * (W - 2)) + 1, y = (int) (GuiAnim.rnd(i, 22) * (H - 2)) + 1;
                    k.dot(x, y, GuiColor.argb(i % 3 == 0 ? p.rim() : 0x000000, 0.14f));
                }
                float beat = GuiAnim.sin(at * 0.09f);
                beat = beat > 0f ? beat * beat * beat * beat * beat * beat : 0f;
                for (int c = 0; c < 3; c++) {
                    int x = 84 + c * 46 + (int) (GuiAnim.rnd(th.tune.corner(), c) * 12), y = 1;
                    for (int i = 0; i < 14; i++) {
                        int nx = x + (GuiAnim.rnd(c * 40 + i, 5) < 0.5f ? -2 : 2), ny = y + 3 + (int) (GuiAnim.rnd(c * 40 + i, 6) * 3);
                        k.line(x, y, nx, ny, GuiColor.argb(0x000000, 0.45f));
                        k.dot(nx + 1, ny, GuiColor.argb(p.accent(), (0.10f + 0.40f * beat) * ac));
                        x = nx;
                        y = ny;
                        if (y > H - 3) break;
                    }
                }
            }
            case RINGS -> {
                for (int i = 0; i < 3; i++) {
                    float r = 32f + (at * 0.9f + i * 34f) % 102f, f = 1f - (r - 32f) / 102f;
                    int n = Math.max(8, (int) (r * 0.55f));
                    k.arc(MED_CX, MED_CY, r, r, -1.25f, 2.5f / n, n, 1, GuiColor.argb(p.accent(), 0.34f * f * ac));
                }
            }
            case RAYS -> {
                for (int i = 0; i < 8; i++) {
                    float a = at * 0.012f + i * GuiAnim.TAU / 8f;
                    float ca = GuiAnim.cos(a), sa = GuiAnim.sin(a);
                    if (ca < -0.2f) continue;
                    for (int s = 0; s < 18; s++) {
                        float r = 34f + s * 7f;
                        k.fill(Math.round(MED_CX + ca * r), Math.round(MED_CY + sa * r), Math.round(MED_CX + ca * r) + 2, Math.round(MED_CY + sa * r) + 2,
                                GuiColor.argb(p.glow(), 0.16f * (1f - s / 18f) * ac));
                    }
                }
            }
            default -> { }
        }
    }

    static void frame(Ink k, GuiTheme th, GuiState st, float at, float en) {
        GuiPalette p = th.pal;
        boolean full = st.phase >= 3;
        float charge = st.charge();
        int rim = GuiColor.argb(p.rim(), full ? 0.95f : 0.65f);
        int inner = GuiColor.argb(p.primary(), full ? 0.55f : 0.28f);
        // border (the corners are left to the ornaments)
        k.fill(2, 0, W - 2, 1, rim);
        k.fill(2, H - 1, W - 2, H, rim);
        k.fill(0, 2, 1, H - 2, rim);
        k.fill(W - 1, 2, W, H - 2, rim);
        k.fill(2, 1, W - 2, 2, inner);
        k.fill(2, H - 2, W - 2, H - 1, inner);
        k.fill(1, 2, 2, H - 2, inner);
        k.fill(W - 2, 2, W - 1, H - 2, inner);
        if (full) {                                                    // the Full Equip: a second bright line and a light that runs round the border
            int glow = GuiColor.argb(p.glow(), 0.35f + 0.2f * GuiAnim.sin(at * 0.2f));
            k.fill(3, 2, W - 3, 3, glow);
            k.fill(3, H - 3, W - 3, H - 2, glow);
            k.fill(2, 3, 3, H - 3, glow);
            k.fill(W - 3, 3, W - 2, H - 3, glow);
            int[] pt = POINT;
            float head = at * 2.2f * th.tune.spin();
            for (int i = 0; i < 26; i++) {
                GuiAnim.perimeter(head - i * th.tune.spin(), W, H, pt);
                k.dot(pt[0], pt[1], GuiColor.argb(p.glow() | 0, (1f - i / 26f) * 0.95f));
            }
        }
        if (charge > 0f) {                                             // an Extreme Magic gathers: the border flickers white
            float f = 0.5f + 0.5f * GuiAnim.sin(at * (1.2f + 2f * charge));
            int c = GuiColor.argb(0xFFFFFF, 0.55f * charge * f);
            k.fill(0, 0, W, 1, c);
            k.fill(0, H - 1, W, H, c);
            k.fill(0, 0, 1, H, c);
            k.fill(W - 1, 0, W, H, c);
        }
        edge(k, th, at, en);
        corners(k, th, st);
        // the divider between the medallion and the text
        k.fill(GuiLayout.COL_X - 4, 7, GuiLayout.COL_X - 3, H - 7, GuiColor.argb(p.rim(), 0.22f));
    }

    private static final int[] POINT = new int[2];
    private static final int[][] CORNERS = {{0, 0, 1, 1}, {W - 1, 0, -1, 1}, {0, H - 1, 1, -1}, {W - 1, H - 1, -1, -1}};

    private static void edge(Ink k, GuiTheme th, float at, float en) {
        GuiPalette p = th.pal;
        float ac = 0.55f + 0.45f * en;
        switch (th.motif) {
            case EMBERS -> {
                for (int x = 5; x < W - 5; x += 3) {
                    float n = GuiAnim.noise(x * 0.3f + at * 0.35f, 11);
                    int h = 1 + (int) (n * 3.4f * (0.5f + 0.5f * en));
                    k.fill(x, H - 1 - h, x + 2, H - 1, GuiColor.argb(GuiColor.mix(p.accent(), p.glow(), n), 0.8f));
                    k.dot(x, H - 2 - h, GuiColor.argb(p.glow(), 0.6f * n));
                }
            }
            case ARCS -> {
                int slot = (int) (at / 3f);
                if (GuiAnim.rnd(slot, 7) < 0.75f) {
                    for (int side = 0; side < 2; side++) {
                        int base = side == 0 ? 2 : H - 3, prev = base;
                        for (int x = 6; x < W - 8; x += 8) {
                            int y = base + (int) (GuiAnim.rnd(slot * 31 + x, side + 1) * 5f) - 2 + (side == 0 ? 1 : -1);
                            k.fill(x, y, x + 8, y + 1, GuiColor.argb(p.glow(), 0.65f * ac));
                            k.fill(x, Math.min(y, prev), x + 1, Math.max(y, prev) + 1, GuiColor.argb(0xFFFFFF, 0.55f * ac));
                            prev = y;
                        }
                    }
                }
            }
            case RIPPLE -> {
                for (int x = 2; x < W - 2; x += 3) {
                    int y = H - 3 + Math.round(1.3f * GuiAnim.sin(x * 0.2f - at * 0.2f));
                    k.fill(x, y, x + 3, y + 1, GuiColor.argb(p.glow(), 0.7f * ac));
                }
            }
            case STREAKS -> {
                for (int i = 0; i < 4; i++) {
                    int x = 5 + (int) ((at * 0.8f + i * 50f) % (W - 14));
                    int c = GuiColor.argb(p.glow(), 0.85f);
                    k.fill(x, 2, x + 2, 3, c);
                    k.fill(x + 2, 3, x + 4, 4, c);
                    k.fill(x, 4, x + 2, 5, c);
                    int x2 = W - 9 - (int) ((at * 0.8f + i * 50f + 20f) % (W - 14));
                    k.fill(x2 + 2, H - 5, x2 + 4, H - 4, c);
                    k.fill(x2, H - 4, x2 + 2, H - 3, c);
                    k.fill(x2 + 2, H - 3, x2 + 4, H - 2, c);
                }
            }
            case VINES -> {
                int stem = GuiColor.argb(GuiColor.mix(p.accent(), 0x2E7A34, 0.5f), 0.9f), leaf = GuiColor.argb(p.accent(), 0.9f);
                for (int x = 3; x < W - 3; x += 3) {
                    int y = H - 3 + Math.round(1.1f * GuiAnim.sin(x * 0.25f + at * 0.06f));
                    k.fill(x, y, x + 3, y + 1, stem);
                    if ((x / 3) % 3 == 0) {
                        int up = (x / 3) % 2 == 0 ? -2 : 1;
                        k.fill(x, y + up, x + 2, y + up + 1, leaf);
                        k.dot(x + 2, y + up + (up < 0 ? -1 : 1), leaf);
                    }
                }
                for (int y = H - 8; y > H - 26; y -= 3) k.fill(W - 3 + Math.round(GuiAnim.sin(y * 0.4f + at * 0.06f)), y, W - 2, y + 3, stem);
            }
            case CRACKS -> {
                float beat = GuiAnim.sin(at * 0.09f);
                beat = beat > 0f ? beat * beat * beat * beat * beat * beat : 0f;
                for (int x = 3; x < W - 3; x += 7) {
                    if (GuiAnim.rnd(x, 31) < 0.22f) k.fill(x, 0, x + 6, 1, GuiColor.argb(0x000000, 0.9f));
                    if (GuiAnim.rnd(x, 32) < 0.22f) k.fill(x, H - 1, x + 6, H, GuiColor.argb(0x000000, 0.9f));
                }
                for (int y = 3; y < H - 3; y += 7) {
                    if (GuiAnim.rnd(y, 33) < 0.22f) k.fill(0, y, 1, y + 6, GuiColor.argb(0x000000, 0.9f));
                    if (GuiAnim.rnd(y, 34) < 0.22f) k.fill(W - 1, y, W, y + 6, GuiColor.argb(0x000000, 0.9f));
                }
                k.fill(1, H - 3, W - 1, H - 2, GuiColor.argb(p.accent(), 0.15f + 0.4f * beat));
            }
            case RINGS -> {
                for (int i = 0; i < 6; i++) {
                    float f = GuiAnim.frac(at * 0.05f - i * 0.15f);
                    float a = f < 0.25f ? 1f - f * 4f : 0.25f;
                    int x = 14 + i * 36;
                    int c = GuiColor.argb(p.glow(), a);
                    k.frame(x, 1, 3, 3, c);
                    k.frame(x, H - 4, 3, 3, c);
                }
            }
            case RAYS -> {
                float tw = GuiAnim.tri(at * 0.04f);
                k.star(W / 2 + 20, 2, 2 + (int) (tw * 2f), GuiColor.argb(p.glow(), 0.9f));
                k.star(W / 2 + 20, H - 3, 2 + (int) ((1f - tw) * 2f), GuiColor.argb(p.glow(), 0.9f));
                k.fill(W / 2 - 40, 1, W / 2 + 80, 2, GuiColor.argb(GuiColor.GOLD, 0.35f));
                k.fill(W / 2 - 40, H - 2, W / 2 + 80, H - 1, GuiColor.argb(GuiColor.GOLD, 0.35f));
            }
            default -> { }
        }
    }

    /** Four corner ornaments, one of four shapes per Djinn. */
    private static void corners(Ink k, GuiTheme th, GuiState st) {
        GuiPalette p = th.pal;
        int c = GuiColor.argb(st.phase >= 3 ? p.glow() : p.rim(), 1f);
        int d = GuiColor.argb(p.primary(), 0.8f);

        for (int[] q : CORNERS) {
            int x = q[0], y = q[1], sx = q[2], sy = q[3];
            switch (th.tune.corner()) {
                case 0 -> {                                              // L brackets
                    k.fill(Math.min(x, x + sx * 6), y, Math.max(x + 1, x + sx * 6 + 1), y + 1, c);
                    k.fill(x, Math.min(y, y + sy * 6), x + 1, Math.max(y + 1, y + sy * 6 + 1), c);
                    k.dot(x + sx, y + sy, d);
                }
                case 1 -> {                                              // a round stud
                    k.fill(Math.min(x, x + sx * 3), Math.min(y, y + sy * 3), Math.max(x + 1, x + sx * 3 + 1), Math.max(y + 1, y + sy * 3 + 1), d);
                    k.dot(x, y, c);
                    k.dot(x + sx, y + sy, c);
                }
                case 2 -> {                                              // stepped
                    for (int i = 0; i < 4; i++) {
                        int px = x + sx * i, py = y + sy * (3 - i);
                        k.dot(px, py, c);
                        if (i > 0) k.dot(px + sx, py - sy, d);
                    }
                }
                default -> {                                             // a diagonal slash
                    for (int i = 0; i < 5; i++) k.dot(x + sx * i, y + sy * (4 - i), i % 2 == 0 ? c : d);
                    k.dot(x, y, c);
                }
            }
        }
    }
}
