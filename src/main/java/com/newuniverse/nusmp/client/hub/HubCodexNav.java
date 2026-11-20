package com.newuniverse.nusmp.client.hub;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: Hub v2 hosts every page of the Codex inside the hub (docs/codex.md, "Hub v2"), reached through a left navigation rail grouped into categories. This is the pure part of that rail, with no Minecraft types so
 * {@code HubCodexNavTest} can prove that no Codex page is lost: a page is given an id from its title and class ({@link #idOf}), the id belongs to a category ({@link #catOf}), and {@link #rows} turns the list of
 * page ids (in the order {@code CodexPages.all()} gives them) into the rail rows: a header per non-empty category followed by its pages in the order of {@link #NAV}, with every page index appearing exactly once
 * (a page this class has never heard of, like a data section somebody adds, is filed under "Reference" with the generic icon, so it is still reachable). Rows have fixed heights, so a click is plain arithmetic
 * ({@link #rowAt}). The ids are also the names of the icon files ({@code codex_<id>.png}), the categories of {@code cat_<key>.png}.
 */
public final class HubCodexNav {
    private HubCodexNav() {}

    /** The categories, in rail order. Append only. */
    public enum Cat {
        YOU("you"), COLLECTION("collection"), MAGIC("magic"), WORLD("world"), REFERENCE("reference");

        /** {@code anime_multiverse.hub.cat.<key>} is the lang key of the heading, {@code cat_<key>.png} the icon. */
        public final String key;

        Cat(String key) { this.key = key; }
    }

    /** A page id and its category. */
    public record Item(String id, Cat cat) {}

    /** Every id the rail knows, in rail order inside a category. The last one, {@code data}, is the id of any other data-driven section. Append only. */
    public static final Item[] NAV = {
            new Item("overview", Cat.YOU), new Item("status", Cat.YOU), new Item("weapon", Cat.YOU), new Item("beastwill", Cat.YOU),
            new Item("weapons", Cat.COLLECTION), new Item("relics", Cat.COLLECTION), new Item("bestiary", Cat.COLLECTION), new Item("spirit_lords", Cat.COLLECTION),
            new Item("spells", Cat.MAGIC), new Item("magic", Cat.MAGIC), new Item("effects", Cat.MAGIC),
            new Item("blocks", Cat.WORLD), new Item("entities", Cat.WORLD),
            new Item("guide", Cat.REFERENCE), new Item("rules", Cat.REFERENCE), new Item("wiki", Cat.REFERENCE), new Item("data", Cat.REFERENCE)};

    public static final String DATA = "data";

    /** The English tab titles of the stock pages (and of the two data sections the mod ships) mapped to their ids. */
    private static final String[][] TITLES = {
            {"you", "overview"}, {"status", "status"}, {"weapons", "weapons"}, {"relics", "relics"}, {"bestiary", "bestiary"}, {"spirit lords", "spirit_lords"}, {"guide", "guide"},
            {"spells", "spells"}, {"blocks", "blocks"}, {"entities", "entities"}, {"magic", "magic"}, {"effects", "effects"}, {"rules", "rules"}, {"wiki", "wiki"}};

    /** The id of a page from its tab title and the simple name of its class; {@link #DATA} for one this class does not know. */
    public static String idOf(String title, String pageClass) {
        if ("BeastWillCodexPage".equals(pageClass)) return "beastwill";
        if ("WeaponCodexPage".equals(pageClass)) return "weapon";
        String t = title == null ? "" : title.strip().toLowerCase(Locale.ROOT);
        for (String[] e : TITLES) if (e[0].equals(t)) return e[1];
        return DATA;
    }

    public static Cat catOf(String id) {
        for (Item i : NAV) if (i.id().equals(id)) return i.cat();
        return Cat.REFERENCE;
    }

    private static int order(String id) {
        for (int i = 0; i < NAV.length; i++) if (NAV[i].id().equals(id)) return i;
        return NAV.length - 1;
    }

    /** A rail row: a category heading ({@code page == -1}) or a page (the index into the page list). */
    public record Row(Cat cat, int page) {
        public boolean header() { return page < 0; }
    }

    public static final int HEADER_H = 12, ITEM_H = 14;

    /** The rows for pages whose ids are {@code ids} (index = page index): per category in {@link Cat} order a heading and then its pages in {@link #NAV} order, ties in page order. Every index appears once. */
    public static List<Row> rows(List<String> ids) {
        List<Row> out = new ArrayList<>();
        for (Cat c : Cat.values()) {
            List<Integer> in = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) if (catOf(ids.get(i)) == c) in.add(i);
            if (in.isEmpty()) continue;
            in.sort((a, b) -> Integer.compare(order(ids.get(a)), order(ids.get(b))));          // stable: equal ids keep page order
            out.add(new Row(c, -1));
            for (int i : in) out.add(new Row(c, i));
        }
        return out;
    }

    public static int rowH(Row r) { return r.header() ? HEADER_H : ITEM_H; }

    /** The height of all rows. */
    public static int height(List<Row> rows) {
        int h = 0;
        for (Row r : rows) h += rowH(r);
        return h;
    }

    /** The top of row {@code i} from the top of the rail content. */
    public static int rowY(List<Row> rows, int i) {
        int y = 0;
        for (int k = 0; k < i; k++) y += rowH(rows.get(k));
        return y;
    }

    /** The index of the row at content height {@code y}, or -1. */
    public static int rowAt(List<Row> rows, int y) {
        if (y < 0) return -1;
        int top = 0;
        for (int i = 0; i < rows.size(); i++) {
            int h = rowH(rows.get(i));
            if (y < top + h) return i;
            top += h;
        }
        return -1;
    }

    /** The row index showing page {@code page}, or -1. */
    public static int rowOfPage(List<Row> rows, int page) {
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).page() == page) return i;
        return -1;
    }

    /** The next (dir = 1) or previous (dir = -1) page in rail order from page {@code page}, wrapping round; {@code page} itself when there is none other. */
    public static int step(List<Row> rows, int page, int dir) {
        int at = rowOfPage(rows, page), n = rows.size();
        if (at < 0 || n == 0) return page;
        for (int k = 1; k <= n; k++) {
            Row r = rows.get(Math.floorMod(at + dir * k, n));
            if (!r.header()) return r.page();
        }
        return page;
    }
}
