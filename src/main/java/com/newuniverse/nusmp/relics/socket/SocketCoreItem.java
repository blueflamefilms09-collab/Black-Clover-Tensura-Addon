package com.newuniverse.nusmp.relics.socket;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A Spirit Core or a Devil Core: an item that only means something to {@link SocketEngine} (sneak-use with a relic in the main hand and the Core in the
 * off hand, or {@code /multiverse relic socket}). It does nothing when used on its own.
 */
public final class SocketCoreItem extends Item {
    private final SocketDef def;

    public SocketCoreItem(SocketDef def, Properties props) {
        super(props);
        this.def = def;
    }

    public SocketDef socket() { return def; }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(def.nameKey()).withColor(def.rgb()));
        tip.add(Component.translatable("socket.nusmp.core." + def.kind().prefix()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("socket.nusmp.core.how").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.translatable("socket.nusmp.core.act", def.act().name()).withStyle(ChatFormatting.DARK_AQUA));
    }
}
