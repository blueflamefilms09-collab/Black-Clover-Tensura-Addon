package com.newuniverse.nusmp.client.skilltree;

import com.newuniverse.nusmp.client.skilltree.layout.NodeState;
import com.newuniverse.nusmp.client.skilltree.layout.Rect;
import com.newuniverse.nusmp.client.skilltree.layout.RouteGrouping;
import com.newuniverse.nusmp.client.skilltree.layout.TreeText;
import com.newuniverse.nusmp.client.ui.theme.SageLayout;
import com.newuniverse.nusmp.client.ui.theme.UiBar;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rules.TreeState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the left column answers the first question a player has, "which of these magics do I have", before any tree is looked at. It lists every route of the synced registry grouped by
 * family (a family is whatever the data says, so a new anime appears under its own heading with no code), and for each route shows what matters at a glance: its icon and name in its
 * own colour, whether it is unlocked, active or sealed (a padlock over the icon and "LOCKED" for a route whose book has not been read), and how much of it the player has learned
 * (a bar, and the count when there is room for it). Route names are data and can be long ("Lightning Dragon Slayer"), so a row is as tall as its wrapped name needs (one to three
 * lines) instead of cutting names to fit a fixed row; the rows are rebuilt every frame from the data it is handed, because the list is a few dozen rows at most and a rebuild is
 * cheaper than the bookkeeping that would keep a cached list in step with a sync packet, and it means an unlock that arrives while the screen is open shows in the very next frame.
 * The class owns the scroll offset and the row geometry because the click handler needs the same rows again. Rows are plain fills and text clipped by a scissor; the only per-route
 * cost is the icon.
 */
final class RouteList {
    static final int HEADING_H = 12, GAP = 1, ICON_COLUMN = 22, MAX_NAME_LINES = 3;

    private record Row(String heading, SkillRoute route, String[] name, int y, int h) {}

    private int scroll;
    private List<Row> rows = List.of();
    private int contentH;

    /** The width the name text of a row may use inside a list area of width {@code areaW}. */
    static int textWidth(int areaW) { return Math.max(20, areaW - 6 - ICON_COLUMN - 2); }

    /** Rebuilds the rows from the groups for a list area {@code areaW} wide. Cheap, called every frame. */
    private List<RouteGrouping.Group> builtFrom;
    private int builtW = -1;

    void build(List<RouteGrouping.Group> groups, Font font, int areaW) {
        if (groups == builtFrom && areaW == builtW) return;             // 1.82.0: the rows are rebuilt only when the grouped list (or the width) changed, not every frame
        builtFrom = groups;
        builtW = areaW;
        SageLayout.Metrics m = font::width;
        int tw = textWidth(areaW);
        List<Row> out = new ArrayList<>();
        int y = 0;
        for (RouteGrouping.Group group : groups) {
            out.add(new Row(headingOf(group.magic()), null, new String[0], y, HEADING_H));
            y += HEADING_H;
            for (SkillRoute r : group.routes()) {
                String[] lines = SageLayout.wrap(SageLayout.clamp(r.name(), m, tw, MAX_NAME_LINES), m, tw);
                int n = Math.max(1, Math.min(MAX_NAME_LINES, lines.length));
                int h = 16 + 9 * n;
                out.add(new Row(null, r, lines, y, h));
                y += h + GAP;
            }
        }
        rows = out;
        contentH = y;
    }

    private static String headingOf(String magic) {
        String key = SkillTreeLang.MAGIC + magic;
        String text = Language.getInstance().has(key) ? SkillTreeLang.str(key) : TreeText.prettify(magic);
        return (text.isEmpty() ? SkillTreeLang.str(SkillTreeLang.ROUTES) : text).toUpperCase(Locale.ROOT);
    }

    /** Scrolls by {@code dy} pixels (positive = further down), kept inside the content. */
    void scrollBy(int dy, int viewH) { scroll = clamp(scroll + dy, viewH); }

    /** Brings a route into view (the screen selected it by key or by default). */
    void reveal(SkillRoute route, int viewH) {
        for (Row r : rows) {
            if (r.route() != route) continue;
            if (r.y() < scroll) scroll = r.y() - HEADING_H;
            else if (r.y() + r.h() > scroll + viewH) scroll = r.y() + r.h() - viewH + 2;
            scroll = clamp(scroll, viewH);
            return;
        }
    }

    private int clamp(int v, int viewH) { return Math.max(0, Math.min(Math.max(0, contentH + 2 - viewH), v)); }

    /** The route whose row is under the mouse, or null. */
    SkillRoute routeAt(double mx, double my, Rect area) {
        if (!area.contains(mx, my)) return null;
        int cy = (int) my - area.y() - 2 + scroll;
        for (Row r : rows) if (r.route() != null && cy >= r.y() && cy < r.y() + r.h()) return r.route();
        return null;
    }

