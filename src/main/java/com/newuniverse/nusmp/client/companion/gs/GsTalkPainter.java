package com.newuniverse.nusmp.client.companion.gs;

import com.newuniverse.nusmp.client.companion.TalkPainter;
import com.newuniverse.nusmp.client.ui.gs.GsClover;
import com.newuniverse.nusmp.client.ui.gs.GsPalette;
import com.newuniverse.nusmp.client.ui.gs.GsRipple;
import com.newuniverse.nusmp.client.ui.gs.GsSkin;

/**
 * Draws the Great Spirit talk screen from a {@link GsTalkFrame} and a {@link GsTalkLayout} through a {@link GsCanvas}, in pure code, so the game, the preview tool and the tests draw the same thing. Skin B
 * (Parchment and Ink: a dark wood panel with a parchment page for her words, gold filigree) is for normal users and skin C (Slime Crest: dark glass, violet and magenta) for devil users; the choice is the
 * frame's, made by the screen. The painter allocates nothing at rest (rectangles go through a reused scratch array, texts come ready from the frame). Worst case per frame, all four tabs counted at their
 * fullest: about 450 fills, 35 gradients, 30 sprites and 30 item icons; see docs/gui_redesign/companion_gui.md.
 */
public final class GsTalkPainter {
    private GsTalkPainter() {}

    private static final int[] DLG_TOP = {0xFFF2E4C0, 0xFF241038}, DLG_BOT = {0xFFE0CC9A, 0xFF130922}, DLG_EDGE = {0xFF9A7A44, 0xFFA65AFF};
    private static final int[] DLG_INK = {0xFF3A2616, 0xFFECE2F8}, DLG_NAME = {0xFF6A3A10, 0xFFFF8EEA}, DLG_DIM = {0xFF8A6A3C, 0xFF9E82B8};
    private static final int[] ROW_TOP = {0xFF3E2A15, 0xFF22103A}, ROW_BOT = {0xFF2C1D0E, 0xFF140A24}, ROW_HOT_TOP = {0xFF59401F, 0xFF3A1860}, ROW_HOT_BOT = {0xFF3E2A15, 0xFF22103A};
    private static final int[] WELL = {0xFF150D06, 0xFF0A0512}, BEZEL = {0xFF5A4326, 0xFF3A2268};
    private static final int[] PORT_TOP = {0xFF6B4D2B, 0xFF33165A}, PORT_BOT = {0xFF2F200F, 0xFF0F0620};
    private static final int LOCKED = 0xFF54505C;

    private static final int[] RECT = new int[4];                                       // scratch (the render thread only)

    public static int a(int alpha, int rgb) { return TalkPainter.a(Math.max(0, Math.min(255, alpha)), rgb); }

    private static int rgb(int[] pal, GsSkin s) { return pal[s.ordinal()]; }

    // ================================================================ the whole screen
    public static void paint(GsCanvas c, GsTalkLayout L, GsTalkFrame f) {
        int s = f.skin.ordinal();
        panel(c, L, f, s);
        portrait(c, L, f, s);
        leftColumn(c, L, f, s);
        tabs(c, L, f, s);
        dialogue(c, L, f, s);
        switch (L.tab) {
            case 0 -> rows(c, L, f, s);
            case 1 -> {
                interact(c, L, f, s);
                rows(c, L, f, s);
            }
            case 2 -> bond(c, L, f, s);
            default -> pouch(c, L, f, s);
        }
        footer(c, L, f, s);
        if (f.pickerOpen) picker(c, L, f, s);
    }

    // ================================================================ panel
    private static void panel(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        int[] p = L.panel;
        c.fill(p[0] - 1, p[1] - 1, p[0] + p[2] + 1, p[1] + p[3] + 1, a(0xC0, GsPalette.NEON[s]));
        c.gradient(p[0], p[1], p[0] + p[2], p[1] + p[3], a(0xF4, GsPalette.PANEL_TOP[s]), a(0xF7, GsPalette.PANEL_BOTTOM[s]));
        c.fill(p[0] + 1, p[1] + 1, p[0] + p[2] - 1, p[1] + 2, a(0x40, GsPalette.NEON2[s]));
        if (f.fancy && p[2] >= 96 && p[3] >= 64) {
            int o = 2, z = 30, sh = GsSprite.SHEET, white = 0xFFFFFFFF;
            c.sprite(GsSprite.CORNER, f.skin, p[0] - o, p[1] - o, z, z, 0, 0, sh, sh, white, false);
            c.sprite(GsSprite.CORNER, f.skin, p[0] + p[2] + o - z, p[1] - o, z, z, sh, 0, -sh, sh, white, false);
            c.sprite(GsSprite.CORNER, f.skin, p[0] - o, p[1] + p[3] + o - z, z, z, 0, sh, sh, -sh, white, false);
            c.sprite(GsSprite.CORNER, f.skin, p[0] + p[2] + o - z, p[1] + p[3] + o - z, z, z, sh, sh, -sh, -sh, white, false);
        }
    }

