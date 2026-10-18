package com.newuniverse.nusmp.elite.hub;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Why: the Merchant of the Hub (the brief's {@code MerchantNPC}). A villager-style creature that stands behind the counter of a market stall on the plaza of Floor 0; a right click opens the shop
 * ({@link HubMenus.ShopMenu} and its screen) where the player spends coins on standard gear, survival food, utility items and the Dungeon Map. All the trading is {@link ShopService}; the
 * entity is only the face that opens it.
 */
public class MerchantNPC extends HubNpcBase {
    public MerchantNPC(EntityType<? extends MerchantNPC> type, Level level) { super(type, level); }

    @Override
    protected void open(ServerPlayer player) {
        if (!ShopService.on()) {
            ShopService.tellOff(player);
            return;
        }
        openMenu(player, (id, inv, p) -> new HubMenus.ShopMenu(id, getId()));
        ShopService.sendState(player);
    }
}
