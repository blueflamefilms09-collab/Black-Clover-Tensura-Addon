package com.newuniverse.nusmp.elite.forge.item;

import com.newuniverse.nusmp.elite.forge.rules.StationTier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Why: the item half of the Station Upgrade handler ("item + cost"). A station upgrade kit is crafted from the materials of the tier it opens (that is its cost,
 * the recipes are in data/nusmp/recipe), and right clicking a station of the PREVIOUS tier with it turns the station into the next tier, using the kit up. The kit
 * only knows which tier it makes; the rule (right tier, enabled, not creative-free) is {@code StationUpgrade}. Three kits exist (Magisteel, Mythril, Dragonforge);
 * the Basic station is crafted directly.
 */
public class UpgradeKitItem extends Item {
    private final StationTier target;

    public UpgradeKitItem(StationTier target, Properties props) {
        super(props);
        this.target = target;
    }

    /** The tier of station this kit makes. */
    public StationTier target() { return target; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("nusmp.eforge.kit.tip", Component.translatable("nusmp.eforge.tier." + target.prev().id())).withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable("nusmp.eforge.kit.tip2", Component.translatable("nusmp.eforge.tier." + target.id())).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