    // ================================================================ the portrait
    private static void portrait(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        int[] p = L.portrait;
        boolean hot = f.hover == GsHit.PORTRAIT;
        c.fill(p[0] - 2, p[1] - 2, p[0] + p[2] + 2, p[1] + p[3] + 2, a(0xFF, BEZEL[s]));
        c.fill(p[0] - 1, p[1] - 1, p[0] + p[2] + 1, p[1] + p[3] + 1, a(0xFF, hot ? GsPalette.NEON2[s] : GsPalette.NEON[s]));
        c.gradient(p[0], p[1], p[0] + p[2], p[1] + p[3], a(0xFF, PORT_TOP[s]), a(0xFF, PORT_BOT[s]));
        int[] in = L.portraitInner;
        int tier = RapportMeter.tier(Math.max(0, f.rapport));
        c.clip(in[0], in[1], in[0] + in[2], in[1] + in[3]);
        if (f.fancy && tier >= 2) {                                                      // a soft glow of the tier's clover behind her
            int g = Math.min(in[2], in[3]);
            int k = (int) (0x70 * (0.55f + 0.45f * GsClover.pulse(f.time, 0)));
            c.sprite(tier >= 3 ? GsSprite.GLOW5 : GsSprite.GLOW4, tier >= 3 ? GsSkin.C : f.skin, in[0] + (in[2] - g) / 2, in[1] + (in[3] - g) / 2, g, g, 0, 0, GsSprite.SHEET, GsSprite.SHEET, a(k, 0xFFFFFF), true);
        }
        if (f.face.has()) bust(c, L, f, in); else crest(c, f, in, s);
        if (f.fancy) sparkles(c, f, in);
        c.unclip();
        c.fill(p[0], p[1], p[0] + p[2], p[1] + 1, a(0x50, 0x000000));                    // an inner shade at the top
        medal(c, L, f, s);
    }

    private static void crest(GsCanvas c, GsTalkFrame f, int[] in, int s) {
        int z = Math.min(in[2], in[3]) - 8;
        c.sprite(GsSprite.CLOVER3, f.skin, in[0] + (in[2] - z) / 2, in[1] + (in[3] - z) / 2, z, z, 0, 0, GsSprite.SHEET, GsSprite.SHEET, 0xFFFFFFFF, false);
        String first = f.name == null || f.name.isEmpty() ? "?" : f.name.substring(0, 1);
        c.text(first, in[0] + (in[2] - c.textWidth(first)) / 2, in[1] + (in[3] - 8) / 2, a(0xFF, GsPalette.TEXT[s]));
    }

    /** The bust: shoulders and arms, the head, her hat layer, the expression and the blink. A few blits and at most {@link PortraitPlan#maxRects()} fills. */
    private static void bust(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int[] in) {
        int S = L.portraitScale, white = 0xFFFFFFFF;
        FaceInfo fi = f.face;
        int armW = fi.armW();
        int breath = PortraitPlan.breath(f.time);
        int headW = 8 * S;
        int hx = in[0] + (in[2] - headW) / 2, top = in[1] + in[3] - 12 * S;
        int hy = top - breath, ty = top + 8 * S - breath, th = 4 * S + breath;
        int sx = hx + PortraitPlan.shake(f.mood, f.moodAge);
        // shoulders: the torso and the two arms (the left arm of an old skin is the right one turned round)
        c.sprite(GsSprite.FACE, f.skin, hx, ty, headW, th, 20, 20, 8, 4, white, false);
        c.sprite(GsSprite.FACE, f.skin, hx - armW * S, ty, armW * S, th, 44, 20, armW, 4, white, false);
        if (fi.legacy()) c.sprite(GsSprite.FACE, f.skin, hx + headW, ty, armW * S, th, 44 + armW, 20, -armW, 4, white, false);
        else c.sprite(GsSprite.FACE, f.skin, hx + headW, ty, armW * S, th, 36, 52, armW, 4, white, false);
        if (!fi.legacy()) {                                                              // the second layer of the body
            c.sprite(GsSprite.FACE, f.skin, hx, ty, headW, th, 20, 36, 8, 4, white, false);
            c.sprite(GsSprite.FACE, f.skin, hx - armW * S, ty, armW * S, th, 44, 36, armW, 4, white, false);
            c.sprite(GsSprite.FACE, f.skin, hx + headW, ty, armW * S, th, 52, 52, armW, 4, white, false);
        }
        // the head and its hat layer
        c.sprite(GsSprite.FACE, f.skin, sx, hy, headW, 8 * S, 8, 8, 8, 8, white, false);
        c.sprite(GsSprite.FACE, f.skin, sx, hy, headW, 8 * S, 40, 8, 8, 8, white, false);
        // the expression, then the blink
        faceRects(c, PortraitPlan.rects(f.mood), sx, hy, S, fi.tone());
        if (PortraitPlan.blinking(f.time, f.mood)) faceRects(c, PortraitPlan.blinkRects(), sx, hy, S, fi.tone());
    }

    private static void faceRects(GsCanvas c, int[][] rects, int hx, int hy, int S, int tone) {
        for (int[] r : rects) {
            int x = hx + PortraitPlan.px(r[0], S), y = hy + PortraitPlan.px(r[1], S);
            c.fill(x, y, x + PortraitPlan.pxSize(r[2], S), y + PortraitPlan.pxSize(r[3], S), r[4] == PortraitPlan.TONE ? tone : r[4]);
        }
    }

    private static final int[] AURA = new int[3];

