package com.newuniverse.nusmp.avatar.aptitude;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Why: the owner wants the Magic Aptitudes as datapack data, so this is the reload listener that fills {@link AptitudeCatalog} (the contract list) and
 * {@link AptitudeLook} (the colours and icons) from every {@code data/<namespace>/avatar_aptitudes/*.json}. It follows the race loader: a plain
 * resource-manager reload listener (no dynamic registry, because the client only ever needs the rows the server sends in the Soul Forge state packet),
 * one bad file is logged and skipped, and both tables are swapped in one assignment each so a reload in progress never exposes a half-filled list.
 * A pack may override a shipped aptitude by shipping the same file name (the later pack wins, as for every datapack file). Server side only: it is added
 * through {@code AddReloadListenerEvent} by {@code ritual.SoulForgeSystem}.
 */
public final class AptitudeLoader implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String DIR = "avatar_aptitudes";

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, AptitudeParser.Parsed> loaded = new TreeMap<>();
        for (Map.Entry<ResourceLocation, Resource> e : manager.listResources(DIR, rl -> rl.getPath().endsWith(".json")).entrySet()) {
            String path = e.getKey().getPath();
            String id = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
            try (Reader reader = e.getValue().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                loaded.put(id, AptitudeParser.parse(id, root));
            } catch (Exception ex) {
                LOG.warn("[nusmp] Skipping avatar aptitude {}: {}", e.getKey(), ex.toString());
            }
        }
        List<Aptitude> list = new ArrayList<>();
        Map<String, AptitudeLook> looks = new HashMap<>();
        for (AptitudeParser.Parsed p : loaded.values()) {
            list.add(p.aptitude());
            looks.put(p.aptitude().id(), p.look());
        }
        AptitudeCatalog.replaceAll(list);
        AptitudeLook.replaceAll(looks);
        LOG.info("[nusmp] Loaded {} avatar aptitudes", list.size());
    }
}
