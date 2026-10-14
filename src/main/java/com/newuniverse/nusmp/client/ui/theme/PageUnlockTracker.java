package com.newuniverse.nusmp.client.ui.theme;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decides, from successive status snapshots of the grimoire, which pages are newly open. The first snapshot after logging in (or after the
 * grimoire changed, e.g. a different book was taken) only sets the baseline, so joining a world never announces the pages already open.
 * Pure: the overlay feeds it the grimoire's identity and the ids of the pages it has open.
 */
public final class PageUnlockTracker {
    private String book;
    private final Set<String> known = new HashSet<>();
    private boolean primed;

    /** Forgets everything (logout). */
    public void reset() {
        book = null;
        known.clear();
        primed = false;
    }

    /**
     * @param bookKey  identity of the grimoire (cover and magic); a change re-primes the baseline
     * @param openIds  ids of the pages that are open now, in book order
     * @return the ids that were not open in the previous snapshot of the same grimoire, in order (empty on the baseline)
     */
    public List<String> update(String bookKey, Collection<String> openIds) {
        List<String> fresh = new ArrayList<>();
        String key = bookKey == null ? "" : bookKey;
        if (!primed || !key.equals(book)) {
            book = key;
            known.clear();
            known.addAll(openIds);
            primed = !key.isEmpty();
            return fresh;
        }
        for (String id : openIds) if (known.add(id)) fresh.add(id);
        return fresh;
    }
}
