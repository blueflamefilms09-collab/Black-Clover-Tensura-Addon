package com.newuniverse.nusmp.elite.hub;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Merchant is the one place coins turn into goods, so the purchase is decided entirely on the SERVER, from nothing the client says except the id of the offer: the price comes from the
 * shop table (times the server's {@code shop.priceScale}), the stock is the player's own counter for today, the floor lock is their saved deepest floor, and the money is the {@link Currency}
 * seam's (coins by default, Magic Stones in the old mode). The sender must have a {@link HubMenus.ShopMenu} open next to a living Merchant. The order is: check ({@link ShopRules#decide}), charge,
 * give the goods (into the inventory, or onto the ground at the player's feet when it is full, so a purchase never fails after the coins left), count it, and send the screen a fresh whole
 * picture whatever the verdict, so it can never keep showing something the server just refused. WHY the stock is per player and per world day: the Hub is shared, and a global stock would let one
 * player buy out the Dungeon Map for everybody; a per-player counter needs no shared state to save or sync, resets with the world's day count like the daily quests, and is the simplest rule
 * a player can predict.
 */
public final class ShopService {
    private ShopService() {}

    public static boolean on() { return HubConfig.enabled() && HubConfig.get(HubConfig.SHOP_ENABLED); }

    /** A purchase request of the open shop screen. */
    static void buy(ServerPlayer sp, String offerId) {
        if (!(sp.containerMenu instanceof HubMenus.ShopMenu menu) || !menu.stillValid(sp)) return;
        purchase(sp, offerId);
        sendState(sp);
    }

    /** Buys one stack of an offer for the player; returns the verdict (the player is told). Used by the screen's packet and by the self-test. */
    public static ShopRules.Verdict purchase(ServerPlayer sp, String offerId) {
        PlayerQuestData d = QuestService.fresh(sp);
        long day = QuestService.today(sp.server);
        ShopTable.Offer offer = HubCatalogs.shop().get(offerId);
        double scale = HubConfig.get(HubConfig.PRICE_SCALE);
        ShopRules.Verdict v = ShopRules.decide(on(), offer, offer == null ? 0 : d.boughtToday(offer.id(), day), d.maxFloor(), Currency.balance(sp), scale);
        if (v != ShopRules.Verdict.OK) {
            tell(sp, v, offer);
            return v;
        }
        Item item = QuestService.item(offer.item());
        long price = ShopRules.price(offer, scale);
        if (item == null || !Currency.charge(sp, price)) {
            tell(sp, ShopRules.Verdict.CANNOT_AFFORD, offer);
            return ShopRules.Verdict.CANNOT_AFFORD;
        }
        ItemStack goods = new ItemStack(item, offer.count());
        if (!sp.getInventory().add(goods) && !goods.isEmpty()) sp.drop(goods, false);
        d.recordPurchase(offer.id(), day, 1);
        QuestService.tell(sp, "message.nusmp.hub.shop.bought", "Bought %s x%s for %s.", new ItemStack(item).getHoverName(), offer.count(), CoinMath.describe(price));
        com.newuniverse.nusmp.audio.voice.Voice.npc(sp, com.newuniverse.nusmp.audio.voice.VoicePicker.NpcAct.BUY, true);               // 1.110.0: spoken line
        return v;
    }

    private static void tell(ServerPlayer sp, ShopRules.Verdict v, ShopTable.Offer offer) {
        switch (v) {
            case SHOP_OFF -> QuestService.tell(sp, "message.nusmp.hub.shop.off", "The Merchant is not trading.");
            case UNKNOWN_OFFER -> QuestService.tell(sp, "message.nusmp.hub.shop.unknown", "The Merchant does not sell that.");
            case LOCKED -> QuestService.tell(sp, "message.nusmp.hub.shop.locked", "The Merchant will not sell you that until you have reached floor %s.", offer == null ? 0 : offer.minFloor());
            case SOLD_OUT -> QuestService.tell(sp, "message.nusmp.hub.shop.sold_out", "You have bought all of that the Merchant will sell you today.");
            case CANNOT_AFFORD -> QuestService.tell(sp, "message.nusmp.hub.shop.poor", "You cannot afford that.");
            case OK -> { }
        }
    }

    /** The whole screen content for the player. */
    public static HubNet.ShopSync stateFor(ServerPlayer sp) {
        PlayerQuestData d = QuestService.fresh(sp);
        long day = QuestService.today(sp.server);
        double scale = HubConfig.get(HubConfig.PRICE_SCALE);
        List<HubNet.ShopEntry> out = new ArrayList<>();
        if (on()) {
            for (ShopTable.Offer o : HubCatalogs.shop().offers()) {
                if (out.size() >= HubNet.MAX_SHOP) break;
                out.add(new HubNet.ShopEntry(o.id(), o.item(), o.count(), ShopRules.price(o, scale), o.category(), ShopRules.remaining(o, d.boughtToday(o.id(), day)), o.minFloor() > d.maxFloor() ? o.minFloor() : 0));
            }
        }
        return new HubNet.ShopSync(Currency.balance(sp), Currency.unitName(), out);
    }

    public static void sendState(ServerPlayer sp) {
        if (HubSync.hasConnection(sp)) PacketDistributor.sendToPlayer(sp, stateFor(sp));
    }

    /** A line of chat for a player that tried a Merchant while the economy is off. */
    public static void tellOff(ServerPlayer sp) {
        sp.sendSystemMessage(Component.translatableWithFallback("message.nusmp.hub.shop.off", "The Merchant is not trading."));
    }
}
