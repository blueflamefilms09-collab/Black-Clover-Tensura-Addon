package com.newuniverse.nusmp.visual.grip;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 1.111.0 (docs/weapon_grip.md): assets/nusmp/weapon_grips.json, the per-weapon half of the grip table. Pure (Gson only).
 * <pre>
 * { "version": 1,
 *   "fractions": { "elsdocia": 0.12, ... },     per-weapon grip fraction (share of the length from the butt) that beats the mesh's own grip line and the handle finder
 *   "classOf":   { "djinn_weapon_baal": "spear", ... } the grip class of a weapon weapon_classes.json does not list
 *   "points":    { "elsdocia": [x, y, z], ... }   the grip point itself in mesh space of the UPGRADED meshes (weapon_meshes_vu; their effect groups would blur the profile)
 * }
 * </pre>
 * Every weapon mesh must have a grip class: {@link #hasGripClass} is true when weapon_classes.json lists the id (or its base id) or {@code classOf} does; the data test
 * {@code GripTableTest} fails for a mesh without one, so a new weapon cannot ship with only a keyword guess for its grip.
 */
public final class GripTable {
    /** What the table needs of the weapon class table (client/relics/item/WeaponClasses implements it): is the id listed by review, and its class (a keyword guess when unlisted). */
    public interface Classes {
        boolean isListed(String id);

        String className(String id);
    }

    private static final Pattern STATE_SUFFIX = Pattern.compile("(_(pull[123]|charged|spent|genesis|5leaf|liquid|off|blue|left|curse)|(?<=lantern)_red)$");

    /** The id of the rest mesh a state mesh belongs to (the same rule as WeaponClasses.baseId, which a test keeps equal). */
    public static String baseId(String id) {
        return STATE_SUFFIX.matcher(id).replaceFirst("");
    }

    private final Map<String, Float> fractions = new HashMap<>();
    private final Map<String, String> classOf = new HashMap<>();
    private final Map<String, float[]> points = new HashMap<>();

    public static final GripTable EMPTY = new GripTable();

    public static GripTable parse(String json) {
        GripTable t = new GripTable();
        if (json == null || json.isBlank()) return t;
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root.has("fractions")) for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("fractions").entrySet()) t.fractions.put(e.getKey(), e.getValue().getAsFloat());
        if (root.has("classOf")) for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("classOf").entrySet()) t.classOf.put(e.getKey(), e.getValue().getAsString());
        if (root.has("points")) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("points").entrySet()) {
                com.google.gson.JsonArray a = e.getValue().getAsJsonArray();
                t.points.put(e.getKey(), new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()});
            }
        }
        return t;
    }

    /** The stated grip point (mesh space) of an id (then its base id), null when there is none. */
    public float[] point(String id) {
        float[] p = points.get(id);
        return p != null ? p : points.get(baseId(id));
    }

    public Map<String, float[]> points() { return points; }

    /** The per-weapon fraction of an id (then its base id), NaN when there is none. */
    public float fraction(String id) {
        Float f = fractions.get(id);
        if (f == null) f = fractions.get(baseId(id));
        return f == null ? Float.NaN : f;
    }

    /** True when the id has a reviewed grip class: weapon_classes.json lists it (or its base id), or this table's {@code classOf} does. */
    public boolean hasGripClass(String id, Classes classes) {
        return classes.isListed(id) || classOf.containsKey(id) || classOf.containsKey(baseId(id));
    }

    /** The grip class of an id: weapon_classes.json first, then {@code classOf}, then the keyword guess of the class table. */
    public String gripClass(String id, Classes classes) {
        if (classes.isListed(id)) return classes.className(id);
        String c = classOf.get(id);
        if (c == null) c = classOf.get(baseId(id));
        return c != null ? c : classes.className(id);
    }

    public Map<String, Float> fractions() { return fractions; }

    public Map<String, String> classOf() { return classOf; }
}