    private static void sparkles(GsCanvas c, GsTalkFrame f, int[] in) {
        int n = PortraitPlan.auraCount(f.mood);
        if (n == 0) return;
        int col = PortraitPlan.auraColor(f.mood);
        for (int i = 0; i < n; i++) {
            if (!PortraitPlan.aura(f.mood, i, f.time, in[2], in[3], AURA) || AURA[2] <= 6) continue;
            int x = in[0] + AURA[0], y = in[1] + AURA[1], k = a(AURA[2], col);
            c.fill(x - 1, y, x + 2, y + 1, k);                                           // a small plus
            c.fill(x, y - 1, x + 1, y + 2, k);
        }
    }

    /** The clover badge on the portrait's corner: three leaves for strangers and acquaintances, four with a glow for friends, five with drips for close friends; the rapport number sits in its hole. */
    private static void medal(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        if (f.rapport < 0) return;
        int[] m = L.medal;
        int tier = RapportMeter.tier(f.rapport);
        int n = tier >= 3 ? 5 : tier == 2 ? 4 : 3;
        GsSkin skin = GsClover.skinFor(n, f.skin);
        GsSprite frame = n == 5 ? GsSprite.CLOVER5 : n == 4 ? GsSprite.CLOVER4 : GsSprite.CLOVER3;
        c.sprite(frame, skin, m[0], m[1], m[2], m[3], 0, 0, GsSprite.SHEET, GsSprite.SHEET, 0xFFFFFFFF, false);
        float lift = Math.max(f.rapportGlow, 0f);
        if (n >= 4 && f.fancy) {
            int k = (int) (255 * Math.min(1f, 0.35f + 0.65f * GsClover.pulse(f.time, 3) + 0.5f * lift));
            c.sprite(n == 5 ? GsSprite.GLOW5 : GsSprite.GLOW4, skin, m[0], m[1], m[2], m[3], 0, 0, GsSprite.SHEET, GsSprite.SHEET, a(k, 0xFFFFFF), true);
        }
        if (n == 5) c.sprite(GsSprite.DRIPS, skin, m[0] + Math.round((float) Math.sin(f.time * 0.08f)), m[1], m[2], m[3], 0, 0, GsSprite.SHEET, GsSprite.SHEET, 0xFFFFFFFF, false);
        String t = f.rapportText;
        c.text(t, m[0] + (m[2] - c.textWidth(t)) / 2, m[1] + (m[3] - 8) / 2, a(0xFF, 0xFFFFFF));
    }

    // ================================================================ left column: plate, sub line, meter, mood
    private static void leftColumn(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        int col = f.color & 0xFFFFFF;
        int[] pl = L.plate;
        c.gradient(pl[0], pl[1], pl[0] + pl[2], pl[1] + pl[3], a(0xFF, TalkPainter.mix(col, 0x000000, 0.45f)), a(0xFF, TalkPainter.mix(col, 0x000000, 0.72f)));
        c.fill(pl[0], pl[1], pl[0] + 3, pl[1] + pl[3], a(0xFF, col));
        c.fill(pl[0], pl[1] + pl[3] - 1, pl[0] + pl[2], pl[1] + pl[3], a(0xC0, GsPalette.NEON[s]));
        c.text(TalkPainter.fit(c, f.name, pl[2] - 10), pl[0] + 7, pl[1] + 3, a(0xFF, TalkPainter.mix(col, 0xFFFFFF, 0.6f)));
        int[] sl = L.subLine;
        c.text(TalkPainter.fit(c, f.sub, sl[2]), sl[0], sl[1], a(0xFF, GsPalette.DIM[s]));
        meter(c, L, f, s);
        int[] mc = L.moodChip;
        if (mc[1] + mc[3] <= L.panel[1] + L.panel[3] - 2) {
            GsIcons.paint(c, GsIcons.Icon.GEM, mc[0], mc[1] + 1, 1, a(0xFF, moodColor(f.mood)), a(0xFF, 0xFFFFFF));
            c.text(TalkPainter.fit(c, f.moodName, mc[2] - 12), mc[0] + 12, mc[1] + 2, a(0xFF, GsPalette.DIM[s]));
        }
    }

    private static int moodColor(Mood m) {
        return switch (m) {
            case HAPPY -> 0xFFD060;
            case SHY -> 0xFF8EB4;
            case ANGRY -> 0xFF5A4A;
            case HUNGRY -> 0x8EDCFF;
            default -> 0xB8B8C8;
        };
    }

