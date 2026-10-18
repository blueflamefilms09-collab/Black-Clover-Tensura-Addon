package com.newuniverse.nusmp.elite.races.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Why: the second moment the race files are read (the first is the class-path read at registration, see {@link RaceTemplates}): when a world loads or {@code /reload} runs, every
 * {@code data/<namespace>/elite/races/*.json} is read through the resource manager, so a datapack can replace a shipped race file (the later pack wins, as for any datapack file)
 * or tune a state. The result is swapped in by {@link RaceTemplates#replace}; a bad file is logged and the bundled version of that race stays. Only files of the {@code nusmp}
 * namespace count, and {@code index.json} is not a race. Server side: added through {@code AddReloadListenerEvent}.
 */
public final class RaceTemplateLoader implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String DIR = "elite/races";

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, JsonObject> found = new TreeMap<>();
        for (Map.Entry<ResourceLocation, Resource> e : manager.listResources(DIR, rl -> rl.getPath().endsWith(".json")).entrySet()) {
            if (!"nusmp".equals(e.getKey().getNamespace())) continue;
            String path = e.getKey().getPath();
            String id = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
            if (id.equals("index")) continue;
            try (Reader reader = e.getValue().openAsReader()) {
                found.put(id, JsonParser.parseReader(reader).getAsJsonObject());
            } catch (Exception ex) {
                LOG.warn("[nusmp] elite races: skipping {}: {}", e.getKey(), ex.toString());
            }
        }
        // keep the order of the bundled index first (the lines read top to bottom), then any extra race a pack adds
        Map<String, JsonObject> ordered = new LinkedHashMap<>();
        for (String known : List.copyOf(RaceTemplates.all().keySet())) if (found.containsKey(known)) ordered.put(known, found.get(known));
        ordered.putAll(found);
        List<String> bad = RaceTemplates.replace(ordered);
        for (String b : bad) LOG.warn("[nusmp] elite races: {} (the bundled version of that race stays)", b);
        LOG.info("[nusmp] elite races: {} race file(s) read from the data packs", ordered.size() - bad.size());
    }
}
