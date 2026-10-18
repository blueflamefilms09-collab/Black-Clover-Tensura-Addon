package com.newuniverse.nusmp.elite.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.world.rules.BiomeFactors;
import com.newuniverse.nusmp.elite.world.rules.CalamityDef;
import com.newuniverse.nusmp.elite.world.rules.MobTouch;
import com.newuniverse.nusmp.elite.world.rules.OreConversion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: the world engine is data driven: the ore map, the touched-mob table, the biome table and the calamities are JSON under {@code data/nusmp/elite/world/}, reloaded with {@code /reload}.
 * The files of every pack are MERGED (the mod's first, a datapack's after, so a pack adds entries or overrides single ores) with {@link ResourceManager#getResourceStack}; the calamities are
 * one file per calamity, a later file of the same id replaces the earlier. A broken file is logged and skipped, never fatal, and a table that failed to load stays at what it was.
 * The tables are published as whole immutable snapshots (volatile fields), so the engine reads them from the server thread without locks.
 */
public final class WorldData {
    private WorldData() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    private static final String DIR = "elite/world/";

    private static volatile OreConversion.Table ores = new OreConversion.Table();
    private static volatile MobTouch.Table mobs = new MobTouch.Table();
    private static volatile BiomeFactors biomes = new BiomeFactors();
    private static volatile List<CalamityDef> calamities = List.of();

    public static OreConversion.Table ores() { return ores; }

    public static MobTouch.Table mobs() { return mobs; }

    public static BiomeFactors biomes() { return biomes; }

    public static List<CalamityDef> calamities() { return calamities; }

    public static CalamityDef calamity(String id) {
        for (CalamityDef d : calamities) if (d.id().equals(id)) return d;
        return null;
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener((AddReloadListenerEvent e) -> e.addListener((ResourceManagerReloadListener) WorldData::load));
    }

    /** Public so the in-game self-test can reload from the running server's resources. */
    public static void load(ResourceManager manager) {
        try {
            OreConversion.Table o = new OreConversion.Table();
            for (JsonObject f : files(manager, "ore_conversion.json")) o.add(f);
            ores = o;
            MobTouch.Table m = new MobTouch.Table();
            for (JsonObject f : files(manager, "mob_touch.json")) m.add(f);
            mobs = m;
            BiomeFactors b = new BiomeFactors();
            for (JsonObject f : files(manager, "biome_magicule.json")) b.add(f);
            biomes = b;
            List<CalamityDef> list = new ArrayList<>();
            Map<ResourceLocation, Resource> found = manager.listResources(DIR + "calamities", rl -> rl.getPath().endsWith(".json"));
            for (Map.Entry<ResourceLocation, Resource> en : found.entrySet()) {
                JsonObject obj = read(en.getValue(), en.getKey());
                CalamityDef d = obj == null ? null : CalamityDef.parse(obj);
                if (d == null) {
                    LOG.warn("[nusmp] elite world: {} is not a calamity file (needs a snake_case id and an entity), skipped", en.getKey());
                    continue;
                }
                list.removeIf(x -> x.id().equals(d.id()));
                list.add(d);
            }
            list.sort((x, y) -> x.id().compareTo(y.id()));
            calamities = List.copyOf(list);
            LOG.info("[nusmp] elite world: {} ore conversions, {} touched mob types, {} biome rules, {} calamities", ores.size(), mobs.eligible.size(), biomes.size(), calamities.size());
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] elite world: the data could not be loaded: {}", ex.toString());
        }
    }

    private static List<JsonObject> files(ResourceManager manager, String name) {
        List<JsonObject> out = new ArrayList<>();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, DIR + name);
        for (Resource r : manager.getResourceStack(id)) {
            JsonObject o = read(r, id);
            if (o != null) out.add(o);
        }
        return out;
    }

    private static JsonObject read(Resource r, ResourceLocation id) {
        try (Reader in = new InputStreamReader(r.open(), StandardCharsets.UTF_8)) {
            JsonElement el = JsonParser.parseReader(in);
            if (el.isJsonObject()) return el.getAsJsonObject();
            LOG.warn("[nusmp] elite world: {} is not a JSON object, skipped", id);
        } catch (Exception ex) {
            LOG.warn("[nusmp] elite world: {} could not be read: {}", id, ex.toString());
        }
        return null;
    }
}
