package com.newuniverse.nusmp.skilltree.shop;

import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcNpcEntity;
import com.newuniverse.nusmp.dungeon.DungeonFood;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.skilltree.SkillTreeApi;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: nothing of the shop can be played by its author, so the CI server smoke test runs two checks in the real game ({@code /multiverse mega selftest}, keys
 * {@code tree.shop.*}). {@code tree.shop.stock} proves the DATA is right where only the real game can tell: both files loaded through the real reload listener with no problem,
 * every {@code minecraft:} and {@code nusmp:} item the witch buys exists, the shelf built from the REAL route registry is deterministic, changes with the restock period, offers
 * every route its line promises, and every offer can be turned into a real item stack (a wrong factory name or an unknown SP kind shows here), and the flagship sits in the top
 * third of its band. {@code tree.shop.buy} proves the DEAL: a stand-in player and a real witch entity, a purchase refused when poor, when far, when the price moved and when the
 * offer is unknown (each leaving gold, stock and inventory untouched), a real purchase that moves exactly the right gold, stock and item, a sold-out refusal, and a sale, a bulk
 * sale and the refusal of a bulk sale of a valuable. Both pass with an empty route registry (and say so); the deal test passes with a message when the shop or the Skill Tree is switched off
 * in the config (a closed shop would refuse every step, which proves nothing), and ignores a daily purchase limit; both put the world back as they found it.
 */
public final class ShopSelfTests {
    private ShopSelfTests() {}

    private static final String PLAYER = "shop_tester";

    public static void register() {
        SelfTests.register("tree.shop.stock", ShopSelfTests::stock);
        SelfTests.register("tree.shop.buy", ShopSelfTests::buy);
    }

    private static SelfTests.Result ok(String detail) { return SelfTests.Result.ok(detail); }

    private static SelfTests.Result fail(String detail) { return SelfTests.Result.fail(detail); }

    // ================================================================ the data and the shelf
    private static SelfTests.Result stock(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        if (MarketData.stock().isEmpty()) bad.add("no shelf line loaded (data/nusmp/black_market/stock.json)");
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
        for (SkillRoute r : RouteRegistry.all()) {
            StockEntry line = lineFor(r.id());
            boolean always = line != null && line.weight() >= 100;
            boolean listed = false;
            for (StockBoard.Offer o : shelf) if (o.kind() == StockEntry.Kind.ROUTE_BOOK && o.ref().equals(r.id())) listed = true;
            if (always && !listed && MarketState.get(server).boughtView().isEmpty()) bad.add("the route " + r.id() + " has an always-stocked line but no offer");
        }
        StockBoard.Offer fang = StockBoard.find(StockBoard.build(seed, 11, 0, slots, id -> 0), "fang_of_smoldering_insanity");
        String flagship = "no Fire Dragon Slayer route in the registry yet";
        StockEntry fangLine = null;
        for (StockEntry e : MarketData.stock()) if (e.id().equals("fang_of_smoldering_insanity")) fangLine = e;
        if (fang != null && fangLine != null) {
            if (fang.price() < fangLine.bandStart()) bad.add("the flagship rolled " + fang.price() + ", below the top third of its band (" + (long) fangLine.bandStart() + ")");
            flagship = "the flagship rolls " + GoldFormat.full(fang.price()) + " at period 11";
        }
        if (!bad.isEmpty()) return fail(String.join(" | ", bad));
        return ok(MarketData.stock().size() + " shelf lines and " + MarketData.prices().size() + " prices load clean (" + foreign + " foreign item(s)), "
                + (RouteRegistry.all().isEmpty() ? "the route registry is empty (only SP books can be on the shelf), " : RouteRegistry.all().size() + " routes on the shelf, ")
                + shelf.size() + " offers now, every one can be made; " + flagship);
    }

    /** The data line that stocks a route: its own line, else the all-routes line. */
    private static StockEntry lineFor(String routeId) {
        StockEntry all = null;
        for (StockEntry e : MarketData.stock()) {
            if (e.kind() != StockEntry.Kind.ROUTE_BOOK) continue;
            if (!e.allRoutes() && e.route().equals(routeId)) return e;
            if (e.allRoutes()) all = e;
        }
        return all;
    }

