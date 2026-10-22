package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the owner asked to keep the dungeon's size and rebuild the INSIDES of its rooms, halls and hallways. The plan (which room goes where, how it is turned, which doors lead
 * where) is made from the shipped templates and must not change, because the cyclic loop planner, the dead-end pruner, the boss chain, the stairs and every saved sector rest on it.
 * So the rebuilt rooms are separate template files with exactly the shipped template's size, sockets and data markers ({@code structure/mega_r/<folder>/<stem>_v1 .. _vN}, written by
 * {@code tools/labyrinth/gen_rooms_rebuilt.py} and proved by {@code Rooms*Test}), and the only thing that changes is WHICH FILE a placed piece is built from: the plan keeps naming the
 * shipped template, this class names the rebuilt one that stands in its place. The choice is a pure function of the plan and its seed, so a regeneration or a repair gives the same
 * rooms, and the first N pieces of one template in a plan get N different variants (no two rooms of a floor look alike while variants last).
 * <p>
 * The table of which shipped template has how many variants is {@code data/nusmp/mega/rooms_rebuilt.json}, read from the class path (it is the mod's own data; no data pack is needed
 * and it works while the registry is still being built). A missing or unreadable file is an empty table: nothing is swapped, the old rooms are built. Pure: no Minecraft type.
 */
public final class RoomVariants {
    private RoomVariants() {}

    /** The resource the table is read from. */
    public static final String RESOURCE = "/data/nusmp/mega/rooms_rebuilt.json";
    /** The folder under {@code structure/} that holds the rebuilt files. */
    public static final String FOLDER = "mega_r";
    /** No template has more variants than this (the file names are {@code _v1} to {@code _v5} at most). */
    public static final int MAX = 5;

    /** The shipped templates that have variants (with their count) and the rebuilt rooms that carry the single armour stand slot. */
    public record Table(Map<String, Integer> counts, Set<String> standRooms, String problem) {
        public static Table empty() { return new Table(Map.of(), Set.of(), ""); }

        public int count(String shippedId) { return counts.getOrDefault(bare(shippedId), 0); }

        public boolean has(String shippedId) { return count(shippedId) > 0; }

        public boolean standRoom(String shippedId) { return standRooms.contains(bare(shippedId)); }

        public int size() { return counts.size(); }
    }

    private static volatile Table cached;

    /** The table of the mod (read once). */
    public static Table table() {
        Table t = cached;
        if (t == null) {
            t = load();
            cached = t;
        }
        return t;
    }

    private static Table load() {
        try (InputStream in = RoomVariants.class.getResourceAsStream(RESOURCE)) {
            if (in == null) return new Table(Map.of(), Set.of(), "no " + RESOURCE);
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(r));
            }
        } catch (Exception e) {
            return new Table(Map.of(), Set.of(), e.toString());
        }
    }

    /** Parses the table from its JSON: {@code variants} (template to count, counts above {@link #MAX} or below 1 are ignored) and {@code stand_rooms} (a list of templates). */
    public static Table parse(JsonElement root) {
        Map<String, Integer> counts = new HashMap<>();
        Set<String> stands = new HashSet<>();
        if (root != null && root.isJsonObject()) {
            JsonObject o = root.getAsJsonObject();
            if (o.has("variants") && o.get("variants").isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("variants").entrySet()) {
                    int n = e.getValue().isJsonPrimitive() ? e.getValue().getAsInt() : 0;
                    if (n >= 1 && n <= MAX) counts.put(bare(e.getKey()), n);
                }
            }
            if (o.has("stand_rooms") && o.get("stand_rooms").isJsonArray()) {
                for (JsonElement e : o.getAsJsonArray("stand_rooms")) if (e.isJsonPrimitive()) stands.add(bare(e.getAsString()));
            }
        }
        return new Table(Map.copyOf(counts), Set.copyOf(stands), "");
    }

    /** {@code nusmp:mega/hall/x} and {@code mega/hall/x} are the same template. */
    public static String bare(String id) {
        if (id == null) return "";
        int c = id.indexOf(':');
        return c >= 0 ? id.substring(c + 1) : id;
    }

    /**
     * The id of variant {@code k} (1-based) of a shipped template: {@code mega/hall/library_a} becomes {@code mega_r/hall/library_a_v2}. The namespace of the argument is kept
     * ({@code nusmp:mega/hall/library_a} becomes {@code nusmp:mega_r/hall/library_a_v2}). A template that is not under {@code mega/} has no variant: the argument comes back.
     */
    public static String variantId(String shippedId, int k) {
        String ns = "";
        String p = shippedId;
        int c = p.indexOf(':');
        if (c >= 0) {
            ns = p.substring(0, c + 1);
            p = p.substring(c + 1);
        }
        if (!p.startsWith("mega/")) return shippedId;
        return ns + FOLDER + "/" + p.substring("mega/".length()) + "_v" + k;
    }

    /** Is this the id of a rebuilt file (not a shipped template)? */
    public static boolean isVariantId(String id) { return bare(id).startsWith(FOLDER + "/"); }

    /** The shipped template a rebuilt file stands in for ({@code mega_r/hall/library_a_v2} gives {@code mega/hall/library_a}), or the argument when it is not a rebuilt file. */
    public static String shippedOf(String id) {
        String ns = "";
        String p = id;
        int c = p.indexOf(':');
        if (c >= 0) {
            ns = p.substring(0, c + 1);
            p = p.substring(c + 1);
        }
        if (!p.startsWith(FOLDER + "/")) return id;
        int v = p.lastIndexOf("_v");
        if (v < 0) return id;
        return ns + "mega/" + p.substring(FOLDER.length() + 1, v);
    }

    /**
     * Chooses the template each piece of a plan is built from. {@code ids} are the shipped templates of the pieces in plan order; the answer has the same length. A piece whose
     * template has variants gets one of them (never the shipped one: this is only called when the rebuilt rooms are on); the first {@code count} pieces of the same template get
     * {@code count} different variants, in an order that depends on the seed, and after that the order repeats with a seed-dependent shift, so neighbouring copies still differ.
     * Every other piece keeps its template. Deterministic: the same ids and seed give the same answer on every machine.
     */
    public static String[] assign(List<String> ids, long seed, Table table) {
        String[] out = new String[ids.size()];
        Map<String, Integer> seen = new HashMap<>();
        for (int i = 0; i < out.length; i++) {
            String id = ids.get(i);
            int n = table.count(id);
            if (n <= 0 || !id.contains("mega/")) {
                out[i] = id;
                continue;
            }
            int nth = seen.merge(bare(id), 1, Integer::sum) - 1;
            long h = Rng.mix(seed, bare(id).hashCode() * 31L + 7L);
            int start = (int) Long.remainderUnsigned(h >>> 1, n);
            int k = Math.floorMod(start + nth + nth / n, n) + 1;                             // a rotation: every variant appears once per n pieces
            out[i] = variantId(id, k);
        }
        return out;
    }

    /** The shipped templates the plan names, to feed {@link #assign}. */
    public static List<String> templatesOf(List<Placed> pieces) {
        List<String> out = new ArrayList<>(pieces.size());
        for (Placed p : pieces) out.add(p.shape.id);
        return out;
    }
}
