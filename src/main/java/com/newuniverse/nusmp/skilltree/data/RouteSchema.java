package com.newuniverse.nusmp.skilltree.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: a skill route is written by hand as a JSON file by people who write magic, not Java, so the loader has to turn a file into a {@link SkillRoute} AND tell them in plain words everything that
 * is wrong with it, all at once, instead of stopping at the first mistake or (worse) loading half a tree without saying so. A Mojang {@code Codec} gives one machine-style error for the whole
 * file and no partial result, which is the opposite of the owner's rule here: a bad ROUTE is skipped while the other routes still load, and a bad NODE is skipped together with the nodes that need
 * it while the rest of the tree still works (a typo in an advanced node must not take a whole magic away from the players). So this is a hand-written, pure reader over Gson's tree (no Minecraft
 * type, so it is unit-tested without a game), it applies the defaults of the design, validates every rule of {@code docs/skilltree/design.md} section 2, and returns the route plus a list of
 * readable problems (errors: something was skipped) and warnings (oddities that still load, such as a misspelt field name that was ignored). The same numeric limits are used by the wire format
 * ({@link RouteWire}), so a route that passes here always fits in a packet. Whether a node's {@code skill} id exists in the ManasCore registry cannot be known here (the registry is a game
 * object); the in-game self-test {@code tree.core.routes} checks it.
 *
 * <p>The rules, in the order they are applied to a node: the id is lower-case snake and unused; the texts are within their lengths; {@code cost} 1..50 (default 1); {@code max_level} 1..10 (default 1);
 * {@code costs} no longer than {@code max_level}, each 1..50; {@code values} tables of finite numbers; {@code x} 0..12 and {@code y} 0..9 (default 0); every parent is a node of the same route that
 * appears EARLIER in the file, is still loaded, and is asked for at a level its {@code max_level} reaches (which also makes a cycle impossible: a parent that comes later is refused, and so is
 * a node that names itself); a {@code skill}, when present, is a lower-case resource location that no other node grants. Route level: the file is an object; {@code name}, {@code book},
 * {@code class_id}, {@code color} and {@code nodes} are present and valid; at most 40 nodes; at least one root node survives. Unique-among-routes rules (book, class id, skill) belong to
 * {@link RouteIndex}, which sees all the files.
 */
public final class RouteSchema {
    private RouteSchema() {}

    // ---------------------------------------------------------------- limits (RouteWire uses the same numbers)
    public static final int MAX_NODES = 40, MAX_PARENTS = 6, MAX_LEVEL = 10, MAX_COST = 50, MAX_PASSIVES = 8, MAX_VALUE_KEYS = 16;
    /** The canvas grid: columns 0..12, rows 0..9. */
    public static final int MAX_X = 12, MAX_Y = 9;
    public static final int MAX_DUPLICATE_SP = 100, MAX_LOOT_WEIGHT = 1000;
    /** A value in a {@code values} table: finite and within this magnitude (a damage, a range, a duration, a share: nothing needs more). */
    public static final double MAX_VALUE = 1.0e9;
    /** Text limits in characters (the wire format writes the same bounds). */
    public static final int LEN_ID = 40, LEN_KEY = 32, LEN_NAME = 64, LEN_ROUTE_DESC = 400, LEN_NODE_DESC = 300, LEN_LORE = 200, LEN_RESOURCE = 64, LEN_ROUTE_ID = 80;

    private static final Pattern SNAKE = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    private static final Set<String> ROUTE_FIELDS = Set.of("magic", "name", "desc", "book", "book_name", "book_lore", "color", "icon", "class_id", "duplicate_sp", "loot_weight", "nodes");
    private static final Set<String> NODE_FIELDS = Set.of("id", "name", "desc", "parents", "cost", "costs", "max_level", "skill", "passives", "values", "x", "y", "icon");

