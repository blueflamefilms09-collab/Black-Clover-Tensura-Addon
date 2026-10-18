package com.newuniverse.nusmp.skilltree.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: new magics are data, so a server owner or the mod's author must be able to add or retune a skill tree with a datapack and {@code /reload}, with no rebuild. The game's own
 * {@code SimpleJsonResourceReloadListener} already does the part that is easy to get wrong (it lists {@code data/<namespace>/skill_trees/*.json} across every datapack in priority order, so a
 * datapack can replace any file, and turns each into a JSON tree off the main thread), and the tree is small, validated in one place and indexed for lookups, so a custom listener is lighter
 * than a datapack registry (which would add a codec, a dynamic-registry sync and freeze rules for no gain). This class only adds what the report needs: files that exist but are not valid JSON
 * are dropped by the game with a log line, so they are listed here too (by comparing the files found with the files parsed) and show up in {@code /multiverse tree routes}. The pure
 * {@link RouteIndex} builds the immutable snapshot (validation, cross-file rules, size caps) on the main thread in {@link #apply}, and {@link RouteRegistry#publish} swaps it in with one
 * write; if anything unexpected is thrown the previous routes stay in force. Registered through {@code AddReloadListenerEvent} by {@code TreeEvents}; the route id is
 * {@code <namespace>:<file name>} because that is the id the game gives the JSON file.
 */
public final class RouteLoader extends SimpleJsonResourceReloadListener {
    /** The folder under {@code data/<namespace>/}. */
    public static final String DIRECTORY = "skill_trees";
    /**
     * 1.82.0: {@code data/<namespace>/unique_skills/*.json} is an ALIAS folder of {@link #DIRECTORY}: the same files, the same route ids ({@code <namespace>:<file name>}), the same schema. The Unique
     * Skill Lines are authored under that name (the owner's word for them) and a pack author may use either folder. If a route id exists in both, the {@code skill_trees} file wins and the clash is logged.
     */
    public static final String ALIAS_DIRECTORY = "unique_skills";

    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    /** Ids of files that exist but did not parse, found by {@link #prepare} on the loading thread and read by {@link #apply} on the main thread (the reload hands one to the other). */
    private volatile List<String> unreadable = List.of();

    public RouteLoader() { super(GSON, DIRECTORY); }

    @Override
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> parsed = new HashMap<>(super.prepare(manager, profiler));
        scanAlias(manager, parsed);
        unreadable = findUnreadable(manager, parsed.keySet());
        return parsed;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        build(files, unreadable);
    }

    /**
     * Re-reads the routes from the datapacks that are loaded now and puts them in force, without a full {@code /reload} (what {@code /multiverse tree reload} runs). Returns the new snapshot.
     * Server thread.
     */
    public static RouteIndex reloadNow(ResourceManager manager) {
        Map<ResourceLocation, JsonElement> files = new HashMap<>();
        scanDirectory(manager, DIRECTORY, GSON, files);
        scanAlias(manager, files);
        return build(files, findUnreadable(manager, files.keySet()));
    }

    /** Adds the files of the alias folder to {@code files}; an id that is already there (from {@code skill_trees}) is kept and the clash is logged. */
    private static void scanAlias(ResourceManager manager, Map<ResourceLocation, JsonElement> files) {
        try {
            Map<ResourceLocation, JsonElement> alias = new HashMap<>();
            scanDirectory(manager, ALIAS_DIRECTORY, GSON, alias);
            for (Map.Entry<ResourceLocation, JsonElement> e : alias.entrySet()) {
                if (files.putIfAbsent(e.getKey(), e.getValue()) != null) LOG.warn("[nusmp] skill tree {} exists in both {} and {}; the {} file is used", e.getKey(), DIRECTORY, ALIAS_DIRECTORY, DIRECTORY);
            }
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] skill trees: could not read the {} folder", ALIAS_DIRECTORY, ex);
        }
    }

    private static RouteIndex build(Map<ResourceLocation, JsonElement> files, Collection<String> unreadable) {
        try {
            Map<String, JsonElement> byId = new HashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) byId.put(e.getKey().toString(), e.getValue());
            RouteIndex index = RouteIndex.build(byId, unreadable, RouteIndex.Caps.REGISTRY);
            RouteRegistry.publish(index);
            LOG.info("[nusmp] skill trees: {} route(s) loaded from {} file(s), {} file(s) with problems, {} bytes to sync", index.all().size(), index.report().size(), index.problemFiles(), index.bytes());
            for (RouteIndex.Entry e : index.report()) {
                for (String p : e.problems()) LOG.warn("[nusmp] skill tree {}: {}", e.id(), p);
                for (String w : e.warnings()) LOG.info("[nusmp] skill tree {} (warning): {}", e.id(), w);
            }
            return index;
        } catch (RuntimeException e) {
            LOG.error("[nusmp] skill trees: the reload failed, the previous routes stay in force", e);
            return RouteRegistry.snapshot();
        }
    }

    /** Files under the folder that the game listed but did not parse, as {@code namespace:name} ids, in id order. */
    private static List<String> findUnreadable(ResourceManager manager, Set<ResourceLocation> parsed) {
        try {
            List<String> out = new ArrayList<>();
            for (String dir : new String[]{DIRECTORY, ALIAS_DIRECTORY}) {
                Map<ResourceLocation, Resource> listed = manager.listResources(dir, rl -> rl.getPath().endsWith(".json"));
                for (ResourceLocation rl : listed.keySet()) {
                    String path = rl.getPath();
                    if (path.length() <= dir.length() + 6) continue;
                    String name = path.substring(dir.length() + 1, path.length() - ".json".length());
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), name);
                    if (!parsed.contains(id) && !out.contains(id.toString())) out.add(id.toString());
                }
            }
            Collections.sort(out);
            return Collections.unmodifiableList(out);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] skill trees: could not list the data folder to look for unreadable files", e);
            return List.of();
        }
    }
}
