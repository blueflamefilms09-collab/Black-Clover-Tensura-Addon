package com.newuniverse.nusmp.engraving.codex;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Engravement Codex item: a grimoire-style book (3D model through the grimoire geometry, see {@link EngravingCodexLook}) that opens the {@code EngravingCodexScreen} on right-click. It
 * is a purely client-side book: the server does nothing and sends nothing (the data the book shows is on every client already). The call to the client class sits behind
 * {@code level.isClientSide()}, so on a dedicated server that class is never loaded. The tooltip lines are decorated by the unified tooltip like every item of the mod.
 */
public final class EngravingCodexItem extends Item {
    public EngravingCodexItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) com.newuniverse.nusmp.client.engraving.EngravingCodexClient.open();
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatableWithFallback("nusmp.engraving_codex.tip.kind", "A book of every engraving in the mod").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatableWithFallback("nusmp.engraving_codex.tip.lore", "What each engraving does, its cooldowns and caps, and which items carry it.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatableWithFallback("nusmp.engraving_codex.tip.use", "Use: open the book. Type in the search box to find an engraving or an item.").withStyle(ChatFormatting.GRAY));
    }
}
