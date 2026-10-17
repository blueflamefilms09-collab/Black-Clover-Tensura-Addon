package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Why: the factions are data ({@code data/<namespace>/mega/factions/<id>.json}), so a datapack can retune a quarrel, add a twelfth faction or
 * silence a rivalry without a mod update, and a data reload ({@code /reload}, a world load) must rebuild the graph. A plain
 * {@code ResourceManagerReloadListener} (the pattern of the avatar races and chatter loaders) reads every file, hands the parsed tree to the pure
 * {@link FactionParser}, logs each problem once with the file it came from, builds the {@link FactionGraph} and installs it in one step. One bad
 * file never stops the others: it is skipped with a log line. The file name is the faction id; when two packs ship the same name the pack the
 * resource manager ranks highest wins (it lists one resource per name). The Gson tree is converted to plain maps and lists here, and only here,
 * so the parser stays library-free and testable.
 */
public final class FactionReload implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    static final String DIR = "mega/factions";

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, FactionDef> loaded = new TreeMap<>();
        try {
            for (Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource> e : manager.listResources(DIR, rl -> rl.getPath().endsWith(".json")).entrySet()) {
                String path = e.getKey().getPath();
                String id = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
                try (Reader reader = e.getValue().openAsReader()) {
                    JsonElement root = JsonParser.parseReader(reader);
                    Object tree = root.isJsonObject() ? toTree(root.getAsJsonObject()) : null;
                    @SuppressWarnings("unchecked") FactionParser.Result r = FactionParser.parse(id, (Map<String, Object>) tree);
                    for (String p : r.problems()) LOG.warn("[nusmp] Mega faction file {}: {}", e.getKey(), p);
                    if (r.def() != null) loaded.put(r.def().id(), r.def());
                } catch (Exception ex) {
                    LOG.warn("[nusmp] Mega faction file {} skipped: {}", e.getKey(), ex.toString());
                }
            }
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] Mega factions could not scan their data folder", ex);
        }
        FactionGraph graph = FactionGraph.of(new ArrayList<>(loaded.values()));
        for (String p : graph.problems()) LOG.warn("[nusmp] Mega factions: {}", p);
        FactionRegistry.install(graph);
        LOG.info("[nusmp] Loaded {} Mega-Dungeon factions: {}", graph.size(), graph.ids());
    }

    /** A Gson element as plain Java: objects to insertion-ordered maps, arrays to lists, numbers to Double, booleans, strings, null. */
    static Object toTree(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : o.entrySet()) m.put(e.getKey(), toTree(e.getValue()));
            return m;
        }
        if (el.isJsonArray()) {
            JsonArray a = el.getAsJsonArray();
            List<Object> l = new ArrayList<>();
            for (JsonElement x : a) l.add(toTree(x));
            return l;
        }
        JsonPrimitive p = el.getAsJsonPrimitive();
        if (p.isBoolean()) return p.getAsBoolean();
        if (p.isNumber()) return p.getAsDouble();
        return p.getAsString();
    }
}
