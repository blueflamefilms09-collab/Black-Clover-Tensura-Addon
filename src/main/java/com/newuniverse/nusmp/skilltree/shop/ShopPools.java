package com.newuniverse.nusmp.skilltree.shop;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Why: an ITEM line of a shop file names a single item or an item TAG, and what a tag holds is only known once the world exists (tags are bound after the data files are read, and other mods add
 * to them). This is the one place that turns a line into its POOL of real items, on the server only, and keeps the answer: the pool of a line is built once after a data reload or a tag rebinding
 * ({@link MarketData#generation()} changes) and then read from memory, so a shelf is O(slots) per call with no registry walk. A pool holds only items that (1) exist in this game, (2) are not
 * on the line's {@code exclude} list or the shop type's ban list, (3) belong to the shop's type, (4) the witch does not buy back for as much as the line asks (the loop guard), sorted by id so the
 * pool, and the roll that indexes into it, is the same on every machine, and (5) at most {@link #MAX_POOL}. A tag that is unknown or empty, or an item no mod provides, gives an empty pool: the line
 * makes no slot, the problem is logged ONCE per data generation (not once per shelf), and nothing throws. Server thread only.
 */
public final class ShopPools {
    private ShopPools() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The most items of one pool kept; a bigger tag is cut at the first ids in alphabetical order. */
    public static final int MAX_POOL = 512;

    private static int builtFor = -1;
    private static final Map<String, List<StockBoard.Slot>> SLOTS = new HashMap<>();
    private static final Map<String, List<StockBoard.PoolItem>> POOLS = new HashMap<>();
    private static final Set<String> LOGGED = new HashSet<>();

    /** The slots of a shop (empty for a shop that does not exist), built on first use after a reload and kept. */
    public static synchronized List<StockBoard.Slot> slots(String shopId) {
        sync();
        return SLOTS.computeIfAbsent(shopId, ShopPools::build);
    }

    /** The resolved pool of one line of a shop (empty when it has none), for the self-test and the status command. */
    public static synchronized List<StockBoard.PoolItem> pool(String shopId, StockEntry line) {
        sync();
        MarketData.Shop shop = MarketData.shop(shopId);
        ShopType type = shop == null ? null : MarketData.types().get(shop.type());
        return resolve(shopId, type, line);
    }

    /** Forgets everything (server stopped). */
    public static synchronized void clear() {
        SLOTS.clear();
        POOLS.clear();
        LOGGED.clear();
        builtFor = -1;
    }

    private static void sync() {
        int gen = MarketData.generation();
        if (gen != builtFor) {
            SLOTS.clear();
            POOLS.clear();
            LOGGED.clear();
            builtFor = gen;
        }
    }

    /** The prefix that keeps two shops' purchase counters apart: none for the witch (her counters were always the bare slot ids). */
    static String prefix(String shopId) {
        return MarketData.WITCH_SHOP.equals(shopId) ? "" : shopId + "/";
    }

    private static List<StockBoard.Slot> build(String shopId) {
        MarketData.Shop shop = MarketData.shop(shopId);
        if (shop == null) return List.of();
        ShopType type = MarketData.types().get(shop.type());
        return List.copyOf(StockBoard.slots(shop.entries(), ShopGoods.routeIds(), e -> resolve(shopId, type, e), prefix(shopId)));
    }

    private static List<StockBoard.PoolItem> resolve(String shopId, ShopType type, StockEntry e) {
        if (e.kind() != StockEntry.Kind.ITEM) return List.of();
        String key = shopId + "/" + e.id();
        List<StockBoard.PoolItem> cached = POOLS.get(key);
        if (cached != null) return cached;
        List<StockBoard.PoolItem> pool = compute(key, type, e);
        POOLS.put(key, pool);
        return pool;
    }

    private static List<StockBoard.PoolItem> compute(String key, ShopType type, StockEntry e) {
        if (type == null) {
            once(key, "the shop type is unknown");
            return List.of();
        }
        ResourceLocation rl = ResourceLocation.tryParse(e.itemOrTagId());
        if (rl == null) {
            once(key, "'" + e.item() + "' is not a valid id");
            return List.of();
        }
        boolean viaTag = e.isTag();
        if (type.check(e.item(), null) == ShopType.Check.REJECTED) {
            once(key, e.item() + " is not something a '" + type.id() + "' shop sells");
            return List.of();
        }
        TreeMap<String, Item> found = new TreeMap<>();
        if (viaTag) {
            Optional<HolderSet.Named<Item>> set = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, rl));
            if (set.isEmpty()) {
                once(key, "the item tag " + e.item() + " does not exist in this game");
                return List.of();
            }
            for (Holder<Item> h : set.get()) {
                Optional<ResourceLocation> id = h.unwrapKey().map(k -> k.location());
                if (id.isPresent()) found.put(id.get().toString(), h.value());
            }
        } else if (BuiltInRegistries.ITEM.containsKey(rl)) {
            Item item = BuiltInRegistries.ITEM.get(rl);
            found.put(rl.toString(), item);
        } else {
            once(key, "the item " + e.item() + " is not in this game (a mod that is not installed?)");
            return List.of();
        }
        List<StockBoard.PoolItem> pool = new ArrayList<>();
        int dropped = 0;
        for (Map.Entry<String, Item> m : found.entrySet()) {
            String id = m.getKey();
            Item item = m.getValue();
            boolean ok = item != Items.AIR && id.length() <= StockBoard.MAX_ID && !e.exclude().contains(id) && !type.isBanned(id) && (viaTag || type.allows(id, tagsOf(item)));
            if (!ok) {
                dropped++;
                continue;
            }
            pool.add(new StockBoard.PoolItem(id, Math.max(1, new ItemStack(item).getMaxStackSize())));
            if (pool.size() >= MAX_POOL) break;
        }
        List<StockBoard.PoolItem> safe = ShopLoopGuard.safePool(pool, e.priceMin(), id -> {
            PriceTable.Price p = MarketData.prices().get(id);
            return p == null ? 0 : p.gold();
        });
        if (safe.size() < pool.size()) LOG.info("[nusmp] black market: {} item(s) of {} left out of the pool because the witch pays as much or more for them", pool.size() - safe.size(), key);
        if (safe.isEmpty()) once(key, "the pool of " + e.item() + " is empty (" + found.size() + " found, " + dropped + " not allowed)");
        return List.copyOf(safe);
    }

    private static List<String> tagsOf(Item item) {
        List<String> out = new ArrayList<>();
        BuiltInRegistries.ITEM.wrapAsHolder(item).tags().forEach(t -> out.add(t.location().toString()));
        return out;
    }

    private static void once(String key, String why) {
        if (LOGGED.add(key)) LOG.warn("[nusmp] black market: the line {} makes no slot: {}", key, why);
    }
}
