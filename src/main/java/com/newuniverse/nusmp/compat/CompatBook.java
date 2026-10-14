package com.newuniverse.nusmp.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The text of the Compatibility Codex (pure, unit tested; no Minecraft types, so the page rules can be checked headless). The written book
 * itself is made by {@link CompatBookItem}.
 * <p>
 * A vanilla book page is 114 px wide and about 14 lines tall. {@link #LINE_CHARS} is a cautious 19 characters per line (a typical
 * line is narrower than 114 px at that width), every line is broken here with an explicit newline, and a page never exceeds
 * {@link #MAX_LINES} lines or {@link #MAX_CHARS} characters.
 * <p>
 * The Loaded / Not installed words are read from {@code loaded} (a mod-list predicate) when the pages are built, so a copy of the book
 * shows the state of the game that made it. Nothing is claimed that {@link CompatCatalog} does not say.
 */
public final class CompatBook {
    private CompatBook() {}

    public static final String TITLE = "Compatibility Codex";
    public static final String AUTHOR = "Multiverse of Anime";
    public static final int LINE_CHARS = 19, MAX_LINES = 14, MAX_CHARS = 250;
    public static final String LOADED = "Loaded", ABSENT = "Not installed";

    /** A piece of text; {@code newPage} starts it on a fresh page (a section start). */
    public record Block(boolean newPage, String text, String modId) {
        Block(boolean newPage, String text) { this(newPage, text, null); }
    }

    public static String word(boolean loaded) { return loaded ? LOADED : ABSENT; }

    /** The pages of the book for the given mod-list state. */
    public static List<String> pages(Predicate<String> loaded) {
        return paginate(blocks(loaded == null ? id -> false : loaded));
    }

    /** The blocks of the book (also read by the Compatibility Codex screen, so the text lives only here). */
    public static List<Block> blocks(Predicate<String> loaded) {
        List<Block> out = new ArrayList<>();
        out.add(new Block(true, TITLE + "\n\n" + AUTHOR + "\n\nWhat this mod works with, and what it does not."));
        out.add(new Block(true, "How to read\n\nLoaded or Not installed was read from your mod list when this copy was made.\n\n"
                + "New copy: /multiverse compat book\nLive list: /multiverse compat status"));

        out.add(new Block(true, "REQUIRED\nThe mod needs these to start."));
        for (CompatCatalog.Entry e : CompatCatalog.of(CompatCatalog.Kind.REQUIRED)) out.add(statusBlock(e, loaded, false));

        out.add(new Block(true, "NEEDED BY TENSURA\nNot in our mod file, but Tensura needs them and our code uses them."));
        for (CompatCatalog.Entry e : CompatCatalog.of(CompatCatalog.Kind.VIA_TENSURA)) out.add(statusBlock(e, loaded, true));

        out.add(new Block(true, "OPTIONAL\nThe mod runs without them. Each line says what code or data really does."));
        for (CompatCatalog.Entry e : CompatCatalog.of(CompatCatalog.Kind.OPTIONAL)) out.add(statusBlock(e, loaded, true));

        boolean pack = CompatCatalog.packActive(loaded);
        out.add(new Block(true, "TENSURA NO INDEX\n" + word(loaded.test(CompatCatalog.TNI_MODID)) + "\nThe built-in data pack "
                + CompatCatalog.PACK_ID + " is " + (pack ? "ON" : "off (the other mod is not here)") + ". It switches itself on; nothing to configure.",
                CompatCatalog.TNI_MODID));
        out.add(new Block(true, "The pack adds\n- Our Library Ruins and Grimoire Tower chests: a 25% extra roll of Phase Dust,"
                + " Name Slip or Canalis Relic.\n- The Tower: 3% for the Philosopher's Stone."));
        out.add(new Block(true, "- Our 8 bosses: 15% Phase Dust or Canalis Relic, 4% Philosopher's Stone, where they roll a loot table"
                + " (the Dungeon clears boss drops).\n- Our 8 bosses and its 7 join the Tensura tag no_max_ep_plunder."));
        out.add(new Block(true, "Its own bridge pack\nIt lists no global loot modifiers, so its loot never loads; it targets loot tables"
                + " we do not have; its tag folder is entity_types, which 1.21 ignores. Ours repairs the tag and adds working loot."));
        out.add(new Block(true, "Not covered\n- Toaru skills in a skill codex: we have no such codex.\n- Index grimoires as loot (they need a book id).\n"
                + "- Loot from its bosses (they have no loot tables).\n- Curios tags: its worn items are already tagged."));
        out.add(new Block(true, "Tested?\n\nNo. Not one line of this was run in a game. The files were read and checked against the"
                + " other mod's jar; the build server only compiles. Report what you see."));
        return out;
    }

    private static Block statusBlock(CompatCatalog.Entry e, Predicate<String> loaded, boolean withLine) {
        String head = e.name() + ": " + word(loaded.test(e.modId()));
        return new Block(false, withLine ? head + "\n" + e.line() + "\n" : head, e.modId());
    }

    /** Wraps one text at {@code width} characters on word boundaries; explicit newlines are kept; a longer word is cut. */
    static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        for (String para : text.split("\n", -1)) {
            if (para.isBlank()) {
                lines.add("");
                continue;
            }
            StringBuilder cur = new StringBuilder();
            for (String w : para.trim().split("\\s+")) {
                while (w.length() > width) {
                    if (cur.length() > 0) {
                        lines.add(cur.toString());
                        cur.setLength(0);
                    }
                    lines.add(w.substring(0, width));
                    w = w.substring(width);
                }
                if (cur.length() == 0) cur.append(w);
                else if (cur.length() + 1 + w.length() <= width) cur.append(' ').append(w);
                else {
                    lines.add(cur.toString());
                    cur.setLength(0);
                    cur.append(w);
                }
            }
            if (cur.length() > 0) lines.add(cur.toString());
        }
        return lines;
    }

    /**
     * Packs blocks into pages. A section starts a new page; a block is kept whole on one page when it fits on an empty one (it moves to the
     * next page rather than being cut); a block longer than a page is cut line by line. No page passes the line or character limit.
     */
    static List<String> paginate(List<Block> blocks) {
        List<String> pages = new ArrayList<>();
        List<String> page = new ArrayList<>();
        int chars = 0;
        for (Block b : blocks) {
            List<String> lines = wrap(b.text(), LINE_CHARS);
            boolean spacer = !lines.isEmpty() && lines.get(lines.size() - 1).isEmpty();
            while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) lines = lines.subList(0, lines.size() - 1);
            if (lines.isEmpty()) continue;
            if (!page.isEmpty() && (b.newPage() || (!fits(page.size(), chars, lines) && fits(0, 0, lines)))) {
                flush(pages, page);
                chars = 0;
            }
            for (String line : lines) {
                if (page.isEmpty() && line.isEmpty()) continue;
                int add = line.length() + (page.isEmpty() ? 0 : 1);
                if (page.size() >= MAX_LINES || chars + add > MAX_CHARS) {
                    flush(pages, page);
                    chars = 0;
                    if (line.isEmpty()) continue;
                    add = line.length();
                }
                page.add(line);
                chars += add;
            }
            if (spacer && page.size() < MAX_LINES && chars + 1 <= MAX_CHARS) {
                page.add("");
                chars += 1;
            }
        }
        flush(pages, page);
        return List.copyOf(pages);
    }

    /** Would these lines fit on a page that already holds {@code have} lines and {@code chars} characters? */
    private static boolean fits(int have, int chars, List<String> lines) {
        int add = 0;
        for (String l : lines) add += l.length() + 1;
        if (have == 0) add -= 1;
        return have + lines.size() <= MAX_LINES && chars + add <= MAX_CHARS;
    }

    private static void flush(List<String> pages, List<String> page) {
        int end = page.size();
        while (end > 0 && page.get(end - 1).isEmpty()) end--;
        if (end > 0) pages.add(String.join("\n", page.subList(0, end)));
        page.clear();
    }
}
