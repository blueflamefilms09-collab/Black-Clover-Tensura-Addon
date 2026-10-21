package com.newuniverse.nusmp.client.vupgrade.godb;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The tables of the upgraded weapons, read from assets/nusmp/visual_upgrade/&lt;item id&gt;.json the first time an item is asked for and kept until the next resource reload
 * ({@link GodBUpgrade} clears it). A missing table is remembered too, so an item that is not upgraded costs one hash lookup per frame and nothing else. Client thread only.
 * Next to each table it keeps what the renderer and the particle tick would otherwise rebuild every frame: the glow-map location of the vein pass and one {@link ParticleOptions}
 * per particle layer.
 */
public final class UpgradeSpecs {
    private UpgradeSpecs() {}

    /** A loaded table with its client-side extras. */
    public static final class Entry {
        public final UpgradeSpec spec;
        public final ResourceLocation veinTexture;
        public final ParticleOptions[] particles;

        Entry(UpgradeSpec spec) {
            this.spec = spec;
            this.veinTexture = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/weapon/" + spec.mesh() + "_glow2.png");
            this.particles = new ParticleOptions[spec.layers().size()];
            for (int i = 0; i < particles.length; i++) particles[i] = options(spec.layers().get(i));
        }
    }

    private static final Entry NONE = null;
    private static final Map<String, Optional<Entry>> BY_ITEM = new HashMap<>();
    private static final Map<String, Entry> BY_MESH = new HashMap<>();

    /** The table of an item id, or null when the item is not upgraded (or its table is broken: logged once). */
    public static Entry get(String itemId) {
        Optional<Entry> e = BY_ITEM.get(itemId);
        if (e == null) {
            e = Optional.ofNullable(load(itemId));
            BY_ITEM.put(itemId, e);
            e.ifPresent(x -> BY_MESH.put(x.spec.mesh(), x));
        }
        return e.orElse(NONE);
    }

    /** The table of an upgraded mesh id ({@code up/<item>}), or null (the mesh was drawn through {@link #get} first, so it is known). */
    public static Entry byMesh(String meshId) {
        return BY_MESH.get(meshId);
    }

    public static void clear() {
        BY_ITEM.clear();
        BY_MESH.clear();
    }

    private static Entry load(String itemId) {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("nusmp", "visual_upgrade/" + itemId + ".json"));
            if (res.isEmpty()) return null;
            try (BufferedReader in = res.get().openAsReader()) {
                JsonObject o = JsonParser.parseReader(in).getAsJsonObject();
                return new Entry(UpgradeSpec.parse(o));
            }
        } catch (Exception ex) {
            LogUtils.getLogger().error("[nusmp] visual upgrade table of {} is broken; the old model is drawn", itemId, ex);
            return null;
        }
    }

    /**
     * The particle of a layer, always one of the mod's own custom particles (1.110.0): "fx" and "dust" are the hue-themed custom mote dyed with the layer's colour, a catalog id is itself,
     * and a vanilla name goes through the alias table of {@link com.newuniverse.nusmp.particle.fx.UpgradeParticleAlias} to the nearest custom particle. Never a vanilla particle.
     */
    static ParticleOptions options(UpgradeSpec.Layer l) {
        return com.newuniverse.nusmp.particle.fx.UpgradeParticleOptions.of(l.particle(), l.rgb(), l.size());
    }
}
