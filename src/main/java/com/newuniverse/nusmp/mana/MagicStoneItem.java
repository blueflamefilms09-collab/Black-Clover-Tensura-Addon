package com.newuniverse.nusmp.mana;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A Magic Stone: one of the ancient jewels that amplify mana and open the way to negative mana (Forbidden Magic). Use it to
 * attune for a minute: every spell is 60% stronger and 30% cheaper, and every cast corrupts you a little. Only an Arcane Stage
 * mage can carry it (see {@link ManaLaw#attuneNegative}). The stone is spent.
 */
public class MagicStoneItem extends Item {
    public MagicStoneItem(Properties props) { super(props); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.pass(stack);
        if (!ManaLaw.attuneNegative(p)) return InteractionResultHolder.fail(stack);
        if (!p.isCreative()) stack.shrink(1);
        p.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Use: attune to negative mana for 60 s (spells +60% power, -30% mana cost).").withStyle(ChatFormatting.DARK_PURPLE));
        tip.add(Component.literal("  Every cast corrupts you; at 100 corruption your grimoire seals itself.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("  Only an Arcane Stage mage can hold it; anyone else is torn by it.").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("One of the ancient Magic Stones, jewels that amplify mana and open the way to the underworld.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
