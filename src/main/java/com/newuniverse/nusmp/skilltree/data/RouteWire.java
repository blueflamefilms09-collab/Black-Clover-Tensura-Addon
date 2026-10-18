package com.newuniverse.nusmp.skilltree.data;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the game does not send a mod's own datapack JSON to clients, so the server has to ship the validated routes in a packet, and the byte layout of that packet is a contract that must never
 * silently drift between the writer and the reader. This class IS that layout, written once as a sequence of primitive calls (a variable-length integer, a bounded string, a double) on two tiny
 * interfaces, {@link Sink} and {@link Source}. The Minecraft side ({@code RouteCodecs}) binds them to a {@code FriendlyByteBuf}; the tests bind them to plain byte arrays, so the round trip, the size
 * and the refusal of oversized input are proved without a game. A third sink counts bytes, which is how the registry refuses, at LOAD time and with a readable message, a route that would not fit
 * in a packet (64 KB per route, 900 KB for all routes, safely below the 1 MiB a clientbound custom payload may carry): a datapack mistake is then reported on the server console instead of
 * kicking every player who joins. Every count and every string is bounded by the same limits as {@link RouteSchema}, and the reader fails loudly (an exception, which drops that one connection)
 * on anything above them (and on a number outside the range the schema allows, because the screen relies on those ranges): a packet is never trusted to be sane. There is deliberately no version byte inside the layout: the payload protocol version registered with NeoForge ("1") already
 * rejects a peer with another layout, and a second version number would only have two places to forget to bump.
 */
public final class RouteWire {
    private RouteWire() {}

    /**
     * Why: the layout is written once against these three primitives so the same code can be bound to a Minecraft buffer in the game, to a byte array in a test and to a byte counter for the
     * size check, which is what lets the layout be proved without a game. This is the writing side.
     */
    public interface Sink {
        void varInt(int value);

        /** A string of at most {@code maxChars} characters; longer is a bug of the writer and throws. */
        void utf(String value, int maxChars);

        void f64(double value);
    }

    /**
     * Why: the reading side of the same three primitives (see {@link Sink}); every method throws on a truncated or oversized input, so a packet that cannot be right is never half-read into a record.
     */
    public interface Source {
        int varInt();

        String utf(int maxChars);

        double f64();
    }

    /** One route may encode to at most this many bytes. */
    public static final int MAX_ROUTE_BYTES = 65_536;
    /** At most this many routes are synced. */
    public static final int MAX_ROUTES = 64;
    /** All routes together may encode to at most this many bytes (a clientbound custom payload may carry 1 MiB). */
    public static final int MAX_TOTAL_BYTES = 900_000;

    // ---------------------------------------------------------------- write
    public static void write(Sink out, SkillRoute r) {
        out.utf(r.id(), RouteSchema.LEN_ROUTE_ID);
        out.utf(r.magic(), RouteSchema.LEN_KEY);
        out.utf(r.name(), RouteSchema.LEN_NAME);
        out.utf(r.desc(), RouteSchema.LEN_ROUTE_DESC);
        out.utf(r.book(), RouteSchema.LEN_RESOURCE);
        out.utf(r.bookName(), RouteSchema.LEN_NAME);
        out.utf(r.bookLore(), RouteSchema.LEN_LORE);
        out.varInt(r.color() & 0xFFFFFF);
        out.utf(r.icon(), RouteSchema.LEN_RESOURCE);
        out.utf(r.classId(), RouteSchema.LEN_ID);
        out.varInt(r.duplicateSp());
        out.varInt(r.lootWeight());
        out.utf(nz(r.franchise()), RouteSchema.LEN_KEY);                      // 1.82.0: the line fields (empty for a classic route)
        out.utf(nz(r.guiBackground()), RouteSchema.LEN_PATH);
        out.utf(nz(r.bookTexture()), RouteSchema.LEN_PATH);
        out.utf(nz(r.backgroundTheme()), RouteSchema.LEN_KEY);
        out.varInt(r.nodes().size());
        for (SkillNode n : r.nodes()) writeNode(out, n);
    }