    /**
     * Why: a file can be wrong in many places at once and the author should hear about all of them in one pass, so reading returns a result with three parts instead of throwing at the first
     * mistake. {@code route} is null when the route was refused; {@code problems} lists what was wrong (a route with problems but a non-null record had some nodes skipped); {@code warnings}
     * lists oddities that did not stop anything.
     */
    public record Parsed(SkillRoute route, List<String> problems, List<String> warnings) {
        /** The route loaded (possibly with some nodes skipped). */
        public boolean ok() { return route != null; }

        /** The route loaded and nothing at all was wrong with it: what a shipped file must be. */
        public boolean clean() { return route != null && problems.isEmpty(); }
    }

    // ---------------------------------------------------------------- small checks (also used by RouteIndex and the tests)
    /** {@code a-z}, {@code 0-9}, {@code _}, starting with a letter. */
    public static boolean validSnake(String s) { return s != null && !s.isEmpty() && SNAKE.matcher(s).matches(); }

    /** {@code namespace:path} in lower case with the characters a resource location allows, at most {@value #LEN_RESOURCE} characters. */
    public static boolean validResource(String s) {
        if (s == null || s.length() > LEN_RESOURCE) return false;
        int colon = s.indexOf(':');
        if (colon <= 0 || colon != s.lastIndexOf(':') || colon == s.length() - 1) return false;
        return NAMESPACE.matcher(s.substring(0, colon)).matches() && PATH.matcher(s.substring(colon + 1)).matches();
    }

    /** A route id: {@code <namespace>:<file name>}, the file name lower-case snake (so a file in a sub-folder cannot be a route). */
    public static boolean validRouteId(String id) {
        if (id == null || id.length() > LEN_ROUTE_ID) return false;
        int colon = id.indexOf(':');
        if (colon <= 0 || colon != id.lastIndexOf(':')) return false;
        String name = id.substring(colon + 1);
        return NAMESPACE.matcher(id.substring(0, colon)).matches() && validSnake(name) && name.length() <= LEN_ID;
    }

    /** {@code "#RRGGBB"} to 0xRRGGBB, or -1 when it is not exactly that. */
    public static int parseColor(String s) {
        if (s == null || !COLOR.matcher(s).matches()) return -1;
        return Integer.parseInt(s.substring(1), 16);
    }

