package com.newuniverse.nusmp.relics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the client has two ways to know what a wearer has on that vanilla does not tell it: its own read of the Curios inventory (reflection, which can fail) and the list the
 * server sends ({@link WornSync}; the only way for a named mob's pack, which lives in persistent data that never leaves the server). The client uses both and must not draw a relic
 * twice when both know it. This is that rule, on plain strings so a test can run it: every stack of the server's list that the client's own look did not already find (by item, one
 * for one) is added. Pure.
 */
public final class WornMerge {
    private WornMerge() {}

    /**
     * @param found  the item keys (registry ids) of what the client's own look found, hands excluded
     * @param synced the item keys of the server's list, in order
     * @return the indexes into {@code synced} that the client's look did not find, ascending (a key present twice in {@code synced} and once in {@code found} yields the second)
     */
    public static List<Integer> missing(List<String> found, List<String> synced) {
        Map<String, Integer> have = new HashMap<>();
        for (String k : found) if (k != null) have.merge(k, 1, Integer::sum);
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < synced.size(); i++) {
            String k = synced.get(i);
            if (k == null) continue;
            Integer n = have.get(k);
            if (n != null && n > 0) have.put(k, n - 1);
            else out.add(i);
        }
        return out;
    }

    /** A cheap, order-sensitive signature of a list of item keys and counts, to tell whether the server's list changed since it was last sent. */
    public static String signature(List<String> keys) {
        StringBuilder b = new StringBuilder(keys.size() * 24);
        for (String k : keys) b.append(k).append('|');
        return b.toString();
    }
}
