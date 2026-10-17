package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
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

/**
 * Why: the loaded set of {@link ThemeDef}s and the reload listener that fills it from {@code data/<namespace>/mega/themes/*.json}. A plain
 * resource-manager reload listener (the pattern of {@code AvatarRaces}, which compiles in this project) rather than a dynamic datapack registry:
 * a registry would need a codec for the whole record and a client sync nobody reads (themes only matter on the server, where the processor and the
 * Crucible run), and a listener lets ONE broken file be skipped with a log line instead of failing the whole datapack load, which matters because a
 * theme file is edited by hand. A datapack replaces a theme by providing the same path; the baked baseline for {@code gravity} stays even when its file
 * is gone, and any other theme without a file simply looks like the baseline (a missing file never breaks generation). The set is swapped in one
 * assignment, so a reload in progress never exposes a half-filled map to the processor that may be running on the server thread, and the block
 * tables derived from the old definitions are dropped with it.
 */
public final class ThemeDefs implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    static final String DIR = "mega/themes";
    private static volatile Map<String, ThemeDef> DEFS = Map.of("gravity", ThemeDef.gravity());

    /** The definition of a theme: its file, or a plain baseline-looking one when it has none. Never null. */
    public static ThemeDef get(RegionTheme theme) {
        if (theme == null) return ThemeDef.gravity();
        ThemeDef d = DEFS.get(theme.id());
        return d != null ? d : ThemeDef.plain(theme.id());
    }

    /** The definition with this id (any case), or null. */
    public static ThemeDef byId(String id) { return id == null ? null : DEFS.get(id.toLowerCase(java.util.Locale.ROOT)); }

    /** Every loaded definition, in load order. */
    public static Collection<ThemeDef> all() { return DEFS.values(); }

    /** Replaces the loaded set (the reload, and tests); the baseline is always present. */
    public static void replace(Map<String, ThemeDef> defs) {
        Map<String, ThemeDef> m = new LinkedHashMap<>(defs);
        m.putIfAbsent("gravity", ThemeDef.gravity());
        DEFS = Collections.unmodifiableMap(m);
        ThemeTables.clear();
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, ThemeDef> loaded = new LinkedHashMap<>();
        List<Map.Entry<ResourceLocation, Resource>> found = new ArrayList<>();
        try {
            found.addAll(manager.listResources(DIR, rl -> rl.getPath().endsWith(".json")).entrySet());
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] mega themes: could not list {}: {}", DIR, e.toString());
        }
        found.sort(java.util.Comparator.comparing(x -> x.getKey().toString()));
        for (Map.Entry<ResourceLocation, Resource> e : found) {
            String path = e.getKey().getPath();
            String fileId = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
            try (Reader reader = e.getValue().openAsReader()) {
                ThemeDef.Parsed p = ThemeDef.parse(fileId, JsonTrees.read(reader));
                for (String w : p.warnings()) LOG.warn("[nusmp] mega theme {}: {}", e.getKey(), w);
                if (p.def() != null) loaded.put(p.def().id(), p.def());
            } catch (Exception ex) {
                LOG.warn("[nusmp] mega theme {} skipped: {}", e.getKey(), ex.toString());
            }
        }
        for (RegionTheme t : RegionTheme.values()) if (!loaded.containsKey(t.id()) && t != RegionTheme.GRAVITY) LOG.warn("[nusmp] mega themes: no file for theme '{}': its sectors look like the baseline", t.id());
        replace(loaded);
        LOG.info("[nusmp] mega themes: loaded {}", loaded.keySet());
    }
}
