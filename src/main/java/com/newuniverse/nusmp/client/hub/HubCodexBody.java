package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.heroweapon.forms.BookLayout.Rect;
import com.newuniverse.nusmp.client.ui.CodexContext;
import com.newuniverse.nusmp.client.ui.CodexPage;
import com.newuniverse.nusmp.client.ui.CodexPages;
import com.newuniverse.nusmp.client.ui.UiDraw;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: Hub v2 replaces the Codex book with the hub (docs/codex.md, "Hub v2"): every page the book had, the stock pages and every data section found at run time, is drawn by the hub. This class is the Codex tab of the
 * hub: it takes the very same {@link CodexPage}s from {@link CodexPages#all()} (nothing is copied; a page that is registered later, or a data section a resource pack adds, shows up by itself), lists them in a left
 * navigation rail grouped into categories ({@link HubCodexNav}: You, Collection, Magic, World, Reference, each page with its own icon), and draws the picked page into the hub's body rectangle through
 * {@link CodexPage#render} and {@link CodexPage#click}, with a {@link CodexContext} that has no book screen. The hub screen owns the scrolling (from {@link #contentHeight}); this class owns the rail (its scroll, its
 * rows, its hover) and the page choice. Everything is client only and read only. Cost: the rail draws only the rows that are on screen (about 12 rows: an icon quad, a string and a chip each), the body is whatever the
 * page draws (the same as in the book), nothing is allocated per frame (labels and row tops are cooked when the Codex opens).
 */
final class HubCodexBody {
    /** The colour of each category, indexed by {@link HubCodexNav.Cat#ordinal()}; the same colours the icons are painted in. */
    private static final int[] CAT_RGB = {0x5CD8FF, 0xE8C468, 0x9A7CFF, 0x7CF0B0, 0x8FD0FF};
    private static final int ICON = 12, CAT_ICON = 10, PAD = 3;

    private final CodexContext ctx = new CodexContext();
    private List<CodexPage> pages = List.of();
    private List<HubCodexNav.Row> rows = List.of();
    private String[] ids = new String[0];
    private int[] rowTop = new int[0];
    private HubText.Fit[] label = new HubText.Fit[0], head = new HubText.Fit[HubCodexNav.Cat.values().length];
    private boolean[] failed = new boolean[0];
    private String[] iconName = new String[0];                                     // codex_<id> per page, cat_<key> per category: built once, a frame builds no string
    private final String[] catIcon = new String[HubCodexNav.Cat.values().length];
    private int page;
    private float railScroll;
    private String failText = "";

    // ---------------------------------------------------------------- opening
    /** Takes the pages and builds the rail. {@code startTitle} (may be null) picks the page whose tab title is that (the Wiki book opens on "Wiki"); otherwise the first page of the rail. */
    void opened(Font font, String startTitle) {
        CodexPages.opened();
        pages = CodexPages.all();
        int n = pages.size();
        ids = new String[n];
        failed = new boolean[n];
        iconName = new String[n];
        List<String> idList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            CodexPage p = pages.get(i);
            ids[i] = HubCodexNav.idOf(p.title(), p.getClass().getSimpleName());
            idList.add(ids[i]);
            iconName[i] = "codex_" + ids[i];
        }
        for (HubCodexNav.Cat c : HubCodexNav.Cat.values()) catIcon[c.ordinal()] = "cat_" + c.key;
        rows = HubCodexNav.rows(idList);
        rowTop = new int[rows.size()];
        int y = 0;
        for (int i = 0; i < rowTop.length; i++) {
            rowTop[i] = y;
            y += HubCodexNav.rowH(rows.get(i));
        }
        int labelW = HubLayout.SUBRAIL.w() - (PAD + ICON + 5) - 6;
        label = new HubText.Fit[n];
        for (int i = 0; i < n; i++) label[i] = HubText.fit(pages.get(i).title(), labelW, font::width);
        for (HubCodexNav.Cat c : HubCodexNav.Cat.values())
            head[c.ordinal()] = HubText.fit(I18n.get("anime_multiverse.hub.cat." + c.key).toUpperCase(Locale.ROOT), HubLayout.SUBRAIL.w() - (PAD + CAT_ICON + 5) - 4, font::width);
        failText = I18n.get("anime_multiverse.hub.codex.failed");
        page = firstPage();
        if (startTitle != null) {
            for (int i = 0; i < n; i++) if (startTitle.equalsIgnoreCase(pages.get(i).title())) { page = i; break; }
        }
        railScroll = 0f;
        keepVisible();
    }

    private int firstPage() {
        for (HubCodexNav.Row r : rows) if (!r.header()) return r.page();
        return 0;
    }

    boolean ready() { return !pages.isEmpty(); }

    /** Tells the pages where the window sits (see {@link CodexContext#scissor}). Call when the screen is laid out. */
    void setView(HubLayout.View v) {
        ctx.viewScale = v.scale();
        ctx.viewX = v.ox();
        ctx.viewY = v.oy();
    }

    int page() { return page; }

    /** The id ({@link HubCodexNav#idOf}) of the open page, or "" when there are none. */
    String pageId() { return ready() ? ids[page] : ""; }

    CodexContext ctx() { return ctx; }

    int pageCount() { return pages.size(); }

    String pageTitle(int i) { return pages.get(i).title(); }

    String idOf(int i) { return ids[i]; }

    /** The pages in rail order (page indices), for tests and the footer; a fresh list. */
    List<Integer> railOrder() {
        List<Integer> out = new ArrayList<>();
        for (HubCodexNav.Row r : rows) if (!r.header()) out.add(r.page());
        return out;
    }

    // ---------------------------------------------------------------- choosing
    void select(int p) {
        if (!ready()) return;
        page = Mth.clamp(p, 0, pages.size() - 1);
        keepVisible();
    }

    void step(int dir) { if (ready()) select(HubCodexNav.step(rows, page, dir)); }

    private void keepVisible() {
        if (!ready()) return;
        int at = HubCodexNav.rowOfPage(rows, page);
        if (at < 0) return;
        int top = rowTop[at > 0 && rows.get(at - 1).header() ? at - 1 : at], bottom = rowTop[at] + HubCodexNav.rowH(rows.get(at)), vis = HubLayout.SUBRAIL.h();
        if (top < railScroll) railScroll = top;
        else if (bottom > railScroll + vis) railScroll = bottom - vis;
        clampRail();
    }

    private int railMax() { return Math.max(0, HubCodexNav.height(rows) - HubLayout.SUBRAIL.h()); }

    private void clampRail() { railScroll = Mth.clamp(railScroll, 0f, railMax()); }

    /** The wheel over the rail. */
    void scrollRail(double amount) {
        railScroll = Mth.clamp(railScroll - (float) amount * 18f, 0f, railMax());
    }

    /** The page whose rail row is at stage point (sx, sy), or -1 (also for a heading or a point outside the rail). */
    int pageAt(double sx, double sy) {
        Rect r = HubLayout.SUBRAIL;
        if (!r.contains(sx, sy)) return -1;
        int row = HubCodexNav.rowAt(rows, (int) (sy - r.y() + railScroll));
        return row < 0 || rows.get(row).header() ? -1 : rows.get(row).page();
    }

    /** The full title of a cut rail label under the pointer, or null. */
    String tipAt(double sx, double sy) {
        int p = pageAt(sx, sy);
        return p >= 0 && label[p].cut() ? label[p].full() : null;
    }

    // ---------------------------------------------------------------- the body
    int contentHeight(int w) { return ready() ? pages.get(page).contentHeight(ctx, w) : 0; }

    /** Draws the open page into {@code body} (stage coordinates, the caller has clipped to it and set the pose); (mx, my) is the pointer in the same coordinates. */
    void drawBody(GuiGraphics g, Font font, Rect body, int scroll, float mx, float my, float now) {
        if (!ready()) return;
        if (!UiTheme.anime()) g.fill(body.x() - 2, body.y() - 2, body.right() + 2, body.bottom() + 2, UiDraw.PAPER);      // the classic look's pages are dark ink: give them their paper
        if (failed[page]) {
            g.drawString(font, failText, body.x() + 4, body.y() + 4, UiTheme.TEXT_DIM, false);
            return;
        }
        try {
            pages.get(page).render(ctx, g, body.x(), body.y(), body.w(), body.h(), scroll, Math.round(mx), Math.round(my), now);
        } catch (RuntimeException | LinkageError e) {
            failed[page] = true;                                                                    // a page that cannot draw must not take the hub down; say so once per open
        }
    }

    /** A click inside the body (stage coordinates); true when the page used it. */
    boolean click(double sx, double sy, int button, Rect body) {
        if (!ready() || failed[page]) return false;
        try {
            return pages.get(page).click(ctx, sx, sy, button, body.x(), body.y(), body.w(), body.h());
        } catch (RuntimeException | LinkageError e) {
            failed[page] = true;
            return false;
        }
    }

    // ---------------------------------------------------------------- the rail
    /**
     * Draws the rail into {@link HubLayout#SUBRAIL}: a well, then the visible rows. {@code clip} is the rail in screen coordinates (the caller computes it from the view), because a scissor takes screen units.
     */
    void drawRail(GuiGraphics g, Font font, int[] clip, float mx, float my, float alpha) {
        Rect r = HubLayout.SUBRAIL;
        UiPanel.well(g, r.x() - 2, r.y() - 2, r.w() + 4, r.h() + 4, 0xFF000000 | UiTheme.CYAN, alpha);
        g.enableScissor(clip[0], clip[1], clip[2], clip[3]);
        boolean icons = HubIcons.on();
        int hover = pageAt(mx, my), scroll = Math.round(railScroll);
        for (int i = 0; i < rows.size(); i++) {
            HubCodexNav.Row row = rows.get(i);
            int y = r.y() + rowTop[i] - scroll, h = HubCodexNav.rowH(row);
            if (y + h <= r.y() || y >= r.bottom()) continue;
            int rgb = CAT_RGB[row.cat().ordinal()];
            if (row.header()) {
                if (icons) HubIcons.draw(g, catIcon[row.cat().ordinal()], r.x() + PAD + CAT_ICON / 2, y + h / 2 + 1, CAT_ICON, alpha, 0f);
                g.drawString(font, head[row.cat().ordinal()].shown(), r.x() + PAD + CAT_ICON + 4, y + 2, UiMath.withAlpha(rgb, 0.9f * alpha), false);
                g.fill(r.x() + 2, y + h - 1, r.right() - 2, y + h, UiMath.withAlpha(rgb, 0.2f * alpha));
                continue;
            }
            boolean on = row.page() == page, over = row.page() == hover;
            if (on || over) UiPanel.chip(g, r.x() + 1, y, r.w() - 2, h - 1, on, over, 0xFF000000 | rgb);
            int cy = y + h / 2;
            if (icons) HubIcons.draw(g, iconName[row.page()], r.x() + PAD + 2 + ICON / 2, cy, ICON, alpha, on ? 1f : over ? 0.6f : 0f);
            else g.drawCenteredString(font, pages.get(row.page()).glyph(), r.x() + PAD + 2 + ICON / 2, cy - 4, UiMath.withAlpha(rgb, alpha));
            g.drawString(font, label[row.page()].shown(), r.x() + PAD + ICON + 5, cy - 4, UiMath.withAlpha(on ? UiTheme.WHITE : over ? UiTheme.TEXT : UiTheme.TEXT_DIM, alpha), false);
        }
        g.disableScissor();
        int max = railMax();
        if (max > 0) {
            int bar = Math.max(10, r.h() * r.h() / (max + r.h())), pos = (int) ((r.h() - bar) * (railScroll / max));
            g.fill(r.right() - 2, r.y() + pos, r.right(), r.y() + pos + bar, UiMath.withAlpha(UiTheme.CYAN, 0.7f * alpha));
        }
    }
}
