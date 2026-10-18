package com.newuniverse.nusmp.client.skilltree.layout;

import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.line.LineGate;
import com.newuniverse.nusmp.skilltree.rules.TreeState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Why: the owner wants the arts and passives of a line shown NESTED under that line's entry in Tensura's menu: the line is the parent, its arts and its passives are the children, kept apart and in
 * a fixed order so a player always knows where to look. This is that nesting as a pure function (no Minecraft type, so it is tested at scale on the real route files): a line becomes a flat list of
 * rows, three sections in a fixed order (Arts, Passives, Stat bonuses), each a header followed by its entries in the order of the route file (parents before children). What an entry is depends on
 * ownership, which is the fog of war again: for a line the player OWNS, only the nodes they have BOUGHT are listed (an unbought node is not theirs to see in this menu, the Skill Tree is where they
 * shop), and a passive or stat bonus carries a checkbox whose state is the real switch ({@code toggleOn}); for a line the player does NOT own (an operator's inspect), every node is listed, dimmed, with no
 * checkbox, because nothing there can be switched. {@code asleep} marks the entries of a line that is not the equipped one (the single-active-line rule, {@link LineGate#equipped}): they are still listed and
 * still switchable (a switch is remembered), but drawn dimmed, because their effects are off until the line is equipped. Fixed row heights let the panel find the row under the mouse by simple arithmetic.
 */
public final class MenuNesting {
    private MenuNesting() {}

    public static final int HEADER_H = 14, ROW_H = 20;

    public enum Kind { HEADER_ARTS, HEADER_PASSIVES, HEADER_STATS, ART, STANCE, PASSIVE, STAT }

    /**
     * One row. {@code node} is null for a header; {@code count} is the number of entries of a header; {@code level}/{@code max} are the bought and maximum level of an entry; {@code checkbox} says a box is drawn,
     * {@code checked} its state; {@code dim} says the entry is drawn faded (not owned, or asleep).
     */
    public record Row(Kind kind, SkillNode node, int level, int max, int count, boolean checkbox, boolean checked, boolean dim) {
        public boolean header() { return node == null; }

        public int height() { return header() ? HEADER_H : ROW_H; }
    }

    /** A summary line: how many nodes of the line the player has bought and how many exist (0 of 0 for a card with no tree). */
    public record Summary(int bought, int total) {}

    public static Summary summary(SkillRoute route, TreeState player) {
        if (route == null) return new Summary(0, 0);
        int bought = 0;
        for (SkillNode n : route.nodes()) if (player.level(route.id(), n.id()) >= 1) bought++;
        return new Summary(bought, route.nodes().size());
    }

    /**
     * Builds the rows of one line. {@code owned} is whether the player owns it; {@code toggleOn} answers the real switch of (route, node).
     */
    public static List<Row> build(SkillRoute route, TreeState player, BiPredicate<String, String> toggleOn, boolean owned) {
        List<Row> out = new ArrayList<>();
        if (route == null || route.nodes().isEmpty()) return out;
        boolean asleep = owned && !LineGate.equipped(player, route);
        section(out, route, player, toggleOn, owned, asleep, Kind.HEADER_ARTS);
        section(out, route, player, toggleOn, owned, asleep, Kind.HEADER_PASSIVES);
        section(out, route, player, toggleOn, owned, asleep, Kind.HEADER_STATS);
        return out;
    }

    private static void section(List<Row> out, SkillRoute route, TreeState player, BiPredicate<String, String> toggleOn, boolean owned, boolean asleep, Kind header) {
        List<Row> entries = new ArrayList<>();
        for (SkillNode n : route.nodes()) {
            Kind k = entryKind(n);
            if (k == null || headerOf(k) != header) continue;
            int level = player.level(route.id(), n.id());
            if (owned && level < 1) continue;                                   // an owned line lists only what is bought
            boolean box = owned && n.hasToggle() && level >= 1 && (k == Kind.PASSIVE || k == Kind.STAT);
            entries.add(new Row(k, n, level, n.maxLevel(), 0, box, box && toggleOn.test(route.id(), n.id()), !owned || asleep));
        }
        if (entries.isEmpty()) return;
        out.add(new Row(header, null, 0, 0, entries.size(), false, false, false));
        out.addAll(entries);
    }

    /** The entry kind of a node, or null for a node that only unlocks others (it has nothing to show). */
    public static Kind entryKind(SkillNode n) {
        return switch (n.nodeType()) {
            case ACTIVE -> "stance".equals(n.kind()) ? Kind.STANCE : Kind.ART;
            case PASSIVE -> Kind.PASSIVE;
            case ATTR -> Kind.STAT;
            case PLAIN -> null;
        };
    }

    private static Kind headerOf(Kind entry) {
        return switch (entry) {
            case ART, STANCE -> Kind.HEADER_ARTS;
            case PASSIVE -> Kind.HEADER_PASSIVES;
            case STAT -> Kind.HEADER_STATS;
            default -> entry;
        };
    }

    // ---------------------------------------------------------------- geometry
    /** Total height of the rows in pixels. */
    public static int contentHeight(List<Row> rows) {
        int h = 0;
        for (Row r : rows) h += r.height();
        return h;
    }

    /** The index of the row at content y, or -1. */
    public static int rowAt(List<Row> rows, int y) {
        if (y < 0) return -1;
        int top = 0;
        for (int i = 0; i < rows.size(); i++) {
            int h = rows.get(i).height();
            if (y < top + h) return i;
            top += h;
        }
        return -1;
    }

    /** The y (from the top of the content) of row {@code i}. */
    public static int top(List<Row> rows, int i) {
        int t = 0;
        for (int k = 0; k < i && k < rows.size(); k++) t += rows.get(k).height();
        return t;
    }

    /** The scroll offset kept legal for a view of {@code viewH} pixels. */
    public static int clampScroll(List<Row> rows, int scroll, int viewH) { return Math.max(0, Math.min(Math.max(0, contentHeight(rows) - viewH), scroll)); }
}
