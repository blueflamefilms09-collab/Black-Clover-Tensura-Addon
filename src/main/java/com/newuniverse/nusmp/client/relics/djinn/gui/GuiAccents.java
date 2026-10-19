package com.newuniverse.nusmp.client.relics.djinn.gui;

import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.MED_CX;
import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.MED_CY;
import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.MED_X;
import static com.newuniverse.nusmp.client.relics.djinn.gui.GuiLayout.MED_Y;

/**
 * The accents ({@link GuiAccent}): the big signature shape behind the emblem, drawn procedurally round the medallion's centre, animated from the
 * clock and scaled by {@code a} (0..1, how much of the Djinn is revealed). Nothing is allocated.
 */
final class GuiAccents {
    private GuiAccents() {}

    private static final float TAU = GuiAnim.TAU;

    static void paint(Ink k, GuiTheme th, float at, float en, float a) {
        if (a <= 0.02f) return;
        GuiPalette p = th.pal;
        int spin = th.tune.spin();
        int cx = MED_CX, cy = MED_CY;
        int hx = MED_X + Emblem.HEAD_X, hy = MED_Y + Emblem.HEAD_Y;
        switch (th.tune.accent()) {
            case BLOOD_RING -> {
                int c = GuiColor.mix(0xC01030, p.primary(), 0.3f);
                float rot = at * 0.02f * spin;
                k.arc(cx, cy, 25, 25, rot, TAU / 40f, 40, 1, GuiColor.argb(c, 0.5f * a));
                for (int i = 0; i < 4; i++) k.box(Math.round(cx + GuiAnim.cos(rot + i * TAU / 4f) * 25f) - 1, Math.round(cy + GuiAnim.sin(rot + i * TAU / 4f) * 25f) - 1, 3, 3, GuiColor.argb(c, 0.9f * a));
            }
            case SUN_ARCH -> {
                float len = 9f + 5f * GuiAnim.breathe(at, 40f, 0f, 1f);
                for (int i = 0; i < 13; i++) {
                    float ang = -(float) Math.PI + (i + 0.5f) * (float) Math.PI / 13f;
                    float l = len * (i % 2 == 0 ? 1f : 0.6f);
                    for (int s = 0; s < 4; s++) {
                        float r = 15f + s * l / 4f;
                        k.box(Math.round(hx + GuiAnim.cos(ang) * r), Math.round(hy - 4 + GuiAnim.sin(ang) * r), 2, 1, GuiColor.argb(p.glow(), (0.7f - s * 0.15f) * a));
                    }
                }
                k.arc(hx, hy - 4, 14, 14, -(float) Math.PI, (float) Math.PI / 24f, 25, 1, GuiColor.argb(p.accent(), 0.6f * a));
            }
            case WATER_RIBBONS -> {
                for (int r = 0; r < 2; r++)
                    for (int x = cx - 26; x <= cx + 26; x += 2) {
                        int y = cy + 6 + (r == 0 ? -4 : 4) + Math.round(GuiAnim.sin(x * 0.22f + at * 0.12f + r * (float) Math.PI) * 6f);
                        k.box(x, y, 2, 2, GuiColor.argb(r == 0 ? p.accent() : p.primary(), 0.5f * a));
                    }
            }
            case CYCLONE_RINGS -> {
                for (int r = 0; r < 2; r++) {
                    float tilt = r == 0 ? 0.35f : -0.35f, ph = at * 0.05f * (r == 0 ? 1 : -1);
                    for (int i = 0; i < 30; i++) {
                        float ang = ph + i * TAU / 30f, x = GuiAnim.cos(ang) * 27f, y = GuiAnim.sin(ang) * 9f;
                        k.box(Math.round(cx + x), Math.round(cy + 4 + y + x * tilt), 2, 1, GuiColor.argb(r == 0 ? p.glow() : p.accent(), (0.25f + 0.35f * (0.5f + 0.5f * GuiAnim.sin(ang))) * a));
                    }
                }
            }
            case VINE_CROWN -> {
                float rot = at * 0.01f * spin;
                for (int i = 0; i < 12; i++) {
                    float ang = rot + i * TAU / 12f;
                    int x = Math.round(hx + GuiAnim.cos(ang) * 13f), y = Math.round(hy - 4 + GuiAnim.sin(ang) * 10f);
                    k.dot(x, y, GuiColor.argb(GuiColor.mix(p.accent(), 0x2E7A34, 0.5f), 0.7f * a));
                    if (i % 2 == 0) k.box(x + (GuiAnim.cos(ang) > 0f ? 1 : -2), y - 1, 2, 1, GuiColor.argb(p.accent(), 0.9f * a));
                }
            }
            case SHARD_CROWN -> {
                float rot = at * 0.015f * spin;
                for (int i = 0; i < 6; i++) {
                    float ang = rot + i * TAU / 6f;
                    int x = Math.round(hx + GuiAnim.cos(ang) * 18f), y = Math.round(hy - 2 + GuiAnim.sin(ang) * 11f + GuiAnim.sin(at * 0.1f + i) * 1.5f);
                    int c = GuiColor.argb(GuiColor.mix(p.primary(), p.glow(), 0.4f), (0.55f + 0.35f * GuiAnim.sin(ang)) * a);
                    k.dot(x, y - 3, c);
                    k.box(x - 1, y - 2, 3, 1, c);
                    k.box(x - 1, y - 1, 3, 2, c);
                    k.dot(x, y + 1, c);
                }
            }
            case SOUND_FORK -> {
                for (int s = -1; s <= 1; s += 2)
                    for (int i = 0; i < 3; i++) {
                        float r = 17f + ((at * 0.35f + i * 6f) % 18f), f = 1f - (r - 17f) / 18f;
                        float base = s > 0 ? 0f : (float) Math.PI;
                        int n = 14;
                        for (int j = 0; j < n; j++) {
                            float ang = base + (j - n / 2f) * 0.07f * (s > 0 ? 1 : -1);
                            k.dot(Math.round(cx + GuiAnim.cos(ang) * r), Math.round(cy + GuiAnim.sin(ang) * r), GuiColor.argb(i == 0 ? p.glow() : p.accent(), 0.7f * f * a));
                        }
                    }
            }
            case EMBER_MANE -> {
                for (int i = 0; i < 15; i++) {
                    float ang = -(float) Math.PI * (0.05f + 0.9f * i / 14f) - 0.1f;
                    float l = 5f + 8f * GuiAnim.noise(i * 0.8f + at * 0.25f, 4);
                    int x0 = Math.round(hx + GuiAnim.cos(ang) * 11f), y0 = Math.round(hy - 2 + GuiAnim.sin(ang) * 11f);
                    int x1 = Math.round(hx + GuiAnim.cos(ang) * (11f + l)), y1 = Math.round(hy - 2 + GuiAnim.sin(ang) * (11f + l));
                    k.line(x0, y0, x1, y1, GuiColor.argb(GuiColor.mix(p.accent(), p.glow(), 0.3f + 0.5f * (1f - l / 13f)), 0.7f * a));
                }
            }
            case DAWN_FAN -> {
                for (int i = 0; i < 15; i++) {
                    float ang = -(float) Math.PI + (i + 0.5f) * (float) Math.PI / 15f;
                    float l = i % 2 == 0 ? 28f : 21f, tw = 0.5f + 0.5f * GuiAnim.sin(at * 0.06f + i * 0.7f);
                    for (float r = 10f; r < l; r += 3f)
                        k.dot(Math.round(hx + GuiAnim.cos(ang) * r), Math.round(hy + 6 + GuiAnim.sin(ang) * r), GuiColor.argb(p.glow(), (0.55f - 0.35f * (r - 10f) / 18f) * (0.5f + 0.5f * tw) * a));
                }
            }
            case STORM_ARCS -> {
                int slot = (int) (at / 3f);
                for (int s = 0; s < 3; s++) {
                    if (GuiAnim.rnd(slot, s + 40) < 0.4f) continue;
                    float a0 = GuiAnim.rnd(slot, s + 50) * TAU;
                    float px = cx + GuiAnim.cos(a0) * 26f, py = cy + GuiAnim.sin(a0) * 26f;
                    for (int i = 1; i <= 9; i++) {
                        float ang = a0 + i * 0.12f * spin, r = 26f + (GuiAnim.rnd(slot * 13 + i, s + 60) - 0.5f) * 5f;
                        float nx = cx + GuiAnim.cos(ang) * r, ny = cy + GuiAnim.sin(ang) * r;
                        k.line(Math.round(px), Math.round(py), Math.round(nx), Math.round(ny), GuiColor.argb(i % 3 == 0 ? 0xFFFFFF : p.glow(), 0.85f * a));
                        px = nx;
                        py = ny;
                    }
                }
            }
            case MOON -> {
                int mx = hx, my = hy - 6;
                k.disc(mx, my, 15, GuiColor.argb(p.accent(), 0.14f * a));
                k.disc(mx, my, 12, GuiColor.argb(p.glow(), 0.10f * a));
                k.arc(mx, my, 15, 15, 0f, TAU / 48f, 48, 1, GuiColor.argb(p.glow(), 0.5f * a));
                k.box(mx - 6, my - 4, 3, 2, GuiColor.argb(p.accent(), 0.18f * a));
                k.box(mx + 3, my + 3, 4, 2, GuiColor.argb(p.accent(), 0.18f * a));
            }
            case REAPER_MOON -> {
                int mx = hx - 1, my = hy - 4;
                float br = GuiAnim.breathe(at, 60f, 0.7f, 1f);
                for (int dy = -17; dy <= 17; dy++) {
                    int hwo = (int) Math.floor(Math.sqrt(17 * 17 + 17 - dy * dy));
                    int dyi = dy + 3;
                    float inner = 15f * 15f + 15f - dyi * dyi;
                    int c = GuiColor.argb(GuiColor.mix(p.accent(), p.glow(), 0.4f), 0.55f * br * a);
                    if (inner <= 0f) k.fill(mx - hwo, my + dy, mx + hwo + 1, my + dy + 1, c);
                    else {
                        int hwi = (int) Math.floor(Math.sqrt(inner));
                        int il = mx + 7 - hwi, ir = mx + 7 + hwi + 1;
                        k.fill(mx - hwo, my + dy, Math.min(il, mx + hwo + 1), my + dy + 1, c);
                        if (ir < mx + hwo + 1) k.fill(ir, my + dy, mx + hwo + 1, my + dy + 1, c);
                    }
                }
            }
            default -> { }
        }
    }
}
