package com.newuniverse.nusmp.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * The content and the page rules of the Compatibility Codex screen (pure, unit tested; no Minecraft types). The text is not written here: it is
 * read from {@link CompatBook#blocks} (the same blocks the written-book copy is made of) and from {@link CompatCatalog}, so the book and the screen
 * cannot disagree. The screen measures each {@link Item} with its font and hands the heights to {@link #paginate}.
 */
public final class CompatCodexModel {
    private CompatCodexModel() {}

    /** HEADING: a section title (with a status chip when {@code modId} is set); TEXT: a paragraph block; ENTRY: one mod with its status chip. */
    public enum Type { HEADING, TEXT, ENTRY }

    /** One drawable piece. {@code detail} is the line under an ENTRY's name (may be empty); {@code newPage} forces a fresh page. */
    public record Item(Type type, String text, String detail, String modId, boolean newPage) {}

    /** The items of the book, in order, for the given mod-list state. */
    public static List<Item> items(Predicate<String> loaded) {
        Predicate<String> p = loaded == null ? id -> false : loaded;
        List<Item> out = new ArrayList<>();
        for (CompatBook.Block b : CompatBook.blocks(p)) {
            String text = b.text();
            if (b.newPage()) {
                int nl = text.indexOf('\n');
                String head = nl < 0 ? text.trim() : text.substring(0, nl).trim();
                String rest = nl < 0 ? "" : text.substring(nl + 1);
                if (b.modId() != null && rest.startsWith(CompatBook.word(p.test(b.modId())) + "\n")) rest = rest.substring(rest.indexOf('\n') + 1);   // the chip says it
                out.add(new Item(Type.HEADING, head, "", b.modId(), true));
                rest = rest.strip();
                if (!rest.isEmpty()) out.add(new Item(Type.TEXT, rest, "", null, false));
            } else if (b.modId() != null) {
                CompatCatalog.Entry e = CompatCatalog.entry(b.modId());
                int nl = text.indexOf('\n');
                String detail = nl < 0 ? "" : text.substring(nl + 1).strip();
                out.add(new Item(Type.ENTRY, e == null ? b.modId() : e.name(), detail, b.modId(), false));
            } else {
                out.add(new Item(Type.TEXT, text.strip(), "", null, false));
            }
        }
        return List.copyOf(out);
    }

    /**
     * Packs items into pages of {@code pageHeight} pixels with {@code gap} pixels between items. A newPage item starts a page; a HEADING is never
     * left at the foot of a page after other items (it moves down with the item after it); an item taller than a page gets a page of its own. Always at least
     * one page; no page is empty.
     */
    public static List<List<Item>> paginate(List<Item> items, ToIntFunction<Item> height, int pageHeight, int gap) {
        List<List<Item>> pages = new ArrayList<>();
        List<Item> cur = new ArrayList<>();
        int used = 0;
        for (Item it : items) {
            int h = Math.max(0, height.applyAsInt(it));
            int add = h + (cur.isEmpty() ? 0 : gap);
            if (!cur.isEmpty() && (it.newPage() || used + add > pageHeight)) {
                Item carry = null;
                if (!it.newPage() && cur.size() > 1 && cur.get(cur.size() - 1).type() == Type.HEADING) carry = cur.remove(cur.size() - 1);   // the heading goes down with its text
                pages.add(List.copyOf(cur));
                cur.clear();
                used = 0;
                if (carry != null) { cur.add(carry); used = Math.max(0, height.applyAsInt(carry)); }
                add = h + (cur.isEmpty() ? 0 : gap);
            }
            used += add;
            cur.add(it);
        }
        if (!cur.isEmpty() || pages.isEmpty()) pages.add(List.copyOf(cur));
        return List.copyOf(pages);
    }

    /** How many two-page spreads {@code pages} pages make. */
    public static int spreads(int pages) { return Math.max(1, (pages + 1) / 2); }

    /** How many mods of the catalog are loaded (for the status line). */
    public static int loadedCount(Predicate<String> loaded) {
        int n = 0;
        for (CompatCatalog.Status s : CompatCatalog.statuses(loaded)) if (s.loaded()) n++;
        return n;
    }
}
