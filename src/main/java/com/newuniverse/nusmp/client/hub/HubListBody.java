package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the pages of the hub that are a list (Status, Quests, Multiverse, Settings, and the lock page) share one body: cards (a coloured title, fitted lines, an optional bar) followed by clickable rows (a label, a
 * short note, an optional on / off switch). It is cooked once when the page opens or its data changes ({@link Builder#build}): every text is fitted to the width with {@link HubText#fit} and the full text of a cut line is
 * kept for the tooltip. A frame only draws arrays: no string, list or record is built. Local coordinates: (0, 0) is the top-left of the scrolled content, the screen translates and clips. Everything is drawn only when it
 * is inside the visible window {@code [top, bottom]}, so a long page costs what is on screen.
 */
final class HubListBody {
    static final int PAD = 4, TITLE_H = 11, LINE_H = 10, BAR_H = 8, CARD_GAP = 4, ROW_H = 14, ROW_PITCH = 16, SECTION_GAP = 6;

    /** What a row's click needs from the screen. */
    interface Host {
        /** The hub itself (the parent of every screen a row opens). */
        net.minecraft.client.gui.screens.Screen screen();

        /** Opens a screen that returns to the hub through its own parent argument. */
        void open(net.minecraft.client.gui.screens.Screen child);

        /** Opens a screen that has no parent: the hub comes back when it closes to the game. */
        void openWatched(net.minecraft.client.gui.screens.Screen child);

        /** The page's data changed (a switch was flipped): cook it again. */
        void refresh();
    }

    static final class Card {
        final int rgb;
        final HubText.Fit title;
        final HubText.Fit[] lines;
        final float bar;
        final String barText;
        final int h;

        Card(int rgb, HubText.Fit title, HubText.Fit[] lines, float bar, String barText) {
            this.rgb = rgb;
            this.title = title;
            this.lines = lines;
            this.bar = bar;
            this.barText = barText;
            this.h = PAD + TITLE_H + lines.length * LINE_H + (bar >= 0f ? BAR_H : 0) + PAD - 1;
        }
    }

    static final class Row {
        final HubText.Fit label;
        final String note;
        final int noteW, rgb;
        final boolean enabled, toggle, on;
        final Runnable action;

        Row(HubText.Fit label, String note, int noteW, int rgb, boolean enabled, boolean toggle, boolean on, Runnable action) {
            this.label = label;
            this.note = note;
            this.noteW = noteW;
            this.rgb = rgb;
            this.enabled = enabled;
            this.toggle = toggle;
            this.on = on;
            this.action = action;
        }
    }

    private final Card[] cards;
    private final Row[] rows;
    private final List<FormattedCharSequence> lockLines;
    private final int w, height, rowsTop;

    private HubListBody(int w, Card[] cards, Row[] rows, List<FormattedCharSequence> lockLines) {
        this.w = w;
        this.cards = cards;
        this.rows = rows;
        this.lockLines = lockLines;
        int y = 0;
        for (Card c : cards) y += c.h + CARD_GAP;
        if (cards.length > 0 && rows.length > 0) y += SECTION_GAP - CARD_GAP;
        this.rowsTop = y;
        y += rows.length * ROW_PITCH;
        this.height = lockLines != null ? 96 + lockLines.size() * (9 + 1) : Math.max(0, y) + 2;
    }

    int height() { return height; }

    boolean isLock() { return lockLines != null; }

    // ---------------------------------------------------------------- drawing
    void draw(GuiGraphics g, Font font, int top, int bottom, int mx, int my, float now) {
        if (lockLines != null) {
            drawLock(g, font, now);
            return;
        }
        int y = 0;
        for (Card c : cards) {
            if (y + c.h >= top && y <= bottom) drawCard(g, font, c, y, now);
            y += c.h + CARD_GAP;
        }
        for (int i = 0; i < rows.length; i++) {
            int ry = rowsTop + i * ROW_PITCH;
            if (ry + ROW_H >= top && ry <= bottom) drawRow(g, font, rows[i], ry, mx >= 0 && mx < w && my >= ry && my < ry + ROW_H, now);
        }
    }

    private void drawCard(GuiGraphics g, Font font, Card c, int y, float now) {
        UiPanel.well(g, 0, y, w, c.h, 0xFF000000 | c.rgb, 1f);
        g.fill(0, y, 2, y + c.h, UiMath.withAlpha(c.rgb, 0.9f));
        g.drawString(font, c.title.shown(), 6, y + PAD, 0xFF000000 | c.rgb, false);
        if (c.barText != null) g.drawString(font, c.barText, w - 4 - font.width(c.barText), y + PAD, UiTheme.TEXT_DIM, false);
        int ly = y + PAD + TITLE_H;
        for (HubText.Fit l : c.lines) {
            g.drawString(font, l.shown(), 6, ly, UiTheme.TEXT, false);
            ly += LINE_H;
        }
        if (c.bar >= 0f) UiBar.draw(g, 6, ly + 1, w - 12, 4, c.bar, 0xFF000000 | c.rgb, now, 1f);
    }

    private void drawRow(GuiGraphics g, Font font, Row r, int y, boolean hover, float now) {
        boolean hot = hover && r.enabled;
        UiPanel.chip(g, 0, y, w, ROW_H, r.toggle && r.on, hot, 0xFF000000 | r.rgb);
        if (hot) UiPanel.hover(g, 1, y + 1, w - 2, ROW_H - 2, 1f, 0xFF000000 | r.rgb);
        int color = r.enabled ? UiTheme.TEXT : UiTheme.TEXT_FAINT;
        g.drawString(font, r.label.shown(), 5, y + (ROW_H - 8) / 2, color, false);
        if (!r.note.isEmpty()) g.drawString(font, r.note, w - 5 - r.noteW, y + (ROW_H - 8) / 2, r.toggle ? (r.on ? UiTheme.GREEN : UiTheme.TEXT_DIM) : UiTheme.TEXT_DIM, false);
    }

    private void drawLock(GuiGraphics g, Font font, float now) {
        float pulse = UiMath.pulse(now, 80f, 0f);
        g.pose().pushPose();
        g.pose().translate(w / 2f, 34f, 0f);
        g.pose().scale(3f, 3f, 1f);
        HubFx.lock(g, 0, 0, UiMath.withAlpha(UiTheme.GOLD, 0.6f + 0.3f * pulse));
        g.pose().popPose();
        int y = 70;
        for (FormattedCharSequence l : lockLines) {
            g.drawString(font, l, (w - font.width(l)) / 2, y, UiTheme.TEXT_DIM, false);
            y += 10;
        }
    }

    // ---------------------------------------------------------------- input and tooltips
    /** The row under the local point, or null. */
    Row rowAt(double lx, double ly) {
        if (lockLines != null || lx < 0 || lx >= w) return null;
        for (int i = 0; i < rows.length; i++) {
            int ry = rowsTop + i * ROW_PITCH;
            if (ly >= ry && ly < ry + ROW_H) return rows[i];
        }
        return null;
    }

    /** Runs the row's action when it is enabled; returns true when the click was on a row. */
    boolean click(double lx, double ly) {
        Row r = rowAt(lx, ly);
        if (r == null) return false;
        if (r.enabled && r.action != null) r.action.run();
        return true;
    }

    /** The full text of a cut line (or row label) under the local point, or null. */
    String tipAt(double lx, double ly) {
        if (lockLines != null || lx < 0 || lx >= w) return null;
        int y = 0;
        for (Card c : cards) {
            if (ly >= y && ly < y + c.h) {
                if (ly < y + PAD + TITLE_H) return c.title.cut() ? c.title.full() : null;
                int i = (int) ((ly - y - PAD - TITLE_H) / LINE_H);
                return i >= 0 && i < c.lines.length && c.lines[i].cut() ? c.lines[i].full() : null;
            }
            y += c.h + CARD_GAP;
        }
        Row r = rowAt(lx, ly);
        return r != null && r.label.cut() ? r.label.full() : null;
    }

    // ---------------------------------------------------------------- building (once per open or data change)
    static final class Builder {
        private final Font font;
        private final int w;
        private final List<Card> cards = new ArrayList<>();
        private final List<Row> rows = new ArrayList<>();

        Builder(Font font, int w) {
            this.font = font;
            this.w = w;
        }

        HubText.Fit fit(String s, int max) { return HubText.fit(s, max, font::width); }

        /** A card: {@code lines} are fitted to the card's inner width; {@code bar} below 0 draws no bar, {@code barText} (may be null) sits at the right of the title line. */
        Builder card(int rgb, String title, List<String> lines, float bar, String barText) {
            int inner = w - 12;
            int titleMax = barText == null ? inner : inner - font.width(barText) - 6;
            HubText.Fit[] fits = new HubText.Fit[lines.size()];
            for (int i = 0; i < fits.length; i++) fits[i] = fit(lines.get(i), inner);
            cards.add(new Card(rgb, fit(title, titleMax), fits, bar, barText));
            return this;
        }

        Builder row(int rgb, String label, String note, boolean enabled, Runnable action) { return row(rgb, label, note, enabled, false, false, action); }

        /** A row with an on / off switch: the note shows the state. */
        Builder toggle(int rgb, String label, String stateText, boolean on, Runnable action) { return row(rgb, label, stateText, true, true, on, action); }

        private Builder row(int rgb, String label, String note, boolean enabled, boolean toggle, boolean on, Runnable action) {
            String n = note == null ? "" : note;
            int noteW = n.isEmpty() ? 0 : font.width(n);
            rows.add(new Row(fit(label, w - 10 - (noteW == 0 ? 0 : noteW + 8)), n, noteW, rgb, enabled, toggle, on, action));
            return this;
        }

        HubListBody build() { return new HubListBody(w, cards.toArray(new Card[0]), rows.toArray(new Row[0]), null); }

        /** The lock page: a padlock and the hint, wrapped to the width. */
        HubListBody lock(String hint) {
            return new HubListBody(w, new Card[0], new Row[0], font.split(Component.literal(hint), w - 16));
        }
    }
}
