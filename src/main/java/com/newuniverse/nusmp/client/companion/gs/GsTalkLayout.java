package com.newuniverse.nusmp.client.companion.gs;

/**
 * Where everything on the Great Spirit talk screen sits, as pure arithmetic (no Minecraft type), so the painter, the clicks and the tests share one answer and the screen fits any GUI scale. The panel is at
 * most 376 by 236 and shrinks to the window with a 4 pixel margin. A column on the left holds the portrait (a framed bust), her name plate, the rapport meter and her mood; the column on the right holds the
 * four tabs, the box with her words, the widgets of the shown tab and the Close button. Every rectangle is an {@code int[4]} of x, y, width, height. {@link #hit} says what is under a point.
 */
public final class GsTalkLayout {
    public static final int TABS = 4, MAX_W = 376, MAX_H = 236, MARGIN = 4, PAD = 8, ROW_H = 14, ROW_PITCH = 15, SLOT = 18, BIG_SLOT = 22;

    public final int guiW, guiH, tab, width, height, leftW, portraitScale, tabW, dialogueLines, storyLines, visibleRows, rowsW, pouchPitch, tasteOff;
    public final boolean wide;
    public final int[] panel, portrait, portraitInner, plate, subLine, meter, moodChip, tabs, dialogue, close, hintLine, list, medal;
    public final int[] giftSlot, give, likes, tastes, stanceArea, storyArea;
    public final int[][] stanceBtn = new int[3][], tile = new int[3][];
    public final int[] grid, ring, use, put, takeAll, strip, picker;
    public final boolean ringShown, stripShown, compactBond;
    private final int pickerCols = 9;
    private final int[] scratch = new int[4];

