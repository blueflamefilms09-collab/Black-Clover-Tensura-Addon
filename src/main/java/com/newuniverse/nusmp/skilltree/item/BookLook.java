package com.newuniverse.nusmp.skilltree.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.newuniverse.nusmp.skilltree.rarity.BookRarity;

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

    /**
     * One line's book as the client sees it. {@code franchise} is the key (empty when none), {@code color} is 0xRRGGBB, {@code rarity} the book's rarity (3.26.0; a table without the field reads as common).
     */
    public record Entry(String line, String book, String name, String bookName, int color, String franchise, BookRarity rarity) {
        public Entry {
            rarity = rarity == null ? BookRarity.COMMON : rarity;
        }

        /** The six-argument form of 3.25 (common); kept so old callers and tables compile. */
        public Entry(String line, String book, String name, String bookName, int color, String franchise) {
            this(line, book, name, bookName, color, franchise, BookRarity.COMMON);
        }
    }

    /** An immutable snapshot of the table. */
    public static final class Index {
        public static final Index EMPTY = new Index(List.of(), 20, Map.of());

        private final List<String> franchises;
        private final int steps;
        private final Map<String, Entry> byBook;
        private final Map<String, Entry> byLine;
        private final Map<String, Integer> franchiseIndex;
        /** 3.26.0: book id -> rarity rank (0 common .. 4 mythic), from the optional {@code rarity} field of a line; a book with no field is common. Set once by {@link #parse}. */
        private volatile Map<String, Integer> ranks = Map.of();

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

        /** The rarity rank (0 common .. 4 mythic) of a book id; 0 when the book or its field is unknown. */
        public int rankOf(String bookId) {
            Integer r = bookId == null ? null : ranks.get(bookId);
            return r == null ? 0 : r;
        }

        /** The value of the item property {@code nusmp:rarity} of a book id: {@code (rank + 1) / 6}, so 1/6 for common (and for an unknown book) up to 5/6 for mythic. */
        public float rarityValue(String bookId) { return rarityStep(rankOf(bookId)); }

        void setRanks(Map<String, Integer> r) { this.ranks = Collections.unmodifiableMap(new HashMap<>(r)); }

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

    /** The rarity of a book id in the table in force (common for an unknown book): the stack's own component, when it has one, wins in {@code SkillItems.rarityOf}. */
    public static BookRarity rarityOf(String bookId) {
        Entry e = current.byBook(bookId);
        return e == null ? BookRarity.COMMON : e.rarity();
    }

    /** The value of the item property {@code nusmp:rarity} for a book id: {@code (rank + 1) / 6}, 0 for a book the table does not know (the base look). */
    public static float rarityValue(String bookId) {
        Entry e = current.byBook(bookId);
        return e == null ? 0f : e.rarity().modelValue();
    }

    // ------------------------------------------------------------------------------------------------------------------ rarity (3.26.0, the look)
    /** The rarity ids in rank order; the item property {@code nusmp:rarity} is {@code (rank + 1) / RARITY_STEPS}. */
    public static final List<String> RARITY_IDS = List.of("common", "uncommon", "rare", "epic", "mythic");
    public static final float RARITY_STEPS = 6f;

    /** The rank of a rarity id (case-insensitive, with or without the {@code nusmp:} prefix); 0 (common) for anything unknown. */
    public static int rankOfId(String id) {
        if (id == null) return 0;
        String t = id.strip().toLowerCase(java.util.Locale.ROOT);
        if (t.startsWith("nusmp:")) t = t.substring(6);
        int i = RARITY_IDS.indexOf(t);
        return i < 0 ? 0 : i;
    }

    /** The rank held by a component value: -1 for none (null), the clamped number for a number, else the rank of its text. Pure, so the guarded stack reader can be tested without a stack. */
    public static int rankOfValue(Object v) {
        if (v == null) return -1;
        if (v instanceof Number n) return Math.max(0, Math.min(RARITY_IDS.size() - 1, n.intValue()));
        return rankOfId(v.toString());
    }

    /** {@code (rank + 1) / 6} for a rank clamped to 0..4. */
    public static float rarityStep(int rank) { return (Math.max(0, Math.min(RARITY_IDS.size() - 1, rank)) + 1) / RARITY_STEPS; }

    // ------------------------------------------------------------------------------------------------------------------ parsing
    /** Parses the shipped json. Tolerant: a line without a usable book or colour is skipped, a bad colour becomes white; never throws for shape problems. */
    public static Index parse(JsonObject root) {
        List<String> franchises = new ArrayList<>();
        if (root.has("franchises") && root.get("franchises").isJsonArray()) {
            for (JsonElement f : root.getAsJsonArray("franchises")) if (f.isJsonPrimitive()) franchises.add(f.getAsString());
        }
        int steps = root.has("prop_steps") && root.get("prop_steps").isJsonPrimitive() ? root.get("prop_steps").getAsInt() : 20;
        Map<String, Entry> lines = new HashMap<>();
        Map<String, Integer> ranks = new HashMap<>();
        if (root.has("lines") && root.get("lines").isJsonObject()) {
            for (Map.Entry<String, JsonElement> me : root.getAsJsonObject("lines").entrySet()) {
                if (!me.getValue().isJsonObject()) continue;
                JsonObject o = me.getValue().getAsJsonObject();
                String book = str(o, "book");
                if (book.isEmpty()) continue;
                String name = str(o, "name");
                String bookName = str(o, "book_name");
                lines.put(me.getKey(), new Entry(me.getKey(), book, name.isEmpty() ? me.getKey() : name, bookName.isEmpty() ? name : bookName, color(str(o, "color")), str(o, "franchise"),
                        BookRarity.fromId(str(o, "rarity"))));
                String rarity = str(o, "rarity");
                if (!rarity.isEmpty()) ranks.putIfAbsent(book, rankOfId(rarity));
            }
        }
        Index idx = new Index(franchises, steps, lines);
        idx.setRanks(ranks);
        return idx;
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
