package com.newuniverse.nusmp.client.relics.hand;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.client.relics.worn.lib.WornMesh;
import com.newuniverse.nusmp.client.relics.worn.lib.WornModels;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the hand library reads three kinds of files (the hand models, the signature catalog, the transform styles) from the resource packs, and each must be parsed once and
 * kept, not re-read every frame, and forgotten on a resource reload so a pack can replace them. A file that is missing or malformed is logged once and answered with an
 * empty result, so the item keeps its flat sprite instead of crashing or vanishing. Client thread only.
 */
public final class HandModels {
    private HandModels() {}

    private static final Map<String, Optional<WornMesh>> MESHES = new HashMap<>();
    private static HandCatalog catalog;
    private static HandTransforms transforms;

    static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    private static List<String> lines(String path) {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl(path));
            if (res.isPresent()) {
                try (BufferedReader in = res.get().openAsReader()) {
                    return in.lines().toList();
                }
            }
            LogUtils.getLogger().warn("[nusmp] hand library file {} is missing", path);
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] hand library file {} could not be read", path, ex);
        }
        return List.of();
    }

    /** The signature catalog (empty if the file is missing). */
    public static HandCatalog catalog() {
        if (catalog == null) catalog = HandCatalog.parse(lines("relic_hand/catalog.txt"));
        return catalog;
    }

    /** The hold styles (a file that does not give the default and sprite styles is reported once; the renderer then keeps every item as its sprite). */
    public static HandTransforms transforms() {
        if (transforms == null) {
            transforms = HandTransforms.parse(lines("relic_hand/transforms.txt"));
            for (String p : transforms.problems()) LogUtils.getLogger().error("[nusmp] relic_hand/transforms.txt: {}", p);
            if (!transforms.complete()) LogUtils.getLogger().error("[nusmp] relic_hand/transforms.txt has no complete default and sprite style; relics are held as sprites");
        }
        return transforms;
    }

    /**
     * The model of this name: the hand model of the same name first (assets/nusmp/relic_hand/&lt;name&gt;.txt), else the worn model (relic_worn/&lt;name&gt;.txt), so a hand
     * form may reuse a worn model by name. Null if neither exists or it cannot be parsed.
     */
    public static WornMesh mesh(String name) {
        Optional<WornMesh> cached = MESHES.get(name);
        if (cached != null) return cached.orElse(null);
        WornMesh mesh = null;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl("relic_hand/" + name + ".txt"));
            if (res.isPresent()) {
                try (BufferedReader in = res.get().openAsReader()) {
                    mesh = WornMesh.parse(name, in.lines().toList());
                }
            } else {
                mesh = WornModels.mesh(name);
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] hand model {} failed to load", name, ex);
            mesh = null;
        }
        MESHES.put(name, Optional.ofNullable(mesh));
        return mesh;
    }

    /** Forgets everything read (a resource reload). */
    public static void clear() {
        MESHES.clear();
        catalog = null;
        transforms = null;
        HandForms.clear();
    }
}
