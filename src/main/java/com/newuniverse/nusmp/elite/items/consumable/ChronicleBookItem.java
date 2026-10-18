package com.newuniverse.nusmp.elite.items.consumable;

import com.newuniverse.nusmp.elite.items.EliteComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
 * Why: the Chronicle Book is the item the seasons builder will hang the chronicles on. It opens NOTHING yet (the owner's inventory names it; the seasons subsystem gives it its use): it only STORES a
 * chronicle id in the data component {@code nusmp:chronicle_id}, read with {@link EliteComponents#chronicleId}, and says so when used. In hand it is a real 3D book (the model
 * {@code models/item/chronicle_book.json}, built by {@code tools/elite/gen_items_art.py} in the manner of the 3D Skill Book); the inventory shows its flat icon. Not a weapon, so no engravings.
 * A book with no id is a blank chronicle.
 */
public class ChronicleBookItem extends Item {
    public ChronicleBookItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    /** A Chronicle Book of this chronicle (the factory other systems use). */
    public static ItemStack of(ItemStack book, String chronicleId) {
        EliteComponents.setChronicleId(book, chronicleId);
        return book;
    }

    @Override
    public boolean isFoil(ItemStack stack) { return !EliteComponents.chronicleId(stack).isEmpty(); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            String id = EliteComponents.chronicleId(stack);
            player.displayClientMessage(id.isEmpty()
                    ? Component.translatableWithFallback("message.nusmp.chronicle.blank", "The pages are blank. No chronicle is written here yet.").withStyle(ChatFormatting.GRAY)
                    : Component.translatableWithFallback("message.nusmp.chronicle.bound", "A chronicle is bound to this book: %s. Its pages will open in a later season.", id).withStyle(ChatFormatting.GOLD), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        String id = EliteComponents.chronicleId(stack);
        if (id.isEmpty()) tip.add(Component.translatableWithFallback("tooltip.nusmp.chronicle.blank", "A blank chronicle.").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        else tip.add(Component.translatableWithFallback("tooltip.nusmp.chronicle.id", "Chronicle: %s", id).withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatableWithFallback("tooltip.nusmp.chronicle.note", "It opens nothing yet; it only remembers which chronicle it belongs to.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
