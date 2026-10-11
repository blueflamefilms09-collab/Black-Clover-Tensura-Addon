package com.newuniverse.nusmp.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A physical Key Magic token collected by Janus Baptism and spent to open Janus Abigail gates.
 * Restored in 0.99 after being removed in the grimoire refactor; the item is kept (rule 2: add new, keep old).
 */
public final class MagicKeyItem extends Item {
    public MagicKeyItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.nusmp.magic_key.tooltip"));
    }
}
