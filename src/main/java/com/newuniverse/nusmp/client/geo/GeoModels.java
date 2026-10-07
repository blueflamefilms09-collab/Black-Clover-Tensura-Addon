package com.newuniverse.nusmp.client.geo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * 0.53: loads and caches the geo.json models and animation.json files, once per resource reload. A missing or broken file is logged once
 * and gives an empty model (nothing is drawn), never an exception in the render thread.
 */
public final class GeoModels {
    private GeoModels() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<ResourceLocation, GeoModelData> MODELS = new HashMap<>();
    private static final Map<ResourceLocation, GeoAnim> ANIMS = new HashMap<>();
    private static Function<ResourceLocation, Reader> source = GeoModels::fromResources;

    /** Called once on the client: a resource reload re-reads the files. */
    public static void init(IEventBus modBus) {
        modBus.addListener((ModelEvent.BakingCompleted e) -> clear());
    }

    public static void clear() {
        MODELS.clear();
        ANIMS.clear();
    }

    /** Replaces where files come from (the headless preview reads them from disk). */
    public static void setSource(Function<ResourceLocation, Reader> s) {
        source = s;
        clear();
    }

    public static GeoModelData model(ResourceLocation id) {
        GeoModelData m = MODELS.get(id);
        if (m == null) {
            JsonObject o = read(id, true);
            m = o == null ? GeoModelData.empty(id.toString()) : GeoModelData.parse(id.toString(), o);
            MODELS.put(id, m);
        }
        return m;
    }

    /** The animation file, or null if it does not exist (a model without animations is drawn at rest). */
    public static GeoAnim animations(ResourceLocation id) {
        if (id == null) return null;
        if (ANIMS.containsKey(id)) return ANIMS.get(id);
        JsonObject o = read(id, false);
        GeoAnim a = o == null ? null : GeoAnim.parse(o);
        ANIMS.put(id, a);
        return a;
    }

    /** Reads and parses a json file; 'required' = a missing file is logged (animations are optional). */
    private static JsonObject read(ResourceLocation id, boolean required) {
        try (Reader in = source.apply(id)) {
            if (in == null) {
                if (required) LOG.warn("[nusmp] geo file {} not found", id);
                return null;
            }
            return JsonParser.parseReader(in).getAsJsonObject();
        } catch (Exception ex) {
            LOG.error("[nusmp] geo file {} failed to load; it is not drawn", id, ex);
            return null;
        }
    }

    private static Reader fromResources(ResourceLocation id) {
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(id);
            return res.isPresent() ? res.get().openAsReader() : null;
        } catch (IOException ex) {
            return null;
        }
    }
}
