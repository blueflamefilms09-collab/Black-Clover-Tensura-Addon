package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.elite.hub.CoinMath;
import com.newuniverse.nusmp.elite.hub.Currency;
import com.newuniverse.nusmp.elite.hub.MerchantNPC;
import com.newuniverse.nusmp.elite.hub.ShopService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Why: the monster parts are the dungeon's small change, and the Hub's Merchant ({@code elite/hub/MerchantNPC}, the shop of 1.85.0) is where change becomes coins. The shop
 * there only SELLS (its table is the offers a player buys), so the other direction is added beside it without touching it: sneak and use on the Merchant with a loot item in
 * the main hand and the whole stack is bought for its roster value (a piece of gear for its base value times its rarity's factor, see {@link LootValue}), paid in the same
 * Tensura coins as everything else through {@link Currency#give}, with the config's {@code sellFactor}. A plain click, or an item the Merchant has no price for, goes to the
 * shop screen exactly as before: the handler only takes the click when it sells something. The shop switch and the economy switch both apply (an economy that is off pays
 * nothing). Server side; the handler runs before the Merchant's own and cancels the click when it sells.
 */
public final class MaterialSell {
    private MaterialSell() {}

    public static final EventPriority PRIORITY = EventPriority.HIGH;

    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        try {
            if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND || !(e.getTarget() instanceof MerchantNPC) || !(e.getEntity() instanceof ServerPlayer sp)) return;
            if (!sp.isShiftKeyDown() || !LootConfig.get(LootConfig.SELL)) return;
            ItemStack hand = sp.getMainHandItem();
            long coins = price(hand);
            if (coins <= 0L) return;
            if (!ShopService.on()) {
                sp.sendSystemMessage(LootText.text("message.nusmp.loot.shop_off"));
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
            Component name = hand.getHoverName();
            int count = hand.getCount();
            hand.shrink(count);
            Currency.give(sp, coins);
            sp.sendSystemMessage(LootText.text("message.nusmp.loot.sold", Component.literal(count + " x ").append(name), CoinMath.describe(coins)));
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
        } catch (RuntimeException ignored) {
            // the Merchant's own screen opens as before
        }
    }

    /** What the Merchant pays for the stack in bronze coins, with the config's factor; 0 when it is not loot. */
    public static long price(ItemStack stack) {
        if (stack.isEmpty()) return 0L;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (!"nusmp".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace()) || !LootItems.isLoot(id)) return 0L;
        double factor = LootConfig.get(LootConfig.SELL_FACTOR);
        return Math.round(LootValue.stack(LootRoster.get(), id, GearStacks.rollOf(stack), stack.getCount()) * factor);
    }
}
