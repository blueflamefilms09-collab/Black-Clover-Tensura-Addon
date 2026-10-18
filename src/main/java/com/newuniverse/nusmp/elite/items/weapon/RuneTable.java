package com.newuniverse.nusmp.elite.items.weapon;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: the "rune items" of the Runic Swords. Besides an enchanted book that carries an engraving, a plain item can be a rune: {@code data/nusmp/elite/items/runes/*.json} map an item id to the
 * engraving it slots ({@code "minecraft:echo_shard": "tensura:magic_interference"}), so a datapack can make any item a rune with no code. All files of the directory are merged (a later file
 * wins). Bad lines are logged and skipped.
 */
public final class RuneTable extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static volatile Map<String, String> runes = Map.of();

    public RuneTable() { super(new Gson(), "elite/items/runes"); }

    public static void onReload(AddReloadListenerEvent e) { e.addListener(new RuneTable()); }

    /** The engraving id a rune item slots, or null when the item is not a rune. */
    public static String engravingOf(String itemId) { return runes.get(itemId); }

    public static Map<String, String> all() { return runes; }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, String> next = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> f : files.entrySet()) {
            if (!f.getValue().isJsonObject() || !f.getValue().getAsJsonObject().has("runes") || !f.getValue().getAsJsonObject().get("runes").isJsonObject()) {
                LOG.warn("[nusmp] rune file {} needs an object 'runes'", f.getKey());
                continue;
            }
            JsonObject o = f.getValue().getAsJsonObject().getAsJsonObject("runes");
            for (Map.Entry<String, JsonElement> e : o.entrySet()) {
                if (!e.getValue().isJsonPrimitive() || !RuneRules.validRuneId(e.getValue().getAsString()) || !RuneRules.validRuneId(e.getKey())) {
                    LOG.warn("[nusmp] rune file {}: bad line '{}'", f.getKey(), e.getKey());
                    continue;
                }
                next.put(e.getKey(), e.getValue().getAsString());
            }
        }
        runes = next;
    }
}
