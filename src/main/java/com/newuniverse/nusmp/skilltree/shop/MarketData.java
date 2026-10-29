package com.newuniverse.nusmp.skilltree.shop;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Why: the witch's prices and shelf are data ({@code data/nusmp/black_market/prices.json} and {@code stock.json}) so the owner can retune the economy and a server owner can
 * replace it with a datapack and {@code /reload}, with no rebuild. A single-file-per-kind listener is enough here: each file is one small table, the top-most data pack wins by
 * the normal resource rule, and the validation is the pure {@link StockSchema} and {@link PriceTable} readers. The result is two immutable snapshots held in volatile statics
 * (a cache of immutable data, replaced whole on a reload and cleared when the server stops), so the packet handlers read them without a lock and never see a half-loaded table.
 * A file that is missing or broken leaves the previous snapshot empty rather than throwing: the witch then has an empty shelf and buys nothing, and {@code /multiverse skillbook
 * shop status} prints every problem. Rows that name an item the game does not know (an addon that is not installed) are dropped here, where the item registry can be asked.
 */
public final class MarketData implements ResourceManagerReloadListener {
    private static final Logger LOG = LogUtils.getLogger();
    public static final ResourceLocation PRICES = ResourceLocation.fromNamespaceAndPath("nusmp", "black_market/prices.json");
    /** The witch's own file, kept from before shops had files of their own; it is the shelf of the shop {@value #WITCH_SHOP} unless {@code shops/witch.json} exists. */
    public static final ResourceLocation STOCK = ResourceLocation.fromNamespaceAndPath("nusmp", "black_market/stock.json");
    /** The shop types: type id to the tags, namespaces and item ids it sells. */
    public static final ResourceLocation TYPES = ResourceLocation.fromNamespaceAndPath("nusmp", "black_market/types.json");
    /** The folder with one file per shop, {@code shops/<shop_id>.json}. */
    public static final String SHOPS_DIR = "black_market/shops";
    /** The id of the Black Market witch's shop. */
    public static final String WITCH_SHOP = "witch";
    /** The most shop files read from the folder (a datapack cannot make the reload unbounded). */
    public static final int MAX_SHOPS = 16;

    /** One shop: its id, its type, and its valid lines in file order. */
    public record Shop(String id, String type, List<StockEntry> entries) {}

    private static volatile PriceTable prices = PriceTable.EMPTY;
    private static volatile Map<String, Shop> shops = Map.of();
    private static volatile ShopType.Parsed types = fallbackTypes();
    private static final java.util.concurrent.atomic.AtomicInteger GENERATION = new java.util.concurrent.atomic.AtomicInteger();
    private static volatile List<String> problems = List.of();
    private static volatile List<String> unknownItems = List.of();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<String> bad = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        PriceTable table = PriceTable.EMPTY;
        String priceText = read(manager, PRICES, bad);
        if (priceText != null) {
            PriceTable.Parsed p = PriceTable.parse(priceText);
            for (String s : p.problems()) bad.add("prices.json: " + s);
            table = p.table().filtered(id -> {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                boolean known = rl != null && BuiltInRegistries.ITEM.containsKey(rl);
                if (!known) unknown.add(id);
                return known;
            });
        }
        ShopType.Parsed ty = fallbackTypes();
        String typesText = read(manager, TYPES, bad);
        if (typesText != null) {
            ty = ShopType.parse(typesText);
            for (String p : ty.problems()) bad.add("types.json: " + p);
        }
        Map<String, Shop> found = new LinkedHashMap<>();
        readShopFiles(manager, ty, found, bad);
        if (!found.containsKey(WITCH_SHOP)) {                                      // the legacy file is the witch's shelf unless a shop file of her own exists
            String stockText = read(manager, STOCK, bad);
            if (stockText != null) {
                StockSchema.ShopFile f = StockSchema.parseShop(stockText, ty, null);
                for (String p : f.problems()) bad.add("stock.json: " + p);
                found.put(WITCH_SHOP, new Shop(WITCH_SHOP, f.type(), f.entries()));
            }
        }
        prices = table;
        shops = Collections.unmodifiableMap(found);
        types = ty;
        GENERATION.incrementAndGet();                                              // the pools of the ITEM lines are resolved again from the new data (see ShopPools)
        problems = List.copyOf(bad);
        unknownItems = List.copyOf(unknown);
        if (!bad.isEmpty()) LOG.warn("[nusmp] black market data: {} problem(s): {}", bad.size(), String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
        if (!unknown.isEmpty()) LOG.info("[nusmp] black market: {} priced item(s) are not in this game and were dropped: {}", unknown.size(), String.join(", ", unknown));
    }

