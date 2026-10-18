package com.newuniverse.nusmp.elite.items.weapon;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
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
 * Why: loads the technique files {@code data/nusmp/elite/items/abilities/<item>.json} on every datapack reload and answers {@link #of} by the item's registry path. A file with a mistake does not
 * stop the game: {@link AbilityParser} reports the problems (logged here) and keeps what it could read; a file with no usable primary technique is skipped and the weapon is then a plain sword (one
 * log line per reload). The map is swapped in one volatile assignment, so a technique that is running while a reload happens uses one consistent set.
 */
public final class AbilityTables extends SimpleJsonResourceReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    private static volatile Map<String, WeaponAbilities> tables = Map.of();

    public AbilityTables() { super(new Gson(), "elite/items/abilities"); }

    public static void onReload(AddReloadListenerEvent e) { e.addListener(new AbilityTables()); }

    /** The abilities of the weapon with this registry path ({@code void_edge}), or null. */
    public static WeaponAbilities of(String itemPath) { return tables.get(itemPath); }

    /** How many weapons have abilities loaded. */
    public static int size() { return tables.size(); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, WeaponAbilities> next = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
            if (!e.getValue().isJsonObject()) { LOG.warn("[nusmp] ability file {} is not a JSON object", e.getKey()); continue; }
            AbilityParser.Result res = AbilityParser.parse(e.getValue().getAsJsonObject());
            for (String err : res.errors()) LOG.warn("[nusmp] ability file {}: {}", e.getKey(), err);
            if (res.abilities() != null) next.put(e.getKey().getPath(), res.abilities());
            else LOG.warn("[nusmp] ability file {} has no usable technique; that weapon is a plain sword", e.getKey());
        }
        tables = next;
    }
}
