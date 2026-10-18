package com.newuniverse.nusmp.relics.tier;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The two catalysts of the top jumps that Tensura has no item for: the Devil's Core (to God grade, as an alternative to four Hihiirokane ingots)
 * and the Genesis Seed (to Genesis grade). The lower jumps use Tensura's own ingots (see {@link TierRules#catalystsTo}).
 */
public final class TierItems {
    private TierItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);

    public static final DeferredItem<Item> DEVILS_CORE = ITEMS.register("devils_core",
            () -> new CatalystItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant(), "item.nusmp.devils_core.tip", ChatFormatting.DARK_RED));
    public static final DeferredItem<Item> GENESIS_SEED = ITEMS.register("genesis_seed",
            () -> new CatalystItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant(), "item.nusmp.genesis_seed.tip", ChatFormatting.WHITE));

    /** 1.84.0: frees an attuned relic from its attunement (hold it with the relic in the other hand and use it). */
    public static final DeferredItem<Item> UNBINDING_SEAL = ITEMS.register("unbinding_seal",
            () -> new com.newuniverse.nusmp.relics.UnbindingSealItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    /** A plain item whose only use is the ascension ritual; it explains that in its tooltip and glints. */
    static final class CatalystItem extends Item {
        private final String tipKey;
        private final ChatFormatting colour;

        CatalystItem(Properties props, String tipKey, ChatFormatting colour) {
            super(props);
            this.tipKey = tipKey;
            this.colour = colour;
        }

        @Override public boolean isFoil(ItemStack stack) { return true; }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.translatable(tipKey).withStyle(colour));
            tip.add(Component.translatable("item.nusmp.catalyst.how").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Creative tab entries. */
    public static List<Item> all() { return List.of(DEVILS_CORE.get(), GENESIS_SEED.get(), UNBINDING_SEAL.get()); }
}
