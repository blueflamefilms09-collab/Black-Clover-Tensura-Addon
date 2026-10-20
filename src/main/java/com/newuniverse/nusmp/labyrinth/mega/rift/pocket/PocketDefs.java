package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndProfile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the registry of the exclusive pocket dungeons, filled by a reload listener on {@code data/<ns>/mega/pockets} (the same pattern as the Decennial boss profiles: the game lists the files across
 * every datapack in priority order, so a datapack can replace a tomb or add another, and {@code /reload} applies it). One listener reads two kinds of file: {@code <id>.json} is a pocket
 * ({@link PocketDef}), {@code bosses/<id>.json} is the profile of a mega-boss (the Guardian's own {@link DndProfile} reader, plus the {@link PocketBossExtra} extras in the same file). The new maps are
 * built whole and swapped in with one volatile write, so a rift that is choosing its tomb in the middle of a reload sees the old set or the new set and never half of one; a file with a problem
 * is logged and skipped (that tomb is simply not in the pool) and the rest still load; a pocket whose boss profile is missing is skipped too, because a tomb with no master would be an open door.
 */
public final class PocketDefs extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "mega/pockets";
    private static final Logger LOG = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();

    /** Everything one load produced. */
    private record Loaded(Map<String, PocketDef> defs, Map<String, DndProfile> bosses, Map<String, PocketBossExtra> extras) {}

    private static volatile Loaded loaded = new Loaded(Map.of(), Map.of(), Map.of());

    public PocketDefs() { super(GSON, DIRECTORY); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, DndProfile> bosses = new LinkedHashMap<>();
        Map<String, PocketBossExtra> extras = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            String path = e.getKey().getPath();
            if (!path.startsWith("bosses/")) continue;
            String id = path.substring("bosses/".length());
            try {
                JsonObject o = e.getValue().getAsJsonObject();
                bosses.put(id, DndProfile.parse(id, o));
                extras.put(id, PocketBossExtra.parse(id, o));
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] pocket boss profile {} skipped: {}", e.getKey(), ex.getMessage());
            }
        }
        List<PocketDef> parsed = new ArrayList<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            String path = e.getKey().getPath();
            if (path.contains("/")) continue;
            try {
                PocketDef d = PocketDef.parse(path, e.getValue().getAsJsonObject());
                if (!bosses.containsKey(d.boss())) {
                    LOG.warn("[nusmp] pocket {} skipped: its boss profile '{}' is missing or unreadable", d.id(), d.boss());
                    continue;
                }
                parsed.add(d);
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] pocket {} skipped: {}", e.getKey(), ex.getMessage());
            }
        }
        parsed.sort(Comparator.comparing(PocketDef::id));
        Map<String, PocketDef> defs = new LinkedHashMap<>();
        for (PocketDef d : parsed) defs.put(d.id(), d);
        loaded = new Loaded(Map.copyOf(defs), Map.copyOf(bosses), Map.copyOf(extras));
        LOG.info("[nusmp] exclusive pockets: {} loaded, {} boss profiles", defs.size(), bosses.size());
    }

    // ---------------------------------------------------------------- queries
    /** The pocket of an id, or null. */
    public static PocketDef get(String id) { return id == null ? null : loaded.defs().get(id); }

    /** Every loaded pocket, in id order. */
    public static List<PocketDef> all() {
        List<PocketDef> out = new ArrayList<>(loaded.defs().values());
        out.sort(Comparator.comparing(PocketDef::id));
        return out;
    }

    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (PocketDef d : all()) out.add(d.id());
        return out;
    }

    public static int count() { return loaded.defs().size(); }

    /** The boss profile of a pocket (the Guardian's reader's record), or null. */
    public static DndProfile boss(PocketDef d) { return d == null ? null : loaded.bosses().get(d.boss()); }

    public static PocketBossExtra extra(PocketDef d) {
        PocketBossExtra x = d == null ? null : loaded.extras().get(d.boss());
        return x == null ? PocketBossExtra.NONE : x;
    }

    /** Test seam: replaces the loaded set. */
    static void set(Map<String, PocketDef> defs, Map<String, DndProfile> bosses, Map<String, PocketBossExtra> extras) { loaded = new Loaded(Map.copyOf(defs), Map.copyOf(bosses), Map.copyOf(extras)); }
}
