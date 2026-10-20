package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.elite.hub.CoinMath;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Why: a monster part is an ordinary stackable item whose whole interest is what it is FOR, so the tooltip says so: its lore, what it sells for to a Hub Merchant, and for the
 * three parts that have a use in code (the Shadow Core Shard, the Golem Core and the Venom Sac, which work at an anvil) a line saying what. The crafting uses are the recipes
 * (shown by the recipe book); the selling is {@link MaterialSell}. Tier three parts are uncommon, the shadow shard rare.
 */
public class LootMaterialItem extends Item {
    private final String id, lore;
    private final int value;
    private final boolean code;

    public LootMaterialItem(Properties props, String id, String lore, int value, boolean code) {
        super(props);
        this.id = id;
        this.lore = lore;
        this.value = value;
        this.code = code;
    }

    /** The item properties of a material of the given tier (1 to 3). */
    public static Properties props(int tier) {
        return new Properties().rarity(tier >= 3 ? Rarity.UNCOMMON : Rarity.COMMON);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("item.nusmp." + id + ".lore", lore).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        if (code) tip.add(LootText.text("tooltip.nusmp.loot.use." + id).withStyle(ChatFormatting.AQUA));
        tip.add(LootText.text("tooltip.nusmp.loot.value", CoinMath.describe(value)).withStyle(ChatFormatting.DARK_GRAY));
        tip.add(LootText.text("tooltip.nusmp.loot.sell_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
