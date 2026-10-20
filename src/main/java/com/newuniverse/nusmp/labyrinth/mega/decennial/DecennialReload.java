package com.newuniverse.nusmp.labyrinth.mega.decennial;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the 200 bosses are data ({@code data/<ns>/mega/decennial/bosses/<side>_<nnn>.json}), so a datapack can replace one boss or add a level and {@code /reload} applies it. This is the reload
 * listener that fills {@link DecennialRoster}, the pattern of the D&D profiles ({@code DndProfiles}): files from every datapack in priority order, parsed by the strict reader, a file with a problem
 * reported and skipped (that level falls back to the Guardian's own D&D profile of its floor) and the rest still loaded, the new roster built whole and published with one write. If NOTHING parses
 * (a broken datapack that replaced every file) the old roster is kept, so a stripped pack can never leave the dungeon without bosses by accident.
 */
public final class DecennialReload extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    public DecennialReload() { super(GSON, DecennialRoster.DIRECTORY); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, JsonObject> objects = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            if (e.getValue().isJsonObject()) objects.put(e.getKey().getPath(), e.getValue().getAsJsonObject());
            else LOG.warn("[nusmp] decennial boss file {} skipped: it is not a JSON object", e.getKey());
        }
        DecennialRoster.Loaded l = DecennialRoster.parseObjects(objects);
        for (String problem : l.problems()) LOG.warn("[nusmp] decennial boss file skipped: {}", problem);
        if (l.profiles().isEmpty() && DecennialRoster.size() > 0) {
            LOG.error("[nusmp] No decennial boss file could be read: the previous roster ({} bosses) is kept.", DecennialRoster.size());
            return;
        }
        DecennialRoster.replace(l.profiles());
        LOG.info("[nusmp] decennial bosses: {} loaded from {} file(s)", l.profiles().size(), files.size());
    }
}
