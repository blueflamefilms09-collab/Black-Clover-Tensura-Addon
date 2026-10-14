package com.newuniverse.nusmp.client.relics.worn.lib;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the worn model files and the catalog from the resource packs, once each, and keeps them until the next resource reload ({@link #clear}). A model that is
 * missing or malformed is logged once and answered with null, so its relic keeps the old plain look. Client thread only.
 */
public final class WornModels {
    private WornModels() {}

    private static final Map<String, Optional<WornMesh>> MESHES = new HashMap<>();
    private static WornCatalog catalog;

    static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    /** The catalog (read on first use after a reload; an empty one if the file is missing). */
    public static WornCatalog catalog() {
        if (catalog != null) return catalog;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl("relic_worn/catalog.txt"));
            if (res.isPresent()) {
                try (BufferedReader in = res.get().openAsReader()) {
                    catalog = WornCatalog.parse(in.lines().toList());
                }
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] the worn relic catalog could not be read; relics keep their plain look", ex);
        }
        if (catalog == null) catalog = WornCatalog.parse(List.of());
        return catalog;
    }

    /** The model of this name, or null. */
    public static WornMesh mesh(String name) {
        Optional<WornMesh> cached = MESHES.get(name);
        if (cached != null) return cached.orElse(null);
        WornMesh mesh = null;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl("relic_worn/" + name + ".txt"));
            if (res.isPresent()) {
                try (BufferedReader in = res.get().openAsReader()) {
                    mesh = WornMesh.parse(name, in.lines().toList());
                }
            } else {
                LogUtils.getLogger().warn("[nusmp] worn model {} is missing", name);
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] worn model {} failed to load", name, ex);
            mesh = null;
        }
        MESHES.put(name, Optional.ofNullable(mesh));
        return mesh;
    }

    /** Forgets everything read (a resource reload). */
    public static void clear() {
        MESHES.clear();
        catalog = null;
    }
}
