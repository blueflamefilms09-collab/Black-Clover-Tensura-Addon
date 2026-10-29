package com.newuniverse.nusmp.skilltree.shop;

import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcNpcEntity;
import com.newuniverse.nusmp.dungeon.DungeonFood;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: nothing of the shop can be played by its author, so the CI server smoke test runs checks in the real game ({@code /multiverse mega selftest}, keys
 * {@code skillbook.shop.*}; {@code items} and {@code buy_item} cover the item goods and the shop types, see {@code docs/black_market_goods.md}). {@code skillbook.shop.stock} proves the DATA is right where only the real game can tell: both files loaded through the real reload listener with no problem,
 * every {@code minecraft:} and {@code nusmp:} item the witch buys exists, and the shelf is deterministic and changes with the restock period (the Skill Books were removed, so the shelf is empty and
 * {@code stock.json} lists no line; any line a datapack adds must still make a real item). {@code skillbook.shop.buy} proves the DEAL with a stand-in player and a real witch entity: a sale refused when far,
 * a purchase of an unknown offer refused, a sale, a bulk sale and the refusal of a bulk sale of a valuable. It passes with a message when the shop is switched off in the config (a closed shop would refuse every
 * step, which proves nothing), and puts the world back as it found it.
 */
public final class ShopSelfTests {
    private ShopSelfTests() {}

    private static final String PLAYER = "shop_tester";

    public static void register() {
        SelfTests.register("skillbook.shop.stock", ShopSelfTests::stock);
        SelfTests.register("skillbook.shop.buy", ShopSelfTests::buy);
        SelfTests.register("skillbook.shop.items", ShopSelfTests::items);
        SelfTests.register("skillbook.shop.buy_item", ShopSelfTests::buyItem);
    }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    // ================================================================ the data and the shelf
    private static SelfTests.Result stock(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        if (MarketData.prices().isEmpty()) bad.add("no price loaded (data/nusmp/black_market/prices.json)");
        if (!MarketData.problems().isEmpty()) bad.add("data problems: " + String.join("; ", MarketData.problems().subList(0, Math.min(4, MarketData.problems().size()))));
        int foreign = 0;
        for (String id : MarketData.unknownItems()) {
            if (id.startsWith("minecraft:") || id.startsWith("nusmp:")) bad.add("the price table names " + id + ", which is not an item");
            else foreign++;
        }
        if (!bad.isEmpty()) return fail(String.join(" | ", bad));

        long seed = server.overworld().getSeed();
        List<StockBoard.Slot> slots = StockBoard.slots(MarketData.stock(), ShopGoods.routeIds());
        List<StockBoard.Offer> a = StockBoard.build(seed, 7, 0, slots, id -> 0);
        List<StockBoard.Offer> b = StockBoard.build(seed, 7, 0, slots, id -> 0);
        if (!a.equals(b)) return fail("the same inputs gave two different shelves");
        List<StockBoard.Offer> next = StockBoard.build(seed, 8, 0, slots, id -> 0);
        if (a.size() > 1 && a.equals(next)) return fail("a new restock period gave the very same shelf (prices never move)");
        for (StockBoard.Offer o : ShopService.shelf(server)) {
            if (ShopGoods.stack(o).isEmpty()) bad.add("the offer " + o.id() + " (" + o.kind().id() + " " + o.ref() + ") cannot be made into an item");
            if (o.price() < 1 || o.left() > o.total()) bad.add("the offer " + o.id() + " has price " + o.price() + " and " + o.left() + "/" + o.total() + " left");
        }
        List<StockBoard.Offer> shelf = ShopService.shelf(server);
        if (!bad.isEmpty()) return fail(String.join(" | ", bad));
        return ok(MarketData.stock().size() + " shelf lines and " + MarketData.prices().size() + " prices load clean (" + foreign + " foreign item(s)), "
                + shelf.size() + " offers now, every one can be made");
    }

