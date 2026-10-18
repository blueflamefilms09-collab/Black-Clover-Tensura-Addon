package com.newuniverse.nusmp.heroweapon.weapon.book;

import java.util.List;

/**
 * Why: a wielder of the Vassal Book holds ONE volume with two chapters (offence and support), each a {@link PageDeck}, and the volume has a single loop: read the
 * pages in order, and when every page of both chapters has been read the volume is COMPLETE and Libraria (the white room, the ultimate) may be cast, which
 * uses the volume up and starts it afresh. This is that volume, with no game imports, so the rules are tested: it rebuilds its decks (and forgets the progress)
 * only when the form's pages really changed, a turn of the page skips the next page of the chapter read last, completion can forgive one missing page (the Grand
 * Library), and consuming it resets both chapters. It lives in memory only.
 */
public final class Volume {
    private PageDeck offense = new PageDeck(List.of());
    private PageDeck support = new PageDeck(List.of());
    private boolean lastWasSupport;

    /** Makes the decks hold these pages; the decks (and the progress) are replaced only when the pages differ from the ones held. */
    public void ensure(List<String> offensePages, List<String> supportPages) {
        if (!offense.sameAs(offensePages)) offense = new PageDeck(offensePages);
        if (!support.sameAs(supportPages)) support = new PageDeck(supportPages);
    }

    public PageDeck offense() { return offense; }

    public PageDeck support() { return support; }

    /** Notes which chapter was read last (a turn of the page turns that one). */
    public void noteRead(boolean supportChapter) { lastWasSupport = supportChapter; }

    /** Turns past the next page of the chapter read last (the offence chapter if none was read yet); returns the page skipped, or null if that chapter is empty. */
    public String turn() { return (lastWasSupport ? support : offense).skip(); }

    public int pagesRead() { return offense.readCount() + support.readCount(); }

    public int pagesTotal() { return offense.size() + support.size(); }

    /** True when at most {@code allowMissing} pages of the whole volume are still unread and the volume has pages at all. */
    public boolean complete(int allowMissing) {
        return pagesTotal() > 0 && pagesTotal() - pagesRead() <= Math.max(0, allowMissing);
    }

    /** The volume has been used up (Libraria was cast): both chapters start afresh. */
    public void consume() {
        offense.resetProgress();
        support.resetProgress();
        lastWasSupport = false;
    }

    /** "3/5": the pages read of the pages held. */
    public String progress() { return pagesRead() + "/" + pagesTotal(); }
}
