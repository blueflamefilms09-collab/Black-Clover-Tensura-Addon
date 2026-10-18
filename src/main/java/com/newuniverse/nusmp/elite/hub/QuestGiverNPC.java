package com.newuniverse.nusmp.elite.hub;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Why: the Quest Giver of the Hub (the brief's {@code QuestGiverNPC}). A villager-style creature that stands in front of the notice pavilion of Floor 0; a right click opens the dialogue (accept,
 * hand in, abandon: {@link HubMenus.QuestMenu} and its screen) with today's board. Everything it does in the world is {@link HubNpcBase}; everything the dialogue does is {@link QuestService}.
 */
public class QuestGiverNPC extends HubNpcBase {
    public QuestGiverNPC(EntityType<? extends QuestGiverNPC> type, Level level) { super(type, level); }

    @Override
    protected void open(ServerPlayer player) {
        if (!QuestService.on()) {
            player.sendSystemMessage(Component.translatableWithFallback("message.nusmp.hub.quest.off", "The Quest Giver has nothing for you today."));
            return;
        }
        QuestService.fresh(player);
        openMenu(player, (id, inv, p) -> new HubMenus.QuestMenu(id, getId()));
        QuestService.sendBoard(player);
    }
}
