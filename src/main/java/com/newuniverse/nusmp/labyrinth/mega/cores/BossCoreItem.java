package com.newuniverse.nusmp.labyrinth.mega.cores;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Why: the core a deep Decennial boss lets go of: a glowing gem that is the crafting material of one armour set ({@link BossCoreRules.Kit}). It does nothing by itself; it does not burn
 * (fire resistant), it shimmers (foil) and its tooltip says which set it makes and where it drops.
 */
public class BossCoreItem extends Item {
    private final BossCoreRules.Kit kit;

    public BossCoreItem(BossCoreRules.Kit kit) {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.EPIC).fireResistant());
        this.kit = kit;
    }

    public BossCoreRules.Kit kit() { return kit; }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("item.anime_multiverse." + kit.coreId() + ".lore", "Dropped by the deep bosses of the Mega-Dungeon. Crafts the " + kit.id() + " armour.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tip.add(Component.translatableWithFallback("tooltip.anime_multiverse.boss_core.drop", "Paid once per boss hall, from floor 250 (Decennial bosses).").withStyle(ChatFormatting.DARK_GRAY));
    }
}
