package com.newuniverse.nusmp.client.companion.gs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the player has learnt about a companion's taste: after the FIRST reaction to an item (loved, plainly liked, hated or refused) the Gift tab shows a small hint on that item, in the picker and in the
 * "tastes" notebook. The memory lives on the client only (nothing is asked of the server), is bounded (at most {@link #MAX_COMPANIONS} companions, {@link #MAX_ITEMS} items each; the least recently used one
 * goes first) and is dropped on logout. Keys are plain strings (a companion's lang prefix or name, an item's registry id), so it is pure and unit tested.
 */
public final class GiftMemory {
    /** How a companion took a gift. */
    public enum Reaction { LOVED, LIKED, HATED, REFUSED }

    public static final int MAX_COMPANIONS = 8, MAX_ITEMS = 24, MAX_KEY = 80;

    private final Map<String, Map<String, Reaction>> byCompanion = new LinkedHashMap<>(16, 0.75f, true);

    private static String cut(String s) { return s == null ? "" : s.length() <= MAX_KEY ? s : s.substring(0, MAX_KEY); }

    /** Remembers a reaction (the newest answer for an item replaces an older one). */
    public void record(String companion, String item, Reaction r) {
        if (companion == null || companion.isEmpty() || item == null || item.isEmpty() || r == null) return;
        String c = cut(companion), i = cut(item);
        Map<String, Reaction> m = byCompanion.get(c);
        if (m == null) {
            if (byCompanion.size() >= MAX_COMPANIONS) {
                Iterator<String> it = byCompanion.keySet().iterator();
                it.next();
                it.remove();                                                          // the least recently used companion
            }
            m = new LinkedHashMap<>(16, 0.75f, true);
            byCompanion.put(c, m);
        }
        m.remove(i);                                                                  // re-insert as the newest
        if (m.size() >= MAX_ITEMS) {
            Iterator<String> it = m.keySet().iterator();
            it.next();
            it.remove();
        }
        m.put(i, r);
    }

    /** The remembered reaction, or null when this item has never been tried on this companion. */
    public Reaction get(String companion, String item) {
        Map<String, Reaction> m = byCompanion.get(cut(companion));
        return m == null || item == null ? null : m.get(cut(item));
    }

    public int count(String companion) {
        Map<String, Reaction> m = byCompanion.get(cut(companion));
        return m == null ? 0 : m.size();
    }

    public int companions() { return byCompanion.size(); }

    /**
     * The notebook: up to 'max' remembered items of a companion, loved first, then hated, then plainly liked, then refused (the newest first inside a group). Fills the two lists (cleared first).
     */
    public void notebook(String companion, int max, List<String> items, List<Reaction> reactions) {
        items.clear();
        reactions.clear();
        Map<String, Reaction> m = byCompanion.get(cut(companion));
        if (m == null) return;
        List<Map.Entry<String, Reaction>> all = new ArrayList<>(m.entrySet());
        for (Reaction want : new Reaction[]{Reaction.LOVED, Reaction.HATED, Reaction.LIKED, Reaction.REFUSED}) {
            for (int k = all.size() - 1; k >= 0 && items.size() < max; k--) {
                if (all.get(k).getValue() == want) {
                    items.add(all.get(k).getKey());
                    reactions.add(want);
                }
            }
        }
    }

    public void clear() { byCompanion.clear(); }
}
