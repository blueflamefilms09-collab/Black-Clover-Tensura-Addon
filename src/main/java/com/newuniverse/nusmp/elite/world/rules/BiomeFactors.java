package com.newuniverse.nusmp.elite.world.rules;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the biome term of the density (0..1 factor, see {@link MagiculeModel#biomeTerm}) is data: {@code data/nusmp/elite/world/biome_magicule.json} lists substrings of biome ids and the factor each
 * gives; the FIRST entry whose text is contained in the id wins, so a more specific text goes first, and the file's {@code default} answers for the rest. Substrings (not exact ids) so that
 * biomes of other mods and Tensura's own biomes get a sensible answer without a list of hundreds of ids.
 * File: {@code {"default":0.3,"entries":[{"match":"deep_dark","factor":1.0}, ...]}}.
 */
public final class BiomeFactors {
    public record Entry(String match, double factor) {}

    private final List<Entry> entries = new ArrayList<>();
    private double fallback = 0.3;

    public double factorOf(String biomeId) {
        if (biomeId == null) return fallback;
        String id = biomeId.toLowerCase(Locale.ROOT);
        for (Entry e : entries) if (id.contains(e.match())) return MagiculeModel.clamp01(e.factor());
        return MagiculeModel.clamp01(fallback);
    }

    public int size() { return entries.size(); }

    /** Adds the entries of one file after the current ones (so the mod's file, loaded first, keeps priority for equal matches) and takes its default. */
    public void add(JsonObject root) {
        if (root == null) return;
        if (root.has("default") && root.get("default").isJsonPrimitive()) fallback = root.get("default").getAsDouble();
        if (!root.has("entries") || !root.get("entries").isJsonArray()) return;
        for (JsonElement el : root.getAsJsonArray("entries")) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            if (!o.has("match") || !o.has("factor")) continue;
            entries.add(new Entry(o.get("match").getAsString().toLowerCase(Locale.ROOT), o.get("factor").getAsDouble()));
        }
    }
}
