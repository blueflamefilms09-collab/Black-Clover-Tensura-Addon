package com.newuniverse.nusmp.relics.djinn.dark;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A Dark Rukh Shard: a splinter of black rukh, the stuff Al-Thamen feeds to a bond. Sneak and use it with the bound vessel of your own Djinn in
 * the other hand and the Djinn drinks it ({@link DarkDjinn#drink}): the Dark Djinn. It does nothing else, and does nothing if the bond is
 * refused, so a mis-click never wastes one.
 */
public class DarkRukhShardItem extends Item {
    public DarkRukhShardItem(Item.Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack shard = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.sidedSuccess(shard, level.isClientSide);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        DarkDjinn.drink(p, shard, other);
        return InteractionResultHolder.sidedSuccess(shard, false);
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.nusmp.dark_rukh_shard.desc1").withStyle(ChatFormatting.DARK_PURPLE));
        tip.add(Component.translatable("item.nusmp.dark_rukh_shard.desc2").withStyle(ChatFormatting.GRAY));
    }
}