    /** Draws the list inside {@code area}. */
    void draw(GuiGraphics g, Font font, Rect area, TreeState player, String selectedId, float now, int mx, int my) {
        scroll = clamp(scroll, area.h());
        g.enableScissor(area.x() + 1, area.y() + 1, area.right() - 1, area.bottom() - 1);
        int x = area.x() + 3, w = area.w() - 6;
        for (Row r : rows) {
            int y = area.y() + 2 + r.y() - scroll;
            if (y + r.h() < area.y() || y > area.bottom()) continue;
            if (r.route() == null) {
                TreeSkin.heading(g, font, font.plainSubstrByWidth(r.heading(), w - 2), x + 1, y, w - 2, UiTheme.GOLD & 0xFFFFFF);
                continue;
            }
            drawRoute(g, font, new Rect(x, y, w, r.h()), r, player, r.route().id().equals(selectedId), now, mx, my);
        }
        g.disableScissor();
        if (contentH + 2 > area.h()) {
            int total = contentH + 2;
            int bar = Math.max(12, (area.h() - 2) * area.h() / total);
            int span = Math.max(1, total - area.h());
            TreeSkin.scrollbar(g, area.right() - 5, area.y() + 1, area.h() - 2, (int) ((area.h() - 2 - bar) * (scroll / (float) span)), bar);
        }
    }

    private void drawRoute(GuiGraphics g, Font font, Rect r, Row row, TreeState player, boolean selected, float now, int mx, int my) {
        SkillRoute route = row.route();
        boolean unlocked = player.hasRoute(route.id());
        boolean active = unlocked && !route.classId().isEmpty() && route.classId().equals(player.activeClass());
        boolean hover = r.contains(mx, my);
        int rgb = unlocked ? route.color() & 0xFFFFFF : TreeSkin.SEALED & 0xFFFFFF;
        TreeSkin.chip(g, r, selected, hover, selected ? rgb : (UiTheme.CYAN & 0xFFFFFF));
        // the icon, dimmed and padlocked when the route is sealed
        int iy = r.y() + (r.h() - 16) / 2;
        g.renderItem(ItemIcons.of(route.icon(), ""), r.x() + 3, iy);
        if (!unlocked) {
            g.pose().pushPose();
            g.pose().translate(0f, 0f, 200f);                                    // items are drawn at a depth of about 150: the dimming and the padlock go over them
            g.fill(r.x() + 3, iy, r.x() + 19, iy + 16, UiMath.withAlpha(0x05070E, 0.55f));
            TreeSkin.padlock(g, r.x() + 8, iy + 3, UiMath.withAlpha(0xFFFFFF, 0.8f));
            g.pose().popPose();
        }
        // the name, one to three lines
        int tx = r.x() + ICON_COLUMN, tw = r.w() - ICON_COLUMN - 2;
        int nameColor = !unlocked ? TreeSkin.faint() : active ? TreeSkin.gold() : TreeSkin.opaque(rgb);
        int n = Math.min(MAX_NAME_LINES, row.name().length);
        for (int i = 0; i < n; i++) g.drawString(font, row.name()[i], tx, r.y() + 2 + i * 9, nameColor, false);
        // the state line
        int sy = r.y() + 2 + 9 * n;
        if (unlocked) {
            int owned = NodeState.ownedNodes(player, route), total = route.nodes().size();
            String state = SkillTreeLang.str(active ? SkillTreeLang.STATE_ACTIVE : SkillTreeLang.STATE_UNLOCKED).toUpperCase(Locale.ROOT);
            g.drawString(font, font.plainSubstrByWidth(state, tw), tx, sy, active ? TreeSkin.gold() : TreeSkin.good(), false);
            String count = owned + "/" + total;
            if (font.width(state) + 6 + font.width(count) <= tw) g.drawString(font, count, tx + tw - font.width(count), sy, TreeSkin.dim(), false);
            float frac = total == 0 ? 0f : owned / (float) total;
            if (TreeSkin.anime()) UiBar.draw(g, r.x() + 4, r.bottom() - 4, r.w() - 8, 2, frac, rgb, now, 1f);
            else {
                g.fill(r.x() + 4, r.bottom() - 4, r.right() - 4, r.bottom() - 2, 0xFF2A2A38);
                g.fill(r.x() + 4, r.bottom() - 4, r.x() + 4 + Math.round((r.w() - 8) * frac), r.bottom() - 2, TreeSkin.opaque(rgb));
            }
        } else {
            g.drawString(font, font.plainSubstrByWidth(SkillTreeLang.str(SkillTreeLang.STATE_LOCKED).toUpperCase(Locale.ROOT), tw), tx, sy, UiMath.mix(TreeSkin.bad(), TreeSkin.dim(), 0.4f), false);
        }
    }
}
