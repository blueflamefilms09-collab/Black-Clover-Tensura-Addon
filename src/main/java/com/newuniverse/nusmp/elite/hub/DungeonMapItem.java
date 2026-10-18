package com.newuniverse.nusmp.elite.hub;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Why: the brief's "Dungeon Map" is a real item whose use reveals the layout of the CURRENT floor. The item itself does nothing but ask the server ({@link HubMap}) to build the picture of the floor the
 * player stands on and send it; the screen that draws it is client-side. It is a plain stack-of-one-sixteen item (a player may carry several, because {@code map.mode = consumable} uses one up at
 * each use); whether a use costs anything is the server's config ({@code durable} by default: never used up). On the client a use just reports success so the hand swings; the server decides
 * everything.
 */
public class DungeonMapItem extends Item {
    public DungeonMapItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player instanceof ServerPlayer sp) HubMap.use(sp, stack);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nusmp.dungeon_map.tip1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.nusmp.dungeon_map.tip2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
