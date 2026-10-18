package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.data.RouteWire;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Why: the decision "which routes does THIS player get, in how many packets, and when must they be sent again" is the heart of the visibility lock and of the scale rule (283 lines must not become
 * 283 trees on every client), and a decision that lives inside a packet handler cannot be tested without a server. This is that decision as pure functions: the routes a player owns (and only those,
 * in id order, skipping ids the registry does not know), the cut of those routes into packets that respect both payload limits, and a signature of the owned set so the server re-sends the routes
 * only when the player's ownership changed (a book was read, a line taken away) and not on every tree sync. The signature is an order-independent hash of the ids, with the size mixed in, so it
 * is stable across runs and cheap.
 */
public final class LineSyncPlan {
    private LineSyncPlan() {}

    /** The full routes a player must hold: the ones they own that the registry has, in id order. */
    public static List<SkillRoute> owned(Collection<String> ownedIds, Function<String, SkillRoute> byId) {
        List<SkillRoute> out = new ArrayList<>();
        for (String id : new TreeSet<>(ownedIds)) {
            SkillRoute r = byId.apply(id);
            if (r != null) out.add(r);
        }
        return out;
    }

    /** The packets of a sync: consecutive groups of at most {@link RouteWire#MAX_ROUTES} routes and {@link #PAYLOAD_BYTES} bytes. The first group replaces the client's copy, the others are added. */
    public static List<List<SkillRoute>> packets(List<SkillRoute> routes) { return RouteWire.chunks(routes, RouteWire.MAX_ROUTES, PAYLOAD_BYTES); }

    /** A packet's byte budget: well under the 1 MiB a clientbound custom payload may carry (the route cap is {@link RouteWire#MAX_ROUTE_BYTES}, so one route always fits). */
    public static final int PAYLOAD_BYTES = 800_000;

    /** An order-independent signature of a set of ids: equal sets give equal signatures, a different set almost surely a different one. */
    public static int signature(Collection<String> ids) {
        int sum = 0, xor = 0;
        for (String id : ids) {
            int h = id.hashCode() * 0x9E3779B1;
            sum += h;
            xor ^= Integer.rotateLeft(h, 13);
        }
        return sum * 31 + xor + ids.size() * 0x85EBCA6B;
    }
}
