package com.newuniverse.nusmp.client.skilltree.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Why: where every part of the shop screen sits is a handful of integer sums, and the sums are exactly what goes wrong at the window sizes nobody tested (a 427x240 GUI at scale 3, a
 * 320x180 one at scale 4, a 1920x1080 one at scale 1): a button hanging out of the panel, two lists on top of each other, a list with zero rows so nothing can ever be clicked. So the
 * layout is computed here, with no Minecraft type, from the screen size alone, and a test walks it across sizes from 300x170 to 1920x1080 and demands that everything is inside the
 * panel, nothing overlaps, and every list shows at least one row. The panel grows with the screen up to a limit (a bigger window shows more shelf rows, not a bigger font), and the
 * screen asks this class for every rectangle and every click target, so drawing and hit-testing can never disagree. The preview tool {@code tools/skilltree/preview_shop.py} mirrors
 * these numbers by hand (it is a mock-up, not the game).
 */
public final class ShopLayout {
    /** A rectangle in GUI pixels. */
    public record Rect(int x, int y, int w, int h) {
        public int right() { return x + w; }

        public int bottom() { return y + h; }

        public boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }

        public boolean overlaps(Rect o) { return x < o.right() && o.x < right() && y < o.bottom() && o.y < bottom(); }

        public boolean inside(Rect o) { return x >= o.x && y >= o.y && right() <= o.right() && bottom() <= o.bottom(); }
    }

    /** The panel's largest and smallest size, header and footer heights, outer padding, scrollbar width, the heading above each list, and the height of the bulk-sale button row. */
    public static final int MAX_W = 440, MAX_H = 270, MIN_W = 312, MIN_H = 176, HEAD_H = 48, FOOT_H = 30, PAD = 6, SCROLL_W = 4, HEADING_H = 13, BULK_H = 14;
    /** The height of one row of the shelf and of the player's goods, and the sizes of the Buy and Sell buttons. */
    public static final int SHELF_ROW = 24, GOODS_ROW = 22, BUY_W = 36, BUY_H = 16, SELL_W = 32, SELL_H = 14;
    /** The room around a bulk-sale label (both sides together) and the gap between two such buttons, roomy and tight; the tight pair is what a very narrow window falls back on. */
    public static final int BULK_PAD = 8, BULK_GAP = 3, BULK_PAD_TIGHT = 4, BULK_GAP_TIGHT = 2;

    public final Rect panel, portrait, purse, closeButton, speech, shelfBox, shelfList, shelfBar, goodsBox, bulkRow, goodsList, goodsBar;
    public final int shelfVisible, goodsVisible;

    private ShopLayout(Rect panel, int shelfW) {
        this.panel = panel;
        int x = panel.x(), y = panel.y(), w = panel.w(), h = panel.h();
        portrait = new Rect(x + PAD, y + 3, 40, 42);
        purse = new Rect(x + w - PAD - 150, y + 7, 150, 34);
        int bodyTop = y + HEAD_H + 2, bodyBottom = y + h - FOOT_H - 2;
        int bodyH = bodyBottom - bodyTop;
        shelfBox = new Rect(x + PAD, bodyTop, shelfW, bodyH);
        goodsBox = new Rect(shelfBox.right() + PAD, bodyTop, w - 3 * PAD - shelfW, bodyH);
        shelfList = new Rect(shelfBox.x() + 2, shelfBox.y() + HEADING_H, shelfBox.w() - 4 - SCROLL_W - 1, shelfBox.h() - HEADING_H - 2);
        shelfBar = new Rect(shelfList.right() + 1, shelfList.y(), SCROLL_W, shelfList.h());
        bulkRow = new Rect(goodsBox.x() + 2, goodsBox.y() + HEADING_H, goodsBox.w() - 4, BULK_H);
        goodsList = new Rect(goodsBox.x() + 2, bulkRow.bottom() + 2, goodsBox.w() - 4 - SCROLL_W - 1, goodsBox.bottom() - 2 - (bulkRow.bottom() + 2));
        goodsBar = new Rect(goodsList.right() + 1, goodsList.y(), SCROLL_W, goodsList.h());
        closeButton = new Rect(x + w - PAD - 54, y + h - FOOT_H + 7, 54, 16);
        speech = new Rect(x + PAD, y + h - FOOT_H + 2, closeButton.x() - PAD - (x + PAD), FOOT_H - 4);
        shelfVisible = Math.max(1, shelfList.h() / SHELF_ROW);
        goodsVisible = Math.max(1, goodsList.h() / GOODS_ROW);
    }

    /** The layout for a screen of this size in GUI pixels. */
    public static ShopLayout of(int screenW, int screenH) {
        int w = Math.max(MIN_W, Math.min(MAX_W, screenW - 8));
        int h = Math.max(MIN_H, Math.min(MAX_H, screenH - 8));
        if (screenW - 8 < MIN_W) w = Math.max(200, screenW - 4);
        if (screenH - 8 < MIN_H) h = Math.max(150, screenH - 4);
        int shelfW = Math.round((w - 3 * PAD) * 0.58f);
        Rect panel = new Rect((screenW - w) / 2, (screenH - h) / 2, w, h);
        return new ShopLayout(panel, shelfW);
    }

    // ================================================================ rows
    /** The rectangle of the shelf row with this index (in the whole list) when the list is scrolled to {@code scroll}. */
    public Rect shelfRow(int index, int scroll) {
        return new Rect(shelfList.x(), shelfList.y() + (index - scroll) * SHELF_ROW, shelfList.w(), SHELF_ROW - 1);
    }

    public Rect goodsRow(int index, int scroll) {
        return new Rect(goodsList.x(), goodsList.y() + (index - scroll) * GOODS_ROW, goodsList.w(), GOODS_ROW - 1);
    }

    /** The Buy button of a shelf row. */
    public static Rect buyButton(Rect row) {
        return new Rect(row.right() - 3 - BUY_W, row.y() + (row.h() - BUY_H) / 2, BUY_W, BUY_H);
    }

    /** The Sell button of a goods row. */
    public static Rect sellButton(Rect row) {
        return new Rect(row.right() - 2 - SELL_W, row.y() + (row.h() - SELL_H) / 2, SELL_W, SELL_H);
    }

    /** The icon of a row (16x16), at the left. */
    public static Rect icon(Rect row) {
        return new Rect(row.x() + 3, row.y() + (row.h() - 16) / 2, 16, 16);
    }

    /**
     * The rectangles of the bulk-sale buttons for labels of these pixel widths, left to right along the strip: each is its label plus {@code pad} wide with {@code gap} between, and
     * only the ones that fit inside the strip are returned (always a prefix of the labels, never a button hanging out of the box).
     */
    public static List<Rect> bulkRects(Rect strip, int[] labelWidths, int pad, int gap) {
        List<Rect> out = new ArrayList<>();
        int x = strip.x();
        for (int labelW : labelWidths) {
            int w = labelW + pad;
            if (x + w > strip.right()) break;
            out.add(new Rect(x, strip.y(), w, strip.h()));
            x += w + gap;
        }
        return out;
    }

    /** How many lines of speech (9 pixels each, starting 4 below the top edge, 2 clear above the bottom edge) the bubble holds; at least one. */
    public static int speechLines(Rect speech) {
        return Math.max(1, (speech.h() - 5) / 9);
    }

    // ================================================================ text
    /**
     * Wraps a sentence greedily on spaces into lines no wider than {@code maxW} pixels (as {@code width} measures them), at most {@code maxLines} of them. A word wider than a whole
     * line is broken by letters, and when the text needs more lines than there are, the last line is cut and ends in "..." (still within {@code maxW}), so the result always fits the
     * box it was measured for. Blank text gives no lines. Pure, so the one place where text can overflow the bubble is a unit test, not something to look for in the game.
     */
    public static List<String> wrap(String text, int maxW, int maxLines, ToIntFunction<String> width) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank() || maxW <= 0 || maxLines <= 0) return out;
        List<String> all = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String joined = cur.length() == 0 ? word : cur + " " + word;
            if (width.applyAsInt(joined) <= maxW) {
                cur.setLength(0);
                cur.append(joined);
                continue;
            }
            if (cur.length() > 0) {
                all.add(cur.toString());
                cur.setLength(0);
            }
            String rest = word;
            while (rest.length() > 1 && width.applyAsInt(rest) > maxW) {
                int n = rest.length() - 1;
                while (n > 1 && width.applyAsInt(rest.substring(0, n)) > maxW) n--;
                all.add(rest.substring(0, n));
                rest = rest.substring(n);
            }
            cur.append(rest);
        }
        if (cur.length() > 0) all.add(cur.toString());
        if (all.size() <= maxLines) return all;
        out.addAll(all.subList(0, maxLines));
        String last = out.get(maxLines - 1) + "...";
        while (last.length() > 3 && width.applyAsInt(last) > maxW) last = last.substring(0, last.length() - 4) + "...";
        out.set(maxLines - 1, last);
        return out;
    }

    /** The index of the row under the point in a list of {@code total} rows scrolled to {@code scroll}, or -1. */
    public static int rowAt(Rect list, int rowH, int scroll, int total, double mx, double my) {
        if (!list.contains(mx, my)) return -1;
        int i = scroll + (int) ((my - list.y()) / rowH);
        return i >= 0 && i < total ? i : -1;
    }

    // ================================================================ scrolling
    /** The greatest scroll (in rows) that still fills the list. */
    public static int maxScroll(int total, int visible) {
        return Math.max(0, total - visible);
    }

    public static int clampScroll(int scroll, int total, int visible) {
        return Math.max(0, Math.min(scroll, maxScroll(total, visible)));
    }

    /** The thumb of a scrollbar as {@code {top offset, height}} within a track of {@code trackH} pixels, or null when everything fits. */
    public static int[] thumb(int total, int visible, int scroll, int trackH) {
        if (total <= visible || trackH < 8) return null;
        int h = Math.max(8, Math.round(trackH * (visible / (float) total)));
        int travel = trackH - h;
        int max = maxScroll(total, visible);
        int top = max == 0 ? 0 : Math.round(travel * (clampScroll(scroll, total, visible) / (float) max));
        return new int[]{top, h};
    }
}