    private static void writeNode(Sink out, SkillNode n) {
        out.utf(n.id(), RouteSchema.LEN_ID);
        out.utf(n.name(), RouteSchema.LEN_NAME);
        out.utf(n.desc(), RouteSchema.LEN_NODE_DESC);
        out.varInt(n.parents().size());
        for (SkillNode.Parent p : n.parents()) {
            out.utf(p.node(), RouteSchema.LEN_ID);
            out.varInt(p.level());
        }
        out.varInt(n.cost());
        out.varInt(n.costs().size());
        for (int c : n.costs()) out.varInt(c);
        out.varInt(n.maxLevel());
        out.utf(n.skill(), RouteSchema.LEN_RESOURCE);
        out.varInt(n.passives().size());
        for (String s : n.passives()) out.utf(s, RouteSchema.LEN_KEY);
        out.varInt(n.values().size());
        for (Map.Entry<String, List<Double>> e : n.values().entrySet()) {
            out.utf(e.getKey(), RouteSchema.LEN_KEY);
            out.varInt(e.getValue().size());
            for (double d : e.getValue()) out.f64(d);
        }
        out.varInt(n.x());
        out.varInt(n.y());
        out.utf(n.icon(), RouteSchema.LEN_RESOURCE);
        out.utf(nz(n.type()), RouteSchema.LEN_KEY);                           // 1.82.0: the effect of a line node
        out.utf(nz(n.kind()), RouteSchema.LEN_KEY);
        Map<String, String> params = n.params() == null ? Map.of() : n.params();
        out.varInt(params.size());
        for (Map.Entry<String, String> e : params.entrySet()) {
            out.utf(e.getKey(), RouteSchema.LEN_KEY);
            out.utf(e.getValue(), RouteSchema.LEN_PARAM);
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }

    /** All routes: a count, then each route. */
    public static void writeAll(Sink out, List<SkillRoute> routes) {
        out.varInt(routes.size());
        for (SkillRoute r : routes) write(out, r);
    }

    // ---------------------------------------------------------------- read
    public static SkillRoute read(Source in) {
        String id = in.utf(RouteSchema.LEN_ROUTE_ID);
        String magic = in.utf(RouteSchema.LEN_KEY);
        String name = in.utf(RouteSchema.LEN_NAME);
        String desc = in.utf(RouteSchema.LEN_ROUTE_DESC);
        String book = in.utf(RouteSchema.LEN_RESOURCE);
        String bookName = in.utf(RouteSchema.LEN_NAME);
        String bookLore = in.utf(RouteSchema.LEN_LORE);
        int color = in.varInt() & 0xFFFFFF;
        String icon = in.utf(RouteSchema.LEN_RESOURCE);
        String classId = in.utf(RouteSchema.LEN_ID);
        int duplicateSp = range(in.varInt(), 0, RouteSchema.MAX_DUPLICATE_SP, "duplicate_sp");
        int lootWeight = range(in.varInt(), 0, RouteSchema.MAX_LOOT_WEIGHT, "loot_weight");
        String franchise = in.utf(RouteSchema.LEN_KEY);
        String guiBackground = in.utf(RouteSchema.LEN_PATH);
        String bookTexture = in.utf(RouteSchema.LEN_PATH);
        String backgroundTheme = in.utf(RouteSchema.LEN_KEY);
        boolean extended = !franchise.isEmpty();                              // the same rule as the schema: only a line may use the wider grid
        int n = range(in.varInt(), 1, RouteSchema.MAX_NODES, "nodes");
        List<SkillNode> nodes = new ArrayList<>(n);
        for (int i = 0; i < n; i++) nodes.add(readNode(in, extended));
        return new SkillRoute(id, magic, name, desc, book, bookName, bookLore, color, icon, classId, duplicateSp, lootWeight, Collections.unmodifiableList(nodes),
                franchise, guiBackground, bookTexture, backgroundTheme);
    }

    private static SkillNode readNode(Source in, boolean extended) {
        String id = in.utf(RouteSchema.LEN_ID);
        String name = in.utf(RouteSchema.LEN_NAME);
        String desc = in.utf(RouteSchema.LEN_NODE_DESC);
        int np = count(in.varInt(), RouteSchema.MAX_PARENTS, "parents");
        List<SkillNode.Parent> parents = new ArrayList<>(np);
        for (int i = 0; i < np; i++) parents.add(new SkillNode.Parent(in.utf(RouteSchema.LEN_ID), range(in.varInt(), 1, RouteSchema.MAX_LEVEL, "parent level")));
        int cost = range(in.varInt(), 1, RouteSchema.MAX_COST, "cost");
        int nc = count(in.varInt(), RouteSchema.MAX_LEVEL, "costs");
        List<Integer> costs = new ArrayList<>(nc);
        for (int i = 0; i < nc; i++) costs.add(range(in.varInt(), 1, RouteSchema.MAX_COST, "a price"));
        int maxLevel = range(in.varInt(), 1, RouteSchema.MAX_LEVEL, "max_level");
        String skill = in.utf(RouteSchema.LEN_RESOURCE);
        int npa = count(in.varInt(), RouteSchema.MAX_PASSIVES, "passives");
        List<String> passives = new ArrayList<>(npa);
        for (int i = 0; i < npa; i++) passives.add(in.utf(RouteSchema.LEN_KEY));
        int nv = count(in.varInt(), RouteSchema.MAX_VALUE_KEYS, "value tables");
        Map<String, List<Double>> values = new LinkedHashMap<>();
        for (int i = 0; i < nv; i++) {
            String key = in.utf(RouteSchema.LEN_KEY);
            int len = count(in.varInt(), RouteSchema.MAX_LEVEL, "value entries");
            List<Double> table = new ArrayList<>(len);
            for (int j = 0; j < len; j++) table.add(in.f64());
            values.put(key, Collections.unmodifiableList(table));
        }
        int x = range(in.varInt(), 0, extended ? RouteSchema.MAX_LINE_X : RouteSchema.MAX_X, "x");
        int y = range(in.varInt(), 0, extended ? RouteSchema.MAX_LINE_Y : RouteSchema.MAX_Y, "y");
        String icon = in.utf(RouteSchema.LEN_RESOURCE);
        String type = in.utf(RouteSchema.LEN_KEY);
        String kind = in.utf(RouteSchema.LEN_KEY);
        int np2 = count(in.varInt(), RouteSchema.MAX_PARAMS, "params");
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i < np2; i++) params.put(in.utf(RouteSchema.LEN_KEY), in.utf(RouteSchema.LEN_PARAM));
        return new SkillNode(id, name, desc, Collections.unmodifiableList(parents), cost, Collections.unmodifiableList(costs), maxLevel, skill,
                Collections.unmodifiableList(passives), Collections.unmodifiableMap(values), x, y, icon, type, kind, Collections.unmodifiableMap(params));
    }

