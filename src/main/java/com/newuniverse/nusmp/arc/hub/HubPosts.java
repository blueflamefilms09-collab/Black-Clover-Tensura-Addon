package com.newuniverse.nusmp.arc.hub;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Hub has a fixed number of posts ({@link ChronicleSite#posts()}) and the roster of friendly characters grows as builders are merged, so WHO stands on
 * WHICH post must be a rule that never reshuffles the ones already standing: a character keeps the post its place in the roster gives it, the guide of the
 * Chronicle (a character the config names, by default the guild adviser of the DanMachi saga) takes the post nearest the lectern whatever its place in the roster,
 * and a roster with more friends than posts leaves the last ones without a post (they are reported, never crammed in). Pure, tested with fake characters.
 */
public final class HubPosts {
    private HubPosts() {}

    /** The result of one assignment: character id to post index, and the ids that found no post. */
    public record Assignment(Map<String, Integer> posts, List<String> unplaced) {}

    /**
     * Gives each character of {@code npcIds} (in roster order, no id twice) a post of {@code postCount}: the guide first (post 0, the nearest the Chronicle) if it
     * is among them, then the others in roster order on the next posts.
     */
    public static Assignment assign(List<String> npcIds, String guideId, int postCount) {
        Map<String, Integer> posts = new LinkedHashMap<>();
        List<String> unplaced = new ArrayList<>();
        int next = 0;
        if (guideId != null && !guideId.isEmpty() && npcIds.contains(guideId) && postCount > 0) posts.put(guideId, next++);
        for (String id : npcIds) {
            if (posts.containsKey(id)) continue;
            if (next < postCount) posts.put(id, next++);
            else unplaced.add(id);
        }
        return new Assignment(posts, unplaced);
    }
}
