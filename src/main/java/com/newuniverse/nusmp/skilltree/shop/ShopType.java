package com.newuniverse.nusmp.skilltree.shop;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Why: the owner wants every future NPC shop to sell only ONE kind of stuff (a smith sells tools and metal, a cook sells food, an alchemist sells potions), and the Black Market witch to be the
 * general store. The kind of stuff is a SHOP TYPE: pure data, a name plus the item tags, mod namespaces and item ids that belong to it and the item ids that never do. A shop file names its type
 * ({@code "type": "food"}) and the validator refuses every line whose item is outside it, so a typo in a datapack cannot put a netherite block on the baker's shelf. This class is that rule with
 * no game type in it (items and tags are plain {@code namespace:path} strings), so it is unit tested with fake item lists; the game side only supplies the tags an item carries.
 * <p>
 * A type allows an item when the item is not banned AND one of these holds: the type is {@code all} (the general store), the item id is in its {@code ids}, the item's namespace is in its
 * {@code namespaces}, or the item carries an allowed tag. A tag {@code c:ores} also allows its children {@code c:ores/iron}, {@code c:ores/gold} ... (the convention tags nest with a slash). A TAG line
 * of a shop file is allowed when the tag itself is allowed (every member of an allowed tag is an item of that kind by definition; only the banned ids are then filtered out of the pool).
 */
public record ShopType(String id, boolean all, Set<String> tags, Set<String> namespaces, Set<String> ids, Set<String> banned) {

