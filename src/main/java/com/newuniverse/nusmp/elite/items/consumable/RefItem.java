package com.newuniverse.nusmp.elite.items.consumable;

import com.newuniverse.nusmp.elite.items.EliteComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Why: the Walpurgis Orb and the Walpurgis Seal are plain items whose only data is a free string id, {@code nusmp:elite_ref}, that the council builder gives meaning to (which council, which motion,
 * which member). They have no use of their own; the tooltip shows the id when it is set. Read and write it with {@link EliteComponents#eliteRef} / {@link EliteComponents#setEliteRef}. One class
 * for both, only the registered id and the rarity differ.
 */
public class RefItem extends Item {
    private final String lore;

    public RefItem(String lore, Rarity rarity) {
        super(new Item.Properties().stacksTo(16).rarity(rarity));
        this.lore = lore;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return !EliteComponents.eliteRef(stack).isEmpty(); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("tooltip.nusmp." + lore, lore.equals("walpurgis_orb") ? "A crystal of the Walpurgis council." : "The seal of the Walpurgis council.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        String ref = EliteComponents.eliteRef(stack);
        if (!ref.isEmpty()) tip.add(Component.translatableWithFallback("tooltip.nusmp.elite_ref", "Bound to: %s", ref).withStyle(ChatFormatting.GOLD));
    }
}