    // ================================================================ the deal
    private static SelfTests.Result buy(MinecraftServer server) {
        if (!ShopConfig.enabled()) return ok("skipped: the Black Market is switched off in the config (nusmp-skilltree-shop-server.toml, enabled)");
        if (!SkillTreeApi.isEnabled()) return ok("skipped: the Skill Tree is switched off in the config, so the shop is closed with it");
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

            StockBoard.Offer offer = null;
            for (StockBoard.Offer o : ShopService.shelf(server)) if (o.kind() == StockEntry.Kind.SP_BOOK && o.left() > 0) { offer = o; break; }
            if (offer == null) return fail("no SP book is on the shelf (stock.json has none, or all are sold)");
            long price = ShopRules.buyPrice(offer.price(), ShopConfig.priceScale());
            int left = offer.left();
            ItemStack expected = ShopGoods.stack(offer);
            if (expected.isEmpty()) return fail("the SP book " + offer.ref() + " cannot be made (the items factory refused it)");
            String id = offer.id();

            // 1. poor: one gold short
            MarketGold.set(fp, price - 1);
            ShopService.Result r = ShopService.buy(fp, id, price);
            String why = untouched("poor", r, ShopRules.Reply.POOR, fp, price - 1, server, id, left, 0);
            if (why != null) return fail(why);

            // 2. the price moved: the player saw another price
            MarketGold.set(fp, price + 5_000_000);
            r = ShopService.buy(fp, id, price + 1);
            why = untouched("price moved", r, ShopRules.Reply.PRICE_MOVED, fp, price + 5_000_000, server, id, left, 0);
            if (why != null) return fail(why);

            // 3. no such offer
            r = ShopService.buy(fp, "no_such_offer", 1);
            why = untouched("unknown offer", r, ShopRules.Reply.UNKNOWN, fp, price + 5_000_000, server, id, left, 0);
            if (why != null) return fail(why);

            // 4. far from the witch: a purchase and a sale are both refused, and nothing moves
            fp.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
            fp.setPos(60.5, 100, 0.5);
            r = ShopService.buy(fp, id, price);
            why = untouched("far", r, ShopRules.Reply.FAR, fp, price + 5_000_000, server, id, left, 2);
            if (why != null) return fail(why);
            r = ShopService.sell(fp, 0, "minecraft:diamond", 1);
            why = untouched("far sale", r, ShopRules.Reply.FAR, fp, price + 5_000_000, server, id, left, 2);
            if (why != null) return fail(why);
            fp.setPos(0.5, 100, 0.5);
            fp.getInventory().clearContent();

            // 5. a real purchase moves exactly the gold, the stock and the item
            r = ShopService.buy(fp, id, price);
            if (r.reply() != ShopRules.Reply.BOUGHT) return fail("a purchase with enough gold, in reach, answered " + r.reply());
            if (MarketGold.get(fp) != 5_000_000) return fail("the purchase should leave 5,000,000 gold but left " + MarketGold.get(fp));
            if (stockLeft(server, id) != left - 1) return fail("the stock should drop from " + left + " to " + (left - 1) + " but is " + stockLeft(server, id));
            if (!carries(fp, expected)) return fail("the bought book is not in the inventory");

            // 6. sold out
            int guard = 0;
            MarketGold.set(fp, GoldLedger.mul(price, 100));
            while (stockLeft(server, id) > 0 && guard++ < 12) {
                MarketGold.clearCounter(fp);                                          // a daily limit in the config must not stop the test from emptying the shelf
                if (ShopService.buy(fp, id, price).reply() != ShopRules.Reply.BOUGHT) return fail("a purchase of a book still in stock was refused");
            }
            r = ShopService.buy(fp, id, price);
            if (r.reply() != ShopRules.Reply.SOLD_OUT) return fail("buying a sold-out book answered " + r.reply());

            // 7. selling: a diamond stack, then a bulk sale of meat, then the refusal of a bulk sale of a valuable
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
            return ok("poor, price-moved, unknown and far purchases changed nothing; a real purchase of " + offer.ref() + " for " + GoldFormat.full(price) + " moved the gold, the stock ("
                    + left + " to " + (left - 1) + ") and the item; sold out refused; 2 diamonds and 7 meat sold, the nether star kept");
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

    private static boolean carries(FakePlayer fp, ItemStack expected) {
        for (int i = 0; i < fp.getInventory().getContainerSize(); i++) {
            if (ItemStack.isSameItemSameComponents(fp.getInventory().getItem(i), expected)) return true;
        }
        return false;
    }
}
