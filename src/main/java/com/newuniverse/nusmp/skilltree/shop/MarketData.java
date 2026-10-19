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
import java.util.List;
import java.util.Optional;

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
    public static final ResourceLocation STOCK = ResourceLocation.fromNamespaceAndPath("nusmp", "black_market/stock.json");

    private static volatile PriceTable prices = PriceTable.EMPTY;
    private static volatile List<StockEntry> stock = List.of();
    private static volatile List<String> problems = List.of();
    private static volatile List<String> unknownItems = List.of();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        List<String> bad = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        PriceTable table = PriceTable.EMPTY;
        List<StockEntry> lines = List.of();
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
        String stockText = read(manager, STOCK, bad);
        if (stockText != null) {
            StockSchema.Parsed s = StockSchema.parse(stockText);
            for (String p : s.problems()) bad.add("stock.json: " + p);
            lines = s.entries();
        }
        prices = table;
        stock = lines;
        problems = List.copyOf(bad);
        unknownItems = List.copyOf(unknown);
        if (!bad.isEmpty()) LOG.warn("[nusmp] black market data: {} problem(s): {}", bad.size(), String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
        if (!unknown.isEmpty()) LOG.info("[nusmp] black market: {} priced item(s) are not in this game and were dropped: {}", unknown.size(), String.join(", ", unknown));
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

    /** The lines of the shelf, in file order. */
    public static List<StockEntry> stock() { return stock; }

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
        stock = List.of();
        problems = List.of();
        unknownItems = List.of();
    }
}
