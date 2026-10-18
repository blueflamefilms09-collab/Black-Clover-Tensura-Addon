package com.newuniverse.nusmp.client.skilltree.layout;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.line.LineGate;
import com.newuniverse.nusmp.skilltree.rules.TreeState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;

/**
 * Why: the Passives panel is a Tensura-style list of every passive and stat bonus the player has bought, grouped by line, each with an on/off switch. A player who owns many lines (an administrator may own all
 * 283) can have thousands of rows, so the panel must not lay out, measure or draw rows it is not showing. This is the pure model behind it, with no Minecraft type so it is tested at scale: {@link #build}
 * turns the owned routes and the player's levels into a flat array of rows (a header per line, an entry per bought passive node), the equipped line first so the player's working set is at the top and the
 * other lines after it in name order, and an optional search text narrows it; {@link Layout} gives every row a fixed height and a prefix-sum position, so the rows on screen for a scroll offset are found by
 * binary search in O(log n) and drawing walks only those. The rows are plain immutable records created when the model is built (when a purchase, a switch or the filter changes) and never during drawing, so
 * a frame allocates nothing for the list. Entries of lines that are not equipped are marked so the screen can dim them: their passives are asleep.
 */
public final class PassiveRows {
    private PassiveRows() {}

    /** A row of the panel: a line header (route set, {@code node} null) or an entry (a bought passive or stat node). */
    public record Row(boolean header, SkillRoute route, SkillNode node, String line, String name, String desc, int color, int level, boolean on, boolean equipped, boolean stat, int count) {
        /** The key of the entry's switch, {@code <route>#<node>} (the header has none). */
        public String key() { return header ? "" : SkillRoute.key(route.id(), node.id()); }
    }

    public static final int HEADER_H = 14, ENTRY_H = 20;

    /**
     * Builds the rows. Only routes the player owns AND whose full tree is known (a stub has no nodes) contribute; only bought nodes that carry a switch. {@code filter} (may be empty) keeps the entries
     * whose name, description or line name contains it (case-insensitive), and drops a header whose entries all went. An equipped line comes first; the rest by line name then id.
     */
    public static List<Row> build(List<SkillRoute> routes, TreeState player, BiPredicate<String, String> toggleOn, String filter) {
        String f = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        List<SkillRoute> owned = new ArrayList<>();
        for (SkillRoute r : routes) if (player.hasRoute(r.id()) && !r.nodes().isEmpty()) owned.add(r);
        String active = player.activeClass();
        owned.sort(Comparator.<SkillRoute>comparingInt(r -> !r.classId().isEmpty() && r.classId().equals(active) ? 0 : 1).thenComparing(r -> r.name().toLowerCase(Locale.ROOT)).thenComparing(SkillRoute::id));
        List<Row> out = new ArrayList<>();
        for (SkillRoute r : owned) {
            boolean equipped = LineGate.equipped(player, r);
            List<Row> entries = new ArrayList<>();
            for (SkillNode n : r.nodes()) {
                if (!n.hasToggle()) continue;
                int lv = player.level(r.id(), n.id());
                if (lv < 1) continue;
                if (!f.isEmpty() && !(n.name().toLowerCase(Locale.ROOT).contains(f) || n.desc().toLowerCase(Locale.ROOT).contains(f) || r.name().toLowerCase(Locale.ROOT).contains(f))) continue;
                entries.add(new Row(false, r, n, r.name(), n.name(), n.desc(), r.color() & 0xFFFFFF, lv, toggleOn.test(r.id(), n.id()), equipped, SkillNode.TYPE_ATTR.equals(n.type()), 0));
            }
            if (entries.isEmpty()) continue;
            out.add(new Row(true, r, null, r.name(), r.name(), r.desc(), r.color() & 0xFFFFFF, 0, true, equipped, false, entries.size()));
            out.addAll(entries);
        }
        return out;
    }

    /** The number of entries (not headers) in a row list. */
    public static int entries(List<Row> rows) {
        int n = 0;
        for (Row r : rows) if (!r.header()) n++;
        return n;
    }

    /** Fixed row heights and their prefix sums, so any scroll offset maps to a row in O(log n). */
    public static final class Layout {
        private final int[] top;
        private final int total;

        public Layout(List<Row> rows) {
            top = new int[rows.size() + 1];
            int y = 0;
            for (int i = 0; i < rows.size(); i++) {
                top[i] = y;
                y += rows.get(i).header() ? HEADER_H : ENTRY_H;
            }
            top[rows.size()] = y;
            total = y;
        }

        public int rows() { return top.length - 1; }

        /** Total content height in pixels. */
        public int total() { return total; }

        /** The y (from the top of the content) of row {@code i}. */
        public int top(int i) { return top[i]; }

        public int height(int i) { return top[i + 1] - top[i]; }

        /** The index of the row at content y, or -1 when y is outside the content. */
        public int rowAt(int y) {
            if (y < 0 || y >= total || rows() == 0) return -1;
            int lo = 0, hi = rows() - 1;
            while (lo < hi) {
                int mid = (lo + hi + 1) >>> 1;
                if (top[mid] <= y) lo = mid;
                else hi = mid - 1;
            }
            return lo;
        }

        /** The scroll offset kept legal for a view of {@code viewH} pixels. */
        public int clampScroll(int scroll, int viewH) { return Math.max(0, Math.min(Math.max(0, total - viewH), scroll)); }
    }
}
