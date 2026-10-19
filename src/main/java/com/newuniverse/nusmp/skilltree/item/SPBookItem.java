package com.newuniverse.nusmp.skilltree.item;

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
 * Why: Skill Points no longer exist (1.96.0 removed the Skill Tree), but a world saved before may still hold {@code nusmp:sp_book} stacks in chests and inventories, and an item id that vanished would delete
 * them. So the item stays registered, with its data components ({@code nusmp:sp_amount}, {@code nusmp:sp_kind}) so the stack loads, and it is nothing but a voucher now: it is never dropped, sold or offered in
 * the creative tab any more; right-clicking an old one turns it into a random Skill Book the reader does not own yet ({@link BookUse#readSpBook}). The model is the plain sprite.
 */
public class SPBookItem extends Item {
    public SPBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) { return Component.translatableWithFallback("item.nusmp.sp_book.faded", "Faded SP Book"); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatableWithFallback("tooltip.nusmp.sp_book.removed", "Skill Points are gone. Right-click to swap this for a Skill Book.").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer server) BookUse.readSpBook(server, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
