package com.newuniverse.nusmp.labyrinth.seal;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Why: the Shadow Key is the Labyrinth's one progression item: the Floor Warden drops it (by code, always: {@link FloorWardenEntity}) and a
 * Gatekeeper Door of kind {@code warden} eats it ({@link GatekeeperDoorBlock}). It is a plain {@link Item} with no data components, because
 * the door only asks "is this stack a Shadow Key" and a key with no per-stack state stacks (16: a group that farmed one Warden each carries
 * them in one slot) and survives any inventory mod untouched. Epic rarity and the foil sheen mark it as the thing to keep when a player
 * sorts a full inventory on the stairs; the tooltip says where it comes from and what it opens so nobody has to read the wiki.
 */
public class ShadowKeyItem extends Item {
    public ShadowKeyItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));
    }

    @Override public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nusmp.shadow_key.tooltip").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
