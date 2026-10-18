package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the registry of the boss profiles, filled by a reload listener on {@code data/<ns>/mega/dnd_bosses} (the same pattern as the skill-tree routes: the game lists the files across every
 * datapack in priority order, so a datapack can replace one boss or add one, and {@code /reload} applies it). The new map is built whole and swapped in with one volatile write, so a Guardian that is
 * choosing its profile in the middle of a reload sees the old set or the new set, never half of one; a file with a problem is logged and skipped (that boss falls back to the next best profile)
 * and the rest still load. The profile of a floor is looked up by its id {@code <side>_t<tier><a|b>} ({@code holy_t3a}); a floor with no file gets the built-in default, so a stripped datapack can never
 * leave a boss without rules.
 */
public final class DndProfiles extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "mega/dnd_bosses";
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<String, DndProfile> profiles = Map.of();

    public DndProfiles() { super(GSON, DIRECTORY); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, DndProfile> next = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            String id = e.getKey().getPath();
            try {
                DndProfile p = DndProfile.parse(id, e.getValue().getAsJsonObject());
                next.put(id, p);
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] dnd boss profile {} skipped: {}", e.getKey(), ex.getMessage());
            }
        }
        profiles = Map.copyOf(next);
        LOG.info("[nusmp] dnd boss profiles: {} loaded from {} file(s)", next.size(), files.size());
    }

    /** The id of the profile a floor gets: {@code holy_t3b}. */
    public static String idForFloor(int floor) { return DndRules.idForFloor(floor); }

    /** The profile of a floor, or the built-in default. */
    public static DndProfile forFloor(int floor) { return profiles.getOrDefault(idForFloor(floor), DndProfile.DEFAULT); }

    public static DndProfile byId(String id) { return profiles.get(id); }

    public static int count() { return profiles.size(); }

    /** Test seam: replaces the loaded profiles. */
    static void set(Map<String, DndProfile> map) { profiles = Map.copyOf(map); }
}
