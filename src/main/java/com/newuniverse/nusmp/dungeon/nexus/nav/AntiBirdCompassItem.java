package com.newuniverse.nusmp.dungeon.nexus.nav;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Anti-Bird Compass (docs/living_dungeon.md section 3): a rare drop of the avian scavengers of the Upper Catacombs. In the Dungeon it
 * points dead centre at the nearest cluster of life (the server writes where into the stack's {@code target} component every two seconds
 * while it is in a hand or on the hotbar, {@link NavTicker}), and it spins wildly in a pocket of warped space (the client item property
 * {@code nusmp:angle}, client/nexus/NavClient). Outside the Dungeon it has nothing to point at.
 */
public final class AntiBirdCompassItem extends Item {
    AntiBirdCompassItem(Item.Properties props) { super(props); }

    /** Where the compass points (the middle of a cluster of life), or null. */
    public static BlockPos targetOf(ItemStack stack) { return stack.get(NavComponents.TARGET.get()); }

    /** Writes where the compass points, only if that changed (so the stack is not re-sent for nothing); null clears it. Returns whether it changed. */
    static boolean setTarget(ItemStack stack, BlockPos target) {
        BlockPos was = targetOf(stack);
        if (target == null) {
            if (was == null) return false;
            stack.remove(NavComponents.TARGET.get());
            return true;
        }
        if (target.equals(was)) return false;
        stack.set(NavComponents.TARGET.get(), target);
        return true;
    }

    /** Held items change data in the background (the target, every two seconds): that must not replay the hand's equip animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Points dead centre at the nearest cluster of life in the Dungeon.").withStyle(ChatFormatting.GREEN));
        tip.add(Component.literal("It spins wildly where space is warped, and has nothing to point at above ground.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Held in a hand or kept on the hotbar, it reads the life within six chunks.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
