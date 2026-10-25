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

    private static final WornCatalog EMPTY = WornCatalog.parse(List.of());
    private static long retryAt;

    /** Was the catalog really read (so an empty answer for a relic means "no entry", not "not loaded yet")? */
    public static boolean catalogLoaded() { return catalog != null; }

    /**
     * The catalog (read on first use after a reload). 1.99.0: a catalog that could not be read (the resource manager was not ready, the file is missing) is NOT kept: an empty one is
     * answered, nothing is cached from it by the library, and the read is tried again after three seconds. It used to be kept for the rest of the session, which dressed no relic at all.
     */
    public static WornCatalog catalog() {
        if (catalog != null) return catalog;
        if (System.currentTimeMillis() < retryAt) return EMPTY;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl("relic_worn/catalog.txt"));
            if (res.isPresent()) {
                try (BufferedReader in = res.get().openAsReader()) {
                    catalog = WornCatalog.parse(in.lines().toList());
                }
            } else {
                LogUtils.getLogger().error("[nusmp] the worn relic catalog assets/nusmp/relic_worn/catalog.txt is missing; relics keep their plain look");
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] the worn relic catalog could not be read; relics keep their plain look", ex);
        }
        if (catalog == null) {
            retryAt = System.currentTimeMillis() + 3000L;
            return EMPTY;
        }
        return catalog;
    }

    private static UpgradeCatalog upgrade;

    /**
     * The catalog of the visual upgrade (assets/nusmp/visual_upgrade/catalog.txt), read on first use after a reload; empty when the file is missing or cannot be read (every relic then
     * keeps its 1.101 look). A read that failed because the resource manager was not ready is not remembered.
     */
    public static UpgradeCatalog upgrade() {
        if (upgrade != null) return upgrade;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl("visual_upgrade/catalog.txt"));
            if (res.isPresent()) {
                try (BufferedReader in = res.get().openAsReader()) {
                    upgrade = UpgradeCatalog.parse(in.lines().toList());
                }
            } else {
                upgrade = UpgradeCatalog.EMPTY;
            }
        } catch (IllegalStateException | NullPointerException notReady) {
            return UpgradeCatalog.EMPTY;
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] the visual upgrade catalog could not be read; relics keep their 1.101 look", ex);
            upgrade = UpgradeCatalog.EMPTY;
        }
        return upgrade;
    }

    private static java.util.Set<String> x3Index;

    /**
     * 3.9.0: the models that have a blocky detailed version (assets/nusmp/relic_worn_x3/index.txt, written by tools/relics/gen_worn_x3.py), read once after a reload; empty when the file
     * is missing (every armour relic then wears its 3.8 models). A read that failed because the resource manager was not ready is not remembered.
     */
    public static java.util.Set<String> x3Models() {
        if (x3Index != null) return x3Index;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl("relic_worn_x3/index.txt"));
            java.util.Set<String> set = new java.util.HashSet<>();
            if (res.isPresent()) {
                try (BufferedReader in = res.get().openAsReader()) {
                    in.lines().map(String::trim).filter(l -> !l.isEmpty() && l.charAt(0) != '#').forEach(set::add);
                }
            }
            x3Index = java.util.Set.copyOf(set);
        } catch (IllegalStateException | NullPointerException notReady) {
            return java.util.Set.of();
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] the x3 model index could not be read; armour relics keep their 3.8 models", ex);
            x3Index = java.util.Set.of();
        }
        return x3Index;
    }

    /** The model of this name, or null. */
    public static WornMesh mesh(String name) {
        Optional<WornMesh> cached = MESHES.get(name);
        if (cached != null) return cached.orElse(null);
        WornMesh mesh = null;
        try {
            // 1.102.0: a name starting with "vu/" is a visual upgrade model (assets/nusmp/visual_upgrade/models/<rest>.txt); every other name is a library model as before
            // 3.9.0: a name starting with "x3/" is the blocky detailed version of an armour relic model (assets/nusmp/relic_worn_x3/<rest>.txt)
            String file = name.startsWith("vu/") ? "visual_upgrade/models/" + name.substring(3) : name.startsWith("x3/") ? "relic_worn_x3/" + name.substring(3) : "relic_worn/" + name;
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl(file + ".txt"));
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
            if (ex instanceof IllegalStateException || ex instanceof NullPointerException) return null;      // the resource manager was not ready: ask again next time
        }
        MESHES.put(name, Optional.ofNullable(mesh));
        if (mesh != null && mesh.lodName != null && !mesh.lodName.equals(name)) {                  // a visual upgrade model: read its lod model too, once
            WornMesh lod = mesh(mesh.lodName);
            if (lod != null) mesh.attachLod(lod);
        }
        return mesh;
    }

    /** Forgets everything read (a resource reload). */
    public static void clear() {
        MESHES.clear();
        catalog = null;
        upgrade = null;
        x3Index = null;
        retryAt = 0L;
    }
}
