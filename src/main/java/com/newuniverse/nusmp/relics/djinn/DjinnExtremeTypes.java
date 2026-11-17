package com.newuniverse.nusmp.relics.djinn;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Why: each Djinn's Extreme Magic hits with the damage type of its own element or concept (owner request, docs/damage_scale_down.md): thunder is lightning damage, fire is fire damage, ice is freezing
 * damage and so on, so the base game's own resistances and immunities (Fire Resistance, fire-immune mobs, powder-snow immune mobs) answer to them. Pure data: assets/anime_multiverse/djinn_extreme_types.json,
 * read once. The game side only asks {@link #typeId}; an unknown Djinn or concept gives null (the caller keeps its old type).
 */
public final class DjinnExtremeTypes {
    private DjinnExtremeTypes() {}

    private static final Map<String, String> CONCEPT_TYPE = new HashMap<>(), DJINN_CONCEPT = new HashMap<>();
    private static volatile boolean loaded;

    private static synchronized void load() {
        if (loaded) return;
        try (InputStream in = DjinnExtremeTypes.class.getResourceAsStream("/assets/anime_multiverse/djinn_extreme_types.json")) {
            if (in != null) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("concepts").entrySet()) CONCEPT_TYPE.put(e.getKey(), e.getValue().getAsString());
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("djinn").entrySet()) DJINN_CONCEPT.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue().getAsString());
            }
        } catch (Exception ignored) {
            // an unreadable table leaves the old damage types in place
        }
        loaded = true;
    }

    /** The concept ("fire", "lightning", "ice", "water", "wind", "life", "light", "dark", "sound", "force") of a Djinn's Extreme Magic, or null. */
    public static String concept(String djinnId) {
        load();
        if (djinnId == null) return null;
        String key = djinnId.toLowerCase(Locale.ROOT);
        if (key.equals("belial") && BelialCanon.on()) key = "belial_canon";                    // the canon Belial's Extreme Magic is a sever, not a fire
        return DJINN_CONCEPT.get(key);
    }

    /** The damage type id ("minecraft:lightning_bolt") of a Djinn's Extreme Magic, or null if the table does not know it. */
    public static String typeId(String djinnId) {
        String c = concept(djinnId);
        return c == null ? null : CONCEPT_TYPE.get(c);
    }

    /** The concept table (a copy), for tests and docs. */
    public static Map<String, String> concepts() {
        load();
        return new HashMap<>(CONCEPT_TYPE);
    }

    public static Map<String, String> djinnConcepts() {
        load();
        return new HashMap<>(DJINN_CONCEPT);
    }
}
