package com.newuniverse.nusmp.skilltree.shop;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcNpcEntity;
import com.newuniverse.nusmp.arc.dialogue.DialogueBook;
import com.newuniverse.nusmp.arc.dialogue.DialogueContext;
import com.newuniverse.nusmp.arc.dialogue.DialogueLine;
import com.newuniverse.nusmp.arc.dialogue.DialogueLoader;
import com.newuniverse.nusmp.arc.story.ArcStory;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Why: this is the server's side of every deal, in one place, so that the rules a packet handler, a self-test and (later) a command all rely on are the SAME code. Each method
 * gathers the facts from the game (who is near the witch, what the shelf holds, what the player carries and owns), hands them to the pure {@link ShopRules} for the verdict, and
 * only then changes anything, in an order chosen so that a failure leaves the player whole: the item is handed over BEFORE the gold is taken (an error can lose a sale, never
 * gold), and a sale removes the items and adds the gold in one step with nothing that can fail between. Nothing here trusts the client: the witch's distance is measured by
 * looking for her in the world (no "session" state to forge or to leak), the price the player saw is compared with the price now, the item in a slot is compared with the item
 * the player named, and the amounts are clamped. The methods do not send packets (the packet handler does, through {@link #reply}); that separation keeps them callable from a
 * self-test with a stand-in player who has no connection. Server thread only.
 */
public final class ShopService {
    private ShopService() {}

    private static final Logger LOG = LogUtils.getLogger();

    /** The inventory slots a player can sell from: the hotbar and the main inventory. Armour, the off hand and anything equipped are never offered. */
    public static final int SELL_SLOTS = 36;

    /** What a deal came to: the verdict, the gold that changed hands and how many items. */
    public record Result(ShopRules.Reply reply, long gold, int count) {
        static Result of(ShopRules.Reply why) { return new Result(why, 0, 0); }
    }

    // ================================================================ who and where
    /** The nearest living witch within {@link ShopRules#REACH} blocks of the player, or null. */
    public static ArcNpcEntity witchNear(ServerPlayer p) {
        double r = ShopRules.REACH;
        AABB box = new AABB(p.getX() - r, p.getY() - r, p.getZ() - r, p.getX() + r, p.getY() + r, p.getZ() + r);        // from the player's own coordinates, not their bounding box (a stand-in's box can sit elsewhere)
        ArcNpcEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ArcNpcEntity n : p.level().getEntitiesOfClass(ArcNpcEntity.class, box, e -> e.isAlive() && BlackMarketWitch.ID.equals(e.characterId()))) {
            double d = n.distanceTo(p);
            if (d < bestDistance) {
                bestDistance = d;
                best = n;
            }
        }
        return best;
    }

    private static double distance(ServerPlayer p, ArcNpcEntity witch) {
        return witch == null ? Double.NaN : witch.distanceTo(p);
    }

    /**
     * May this player trade at all right now? The shop's own switch must be on AND the Skill Tree engine must be on: with the engine switched off in its config an unlock and an SP gain
     * both do nothing, so a book bought for millions would be a book that cannot be read; the shop stays closed instead of taking gold for it.
     */
    private static boolean mayTrade(ServerPlayer p) {
        return ShopConfig.enabled() && p.isAlive() && !p.isSpectator();
    }

    // ================================================================ the shelf
    /** The shelf of the witch right now: the data file's lines against the routes and items that exist, rolled for the current restock, minus what has been bought. Brings the world's market up to date first. */
    public static List<StockBoard.Offer> shelf(MinecraftServer server) {
        return shelf(server, MarketData.WITCH_SHOP);
    }

    /**
     * The shelf of the shop {@code shopId} right now (the slots come from {@link ShopPools}, built once per data reload; the roll is O(slots)). An ITEM offer the witch would buy back for as much
     * as it costs, at the prices really in force after the server's scales, is left off: no buy-and-sell-back loop can exist whatever the files and the config say.
     */
    public static List<StockBoard.Offer> shelf(MinecraftServer server, String shopId) {
        MarketState state = MarketState.get(server);
        long now = server.overworld().getGameTime();
        state.syncPeriod(StockBoard.period(now, ShopConfig.restockDays()));
        List<StockBoard.Slot> slots = ShopPools.slots(shopId);
        List<StockBoard.Offer> rolled = StockBoard.build(server.overworld().getSeed(), state.period(), state.epoch(), slots, state::bought);
        List<StockBoard.Offer> out = new ArrayList<>(rolled.size());
        PriceTable table = MarketData.prices();
        for (StockBoard.Offer o : rolled) {
            if (o.kind() == StockEntry.Kind.ITEM) {
                PriceTable.Price back = table.get(o.ref());
                if (back != null && ShopLoopGuard.loops(ShopRules.buyPrice(o.price(), ShopConfig.priceScale()), o.amount(), ShopRules.sellUnit(back.gold(), ShopConfig.sellScale()))) continue;
            }
            out.add(o);
        }
        return out;
    }

    /** The operator's restock: a new shelf at once. */
    public static void restock(MinecraftServer server) {
        MarketState.get(server).restock();
    }

    // ================================================================ the picture sent to the screen
    /** The whole picture of the shop for this player. {@code witch} may be null (nobody near: the screen closes). */
    public static ShopSnapshot snapshot(ServerPlayer p, ArcNpcEntity witch, Component voice) {
        MinecraftServer server = p.server;
        long now = server.overworld().getGameTime();
        double priceScale = ShopConfig.priceScale();
        List<ShopSnapshot.Offer> offers = new ArrayList<>();
        for (StockBoard.Offer o : shelf(server)) {
            ItemStack icon = ShopGoods.stack(o);
            if (icon.isEmpty() || offers.size() >= ShopSnapshot.MAX_OFFERS) continue;
            offers.add(new ShopSnapshot.Offer(o.id(), o.kind().ordinal(), o.ref(), icon, ShopRules.buyPrice(o.price(), priceScale), o.left(), o.total(), o.flagship()));
        }
        PriceTable table = MarketData.prices();
        List<ShopSnapshot.Sell> sells = new ArrayList<>();
        List<String> bulk = new ArrayList<>();
        Inventory inv = p.getInventory();
        for (int slot = 0; slot < SELL_SLOTS && sells.size() < ShopSnapshot.MAX_SELLS; slot++) {
            ItemStack st = inv.getItem(slot);
            PriceTable.Price price = MarketData.priceOf(st);
            if (price == null) continue;
            long unit = ShopRules.sellUnit(price.gold(), ShopConfig.sellScale());
            if (unit <= 0) continue;
            sells.add(new ShopSnapshot.Sell(slot, st.copy(), unit, price.category()));
            if (table.isBulk(price.category()) && !bulk.contains(price.category()) && bulk.size() < ShopSnapshot.MAX_BULK) bulk.add(price.category());
        }
        int left = ShopRules.dailyLeft(ShopConfig.maxBuyPerDay(), MarketGold.boughtToday(p, StockBoard.dayOf(now)));
        return new ShopSnapshot(witch == null ? -1 : witch.getId(), MarketGold.get(p), StockBoard.ticksUntilRestock(now, ShopConfig.restockDays()), left, offers, sells, bulk, voice);
    }

    /** One of the witch's lines for a topic, as the words of a chat component (no name in front: the screen has her portrait). Never null. */
    static Component voice(ServerPlayer p, ArcNpcEntity witch, DialogueContext topic) {
        try {
            ArcCharacter c = BlackMarketWitch.CHARACTER;
            DialogueBook book = DialogueLoader.bookOrFallback(c.id());
            DialogueBook.Pick pick = book.pick(topic, ArcStory.situation(p, c), witch == null ? -1 : witch.lastLine(topic), n -> ThreadLocalRandom.current().nextInt(n));
            if (pick == null) return Component.translatable("gui.nusmp.shop.welcome");
            if (witch != null) witch.rememberLine(topic, pick.index());
            DialogueLine line = pick.line();
            String player = p.getGameProfile().getName();
            String saga = ArcStory.sagaOf(p).title();
            return line.key().isEmpty() ? Component.literal(line.plain(player, c.name(), saga))
                    : Component.translatableWithFallback(line.key(), line.template(), player, c.name(), saga);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] the witch could not find her words: {}", e.toString());
            return Component.translatable("gui.nusmp.shop.welcome");
        }
    }

    // ================================================================ opening
    /** Opens the shop for the player: the witch turns to them and the screen opens with the first picture. False when the shop cannot open. */
    public static boolean open(ServerPlayer p, ArcNpcEntity witch) {
        if (witch == null || !witch.isAlive() || !mayTrade(p)) return false;
        witch.beginTalk(p);
        PacketDistributor.sendToPlayer(p, new OpenShopPayload(snapshot(p, witch, voice(p, witch, DialogueContext.SHOP))));
        noise(witch, SoundEvents.WITCH_AMBIENT, 0.9f);
        return true;
    }

    /** Answers a packet: the new picture with the verdict, and the witch's noise for it. Never throws (a late or failed send is not an error of the deal). */
    public static void reply(ServerPlayer p, Result r) {
        try {
            ArcNpcEntity witch = witchNear(p);
            if (witch != null) witch.beginTalk(p);                                  // she keeps facing the customer; her parting mutter comes a few seconds after the LAST deal, not after the first click
            ShopSnapshot snap = snapshot(p, witch, Component.empty());
            PacketDistributor.sendToPlayer(p, new ShopSyncPayload(snap, r.reply().ordinal(), p.getRandom().nextInt(256)));
            cue(witch, r.reply());
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] black market: could not answer {}: {}", p.getGameProfile().getName(), e.toString());
        }
    }

    // ================================================================ buying
    /** Buys one copy of an offer. {@code expectedPrice} is the price on the player's screen. */
    public static Result buy(ServerPlayer p, String offerId, long expectedPrice) {
        try {
            MinecraftServer server = p.server;
            ArcNpcEntity witch = witchNear(p);
            StockBoard.Offer offer = StockBoard.find(shelf(server), offerId);
            long today = StockBoard.dayOf(server.overworld().getGameTime());
            long price = offer == null ? 0 : ShopRules.buyPrice(offer.price(), ShopConfig.priceScale());
            ShopRules.BuyCase facts = new ShopRules.BuyCase(mayTrade(p), distance(p, witch), offer != null, offer == null ? 0 : offer.left(), price, expectedPrice,
                    MarketGold.get(p), ShopConfig.maxBuyPerDay(), MarketGold.boughtToday(p, today));
            ShopRules.Reply verdict = ShopRules.judgeBuy(facts);
            if (verdict != ShopRules.Reply.BOUGHT) return Result.of(verdict);
            ItemStack stack = ShopGoods.stack(offer);
            if (stack.isEmpty()) return Result.of(ShopRules.Reply.FAILED);         // a book that cannot be made is never charged for
            ItemHandlerHelper.giveItemToPlayer(p, stack);                         // the item first: whatever goes wrong after this, the player is never out of gold AND out of the book
            MarketGold.take(p, price);
            MarketState.get(server).addBought(offer.id(), 1);
            MarketGold.recordPurchase(p, today);
            syncTree(p);
            NusmpEvents.fire(p, "tree.shop.buy", offer.id());                     // never throws (the quest door swallows its own errors)
            return new Result(ShopRules.Reply.BOUGHT, price, 1);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] black market: a purchase by {} failed: {}", p.getGameProfile().getName(), e.toString());
            return Result.of(ShopRules.Reply.FAILED);
        }
    }

    // ================================================================ selling
    /** Sells up to {@code want} items from one inventory slot, if the slot still holds the item the player named. */
    public static Result sell(ServerPlayer p, int slot, String itemId, int want) {
        try {
            ArcNpcEntity witch = witchNear(p);
            if (slot < 0 || slot >= SELL_SLOTS) return Result.of(ShopRules.Reply.NOTHING);
            ItemStack stack = p.getInventory().getItem(slot);
            PriceTable.Price price = MarketData.priceOf(stack);
            if (price == null || itemId == null || !itemId.equals(MarketData.keyOf(stack.getItem()))) return Result.of(ShopRules.Reply.NOTHING);
            long unit = ShopRules.sellUnit(price.gold(), ShopConfig.sellScale());
            ShopRules.Sale sale = ShopRules.judgeSell(mayTrade(p), distance(p, witch), unit, stack.getCount(), want, MarketGold.get(p));
            if (!sale.reply().ok()) return Result.of(sale.reply());
            stack.shrink(sale.count());
            MarketGold.add(p, sale.gold());
            syncTree(p);
            return new Result(ShopRules.Reply.SOLD, sale.gold(), sale.count());
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] black market: a sale by {} failed: {}", p.getGameProfile().getName(), e.toString());
            return Result.of(ShopRules.Reply.FAILED);
        }
    }

    /** Sells every stack of a bulk category the player carries. */
    public static Result sellAll(ServerPlayer p, String category) {
        try {
            ArcNpcEntity witch = witchNear(p);
            double dist = distance(p, witch);
            if (!mayTrade(p)) return Result.of(ShopRules.Reply.CLOSED);
            if (!(dist <= ShopRules.REACH)) return Result.of(ShopRules.Reply.FAR);
            if (category == null || !MarketData.prices().isBulk(category)) return Result.of(ShopRules.Reply.NOTHING);
            long purse = MarketGold.get(p);
            long running = purse;
            long gained = 0;
            int items = 0;
            ShopRules.Reply why = ShopRules.Reply.NOTHING;
            Inventory inv = p.getInventory();
            for (int slot = 0; slot < SELL_SLOTS; slot++) {
                ItemStack stack = inv.getItem(slot);
                PriceTable.Price price = MarketData.priceOf(stack);
                if (price == null || !category.equals(price.category())) continue;
                long unit = ShopRules.sellUnit(price.gold(), ShopConfig.sellScale());
                ShopRules.Sale sale = ShopRules.judgeSell(true, dist, unit, stack.getCount(), stack.getCount(), running);
                if (sale.reply() == ShopRules.Reply.FULL_PURSE) {
                    why = sale.reply();
                    break;
                }
                if (!sale.reply().ok()) continue;
                stack.shrink(sale.count());
                running = GoldLedger.add(running, sale.gold());
                gained += sale.gold();
                items += sale.count();
            }
            if (items == 0) return Result.of(why);
            MarketGold.set(p, running);
            syncTree(p);
            return new Result(ShopRules.Reply.SOLD, gained, items);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] black market: a bulk sale by {} failed: {}", p.getGameProfile().getName(), e.toString());
            return Result.of(ShopRules.Reply.FAILED);
        }
    }

    /**
     * Kept as the single place a deal calls after the gold changed. Since 1.96.0 the purse is read from the snapshot the shop sends with every reply ({@link ShopSnapshot}), so there is no
     * separate record to push to the client any more and nothing to do here.
     */
    static void syncTree(ServerPlayer p) {
        // the gold travels in the shop snapshot; the old Skill Tree record that carried it to the client is gone
    }

    // ================================================================ noises
    private static void cue(ArcNpcEntity witch, ShopRules.Reply r) {
        if (witch == null) return;
        switch (r) {
            case BOUGHT -> noise(witch, SoundEvents.WITCH_CELEBRATE, 1.0f);
            case SOLD -> noise(witch, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.5f);
            case POOR, SOLD_OUT, PRICE_MOVED, LIMIT, NOTHING, FULL_PURSE, UNKNOWN, FAILED -> noise(witch, SoundEvents.WITCH_AMBIENT, 0.7f);
            default -> { }
        }
    }

    private static void noise(ArcNpcEntity witch, SoundEvent sound, float pitch) {
        witch.level().playSound(null, witch.getX(), witch.getY(), witch.getZ(), sound, SoundSource.NEUTRAL, 1.0f, pitch);
    }
}
