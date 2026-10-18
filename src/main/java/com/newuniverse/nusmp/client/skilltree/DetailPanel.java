package com.newuniverse.nusmp.client.skilltree;

import com.newuniverse.nusmp.client.skilltree.layout.NodeState;
import com.newuniverse.nusmp.client.skilltree.layout.Rect;
import com.newuniverse.nusmp.client.skilltree.layout.TreeText;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.rules.TreeRules.NodeStatus;
import com.newuniverse.nusmp.skilltree.rules.TreeState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Why: the right-hand card is where the screen explains, and everything it says is data the server synced (the node's text, its per-level numbers, its price, its parents), so it is
 * built from the records and never typed in. It is a list of blocks (a heading, a paragraph, a gap) rather than a fixed layout because the amount of text is not known: a node with
 * three numeric tables and two parents is taller than the card on a 240-pixel-high window, so the card scrolls, and a scrolling card needs its total height before it draws. Building
 * the blocks is the expensive part (every paragraph is word-wrapped), so they are cached under a key made of everything the text depends on (route, node, level, SP, parent levels,
 * width, look) and rebuilt only when that key changes; a sync that changes the SP or a parent level changes the key, so the card always shows the current numbers. It draws the
 * three situations the design asks for: a sealed route (what is needed, where to look), an unlocked route with nothing selected (a summary), and a node (its text, the numbers of
 * every level with the player's level marked, the price, and the requirements coloured by whether they are met).
 */
final class DetailPanel {
    private enum BlockKind { HEADING, PARA, GAP, PADLOCK }

    private record Block(BlockKind kind, String text, List<FormattedCharSequence> lines, int color, int height) {}

    private String key = "";
    private List<Block> blocks = List.of();
    private int contentH, scroll;

    // ------------------------------------------------------------------ content
    /** What the card shows: a sealed route, a route summary (node == null), or a node. */
    record Subject(SkillRoute route, SkillNode node, NodeState nodeState, TreeState player, boolean inspect) {
        Subject(SkillRoute route, SkillNode node, NodeState nodeState, TreeState player) { this(route, node, nodeState, player, false); }
    }

    /** Rebuilds the blocks when the subject (or the width, or the look) changed since the last call. */
    void update(Font font, int width, Subject s) {
        String k = keyOf(width, s);
        if (k.equals(key)) return;
        key = k;
        scroll = 0;
        blocks = build(font, width, s);
        int h = 0;
        for (Block b : blocks) h += b.height();
        contentH = h;
    }

    private static String keyOf(int width, Subject s) {
        StringBuilder b = new StringBuilder();
        boolean unlocked = s.player().hasRoute(s.route().id());
        b.append(System.identityHashCode(s.route())).append('|').append(s.node() == null ? "" : s.node().id()).append('|').append(unlocked).append('|').append(s.player().sp()).append('|')
                .append(width).append('|').append(TreeSkin.anime()).append('|').append(s.inspect());
        if (s.nodeState() != null) b.append('|').append(s.nodeState().status()).append(s.nodeState().level());
        if (s.node() != null) for (NodeState.Requirement r : NodeState.requirements(s.player(), s.route(), s.node())) b.append('|').append(r.have());
        if (s.node() == null && unlocked) b.append('|').append(NodeState.ownedNodes(s.player(), s.route()));
        return b.toString();
    }

    private static List<Block> build(Font font, int w, Subject s) {
        List<Block> out = new ArrayList<>();
        SkillRoute r = s.route();
        int rgb = r.color() & 0xFFFFFF;
        boolean owned = s.player().hasRoute(r.id());
        if (!owned && !s.inspect()) {                                    // a sealed route: what it needs and where to find it
            out.add(new Block(BlockKind.PADLOCK, "", List.of(), 0, 22));
            out.add(para(font, w, SkillTreeLang.tr(SkillTreeLang.LOCKED_TITLE).withStyle(ChatFormatting.BOLD), TreeSkin.gold()));
            out.add(gap(4));
            out.add(para(font, w, SkillTreeLang.tr(SkillTreeLang.REQUIRES, r.bookName()), TreeSkin.text()));
            if (!r.bookLore().isEmpty()) out.add(para(font, w, Component.literal(r.bookLore()), TreeSkin.dim()));
            out.add(gap(5));
            out.add(para(font, w, SkillTreeLang.tr(SkillTreeLang.LOCKED_BODY), TreeSkin.text()));
            out.add(gap(5));
            out.add(para(font, w, SkillTreeLang.tr(SkillTreeLang.FIND_HINT), TreeSkin.dim()));
            if (!r.desc().isEmpty()) {
                out.add(gap(6));
                out.add(para(font, w, Component.literal(r.desc()), TreeSkin.faint()));
            }
            return out;
        }
        if (s.node() == null) {                                          // an unlocked route with nothing selected
            out.add(para(font, w, Component.literal(r.name()).withStyle(ChatFormatting.BOLD), TreeSkin.opaque(rgb)));
            if (!r.desc().isEmpty()) out.add(para(font, w, Component.literal(r.desc()), TreeSkin.text()));
            out.add(gap(5));
            out.add(para(font, w, SkillTreeLang.tr(SkillTreeLang.ROUTE_SUMMARY, NodeState.ownedNodes(s.player(), r), r.nodes().size(), r.totalCost()), TreeSkin.dim()));
            out.add(gap(5));
            out.add(para(font, w, SkillTreeLang.tr(SkillTreeLang.NO_SELECTION), TreeSkin.faint()));
            return out;
        }
        SkillNode n = s.node();
        NodeState st = s.nodeState();
        out.add(para(font, w, Component.literal(n.name()).withStyle(ChatFormatting.BOLD), TreeSkin.gold()));
        MutableComponent level = st.status() == NodeStatus.MAXED ? SkillTreeLang.tr(SkillTreeLang.LEVEL_MAX, st.level()) : SkillTreeLang.tr(SkillTreeLang.LEVEL, st.level(), st.max());
        out.add(para(font, w, level, TreeSkin.opaque(rgb)));
        out.add(para(font, w, SkillTreeLang.tr(stateKey(st.status())), stateColor(st.status())));
        out.add(para(font, w, SkillTreeLang.tr(n.grantsSkill() || n.nodeType() == SkillNode.NodeType.ACTIVE ? SkillTreeLang.TAG_ACTIVE : SkillTreeLang.TAG_PASSIVE), TreeSkin.dim()));
        if (!n.params().isEmpty()) {                                     // 1.82.0: the words of a line node (effect, element, shape...)
            StringBuilder words = new StringBuilder();
            for (var pe : n.params().entrySet()) {
                if (pe.getValue().equals("none")) continue;
                if (words.length() > 0) words.append(", ");
                words.append(TreeText.prettify(pe.getKey())).append(": ").append(TreeText.prettify(pe.getValue().toLowerCase(Locale.ROOT)));
            }
            if (words.length() > 0) out.add(para(font, w, Component.literal(words.toString()), TreeSkin.dim()));
        }
        if (!n.desc().isEmpty()) {
            out.add(gap(4));
            out.add(para(font, w, Component.literal(n.desc()), TreeSkin.text()));
        }
        List<String> keys = new ArrayList<>(n.values().keySet());
        Collections.sort(keys);
        if (!keys.isEmpty()) {
            out.add(gap(5));
            out.add(new Block(BlockKind.HEADING, SkillTreeLang.str(SkillTreeLang.VALUES).toUpperCase(Locale.ROOT), List.of(), TreeSkin.gold(), 14));
            for (String k : keys) {
                out.add(para(font, w, Component.literal(TreeText.prettify(k)), TreeSkin.dim()));
                out.add(para(font, w, numbers(r, n, k, st.level()), TreeSkin.text()));
                out.add(gap(2));
            }
        }
        if (st.status() != NodeStatus.MAXED && owned) {
            out.add(gap(3));
            out.add(new Block(BlockKind.HEADING, SkillTreeLang.str(SkillTreeLang.COST).toUpperCase(Locale.ROOT), List.of(), TreeSkin.gold(), 14));
            boolean afford = s.player().sp() >= st.price();
            out.add(para(font, w, SkillTreeLang.tr(SkillTreeLang.PRICE_NEXT, st.price()), afford ? TreeSkin.good() : TreeSkin.bad()));
            out.add(para(font, w, SkillTreeLang.tr(SkillTreeLang.SP_YOU_HAVE, s.player().sp()), TreeSkin.dim()));
        }
        if (!n.parents().isEmpty()) {
            out.add(gap(5));
            out.add(new Block(BlockKind.HEADING, SkillTreeLang.str(SkillTreeLang.NEEDS).toUpperCase(Locale.ROOT), List.of(), TreeSkin.gold(), 14));
            for (NodeState.Requirement q : NodeState.requirements(s.player(), r, n)) {
                SkillNode pn = r.node(q.node());
                String name = pn == null ? q.node() : pn.name();
                MutableComponent line = SkillTreeLang.tr(SkillTreeLang.NEEDS_LINE, name, q.need());
                if (!q.met()) line.append(Component.literal("  (" + q.have() + ")"));
                out.add(para(font, w, line, q.met() ? TreeSkin.good() : TreeSkin.bad()));
            }
        }
        return out;
    }

    /** The lang key of the one-line description of a node state (also used by the tooltips and the footer notices). */
    static String stateKey(NodeStatus status) {
        return switch (status) {
            case ROUTE_LOCKED -> SkillTreeLang.NODE_SEALED;
            case PARENTS_LOCKED -> SkillTreeLang.NODE_PARENTS;
            case AVAILABLE -> SkillTreeLang.NODE_READY;
            case TOO_EXPENSIVE -> SkillTreeLang.NODE_NO_SP;
            case OWNED -> SkillTreeLang.NODE_OWNED;
            case MAXED -> SkillTreeLang.NODE_MAXED;
        };
    }

    private static int stateColor(NodeStatus status) {
        return switch (status) {
            case AVAILABLE, OWNED -> TreeSkin.good();
            case MAXED -> TreeSkin.gold();
            case TOO_EXPENSIVE -> TreeSkin.bad();
            default -> TreeSkin.dim();
        };
    }

    /**
     * The numbers of one table, one per level, with the player's level in brackets, the next level in gold and the rest dimmed. They are read through
     * {@link RouteRegistry#value}, the very function the abilities read their damage and range from, so the card cannot show a number the ability will not use.
     */
    private static MutableComponent numbers(SkillRoute r, SkillNode n, String key, int level) {
        MutableComponent flow = Component.empty();
        for (int lv = 1; lv <= n.maxLevel(); lv++) {
            String t = TreeText.number(RouteRegistry.value(r.id(), n.id(), key, lv, 0));
            int color = lv < level ? TreeSkin.dim() : lv == level ? 0xFFFFFFFF : lv == level + 1 ? TreeSkin.gold() : TreeSkin.faint();
            int rgb = color & 0xFFFFFF;
            flow.append(Component.literal(lv == level ? "[" + t + "]" : t).withStyle(style -> style.withColor(rgb)));
            if (lv < n.maxLevel()) flow.append(Component.literal("  "));
        }
        return flow;
    }

    private static Block para(Font font, int w, Component text, int color) {
        List<FormattedCharSequence> lines = font.split(text, Math.max(20, w));
        return new Block(BlockKind.PARA, "", lines, color, lines.size() * (font.lineHeight + 1) + 2);
    }

    private static Block gap(int h) { return new Block(BlockKind.GAP, "", List.of(), 0, h); }

    // ------------------------------------------------------------------ scrolling and drawing
    void scrollBy(int dy, int viewH) { scroll = clamp(scroll + dy, viewH); }

    private int clamp(int v, int viewH) { return Math.max(0, Math.min(Math.max(0, contentH - viewH), v)); }

    boolean overflows(int viewH) { return contentH > viewH; }

    /** Draws the blocks inside {@code area} (the card's text area), scrolled and clipped. */
    void draw(GuiGraphics g, Font font, Rect area, int accentRgb) {
        scroll = clamp(scroll, area.h());
        g.enableScissor(area.x(), area.y(), area.right(), area.bottom());
        int y = area.y() - scroll;
        for (Block b : blocks) {
            if (y + b.height() >= area.y() && y <= area.bottom()) {
                switch (b.kind()) {
                    case HEADING -> TreeSkin.heading(g, font, font.plainSubstrByWidth(b.text(), area.w()), area.x(), y, area.w(), b.color() & 0xFFFFFF);
                    case PARA -> {
                        int ly = y;
                        for (FormattedCharSequence line : b.lines()) {
                            g.drawString(font, line, area.x(), ly, b.color(), false);
                            ly += font.lineHeight + 1;
                        }
                    }
                    case PADLOCK -> {
                        g.pose().pushPose();
                        g.pose().translate(area.x() + area.w() / 2f - 6f, y + 1f, 0f);
                        g.pose().scale(2f, 2f, 1f);
                        TreeSkin.padlock(g, 0, 0, UiMath.withAlpha(accentRgb, 0.85f));
                        g.pose().popPose();
                    }
                    default -> { }
                }
            }
            y += b.height();
        }
        g.disableScissor();
        if (contentH > area.h()) {
            int bar = Math.max(12, area.h() * area.h() / contentH);
            int span = Math.max(1, contentH - area.h());
            TreeSkin.scrollbar(g, area.right() + 1, area.y(), area.h(), (int) ((area.h() - bar) * (scroll / (float) span)), bar);
        }
    }
}
