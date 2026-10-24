package com.newuniverse.nusmp.client.companion;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: draws the talk screen from a {@link TalkFrame} and a {@link TalkLayout} through a {@link TalkCanvas}, in pure code, so the game, the preview tool and the tests draw the same thing. The look is the mod's dark glass:
 * a near-black panel with a thin edge and a header stripe in the speaker's colour, chip tabs (the shown one lit), a recessed box with her words, a rapport bar in four segments (stranger, acquaintance, friend, close;
 * each fills as the rapport passes through its tier) on the Bond and Pouch tabs, rows for the choices (a closed one dimmed), a scrollbar when there are more rows than fit, the nine pouch slots, and a Close button. It
 * draws no item icons (the screen draws them over the slots) and allocates only the few lines of wrapped text.
 */
public final class TalkPainter {
    private TalkPainter() {}

    public static final int INK = 0xFFEDEDF4, DIM = 0xFF9A9AB0, BLOCKED = 0xFF6E6E80, WELL = 0xFF0B0B11, PANEL = 0xF0101016, ROW = 0xFF191922, ROW_HOVER = 0xFF262634;
    /** Rapport at which each of the four segments of the bar starts, and where the last ends (CompanionRules: 3, 10, 25, 99). */
    public static final int[] SEGMENTS = {0, 3, 10, 25, 99};

    public static int a(int alpha, int rgb) { return (alpha << 24) | (rgb & 0xFFFFFF); }

