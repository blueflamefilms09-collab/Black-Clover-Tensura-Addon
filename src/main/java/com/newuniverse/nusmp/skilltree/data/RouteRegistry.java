package com.newuniverse.nusmp.skilltree.data;

import java.util.List;

/**
 * Why: the one place that knows the skill trees that exist: loaded from {@code data/nusmp/skill_trees/*.json} on the server by a reload listener and copied to every client by a sync
 * packet (a datapack's own JSON is not sent to clients by the game), so the same lookups work on both sides. The core builder replaces the whole class with the real registry (the
 * reload listener, the validation, the client copy, the index by book and by skill); the stub knows no route.
 */
public final class RouteRegistry {
    private RouteRegistry() {}

    /** Every route, in file-name order. */
    public static List<SkillRoute> all() { return List.of(); }

    /** The route with this id, or null. */
    public static SkillRoute byId(String routeId) { return null; }

    /** The route a skill book id opens, or null. */
    public static SkillRoute byBook(String bookId) { return null; }

    /** The route whose class id this is, or null. */
    public static SkillRoute byClass(String classId) { return null; }

    /** The route and node that grant a ManasCore skill id, as {route, node}, or null. */
    public static String[] bySkill(String skillId) { return null; }
}
