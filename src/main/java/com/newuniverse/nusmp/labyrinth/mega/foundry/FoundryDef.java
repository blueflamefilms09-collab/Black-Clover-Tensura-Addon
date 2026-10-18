package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Why: one foundry mob as {@code data/nusmp/mega/foundry/<id>.json} describes it (written by tools/foundry/gen_foundry.py from the roster, read by {@link FoundryData} on data
 * reload). It holds the facts the SERVER needs: which road and tier it belongs to, its faction (one of the eleven of the Mega-Dungeon contract, which decides who it fights in
 * a turf war and what it leaves), its archetype and its abilities (ids of the library), its size (a multiplier on the model's height) and the source it is inspired by (kept for
 * the docs and the admin command). What the CLIENT needs (the animation class, the glow colour) is in {@code assets/nusmp/foundry/index.json}, because a client never sees data
 * packs. Pure: the parser is strict and reports every problem as a line of text, like the encounter parser.
 *
 * @param height the model's height in blocks (the art kit measured it); with {@code size} it gives the SCALE attribute
 */
public record FoundryDef(String id, String name, Polarity road, int tier, String faction, String archetype, List<String> abilities, double size, double height, String source, int glow) {

    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_]{1,63}");
    /** The eleven factions of the Mega-Dungeon contract (the entity-type tags {@code nusmp:mega/<id>}). */
    public static final List<String> FACTIONS = List.of("undead", "goblin", "ogre", "beast", "slime", "construct", "cultist", "demon", "elemental", "aquatic", "insect");

    public FoundryDef {
        abilities = List.copyOf(abilities);
    }

    /** The result of a parse: the definition (null when unusable) and the problems. */
    public record Parsed(FoundryDef def, List<String> problems) {}

    /**
     * Parses one file. Problems that make the mob meaningless (no id, a road that is not holy or abyssal, a tier outside 1..5, an unknown faction, no abilities) drop it; an ability
     * the library does not have is dropped from the list and reported; at least one known ability must stay. {@code library} may be null (then abilities are not checked).
     */
    public static Parsed parse(String where, String json, FoundryLibrary library) {
        List<String> problems = new ArrayList<>();
        JsonObject o;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) return new Parsed(null, List.of(where + ": not a JSON object"));
            o = root.getAsJsonObject();
        } catch (RuntimeException ex) {
            return new Parsed(null, List.of(where + ": not valid JSON: " + ex.getMessage()));
        }
        String id = FoundryLibrary.str(o, "id", "");
        if (!ID.matcher(id).matches()) return new Parsed(null, List.of(where + ": bad id '" + id + "'"));
        Polarity road = Polarity.fromId(FoundryLibrary.str(o, "road", ""));
        if (road == null) return new Parsed(null, List.of(where + ": road must be holy or abyssal"));
        int tier = (int) FoundryLibrary.num(o, "tier", 0);
        if (tier < 1 || tier > FoundryScaling.TIERS) return new Parsed(null, List.of(where + ": tier must be 1 to 5"));
        String faction = FoundryLibrary.str(o, "faction", "");
        if (!FACTIONS.contains(faction)) return new Parsed(null, List.of(where + ": unknown faction '" + faction + "'"));
        String archetype = FoundryLibrary.str(o, "archetype", "");
        if (library != null && library.archetype(archetype) == null) problems.add(where + ": unknown archetype '" + archetype + "' (the neutral one is used)");
        List<String> abilities = new ArrayList<>();
        if (o.has("abilities") && o.get("abilities").isJsonArray()) {
            JsonArray arr = o.getAsJsonArray("abilities");
            for (JsonElement e : arr) {
                String a = e.isJsonPrimitive() ? e.getAsString() : "";
                if (library != null && library.ability(a) == null) problems.add(where + ": unknown ability '" + a + "' (dropped)");
                else if (!abilities.contains(a)) abilities.add(a);
            }
        }
        if (abilities.isEmpty()) return new Parsed(null, List.of(where + ": no usable ability"));
        String name = FoundryLibrary.str(o, "name", id);
        double size = FoundryLibrary.num(o, "size", 1.0);
        if (!(size >= 0.6 && size <= 2.0)) {
            problems.add(where + ": size " + size + " outside 0.6..2.0 (clamped)");
            size = Math.max(0.6, Math.min(2.0, Double.isNaN(size) ? 1.0 : size));
        }
        double height = FoundryLibrary.num(o, "height", FoundryScaling.TYPE_HEIGHT);
        if (!(height >= 0.8 && height <= 4.5)) {
            problems.add(where + ": height " + height + " outside 0.8..4.5 (clamped)");
            height = Math.max(0.8, Math.min(4.5, Double.isNaN(height) ? FoundryScaling.TYPE_HEIGHT : height));
        }
        String glow = FoundryLibrary.str(o, "glow", "#ffffff");
        int rgb = 0xFFFFFF;
        try {
            rgb = Integer.parseInt(glow.startsWith("#") ? glow.substring(1) : glow, 16) & 0xFFFFFF;
        } catch (NumberFormatException ex) {
            problems.add(where + ": glow '" + glow + "' is not a colour");
        }
        return new Parsed(new FoundryDef(id, name, road, tier, faction, archetype, abilities, size, height, FoundryLibrary.str(o, "source", ""), rgb), problems);
    }

    /** The registry-style key a spawned mob is requested by: {@code nusmp:foundry_mob#<id>}. */
    public static final String ENTITY_ID = "nusmp:foundry_mob";

    public String entityKey() { return ENTITY_ID + "#" + id; }

    /** The registry id of the entity type to create for a key: {@code nusmp:foundry_mob} for a foundry key, the text itself for anything else. */
    public static String baseId(String key) { return idOfKey(key) == null ? key : ENTITY_ID; }

    /** The id of a key made by {@link #entityKey()}, or null when the text is not one. */
    public static String idOfKey(String key) {
        if (key == null || !key.startsWith(ENTITY_ID + "#")) return null;
        String id = key.substring(ENTITY_ID.length() + 1);
        return ID.matcher(id).matches() ? id : null;
    }
}