    // ================================================================ the item pools of the witch's shelf
    /**
     * {@code skillbook.shop.items}: the witch's ITEM lines in the real game. Every line that names a {@code minecraft:}, {@code c:} or {@code nusmp:} tag or item must resolve to a non-empty pool (a typo
     * there is a bug; a line that names another mod's item may be empty when that mod is absent); every pool item exists, is allowed by the shop's type, is not banned or excluded and is sold for
     * more than the witch pays for it; the shelf is deterministic, changes with the restock period, never lists an item twice, and every offer can be made into a real stack within the item's stack size.
     */
    private static SelfTests.Result items(MinecraftServer server) {
        MarketData.Shop shop = MarketData.shop(MarketData.WITCH_SHOP);
        if (shop == null) return fail("no shop 'witch' loaded (data/nusmp/black_market/stock.json)");
        ShopType type = MarketData.types().get(shop.type());
        if (type == null) return fail("the witch's shop type '" + shop.type() + "' is not in types.json");
        if (!"general".equals(type.id())) return fail("the witch is the general store but her file says type '" + type.id() + "'");
        List<String> bad = new ArrayList<>();
        int lines = 0;
        int pooled = 0;
        int foreignEmpty = 0;
        int items = 0;
        for (StockEntry e : shop.entries()) {
            if (e.kind() != StockEntry.Kind.ITEM) continue;
            lines++;
            List<StockBoard.PoolItem> pool = ShopPools.pool(shop.id(), e);
            String id = e.itemOrTagId();
            String ns = id.substring(0, Math.max(0, id.indexOf(':')));
            if (pool.isEmpty()) {
                if (ns.equals("minecraft") || ns.equals("c") || ns.equals("nusmp")) bad.add(e.id() + ": " + e.item() + " gave an empty pool in this game");
                else foreignEmpty++;
                continue;
            }
            pooled++;
            if (pool.size() > ShopPools.MAX_POOL) bad.add(e.id() + ": the pool holds " + pool.size() + " items, above the cap");
            for (StockBoard.PoolItem p : pool) {
                items++;
                ResourceLocation rl = ResourceLocation.tryParse(p.id());
                if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) bad.add(e.id() + ": pool item " + p.id() + " is not an item");
                if (type.isBanned(p.id()) || e.exclude().contains(p.id())) bad.add(e.id() + ": pool item " + p.id() + " is banned or excluded");
                if (p.maxStack() < 1) bad.add(e.id() + ": " + p.id() + " has no stack size");
                PriceTable.Price buy = MarketData.prices().get(p.id());
                if (buy != null && e.priceMin() <= buy.gold()) bad.add(e.id() + ": " + p.id() + " would be sold at " + e.priceMin() + " but the witch pays " + buy.gold());
            }
        }
        if (lines == 0) bad.add("the witch's file has no item line");
        if (!bad.isEmpty()) return fail(String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));

        long seed = server.overworld().getSeed();
        List<StockBoard.Slot> slots = ShopPools.slots(shop.id());
        if (slots.size() < 8) return fail("only " + slots.size() + " of " + lines + " lines made a slot");
        if (slots.size() > StockBoard.MAX_SLOTS) return fail("the shelf has " + slots.size() + " slots, above the cap");
        List<StockBoard.Offer> a = StockBoard.build(seed, 7, 0, slots, id -> 0);
        if (!a.equals(StockBoard.build(seed, 7, 0, slots, id -> 0))) return fail("the same inputs gave two different shelves");
        boolean moves = false;
        for (int period = 8; period < 40 && !moves; period++) moves = !a.equals(StockBoard.build(seed, period, 0, slots, id -> 0));
        if (!moves) return fail("40 restock periods never changed the shelf");
        Set<String> refs = new HashSet<>();
        List<StockBoard.Offer> shelf = ShopService.shelf(server);
        for (StockBoard.Offer o : shelf) {
            ItemStack st = ShopGoods.stack(o);
            if (st.isEmpty()) bad.add("offer " + o.id() + " (" + o.ref() + ") cannot be made into an item");
            else if (st.getCount() != o.amount() || st.getCount() > st.getMaxStackSize()) bad.add("offer " + o.id() + ": " + st.getCount() + " of " + o.ref() + " is not a legal stack");
            if (!refs.add(o.ref())) bad.add("the item " + o.ref() + " is on the shelf twice");
            if (o.kind() != StockEntry.Kind.ITEM) bad.add("offer " + o.id() + " is not an item offer");
            PriceTable.Price back = MarketData.prices().get(o.ref());
            if (back != null && ShopLoopGuard.loops(ShopRules.buyPrice(o.price(), ShopConfig.priceScale()), o.amount(), ShopRules.sellUnit(back.gold(), ShopConfig.sellScale()))) {
                bad.add("offer " + o.id() + ": " + o.ref() + " could be sold back for a profit");
            }
        }
        if (!bad.isEmpty()) return fail(String.join(" | ", bad.subList(0, Math.min(5, bad.size()))));
        return ok(lines + " item lines, " + pooled + " pools (" + items + " items, " + foreignEmpty + " foreign line(s) empty), " + slots.size() + " slots, " + shelf.size() + " offers now, no loop, shelf deterministic and moving");
    }

    /**
     * {@code skillbook.shop.buy_item}: a purchase of an ITEM offer end to end with a stand-in player and a real witch: a price that moved is refused and changes nothing, a poor player is refused, then the
     * purchase hands over exactly the offer's stack, takes exactly its price, and counts one copy as bought; the same offer cannot be bought past its stock. Puts the world back as it found it.
     */
    private static SelfTests.Result buyItem(MinecraftServer server) {
        if (!ShopConfig.enabled()) return ok("skipped: the Black Market is switched off in the config (nusmp-skilltree-shop-server.toml, enabled)");
        ServerLevel level = server.overworld();
        FakePlayer fp = SelfTests.standIn(level, PLAYER, 0.5, 100, 0.5);
        ArcNpcEntity witch = ArcEntities.NPC.get().create(level);
        if (witch == null) return fail("the NPC entity could not be created");
        MarketState state = MarketState.get(server);
        Map<String, Integer> keptBought = state.boughtView();
        long keptGold = MarketGold.get(fp);
        try {
            witch.moveTo(2.5, 100, 0.5, 0f, 0f);
            witch.setCharacter(BlackMarketWitch.ID);
            if (!level.addFreshEntity(witch)) return fail("the witch could not be placed in the world");
            fp.getInventory().clearContent();
            MarketGold.clearCounter(fp);
            state.replaceBought(Map.of());
            StockBoard.Offer offer = null;
            for (StockBoard.Offer o : ShopService.shelf(server)) {
                if (o.kind() == StockEntry.Kind.ITEM && o.left() > 0 && !ShopGoods.stack(o).isEmpty()) {
                    offer = o;
                    break;
                }
            }
            if (offer == null) return fail("the shelf has no item offer to buy");
            long price = ShopRules.buyPrice(offer.price(), ShopConfig.priceScale());
            ItemStack want = ShopGoods.stack(offer);

            MarketGold.set(fp, price + 7);
            ShopService.Result r = ShopService.buy(fp, offer.id(), price + 1);                   // the price on the player's screen is not the price now
            if (r.reply() != ShopRules.Reply.PRICE_MOVED || MarketGold.get(fp) != price + 7 || carried(fp) != 0) return fail("a purchase at a moved price answered " + r.reply() + " and changed something");
            MarketGold.set(fp, price - 1);
            r = ShopService.buy(fp, offer.id(), price);
            if (r.reply() != ShopRules.Reply.POOR || MarketGold.get(fp) != price - 1 || carried(fp) != 0) return fail("a purchase with too little gold answered " + r.reply() + " and changed something");

            MarketGold.set(fp, price + 7);
            r = ShopService.buy(fp, offer.id(), price);
            if (r.reply() != ShopRules.Reply.BOUGHT) return fail("the purchase answered " + r.reply());
            if (MarketGold.get(fp) != 7) return fail("the purchase left " + MarketGold.get(fp) + " gold, expected 7");
            int got = 0;
            for (int i = 0; i < fp.getInventory().getContainerSize(); i++) {
                ItemStack st = fp.getInventory().getItem(i);
                if (st.isEmpty()) continue;
                if (!ItemStack.isSameItem(st, want)) return fail("the purchase put " + st + " in the inventory instead of " + want.getItem());
                got += st.getCount();
            }
            if (got != want.getCount()) return fail("the purchase handed over " + got + " items instead of " + want.getCount());
            if (state.bought(offer.id()) != 1) return fail("the purchase was counted " + state.bought(offer.id()) + " times");

            // buy the rest of the stock, then one more: sold out, and nothing is charged
            for (int i = 1; i < offer.total(); i++) {
                MarketGold.set(fp, price);
                if (ShopService.buy(fp, offer.id(), price).reply() != ShopRules.Reply.BOUGHT) return fail("copy " + (i + 1) + " of " + offer.total() + " was refused");
            }
            MarketGold.set(fp, price * 2);
            r = ShopService.buy(fp, offer.id(), price);
            if (r.reply() != ShopRules.Reply.SOLD_OUT || MarketGold.get(fp) != price * 2) return fail("a purchase past the stock answered " + r.reply() + " and left " + MarketGold.get(fp) + " gold");
            return ok("bought " + want.getCount() + " x " + offer.ref() + " for " + price + " gold; a moved price, a poor purse and a sold-out shelf changed nothing");
        } finally {
            witch.discard();
            fp.setPos(0.5, 100, 0.5);
            fp.getInventory().clearContent();
            MarketGold.set(fp, keptGold);
            MarketGold.clearCounter(fp);
            state.replaceBought(keptBought);
        }
    }

    private static int carried(FakePlayer fp) {
        int items = 0;
        for (int i = 0; i < fp.getInventory().getContainerSize(); i++) items += fp.getInventory().getItem(i).getCount();
        return items;
    }

    // ================================================================ the deal
    private static SelfTests.Result buy(MinecraftServer server) {
        if (!ShopConfig.enabled()) return ok("skipped: the Black Market is switched off in the config (nusmp-skilltree-shop-server.toml, enabled)");
        ServerLevel level = server.overworld();
        FakePlayer fp = SelfTests.standIn(level, PLAYER, 0.5, 100, 0.5);
        ArcNpcEntity witch = ArcEntities.NPC.get().create(level);
        if (witch == null) return fail("the NPC entity could not be created");
        MarketState state = MarketState.get(server);
        Map<String, Integer> keptBought = state.boughtView();
        long keptGold = MarketGold.get(fp);
        try {
            witch.moveTo(2.5, 100, 0.5, 0f, 0f);
            witch.setCharacter(BlackMarketWitch.ID);
            if (!level.addFreshEntity(witch)) return fail("the witch could not be placed in the world");
            fp.getInventory().clearContent();
            MarketGold.clearCounter(fp);

            // 1. no such offer (the shelf holds no book any more, so nothing is for sale): refused, nothing moves
            MarketGold.set(fp, 5_000_000);
            ShopService.Result r = ShopService.buy(fp, "no_such_offer", 1);
            String why = untouched("unknown offer", r, ShopRules.Reply.UNKNOWN, fp, 5_000_000, server, "no_such_offer", -1, 0);
            if (why != null) return fail(why);

            // 2. far from the witch: a sale is refused, and nothing moves
            fp.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
            fp.setPos(60.5, 100, 0.5);
            r = ShopService.sell(fp, 0, "minecraft:diamond", 1);
            why = untouched("far sale", r, ShopRules.Reply.FAR, fp, 5_000_000, server, "no_such_offer", -1, 2);
            if (why != null) return fail(why);
            fp.setPos(0.5, 100, 0.5);
            fp.getInventory().clearContent();

            // 3. selling: a diamond stack, then a bulk sale of meat, then the refusal of a bulk sale of a valuable
            fp.getInventory().clearContent();
            MarketGold.set(fp, 0);
            PriceTable table = MarketData.prices();
            PriceTable.Price diamond = table.get("minecraft:diamond");
            PriceTable.Price meat = table.get("nusmp:raw_monster_meat");
            if (diamond == null || meat == null) return fail("the price table has no diamond or no raw monster meat");
            long diamondUnit = ShopRules.sellUnit(diamond.gold(), ShopConfig.sellScale());
            long meatUnit = ShopRules.sellUnit(meat.gold(), ShopConfig.sellScale());
            if (diamondUnit <= 0 || meatUnit <= 0) return fail("sellScale makes the test goods worthless, the sale cannot be tested");
            fp.getInventory().setItem(3, new ItemStack(Items.DIAMOND, 3));
            r = ShopService.sell(fp, 3, "minecraft:diamond", 2);
            if (r.reply() != ShopRules.Reply.SOLD || r.count() != 2 || MarketGold.get(fp) != 2 * diamondUnit) return fail("selling 2 diamonds gave " + r + " and left " + MarketGold.get(fp) + " gold");
            if (fp.getInventory().getItem(3).getCount() != 1) return fail("selling 2 of 3 diamonds left " + fp.getInventory().getItem(3).getCount());
            r = ShopService.sell(fp, 3, "minecraft:emerald", 1);
            if (r.reply() != ShopRules.Reply.NOTHING) return fail("selling with the wrong item named answered " + r.reply());
            r = ShopService.sell(fp, 3, "minecraft:diamond", 99);
            if (r.count() != 1 || fp.getInventory().getItem(3).getCount() != 0) return fail("selling more than the stack holds should sell the last diamond, got " + r);
            r = ShopService.sell(fp, 3, "minecraft:diamond", 1);
            if (r.reply() != ShopRules.Reply.NOTHING) return fail("selling from an empty slot answered " + r.reply());
            r = ShopService.sell(fp, 40, "minecraft:diamond", 1);
            if (r.reply() != ShopRules.Reply.NOTHING) return fail("selling from an armour slot answered " + r.reply());

            long before = MarketGold.get(fp);
            fp.getInventory().setItem(5, new ItemStack(DungeonFood.RAW_MONSTER_MEAT.get(), 4));
            fp.getInventory().setItem(6, new ItemStack(DungeonFood.RAW_MONSTER_MEAT.get(), 3));
            fp.getInventory().setItem(7, new ItemStack(Items.NETHER_STAR, 1));
            r = ShopService.sellAll(fp, meat.category());
            if (r.reply() != ShopRules.Reply.SOLD || r.count() != 7 || MarketGold.get(fp) != before + 7 * meatUnit) return fail("selling all meat gave " + r + " and left " + MarketGold.get(fp) + " gold");
            r = ShopService.sellAll(fp, table.get("minecraft:nether_star").category());
            if (r.reply() != ShopRules.Reply.NOTHING || fp.getInventory().getItem(7).isEmpty()) return fail("a bulk sale of treasure must be refused and the nether star kept, got " + r);
            return ok("an unknown purchase and a far sale changed nothing; 2 diamonds and 7 meat sold, the nether star kept");
        } finally {
            witch.discard();
            fp.setPos(0.5, 100, 0.5);
            fp.getInventory().clearContent();
            MarketGold.set(fp, keptGold);
            MarketGold.clearCounter(fp);
            state.replaceBought(keptBought);
        }
    }

    /** Checks a refused deal: the right reason, and gold, stock and inventory exactly as before. Returns the complaint, or null when all is well. */
    private static String untouched(String what, ShopService.Result r, ShopRules.Reply want, FakePlayer fp, long gold, MinecraftServer server, String id, int left, int carried) {
        if (r.reply() != want) return what + ": expected " + want + " but the witch answered " + r.reply();
        if (MarketGold.get(fp) != gold) return what + ": the gold changed to " + MarketGold.get(fp);
        if (stockLeft(server, id) != left) return what + ": the stock changed to " + stockLeft(server, id);
        int items = 0;
        for (int i = 0; i < fp.getInventory().getContainerSize(); i++) items += fp.getInventory().getItem(i).getCount();
        if (items != carried) return what + ": the inventory holds " + items + " items instead of " + carried;
        return null;
    }

    private static int stockLeft(MinecraftServer server, String id) {
        StockBoard.Offer o = StockBoard.find(ShopService.shelf(server), id);
        return o == null ? -1 : o.left();
    }
}
