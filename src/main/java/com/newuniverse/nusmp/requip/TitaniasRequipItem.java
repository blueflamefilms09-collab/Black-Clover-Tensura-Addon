package com.newuniverse.nusmp.requip;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Why: Requip: The Knight is an Ultimate skill, so it is not taught by a grimoire: it is taught by this book. Using it teaches the skill directly if the reader's max EP is high enough (or a
 * mastered Unique skill makes up for part of it, {@link RequipRules#eligible}); the book is used up only after the skill was learned. Dungeon loot (see {@link RequipSystem}) and the admin command
 * {@code /requip book} give it out.
 */
public class TitaniasRequipItem extends Item {
    public TitaniasRequipItem() { super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.success(stack);
        String why = RequipSystem.teach(p, false);
        if (why != null) {
            p.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        stack.shrink(1);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatableWithFallback("nusmp.requip.book.lore", "Teaches Requip: The Knight (Ultimate skill): fourteen armours, one per mode.").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatableWithFallback("nusmp.requip.book.need", "Needs %s max EP, or %s with a mastered Unique skill.",
                (long) RequipRules.REQUIRED_EP, (long) RequipRules.REQUIRED_EP_WITH_UNIQUE).withStyle(ChatFormatting.DARK_GRAY));
    }
}