    /** Mixes two 0xRRGGBB colours (t = 0 gives a, t = 1 gives b). */
    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t), bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private record Token(String text, boolean breakAfter) {}

    /** Greedy word wrap by pixel width; a word wider than the line is cut by characters; at most {@code maxLines} lines (the last ends with "..." when text was left over). */
    public static List<String> wrap(TalkCanvas c, String s, int width, int maxLines) {
        List<String> out = new ArrayList<>();
        if (s == null || s.isBlank() || maxLines <= 0) return out;
        List<Token> tokens = new ArrayList<>();
        for (String word : s.strip().split("\\s+")) {
            while (c.textWidth(word) > width && word.length() > 1) {
                int n = word.length() - 1;
                while (n > 1 && c.textWidth(word.substring(0, n)) > width) n--;
                tokens.add(new Token(word.substring(0, n), true));
                word = word.substring(n);
            }
            tokens.add(new Token(word, false));
        }
        StringBuilder line = new StringBuilder();
        for (Token t : tokens) {
            String candidate = line.length() == 0 ? t.text() : line + " " + t.text();
            if (c.textWidth(candidate) <= width || line.length() == 0) {
                line.setLength(0);
                line.append(candidate);
            } else {
                out.add(line.toString());
                line.setLength(0);
                line.append(t.text());
            }
            if (t.breakAfter()) {
                out.add(line.toString());
                line.setLength(0);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        if (out.size() > maxLines) {
            out = new ArrayList<>(out.subList(0, maxLines));
            String last = out.get(maxLines - 1);
            while (last.length() > 1 && c.textWidth(last + "...") > width) last = last.substring(0, last.length() - 1);
            out.set(maxLines - 1, last + "...");
        }
        return out;
    }

    /** Cuts a label to fit a width, with "..." (never wider than the width). */
    public static String fit(TalkCanvas c, String s, int width) {
        if (s == null) return "";
        if (c.textWidth(s) <= width) return s;
        String t = s;
        while (t.length() > 1 && c.textWidth(t + "...") > width) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    public static void paint(TalkCanvas c, TalkLayout L, TalkFrame f) {
        int col = f.color();
        int[] p = L.panel;
        // panel: edge in the speaker's colour, dark body, header stripe
        c.fill(p[0] - 1, p[1] - 1, p[0] + p[2] + 1, p[1] + p[3] + 1, a(0xB0, col));
        c.fill(p[0], p[1], p[0] + p[2], p[1] + p[3], PANEL);
        c.fill(p[0], p[1], p[0] + 4, p[1] + L.header[3], a(0xFF, col));
        c.fill(p[0] + 4, p[1] + L.header[3] - 1, p[0] + p[2], p[1] + L.header[3], a(0x80, col));
        c.text(fit(c, f.title(), p[2] - 24), p[0] + 12, p[1] + 7, 0xFF000000 | mix(col, 0xFFFFFF, 0.55f));
        c.text(fit(c, f.sub(), p[2] - 24), p[0] + 12, p[1] + 18, DIM);

        // tabs
        for (int i = 0; i < TalkLayout.TABS; i++) {
            int[] r = L.tabRect(i);
            boolean on = i == f.tab();
            int bg = on ? a(0xC0, col) : (i == f.hoverTab() ? 0xFF2A2A38 : ROW);
            c.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], bg);
            if (on) c.fill(r[0], r[1] + r[3] - 1, r[0] + r[2], r[1] + r[3], 0xFF000000 | mix(col, 0xFFFFFF, 0.4f));
            String name = f.tabNames()[i];
            c.text(name, r[0] + (r[2] - c.textWidth(name)) / 2, r[1] + 3, on ? 0xFF101016 : INK);
        }

        // her words
        int[] t = L.text;
        c.fill(t[0], t[1], t[0] + t[2], t[1] + t[3], WELL);
        c.fill(t[0], t[1], t[0] + 2, t[1] + t[3], a(0xFF, col));
        int maxLines = Math.max(1, (t[3] - 4) / 10);
        int y = t[1] + 4;
        for (String line : wrap(c, f.text(), t[2] - 12, maxLines)) {
            c.text(line, t[0] + 7, y, INK);
            y += 10;
        }

        // the status strip: rapport in four segments, its tier, the stance
        if (L.status[3] > 0 && f.rapport() >= 0) {
            int[] s = L.status;
            int barW = s[2] * 56 / 100, seg = (barW - 3) / 4;
            for (int i = 0; i < 4; i++) {
                int x0 = s[0] + i * (seg + 1);
                c.fill(x0, s[1] + 3, x0 + seg, s[1] + 9, 0xFF22222C);
                int lo = SEGMENTS[i], hi = SEGMENTS[i + 1];
                int filled = f.rapport() >= hi ? seg : f.rapport() <= lo ? 0 : seg * (f.rapport() - lo) / (hi - lo);
                if (filled > 0) c.fill(x0, s[1] + 3, x0 + filled, s[1] + 9, 0xFF000000 | mix(col, 0xFFFFFF, 0.25f));
            }
            c.text(fit(c, f.tierName() + "  " + f.rapport(), barW), s[0], s[1] + 11, DIM);
            String line = f.statusLine();
            if (line != null && !line.isEmpty()) c.text(fit(c, line, s[2] - barW - 8), s[0] + barW + 8, s[1] + 3, INK);
        }

        // the choices
        int count = f.rows().size();
        for (int slot = 0; slot < L.visibleRows; slot++) {
            int i = f.scroll() + slot;
            if (i >= count) break;
            TalkFrame.Row row = f.rows().get(i);
            int[] r = L.rowRect(slot);
            boolean hover = i == f.hoverRow() && !row.blocked();
            c.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], hover ? ROW_HOVER : ROW);
            if (hover) c.fill(r[0], r[1], r[0] + 2, r[1] + r[3], 0xFF000000 | mix(col, 0xFFFFFF, 0.3f));
            c.text(fit(c, row.label(), r[2] - 10), r[0] + 6, r[1] + 3, row.blocked() ? BLOCKED : INK);
        }
        if (count > L.visibleRows) {                                                         // a scrollbar
            int x = L.list[0] + L.listRowsW - 4, h = L.visibleRows * TalkLayout.ROW_PITCH - 1;
            c.fill(x, L.list[1], x + 3, L.list[1] + h, 0xFF22222C);
            int knob = Math.max(8, h * L.visibleRows / count), max = Math.max(1, count - L.visibleRows);
            int ky = L.list[1] + (h - knob) * Math.min(f.scroll(), max) / max;
            c.fill(x, ky, x + 3, ky + knob, 0xFF000000 | mix(col, 0xFFFFFF, 0.3f));
        }
        if (count == 0) c.text("-", L.list[0] + 6, L.list[1] + 3, BLOCKED);

        // the pouch slots (the screen draws the items over them)
        if (L.tab == 3 && L.grid[2] > 0) {
            for (int i = 0; i < TalkLayout.SLOTS; i++) {
                int[] r = L.slotRect(i);
                String n = i < f.slots().size() ? f.slots().get(i) : "";
                c.fill(r[0], r[1], r[0] + r[2] - 1, r[1] + r[3] - 1, i == f.hoverSlot() && !n.isEmpty() ? ROW_HOVER : WELL);
                c.fill(r[0], r[1], r[0] + r[2] - 1, r[1] + 1, 0xFF2A2A38);
                c.fill(r[0], r[1], r[0] + 1, r[1] + r[3] - 1, 0xFF2A2A38);
            }
        }

        // the footer
        int[] cl = L.close;
        c.fill(cl[0], cl[1], cl[0] + cl[2], cl[1] + cl[3], f.hoverClose() ? 0xFF3A2A2A : ROW);
        c.text(f.closeLabel(), cl[0] + (cl[2] - c.textWidth(f.closeLabel())) / 2, cl[1] + 3, INK);
        if (f.hint() != null && !f.hint().isEmpty()) c.text(f.hint(), p[0] + TalkLayout.PAD, cl[1] + 3, DIM);
    }
}
