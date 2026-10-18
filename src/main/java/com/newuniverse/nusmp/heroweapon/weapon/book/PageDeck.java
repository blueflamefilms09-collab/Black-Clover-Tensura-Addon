package com.newuniverse.nusmp.heroweapon.weapon.book;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Vassal Book is read, not aimed: its pages come up IN ORDER, the way the pages of a book do, so a wielder can plan (the next page is shown), and the
 * book can tell when every page of it has been read. This is that deck and nothing else, with no game imports, so the rules are tested: the pages are drawn in
 * the order they are written and the deck wraps round after the last; turning a page skips it without reading it (a skipped page stays unread); the deck is
 * COMPLETE when every page has been read at least once since it was last reset; an empty deck has no next page. One deck per chapter per wielder.
 */
public final class PageDeck {
    private final List<String> pages;
    private final boolean[] read;
    private int cursor;

    /** @param pages the page ids in reading order; they are copied */
    public PageDeck(List<String> pages) {
        this.pages = List.copyOf(pages);
        this.read = new boolean[this.pages.size()];
    }

    public int size() { return pages.size(); }

    public List<String> pages() { return pages; }

    /** The page that will be read next, or null when the deck is empty. */
    public String next() { return pages.isEmpty() ? null : pages.get(cursor); }

    /** The page after the next one (what comes up once the next is read), or null when the deck is empty. */
    public String after() { return pages.isEmpty() ? null : pages.get((cursor + 1) % pages.size()); }

    /** Reads the next page: marks it read, moves on (round to the first after the last) and returns it; null when the deck is empty. */
    public String draw() {
        if (pages.isEmpty()) return null;
        String p = pages.get(cursor);
        read[cursor] = true;
        cursor = (cursor + 1) % pages.size();
        return p;
    }

    /** Turns past the next page without reading it (it stays unread) and returns the page that was skipped; null when the deck is empty. */
    public String skip() {
        if (pages.isEmpty()) return null;
        String p = pages.get(cursor);
        cursor = (cursor + 1) % pages.size();
        return p;
    }

    /** How many pages have been read at least once since the last reset. */
    public int readCount() {
        int n = 0;
        for (boolean b : read) if (b) n++;
        return n;
    }

    public boolean isRead(String page) {
        int i = pages.indexOf(page);
        return i >= 0 && read[i];
    }

    /** True when every page has been read at least once (an empty deck is complete: it has nothing left to read). */
    public boolean complete() { return readCount() == pages.size(); }

    /** Marks every page unread and goes back to the first page. */
    public void resetProgress() {
        for (int i = 0; i < read.length; i++) read[i] = false;
        cursor = 0;
    }

    /** True when this deck holds exactly these pages in this order. */
    public boolean sameAs(List<String> other) { return pages.equals(other); }

    /** A copy of the pages in a new list (for a message). */
    public List<String> copyOfPages() { return new ArrayList<>(pages); }
}
