package com.newuniverse.nusmp.book;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 1.93.0 magic rebuild: the pure rule every rebuilt book must keep (no Minecraft types, so it is unit tested): the pages that existed before keep their ids at
 * their old places (saves and the mastery bits are keyed by them), the new pages come right after them, and ids are unique.
 */
public final class SpatialPageOrder {
    private SpatialPageOrder() {}

    /** What is wrong with a page list; empty = fine. */
    public static List<String> problems(List<String> ids, List<String> oldIds, List<String> newIds) {
        List<String> bad = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String id : ids) if (!seen.add(id)) bad.add("duplicate page " + id);
        for (int i = 0; i < oldIds.size(); i++) {
            if (i >= ids.size() || !ids.get(i).equals(oldIds.get(i))) bad.add("page " + i + " should be " + oldIds.get(i) + " but is " + (i < ids.size() ? ids.get(i) : "missing"));
        }
        for (int k = 0; k < newIds.size(); k++) {
            int at = oldIds.size() + k;
            if (at >= ids.size() || !ids.get(at).equals(newIds.get(k))) bad.add("new page " + newIds.get(k) + " should be at " + at);
        }
        return bad;
    }
}
