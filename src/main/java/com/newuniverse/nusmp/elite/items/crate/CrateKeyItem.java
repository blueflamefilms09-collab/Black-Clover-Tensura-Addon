package com.newuniverse.nusmp.elite.items.crate;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Why: the key of a crate tier. It does nothing in the hand; {@link CrateBlock} checks for it when the crate is used and {@link CrateOpening} takes one. A key is a separate class (not a plain
 * {@code Item}) only so the block can ask "is this the key of MY tier" with an {@code instanceof} and the tooltip can name the tier. 16 to a stack: keys are meant to be counted.
 */
public class CrateKeyItem extends Item {
    private final CrateTier tier;

    public CrateKeyItem(CrateTier tier) {
        super(new Item.Properties().stacksTo(16).rarity(switch (tier) {
            case COMMON -> Rarity.COMMON;
            case RARE -> Rarity.RARE;
            case ELITE -> Rarity.EPIC;
        }));
        this.tier = tier;
    }

    public CrateTier tier() { return tier; }

    @Override
    public boolean isFoil(ItemStack stack) { return tier == CrateTier.ELITE; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("tooltip.nusmp.crate_key", "Opens one %s Crate. Use it on the crate.",
                Component.translatableWithFallback("crate_tier.nusmp." + tier.id(), cap(tier.id()))).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.crate_key.used", "The key is used up.").withStyle(ChatFormatting.DARK_GRAY));
    }

    static String cap(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
}
