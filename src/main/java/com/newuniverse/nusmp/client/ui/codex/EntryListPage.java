package com.newuniverse.nusmp.client.ui.codex;

import com.newuniverse.nusmp.client.discovery.DiscoveryDraw;
import com.newuniverse.nusmp.client.ui.CodexContext;
import com.newuniverse.nusmp.client.ui.CodexPage;
import com.newuniverse.nusmp.client.ui.UiDraw;
import com.newuniverse.nusmp.client.ui.theme.CodexSkin;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.core.discovery.DiscoveryCache;
import com.newuniverse.nusmp.core.discovery.DiscoveryRules;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Why: the one {@link CodexPage} behind every generated tab (Rules and the other data sections, Spells, Blocks, Entities, Magic, Effects). A page
 * of this kind owns no content: it asks a {@code Supplier<List<CodexBlock>>} and lays what comes back out in two columns ({@link CodexLayout}),
 * so a new tab is a supplier, not a new drawing class. The supplier is asked again when the Codex opens ({@link #opened}), when the loaded
 * data changed ({@link CodexData#get()} generation) and, for pages that follow live state (spells, rule values, discovery), every
 * {@code refreshTicks} game ticks while open (40 = two seconds). {@link #contentHeight} and {@link #render} read the same layout, so the scroll
 * range always matches what is drawn. Undiscovered rows are drawn as a "???" silhouette with nothing of the real entry (the discovery rule).
 */
public final class EntryListPage implements CodexPage {
    private final String title, glyph;
    private final Supplier<List<CodexBlock>> source;
    private final int refreshTicks;

    /** A row after translation and the discovery check; text that is not shown is never kept. */
    private record Laid(String title, String text, String note, ItemStack icon, boolean shown, int height) {}

    private record LaidBlock(String header, List<Laid> rows, int height) {}

    private List<LaidBlock> laid;
    private CodexLayout.Placement placement;
    private long builtAt = Long.MIN_VALUE;
    private int builtWidth = -1, builtGeneration = -1, laidWidth = -1;

    public EntryListPage(String title, String glyph, int refreshTicks, Supplier<List<CodexBlock>> source) {
        this.title = title; this.glyph = glyph; this.refreshTicks = refreshTicks; this.source = source;
    }

    @Override public String title() { return title; }
    @Override public String glyph() { return glyph; }

    /** The Codex was opened: forget what was built, the registries and the player may have changed since. */
    @Override public void opened() { laid = null; builtAt = Long.MIN_VALUE; }

    // ---------------------------------------------------------------- building
    private void ensure(CodexContext ctx, int width) {
        long now = ctx.mc.level == null ? 0 : ctx.mc.level.getGameTime();
        int gen = CodexData.get().generation();
        boolean stale = laid == null || gen != builtGeneration || (refreshTicks > 0 && (now - builtAt >= refreshTicks || now < builtAt));
        if (stale) {
            builtAt = now;
            builtGeneration = gen;
            rebuild();
            laidWidth = -1;
        }
        if (laidWidth != width) { measure(ctx.font(), width); laidWidth = width; }
    }

    private void rebuild() {
        List<CodexBlock> blocks;
        try { blocks = source.get(); } catch (RuntimeException | LinkageError e) { blocks = List.of(CodexBlock.single(CodexRow.of("", "This page could not be read."))); }
        if (blocks == null || blocks.isEmpty()) blocks = List.of(CodexBlock.single(CodexRow.of("", "Nothing recorded here yet.")));
        List<LaidBlock> out = new ArrayList<>();
        for (CodexBlock b : blocks) {
            List<Laid> rows = new ArrayList<>();
            for (CodexRow r : b.rows()) {
                boolean shown = CodexData.shown(r.keys(), DiscoveryCache::visibleAny);
                rows.add(shown ? new Laid(tr(r.title()), tr(r.text()), r.note(), r.icon(), true, 0)
                        : new Laid(DiscoveryRules.QUESTION, DiscoveryDraw.hint(), "", ItemStack.EMPTY, false, 0));
            }
            out.add(new LaidBlock(tr(b.header()), rows, 0));
        }
        laid = out;
    }

    /** A lang key is translated when it exists; any other text (and a key nobody translated) stays as written. */
    static String tr(String s) {
        if (s == null || s.isEmpty()) return "";
        if (CodexData.looksLikeLangKey(s) && I18n.exists(s)) return I18n.get(s);
        return s;
    }

    private int cw(int width) { return width / 2 - 8; }

    private int rowHeight(Font font, Laid r, int cw) {
        int h = 22 + UiDraw.wrappedHeight(font, r.text(), cw - 12);
        if (!r.note().isEmpty()) h += UiDraw.wrappedHeight(font, r.note(), cw - 12) + 2;
        return h + 8;
    }

    private void measure(Font font, int width) {
        int cw = cw(width);
        List<LaidBlock> sized = new ArrayList<>();
        int[] heights = new int[laid.size()];
        for (int i = 0; i < laid.size(); i++) {
            LaidBlock b = laid.get(i);
            List<Laid> rows = new ArrayList<>();
            int h = b.header().isEmpty() ? 0 : font.lineHeight + 7;
            for (Laid r : b.rows()) {
                int rh = rowHeight(font, r, cw);
                rows.add(new Laid(r.title(), r.text(), r.note(), r.icon(), r.shown(), rh));
                h += rh;
            }
            sized.add(new LaidBlock(b.header(), rows, h));
            heights[i] = h;
        }
        laid = sized;
        placement = CodexLayout.place(heights, 0);
    }

    // ---------------------------------------------------------------- CodexPage
    @Override
    public int contentHeight(CodexContext ctx, int width) {
        ensure(ctx, width);
        return placement.total();
    }

    @Override
    public void render(CodexContext ctx, GuiGraphics g, int x, int y, int w, int h, int scroll, int mx, int my, float time) {
        ensure(ctx, w);
        Font font = ctx.font();
        int cw = cw(w);
        for (int i = 0; i < laid.size(); i++) {
            LaidBlock b = laid.get(i);
            int bx = placement.column()[i] == 0 ? x : x + w / 2 + 8;
            int top = y - scroll + placement.y()[i];
            if (top + b.height() < y - 2 || top > y + h) continue;                      // only what is on the page is drawn
            int cy = top;
            if (!b.header().isEmpty()) cy = UiDraw.header(g, font, b.header(), bx, cy, cw - 6);
            for (Laid r : b.rows()) {
                if (cy + r.height() >= y - 2 && cy <= y + h) drawRow(g, font, r, bx, cy, cw, mx, my);
                cy += r.height();
            }
        }
    }

    private void drawRow(GuiGraphics g, Font font, Laid r, int bx, int top, int cw, int mx, int my) {
        if (!r.shown()) {                                                                // undiscovered: a silhouette and ???, nothing of the entry
            DiscoveryDraw.icon(g, font, bx, top);
            g.drawString(font, DiscoveryRules.QUESTION, bx + 20, top + 4, UiDraw.inkDim(), false);
            g.hLine(bx, bx + cw - 6, top + 18, UiDraw.rule());
            UiDraw.wrapped(g, font, r.text(), bx + 2, top + 22, cw - 12, UiDraw.inkDim());
            return;
        }
        boolean icon = !r.icon().isEmpty();
        if (icon) {
            if (UiTheme.anime()) CodexSkin.row(g, bx - 2, top - 1, 20, 18, mx, my, false);
            g.renderItem(r.icon(), bx, top);
        }
        int tx = icon ? bx + 20 : bx;
        if (!r.title().isEmpty()) g.drawString(font, font.plainSubstrByWidth(r.title(), cw - 26), tx, top + 4, UiDraw.goldInk(), false);
        g.hLine(bx, bx + cw - 6, top + 18, UiDraw.rule());
        int ly = UiDraw.wrapped(g, font, r.text(), bx + 2, top + 22, cw - 12, UiDraw.ink());
        if (!r.note().isEmpty()) UiDraw.wrapped(g, font, r.note(), bx + 2, ly + 2, cw - 12, UiDraw.inkDim());
    }
}