    private GsTalkLayout(int guiW, int guiH, int tab) {
        this.guiW = guiW;
        this.guiH = guiH;
        this.tab = Math.max(0, Math.min(TABS - 1, tab));
        int w = Math.max(176, Math.min(MAX_W, guiW - 2 * MARGIN)), h = Math.max(130, Math.min(MAX_H, guiH - 2 * MARGIN));
        width = w;
        height = h;
        int x = (guiW - w) / 2, y = (guiH - h) / 2;
        wide = w >= 300;
        leftW = w >= 300 ? 100 : w >= 220 ? 72 : 56;
        panel = new int[]{x, y, w, h};

        // ---- left column
        int px = x + PAD;
        int fh = Math.max(32, Math.min(wide ? 96 : 70, h - 88));
        portrait = new int[]{px, y + PAD, leftW, fh};
        portraitInner = new int[]{px + 3, y + PAD + 3, leftW - 6, fh - 6};
        portraitScale = PortraitPlan.scaleFor(leftW - 6, fh - 6, 7);
        int med = wide ? 40 : 30;
        medal = new int[]{px + leftW - med + 6, y + PAD + fh - med + 8, med, med};
        plate = new int[]{px, y + PAD + fh + 4, leftW, 14};
        subLine = new int[]{px, plate[1] + 16, leftW, 10};
        meter = new int[]{px, subLine[1] + 12, leftW, 28};
        moodChip = new int[]{px, meter[1] + 31, leftW, 12};

        // ---- right column
        int rx = px + leftW + 8, rw = w - leftW - 3 * PAD;
        tabs = new int[]{rx, y + PAD, rw, 16};
        tabW = rw / TABS;
        dialogueLines = this.tab == 0 ? (h >= 200 ? 4 : h >= 170 ? 3 : 2) : this.tab == 3 ? (h >= 200 ? 2 : 1) : (h >= 160 ? 2 : 1);
        int dh = 14 + dialogueLines * 10 + 4;
        dialogue = new int[]{rx, tabs[1] + tabs[3] + 4, rw, dh};
        int ct = dialogue[1] + dh + 4;
        int footerY = y + h - PAD - 14;
        int cb = footerY - 4;
        close = new int[]{rx + rw - 56, footerY, 56, 14};
        hintLine = new int[]{rx, footerY, rw - 62, 14};

        // ---- plain rows (Talk: under the box; Interact: under the tiles; Bond: in the story area when there are any)
        int room = Math.max(0, cb - ct);
        compactBond = room < 96;
        int listTop = this.tab == 0 ? ct : this.tab == 1 ? ct + 38 : this.tab == 2 ? ct + (compactBond ? 54 : 78) : cb;
        int listH = Math.max(0, cb - listTop);
        list = new int[]{rx, listTop, rw, listH};
        visibleRows = Math.max(1, listH / ROW_PITCH);
        rowsW = rw;

        // ---- interact tiles
        int tw = (rw - 8) / 3;
        for (int i = 0; i < 3; i++) tile[i] = new int[]{rx + i * (tw + 4), ct, tw, 34};

        // ---- bond tab
        tasteOff = compactBond ? 11 : 14;
        giftSlot = new int[]{rx + 2, ct + (compactBond ? 1 : 4), 26, 26};
        give = new int[]{rx + 34, ct + (compactBond ? 0 : 2), 66, compactBond ? 13 : 16};
        likes = new int[]{rx + 34, ct + (compactBond ? 14 : 22), 66, compactBond ? 13 : 16};
        tastes = new int[]{rx + 106, ct, Math.max(0, rw - 106), compactBond ? 28 : 40};
        int sw = (rw - 8) / 3;
        int sy = ct + (compactBond ? 30 : 44), sh = compactBond ? 22 : 28;
        stanceArea = new int[]{rx, sy, rw, sh + 2};
        for (int i = 0; i < 3; i++) stanceBtn[i] = new int[]{rx + i * (sw + 4), sy, sw, sh};
        int storyTop = ct + (compactBond ? 54 : 78), storyH = Math.max(0, cb - storyTop);
        storyArea = new int[]{rx, storyTop, rw, storyH};
        storyLines = storyH >= 22 ? Math.min(BondStory.MAX, (storyH - 11) / 10) : 0;

        // ---- pouch tab
        // the nine slots take the biggest pitch (22, 20 or 18) that still leaves room for the hotbar strip under them; when even 18 leaves none the strip is left out and "Put" opens the picker
        int pitch = 18;
        boolean withStrip = false;
        for (int p : new int[]{BIG_SLOT, 20, 18}) {
            if (room >= 3 * p + 22 && rw >= 9 * SLOT) {
                pitch = p;
                withStrip = true;
                break;
            }
        }
        if (!withStrip) pitch = room >= 3 * BIG_SLOT ? BIG_SLOT : room >= 60 ? 20 : 18;
        pouchPitch = pitch;
        stripShown = withStrip;
        grid = new int[]{rx, ct, 3 * pitch, 3 * pitch};
        ringShown = rw >= 3 * pitch + 108 && room >= 58;                               // the ring (52 wide) and buttons at least 44 wide beside the slots
        int rxr = rx + 3 * pitch + 8;
        ring = ringShown ? new int[]{rxr, ct, 52, 58} : new int[]{0, 0, 0, 0};
        int bx = ringShown ? rxr + 56 : rxr;
        int bw = Math.max(0, rx + rw - bx);
        int bh = room >= 64 ? 16 : 13, bp = bh + 4;
        use = new int[]{bx, ct, bw, bh};
        put = new int[]{bx, ct + bp, bw, bh};
        takeAll = new int[]{bx, ct + 2 * bp, bw, bh};
        strip = new int[]{rx, ct + 3 * pitch + 4, 9 * SLOT, SLOT};

        // ---- the picker popup (inventory 9 by 3 plus the hotbar row), under the gift slot and kept inside the panel
        int pw = pickerCols * SLOT + 8, ph = 12 + 4 * SLOT + 4 + 8;
        int pxx = Math.max(x + 2, Math.min(rx, x + w - pw - 2));
        int pyy = Math.max(y + 2, Math.min(ct + 32, y + h - ph - 2));
        picker = new int[]{pxx, pyy, pw, ph};
    }