    /** 0xRRGGBB to {@code "#RRGGBB"}. */
    public static String colorString(int rgb) { return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF); }

    /** {@code iron_fist} to {@code Iron Fist}: the name a node gets when the file gives none. */
    public static String titleCase(String snake) {
        StringBuilder sb = new StringBuilder();
        for (String w : snake.split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- entry points
    /** Reads a file given as text; invalid JSON is a refused route with one problem. */
    public static Parsed parse(String routeId, String jsonText) {
        try {
            return parse(routeId, JsonParser.parseString(jsonText == null ? "" : jsonText));
        } catch (JsonParseException | IllegalStateException e) {
            List<String> problems = new ArrayList<>();
            problems.add("not valid JSON: " + shorten(String.valueOf(e.getMessage())));
            return new Parsed(null, problems, new ArrayList<>());
        }
    }

    /** Reads a parsed file. {@code routeId} is {@code <namespace>:<file name>}. Never throws: every problem is a line in the result. */
    public static Parsed parse(String routeId, JsonElement json) {
        List<String> problems = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        try {
            return read(routeId, json, problems, warnings);
        } catch (RuntimeException e) {                                       // a shape this reader did not foresee must not take the whole reload down
            problems.add("could not be read (" + e.getClass().getSimpleName() + "): " + shorten(String.valueOf(e.getMessage())));
            return new Parsed(null, problems, warnings);
        }
    }

    private static Parsed read(String routeId, JsonElement json, List<String> problems, List<String> warnings) {
        if (!validRouteId(routeId)) {
            problems.add("the route id '" + shorten(String.valueOf(routeId)) + "' must be <namespace>:<file_name> with a lower-case snake file name (a file in a sub-folder cannot be a route)");
            return new Parsed(null, problems, warnings);
        }
        if (json == null || !json.isJsonObject()) {
            problems.add("the file must be a JSON object");
            return new Parsed(null, problems, warnings);
        }
        JsonObject o = json.getAsJsonObject();
        unknownFields(o, ROUTE_FIELDS, "route", warnings);

        Reader r = new Reader(o, problems);
        String magic = r.snake("magic", "other", LEN_KEY);
        String name = r.requiredText("name", LEN_NAME);
        String desc = r.text("desc", "", LEN_ROUTE_DESC);
        String book = r.requiredResource("book");
        String bookName = r.text("book_name", name.length() + 11 <= LEN_NAME ? name + " Skill Book" : name, LEN_NAME);
        String bookLore = r.text("book_lore", "", LEN_LORE);
        int color = r.color("color");
        String icon = r.resource("icon", "minecraft:book");
        String classId = r.requiredSnake("class_id", LEN_ID);
        int duplicateSp = r.integer("duplicate_sp", 3, 0, MAX_DUPLICATE_SP);
        int lootWeight = r.integer("loot_weight", 1, 0, MAX_LOOT_WEIGHT);

        List<SkillNode> nodes = new ArrayList<>();
        JsonElement ne = o.get("nodes");
        if (ne == null || ne.isJsonNull()) {
            problems.add("nodes: is required");
        } else if (!ne.isJsonArray()) {
            problems.add("nodes: must be an array of nodes");
        } else if (ne.getAsJsonArray().isEmpty()) {
            problems.add("nodes: a route needs at least one node");
        } else if (ne.getAsJsonArray().size() > MAX_NODES) {
            problems.add("nodes: " + ne.getAsJsonArray().size() + " nodes, the limit is " + MAX_NODES);
        } else {
            nodes = readNodes(ne.getAsJsonArray(), problems, warnings);
            if (nodes.isEmpty()) problems.add("nodes: no node could be loaded");
            else if (nodes.stream().noneMatch(SkillNode::isRoot)) problems.add("nodes: no root node (a node without parents) could be loaded, so nothing in the route could ever be bought");
        }

        boolean routeBroken = r.errors > 0 || nodes.isEmpty() || nodes.stream().noneMatch(SkillNode::isRoot);
        if (routeBroken) return new Parsed(null, problems, warnings);
        SkillRoute route = new SkillRoute(routeId, magic, name, desc, book, bookName, bookLore, color, icon, classId, duplicateSp, lootWeight, List.copyOf(nodes));
        return new Parsed(route, problems, warnings);
    }

    // ---------------------------------------------------------------- nodes
    private static List<SkillNode> readNodes(JsonArray arr, List<String> problems, List<String> warnings) {
        Ctx c = new Ctx(problems, warnings);
        for (JsonElement e : arr) {                                                   // every id that appears anywhere, so "comes later" can be told from "does not exist"
            if (e != null && e.isJsonObject() && e.getAsJsonObject().get("id") instanceof JsonPrimitive p && p.isString()) c.allIds.add(p.getAsString());
        }
        for (int i = 0; i < arr.size(); i++) {
            SkillNode n = readNode(arr.get(i), i, c);
            if (n != null) c.accepted.put(n.id(), n);
        }
        // oddities that load but are probably not what the author meant
        Map<String, String> at = new HashMap<>();
        for (SkillNode n : c.accepted.values()) {
            String cell = n.x() + "," + n.y();
            String other = at.put(cell, n.id());
            if (other != null) warnings.add("nodes '" + other + "' and '" + n.id() + "' are both at x=" + n.x() + ", y=" + n.y() + " (they would be drawn on top of each other)");
        }
        return new ArrayList<>(c.accepted.values());
    }

    /** Everything the node reader carries from node to node. */
    private static final class Ctx {
        final List<String> problems, warnings;
        final Map<String, SkillNode> accepted = new LinkedHashMap<>();     // loaded nodes, in file order
        final Set<String> allIds = new HashSet<>();                        // every id that appears in the file
        final Set<String> seen = new HashSet<>();                          // ids met so far (to catch a repeated id)
        final Set<String> rejected = new HashSet<>();                      // ids of nodes skipped because of their own problems
        final Map<String, String> skillOwner = new HashMap<>();            // skill id -> the node that grants it

        Ctx(List<String> problems, List<String> warnings) {
            this.problems = problems;
            this.warnings = warnings;
        }
    }

    private static SkillNode readNode(JsonElement e, int index, Ctx c) {
        String label = "node #" + (index + 1);
        if (e == null || !e.isJsonObject()) {
            c.problems.add(label + ": a node must be a JSON object");
            return null;
        }
        JsonObject o = e.getAsJsonObject();
        JsonElement idEl = o.get("id");
        String id = idEl != null && idEl.isJsonPrimitive() && idEl.getAsJsonPrimitive().isString() ? idEl.getAsString() : null;
        if (id == null || !validSnake(id) || id.length() > LEN_ID) {
            c.problems.add(label + ": \"id\" must be lower-case snake text (a-z, 0-9, _) of at most " + LEN_ID + " characters" + (id == null ? "" : ", got '" + shorten(id) + "'"));
            return null;                                                              // with no usable id nothing can name this node, so nothing can depend on it
        }
        label = "node '" + id + "'";
        if (!c.seen.add(id)) {
            c.problems.add(label + ": the id is already used by an earlier node (this one is skipped)");
            return null;
        }
        unknownFields(o, NODE_FIELDS, label, c.warnings);

        List<String> own = new ArrayList<>();
        Reader r = new Reader(o, own);
        String name = r.text("name", titleCase(id), LEN_NAME);
        if (name.isBlank()) name = titleCase(id);
        String desc = r.text("desc", "", LEN_NODE_DESC);
        int cost = r.integer("cost", 1, 1, MAX_COST);
        int maxLevel = r.integer("max_level", 1, 1, MAX_LEVEL);
        List<Integer> costs = readCosts(o, maxLevel, cost, own, c.warnings, label);
        String skill = r.optionalResource("skill");
        String icon = r.optionalResource("icon");
        List<String> passives = readPassives(o, own);
        Map<String, List<Double>> values = readValues(o, maxLevel, own, c.warnings, label);
        int x = r.integer("x", 0, 0, MAX_X);
        int y = r.integer("y", 0, 0, MAX_Y);
        List<SkillNode.Parent> parents = readParents(o, own);

        for (SkillNode.Parent p : parents) {
            SkillNode pn = c.accepted.get(p.node());
            if (p.node().equals(id)) own.add("parents: a node cannot be its own parent");
            else if (pn != null) {
                if (p.level() > pn.maxLevel()) own.add("parents: '" + p.node() + "' is asked for at level " + p.level() + " but it only goes up to " + pn.maxLevel());
            } else if (c.rejected.contains(p.node())) own.add("parents: needs '" + p.node() + "', which was skipped because of its own problems");
            else if (c.allIds.contains(p.node())) own.add("parents: needs '" + p.node() + "', which comes later in the file (a parent must come first; this also rules out cycles)");
            else own.add("parents: '" + p.node() + "' is not a node of this route");
        }
        if (!skill.isEmpty()) {
            String owner = c.skillOwner.get(skill);
            if (owner != null) own.add("skill: '" + skill + "' is already granted by node '" + owner + "'");
        }

        if (!own.isEmpty()) {
            for (String p : own) c.problems.add(label + ": " + p);
            c.rejected.add(id);
            return null;
        }
        if (!skill.isEmpty()) c.skillOwner.put(skill, id);
        return new SkillNode(id, name, desc, List.copyOf(parents), cost, List.copyOf(costs), maxLevel, skill, List.copyOf(passives), values, x, y, icon);
    }

    private static List<Integer> readCosts(JsonObject o, int maxLevel, int cost, List<String> own, List<String> warnings, String label) {
        List<Integer> out = new ArrayList<>();
        JsonElement e = o.get("costs");
        if (e == null || e.isJsonNull()) return out;
        if (!e.isJsonArray()) {
            own.add("costs: must be an array of whole numbers");
            return out;
        }
        JsonArray a = e.getAsJsonArray();
        if (a.size() > maxLevel) {
            own.add("costs: " + a.size() + " prices for a node with max_level " + maxLevel + " (no more prices than levels)");
            return out;
        }
        for (int i = 0; i < a.size(); i++) {
            Integer v = whole(a.get(i));
            if (v == null || v < 1 || v > MAX_COST) {
                own.add("costs[" + i + "]: must be a whole number from 1 to " + MAX_COST);
                return new ArrayList<>();
            }
            out.add(v);
        }
        if (!out.isEmpty() && out.get(0) != cost && o.has("cost")) warnings.add(label + ": \"cost\" is " + cost + " but costs[0] is " + out.get(0) + " (the list wins)");
        return out;
    }

    private static List<String> readPassives(JsonObject o, List<String> own) {
        List<String> out = new ArrayList<>();
        JsonElement e = o.get("passives");
        if (e == null || e.isJsonNull()) return out;
        if (!e.isJsonArray()) {
            own.add("passives: must be an array of ids");
            return out;
        }
        JsonArray a = e.getAsJsonArray();
        if (a.size() > MAX_PASSIVES) {
            own.add("passives: " + a.size() + " entries, the limit is " + MAX_PASSIVES);
            return out;
        }
        for (int i = 0; i < a.size(); i++) {
            JsonElement p = a.get(i);
            String s = p != null && p.isJsonPrimitive() && p.getAsJsonPrimitive().isString() ? p.getAsString() : null;
            if (s == null || !validSnake(s) || s.length() > LEN_KEY) {
                own.add("passives[" + i + "]: must be lower-case snake text of at most " + LEN_KEY + " characters");
                return new ArrayList<>();
            }
            if (!out.contains(s)) out.add(s);
        }
        return out;
    }

    private static Map<String, List<Double>> readValues(JsonObject o, int maxLevel, List<String> own, List<String> warnings, String label) {
        Map<String, List<Double>> out = new LinkedHashMap<>();
        JsonElement e = o.get("values");
        if (e == null || e.isJsonNull()) return Collections.unmodifiableMap(out);
        if (!e.isJsonObject()) {
            own.add("values: must be an object of named number tables");
            return Collections.unmodifiableMap(out);
        }
        JsonObject vo = e.getAsJsonObject();
        if (vo.size() > MAX_VALUE_KEYS) {
            own.add("values: " + vo.size() + " tables, the limit is " + MAX_VALUE_KEYS);
            return Collections.unmodifiableMap(out);
        }
        for (Map.Entry<String, JsonElement> en : vo.entrySet()) {
            String key = en.getKey();
            if (!validSnake(key) || key.length() > LEN_KEY) {
                own.add("values." + shorten(key) + ": the name must be lower-case snake text of at most " + LEN_KEY + " characters");
                continue;
            }
            JsonElement v = en.getValue();
            List<Double> table = new ArrayList<>();
            if (v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) {                       // a lone number is a one-entry table
                Double d = finite(v);
                if (d == null) own.add("values." + key + ": must be a finite number");
                else table.add(d);
            } else if (v != null && v.isJsonArray()) {
                JsonArray a = v.getAsJsonArray();
                if (a.isEmpty() || a.size() > MAX_LEVEL) own.add("values." + key + ": needs 1 to " + MAX_LEVEL + " numbers, has " + a.size());
                else {
                    for (int i = 0; i < a.size(); i++) {
                        Double d = finite(a.get(i));
                        if (d == null) {
                            own.add("values." + key + "[" + i + "]: must be a finite number (at most " + (long) MAX_VALUE + " in size)");
                            table.clear();
                            break;
                        }
                        table.add(d);
                    }
                    if (a.size() > maxLevel && !table.isEmpty()) warnings.add(label + ": values." + key + " has " + a.size() + " entries but the node only goes to level " + maxLevel + " (the extra entries are never used)");
                }
            } else {
                own.add("values." + key + ": must be a number or an array of numbers");
            }
            if (!table.isEmpty()) out.put(key, Collections.unmodifiableList(table));
        }
        return Collections.unmodifiableMap(out);
    }

    private static List<SkillNode.Parent> readParents(JsonObject o, List<String> own) {
        List<SkillNode.Parent> out = new ArrayList<>();
        JsonElement e = o.get("parents");
        if (e == null || e.isJsonNull()) return out;
        if (!e.isJsonArray()) {
            own.add("parents: must be an array of node ids or {\"node\": id, \"level\": n} objects");
            return out;
        }
        JsonArray a = e.getAsJsonArray();
        if (a.size() > MAX_PARENTS) {
            own.add("parents: " + a.size() + " entries, the limit is " + MAX_PARENTS);
            return out;
        }
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < a.size(); i++) {
            JsonElement p = a.get(i);
            String node = null;
            int level = 1;
            if (p != null && p.isJsonPrimitive() && p.getAsJsonPrimitive().isString()) {
                node = p.getAsString();
            } else if (p != null && p.isJsonObject()) {
                JsonObject po = p.getAsJsonObject();
                JsonElement n = po.get("node");
                if (n != null && n.isJsonPrimitive() && n.getAsJsonPrimitive().isString()) node = n.getAsString();
                if (po.has("level")) {
                    Integer lv = whole(po.get("level"));
                    if (lv == null || lv < 1 || lv > MAX_LEVEL) {
                        own.add("parents[" + i + "]: \"level\" must be a whole number from 1 to " + MAX_LEVEL);
                        continue;
                    }
                    level = lv;
                }
            }
            if (node == null || !validSnake(node) || node.length() > LEN_ID) {
                own.add("parents[" + i + "]: must be a node id (or an object with a \"node\" id)");
                continue;
            }
            if (!seen.add(node)) {
                own.add("parents[" + i + "]: '" + node + "' is listed twice");
                continue;
            }
            out.add(new SkillNode.Parent(node, level));
        }
        return out;
    }

    // ---------------------------------------------------------------- typed reads
    /** Reads fields of one JSON object and files every problem into {@code sink} as {@code field: what is wrong}; {@link #errors} counts them. */
    private static final class Reader {
        final JsonObject o;
        final List<String> sink;
        int errors;

        Reader(JsonObject o, List<String> sink) {
            this.o = o;
            this.sink = sink;
        }

        boolean has(String k) { return o.has(k) && !o.get(k).isJsonNull(); }

        void err(String k, String msg) {
            errors++;
            sink.add(k + ": " + msg);
        }

        /** The text of a field that is present: null (the error is filed) when it is not text or is too long. */
        private String string(String k, int max) {
            JsonElement e = o.get(k);
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
                err(k, "must be text");
                return null;
            }
            String s = e.getAsString();
            if (s.length() > max) {
                err(k, "is " + s.length() + " characters long, the limit is " + max);
                return null;
            }
            return s;
        }

        String text(String k, String dflt, int max) {
            if (!has(k)) return dflt;
            String s = string(k, max);
            return s == null ? dflt : s;
        }

        String requiredText(String k, int max) {
            if (!has(k)) {
                err(k, "is required");
                return "";
            }
            String s = string(k, max);
            if (s == null) return "";
            if (s.isBlank()) {
                err(k, "must not be blank");
                return "";
            }
            return s;
        }

        String snake(String k, String dflt, int max) {
            if (!has(k)) return dflt;
            String s = string(k, max);
            if (s == null) return dflt;
            if (!validSnake(s)) {
                err(k, "must be lower-case snake text (a-z, 0-9, _) starting with a letter, got '" + shorten(s) + "'");
                return dflt;
            }
            return s;
        }

        String requiredSnake(String k, int max) {
            if (!has(k)) {
                err(k, "is required");
                return "";
            }
            String s = string(k, max);
            if (s == null) return "";
            if (!validSnake(s)) {
                err(k, "must be lower-case snake text (a-z, 0-9, _) starting with a letter, got '" + shorten(s) + "'");
                return "";
            }
            return s;
        }

        String requiredResource(String k) {
            if (!has(k)) {
                err(k, "is required");
                return "";
            }
            return resource(k, "");
        }

        /** A resource location with a default; a value that is present but not a lower-case {@code namespace:path} is an error. */
        String resource(String k, String dflt) {
            if (!has(k)) return dflt;
            String s = string(k, LEN_RESOURCE);
            if (s == null) return dflt;
            if (!validResource(s)) {
                err(k, "must be a lower-case resource location like \"namespace:path\", got '" + shorten(s) + "'");
                return dflt;
            }
            return s;
        }

        /** A resource location that may be absent or empty (an empty string means none). */
        String optionalResource(String k) {
            if (!has(k)) return "";
            String s = string(k, LEN_RESOURCE);
            if (s == null || s.isEmpty()) return "";
            if (!validResource(s)) {
                err(k, "must be a lower-case resource location like \"namespace:path\", got '" + shorten(s) + "'");
                return "";
            }
            return s;
        }

        int integer(String k, int dflt, int min, int max) {
            if (!has(k)) return dflt;
            Integer v = whole(o.get(k));
            if (v == null || v < min || v > max) {
                err(k, "must be a whole number from " + min + " to " + max);
                return dflt;
            }
            return v;
        }

        int color(String k) {
            if (!has(k)) {
                err(k, "is required (a colour like \"#E8541C\")");
                return 0;
            }
            JsonElement e = o.get(k);
            int c = e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? parseColor(e.getAsString()) : -1;
            if (c < 0) {
                err(k, "must be a colour like \"#E8541C\" (a # and six hex digits)");
                return 0;
            }
            return c;
        }
    }

    /** A JSON number that is a whole number within int range, else null. */
    private static Integer whole(JsonElement e) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return null;
        double d;
        try {
            d = e.getAsDouble();
        } catch (NumberFormatException bad) {
            return null;
        }
        if (!Double.isFinite(d) || d != Math.rint(d) || Math.abs(d) > 1.0e9) return null;
        return (int) d;
    }

    /** A JSON number that is finite and of sane size, else null. */
    private static Double finite(JsonElement e) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return null;
        double d;
        try {
            d = e.getAsDouble();
        } catch (NumberFormatException bad) {
            return null;
        }
        return Double.isFinite(d) && Math.abs(d) <= MAX_VALUE ? d : null;
    }

    // ---------------------------------------------------------------- helpers
    private static void unknownFields(JsonObject o, Set<String> known, String label, List<String> warnings) {
        for (String k : o.keySet()) {
            if (known.contains(k)) continue;
            String snake = k.replaceAll("([a-z0-9])([A-Z])", "$1_$2").replace('-', '_').toLowerCase(Locale.ROOT);
            warnings.add(label + ": unknown field \"" + shorten(k) + "\" is ignored" + (known.contains(snake) ? " (did you mean \"" + snake + "\"?)" : ""));
        }
    }

    /** A message fragment cut to a readable length. */
    static String shorten(String s) { return s.length() <= 60 ? s : s.substring(0, 57) + "..."; }
}