    /** Three leaves (lit at rapport 3, 10 and 25), a bar that fills toward the next one with a glow when it rises, and the tier line. */
    private static void meter(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        if (f.rapport < 0) return;
        int[] m = L.meter;
        float shown = f.rapportShown;
        for (int i = 0; i < RapportMeter.LEAVES; i++) {
            boolean lit = shown >= RapportMeter.LEAF_AT[i] - 0.001f && RapportMeter.isLit(f.rapport, i);
            int x = m[0] + i * 12;
            if (lit && f.fancy && i == f.litLeaf) {                                       // the leaf that just lit pops with a ring of light
                float pop = RapportMeter.pop(f.sinceRise);
                int r = Math.round(6 + 7 * (pop - 1f) / 0.45f);
                c.sprite(GsSprite.RIPPLE, f.skin, x + 4 - r, m[1] + 4 - r, 2 * r, 2 * r, 0, 0, GsSprite.RIPPLE_SHEET, GsSprite.RIPPLE_SHEET, a((int) (200 * (1.45f - pop) / 0.45f + 40), 0xFFFFFF), true);
            }
            GsIcons.paint(c, GsIcons.Icon.HEART, x, m[1], 1, a(0xFF, lit ? GsPalette.NEON2[s] : 0x4A4650), a(0xFF, 0xFFFFFF));
        }
        if (!f.nextText.isEmpty()) c.text(f.nextText, m[0] + m[2] - c.textWidth(f.nextText), m[1] + 1, a(0xFF, GsPalette.DIM[s]));
        int seg = (m[2] - 4) / 3, by = m[1] + 12;
        for (int i = 0; i < RapportMeter.LEAVES; i++) {
            int x0 = m[0] + i * (seg + 2);
            c.fill(x0, by, x0 + seg, by + 5, a(0xFF, WELL[s]));
            int fill = Math.round(seg * RapportMeter.leafProgress(shown, i));
            if (fill > 0) {
                c.gradient(x0, by, x0 + fill, by + 5, a(0xFF, GsPalette.NEON2[s]), a(0xFF, GsPalette.NEON[s]));
                if (f.rapportGlow > 0.02f) c.fill(x0, by, x0 + fill, by + 5, a((int) (150 * f.rapportGlow), 0xFFFFFF));
            }
            c.fill(x0, by + 5, x0 + seg, by + 6, a(0x60, 0x000000));
        }
        c.text(TalkPainter.fit(c, f.tierLine, m[2]), m[0], m[1] + 20, a(0xFF, GsPalette.TEXT[s]));
    }

    // ================================================================ tabs
    private static final GsIcons.Icon[] TAB_ICONS = {GsIcons.Icon.TALK, GsIcons.Icon.HAND, GsIcons.Icon.HEART, GsIcons.Icon.BAG};

    private static void tabs(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        for (int i = 0; i < GsTalkLayout.TABS; i++) {
            int[] r = L.tabRect(i, RECT);
            boolean on = i == f.tab, hot = f.hover == GsHit.TAB + i;
            int x = r[0], y = r[1], w = r[2], h = r[3];
            if (on) c.gradient(x, y, x + w, y + h, a(0xFF, TalkPainter.mix(GsPalette.NEON[s], 0x000000, 0.1f)), a(0xFF, TalkPainter.mix(GsPalette.NEON[s], 0x000000, 0.45f)));
            else c.gradient(x, y, x + w, y + h, a(0xFF, hot ? ROW_HOT_TOP[s] : ROW_TOP[s]), a(0xFF, hot ? ROW_HOT_BOT[s] : ROW_BOT[s]));
            if (on) c.fill(x, y + h - 1, x + w, y + h, a(0xFF, GsPalette.NEON2[s]));
            int ink = on ? (s == 0 ? 0xFF2B1A0C : 0xFFFFFFFF) : a(0xFF, hot ? GsPalette.NEON2[s] : GsPalette.TEXT[s]);
            GsIcons.paint(c, TAB_ICONS[i], x + 4, y + 3, 1, ink, ink);
            if (w >= 56) c.text(TalkPainter.fit(c, f.tabNames[i], w - 18), x + 16, y + 4, ink);
            if (hot && !on) ripple(c, f, x, y, w, h);
        }
    }

    // ================================================================ dialogue
    private static void dialogue(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        int[] d = L.dialogue;
        int x = d[0], y = d[1], w = d[2], h = d[3];
        c.fill(x - 1, y - 1, x + w + 1, y + h + 1, DLG_EDGE[s]);
        c.gradient(x, y, x + w, y + h, DLG_TOP[s], DLG_BOT[s]);
        c.fill(x, y, x + 3, y + h, a(0xFF, f.color));
        c.text(TalkPainter.fit(c, f.name, w - 20), x + 8, y + 3, DLG_NAME[s]);
        c.fill(x + 8, y + 12, x + w - 8, y + 13, a(0x55, s == 0 ? 0x6A3A10 : 0xA65AFF));
        int ly = y + 15, lines = Math.min(L.dialogueLines, f.dialogue.length);
        for (int i = 0; i < lines; i++) {
            String line = f.dialogue[i];
            int n = TypeWriter.ofLine(f.dialogue, i, f.typed);
            if (n <= 0) break;
            String t = n >= line.length() ? line : line.substring(0, n);
            c.text(t, x + 8, ly + i * 10, DLG_INK[s]);
            if (n < line.length() && ((int) (f.time / 4f) & 1) == 0) c.fill(x + 8 + c.textWidth(t) + 1, ly + i * 10, x + 8 + c.textWidth(t) + 4, ly + i * 10 + 8, a(0xA0, DLG_INK[s]));
        }
        if (f.dialogueDone && ((int) (f.time / 14f) & 1) == 0) GsIcons.paint(c, GsIcons.Icon.ARROW, x + w - 13, y + h - 11, 1, a(0xD0, DLG_NAME[s]), a(0xD0, DLG_NAME[s]));
    }