    /** Lays the screen out for a window of {@code guiW} by {@code guiH} (GUI pixels) and the shown tab (0 Talk, 1 Interact, 2 Bond, 3 Pouch). */
    public static GsTalkLayout compute(int guiW, int guiH, int tab) { return new GsTalkLayout(guiW, guiH, tab); }

    public static boolean in(int[] r, int mx, int my) { return r != null && r[2] > 0 && r[3] > 0 && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]; }

    // ---------------------------------------------------------------- rectangles of the repeated parts
    // Each has a form that fills a caller's array (the painter's, so a frame allocates nothing) and a convenience form that returns a new one.
    private static int[] set(int[] o, int x, int y, int w, int h) {
        o[0] = x;
        o[1] = y;
        o[2] = w;
        o[3] = h;
        return o;
    }

    public int[] tabRect(int i, int[] o) { return set(o, tabs[0] + i * tabW, tabs[1], tabW - 2, tabs[3]); }

    public int[] tabRect(int i) { return tabRect(i, new int[4]); }

    public int[] rowRect(int slot, int[] o) { return set(o, list[0], list[1] + slot * ROW_PITCH, rowsW - 6, ROW_H); }

    public int[] rowRect(int slot) { return rowRect(slot, new int[4]); }

    public int[] pouchSlot(int i, int[] o) { return set(o, grid[0] + (i % 3) * pouchPitch, grid[1] + (i / 3) * pouchPitch, pouchPitch - 1, pouchPitch - 1); }

    public int[] pouchSlot(int i) { return pouchSlot(i, new int[4]); }

    public int[] hotbarSlot(int i, int[] o) { return set(o, strip[0] + i * SLOT, strip[1], SLOT - 1, SLOT - 1); }

    public int[] hotbarSlot(int i) { return hotbarSlot(i, new int[4]); }

    /** Picker slot for inventory index 0..35: indexes 9..35 are the three rows above, 0..8 the hotbar row under them. */
    public int[] pickerSlot(int index, int[] o) {
        if (index >= 9) {
            int k = index - 9;
            return set(o, picker[0] + 4 + (k % 9) * SLOT, picker[1] + 12 + (k / 9) * SLOT, SLOT - 1, SLOT - 1);
        }
        return set(o, picker[0] + 4 + index * SLOT, picker[1] + 12 + 3 * SLOT + 4, SLOT - 1, SLOT - 1);
    }

    public int[] pickerSlot(int index) { return pickerSlot(index, new int[4]); }

    /** Notebook item i (0..7): a row of 16 pixel cells in the tastes box, under its label. */
    public int[] tasteSlot(int i, int[] o) { return set(o, tastes[0] + 2 + i * 17, tastes[1] + tasteOff, 16, 16); }

    public int[] tasteSlot(int i) { return tasteSlot(i, new int[4]); }

    /** The most notebook cells that fit the tastes box. */
    public int tasteCells() { return Math.max(0, Math.min(8, (tastes[2] - 4) / 17)); }

    // ---------------------------------------------------------------- what is under a point
    /**
     * The {@link GsHit} code of what is at the point, given what the frame offers. The picker (when open) takes the whole point first; every widget that the frame does not offer cannot be hit.
     */
    public int hit(int mx, int my, GsTalkFrame f) {
        if (f.pickerOpen) {
            if (in(picker, mx, my)) {
                for (int i = 0; i < 36; i++) if (in(pickerSlot(i, scratch), mx, my)) return GsHit.PICKER + i;
                return GsHit.DIALOGUE;                                                       // the popup's own surface: swallows the click
            }
        }
        if (in(close, mx, my)) return GsHit.CLOSE;
        for (int i = 0; i < TABS; i++) if (in(tabRect(i, scratch), mx, my)) return GsHit.TAB + i;
        if (in(portrait, mx, my)) return GsHit.PORTRAIT;
        if (in(meter, mx, my)) return GsHit.METER;
        if (in(moodChip, mx, my)) return GsHit.MOOD;
        if (in(dialogue, mx, my)) return GsHit.DIALOGUE;
        switch (tab) {
            case 1 -> {
                if (f.hasFive && in(tile[0], mx, my)) return GsHit.FIVE;
                if (f.hasFollow && in(tile[1], mx, my)) return GsHit.FOLLOW;
                if (f.hasPanel && in(tile[2], mx, my)) return GsHit.PANEL;
            }
            case 2 -> {
                if (f.hasGift) {
                    if (in(giftSlot, mx, my)) return GsHit.GIFT_SLOT;
                    if (in(give, mx, my)) return GsHit.GIVE;
                    for (int i = 0; i < Math.min(tasteCells(), f.tasteReactions.size()); i++) if (in(tasteSlot(i, scratch), mx, my)) return GsHit.TASTE + i;
                }
                if (f.hasLikes && in(likes, mx, my)) return GsHit.LIKES;
                if (f.stanceOffered) for (int i = 0; i < 3; i++) if (in(stanceBtn[i], mx, my)) return GsHit.STANCE + i;
            }
            case 3 -> {
                for (int i = 0; i < 9; i++) if (in(pouchSlot(i, scratch), mx, my)) return GsHit.POUCH + i;
                if (f.hasUse && in(use, mx, my)) return GsHit.USE;
                if (f.hasPut && in(put, mx, my)) return GsHit.PUT;
                if (f.hasTakeAll && in(takeAll, mx, my)) return GsHit.TAKE_ALL;
                if (in(ring, mx, my)) return GsHit.RING;
                if (stripShown) for (int i = 0; i < 9; i++) if (in(hotbarSlot(i, scratch), mx, my)) return GsHit.HOTBAR + i;
            }
            default -> {}
        }
        if (tab != 3 && !(tab == 2 && f.rows.isEmpty())) {
            for (int s = 0; s < visibleRows; s++) {
                int i = f.scroll + s;
                if (i >= f.rows.size()) break;
                if (in(rowRect(s, scratch), mx, my)) return GsHit.ROW + i;
            }
        }
        return GsHit.NONE;
    }

    /** The largest first-shown index for a list of {@code count} rows. */
    public int maxScroll(int count) { return Math.max(0, count - visibleRows); }

    /** The rectangle of whatever a hit code names (for the ripple and the tooltips), or null when it has none. */
    public int[] rectOf(int hit, GsTalkFrame f) {
        if (hit == GsHit.CLOSE) return close;
        if (GsHit.isTab(hit)) return tabRect(hit - GsHit.TAB);
        if (hit == GsHit.GIFT_SLOT) return giftSlot;
        if (hit == GsHit.GIVE) return give;
        if (hit == GsHit.LIKES) return likes;
        if (hit == GsHit.FIVE) return tile[0];
        if (hit == GsHit.FOLLOW) return tile[1];
        if (hit == GsHit.PANEL) return tile[2];
        if (GsHit.isStance(hit)) return stanceBtn[hit - GsHit.STANCE];
        if (hit == GsHit.USE) return use;
        if (hit == GsHit.PUT) return put;
        if (hit == GsHit.TAKE_ALL) return takeAll;
        if (hit == GsHit.RING) return ring;
        if (GsHit.isPouch(hit)) return pouchSlot(hit - GsHit.POUCH);
        if (GsHit.isHotbar(hit)) return hotbarSlot(hit - GsHit.HOTBAR);
        if (GsHit.isPicker(hit)) return pickerSlot(hit - GsHit.PICKER);
        if (GsHit.isTaste(hit)) return tasteSlot(hit - GsHit.TASTE);
        if (GsHit.isRow(hit)) {
            int slot = GsHit.rowIndex(hit) - f.scroll;
            return slot >= 0 && slot < visibleRows ? rowRect(slot) : null;
        }
        if (hit == GsHit.PORTRAIT) return portrait;
        if (hit == GsHit.DIALOGUE) return dialogue;
        if (hit == GsHit.METER) return meter;
        if (hit == GsHit.MOOD) return moodChip;
        return null;
    }
}