    /** Reads {@code shops/<id>.json} of the mod's namespace, in name order, at most {@link #MAX_SHOPS}. */
    private static void readShopFiles(ResourceManager manager, ShopType.Parsed ty, Map<String, Shop> into, List<String> bad) {
        Map<String, Resource> files = new TreeMap<>();
        try {
            for (Map.Entry<ResourceLocation, Resource> e : manager.listResources(SHOPS_DIR, rl -> rl.getNamespace().equals("nusmp") && rl.getPath().endsWith(".json")).entrySet()) {
                files.put(e.getKey().getPath(), e.getValue());
            }
        } catch (RuntimeException e) {
            bad.add(SHOPS_DIR + " could not be listed: " + e);
            return;
        }
        int n = 0;
        for (Map.Entry<String, Resource> f : files.entrySet()) {
            String path = f.getKey();
            String id = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
            if (!id.matches("[a-z][a-z0-9_]{0,31}")) {
                bad.add("shops/" + id + ".json: the shop id (the file name) must be lower-case snake_case, skipped");
                continue;
            }
            if (++n > MAX_SHOPS) {
                bad.add("shops: only the first " + MAX_SHOPS + " shop files are read, the rest ignored");
                break;
            }
            String text;
            try (InputStream in = f.getValue().open()) {
                text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException | RuntimeException e) {
                bad.add("shops/" + id + ".json could not be read: " + e);
                continue;
            }
            StockSchema.ShopFile sf = StockSchema.parseShop(text, ty, null);
            for (String p : sf.problems()) bad.add("shops/" + id + ".json: " + p);
            into.put(id, new Shop(id, sf.type(), sf.entries()));
        }
    }

    private static ShopType.Parsed fallbackTypes() {
        return new ShopType.Parsed(Map.of(ShopType.DEFAULT, ShopType.fallbackGeneral()), List.of());
    }

    private static String read(ResourceManager manager, ResourceLocation file, List<String> bad) {
        try {
            Optional<Resource> r = manager.getResource(file);
            if (r.isEmpty()) {
                bad.add(file.getPath() + " is missing");
                return null;
            }
            try (InputStream in = r.get().open()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException e) {
            bad.add(file.getPath() + " could not be read: " + e);
            return null;
        }
    }

    /** What the witch pays for goods (an empty table before the first load). */
    public static PriceTable prices() { return prices; }

    /** The lines of the witch's shelf, in file order. */
    public static List<StockEntry> stock() {
        Shop s = shops.get(WITCH_SHOP);
        return s == null ? List.of() : s.entries();
    }

    /** The shop with this id, or null. */
    public static Shop shop(String id) { return id == null ? null : shops.get(id); }

    /** Every shop that loaded, by id. */
    public static Map<String, Shop> shops() { return shops; }

    /** The shop types that loaded (the general store is always among them). */
    public static ShopType.Parsed types() { return types; }

    /** Counts up whenever the data is reloaded or the world's tags are rebound; the pools of the ITEM lines are rebuilt when it changes. */
    public static int generation() { return GENERATION.get(); }

    /** Marks the cached pools stale (the world's item tags were rebound). */
    public static void invalidatePools() { GENERATION.incrementAndGet(); }

    /** Every problem found at the last load. */
    public static List<String> problems() { return problems; }

    /** The priced item ids that this game does not have (dropped from the table). */
    public static List<String> unknownItems() { return unknownItems; }

    /** The registry id of an item as a string ({@code minecraft:diamond}), the key of the price table. */
    public static String keyOf(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** The price row for a stack, or null when the witch does not buy that item (or the stack is empty). */
    public static PriceTable.Price priceOf(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : prices.get(keyOf(stack.getItem()));
    }

    /** Forgets everything (server stopped). */
    public static void clear() {
        prices = PriceTable.EMPTY;
        shops = Map.of();
        types = fallbackTypes();
        GENERATION.incrementAndGet();
        problems = List.of();
        unknownItems = List.of();
    }
}
