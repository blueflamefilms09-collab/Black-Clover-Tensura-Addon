package com.newuniverse.nusmp.skilltree.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a Skill Book must look right and say what it opens on a client that does NOT know the route. The server syncs a client only the lines its player owns
 * (docs/skillbooks_unique.md, scale rule), so a book found in a chest, dropped by a boss or bought for a line the player has not unlocked yet has no route in the client's
 * registry, and the old code then showed a grey cover and "The pages are blank". The assets therefore ship {@code assets/nusmp/skill_books.json}, a table generated from the same
 * route files and rows as the routes themselves ({@code tools/lines/gen_book_index.py}): book id -> line id, line name, book name, colour, franchise. This class is that table,
 * parsed once per resource reload into immutable maps, read lock-free (one volatile reference) by the item's name and tooltip, the cover tint and the model's franchise property.
 * It is pure (Gson only, no Minecraft type) so the parsing, the property values and the colour fall-backs are unit tested; the client class {@code BookClient} feeds it from the
 * resource manager. On a dedicated server nothing feeds it and it stays empty, which is right: the server has the real routes.
 *
 * <p>The franchise property: the item property {@code nusmp:franchise} is {@code (index + 1) / steps} for the franchise at {@code index} in the table's own {@code franchises}
 * list (0 for an unknown book or a franchise without a cover, which selects the generic cover). The generated model overrides use thresholds half a step below each value, so
 * no float rounding can make a franchise miss its own cover; the order of the list is part of the asset contract, new franchises are only appended.
 */
public final class BookLook {
    private BookLook() {}

    /** One line's book as the client sees it. {@code franchise} is the key (empty when none), {@code color} is 0xRRGGBB. */
    public record Entry(String line, String book, String name, String bookName, int color, String franchise) {}

    /** An immutable snapshot of the table. */
    public static final class Index {
        public static final Index EMPTY = new Index(List.of(), 20, Map.of());

        private final List<String> franchises;
        private final int steps;
        private final Map<String, Entry> byBook;
        private final Map<String, Entry> byLine;
        private final Map<String, Integer> franchiseIndex;

        Index(List<String> franchises, int steps, Map<String, Entry> byLine) {
            this.franchises = Collections.unmodifiableList(new ArrayList<>(franchises));
            this.steps = Math.max(franchises.size() + 1, steps);
            Map<String, Entry> bb = new HashMap<>();
            for (Entry e : byLine.values()) bb.putIfAbsent(e.book(), e);
            this.byBook = Collections.unmodifiableMap(bb);
            this.byLine = Collections.unmodifiableMap(new HashMap<>(byLine));
            Map<String, Integer> fi = new HashMap<>();
            for (int i = 0; i < this.franchises.size(); i++) fi.putIfAbsent(this.franchises.get(i), i);
            this.franchiseIndex = Collections.unmodifiableMap(fi);
        }

        public int size() { return byLine.size(); }

        public List<String> franchises() { return franchises; }

        public int steps() { return steps; }

        /** The entry of a book id ({@code nusmp:lady_luck_book}), or null. */
        public Entry byBook(String bookId) { return bookId == null ? null : byBook.get(bookId); }

        /** The entry of a line id ({@code lady_luck} or {@code nusmp:lady_luck}), or null. */
        public Entry byLine(String lineId) {
            if (lineId == null) return null;
            Entry e = byLine.get(lineId);
            if (e == null && lineId.startsWith("nusmp:")) e = byLine.get(lineId.substring(6));
            return e;
        }

        /** The model property value of a franchise key: {@code (index + 1) / steps}, or 0 when the key is empty or has no cover. */
        public float franchiseValue(String franchise) {
            if (franchise == null || franchise.isEmpty()) return 0f;
            Integer i = franchiseIndex.get(franchise);
            return i == null ? 0f : (i + 1) / (float) steps;
        }
    }

    private static volatile Index current = Index.EMPTY;

    /** The table in force on this side (empty on a dedicated server, loaded by the client's resource reload). */
    public static Index index() { return current; }

    public static void set(Index index) { current = index == null ? Index.EMPTY : index; }

    public static Entry byBook(String bookId) { return current.byBook(bookId); }

    /**
     * The tint of a book's cover: the known route's colour first (the caller passes it, or -1), else the table's colour, else {@code fallback}. Always 0xRRGGBB.
     */
    public static int colorOf(String bookId, int routeColor, int fallback) {
        if (routeColor >= 0) return routeColor & 0xFFFFFF;
        Entry e = current.byBook(bookId);
        return e == null ? fallback : e.color();
    }

    /** The value of the item property {@code nusmp:franchise} for a book id; {@code routeFranchise} (the known route's {@code magic}, or null) is used when the table has no entry. */
    public static float franchiseValue(String bookId, String routeFranchise) {
        Index idx = current;
        Entry e = idx.byBook(bookId);
        if (e != null && !e.franchise().isEmpty()) return idx.franchiseValue(e.franchise());
        return idx.franchiseValue(routeFranchise);
    }

    // ------------------------------------------------------------------------------------------------------------------ parsing
    /** Parses the shipped json. Tolerant: a line without a usable book or colour is skipped, a bad colour becomes white; never throws for shape problems. */
    public static Index parse(JsonObject root) {
        List<String> franchises = new ArrayList<>();
        if (root.has("franchises") && root.get("franchises").isJsonArray()) {
            for (JsonElement f : root.getAsJsonArray("franchises")) if (f.isJsonPrimitive()) franchises.add(f.getAsString());
        }
        int steps = root.has("prop_steps") && root.get("prop_steps").isJsonPrimitive() ? root.get("prop_steps").getAsInt() : 20;
        Map<String, Entry> lines = new HashMap<>();
        if (root.has("lines") && root.get("lines").isJsonObject()) {
            for (Map.Entry<String, JsonElement> me : root.getAsJsonObject("lines").entrySet()) {
                if (!me.getValue().isJsonObject()) continue;
                JsonObject o = me.getValue().getAsJsonObject();
                String book = str(o, "book");
                if (book.isEmpty()) continue;
                String name = str(o, "name");
                String bookName = str(o, "book_name");
                lines.put(me.getKey(), new Entry(me.getKey(), book, name.isEmpty() ? me.getKey() : name, bookName.isEmpty() ? name : bookName, color(str(o, "color")), str(o, "franchise")));
            }
        }
        return new Index(franchises, steps, lines);
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    /** {@code #RRGGBB} (or {@code RRGGBB}) to 0xRRGGBB; anything else is white. */
    static int color(String s) {
        String h = s.startsWith("#") ? s.substring(1) : s;
        if (h.length() != 6) return 0xFFFFFF;
        try {
            return Integer.parseInt(h, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}