    /** The verdict on one item or tag against a type. */
    public enum Check {
        /** The type sells it. */
        ALLOWED,
        /** The type does not sell it: the line is rejected. */
        REJECTED,
        /** A single item whose tags are not known yet (the tags of a world are bound after the data files load): the pool filter decides when the tags are known. */
        UNKNOWN
    }

    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]{0,31}");
    private static final Pattern RES = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    /** The most entries any one list of a type may hold. */
    public static final int MAX_LIST = 256;
    /** The most types a file may declare. */
    public static final int MAX_TYPES = 32;
    /** The type a shop file gets when it names none: the general store. */
    public static final String DEFAULT = "general";

    public ShopType {
        tags = Set.copyOf(tags);
        namespaces = Set.copyOf(namespaces);
        ids = Set.copyOf(ids);
        banned = Set.copyOf(banned);
    }

    /** True when {@code tag} (no hash) is one of this type's tags or a child of one ({@code c:ores/iron} under {@code c:ores}). */
    public boolean tagAllowed(String tag) {
        if (tag == null) return false;
        if (tags.contains(tag)) return true;
        for (String t : tags) if (tag.startsWith(t + "/")) return true;
        return false;
    }

    /** True when the item id is on the type's ban list. */
    public boolean isBanned(String itemId) {
        return itemId != null && banned.contains(itemId);
    }

    /**
     * Does this type sell this item? {@code itemTags} are the tags the item carries (ids with no hash); an item of an {@code all} type or of an allowed id or namespace needs none.
     */
    public boolean allows(String itemId, Collection<String> itemTags) {
        if (itemId == null || banned.contains(itemId)) return false;
        if (all || ids.contains(itemId)) return true;
        int colon = itemId.indexOf(':');
        if (colon > 0 && namespaces.contains(itemId.substring(0, colon))) return true;
        if (itemTags != null) for (String t : itemTags) if (tagAllowed(t)) return true;
        return false;
    }

    /**
     * The check a shop file's line gets when it is read, before any world exists. A tag line is allowed when the tag is allowed; a single item is decided at once when it is banned or
     * allowed by id or namespace or by the type being {@code all}, and otherwise by {@code itemTags} when the caller knows them, else UNKNOWN.
     *
     * @param item     an item id or a tag with a leading hash
     * @param itemTags the tags of the item when known (null when not)
     */
    public Check check(String item, Collection<String> itemTags) {
        if (item == null || item.isEmpty()) return Check.REJECTED;
        if (item.startsWith("#")) return all || tagAllowed(item.substring(1)) ? Check.ALLOWED : Check.REJECTED;
        if (banned.contains(item)) return Check.REJECTED;
        if (allows(item, null)) return Check.ALLOWED;
        if (itemTags == null) return Check.UNKNOWN;
        return allows(item, itemTags) ? Check.ALLOWED : Check.REJECTED;
    }

    // ================================================================ the types file
    /** Every type of a {@code types.json}: the valid ones by id in file order, and every problem met on the way. */
    public record Parsed(Map<String, ShopType> types, List<String> problems) {
        /** The type with this id, or null. */
        public ShopType get(String id) {
            return id == null ? null : types.get(id);
        }
    }

    /** The general store alone: what the game uses when {@code types.json} is missing or broken, so the witch always has a type. */
    public static ShopType fallbackGeneral() {
        return new ShopType(DEFAULT, true, Set.of(), Set.of(), Set.of(), Set.of());
    }

    /** Reads the text of a {@code types.json}. Never throws. */
    public static Parsed parse(String json) {
        List<String> problems = new ArrayList<>();
        JsonElement root;
        try {
            root = JsonParser.parseString(json == null ? "" : json);
        } catch (JsonParseException | IllegalStateException e) {
            problems.add("not valid JSON: " + e.getMessage());
            return withGeneral(new LinkedHashMap<>(), problems);
        }
        if (!root.isJsonObject() || !root.getAsJsonObject().has("types") || !root.getAsJsonObject().get("types").isJsonObject()) {
            problems.add("the file must be an object with a \"types\" object (type id to its rules)");
            return withGeneral(new LinkedHashMap<>(), problems);
        }
        Map<String, ShopType> out = new LinkedHashMap<>();
        int n = 0;
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject().getAsJsonObject("types").entrySet()) {
            if (e.getKey().startsWith("_")) continue;                                   // underscore keys are comments
            if (++n > MAX_TYPES) {
                problems.add("types: only the first " + MAX_TYPES + " types are read, the rest ignored");
                break;
            }
            ShopType t = type(e.getKey(), e.getValue(), problems);
            if (t != null) out.put(t.id(), t);
        }
        return withGeneral(out, problems);
    }

    /** The general store is always there: a file that forgets it (or breaks it) does not leave the witch without a type. */
    private static Parsed withGeneral(Map<String, ShopType> out, List<String> problems) {
        if (!out.containsKey(DEFAULT)) {
            problems.add("no valid \"" + DEFAULT + "\" type: the built-in general store (everything but the banned ids) is used for it");
            out.put(DEFAULT, fallbackGeneral());
        }
        return new Parsed(Collections.unmodifiableMap(out), problems);
    }

    private static final Set<String> KEYS = Set.of("all", "tags", "namespaces", "ids", "banned_ids");

    private static ShopType type(String id, JsonElement el, List<String> problems) {
        String where = "type '" + id + "'";
        if (!ID.matcher(id).matches()) {
            problems.add(where + ": the type id must be lower-case snake_case, skipped");
            return null;
        }
        if (!el.isJsonObject()) {
            problems.add(where + ": a type must be an object, skipped");
            return null;
        }
        JsonObject o = el.getAsJsonObject();
        for (String k : o.keySet()) if (!k.startsWith("_") && !KEYS.contains(k)) problems.add(where + ": unknown key '" + k + "' (known: all, banned_ids, ids, namespaces, tags)");
        boolean all = false;
        if (o.has("all")) {
            JsonElement a = o.get("all");
            if (a.isJsonPrimitive() && a.getAsJsonPrimitive().isBoolean()) all = a.getAsBoolean();
            else problems.add(where + ": \"all\" must be true or false, ignored");
        }
        Set<String> tags = list(o, "tags", RES, where, problems);
        Set<String> namespaces = list(o, "namespaces", NAMESPACE, where, problems);
        Set<String> ids = list(o, "ids", RES, where, problems);
        Set<String> banned = list(o, "banned_ids", RES, where, problems);
        if (!all && tags.isEmpty() && namespaces.isEmpty() && ids.isEmpty()) {
            problems.add(where + ": sells nothing (no \"all\", tags, namespaces or ids), skipped");
            return null;
        }
        return new ShopType(id, all, tags, namespaces, ids, banned);
    }

    private static Set<String> list(JsonObject o, String key, Pattern pattern, String where, List<String> problems) {
        Set<String> out = new LinkedHashSet<>();
        if (!o.has(key)) return out;
        JsonElement e = o.get(key);
        if (!e.isJsonArray()) {
            problems.add(where + ": \"" + key + "\" must be an array of strings, ignored");
            return out;
        }
        JsonArray a = e.getAsJsonArray();
        for (int i = 0; i < a.size(); i++) {
            if (i >= MAX_LIST) {
                problems.add(where + ": \"" + key + "\" is cut at " + MAX_LIST + " entries");
                break;
            }
            JsonElement v = a.get(i);
            String s = v.isJsonPrimitive() && v.getAsJsonPrimitive().isString() ? v.getAsString() : null;
            if (s == null || !pattern.matcher(s).matches()) problems.add(where + ": \"" + key + "\" entry " + v + " is not a valid " + (pattern == NAMESPACE ? "namespace" : "id") + ", ignored");
            else out.add(s);
        }
        return out;
    }
}
