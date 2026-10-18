package com.newuniverse.nusmp.elite.hub;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Why: the quests ({@code data/<ns>/elite/hub/quests/*.json}) and the shop ({@code data/<ns>/elite/hub/shop.json}) are data, so a data reload ({@code /reload}, a world load) must rebuild them.
 * A plain {@code ResourceManagerReloadListener} (the pattern of the System's quest loader) reads every such file in every namespace, hands the text to the pure {@link QuestParser} /
 * {@link ShopTable}, logs each problem once with the file it came from, and installs both results in one step. One bad file or row never stops the others. The shipped {@code nusmp} files are read
 * FIRST, so a datapack's quest with the same id replaces a shipped one, and a datapack's {@code shop.json} (another namespace) replaces the shipped shop whole. Offers whose item does not exist
 * in this game (an addon that is not installed) are dropped with a log line.
 */
public final class HubReload implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    static final String QUEST_DIR = "elite/hub/quests";
    static final String SHOP_FILE = "elite/hub/shop.json";

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<QuestDef> quests = new ArrayList<>();
        ShopTable shop = ShopTable.EMPTY;
        try {
            for (Map.Entry<ResourceLocation, Resource> e : sorted(manager.listResources(QUEST_DIR, rl -> rl.getPath().endsWith(".json")))) {
                String text = read(e);
                if (text == null) continue;
                QuestParser.Result r = QuestParser.parse(text);
                for (String p : r.problems()) LOG.warn("[nusmp] Hub quest file {}: {}", e.getKey(), p);
                quests.addAll(r.quests());
            }
            for (Map.Entry<ResourceLocation, Resource> e : sorted(manager.listResources("elite/hub", rl -> rl.getPath().equals(SHOP_FILE)))) {
                String text = read(e);
                if (text == null) continue;
                ShopTable.Parsed p = ShopTable.parse(text);
                for (String problem : p.problems()) LOG.warn("[nusmp] Hub shop file {}: {}", e.getKey(), problem);
                if (!p.table().isEmpty()) shop = p.table();
            }
        } catch (RuntimeException ex) {
            LOG.warn("[nusmp] The Hub could not scan its data folder", ex);
        }
        final ShopTable all = shop;
        ShopTable known = all.filtered(item -> {
            ResourceLocation id = ResourceLocation.tryParse(item);
            boolean ok = id != null && BuiltInRegistries.ITEM.containsKey(id);
            if (!ok) LOG.info("[nusmp] Hub shop: '{}' is not an item in this game, the offer is dropped", item);
            return ok;
        });
        QuestCatalog catalog = new QuestCatalog(quests);
        HubCatalogs.install(catalog, known);
        if (catalog.isEmpty()) LOG.warn("[nusmp] The Hub has no quests (data/<namespace>/{}/*.json)", QUEST_DIR);
        LOG.info("[nusmp] Loaded {} Hub quests and {} shop offers", catalog.size(), known.size());
    }

    /** The files in the order they are applied: the mod's own namespace first, then the others alphabetically, so a datapack overrides the shipped rows. */
    private static List<Map.Entry<ResourceLocation, Resource>> sorted(Map<ResourceLocation, Resource> found) {
        List<Map.Entry<ResourceLocation, Resource>> list = new ArrayList<>(found.entrySet());
        list.sort(Comparator.comparing((Map.Entry<ResourceLocation, Resource> e) -> !"nusmp".equals(e.getKey().getNamespace()))
                .thenComparing(e -> e.getKey().getNamespace())
                .thenComparing(e -> e.getKey().getPath()));
        return list;
    }

    private static String read(Map.Entry<ResourceLocation, Resource> e) {
        try (Reader reader = e.getValue().openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            return root.toString();
        } catch (Exception ex) {
            LOG.warn("[nusmp] Hub data file {} skipped: {}", e.getKey(), ex.toString());
            return null;
        }
    }
}
