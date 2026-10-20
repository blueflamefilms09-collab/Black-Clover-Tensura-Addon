package com.newuniverse.nusmp.client.upgrade;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import com.newuniverse.nusmp.relics.upgrade.UpgradeItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the client's table of the effect specs (assets/nusmp/visual_upgrade/&lt;id&gt;.json), read lazily from the resource manager and thrown away on a resource reload
 * ({@code WeaponUpgrade.clear} is called where the weapon meshes are cleared). A spec that is missing or broken is logged once and simply absent: the item then draws as it did before.
 * Also the lookup of an act recipe by its signature id (the crc32 the server sends).
 */
public final class UpgradeSpecs {
    private UpgradeSpecs() {}

    private static final Map<String, UpgradeSpec> SPECS = new HashMap<>();
    private static Map<Integer, UpgradeSpec.Act> signatures;

    public static void clear() {
        SPECS.clear();
        signatures = null;
    }

    /** The spec of an upgraded weapon, or null when it has none (or it is broken). */
    public static UpgradeSpec get(String id) {
        if (id == null) return null;
        if (SPECS.containsKey(id)) return SPECS.get(id);
        UpgradeSpec spec = null;
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("nusmp", "visual_upgrade/" + id + ".json"));
            if (res.isPresent()) {
                try (BufferedReader in = res.get().openAsReader()) {
                    spec = UpgradeSpec.parse(in);
                }
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] visual upgrade spec {} failed to load; the item keeps its old look", id, ex);
            spec = null;
        }
        SPECS.put(id, spec);
        return spec;
    }

    /** The recipe of the act with this signature id, or null when it is not one of ours. */
    public static UpgradeSpec.Act signature(int id) {
        if (signatures == null) {
            Map<Integer, UpgradeSpec.Act> map = new HashMap<>();
            for (String item : UpgradeItems.ALL) {
                UpgradeSpec s = get(item);
                if (s != null) for (UpgradeSpec.Act a : s.acts) map.put(a.id(), a);
            }
            signatures = map;
        }
        return signatures.get(id);
    }
}
