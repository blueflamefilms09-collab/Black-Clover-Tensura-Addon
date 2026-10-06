package com.newuniverse.nusmp.vfx.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.Reader;
import java.util.EnumMap;
import java.util.Map;

/** Loads assets/nusmp/vfx/effects.json (reloads with F3+T / resource packs can override it). */
public final class VfxDefinitions implements ResourceManagerReloadListener {
    public record Def(int color, int duration, float shake) {}

    private static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath("nusmp", "vfx/effects.json");
    private static final Map<VfxShape, Def> DEFS = new EnumMap<>(VfxShape.class);

    public static Def get(VfxShape shape) { return DEFS.get(shape); }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        DEFS.clear();
        manager.getResource(FILE).ifPresent(res -> {
            try (Reader reader = res.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (VfxShape s : VfxShape.values()) {
                    String key = s.name().toLowerCase();
                    if (!root.has(key)) continue;
                    JsonObject o = root.getAsJsonObject(key);
                    int color = o.has("color") ? (int) Long.parseLong(o.get("color").getAsString(), 16) : 0;
                    int dur = o.has("duration") ? o.get("duration").getAsInt() : 0;
                    float shake = o.has("shake") ? o.get("shake").getAsFloat() : 0f;
                    DEFS.put(s, new Def(color, dur, shake));
                }
            } catch (Exception e) {
                com.mojang.logging.LogUtils.getLogger().warn("[nusmp] Could not read vfx/effects.json", e);
            }
        });
    }
}
