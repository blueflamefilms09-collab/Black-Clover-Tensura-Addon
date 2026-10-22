package com.newuniverse.nusmp.client.engraving;

import com.newuniverse.nusmp.client.ui.theme.ThemedButton;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.engraving.codex.CodexEntries;
import com.newuniverse.nusmp.engraving.codex.CodexFilter;
import com.newuniverse.nusmp.engraving.codex.CodexLayout;
import com.newuniverse.nusmp.tooltip.UnifiedTooltipRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The Engravement Codex, opened by the {@code engraving_codex} item: a dark two-page book (the relic tooltip's palette: deep glass, a thin cyan edge, gold brackets, the slate and green of
 * the tooltip's stat lines) listing every engraving of the mod with what it does, its cooldowns and caps, and which items carry it. On the left a contents list (all, this mod's own
 * enchantments, Tensura Reincarnated's engravings, then one section per franchise) and a search box; the pages turn with a short leaf animation, the arrow buttons, the keys and the wheel.
 * <p>
 * Cost: the entries are laid out ONCE per open or resize ({@link CodexLayout}: wrapped strings and heights), the filter and the pagination run only when the section or the search text
 * changes, and a frame only draws the strings of the (at most) two visible pages plus the turning leaf. No allocation per frame beyond what {@link GuiGraphics} itself does, no world or
 * entity access, no packet. The frame is drawn in {@link #renderBackground} (flat, never the blurred vanilla background), so the search box and the buttons sit on top of it.
 */
public class EngravingCodexScreen extends Screen {
    private static final int FLIP_TICKS = 12, GAP = 8, FOUND_MAX = 6, ROW_H = 12, STRIP_W = 132;
    /** The relic tooltip's slate for secondary lines, its green for the cap lines, gold for names. */
    private static final int SLATE = UnifiedTooltipRules.COLOUR_TYPE, FOOT = UnifiedTooltipRules.COLOUR_FOOTER, GREEN = UnifiedTooltipRules.COLOUR_STAT_PLUS;

    private final Screen parent;
    private final CodexEntries.Book book;
    private CodexLayout.Laid[] laid = new CodexLayout.Laid[0];
    private String[] rowLabel = new String[0], rowCount = new String[0];
    private int[] rowColour = new int[0];
    private int rows;                                   // 1 + groups: row 0 is "All"

    private String groupId = "", query = "";
    private int[] shown = new int[0];
    private List<int[]> pages = List.of(new int[]{0, 0});
    private int spread, prev = -1, dir, listScroll;
    private float flipAge = -1f, time;
    private int bx, by, bw, bh, pw, pageH;
    private int pagesX, spineX;
    /** Text scale of the entries and the page width, page height and gap in that scaled space (see {@link CodexLayout#textScale}). */
    private float ts = 1f;
    private int vWidth, vPageH, vGap;
    private EditBox search;
    private ThemedButton prevBtn, nextBtn;
    private String allLabel = "", noneText = "", closeText = "", footerHint = "", titleText = "";
    /** The strings and widths of the footer, rebuilt when the filter or the spread changes (never per frame). */
    private String countLine = "", pageLine = "", hintLine = "", noneLine = "";
    private int countW, pageW, hintX;
    private int[] rowCountW = new int[0];
    private CodexLayout.Words words = CodexLayout.Words.ENGLISH;

    public EngravingCodexScreen(Screen parent, CodexEntries.Book book) {
        super(Component.translatableWithFallback("item.nusmp.engraving_codex", "Engravement Codex"));
        this.parent = parent;
        this.book = book;
    }

    private static String tr(String key, String fallback, Object... args) { return Component.translatableWithFallback(key, fallback, args).getString(); }

    @Override
    protected void init() {
        bw = Math.min(width - 16, 600);
        bh = Math.min(height - 44, 380);
        bx = (width - bw) / 2;
        by = Math.max(4, (height - bh - 22) / 2);
        pagesX = bx + 8 + STRIP_W + 10;
        pw = (bx + bw - 8 - pagesX - 14) / 2;
        spineX = pagesX + pw + 7;
        pageH = bh - 24 - 22;
        ts = CodexLayout.textScale(pageH);                                           // the entries are laid out and drawn in a space 1 / ts times bigger
        vWidth = Math.round((pw - 8) / ts);
        vPageH = Math.round(pageH / ts);
        vGap = Math.round(GAP / ts);
        titleText = getTitle().getString();
        allLabel = tr("nusmp.engraving_codex.all", "All engravings");
        noneText = tr("nusmp.engraving_codex.none", "No engraving matches your search.");
        closeText = tr("nusmp.engraving_codex.close", "Close");
        footerHint = tr("nusmp.engraving_codex.hint", "Wheel, arrows or PageUp / PageDown turn the page.");
        words = new CodexLayout.Words(tr("nusmp.engraving_codex.found", "Found on"), tr("nusmp.engraving_codex.more", "+%d more"),
                tr("nusmp.engraving_codex.found_none", "no item of this mod carries it yet"), "...");
        layout();
        buildRows();
        String keep = search == null ? query : search.getValue();
        search = new EditBox(font, bx + 8 + 4, by + 24, STRIP_W - 8, 12, Component.translatableWithFallback("nusmp.engraving_codex.search", "Search"));
        search.setBordered(false);
        search.setMaxLength(CodexFilter.MAX_QUERY);
        search.setTextColor(UiTheme.TEXT);
        search.setHint(Component.translatableWithFallback("nusmp.engraving_codex.search_hint", "Search...").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        search.setValue(keep);
        search.setResponder(this::onQuery);                                         // the filter runs on a text change only
        addRenderableWidget(search);
        setInitialFocus(search);
        prevBtn = addRenderableWidget(new ThemedButton(bx + 4, by + bh + 4, 22, 16, Component.literal("<"), b -> go(spread - 1)));
        nextBtn = addRenderableWidget(new ThemedButton(bx + 30, by + bh + 4, 22, 16, Component.literal(">"), b -> go(spread + 1)));
        addRenderableWidget(new ThemedButton(bx + bw - 64, by + bh + 4, 60, 16, Component.literal(closeText), b -> onClose()));
        query = keep;
        refilter(false);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    // ---------------------------------------------------------------- measuring (once per init)
    /** Lays out every entry for the current page width: wrapped strings and heights (the only place text is split). */
    private void layout() {
        List<CodexEntries.Entry> all = book.entries();
        laid = new CodexLayout.Laid[all.size()];
        for (int i = 0; i < laid.length; i++) laid[i] = CodexLayout.lay(i, all.get(i), vWidth, vPageH, FOUND_MAX, words, font::width);
    }

    private void buildRows() {
        List<CodexEntries.Group> groups = book.groups();
        rows = groups.size() + 1;
        rowLabel = new String[rows];
        rowCount = new String[rows];
        rowCountW = new int[rows];
        rowColour = new int[rows];
        rowLabel[0] = allLabel;
        rowCount[0] = Integer.toString(book.entries().size());
        rowColour[0] = UiTheme.CYAN;
        for (int i = 0; i < groups.size(); i++) {
            CodexEntries.Group g = groups.get(i);
            rowCount[i + 1] = Integer.toString(g.count());
            rowCountW[i + 1] = font.width(rowCount[i + 1]);
            rowLabel[i + 1] = font.plainSubstrByWidth(g.shortName(), STRIP_W - 24 - rowCountW[i + 1]);
            rowColour[i + 1] = 0xFF000000 | g.colour();
        }
        rowCountW[0] = font.width(rowCount[0]);
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, rows - visibleRows()));
    }

    private int listTop() { return by + 42; }

    private int visibleRows() { return Math.max(1, (by + bh - 8 - listTop()) / ROW_H); }

    // ---------------------------------------------------------------- filtering (on a change only)
    private void onQuery(String text) {
        query = text == null ? "" : text;
        refilter(true);
    }

    private void selectRow(int row) {
        String id = row <= 0 ? "" : book.groups().get(row - 1).id();
        if (id.equals(groupId)) return;
        groupId = id;
        refilter(true);
    }

    /** Re-runs the filter and the pagination; {@code reset} goes back to the first spread. */
    private void refilter(boolean reset) {
        shown = CodexFilter.select(book.entries(), groupId, query);
        int[] heights = new int[shown.length];
        for (int i = 0; i < shown.length; i++) heights[i] = laid[shown[i]].height();
        pages = CodexFilter.paginate(heights, vPageH, vGap);
        if (reset) { spread = 0; prev = -1; flipAge = -1f; }
        spread = Mth.clamp(spread, 0, spreadCount() - 1);
        noneLine = font.plainSubstrByWidth(noneText, pw);
        updateLines();
    }

    /** Rebuilds the footer strings and their positions (on a filter change or a page turn, not per frame). */
    private void updateLines() {
        countLine = shown.length == book.entries().size() ? tr("nusmp.engraving_codex.count", "%s engravings", shown.length)
                : tr("nusmp.engraving_codex.count_filtered", "%s of %s shown", shown.length, book.entries().size());
        pageLine = (spread + 1) + " / " + spreadCount();
        countW = font.width(countLine);
        pageW = font.width(pageLine);
        hintX = pagesX + 8 + countW + 12;
        int hintW = bx + bw - 24 - pageW - 12 - hintX;
        hintLine = hintW > 60 ? font.plainSubstrByWidth(footerHint, hintW) : "";
    }

    private int spreadCount() { return CodexFilter.spreads(pages.size()); }

    // ---------------------------------------------------------------- turning
    private void go(int target) {
        target = Mth.clamp(target, 0, spreadCount() - 1);
        if (target == spread || flipAge >= 0f) return;
        prev = spread;
        dir = target > spread ? 1 : -1;
        spread = target;
        flipAge = 0f;
        updateLines();
    }

    @Override
    public void tick() {
        time += 1f;
        if (flipAge >= 0f) { flipAge += 1f; if (flipAge >= FLIP_TICKS) { flipAge = -1f; prev = -1; } }
        if (prevBtn != null) prevBtn.active = spread > 0;
        if (nextBtn != null) nextBtn.active = spread < spreadCount() - 1;
    }

    private boolean overStrip(double mx, double my) { return mx >= bx + 8 && mx < bx + 8 + STRIP_W && my >= listTop() && my < by + bh - 8; }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy == 0) return super.mouseScrolled(mx, my, sx, sy);
        if (overStrip(mx, my)) {
            listScroll = Mth.clamp(listScroll + (sy < 0 ? 2 : -2), 0, Math.max(0, rows - visibleRows()));
            return true;
        }
        go(spread + (sy < 0 ? 1 : -1));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean typing = search != null && search.isFocused();
        switch (key) {
            case GLFW.GLFW_KEY_PAGE_DOWN -> { go(spread + 1); return true; }
            case GLFW.GLFW_KEY_PAGE_UP -> { go(spread - 1); return true; }
            default -> { }
        }
        if (!typing) {
            switch (key) {
                case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_DOWN -> { go(spread + 1); return true; }
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_UP -> { go(spread - 1); return true; }
                case GLFW.GLFW_KEY_HOME -> { go(0); return true; }
                case GLFW.GLFW_KEY_END -> { go(spreadCount() - 1); return true; }
                default -> { }
            }
        }
        return super.keyPressed(key, scan, mods);                                   // ESC closes; the search box takes the letters
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && overStrip(mx, my)) {
            int row = listScroll + (int) ((my - listTop()) / ROW_H);
            if (row >= 0 && row < rows && row < listScroll + visibleRows()) {
                selectRow(row);
                return true;
            }
        }
        if (button == 0 && mx < pagesX + 14 && mx >= pagesX && my > by + bh - 22 && my < by + bh) { go(spread - 1); return true; }                // the page corners
        if (button == 0 && mx > bx + bw - 22 && mx < bx + bw - 8 && my > by + bh - 22 && my < by + bh) { go(spread + 1); return true; }
        return super.mouseClicked(mx, my, button);
    }

    // ---------------------------------------------------------------- drawing
    /** The flat backdrop and the whole book. The search box and the buttons are drawn over this by {@code super.render}. */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        UiPanel.scrim(g, width, height, 1f);
        drawFrame(g);
        drawStrip(g, mx, my);
        int top = by + 24, mid = spineX;
        if (flipAge < 0f) {
            half(g, spread, 0, 0f);
            half(g, spread, 1, 0f);
        } else {
            float t = Mth.clamp((flipAge + partial) / FLIP_TICKS, 0f, 1f);
            half(g, dir > 0 ? prev : spread, 0, 0f);
            half(g, dir > 0 ? spread : prev, 1, 0f);
            float s = t < 0.5f ? 1f - t * 2f : t * 2f - 1f;
            s = s * s * (3f - 2f * s);
            boolean oldLeaf = t < 0.5f;
            int leafSpread = oldLeaf ? prev : spread;
            int leafHalf = dir > 0 ? (oldLeaf ? 1 : 0) : (oldLeaf ? 0 : 1);
            g.pose().pushPose();
            g.pose().translate(mid, 0, 0);
            g.pose().scale(Math.max(0.01f, s), 1f, 1f);
            g.pose().translate(-mid, 0, 0);
            half(g, leafSpread, leafHalf, 1f);
            g.pose().popPose();
            int sx0 = leafHalf == 0 ? Math.round(mid - (pw + 7) * s) : mid, sx1 = leafHalf == 0 ? mid : Math.round(mid + (pw + 7) * s);
            g.fill(sx0, top, sx1, top + pageH, UiMath.withAlpha(0x000000, 0.28f * (1f - s)));
        }
        drawFurniture(g);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);                                           // the book is drawn in renderBackground; this puts the search box and the buttons over it
    }

    /** The relic tooltip's dark glass frame: a soft shadow, a deep blue-black body, a thin cyan edge, gold inner line and brackets. */
    private void drawFrame(GuiGraphics g) {
        g.fill(bx + 3, by + 4, bx + bw + 3, by + bh + 4, 0x55000000);
        g.fillGradient(bx, by, bx + bw, by + bh, 0xF2101A30, 0xF2070B16);
        g.renderOutline(bx - 1, by - 1, bw + 2, bh + 2, UiMath.withAlpha(UiTheme.CYAN, 0.12f));
        g.renderOutline(bx, by, bw, bh, UiMath.withAlpha(UiTheme.CYAN, 0.7f));
        g.renderOutline(bx + 3, by + 3, bw - 6, bh - 6, UiMath.withAlpha(UiTheme.GOLD, 0.45f));
        UiPanel.brackets(g, bx, by, bw, bh, 10, UiMath.withAlpha(UiTheme.GOLD, 0.95f));
    }

    /** The title, the search well and the contents list (rows, a scrollbar when the list is longer than the strip). */
    private void drawStrip(GuiGraphics g, int mx, int my) {
        int sx = bx + 8;
        g.drawString(font, titleText, sx, by + 9, UiTheme.GOLD, false);
        g.fill(sx, by + 20, sx + STRIP_W, by + 21, UiMath.withAlpha(UiTheme.GOLD, 0.35f));
        UiPanel.well(g, sx, by + 22, STRIP_W, 16, search != null && search.isFocused() ? UiTheme.CYAN : UiTheme.TEXT_FAINT, 1f);
        int top = listTop(), vis = visibleRows();
        for (int i = 0; i < vis && listScroll + i < rows; i++) {
            int r = listScroll + i, y = top + i * ROW_H;
            boolean active = r == 0 ? groupId.isEmpty() : book.groups().get(r - 1).id().equals(groupId);
            boolean hover = mx >= sx && mx < sx + STRIP_W - 5 && my >= y && my < y + ROW_H;
            UiPanel.chip(g, sx, y, STRIP_W - 5, ROW_H - 1, active, hover, rowColour[r]);
            g.fill(sx + 3, y + 3, sx + 6, y + ROW_H - 4, rowColour[r]);
            g.drawString(font, rowLabel[r], sx + 9, y + 2, active ? UiTheme.WHITE : UiTheme.TEXT, false);
            g.drawString(font, rowCount[r], sx + STRIP_W - 8 - rowCountW[r], y + 2, UiTheme.TEXT_DIM, false);
        }
        if (rows > vis) {
            int h = vis * ROW_H, bar = Math.max(10, h * vis / rows), pos = (h - bar) * listScroll / Math.max(1, rows - vis);
            g.fill(sx + STRIP_W - 3, top, sx + STRIP_W, top + h, UiMath.withAlpha(UiTheme.CYAN, 0.18f));
            g.fill(sx + STRIP_W - 3, top + pos, sx + STRIP_W, top + pos + bar, UiMath.withAlpha(UiTheme.CYAN, 0.85f));
        }
    }

    /** The count line, the page number, the hint and the page-corner arrows. */
    private void drawFurniture(GuiGraphics g) {
        int fy = by + bh - 14;
        g.drawString(font, countLine, pagesX + 4, fy, UiTheme.TEXT_DIM, false);
        g.drawString(font, pageLine, bx + bw - 24 - pageW, fy, UiTheme.TEXT_DIM, false);
        if (spread > 0) g.drawString(font, "<", pagesX - 2, fy, UiTheme.CYAN, false);
        if (spread < spreadCount() - 1) g.drawString(font, ">", bx + bw - 18, fy, UiTheme.CYAN, false);
        if (!hintLine.isEmpty()) g.drawString(font, hintLine, hintX, fy, UiTheme.TEXT_FAINT, false);
    }

    /** One half of a spread (0 left, 1 right) on its own dark page, clipped to the half. {@code leaf} darkens the half that is turning. */
    private void half(GuiGraphics g, int sp, int which, float leaf) {
        int top = by + 24, x0 = which == 0 ? pagesX - 4 : spineX, x1 = which == 0 ? spineX : pagesX + 2 * pw + 18;
        g.enableScissor(x0, top - 4, x1, top + pageH + 4);
        g.fillGradient(x0, top - 4, x1, top + pageH + 4, UiMath.withAlpha(0x131E38, 0.78f), UiMath.withAlpha(0x090E1C, 0.82f));
        g.fillGradient(x0, top - 4, x1, top + 26, UiMath.withAlpha(0xFFFFFF, 0.05f), 0x00FFFFFF);
        int sh = which == 0 ? 0x00000000 : UiMath.withAlpha(UiTheme.CYAN, 0.10f), sh2 = which == 0 ? UiMath.withAlpha(UiTheme.CYAN, 0.10f) : 0x00000000;
        g.fillGradient(which == 0 ? x1 - 12 : x0, top - 4, which == 0 ? x1 : x0 + 12, top + pageH + 4, sh, sh2);
        int pi = sp * 2 + which;
        if (shown.length == 0) {
            if (which == 0) g.drawString(font, noneLine, pagesX, top + 6, UiTheme.TEXT_DIM, false);
        } else if (pi < pages.size()) {
            int[] page = pages.get(pi);
            int y = 0;
            g.pose().pushPose();
            g.pose().translate(which == 0 ? pagesX : spineX + 7, top, 0f);              // the entries are drawn in the scaled space of the page
            g.pose().scale(ts, ts, 1f);
            for (int k = 0; k < page[1]; k++) {
                CodexLayout.Laid l = laid[shown[page[0] + k]];
                drawEntry(g, l, 0, y);
                y += l.height() + vGap;
            }
            g.pose().popPose();
        }
        if (leaf > 0f) g.fill(x0, top - 4, x1, top + pageH + 4, UiMath.withAlpha(0x000000, 0.25f * leaf));
        g.disableScissor();
    }

    /** One entry: a bar in its source colour, the name, the source, the scope, the text, the facts (cap lines in green) and the "Found on" lines. */
    private void drawEntry(GuiGraphics g, CodexLayout.Laid l, int x, int y) {
        int col = 0xFF000000 | l.colour();
        g.fill(x, y, x + 2, y + l.height(), col);
        int tx = x + 6, ty = y;
        g.drawString(font, l.name(), tx, ty, UiTheme.GOLD, false);
        ty += CodexLayout.NAME_H;
        g.drawString(font, l.groupLine(), tx, ty, col, false);
        if (!l.scopeInline().isEmpty()) g.drawString(font, l.scopeInline(), tx + l.scopeX(), ty, UiTheme.TEXT_DIM, false);
        ty += CodexLayout.LINE;
        for (String s : l.scope()) { g.drawString(font, s, tx, ty, UiTheme.TEXT_DIM, false); ty += CodexLayout.LINE; }
        ty += CodexLayout.GAP_AFTER_SCOPE;
        for (String s : l.text()) { g.drawString(font, s, tx, ty, UiTheme.TEXT, false); ty += CodexLayout.LINE; }
        if (!l.facts().isEmpty()) {
            ty += CodexLayout.GAP_BLOCK;
            List<String> facts = l.facts();
            for (int i = 0; i < facts.size(); i++) {
                g.drawString(font, facts.get(i), tx, ty, l.factCap()[i] ? GREEN : SLATE, false);
                ty += CodexLayout.LINE;
            }
        }
        if (!l.found().isEmpty()) {
            ty += CodexLayout.GAP_BLOCK;
            for (String s : l.found()) { g.drawString(font, s, tx, ty, FOOT, false); ty += CodexLayout.LINE; }
        }
    }
}