    /** All routes: a count (at most {@value #MAX_ROUTES}), then each route. */
    public static List<SkillRoute> readAll(Source in) {
        int n = count(in.varInt(), MAX_ROUTES, "routes");
        List<SkillRoute> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(read(in));
        return Collections.unmodifiableList(out);
    }

    private static int count(int value, int max, String what) { return range(value, 0, max, what); }

    /** A number read off the wire, held to the same range the schema enforces on the file (the GUI relies on these bounds). */
    private static int range(int value, int min, int max, String what) {
        if (value < min || value > max) throw new IllegalArgumentException("a route packet carries " + what + " = " + value + " (allowed " + min + " to " + max + ")");
        return value;
    }

    // ---------------------------------------------------------------- chunks
    /**
     * Why: a clientbound payload carries at most 1 MiB, and a player who owns many lines (an administrator who unlocked them all) has more routes than one payload may hold, so the routes
     * are cut into consecutive groups of at most {@code maxRoutes} routes and {@code maxBytes} bytes, each of which is one payload. A route that alone exceeds {@code maxBytes} still gets its own
     * group (the registry refused such a route at load time, so this cannot happen for a loaded route). Order is kept. Pure and exact (it uses {@link #size}).
     */
    public static List<List<SkillRoute>> chunks(List<SkillRoute> routes, int maxRoutes, int maxBytes) {
        List<List<SkillRoute>> out = new ArrayList<>();
        List<SkillRoute> cur = new ArrayList<>();
        int bytes = 0;
        for (SkillRoute r : routes) {
            int size = size(r);
            if (!cur.isEmpty() && (cur.size() >= maxRoutes || bytes + size > maxBytes)) {
                out.add(cur);
                cur = new ArrayList<>();
                bytes = 0;
            }
            cur.add(r);
            bytes += size;
        }
        if (!cur.isEmpty()) out.add(cur);
        return out;
    }

    // ---------------------------------------------------------------- size
    /**
     * The exact number of bytes {@link #write} produces for this route with the Minecraft primitives (a variable-length integer is 1 to 5 bytes, a string is its length as a variable-length
     * integer plus its UTF-8 bytes, a double is 8). Throws {@link IllegalArgumentException} when a string is longer than the wire allows, which is the same condition under which the real
     * writer would fail: the registry catches it and refuses the route instead of finding out when a player joins.
     */
    public static int size(SkillRoute r) {
        Counter c = new Counter();
        write(c, r);
        return c.bytes;
    }

    /** Bytes of a variable-length integer in the Minecraft layout (LEB128, 5 bytes for a negative number). */
    public static int varIntSize(int value) {
        for (int i = 1; i < 5; i++) if ((value & (-1 << (i * 7))) == 0) return i;
        return 5;
    }

    private static final class Counter implements Sink {
        int bytes;

        @Override public void varInt(int value) { bytes += varIntSize(value); }

        @Override
        public void utf(String value, int maxChars) {
            if (value.length() > maxChars) throw new IllegalArgumentException("a text of " + value.length() + " characters does not fit the wire limit of " + maxChars + ": '" + RouteSchema.shorten(value) + "'");
            int len = value.getBytes(StandardCharsets.UTF_8).length;
            bytes += varIntSize(len) + len;
        }

        @Override public void f64(double value) { bytes += 8; }
    }
}