    // ================================================================ plain rows (also the Interact and Bond rows)
    private static void rows(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        int count = f.rows.size();
        if (L.tab == 3 || (L.tab == 2 && count == 0)) return;
        for (int slot = 0; slot < L.visibleRows; slot++) {
            int i = f.scroll + slot;
            if (i >= count) break;
            GsTalkFrame.Row row = f.rows.get(i);
            int[] r = L.rowRect(slot, RECT);
            int x = r[0], y = r[1], w = r[2], h = r[3];
            boolean hot = f.hover == GsHit.ROW + i && !row.blocked();
            c.gradient(x, y, x + w, y + h, a(0xFF, hot ? ROW_HOT_TOP[s] : ROW_TOP[s]), a(0xFF, hot ? ROW_HOT_BOT[s] : ROW_BOT[s]));
            c.fill(x, y, x + 2, y + h, a(hot ? 0xFF : 0x70, GsPalette.NEON[s]));
            int ink = row.blocked() ? LOCKED : a(0xFF, hot ? GsPalette.NEON2[s] : GsPalette.TEXT[s]);
            GsIcons.paint(c, GsIcons.Icon.DIAMOND, x + 4, y + 2, 1, ink, ink);
            c.text(TalkPainter.fit(c, row.label(), w - 20), x + 16, y + 3, ink);
            if (hot) ripple(c, f, x, y, w, h);
        }
        if (count > L.visibleRows) {
            int x = L.list[0] + L.rowsW - 4, h = L.visibleRows * GsTalkLayout.ROW_PITCH - 1;
            c.fill(x, L.list[1], x + 3, L.list[1] + h, a(0xFF, WELL[s]));
            int knob = Math.max(8, h * L.visibleRows / count), max = Math.max(1, count - L.visibleRows);
            int ky = L.list[1] + (h - knob) * Math.min(f.scroll, max) / max;
            c.fill(x, ky, x + 3, ky + knob, a(0xFF, GsPalette.NEON[s]));
        }
    }

    // ================================================================ a button
    private static void button(GsCanvas c, GsTalkFrame f, int[] r, int code, String label, GsIcons.Icon icon, boolean enabled, boolean active, int s) {
        int x = r[0], y = r[1], w = r[2], h = r[3];
        boolean hot = f.hover == code && enabled;
        c.gradient(x, y, x + w, y + h, a(0xFF, hot || active ? ROW_HOT_TOP[s] : ROW_TOP[s]), a(0xFF, hot || active ? ROW_HOT_BOT[s] : ROW_BOT[s]));
        c.fill(x, y, x + w, y + 1, a(enabled ? 0xA0 : 0x40, active ? GsPalette.NEON2[s] : GsPalette.NEON[s]));
        c.fill(x, y + h - 1, x + w, y + h, a(0x60, 0x000000));
        int ink = !enabled ? LOCKED : a(0xFF, hot || active ? GsPalette.NEON2[s] : GsPalette.TEXT[s]);
        int tx = x + 4;
        if (icon != null) {
            GsIcons.paint(c, icon, x + 4, y + (h - 9) / 2, 1, ink, ink);
            tx = x + 16;
        }
        String t = TalkPainter.fit(c, label, w - (tx - x) - 3);
        c.text(t, icon == null ? x + (w - c.textWidth(t)) / 2 : tx, y + (h - 8) / 2 + 1, ink);
        if (hot) ripple(c, f, x, y, w, h);
    }

    /** The slime ripple of a hovered widget: up to three rings from where the pointer entered, clipped to the widget; nothing on quality LOW or once the last ring has ended. */
    private static void ripple(GsCanvas c, GsTalkFrame f, int x, int y, int w, int h) {
        if (!f.fancy || !GsRipple.anyAlive(f.time, f.rippleAt)) return;
        c.clip(x, y, x + w, y + h);
        for (int i = 0; i < GsRipple.MAX_RINGS; i++) {
            float age = GsRipple.age(f.time, f.rippleAt, i);
            if (!GsRipple.alive(age)) continue;
            int r = Math.max(2, Math.round(GsRipple.radius(age)));
            int px = Math.round(f.rippleX), py = Math.round(f.rippleY);
            c.sprite(GsSprite.RIPPLE, f.skin, px - r, py - r, r * 2, r * 2, 0, 0, GsSprite.RIPPLE_SHEET, GsSprite.RIPPLE_SHEET, a((int) (GsRipple.alpha(age) * 0.85f * 255f), 0xFFFFFF), true);
        }
        c.unclip();
    }

