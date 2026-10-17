package com.newuniverse.nusmp.labyrinth.mega.system;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the quests and the stat table are data ({@code data/<namespace>/mega/system/quests.json} and {@code stats.json}), so a datapack can retune a number, add
 * a quest or switch one off without a mod update, and a data reload ({@code /reload}, a world load) must rebuild them. A plain
 * {@code ResourceManagerReloadListener} (the pattern of the avatar races and the faction loader) reads every such file in every namespace, hands the parsed
 * tree to the pure {@link QuestParser} / {@link StatParser}, logs each problem once with the file it came from, and installs both results in one step. One bad
 * file or row never stops the others. The shipped {@code nusmp} files are read FIRST, so a datapack's file of the same name (or another namespace) overrides a
 * quest or a stat by id, because later entries replace earlier ones. The Gson tree is converted to plain maps and lists here, and only here, so the parsers
 * stay library-free and testable.
 */
public final class SystemReload implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    static final String DIR = "mega/system";

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<QuestTemplate> quests = new ArrayList<>();
        List<StatTable.Def> stats = new ArrayList<>();
        try {
            for (Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource> e : sorted(manager.listResources(DIR, rl -> rl.getPath().endsWith("/quests.json")))) {
                Object tree = read(e);
                if (!(tree instanceof Map<?, ?>)) continue;
                @SuppressWarnings("unchecked") QuestParser.Result r = QuestParser.parse((Map<String, Object>) tree);
                for (String p : r.problems()) LOG.warn("[nusmp] System quest file {}: {}", e.getKey(), p);
                quests.addAll(r.templates());
            }
            for (Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource> e : sorted(manager.listResources(DIR, rl -> rl.getPath().endsWith("/stats.json")))) {
                Object tree = read(e);
                if (!(tree instanceof Map<?, ?>)) continue;
                @SuppressWarnings("unchecked") StatParser.Result r = StatParser.parse((Map<String, Object>) tree);
                for (String p : r.problems()) LOG.warn("[nusmp] System stat file {}: {}", e.getKey(), p);
                stats.addAll(r.defs());
            }
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] The System could not scan its data folder", ex);
        }
        QuestCatalog catalog = new QuestCatalog(quests);
        StatTable table = new StatTable(stats);
        SystemData.install(catalog, table);
        if (catalog.isEmpty()) LOG.warn("[nusmp] The System has no quest templates: floors will have no quests (data/<namespace>/mega/system/quests.json)");
        if (table.isEmpty()) LOG.warn("[nusmp] The System has no stat table: stat points will do nothing (data/<namespace>/mega/system/stats.json)");
        LOG.info("[nusmp] Loaded {} System quest templates and {} stats", catalog.size(), table.size());
    }

    /** The files in the order they are applied: the mod's own namespace first, then the others alphabetically, so a datapack overrides the shipped rows. */
    private static List<Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource>> sorted(Map<ResourceLocation, net.minecraft.server.packs.resources.Resource> found) {
        List<Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource>> list = new ArrayList<>(found.entrySet());
        list.sort(Comparator.comparing((Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource> e) -> !"nusmp".equals(e.getKey().getNamespace()))
                .thenComparing(e -> e.getKey().getNamespace())
                .thenComparing(e -> e.getKey().getPath()));
        return list;
    }

    private static Object read(Map.Entry<ResourceLocation, net.minecraft.server.packs.resources.Resource> e) {
        try (Reader reader = e.getValue().openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            return root.isJsonObject() ? toTree(root.getAsJsonObject()) : null;
        } catch (Exception ex) {
            LOG.warn("[nusmp] System data file {} skipped: {}", e.getKey(), ex.toString());
            return null;
        }
    }

    /** A Gson element as plain Java: objects to insertion-ordered maps, arrays to lists, numbers to Double, booleans, strings, null. */
    static Object toTree(JsonElement el) {
        if (el == null || el.isJsonNull()) return null;
        if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : o.entrySet()) m.put(e.getKey(), toTree(e.getValue()));
            return m;
        }
        if (el.isJsonArray()) {
            JsonArray a = el.getAsJsonArray();
            List<Object> l = new ArrayList<>();
            for (JsonElement x : a) l.add(toTree(x));
            return l;
        }
        JsonPrimitive p = el.getAsJsonPrimitive();
        if (p.isBoolean()) return p.getAsBoolean();
        if (p.isNumber()) return p.getAsDouble();
        return p.getAsString();
    }
}
