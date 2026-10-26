package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.data.RouteEvolution;
import com.newuniverse.nusmp.skilltree.data.RouteSchema;
import com.newuniverse.nusmp.skilltree.data.RouteWire;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Why: with 283 lines the server cannot send every player every tree (about 3.7 MB, and the owner wants the trees of lines a player does not own to stay on the server), but some things on a
 * client must still know a line that is NOT owned without its nodes: a Skill Book in the inventory is named and tinted from its line, the Black Market lists books by line, the creative tab offers
 * one book per line, and an administrator in inspect mode lists every line. A card is exactly that: the identity and look of a line (id, family, name, book, colour, icon, class, cover, theme) and
 * how many nodes it has, about 350 bytes, never a node. The client turns a card into a node-less "stub" route ({@link #toStub}) so the book, shop and creative-tab code, which already asks the route
 * registry by id and by book, works unchanged; a stub can never be opened as a tree (no nodes), and the real tree arrives only when the line is owned or an administrator asks to inspect it.
 * Pure: the byte layout is written on the same three primitives as {@link RouteWire}, so the tests count and round-trip it without a game.
 */
public record LineCard(String id, String magic, String franchise, String name, String book, String bookName, String bookLore, int color, String icon, String classId,
                       String bookTexture, String backgroundTheme, int nodeCount, RouteEvolution evolution) {
    /**
     * Why: the 13-component form of the card before 3.26.0 (no evolution). Every old call site keeps compiling and means a card of a line with no evolution. Add the new, keep the old.
     */
    public LineCard(String id, String magic, String franchise, String name, String book, String bookName, String bookLore, int color, String icon, String classId,
                    String bookTexture, String backgroundTheme, int nodeCount) {
        this(id, magic, franchise, name, book, bookName, bookLore, color, icon, classId, bookTexture, backgroundTheme, nodeCount, null);
    }

    /** At most this many cards in one payload (283 lines is about 100 KB; the cap keeps a payload far under the 1 MiB limit). */
    public static final int MAX_CARDS = 2048;

    public static LineCard of(SkillRoute r) {
        return new LineCard(r.id(), r.magic(), r.franchise() == null ? "" : r.franchise(), r.name(), r.book(), r.bookName(), r.bookLore(), r.color() & 0xFFFFFF, r.icon(), r.classId(),
                r.bookTexture() == null ? "" : r.bookTexture(), r.backgroundTheme() == null ? "" : r.backgroundTheme(), r.nodes().size(), r.evolution());
    }

    /** A route with no nodes that stands for this line on a client that does not own it (see the class comment). */
    public SkillRoute toStub() {
        return new SkillRoute(id, magic, name, "", book, bookName, bookLore, color, icon, classId, 0, 0, List.of(), franchise, "", bookTexture, backgroundTheme, evolution);
    }

    // ---------------------------------------------------------------- wire
    public static void write(RouteWire.Sink out, LineCard c) {
        out.utf(c.id, RouteSchema.LEN_ROUTE_ID);
        out.utf(c.magic, RouteSchema.LEN_KEY);
        out.utf(c.franchise, RouteSchema.LEN_KEY);
        out.utf(c.name, RouteSchema.LEN_NAME);
        out.utf(c.book, RouteSchema.LEN_RESOURCE);
        out.utf(c.bookName, RouteSchema.LEN_NAME);
        out.utf(c.bookLore, RouteSchema.LEN_LORE);
        out.varInt(c.color & 0xFFFFFF);
        out.utf(c.icon, RouteSchema.LEN_RESOURCE);
        out.utf(c.classId, RouteSchema.LEN_ID);
        out.utf(c.bookTexture, RouteSchema.LEN_PATH);
        out.utf(c.backgroundTheme, RouteSchema.LEN_KEY);
        out.varInt(c.nodeCount);
        RouteWire.writeEvolution(out, c.evolution);                          // 3.26.0: the requirement text, count and the name after the evolution (a flag only for a line without one)
    }

    public static LineCard read(RouteWire.Source in) {
        String id = in.utf(RouteSchema.LEN_ROUTE_ID), magic = in.utf(RouteSchema.LEN_KEY), franchise = in.utf(RouteSchema.LEN_KEY), name = in.utf(RouteSchema.LEN_NAME);
        String book = in.utf(RouteSchema.LEN_RESOURCE), bookName = in.utf(RouteSchema.LEN_NAME), bookLore = in.utf(RouteSchema.LEN_LORE);
        int color = in.varInt() & 0xFFFFFF;
        String icon = in.utf(RouteSchema.LEN_RESOURCE), classId = in.utf(RouteSchema.LEN_ID), bookTexture = in.utf(RouteSchema.LEN_PATH), theme = in.utf(RouteSchema.LEN_KEY);
        int nodes = in.varInt();
        if (nodes < 0 || nodes > RouteSchema.MAX_NODES) throw new IllegalArgumentException("a line card carries nodeCount = " + nodes);
        return new LineCard(id, magic, franchise, name, book, bookName, bookLore, color, icon, classId, bookTexture, theme, nodes, RouteWire.readEvolution(in));
    }

    public static void writeAll(RouteWire.Sink out, List<LineCard> cards) {
        int n = Math.min(cards.size(), MAX_CARDS);
        out.varInt(n);
        for (int i = 0; i < n; i++) write(out, cards.get(i));
    }

    public static List<LineCard> readAll(RouteWire.Source in) {
        int n = in.varInt();
        if (n < 0 || n > MAX_CARDS) throw new IllegalArgumentException("a card packet carries " + n + " cards (allowed 0 to " + MAX_CARDS + ")");
        List<LineCard> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(read(in));
        return Collections.unmodifiableList(out);
    }

    /** The exact byte size of a list of cards on the wire (for the scale test and the payload sizing). */
    public static int size(List<LineCard> cards) {
        int[] bytes = {0};
        RouteWire.Sink counter = new RouteWire.Sink() {
            @Override public void varInt(int value) { bytes[0] += RouteWire.varIntSize(value); }

            @Override public void utf(String value, int maxChars) {
                if (value.length() > maxChars) throw new IllegalArgumentException("a text of " + value.length() + " characters does not fit the wire limit of " + maxChars);
                int len = value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                bytes[0] += RouteWire.varIntSize(len) + len;
            }

            @Override public void f64(double value) { bytes[0] += 8; }
        };
        writeAll(counter, cards);
        return bytes[0];
    }
}
