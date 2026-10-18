package com.newuniverse.nusmp.client.skilltree;

import com.newuniverse.nusmp.client.skilltree.layout.TreeLayout;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;

import java.util.List;

/**
 * Why: laying out a route and routing all its parent lines is real work (an obstacle check per line against every other node), and a route only changes when the server sends a new
 * copy of the registry. Doing it every frame would be waste; doing it once at the first sight of a route and never again would show a stale tree after a datapack reload. So the
 * result is cached with the route record it was computed from, and the screen compares by identity: the same record means the cached shape is still right, a new record (the sync
 * packet delivered a fresh copy) means it is rebuilt on the next frame. No Minecraft types, so a shape can be built in a test.
 */
final class RouteShape {
    final SkillRoute route;
    final TreeLayout layout;
    final List<TreeLayout.Edge> edges;

    private RouteShape(SkillRoute route, TreeLayout layout, List<TreeLayout.Edge> edges) {
        this.route = route; this.layout = layout; this.edges = edges;
    }

    static RouteShape of(SkillRoute route) {
        TreeLayout layout = TreeLayout.of(route.nodes());
        return new RouteShape(route, layout, layout.edges(route.nodes()));
    }
}
