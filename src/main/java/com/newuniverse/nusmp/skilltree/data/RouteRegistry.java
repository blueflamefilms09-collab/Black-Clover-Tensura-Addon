package com.newuniverse.nusmp.skilltree.data;

import com.newuniverse.nusmp.skilltree.line.LineCard;

import java.util.List;

/**
 * Why: the one place that knows the skill trees that exist, answering the same questions (by id, by book, by class, by skill) on the server, where the trees are loaded from
 * {@code data/<namespace>/skill_trees/*.json} by {@link RouteLoader}, and on a client, where the game never sends a mod's own datapack JSON and the server's validated copy arrives in a packet.
 * It is a static facade over two immutable {@link RouteIndex} snapshots, a SERVER one and a CLIENT one, each swapped by a single volatile write, so a reader on any thread sees either the whole
 * old set or the whole new set and never a half-applied reload, with no lock. The lookups read the server snapshot while a server is running in this JVM (a dedicated server, or the
 * integrated server of a single-player world, where the client screen and the server rules then share one set and cannot disagree) and the client copy otherwise (a client on a remote
 * server). Everything is static state, so it is cleared when the server stops ({@link #clear}) and the client copy when the client leaves a world ({@link #clearClient}); it never outlives a
 * session. The public lookups keep the signatures of the contract; the extra methods are the writes that only the loader, the sync packet and the reload command use.
 */
public final class RouteRegistry {
    private RouteRegistry() {}

    private static volatile RouteIndex server = RouteIndex.EMPTY;
    private static volatile boolean serverLoaded;
    private static volatile RouteIndex client = RouteIndex.EMPTY;

    /** The snapshot in force on this side: the server's while a server runs in this JVM, else the copy a server sent. */
    public static RouteIndex snapshot() { return serverLoaded ? server : client; }

    // ---------------------------------------------------------------- the lookups of the contract
    /** Every route, in file-name order. */
    public static List<SkillRoute> all() { return snapshot().all(); }

    /** The route with this id, or null. */
    public static SkillRoute byId(String routeId) { return snapshot().byId(routeId); }

    /** The route a skill book id opens, or null. */
    public static SkillRoute byBook(String bookId) { return snapshot().byBook(bookId); }

    /** The route whose class id this is, or null. */
    public static SkillRoute byClass(String classId) { return snapshot().byClass(classId); }

    /** The route and node that grant a ManasCore skill id, as {route, node}, or null. */
    public static String[] bySkill(String skillId) { return snapshot().bySkill(skillId); }

    // ---------------------------------------------------------------- conveniences for the abilities
    /** The node of a route, or null when either is unknown (a datapack removed it, the client has not received the routes yet). */
    public static SkillNode node(String routeId, String nodeId) {
        SkillRoute r = byId(routeId);
        return r == null ? null : r.node(nodeId);
    }

    /**
     * The named number of a node at a level, or {@code fallback} when the route, the node or the table is unknown: what an ability reads its damage, range and duration from, so a missing
     * route degrades to the ability's own default instead of throwing in the middle of a cast.
     */
    public static double value(String routeId, String nodeId, String key, int level, double fallback) {
        SkillNode n = node(routeId, nodeId);
        return n == null ? fallback : n.value(key, level, fallback);
    }

    // ---------------------------------------------------------------- the server side
    /** The report of the last load: one entry per file, with its problems and warnings (what {@code /multiverse tree routes} prints). */
    public static List<RouteIndex.Entry> report() { return snapshot().report(); }

    /** Server thread: puts a freshly built set in force, atomically. Called by the reload listener and by {@code /multiverse tree reload}. */
    public static void publish(RouteIndex index) {
        server = index == null ? RouteIndex.EMPTY : index;
        serverLoaded = true;
    }

    /** The server stopped: forget its routes, so the next world (or a connection to another server) starts from nothing. */
    public static void clear() {
        serverLoaded = false;
        server = RouteIndex.EMPTY;
    }

    // ---------------------------------------------------------------- the client side
    /** Client thread: the routes the server sent replace the client copy as a whole. */
    public static void setClient(List<SkillRoute> routes) {
        clientFull = routes == null ? List.of() : List.copyOf(routes);
        rebuildClient();
    }

    /** The client left its world: forget the copy of that server's routes. */
    public static void clearClient() {
        clientFull = List.of();
        clientCards = List.of();
        client = RouteIndex.EMPTY;
    }

    // ---------------------------------------------------------------- 1.82.0: what a client holds (the lines it owns in full, a node-less stub of every other line)
    private static volatile List<SkillRoute> clientFull = List.of();
    private static volatile List<LineCard> clientCards = List.of();

    /**
     * Client thread: routes that arrive AFTER the first chunk of a sync, or that an administrator asked to inspect, are added to the client copy (a route with the same id is replaced). Why a
     * merge and not another {@link #setClient}: with 283 lines a player's owned routes may not fit one payload, and an inspected line must not wipe the lines the player owns.
     */
    public static void addClient(List<SkillRoute> routes) {
        if (routes == null || routes.isEmpty()) return;
        java.util.Map<String, SkillRoute> merged = new java.util.LinkedHashMap<>();
        for (SkillRoute r : clientFull) merged.put(r.id(), r);
        for (SkillRoute r : routes) merged.put(r.id(), r);
        clientFull = List.copyOf(merged.values());
        rebuildClient();
    }

    /**
     * Client thread: the cards of every registered line. Each becomes a node-less stub route so that the code that names a Skill Book, lists the Black Market or fills the creative tab, which asks
     * this registry by id and by book, works for lines the player does not own; a line the player owns (or inspected) is the full route, never the stub.
     */
    public static void setClientCards(List<LineCard> cards) {
        clientCards = cards == null ? List.of() : List.copyOf(cards);
        rebuildClient();
    }

    private static void rebuildClient() {
        java.util.Map<String, SkillRoute> all = new java.util.LinkedHashMap<>();
        for (SkillRoute r : clientFull) all.put(r.id(), r);
        for (LineCard c : clientCards) all.computeIfAbsent(c.id(), k -> c.toStub());
        java.util.List<SkillRoute> list = new java.util.ArrayList<>(all.values());
        list.sort(java.util.Comparator.comparing(SkillRoute::id));
        client = RouteIndex.of(list);
    }

    /**
     * One card per registered line, in id order: computed from the server snapshot while a server runs in this JVM, else the cards the server sent. Contains every line (the cards carry no
     * nodes), so the Passives panel, the inspect list and the books can name a line; whether a player may SEE a line in the library is decided by ownership, not by this list.
     */
    public static List<LineCard> cards() { return serverLoaded ? server.cards() : clientCards; }
}