    // ================================================================ a slot with an item
    private static void slot(GsCanvas c, GsTalkFrame f, int[] r, int code, int itemRef, boolean filled, boolean selected, int s) {
        boolean hot = f.hover == code;
        c.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], a(0xFF, hot ? GsPalette.NEON[s] : BEZEL[s]));
        c.fill(r[0] + 1, r[1] + 1, r[0] + r[2] - 1, r[1] + r[3] - 1, a(0xFF, WELL[s]));
        if (selected) c.fill(r[0] + 1, r[1] + 1, r[0] + r[2] - 1, r[1] + 2, a(0xFF, GsPalette.NEON2[s]));
        if (filled) c.item(itemRef, r[0] + (r[2] - 16) / 2, r[1] + (r[3] - 16) / 2);
        if (hot) ripple(c, f, r[0], r[1], r[2], r[3]);
    }

    private static void badge(GsCanvas c, GiftMemory.Reaction re, int x, int y, int s) {
        if (re == null) return;
        switch (re) {
            case LOVED -> GsIcons.paint(c, GsIcons.Icon.HEART, x, y, 1, a(0xFF, 0xFF5C8A), a(0xFF, 0xFFFFFF));
            case HATED -> GsIcons.paint(c, GsIcons.Icon.CROSS, x, y, 1, a(0xFF, GsPalette.BAD[s]), a(0xFF, 0xFFFFFF));
            case LIKED -> GsIcons.paint(c, GsIcons.Icon.GEM, x, y, 1, a(0xFF, GsPalette.OK[s]), a(0xFF, 0xFFFFFF));
            default -> GsIcons.paint(c, GsIcons.Icon.CROSS, x, y, 1, a(0xFF, 0x8A8A98), a(0xFF, 0xFFFFFF));
        }
    }

    // ================================================================ Interact tab: the three tiles
    private static void interact(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        if (f.hasFive) tile(c, f, L.tile[0], GsHit.FIVE, GsIcons.Icon.HAND, f.label(GsTalkFrame.L_FIVE), !f.fiveBlocked, false, s);
        if (f.hasFollow) {
            tile(c, f, L.tile[1], GsHit.FOLLOW, f.following ? GsIcons.Icon.PLAY : GsIcons.Icon.PAUSE, f.following ? f.label(GsTalkFrame.L_FOLLOWING) : f.label(GsTalkFrame.L_WAITING), !f.followBlocked, f.following, s);
            int[] t = L.tile[1];                                                          // the little switch under the icon: knob right when she walks with you
            int sx = t[0] + t[2] - 22, sy = t[1] + 5;
            c.fill(sx, sy, sx + 16, sy + 7, a(0xFF, WELL[s]));
            c.fill(sx + (f.following ? 9 : 0), sy, sx + (f.following ? 16 : 7), sy + 7, a(0xFF, f.following ? GsPalette.OK[s] : 0x6A6674));
        }
        if (f.hasPanel) tile(c, f, L.tile[2], GsHit.PANEL, GsIcons.Icon.STAR, f.label(GsTalkFrame.L_PANEL), !f.panelBlocked, false, s);
    }

    private static void tile(GsCanvas c, GsTalkFrame f, int[] r, int code, GsIcons.Icon icon, String label, boolean enabled, boolean active, int s) {
        int x = r[0], y = r[1], w = r[2], h = r[3];
        boolean hot = f.hover == code && enabled;
        c.gradient(x, y, x + w, y + h, a(0xFF, hot || active ? ROW_HOT_TOP[s] : ROW_TOP[s]), a(0xFF, hot || active ? ROW_HOT_BOT[s] : ROW_BOT[s]));
        c.fill(x, y, x + w, y + 1, a(enabled ? 0xC0 : 0x40, GsPalette.NEON[s]));
        c.fill(x, y + h - 1, x + w, y + h, a(0x60, 0x000000));
        int ink = !enabled ? LOCKED : a(0xFF, hot || active ? GsPalette.NEON2[s] : GsPalette.TEXT[s]);
        GsIcons.paint(c, icon, x + 6, y + 5, 2, ink, a(0xFF, 0xFFFFFF));
        String t = TalkPainter.fit(c, label, w - 8);
        c.text(t, x + (w - c.textWidth(t)) / 2, y + h - 11, ink);
        if (hot) ripple(c, f, x, y, w, h);
    }

    // ================================================================ Bond tab: gift, likes, notebook, stance, story
    private static void bond(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        if (f.hasGift) {
            int[] g = L.giftSlot;
            slot(c, f, g, GsHit.GIFT_SLOT, GsItemRef.GIFT, f.heldIndex >= 0, f.pickerOpen, s);
            if (f.heldIndex < 0) GsIcons.paint(c, GsIcons.Icon.GEM, g[0] + 8, g[1] + 8, 1, a(0x60, GsPalette.DIM[s]), a(0x60, GsPalette.DIM[s]));
            badge(c, f.heldReaction, g[0] + g[2] - 8, g[1] - 4, s);
            button(c, f, L.give, GsHit.GIVE, f.label(GsTalkFrame.L_GIVE), GsIcons.Icon.HEART, f.heldIndex >= 0, false, s);
            tastes(c, L, f, s);
        }
        if (f.hasLikes) button(c, f, L.likes, GsHit.LIKES, f.label(GsTalkFrame.L_LIKES), GsIcons.Icon.GEM, true, false, s);
        if (f.stanceOffered) {
            GsIcons.Icon[] ic = {GsIcons.Icon.SHIELD, GsIcons.Icon.SWORD, GsIcons.Icon.LEAF};
            for (int i = 0; i < 3; i++) {
                int[] r = L.stanceBtn[i];
                boolean on = f.stance == i, hot = f.hover == GsHit.STANCE + i;
                c.gradient(r[0], r[1], r[0] + r[2], r[1] + r[3], a(0xFF, on || hot ? ROW_HOT_TOP[s] : ROW_TOP[s]), a(0xFF, on || hot ? ROW_HOT_BOT[s] : ROW_BOT[s]));
                c.fill(r[0], r[1], r[0] + r[2], r[1] + 1, a(on ? 0xFF : 0x70, on ? GsPalette.NEON2[s] : GsPalette.NEON[s]));
                if (on) {
                    c.fill(r[0], r[1] + r[3] - 2, r[0] + r[2], r[1] + r[3], a(0xFF, GsPalette.NEON2[s]));
                    GsIcons.paint(c, GsIcons.Icon.GEM, r[0] + r[2] - 11, r[1] + 3, 1, a(0xFF, GsPalette.OK[s]), a(0xFF, 0xFFFFFF));
                }
                int ink = a(0xFF, on || hot ? GsPalette.NEON2[s] : GsPalette.TEXT[s]);
                int sc = r[3] >= 26 ? 2 : 1;
                GsIcons.paint(c, ic[i], r[0] + 5, r[1] + (r[3] - 9 * sc) / 2, sc, ink, a(0xFF, 0xFFFFFF));
                c.text(TalkPainter.fit(c, f.label(GsTalkFrame.L_STANCE0 + i), r[2] - 12 - 9 * sc), r[0] + 8 + 9 * sc, r[1] + (r[3] - 8) / 2, ink);
                if (hot) ripple(c, f, r[0], r[1], r[2], r[3]);
            }
        }
        story(c, L, f, s);
        if (L.tab == 2 && !f.rows.isEmpty()) rows(c, L, f, s);
    }

    private static void tastes(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        int[] t = L.tastes;
        if (t[2] < 40) return;
        c.text(TalkPainter.fit(c, f.label(GsTalkFrame.L_TASTES), t[2] - 4), t[0] + 2, t[1] + 2, a(0xFF, GsPalette.DIM[s]));
        int cells = Math.min(L.tasteCells(), f.tasteReactions.size());
        if (cells == 0) {
            c.text(TalkPainter.fit(c, f.label(GsTalkFrame.L_NO_TASTES), t[2] - 4), t[0] + 2, t[1] + 16, a(0x90, GsPalette.DIM[s]));
            return;
        }
        for (int i = 0; i < cells; i++) {
            int[] r = L.tasteSlot(i, RECT);
            slot(c, f, r, GsHit.TASTE + i, GsItemRef.taste(i), true, false, s);
            badge(c, f.tasteReactions.get(i), r[0] + r[2] - 8, r[1] + r[3] - 8, s);
        }
    }

    private static void story(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        if (L.storyLines <= 0 || (L.tab == 2 && !f.rows.isEmpty())) return;
        int[] ar = L.storyArea;
        c.fill(ar[0], ar[1], ar[0] + ar[2], ar[1] + 1, a(0x70, GsPalette.NEON[s]));
        c.text(TalkPainter.fit(c, f.label(GsTalkFrame.L_STORY), ar[2]), ar[0] + 2, ar[1] + 3, a(0xFF, GsPalette.NEON[s]));
        if (f.story.length == 0) {
            c.text(TalkPainter.fit(c, f.label(GsTalkFrame.L_NO_STORY), ar[2] - 4), ar[0] + 4, ar[1] + 14, a(0x90, GsPalette.DIM[s]));
            return;
        }
        int n = Math.min(L.storyLines, f.story.length);
        for (int i = 0; i < n; i++) {
            BondStory.Kind k = i < f.storyKinds.length ? f.storyKinds[i] : BondStory.Kind.NOTE;
            c.fill(ar[0] + 3, ar[1] + 15 + i * 10, ar[0] + 6, ar[1] + 18 + i * 10, a(0xFF, kindColor(k, s)));
        }
        c.compiled(f.story, n, ar[0] + 10, ar[1] + 13, 10, f.storyAge, a(0xFF, GsPalette.DIM[s]), a(0xFF, GsPalette.TEXT[s]), a(0xFF, GsPalette.DATA[s]));
    }

    private static int kindColor(BondStory.Kind k, int s) {
        return switch (k) {
            case RAPPORT, LOVED -> 0xFF8AB0;
            case HATED -> GsPalette.BAD[s];
            case LIKED, HEAL -> GsPalette.OK[s];
            case JOINED -> GsPalette.NEON2[s];
            case LEFT, STANCE -> GsPalette.WARN[s];
            default -> GsPalette.DIM[s];
        };
    }

    // ================================================================ Pouch tab: nine slots, the heal ring, switches, the hotbar strip
    private static void pouch(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        for (int i = 0; i < 9; i++) {
            int[] r = L.pouchSlot(i, RECT);
            boolean filled = !f.pouchCounts[i].isEmpty();
            slot(c, f, r, GsHit.POUCH + i, GsItemRef.pouch(i), filled, false, s);
            if (f.ring == PouchRing.State.READY && f.ringSlot == i) c.sprite(GsSprite.RIPPLE, f.skin, r[0] - 3, r[1] - 3, r[2] + 6, r[3] + 6, 0, 0, GsSprite.RIPPLE_SHEET, GsSprite.RIPPLE_SHEET, a(0xD0, GsPalette.OK[s]), true);
        }
        if (L.ringShown) heal(c, L, f, s);
        if (f.hasUse) button(c, f, L.use, GsHit.USE, f.useIt == 1 ? f.label(GsTalkFrame.L_USE_OFF) : f.label(GsTalkFrame.L_USE_ON), f.useIt == 1 ? GsIcons.Icon.PAUSE : GsIcons.Icon.PLAY, true, f.useIt == 1, s);
        if (f.hasPut) button(c, f, L.put, GsHit.PUT, f.label(GsTalkFrame.L_PUT), GsIcons.Icon.BAG, f.heldIndex >= 0 || !L.stripShown, false, s);
        if (f.hasTakeAll) button(c, f, L.takeAll, GsHit.TAKE_ALL, f.label(GsTalkFrame.L_TAKE_ALL), GsIcons.Icon.ARROW, true, false, s);
        if (f.hasPut && L.stripShown) {                                                   // the hotbar strip: which item "Put in" takes
            for (int i = 0; i < 9; i++) {
                int[] r = L.hotbarSlot(i, RECT);
                slot(c, f, r, GsHit.HOTBAR + i, GsItemRef.inventory(i), (f.inventoryFilled & (1L << i)) != 0, f.heldIndex == i, s);
            }
            int tx = L.strip[0] + L.strip[2] + 6;
            c.text(TalkPainter.fit(c, f.label(GsTalkFrame.L_PUT_HINT), L.panel[0] + L.panel[2] - 10 - tx), tx, L.strip[1] + 5, a(0xA0, GsPalette.DIM[s]));
        }
    }

    /** The heal-ready ring: a ring that breathes green while she has something she would use on you, with a light running round it; dim when switched off, empty or locked. */
    private static void heal(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        int[] r = L.ring;
        int cx = r[0] + r[2] / 2, cy = r[1] + 22, size = 44;
        PouchRing.State st = f.ring;
        float b = PouchRing.brightness(st, f.time);
        int col = st == PouchRing.State.READY ? GsPalette.OK[s] : st == PouchRing.State.EMPTY ? GsPalette.WARN[s] : 0x8A8A98;
        c.sprite(GsSprite.RIPPLE, f.skin, cx - size / 2, cy - size / 2, size, size, 0, 0, GsSprite.RIPPLE_SHEET, GsSprite.RIPPLE_SHEET, a((int) (255 * b), col), true);
        if (st == PouchRing.State.READY && f.fancy) {
            double ang = PouchRing.sweep(f.time) * Math.PI * 2.0;
            int dx = (int) Math.round(Math.cos(ang) * 19), dy = (int) Math.round(Math.sin(ang) * 19);
            c.fill(cx + dx - 1, cy + dy - 1, cx + dx + 2, cy + dy + 2, a(0xFF, 0xFFFFFF));
        }
        GsIcons.paint(c, GsIcons.Icon.HEART, cx - 9, cy - 9, 2, a(st == PouchRing.State.READY ? 0xFF : 0x80, col), a(0xFF, 0xFFFFFF));
        String t = switch (st) {
            case READY -> f.label(GsTalkFrame.L_RING_READY);
            case EMPTY -> f.label(GsTalkFrame.L_RING_EMPTY);
            case OFF -> f.label(GsTalkFrame.L_RING_OFF);
            default -> f.label(GsTalkFrame.L_RING_LOCKED);
        };
        String fit = TalkPainter.fit(c, t, r[2] + 4);
        c.text(fit, r[0] + (r[2] - c.textWidth(fit)) / 2, r[1] + 48, a(0xFF, st == PouchRing.State.READY ? col : GsPalette.DIM[s]));
    }

    // ================================================================ footer and picker
    private static void footer(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        int[] cl = L.close;
        button(c, f, cl, GsHit.CLOSE, f.label(GsTalkFrame.L_CLOSE), null, true, false, s);
        if (f.hint != null && !f.hint.isEmpty()) c.text(TalkPainter.fit(c, f.hint, L.hintLine[2]), L.hintLine[0], L.hintLine[1] + 3, a(0xFF, GsPalette.DIM[s]));
    }

    /** The inventory picker over the content: the 27 inventory slots, the hotbar under them, the remembered reactions as badges. */
    private static void picker(GsCanvas c, GsTalkLayout L, GsTalkFrame f, int s) {
        int[] p = L.picker;
        c.fill(p[0] - 1, p[1] - 1, p[0] + p[2] + 1, p[1] + p[3] + 1, a(0xFF, GsPalette.NEON[s]));
        c.gradient(p[0], p[1], p[0] + p[2], p[1] + p[3], a(0xFA, GsPalette.PANEL_TOP[s]), a(0xFC, GsPalette.PANEL_BOTTOM[s]));
        c.text(TalkPainter.fit(c, f.label(GsTalkFrame.L_PICKER), p[2] - 8), p[0] + 4, p[1] + 3, a(0xFF, GsPalette.NEON2[s]));
        for (int i = 0; i < 36; i++) {
            int[] r = L.pickerSlot(i, RECT);
            boolean filled = (f.inventoryFilled & (1L << i)) != 0;
            slot(c, f, r, GsHit.PICKER + i, GsItemRef.inventory(i), filled, f.heldIndex == i, s);
            GiftMemory.Reaction re = f.inventoryReactions[i];
            if (filled && re != null) {
                int bx = r[0] + r[2] - 5, by = r[1] + r[3] - 5;
                c.fill(bx, by, bx + 4, by + 4, a(0xFF, re == GiftMemory.Reaction.LOVED ? 0xFF5C8A : re == GiftMemory.Reaction.HATED ? GsPalette.BAD[s] : re == GiftMemory.Reaction.LIKED ? GsPalette.OK[s] : 0x8A8A98));
            }
        }
    }
}
