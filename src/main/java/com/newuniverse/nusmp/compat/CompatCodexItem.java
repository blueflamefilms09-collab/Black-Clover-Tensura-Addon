package com.newuniverse.nusmp.compat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Compatibility Codex item: a grimoire-style book (3D model through the grimoire geometry, see {@link CompatCodexLook}) that opens the
 * {@code CompatCodexScreen} on right-click. Opening is client-only: the server does nothing. The call to the client class sits behind
 * {@code level.isClientSide()}, so on a dedicated server that class is never loaded.
 */
public final class CompatCodexItem extends Item {
    public CompatCodexItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) com.newuniverse.nusmp.client.compat.CompatCodexClient.open();
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Same conventions as the grimoire tooltip: gold title line, purple italic lore, dark-grey details. */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("nusmp.compat.codex.tip.kind").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("nusmp.compat.codex.tip.kingdom").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("nusmp.compat.codex.tip.lore").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("nusmp.compat.codex.tip.ornament", Component.translatable("nusmp.compat.codex.motif")).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("nusmp.compat.codex.tip.use").withStyle(ChatFormatting.GRAY));
    }
}
