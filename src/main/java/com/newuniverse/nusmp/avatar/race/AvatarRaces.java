package com.newuniverse.nusmp.avatar.race;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Why: the owner wants races as datapack data. This is the loaded set and its reload listener: every {@code data/<namespace>/avatar_races/*.json} becomes an
 * {@link AvatarRace} keyed by file name. A plain resource-manager reload listener was chosen over a dynamic datapack registry
 * ({@code DataPackRegistryEvent.NewRegistry}) because a registry would need a codec for the whole record and a client sync of it, while the client here only
 * ever needs the list the server sends in the races payload; the reload listener also lets one bad file be skipped without failing the datapack load.
 * The map is swapped in one assignment so a reload in progress never exposes a half-filled set.
 */
public final class AvatarRaces implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String DIR = "avatar_races";
    private static volatile Map<String, AvatarRace> RACES = Collections.emptyMap();

    public static Collection<AvatarRace> all() { return RACES.values(); }

    /** The race with this id (the namespace is ignored), or null. */
    public static AvatarRace get(String id) {
        if (id == null) return null;
        int c = id.indexOf(':');
        return RACES.get(c >= 0 ? id.substring(c + 1) : id);
    }

    /** The races this player may pick right now: selectable ones whose connection conditions they satisfy. */
    public static List<AvatarRace> availableTo(ServerPlayer p) {
        List<AvatarRace> out = new ArrayList<>();
        for (AvatarRace r : all()) if (r.selectable() && RaceConnectionChecker.check(p, r).ok()) out.add(r);
        return out;
    }

    /** Replaces the loaded set (used by the reload and by tests/commands). */
    public static void replace(Map<String, AvatarRace> races) { RACES = Collections.unmodifiableMap(new LinkedHashMap<>(races)); }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, AvatarRace> loaded = new TreeMap<>();
        for (Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource> e : manager.listResources(DIR, rl -> rl.getPath().endsWith(".json")).entrySet()) {
            String path = e.getKey().getPath();
            String id = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
            try (Reader reader = e.getValue().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                loaded.put(id, AvatarRaceParser.parse(id, root));
            } catch (Exception ex) {
                LOG.warn("[nusmp] Skipping avatar race {}: {}", e.getKey(), ex.toString());
            }
        }
        for (AvatarRace r : loaded.values()) {
            if (r.evolves() && !loaded.containsKey(r.evolvesTo())) LOG.warn("[nusmp] Avatar race {} evolves into '{}' which does not exist", r.id(), r.evolvesTo());
        }
        replace(loaded);
        LOG.info("[nusmp] Loaded {} avatar races", loaded.size());
    }
}
