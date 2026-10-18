package com.newuniverse.nusmp.client.skilltree.layout;

import com.newuniverse.nusmp.skilltree.data.SkillRoute;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the route list is grouped by magic family (Fairy Tail today, any other anime tomorrow) because the owner's design makes a family the unit a player thinks in, and the families
 * are not a list anyone maintains: they are whatever the {@code magic} field of the loaded routes says, so a datapack that adds a route for a new family gets its own heading with no
 * code change. Grouping in a pure function keeps two guarantees that a screen-side loop would be too easy to break: the families appear in the order their first route does, and
 * the routes inside a family keep the registry's order, so the list does not reshuffle itself when the sync packet arrives or a route is unlocked.
 */
public final class RouteGrouping {
    private RouteGrouping() {}

    /** One heading of the route list: the {@code magic} id and its routes in registry order. */
    public record Group(String magic, List<SkillRoute> routes) {}

    /** The routes grouped by their {@code magic} field (a missing one is the empty family), families in order of first appearance. */
    public static List<Group> group(List<SkillRoute> routes) {
        Map<String, List<SkillRoute>> by = new LinkedHashMap<>();
        for (SkillRoute r : routes) by.computeIfAbsent(r.magic() == null ? "" : r.magic(), k -> new ArrayList<>()).add(r);
        List<Group> out = new ArrayList<>();
        for (Map.Entry<String, List<SkillRoute>> e : by.entrySet()) out.add(new Group(e.getKey(), List.copyOf(e.getValue())));
        return out;
    }
}
