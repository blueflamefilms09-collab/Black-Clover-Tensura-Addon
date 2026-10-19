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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    /** The famous bosses (D&D's well-known villains) that fit a floor: right side (or "any") and the floor's tier inside their range, in id order. */
    public static List<DndProfile> famousFor(int floor) {
        int tier = DndRules.tierOf(floor);
        String side = DndRules.side(floor);
        List<DndProfile> out = new ArrayList<>();
        for (DndProfile p : profiles.values()) {
            if (p.famous() && (p.side().equals("any") || p.side().equals(side)) && tier >= p.tierMin() && tier <= p.tierMax()) out.add(p);
        }
        out.sort(java.util.Comparator.comparing(DndProfile::id));
        return out;
    }

    /**
     * The profile of the boss of a sector on a floor: with the server's chance ({@code [guard] famousPercent}) one of the famous bosses that fit the floor, else the ordinary profile. The pick is
     * a function of the floor and the sector id alone, so the same boss stands there after every reload. A boss with no sector (a test or admin Guardian) is always ordinary.
     */
    public static DndProfile forBoss(int floor, int sectorId) {
        List<DndProfile> famous = famousFor(floor);
        int pick = DndRules.pickFamous(floor, sectorId, GuardConfig.get(GuardConfig.FAMOUS_PERCENT), famous.size());
        return pick < 0 ? forFloor(floor) : famous.get(pick);
    }

    public static int famousCount() {
        int n = 0;
        for (DndProfile p : profiles.values()) if (p.famous()) n++;
        return n;
    }

    public static int count() { return profiles.size(); }

    /** 1.94.0: every loaded profile, in id order (the look checks walk them). */
    public static List<DndProfile> all() {
        List<DndProfile> out = new ArrayList<>(profiles.values());
        out.sort(java.util.Comparator.comparing(DndProfile::id));
        return out;
    }

    /** Test seam: replaces the loaded profiles. */
    static void set(Map<String, DndProfile> map) { profiles = Map.copyOf(map); }
}
